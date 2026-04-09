# 支撑中心模块规范

## 1. 定位

`ele-tender-support` 负责认证、管理端配置、访问日志查询、版本管理、外部系统管理以及 crypto 管理入口。

## 2. 当前接口

- `/api/auth/*`
- `/api/users/*`
- `/api/roles/*`
- `/api/menus/*`
- `/api/versions/*`
- `/api/external-systems/*`
- `/api/external/*`
- `/api/access-logs`
- `/api/crypto/manage/*`

## 3. 当前关键表

### 表用途说明

| 表名 | 用途 |
|------|------|
| `sup_user` | 用户账号，存储 `username` / `password_hash` / `real_name` / `status` |
| `sup_role` | 角色定义，存储 `role_code` / `role_name` |
| `sup_user_role` | 用户角色关联 |
| `sup_menu` | 菜单树，存储 `menu_type` / `path` / `icon` / `parent_id` / `sort_no` / `permission` |
| `sup_role_menu` | 角色菜单关联 |
| `sup_access_system` | 外部接入系统，存储 `app_key` / `app_secret` / `status` / `callback_url` / `callback_signature_key` / `tender_document_suffix` / `bid_document_suffix` |
| `sup_main_version` | 主版本（软件版本管理） |
| `sup_plugin_version` | 插件版本 |
| `sup_operation_log` | 操作日志（由 AOP 切面写入） |
| `sup_access_log` | HTTP 入站访问日志（全系统统一入口） |

## 4. 核心约束

- external token 统一由本模块签发
- 访问日志查询后端接口统一归属本模块
- crypto 的后台管理端点统一归属本模块
- 外部系统签名使用 `HMAC-SHA256`
- 权限接口统一使用 `@RequirePermission`

JWT 约束、字符集约束与通用安全规范见 [PROJECT_SPEC_FINAL.md](PROJECT_SPEC_FINAL.md)。

## 5. 管理边界

- `ele-tender-crypto` 不提供重复的后台管理接口
- `ele-tender-file` 不提供管理页面逻辑
- 接入系统配置以 `sup_access_system` 为唯一来源

## 6. 排障原则

- **登录失败** → 查 `sup_user.status` 字段是否为启用状态，检查 Redis `token:{userId}` 是否存在
- **外部认证签名失败** → 查 `sup_access_system.app_secret` 是否最新（是否已 regenerate），检查请求时间戳偏差是否超出阈值
- **外部系统 token 无效** → 查 Redis `external_token:{appKey}:{userId}` 是否过期或被踢出
- **crypto 解密任务未回调** → 查 `sup_access_system.callback_url` 是否配置正确，核对 `callback_signature_key` 是否匹配业务系统侧配置

## 相关规范

- [CODE_CONVENTIONS.md](CODE_CONVENTIONS.md) — 通用编码规范（命名、分层、异常处理、安全）
- [PROJECT_SPEC_FINAL.md](PROJECT_SPEC_FINAL.md) — 全局模块边界与 JWT 约束
- [ELE_TENDER_CRYPTO_SPEC.md](ELE_TENDER_CRYPTO_SPEC.md) — crypto 管理接口所对应的业务模块规范
