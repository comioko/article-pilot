# 创作工作台

入口：已完成文章的详情页点击「创作工作台」，或生成完成后点击「进入创作工作台」。路由为 `/article/:taskId/workbench`。

## 使用方式

- 左侧目录从正文的 Markdown 标题实时提取，点击后跳转到编辑器中的对应位置。
- 中间直接编辑正文，切换到「预览」查看排版和图片。正文中的 Markdown 图片地址会随文章保留。
- 在编辑器中选中文字，右侧选择精修要求或自行填写。生成后对比修改前后，可编辑候选稿，再采纳到草稿。
- 填写版本说明并「保存版本」，支持 Cmd/Ctrl + S。第一次保存保留初始成稿；版本记录支持全文预览和恢复，恢复也会形成新版本。
- 保存后在「发布包」中生成公众号或小红书文案。两种渠道结果分别保留在当前页面，可编辑、复制、下载；离开页面后发布包不会持久化。
- 未保存的正文在离开或刷新时会提示。发生版本冲突，草稿留在编辑器中，可以先下载草稿再重新加载服务器版本。

## 数据与兼容

本次页面不新增数据库表，复用之前的 `article_revision`。已有数据库若尚未初始化版本历史，需要执行 `sql/add_article_revisions.sql`；全新部署的表结构已在 `deploy/mysql/01-schema.sql` 中。

工作台将当前图文 Markdown 同步写入 `content` 和 `fullContent`，以便后续读取路径都获得最新稿件。历史快照保留两个字段的原始值。

文章详情增加 `contentFingerprint`。保存/恢复请求传入 `baseFingerprint` 检查正文是否已改变，同时在事务内锁定文章行，串行分配版本号。旧调用方可暂时省略指纹；工作台始终携带它。仅已完成且有权限的文章允许编辑。

预览、文章详情和共用 Markdown 渲染器使用 DOMPurify 清理 HTML，移除脚本和事件属性。精修使用原始选区位置而非首次文字匹配，重复段落不会被误替换。正文变化后旧精修候选失效。

## 验证

```sh
# 项目根目录：服务层权限、冲突与版本快照测试（无外部模型/数据库）
./mvnw -B -ntp -Dtest=ArticleWorkbenchTest test

# frontend 目录：编辑算法与构建
npm run test:workbench
npm run build

# 浏览器测试：默认使用 Playwright Chromium；首次需要安装浏览器
npx playwright install chromium
npm run test:workbench:e2e

# 若本机已安装 Chrome，也可使用
PLAYWRIGHT_CHANNEL=chrome npm run test:workbench:e2e
```

浏览器测试拦截 `/api/` 请求，覆盖编辑、重复选区精修、保存、发布下载、版本恢复、失败保留草稿、离开确认、HTML 清理和手机布局。它不验证真实 DashScope 服务质量或 MySQL 事务执行。真实环境需要后端、MySQL、Redis 和现有 DashScope 配置可用。
