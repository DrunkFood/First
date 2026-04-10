# 2026-03-25 support 前端 base 路径部署修复计划

## 1. 范围

- 模块：`ele-tender-support-frontend`
- 文件：`vite.config.ts`

## 2. 计划步骤

1. 在 Vite 配置中增加生产环境 base：`/ele-tender-support/`。
2. 开发环境保持 base 为 `/`，避免影响本地开发与代理。
3. 执行 `npm run build` 验证构建成功。
4. 部署新 `dist` 到服务器后，验证：
   - `http://10.11.20.50:19500/ele-tender-support/` 能正常加载页面；
   - 页面资源请求地址为 `/ele-tender-support/assets/*`；
   - 登录与核心接口调用正常（`/support-api/*`、`/file-api/*`）。

## 3. 风险与回滚

- 风险：若线上访问入口改回根路径 `/`，需要同步调整 base。
- 回滚：恢复 `vite.config.ts` 中 base 配置并重新打包发布。
