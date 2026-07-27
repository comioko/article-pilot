# ArticlePilot Docker + GitHub Actions 部署手册

这套方案适用于一台 Ubuntu 22.04/24.04 AMD64 云服务器，生产流量路径如下：

```text
用户 -> Caddy（80/443，自动 HTTPS）
     -> Vue/Nginx 前端
     -> Spring Boot 后端（仅 Docker 内网）
     -> MySQL + Redis（仅 Docker 内网）
```

每次推送到 `main` 后，GitHub Actions 会：

1. 编译后端并构建前端。
2. 构建前后端 Docker 镜像。
3. 将镜像以 `latest` 和 `sha-<commit>` 两个标签推送到 GHCR。
4. 通过 SSH 上传部署配置到服务器。
5. 拉取本次提交对应的镜像，等待所有服务健康后完成发布。

## 0. 上线前必须处理的安全问题

原本的本地 `application.yml` 中存在看起来可用的 DashScope、腾讯云 COS 和 Pexels 密钥。现在配置已改为只从环境变量读取，但旧密钥仍应立即在对应平台中禁用并重新生成。

依次完成：

1. 在阿里云 DashScope 控制台轮换 API Key。
2. 在腾讯云访问管理控制台轮换 COS SecretId/SecretKey；建议创建仅有目标 Bucket 权限的子账号密钥，不要使用主账号密钥。
3. 在 Pexels 控制台轮换 API Key。
4. 如果这些值曾经被提交到任何 Git 仓库，还要检查并清理 Git 历史；仅删除当前文件不能让历史中的密钥失效。
5. 不要将服务器的 `/opt/article-pilot/.env` 提交到 GitHub。

本部署数据库不会创建默认管理员或测试账号，避免 `admin/12345678` 一类默认凭据直接暴露到公网。

## 1. 准备服务器和域名

推荐服务器配置：

- Ubuntu 22.04 或 24.04，AMD64。
- 至少 2 核 CPU、4 GB 内存、20 GB 磁盘；后端镜像包含 Chromium，用于 Mermaid 渲染。
- 一个有 `sudo` 权限的非 root 用户，例如 `deploy`。
- 一个域名，例如 `article.example.com`。

在域名 DNS 控制台添加：

```text
类型: A
主机记录: article
记录值: 服务器公网 IPv4
```

如果同时使用 IPv6，再添加正确的 `AAAA` 记录；没有配置 IPv6 时不要保留错误的 `AAAA` 记录，否则 HTTPS 验证可能失败。

在云厂商安全组中放行：

```text
TCP 22    SSH
TCP 80    HTTP 和 Let's Encrypt 验证
TCP 443   HTTPS
UDP 443   HTTP/3，可选但推荐
```

如果服务器位于中国大陆，域名通常需要完成 ICP 备案后才能正常使用 80/443 端口。

## 2. 安装 Docker

SSH 登录服务器：

```bash
ssh deploy@服务器公网IP
```

使用 Docker 官方便捷脚本安装 Docker Engine 和 Compose 插件：

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl
curl -fsSL https://get.docker.com -o /tmp/get-docker.sh
sudo sh /tmp/get-docker.sh
sudo usermod -aG docker "$USER"
```

退出 SSH 后重新登录，使 `docker` 用户组生效：

```bash
exit
ssh deploy@服务器公网IP
docker version
docker compose version
```

如果启用了 UFW：

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 443/udp
sudo ufw enable
sudo ufw status
```

不要向公网开放 MySQL `3306`、Redis `6379` 或后端 `8567`。Compose 没有映射这些端口。

## 3. 创建服务器部署目录

在服务器执行：

```bash
sudo mkdir -p /opt/article-pilot
sudo chown -R "$USER":"$USER" /opt/article-pilot
chmod 750 /opt/article-pilot
```

GitHub Actions 使用的 SSH 用户必须能够写入此目录，并能直接执行 `docker` 命令。

## 4. 创建生产环境变量

在本地项目根目录执行：

```bash
scp deploy/.env.example deploy@服务器公网IP:/opt/article-pilot/.env
ssh deploy@服务器公网IP
cd /opt/article-pilot
chmod 600 .env
nano .env
```

生成三个互不相同的随机密码：

