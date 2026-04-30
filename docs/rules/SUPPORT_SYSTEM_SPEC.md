# 支撑中心模块规范

## 1. 定位

`ele-ai-tender-support` (端口 8080) 负责认证、用户/角色/菜单管理、模板配置、知识库配置、模型配置与路由、系统参数、政策文件、消息通知、统计分析、访问/操作日志、版本管理、外部系统对接。

## 2. 当前接口

### 2.1 认证管理 (`/api/auth`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/auth/public-key` | 获取RSA公钥 |
| POST | `/api/auth/login` | 账号密码登录 |
| POST | `/api/auth/send-sms-code` | 发送短信验证码 |
| POST | `/api/auth/phone-login` | 手机号登录 |
| POST | `/api/auth/reset-password` | 通过短信验证码重置密码 |
| POST | `/api/auth/logout` | 退出登录 |
| GET | `/api/auth/userinfo` | 获取当前用户信息 |
| GET | `/api/auth/info` | 获取当前用户信息（别名） |
| GET | `/api/auth/menus` | 获取当前用户菜单 |
| POST | `/api/auth/change-password` | 修改密码 |

### 2.2 用户管理 (`/api/users`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/users` | 分页查询用户列表 |
| GET | `/api/users/{id}` | 获取用户详情 |
| POST | `/api/users` | 创建用户 |
| PUT | `/api/users/{id}` | 更新用户 |
| DELETE | `/api/users/{id}` | 删除用户 |
| POST | `/api/users/{id}/reset-password` | 重置密码 |
| PUT | `/api/users/{id}/status` | 启用/禁用 |

### 2.3 角色管理 (`/api/roles`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/roles` | 分页查询角色列表 |
| GET | `/api/roles/all` | 获取全部角色 |
| GET | `/api/roles/{id}` | 获取角色详情 |
| POST | `/api/roles` | 创建角色 |
| PUT | `/api/roles/{id}` | 更新角色 |
| DELETE | `/api/roles/{id}` | 删除角色 |
| GET | `/api/roles/{id}/menus` | 获取角色菜单ID列表 |
| PUT | `/api/roles/{id}/menus` | 分配角色菜单 |

### 2.4 菜单管理 (`/api/menus`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/menus/tree` | 获取菜单树 |
| GET | `/api/menus` | 获取菜单列表 |
| GET | `/api/menus/{id}` | 获取菜单详情 |
| POST | `/api/menus` | 创建菜单 |
| PUT | `/api/menus/{id}` | 更新菜单 |
| DELETE | `/api/menus/{id}` | 删除菜单 |

### 2.5 模板管理 (`/api/v1/template-configs`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/template-configs` | 分页查询模板 |
| GET | `/api/v1/template-configs/{id}` | 获取模板详情 |
| POST | `/api/v1/template-configs` | 创建模板 |
| PUT | `/api/v1/template-configs/{id}` | 更新模板 |
| DELETE | `/api/v1/template-configs/{id}` | 删除模板 |
| POST | `/api/v1/template-configs/{id}/set-default` | 设为默认模板 |
| PUT | `/api/v1/template-configs/{id}/status` | 设置模板状态 |

### 2.6 知识库管理 (`/api/v1/knowledge-configs`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/knowledge-configs` | 分页查询知识文档 |
| GET | `/api/v1/knowledge-configs/{id}` | 获取详情 |
| POST | `/api/v1/knowledge-configs` | 创建知识文档 |
| DELETE | `/api/v1/knowledge-configs/{id}` | 删除知识文档 |

### 2.7 AI模型配置 (`/api/v1/model-configs`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/model-configs` | 分页查询模型配置 |
| GET | `/api/v1/model-configs/active-list` | 获取启用的模型列表 |
| GET | `/api/v1/model-configs/{id}` | 获取详情 |
| POST | `/api/v1/model-configs` | 创建模型配置 |
| PUT | `/api/v1/model-configs/{id}` | 更新模型配置 |
| DELETE | `/api/v1/model-configs/{id}` | 删除模型配置 |
| DELETE | `/api/v1/model-configs/batch` | 批量删除 |
| PUT | `/api/v1/model-configs/{id}/active` | 启用/禁用 |

