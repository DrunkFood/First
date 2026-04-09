# ele-tender-support

## 模块定位

`ele-tender-support` 是电子标系统的支撑中心模块，负责认证、用户、角色、菜单、版本、外部系统接入和访问日志查询等通用管理能力。
该模块可独立启动，对外接口统一以 `/api` 为前缀。

## 核心能力

- 提供登录、登出、当前用户信息、菜单和密码修改能力
- 提供用户、角色、菜单、版本、外部系统管理接口
- 提供对业务系统的外部认证能力，包括换取 token、验签和获取外部用户信息
- 提供访问日志查询接口，并承接公共日志链路的查询入口
- 提供 crypto 解密请求管理、工件明细和人工重试接口

## 关键接口分组

- 认证：`/api/auth/*`
- 用户：`/api/users/*`
- 角色：`/api/roles/*`
- 菜单：`/api/menus/*`
- 版本：`/api/versions/*`
- 外部系统：`/api/external-systems/*`
- 外部认证：`/api/external/*`
- 访问日志：`/api/access-logs`
- crypto 管理：`/api/crypto/manage/*`

## 关键控制器

- `AuthController`：`/auth/login`、`/auth/logout`、`/auth/userinfo`、`/auth/info`、`/auth/menus`、`/auth/change-password`
- `UserController`：`/users`、`/users/{id}`、`/users/{id}/reset-password`、`/users/{id}/status`
- `RoleController`：`/roles`、`/roles/all`、`/roles/{id}`、`/roles/{id}/menus`
- `MenuController`：`/menus`、`/menus/tree`
- `VersionController`：`/versions`、`/versions/{id}`、`/versions/{id}/publish`、`/versions/{id}/deprecate`、`/versions/{versionId}/plugins`、`/versions/plugins/{id}`
- `ExternalSystemController`：`/external-systems`、`/external-systems/{id}`、`/external-systems/{id}/regenerate-secret`、`/external-systems/{id}/status`
- `ExternalAuthController`：`/external/token`、`/external/verify`、`/external/userinfo`
- `AccessLogController`：`/access-logs`
- `CryptoManageController`：`/crypto/manage/requests`、`/crypto/manage/artifacts/{artifactId}`、`/crypto/manage/requests/{requestId}/retry`

## 依赖关系

- 依赖 `ele-tender-common` 和 `ele-tender-common-interaction`
- 为 `ele-tender-interaction` 和 `ele-tender-tender-document` 提供外部认证、文件协同和接入基础能力
- 为 `ele-tender-crypto` 提供业务系统配置、external token 和管理查询入口
- 启动类为 `com.jy.eletender.support.SupportApplication`

## 常用命令

```bash
cd ele-tender-system
mvn -pl ele-tender-support -am spring-boot:run
mvn -pl ele-tender-support -am package
```

本地配置入口：
- `src/main/resources/application.yml`
- `src/main/resources/ele-tender-support-local.yml`

## 注意事项

- 认证采用 JWT + Redis 双校验
- 所有参与鉴权的服务必须共享同一套 JWT 密钥与过期策略
- 外部系统签名校验统一使用 `SignatureUtil` 的 `HMAC-SHA256`
- 密码统一使用 `PasswordUtil` 的 `BCrypt`
- 访问日志后端查询入口固定在本模块，前缀为 `/api/access-logs`
- crypto 管理接口归属本模块，不在 `ele-tender-crypto` 内重复提供后台管理端点
- 新增管理接口时，应同步确认是否需要登录、权限控制和日志落库