```bash
openssl rand -base64 32
openssl rand -base64 32
openssl rand -base64 32
```

将 `.env` 至少修改为：

```dotenv
SITE_ADDRESS=article.example.com
PUBLIC_BASE_URL=https://article.example.com

MYSQL_ROOT_PASSWORD=第一个随机密码
DB_PASSWORD=第二个随机密码
REDIS_PASSWORD=第三个随机密码

DASHSCOPE_API_KEY=新的DashScope密钥
TENCENT_COS_SECRET_ID=新的腾讯云SecretId
TENCENT_COS_SECRET_KEY=新的腾讯云SecretKey
TENCENT_COS_REGION=ap-guangzhou
TENCENT_COS_BUCKET=你的Bucket名称
PEXELS_API_KEY=新的Pexels密钥
GEMINI_API_KEY=你的Gemini密钥

COOKIE_SECURE=true
API_DOCS_ENABLED=false
```

注意：

- `SITE_ADDRESS` 只写域名，不带 `http://`、`https://` 或路径。
- `PUBLIC_BASE_URL` 要写完整的 `https://域名`，Stripe 支付完成后会回到该地址。
- `DASHSCOPE_API_KEY` 是文章生成核心配置。
- COS、Pexels 和 Gemini 按实际启用的配图方式填写。
- 暂时不用 Stripe 时可以留空 Stripe 两项，支付功能会不可用，但其他服务可运行。
- `.env` 中等号右侧不要随意添加行尾注释。

没有域名时可以先用纯 HTTP 验证部署：

```dotenv
SITE_ADDRESS=:80
PUBLIC_BASE_URL=http://服务器公网IP
COOKIE_SECURE=false
```

获得域名后改回 HTTPS 配置并重新执行 `docker compose up -d`。

## 5. 允许服务器拉取 GHCR 镜像

GitHub Container Registry 的镜像默认可能是私有的。创建一个 GitHub Personal Access Token（classic），只授予 `read:packages`；私有仓库可能还需要 `repo` 读取权限。

在服务器以部署用户执行：

```bash
echo '你的GitHub PAT' | docker login ghcr.io -u 你的GitHub用户名 --password-stdin
```

确认显示 `Login Succeeded`。登录凭据必须属于执行 GitHub Actions SSH 部署的同一个 Linux 用户。

另一种方式是在首次构建后把两个 GHCR Package 设置为 Public，这样服务器不需要登录；源代码仓库是否公开与 Package 是否公开是两个独立设置。

## 6. 配置 GitHub Actions SSH 密钥

在本地电脑生成一把只用于部署的密钥，不要给它设置交互式密码：

```bash
ssh-keygen -t ed25519 -C "article-pilot-github-actions" -f ~/.ssh/article_pilot_actions
```

把公钥安装到服务器：

```bash
ssh-copy-id -i ~/.ssh/article_pilot_actions.pub deploy@服务器公网IP
```

如果本机没有 `ssh-copy-id`，执行：

```bash
cat ~/.ssh/article_pilot_actions.pub | ssh deploy@服务器公网IP 'umask 077; mkdir -p ~/.ssh; cat >> ~/.ssh/authorized_keys'
```

验证密钥：

```bash
ssh -i ~/.ssh/article_pilot_actions deploy@服务器公网IP
```

## 7. 配置 GitHub Secrets

打开 GitHub 仓库：

```text
Settings -> Secrets and variables -> Actions -> New repository secret
```

创建以下 Secrets：

| Secret | 内容 |
| --- | --- |
| `DEPLOY_HOST` | 服务器公网 IP 或域名 |
| `DEPLOY_PORT` | SSH 端口，默认 `22` |
| `DEPLOY_USER` | 部署用户，例如 `deploy` |
| `DEPLOY_SSH_KEY` | `~/.ssh/article_pilot_actions` 私钥的完整内容 |

读取私钥内容：

```bash
cat ~/.ssh/article_pilot_actions
```

私钥内容应包含 `BEGIN OPENSSH PRIVATE KEY` 和 `END OPENSSH PRIVATE KEY` 两行。

然后检查：

```text
Settings -> Actions -> General -> Workflow permissions
```

允许工作流写入 Packages。工作流自身已声明 `packages: write`，但组织级策略仍可能覆盖仓库设置。

