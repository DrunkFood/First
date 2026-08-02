# 交互集成规范

## 1. 定位

本文档描述 `ele-ai-tender-common-interaction` 与 `ele-ai-tender-interaction` 的对外接入规范。

交互模块为外部业务系统提供统一的 Starter POM 接入能力，包括：
- **出站客户端**：认证、AI任务创建/查询、文件操作
- **入站回调**：AI任务结果推送接收

## 2. 模块组成

| 子模块 | 职责 |
|--------|------|
| `ele-ai-tender-common-interaction` | 交互协议 DTO、SPI 接口、枚举、工具类（JDK 8 兼容） |
| `ele-ai-tender-interaction-core` | 出站客户端 + RestTemplate + 签名器 |
| `ele-ai-tender-interaction-autoconfigure` | 自动装配 + 拦截器 + 全局异常处理 + 回调Controller |
| `ele-ai-tender-interaction-spring-boot-starter` | Starter 聚合 POM |

## 3. 兼容性

- 公开 API 必须保持 JDK 8 兼容
- 推荐运行环境：Spring Boot 2.7.x + Spring MVC
- 同时兼容 Spring Boot 2.x（`spring.factories`）和 3.x（`AutoConfiguration.imports`）

## 4. 固定路径

统一前缀：`/api/eleAiTender/interaction`

当前固定接口（仅回调端点）：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/eleAiTender/interaction/callbacks/ai-task-result` | AI任务结果回调（需实现 SPI） |

路径常量统一维护在 `InteractionApiPaths`（`common-interaction/constant/`）。

## 5. SPI 接口

外部系统只需实现以下 SPI 即可接收 AI 任务结果推送：

| SPI 接口 | 方法 | 说明 |
|----------|------|------|
| `InteractionAiTaskResultReceiveService` | `receive(AiTaskResultCallbackRequest)` | AI任务进入终态后接收结果推送 |
| `InteractionEventLogger` | （可选覆盖） | 交互事件日志，默认实现写 SLF4J |

**自动装配机制**：`InteractionAiTaskResultCallbackController` 使用 `@ConditionalOnBean(InteractionAiTaskResultReceiveService.class)`，实现 SPI 并注册为 Bean 后，回调端点自动创建。

## 6. 出站客户端能力

自动装配注册以下客户端 Bean（均支持同名 Bean 覆盖）：

| Bean | 类型 | 能力 |
|------|------|------|
| `aiExternalAuthClient` | `AiExternalAuthClient` | 用 appKey/appSecret 签名换取 JWT Token |
| `aiExternalUserInfoClient` | `AiExternalUserInfoClient` | 查询当前外部用户信息 |
| `aiTaskClient` | `AiTaskClient` | 创建/查询 AI 任务 |
| `aiFileClient` | `AiFileClient` | 查询/下载/上传文件 |
| `interactionAiRestTemplate` | `RestTemplate` | 专用 HTTP 客户端（含超时 + 日志拦截） |
| `interactionAiRequestSigner` | `InteractionRequestSigner` | 出站请求签名注入器（X-App-Key/X-Timestamp/X-Signature） |
| `interactionAiSignatureInterceptor` | `InteractionAiSignatureInterceptor` | 入站回调签名校验拦截器 |

## 7. 配置项

统一前缀：`ele-ai-tender.interaction`

| 配置项 | 必填 | 说明 |
|--------|------|------|
| `app-key` | 是 | 平台分配的应用Key |
| `app-secret` | 是 | 平台分配的应用密钥 |
| `api-base-url` | 是 | 支持服务URL |
| `core-base-url` | 是 | 核心服务URL |
| `file-base-url` | 是 | 文件服务URL |
| `enabled` | 否 | 总开关（默认true） |
| `token-path` | 否 | 认证路径（默认 `/api/external/token`） |
| `user-info-path` | 否 | 用户信息路径（默认 `/api/external/userinfo`） |
| `connect-timeout` | 否 | HTTP连接超时（默认 5s） |
| `read-timeout` | 否 | HTTP读取超时（默认 20s） |
| `controller.base-path` | 否 | 回调Controller路径前缀（默认 `/api/eleAiTender/interaction`） |

## 8. 模型约束

- 协议 DTO 单源维护于 `ele-ai-tender-common-interaction`
- controller/facade 边界做显式 mapper 转换
- `AiTaskCreateRequest` / `AiTaskQueryResponse` / `ExternalTokenRequest` / `ExternalTokenResponse` 等 DTO 集中在 `common-interaction/dto/` 包

协议 DTO 通用约束（validation 注解限制、service 层不透传协议 DTO 等）见 [CODE_CONVENTIONS.md — DTO/类型归属](CODE_CONVENTIONS.md#34-dto--类型归属)。

## 9. 外部 AI 任务入口

除 Starter 客户端调用外，core 模块还提供 `ExternalAiTaskController`（`/api/external/ai-tasks`）供外部系统通过 JWT 直接创建/查询 AI 任务：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/external/ai-tasks` | 创建AI任务（需 @RequireLogin） |
| GET | `/api/external/ai-tasks/{taskId}` | 查询AI任务详情（需 @RequireLogin） |

详见 [CORE_MODULE_SPEC.md](CORE_MODULE_SPEC.md) 2.12。

## 相关规范

- [CODE_CONVENTIONS.md](CODE_CONVENTIONS.md) — DTO 归属单源原则、分层约束
- [PROJECT_SPEC_FINAL.md](PROJECT_SPEC_FINAL.md) — 全局模块边界
- [CORE_MODULE_SPEC.md](CORE_MODULE_SPEC.md) — 外部AI任务入口
