# AI编制系统项目规范

## 1. 定位

本文档是项目级全局约束，覆盖模块清单、技术基线、端口、安全、日志和文档治理规则。

**编码细节**（命名、分层、数据库字段、接口格式、异常处理等）见 [CODE_CONVENTIONS.md](CODE_CONVENTIONS.md)。

裁定顺序：

1. 当前代码实现
2. `docs/rules/*`
3. `docs/projects/*` 和历史设计讨论

## 2. 当前模块

| 模块 | 类型 | 端口 | 详细规范 |
|------|------|------|----------|
| `ele-ai-tender-support-frontend` | 前端 | 5174（开发） | [AI_TENDER_SYSTEM_SPEC.md](AI_TENDER_SYSTEM_SPEC.md) |
| `ele-ai-tender-frontend` | 前端 | 5173（开发） | [AI_TENDER_SYSTEM_SPEC.md](AI_TENDER_SYSTEM_SPEC.md) |
| `ele-ai-tender-common` | 公共库 | — | — |
| `ele-ai-tender-common-interaction` | 协议库 | — | [INTERACTION_INTEGRATION_SPEC.md](INTERACTION_INTEGRATION_SPEC.md) |
| `ele-ai-tender-support` | 后端服务 | 8080 | [SUPPORT_SYSTEM_SPEC.md](SUPPORT_SYSTEM_SPEC.md) |
| `ele-ai-tender-file` | 后端服务 | 8081 | [FILE_SERVICE_SPEC.md](FILE_SERVICE_SPEC.md) |
| `ele-ai-tender-core` | 后端服务 | 8082 | [AI_TENDER_SYSTEM_SPEC.md](AI_TENDER_SYSTEM_SPEC.md) |
| `ele-ai-tender-ai` | 后端服务 | 8083 | [AI_TENDER_SYSTEM_SPEC.md](AI_TENDER_SYSTEM_SPEC.md) |
| `ele-ai-tender-interaction` | Starter | — | [INTERACTION_INTEGRATION_SPEC.md](INTERACTION_INTEGRATION_SPEC.md) |

## 3. 技术基线

- Java：JDK 21（`ele-ai-tender-interaction` / `ele-ai-tender-common-interaction` 对外 API 保持 JDK 8 兼容）
- Spring Boot：3.2.2
- Spring AI：0.8.1
- MyBatis-Plus：3.5.5
- MySQL：8.4.0
- Redis：会话存储、AI任务缓存、Token 限流
- Milvus：2.3.3（向量数据库）
- 前端：Vue 3 + TypeScript + Vite + Pinia + Vue Router

## 4. 模块边界

- 非交互公共类型 → `ele-ai-tender-common`
- 交互协议 DTO、SPI、路径常量 → `ele-ai-tender-common-interaction`（单源，禁止复制）
- controller / facade 边界通过 mapper 显式转换，service 层不直接透传协议 DTO
- 协议 DTO 不放 `jakarta/javax.validation` 注解
- core模块负责业务编排，ai模块负责AI能力提供，通过HTTP调用通信

## 5. 表前缀

| 模块 | 前缀 | 说明 |
|------|------|------|
| 支撑中心 | `sup_*` | 用户、角色、菜单、日志等 |
| 文件服务 | `file_*` | 文件信息 |
| AI编制系统 | `ai_*` | 项目、需求、模板、知识库、检测记录、评审项、模型配置 |

## 6. 关键接口清单

### 支撑中心（`/api`）
- `/auth/*`、`/users/*`、`/roles/*`、`/menus/*`
- `/templates/*`、`/knowledge/*`、`/statistics/*`
- `/ai-config/*`、`/access-logs`、`/messages/*`

### 文件服务（`/api/file`）
- `POST /upload`、`GET /download/{fileId}`
- `GET /info/{fileId}`、`DELETE /delete/{fileId}`

### AI编制核心（`/api/v1`）
- `/projects/*` — 项目管理
- `/requirements/*` — 业务需求
- `/review-items/*` — 评审项
- `/templates/*` — 模板管理

### AI服务（`/api/v1`）
- `/ai/*` — AI助手、文本优化、生成
- `/detection/*` — 智能检测
- `/knowledge/*` — 知识库检索

### 交互固定路径（`/api/eleAiTender/interaction`）
- `/identity/current`
- `/projects/basic-info`
- `/callbacks/document-export`
- `/callbacks/detection-result`

## 7. 安全与 JWT

- 认证采用 JWT + Redis 双校验
- `ele-ai-tender-support` 负责 external token 签发
- 四个后端服务共享同一套 JWT 密钥与过期策略
- 所有使用 `JwtAuthenticationFilter` 的服务启动时必须执行 `JwtUtil.configure(...)`
- 外部系统签名统一使用 `HMAC-SHA256`
- 安全配置缺失时必须抛异常，禁止静默降级

详见 [CODE_CONVENTIONS.md — 安全规范](CODE_CONVENTIONS.md#5-安全规范)。

## 8. 日志约束

- 全系统统一透传 `X-Trace-Id`，MDC 键为 `traceId`
- HTTP 入站日志优先落库到 `sup_access_log`
- 新增链路时尽量记录 `bizType`、`bizId`、`projectId`、`fileId`
- 禁止记录完整 token、签名 secret、文件二进制内容和 AI 原始响应

## 9. 前后端联调

| 前端路径前缀 | 目标 |
|-------------|------|
| `/support-api/*` | support :8080（rewrite → `/api/*`） |
| `/file-api/*` | file :8081 |
| `/core-api/*` | core :8082（rewrite → `/api/*`） |
| `/ai-api/*` | ai :8083（rewrite → `/api/*`） |

修改接口路径、参数位置或代理规则时，必须同步前端 API 文件和相关文档。

## 10. 文档治理

| 目录 | 用途 |
|------|------|
| `docs/rules/` | 权威规范（本目录） |
| `docs/guides/` | 接入指南、接口文档、工具说明 |
| `docs/projects/` | 需求整理、问题分析 |
| `docs/plans/` | 实施计划 |

代码实现优先于文档；文档过期时先对齐代码再修文档。