### 2.8 模型路由管理 (`/api/v1/model-routes`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/model-routes` | 分页查询路由规则 |
| GET | `/api/v1/model-routes/active` | 获取启用的路由规则 |
| POST | `/api/v1/model-routes` | 创建路由规则 |
| PUT | `/api/v1/model-routes/{id}` | 更新路由规则 |
| DELETE | `/api/v1/model-routes/{id}` | 删除路由规则 |
| PUT | `/api/v1/model-routes/{id}/active` | 启用/禁用 |

### 2.9 系统参数 (`/api/v1/sys-params`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/sys-params` | 按分组查询参数列表 |
| GET | `/api/v1/sys-params/value` | 按key查询参数值 |
| PUT | `/api/v1/sys-params` | 批量更新参数 |
| PUT | `/api/v1/sys-params/{paramKey}` | 按key更新参数 |

### 2.10 政策文件管理 (`/api/v1/policy-files`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/policy-files` | 分页查询平台政策文件 |
| GET | `/api/v1/policy-files/{id}` | 获取详情 |
| POST | `/api/v1/policy-files` | 上传政策文件 |
| PUT | `/api/v1/policy-files/{id}` | 更新政策文件 |
| DELETE | `/api/v1/policy-files/{id}` | 删除政策文件 |
| PUT | `/api/v1/policy-files/{id}/status` | 启用/禁用 |

> 平台政策文件(`sup_policy_file`)与用户政策文件(`tb_policy_file`)是独立的两张表，各自管理。

### 2.11 消息中心 (`/api/v1/messages`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/messages` | 分页查询消息 |
| GET | `/api/v1/messages/unread-count` | 获取未读数量 |
| PUT | `/api/v1/messages/{id}/read` | 标记已读 |
| PUT | `/api/v1/messages/read-all` | 全部标记已读 |
| DELETE | `/api/v1/messages/{id}` | 删除消息 |

### 2.12 统计分析 (`/api/v1/statistics`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/statistics/overview` | 获取统计概览 |

### 2.13 访问日志 (`/api/access-logs`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/access-logs` | 分页查询访问日志 |

### 2.14 操作日志 (`/api/v1/operation-logs`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/operation-logs` | 分页查询操作日志 |

### 2.15 版本管理 (`/api/versions`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/versions` | 分页查询主版本 |
| GET | `/api/versions/{id}` | 获取主版本详情 |
| POST | `/api/versions` | 创建主版本 |
| PUT | `/api/versions/{id}` | 更新主版本 |
| DELETE | `/api/versions/{id}` | 删除主版本 |
| POST | `/api/versions/{id}/publish` | 发布版本 |
| POST | `/api/versions/{id}/deprecate` | 废弃版本 |
| GET | `/api/versions/{versionId}/plugins` | 获取插件列表 |
| POST | `/api/versions/{versionId}/plugins` | 创建插件 |
| PUT | `/api/versions/plugins/{id}` | 更新插件 |
| DELETE | `/api/versions/plugins/{id}` | 删除插件 |

### 2.16 外部系统管理 (`/api/external-systems`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/external-systems` | 分页查询接入系统 |
| GET | `/api/external-systems/{id}` | 获取详情 |
| POST | `/api/external-systems` | 创建接入系统 |
| PUT | `/api/external-systems/{id}` | 更新接入系统 |
| DELETE | `/api/external-systems/{id}` | 删除接入系统 |
| POST | `/api/external-systems/{id}/regenerate-secret` | 重新生成密钥 |
| PUT | `/api/external-systems/{id}/status` | 启用/禁用 |

### 2.17 外部系统认证 (`/api/external`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/external/token` | 外部系统获取token |
| GET | `/api/external/verify` | 验证token有效性 |
| GET | `/api/external/userinfo` | 获取外部用户信息 |

## 3. 当前关键表

### 支撑中心表（`sup_*`）