可选：在 `Settings -> Environments` 创建 `production` 环境，并设置人工审批或仅允许 `main` 分支部署。

## 8. 提交部署文件

在本地项目根目录先查看改动：

```bash
git status
git diff --check
```

只暂存本次部署相关文件：

```bash
git add \
  .gitignore .dockerignore .github/workflows/deploy.yml \
  Dockerfile compose.prod.yml deploy docs README.md pom.xml \
  src/main/resources/application.yml \
  src/main/java/github/comioko/articlepilot/config/MermaidConfig.java \
  src/main/java/github/comioko/articlepilot/service/MermaidService.java \
  frontend/Dockerfile frontend/nginx.conf \
  frontend/.env.development frontend/.env.production \
  frontend/src/config/env.ts frontend/src/config/env.example.ts
```

确认暂存区没有 `.env` 或任何真实密钥：

```bash
git diff --cached --stat
git diff --cached
```

提交并推送：

```bash
git commit -m "chore: add production Docker deployment"
git push origin main
```

本地现有的 `sql/` 目录不是生产首次建库入口；生产使用 `deploy/mysql/01-schema.sql`，它不会创建默认测试用户。

## 9. 观察第一次自动部署

打开 GitHub 仓库的 `Actions -> CI and Deploy`：

1. `Validate application` 编译后端并构建前端。
2. 两个 `Publish image` 任务并行发布镜像。
3. `Deploy production` 上传 Compose/Caddy/SQL 文件并通过 SSH 启动服务。

如果 `Deploy production` 在拉取镜像时报 `unauthorized`，回到第 5 步处理 GHCR 登录或 Package 可见性，然后点击 `Re-run failed jobs`。

第一次启动 MySQL、下载镜像、申请 HTTPS 证书可能需要几分钟。工作流使用 `--wait --wait-timeout 240`，服务未健康时部署任务会失败，而不是静默显示成功。

## 10. 验证线上服务

在服务器执行：

```bash
cd /opt/article-pilot
docker compose --env-file .env -f compose.prod.yml ps
docker compose --env-file .env -f compose.prod.yml logs --tail=100 backend
docker compose --env-file .env -f compose.prod.yml logs --tail=100 gateway
```

在本地验证：

```bash
curl -i https://article.example.com/api/health/
```

预期 HTTP 状态为 `200`，响应数据中包含 `ok`。再用浏览器打开：

```text
https://article.example.com
```

如果启用了接口文档 `API_DOCS_ENABLED=true`，Knife4j 地址是：

```text
https://article.example.com/api/doc.html
```

生产环境建议保持接口文档关闭。

## 11. 创建第一个管理员

先在网页注册一个普通账号，然后在服务器执行：

```bash
cd /opt/article-pilot
docker compose --env-file .env -f compose.prod.yml exec mysql \
  mysql -uarticlepilot -p ArticlePilot
```

输入 `.env` 中的 `DB_PASSWORD`，进入 MySQL 后执行：

```sql
SELECT id, userAccount, userRole FROM user;
UPDATE user SET userRole = 'admin' WHERE userAccount = '你的账号';
SELECT id, userAccount, userRole FROM user WHERE userAccount = '你的账号';
EXIT;
```

退出网站并重新登录，使 Redis Session 中的用户信息刷新。

## 12. 配置 Stripe Webhook

需要支付功能时，在 Stripe Dashboard 创建生产 Webhook：

```text
https://article.example.com/api/webhook/stripe
```

至少订阅：

```text
checkout.session.completed
checkout.session.async_payment_succeeded
```

将 Stripe 提供的 `whsec_...` 写入服务器 `.env`：

