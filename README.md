# ArticlePilot

Vue 3 + Spring Boot 3 的 AI 文章创作应用。

## 生产部署

项目已提供 Docker Compose、Caddy 自动 HTTPS、MySQL、Redis、GHCR 镜像发布和 GitHub Actions 自动部署配置。

完整的首次上线、CI/CD、备份与回滚步骤见 [部署手册](docs/DEPLOYMENT.md)。

本地构建检查：

```bash
./mvnw -B -ntp -DskipTests verify
cd frontend && npm ci && npm run build
```