| 表名 | 实体类 | 用途 |
|------|--------|------|
| `sup_user` | SysUser | 用户账号，`username` / `password` / `real_name` / `phone` / `email` / `status` / `last_login_time` |
| `sup_role` | SysRole | 角色定义，`role_code` / `role_name` |
| `sup_user_role` | SysUserRole | 用户角色关联 |
| `sup_menu` | SysMenu | 菜单树，`menu_type` / `menu_url` / `permission` / `icon` / `parent_id` / `sort_order` / `status` |
| `sup_role_menu` | SysRoleMenu | 角色菜单关联 |
| `sup_access_system` | SysAccessSystem | 外部接入系统，`app_key` / `app_secret` / `expire_time` / `status` |
| `sup_main_version` | SysMainVersion | 主版本 |
| `sup_plugin_version` | SysPluginVersion | 插件版本 |
| `sup_operation_log` | SysOperationLog | 操作日志（AOP切面写入） |
| `sup_access_log` | SysAccessLog | HTTP访问日志 |
| `sup_sys_parameter` | SysParameter | 系统参数，`param_group` / `param_key` / `param_value` / `param_type` / `param_name` / `description` / `sort_order` |
| `sup_policy_file` | SupPolicyFile | 平台政策文件，`file_name` / `file_category` / `applicable_category` / `file_id` / `file_size` / `file_type` / `description` / `status` |
| `sup_message` | SupMessage | 消息通知，`user_id` / `title` / `content` / `message_type` / `biz_id` / `biz_type` / `is_read` / `read_time` |
| `sup_sms_code` | SupSmsCode | 短信验证码 |
| `sup_template` | SupTemplate | 招标文件模板，`template_name` / `project_category` / `project_type` / `file_id` / `content` / `structure_definition`(JSON) / `version_no` / `is_default` / `status` |
| `sup_model_config` | SupModelConfig | AI模型配置，`model_name` / `model_type` / `provider` / `api_endpoint` / `api_key` / `model_params`(JSON) / `usage_scenario` / `is_active` / `token_usage` / `cost` |
| `sup_model_route_rule` | SupModelRouteRule | 模型路由规则，`usage_scenario` / `primary_model_id` / `fallback_model_id` / `priority` / `is_active` |

> 支撑中心还跨模块引用了 `TbProject`(tb_project) 和 `TbRequirement`(tb_requirement) 用于统计分析。

所有表继承 `BaseEntity` 基础字段（`create_time`、`modify_time`、`ver`、`is_delete` 等）。

## 4. 核心约束

- 认证双轨：内部用户 JWT (`type=INTERNAL`) / 外部系统 JWT (`type=EXTERNAL`)
- external token 统一由本模块签发
- 外部系统签名使用 `HMAC-SHA256`
- 权限接口统一使用 `@RequirePermission`
- 访问/操作日志查询后端接口统一归属本模块

JWT 约束、字符集约束与通用安全规范见 [PROJECT_SPEC_FINAL.md](PROJECT_SPEC_FINAL.md)。

## 5. 管理边界

- `ele-ai-tender-file` 不提供管理页面逻辑
- 接入系统配置以 `sup_access_system` 为唯一来源
- 模板管理(`sup_template`)归支撑中心，core模块只读取使用
- 知识库配置(`ai_knowledge_document`)管理端在支撑中心，AI能力在ai模块
- 模型配置(`sup_model_config`)管理端在支撑中心，运行时路由在ai模块
- 政策文件分两张表：平台级(`sup_policy_file`)归支撑中心管理，用户级(`tb_policy_file`)归core模块管理

## 6. 排障原则

- **登录失败** → 查 `sup_user.status` 是否启用，检查 Redis `token:{userId}` 是否存在
- **外部认证签名失败** → 查 `sup_access_system.app_secret` 是否最新，检查时间戳偏差
- **外部系统token无效** → 查 Redis `external_token:{appKey}:{userId}` 是否过期
- **模型调用失败** → 查 `sup_model_config` 配置是否正确，检查 `sup_model_route_rule` 路由规则
- **模板不可用** → 查 `sup_template.status` 是否为 ENABLED
- **短信发送失败** → 查 `sup_sms_code` 是否频繁发送触发限流

## 相关规范

- [CODE_CONVENTIONS.md](CODE_CONVENTIONS.md) — 通用编码规范
- [PROJECT_SPEC_FINAL.md](PROJECT_SPEC_FINAL.md) — 全局模块边界与 JWT 约束
- [CORE_MODULE_SPEC.md](CORE_MODULE_SPEC.md) — 核心业务模块规范
- [AI_MODULE_SPEC.md](AI_MODULE_SPEC.md) — AI服务模块规范
