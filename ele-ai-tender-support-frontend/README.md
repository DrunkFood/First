# ele-tender-support-frontend

## 模块定位

`ele-tender-support-frontend` 是电子标系统支撑中心前端，负责登录、首页、用户、角色、菜单、访问日志、外部系统和版本管理页面。

## 技术栈

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Element Plus

## 本地开发

```bash
npm install
npm run dev
```

默认端口：`3000`

## 构建

```bash
npm run build
```

生产构建默认 `base` 为：

```text
/ele-tender-support/
```

## 代理配置

- `/support-api -> ele-tender-support`
- `/file-api -> ele-tender-file`
- `/file-esign-api -> ele-tender-file`
- `/crypto-api -> ele-tender-crypto`

其中：

- `/support-api` 会重写到后端 `/api`
- `/crypto-api` 会重写到后端 `/api/crypto`

## 页面范围

- 登录：`/login`
- 首页：`/dashboard`
- 用户管理：`/system/user`
- 角色管理：`/system/role`
- 菜单管理：`/system/menu`
- 访问日志：`/system/access-log`
- 接入系统管理：`/external`
- 版本管理：`/version`
- 插件管理：`/version/:id/plugins`

## 关键文件

- `vite.config.ts`：开发代理、构建 base、分包策略
- `src/router/index.ts`：静态路由与菜单鉴权
- `src/utils/request.ts`：统一请求封装
- `src/store/user.ts`：登录态、菜单、权限缓存

## 注意事项

- 支撑中心接口统一走 `/support-api`
- 访问日志菜单与页面已经落地，不再是占位入口
- 生产部署到子路径时必须保留 `/ele-tender-support/` base 配置
- 文件服务接口与签章上传接口都不走 `/support-api`
