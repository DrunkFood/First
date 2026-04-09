# EleTender 项目规范

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
| `ele-tender-support-frontend` | 前端 | 3000（开发） | [SUPPORT_SYSTEM_SPEC.md](SUPPORT_SYSTEM_SPEC.md) |
| `ele-tender-common` | 公共库 | — | — |
| `ele-tender-common-interaction` | 协议库 | — | [INTERACTION_INTEGRATION_SPEC.md](INTERACTION_INTEGRATION_SPEC.md) |
| `ele-tender-support` | 后端服务 | 8080 | [SUPPORT_SYSTEM_SPEC.md](SUPPORT_SYSTEM_SPEC.md) |
| `ele-tender-file` | 后端服务 | 8081 | [FILE_SERVICE_SPEC.md](FILE_SERVICE_SPEC.md) |
| `ele-tender-tender-document` | 后端服务 | 8082 | [TENDER_DOCUMENT_PROJECT_SPEC.md](TENDER_DOCUMENT_PROJECT_SPEC.md) |
| `ele-tender-crypto` | 后端服务 | 8083 | [ELE_TENDER_CRYPTO_SPEC.md](ELE_TENDER_CRYPTO_SPEC.md) |
| `ele-tender-interaction` | Starter | — | [INTERACTION_INTEGRATION_SPEC.md](INTERACTION_INTEGRATION_SPEC.md) |

## 3. 技术基线

- Java：JDK 21（`ele-tender-interaction` / `ele-tender-common-interaction` 对外 API 保持 JDK 8 兼容）
- Spring Boot：3.2.2
- MyBatis-Plus：3.5.5
- MySQL：8.4.0
- Redis：会话存储、口令缓存、Stream 任务队列
- 前端：Vue 3 + TypeScript + Vite + Pinia + Vue Router

## 4. 模块边界

- 非交互公共类型 → `ele-tender-common`
- 交互协议 DTO、SPI、路径常量 → `ele-tender-common-interaction`（单源，禁止复制）
- controller / facade 边界通过 mapper 显式转换，service 层不直接透传协议 DTO
- 协议 DTO 不放 `jakarta/javax.validation` 注解

## 5. 表前缀

| 模块 | 前缀 |
|------|------|
| 支撑中心 | `sup_*` |
| 文件服务 | `file_*` |
| 招标文件编制 | `td_*` |
| 投标文件加解密 | `bdc_*` |

## 6. 关键接口清单

### 支撑中心（`/api`）
- `/auth/*`、`/users/*`、`/roles/*`、`/menus/*`
- `/versions/*`、`/external-systems/*`、`/external/*`
- `/access-logs`、`/crypto/manage/*`

### 文件服务（`/api/file`）
- `POST /upload`、`POST /esign/upload`
- `GET /download/{fileId}`、`GET /info/{fileId}`、`DELETE /delete/{fileId}`

### 招标文件编制（`/api/tender-documents`）
- `/entry`、`/overview`、`/recompile`
- `/basic-info`、`/purchase-file`、`/purchase-file/signed`
- `/bid-record`、`/evaluation-rules`、`/evaluation-rules/score-type`
- `/check-items`、`/complete-and-next`
- `/generate`、`/generate/records`、`/generate/callback`

### 加解密服务（`/api/crypto`）
- `POST /bid-document/push`
- `POST /bid-decrypt/submit`
- `GET /bid-decrypt/status/{recordId}`

### 交互固定路径（`/api/eleTender/interaction`）
- `/identity/current`
- `/projects/basic-info`
- `/bid-record-schemes/query`
- `/ca-keys/query`
- `/callbacks/tender-pdf`
- `/callbacks/tender-package`
- `/callbacks/bid-document-result`
- `/callbacks/bid-decrypt-result`

## 7. 安全与 JWT

- 认证采用 JWT + Redis 双校验
- `ele-tender-support` 负责 external token 签发
- 四个后端服务共享同一套 JWT 密钥与过期策略
- 所有使用 `JwtAuthenticationFilter` 的服务启动时必须执行 `JwtUtil.configure(...)`
- 外部系统签名统一使用 `HMAC-SHA256`
- 安全配置缺失时必须抛异常，禁止静默降级

详见 [CODE_CONVENTIONS.md — 安全规范](CODE_CONVENTIONS.md#5-安全规范)

## 8. 日志约束

- 全系统统一透传 `X-Trace-Id`，MDC 键为 `traceId`
- HTTP 入站日志优先落库到 `sup_access_log`
- 新增链路时尽量记录 `bizType`、`bizId`、`projectId`、`tenderId`、`fileId`
- 禁止记录完整 token、签名 secret 和文件二进制内容

## 9. 前后端联调

| 前端路径前缀 | 目标 |
|-------------|------|
| `/support-api/*` | support :8080（rewrite → `/api/*`） |
| `/file-api/*` | file :8081 |
| `/file-esign-api/*` | file :8081（Esign 上传） |
| `/crypto-api/*` | crypto :8083（rewrite → `/api/crypto/*`） |

修改接口路径、参数位置或代理规则时，必须同步前端 API 文件和相关文档。

## 10. 文档治理

| 目录 | 用途 |
|------|------|
| `docs/rules/` | 权威规范（本目录） |
| `docs/guides/` | 接入指南、接口文档、工具说明 |
| `docs/projects/` | 需求整理、问题分析 |
| `docs/plans/` | 实施计划 |

代码实现优先于文档；文档过期时先对齐代码再修文档。
