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
| `ele-ai-tender-support-frontend` | 前端 | 3060（开发） | [SUPPORT_SYSTEM_SPEC.md](SUPPORT_SYSTEM_SPEC.md) |
| `ele-ai-tender-frontend` | 前端 | 5173（开发） | [CORE_MODULE_SPEC.md](CORE_MODULE_SPEC.md) |
| `ele-ai-tender-common` | 公共库 | — | — |
| `ele-ai-tender-common-interaction` | 协议库 | — | [INTERACTION_INTEGRATION_SPEC.md](INTERACTION_INTEGRATION_SPEC.md) |
| `ele-ai-tender-support` | 后端服务 | 8080 | [SUPPORT_SYSTEM_SPEC.md](SUPPORT_SYSTEM_SPEC.md) |
| `ele-ai-tender-file` | 后端服务 | 8081 | [FILE_SERVICE_SPEC.md](FILE_SERVICE_SPEC.md) |
| `ele-ai-tender-core` | 后端服务 | 8082 | [CORE_MODULE_SPEC.md](CORE_MODULE_SPEC.md) |
| `ele-ai-tender-ai` | 后端服务 | 8083 | [AI_MODULE_SPEC.md](AI_MODULE_SPEC.md) |
| `ele-ai-tender-interaction` | Starter | — | [INTERACTION_INTEGRATION_SPEC.md](INTERACTION_INTEGRATION_SPEC.md) |

## 3. 技术基线

- Java：JDK 21（`ele-ai-tender-interaction` / `ele-ai-tender-common-interaction` 对外 API 保持 JDK 8 兼容）
- Spring Boot：3.2.2
- Spring AI：1.1.0
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
- core模块负责业务编排，ai模块负责AI能力提供，通过 `ai_task` 表异步解耦（core写入任务 → AiTaskProcessor轮询执行 → core读取结果），无直接HTTP调用

## 5. 表前缀

| 模块 | 前缀 | 说明 |
|------|------|------|
| 支撑中心 | `sup_*` | 用户、角色、菜单、日志、模板、模型配置、路由规则、消息、政策文件等 |
| 文件服务 | `file_*` | 文件信息 |
| 核心业务 | `tb_*` | 项目、需求、检测记录、评审项、项目模板快照、用户政策文件 |
| AI服务 | `ai_*` | AI任务、知识库文档、AI响应日志、AI内容反馈 |

## 6. 关键接口清单

各模块完整接口清单见对应 spec 文件：

| 模块 | 接口前缀 | 详细规范 |
|------|----------|----------|
| 支撑中心 | `/api/auth` `/api/users` `/api/roles` `/api/menus` `/api/v1/*` | [SUPPORT_SYSTEM_SPEC.md](SUPPORT_SYSTEM_SPEC.md) |
| 文件服务 | `/api/file/*` | [FILE_SERVICE_SPEC.md](FILE_SERVICE_SPEC.md) |
| 核心业务 | `/api/v1/projects` `/api/v1/requirements` `/api/v1/review-items` `/api/v1/detection` `/api/v1/documents` 等 | [CORE_MODULE_SPEC.md](CORE_MODULE_SPEC.md) |
| AI服务 | `/api/v1/ai` `/api/v1/knowledge` | [AI_MODULE_SPEC.md](AI_MODULE_SPEC.md) |
| 交互集成 | `/api/eleAiTender/interaction` | [INTERACTION_INTEGRATION_SPEC.md](INTERACTION_INTEGRATION_SPEC.md) |

## 7. 安全与 JWT

安全规范详见 [CODE_CONVENTIONS.md — 安全规范](CODE_CONVENTIONS.md)。

## 8. 日志约束

日志与链路追踪规范详见 [CODE_CONVENTIONS.md — 链路追踪规范](CODE_CONVENTIONS.md)。

## 9. 前后端联调

代理规则详见 [FRONTEND_CONVENTIONS.md](FRONTEND_CONVENTIONS.md)。

## 10. 文档治理

| 目录 | 用途 |
|------|------|
| `docs/rules/` | 权威规范（本目录） |
| `docs/guides/` | 接入指南、接口文档、工具说明 |
| `docs/projects/` | 需求整理、问题分析 |
| `docs/plans/` | 实施计划 |

代码实现优先于文档；文档过期时先对齐代码再修文档。