```dotenv
STRIPE_API_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

然后重建后端容器：

```bash
cd /opt/article-pilot
docker compose --env-file .env -f compose.prod.yml up -d --force-recreate backend
docker compose --env-file .env -f compose.prod.yml logs -f backend
```

先使用 Stripe Test Mode 完成一笔完整支付和 Webhook 验签，再切换 Live Mode。

## 13. 日常发布流程

以后每次发布只需要：

```bash
git checkout main
git pull --ff-only
git merge 你的功能分支
git push origin main
```

GitHub Actions 会部署本次提交的精确 `sha-<commit>` 镜像，而不是仅依赖可变的 `latest` 标签。

Pull Request 只执行编译和前端构建，不推送镜像、不连接生产服务器。

## 14. 查看日志和重启

查看所有服务：

```bash
cd /opt/article-pilot
docker compose --env-file .env -f compose.prod.yml ps
```

持续查看后端日志：

```bash
docker compose --env-file .env -f compose.prod.yml logs -f --tail=200 backend
```

查看 HTTPS 网关日志：

```bash
docker compose --env-file .env -f compose.prod.yml logs -f --tail=200 gateway
```

重启单个服务：

```bash
docker compose --env-file .env -f compose.prod.yml restart backend
```

## 15. 回滚到历史版本

在 GitHub 提交记录中找到要回滚到的完整 commit SHA。服务器执行：

```bash
cd /opt/article-pilot
IMAGE_TAG=sha-完整commitSHA docker compose --env-file .env -f compose.prod.yml pull backend frontend
IMAGE_TAG=sha-完整commitSHA docker compose --env-file .env -f compose.prod.yml up -d --wait --wait-timeout 240 backend frontend gateway
```

确认恢复后，把 `/opt/article-pilot/.env` 中的 `IMAGE_TAG` 改成该 `sha-...`，否则下次不带临时环境变量执行 Compose 时会回到 `.env` 记录的标签。

数据库变更不随镜像自动回滚。上线涉及数据库迁移时，必须提前备份并为迁移编写向前兼容方案。

## 16. 备份和恢复 MySQL

创建备份：

```bash
cd /opt/article-pilot
mkdir -p backups
docker compose --env-file .env -f compose.prod.yml exec -T mysql \
  sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction ArticlePilot' \
  | gzip > "backups/article-pilot-$(date +%F-%H%M%S).sql.gz"
```

检查备份文件：

```bash
gzip -t backups/article-pilot-*.sql.gz
ls -lh backups/
```

恢复指定备份前先停止后端写入：

```bash
docker compose --env-file .env -f compose.prod.yml stop backend
gunzip -c backups/要恢复的文件.sql.gz | \
  docker compose --env-file .env -f compose.prod.yml exec -T mysql \
  sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" ArticlePilot'
docker compose --env-file .env -f compose.prod.yml start backend
```

至少再将备份同步到另一台机器或对象存储，不要只保存在同一块服务器磁盘。

## 17. 数据卷和初始化注意事项

`deploy/mysql/01-schema.sql` 只会在 MySQL 数据卷第一次创建时执行。之后修改该文件不会自动修改已有数据库，正式迭代应使用 Flyway/Liquibase 或显式版本化迁移脚本。

以下命令会永久删除 MySQL、Redis 和 Caddy 数据，生产环境不要执行：

```bash
docker compose --env-file .env -f compose.prod.yml down -v
```

正常停止服务只使用：

```bash
docker compose --env-file .env -f compose.prod.yml down
```

不带 `-v` 时命名数据卷会保留。

## 18. 常见故障

### HTTPS 证书申请失败

检查 DNS 是否已经指向当前服务器、80/443 是否同时在云安全组和 UFW 放行，并查看：

```bash
docker compose --env-file .env -f compose.prod.yml logs gateway
```

### 后端一直 unhealthy

查看：

```bash
docker compose --env-file .env -f compose.prod.yml logs mysql redis backend
```

重点检查 `.env` 的数据库/Redis 密码、DashScope Key、腾讯云配置以及服务器内存。

### 前端请求仍访问 localhost

当前前端已改为使用 `VITE_API_BASE_URL=/api`。必须重新构建并部署前端镜像，旧镜像不会自动获得此改动。

### GHCR 拉取 unauthorized

确认服务器已使用拥有 `read:packages` 的 PAT 登录，且登录用户与 `DEPLOY_USER` 相同：

```bash
docker logout ghcr.io
echo '新的PAT' | docker login ghcr.io -u GitHub用户名 --password-stdin
```

### 服务器是 ARM64

当前工作流构建 `linux/amd64`。如果服务器执行 `uname -m` 返回 `aarch64`，需要在工作流中加入 QEMU，并将平台改为 `linux/amd64,linux/arm64`，或换用 AMD64 服务器。
