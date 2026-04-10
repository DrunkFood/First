# 2026-03-25 support 前端 base 路径部署问题需求整理

## 1. 背景

- 用户反馈访问 `http://10.11.20.50:19500/ele-tender-support/` 打不开。
- 前端打包文件已部署在 `nginx-1.28.3/html` 目录下。

## 2. 现象排查

- 页面入口 `index.html` 可正常返回（HTTP 200）。
- `index.html` 中静态资源引用为根路径：`/assets/*.js`、`/assets/*.css`。
- 实际可访问路径为：`/ele-tender-support/assets/*`（HTTP 200）。
- 根路径资源 `/assets/*` 返回 404。
- 支撑服务接口 `/support-api/v3/api-docs` 可访问（HTTP 200），后端服务本身正常。

## 3. 根因

- 前端采用子路径 `/ele-tender-support/` 部署，但构建产物使用了根路径 base（`/`），导致静态资源请求地址错误，页面白屏或无法正常加载。

## 4. 处理目标

- 将前端构建 base 调整为生产环境 `/ele-tender-support/`，保证子路径部署时静态资源地址正确。
- 保持本地开发环境（`vite dev`）仍使用根路径 `/`，避免影响日常联调。
