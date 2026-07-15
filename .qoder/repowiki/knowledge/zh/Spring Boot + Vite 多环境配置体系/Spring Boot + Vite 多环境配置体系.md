---
kind: configuration_system
name: Spring Boot + Vite 多环境配置体系
slug: configuration_system
category: configuration_system
scope:
    - '**'
---

## 系统概览

本仓库采用 **Spring Boot 3 + Vite** 的双端配置体系，后端基于 Spring Boot 的 Profile 机制与 `@ConfigurationProperties` 类型安全绑定，前端通过 `.env*` 文件配合 Vite 的 `import.meta.env` 注入。

## 后端配置（Spring Boot）

### 配置文件组织
每个子模块（core、ai、file、support）在 `src/main/resources/` 下提供：
- `application.yml` — 基础配置，统一使用 `${ENV_VAR:default}` 占位符覆盖数据库、Redis、JWT、AI 模型等敏感参数
- `application-dev.yml` / `application-test.yml` / `application-wuyx.yml` — 按环境拆分的具体实现
- 支持通过 `spring.config.import=optional:file:${user.home}/.ele-ai-tender/<module>-local.yml` 引入用户级本地覆盖文件，实现"共享默认 + 个人覆盖"的分层模式

### 配置属性绑定
- 使用 `@ConfigurationProperties(prefix = "...")` 将 YAML 节点映射为强类型 Java Bean，如 `InternalFileServiceProperties`、`SmsGatewayProperties`、`EleAiTenderInteractionProperties`、`AccessLogProperties`、`CryptoAdminProperties`、`FileStorageConfig` 等
- 对 JWT 密钥与过期时间，各服务通过构造器注入 `@Value("${jwt.secret:...}")` 并调用 `JwtUtil.configure()` 完成全局初始化，确保签发方与校验方一致
- 交互模块通过 `@EnableConfigurationProperties` 或自动装配暴露 `ele-ai-tender.interaction.*` 前缀的配置项

### 关键配置域
| 前缀 | 说明 |
|------|------|
| `spring.datasource.*` | MySQL 连接 |
| `spring.data.redis.*` | Redis 连接 |
| `spring.ai.openai.*` | DeepSeek/OpenAI 兼容接口 |
| `milvus.*` | 向量库地址 |
| `jwt.*` | JWT 密钥与过期策略 |
| `internal.file-service.*` | 内部文件服务客户端 |
| `file.storage.*` | 本地文件存储路径与大小限制 |
| `msg.*` | 短信网关凭据 |
| `ele-ai-tender.logging.access.*` | 访问日志开关与阈值 |
| `ele-ai-tender.interaction.*` | 电子标对接 API 基址、超时、路径 |
| `crypto-admin.*` | 加解密任务 Stream Key |

## 前端配置（Vite）

两个前端应用（业务前端 `ele-ai-tender-frontend`、支撑管理前端 `ele-ai-tender-support-frontend`）遵循相同约定：
- `.env.development` / `.env.production` / `.env.test` / `.env.localdev` 定义 `VITE_*` 变量
- `vite.config.ts` 中读取 `env.VITE_CORE_API_URL`、`VITE_AI_API_URL`、`VITE_FILE_API_URL`、`VITE_SUPPORT_API_URL` 作为代理目标
- 路由 `createWebHistory(import.meta.env.BASE_URL)` 支持部署到子路径

## 设计决策与约束

1. **环境变量优先**：所有敏感信息（数据库密码、API Key、JWT Secret）一律通过环境变量注入，禁止硬编码
2. **Profile 分层**：`application.yml` 仅声明占位符与默认值，具体 IP/端口放入 `application-{profile}.yml`
3. **类型安全**：新增外部依赖配置时，必须新建 `@ConfigurationProperties` 类而非散落的 `@Value`
4. **跨服务一致性**：JWT 密钥通过统一的 `jwt.secret` 键在各服务间共享，避免签名不一致
5. **可插拔性**：交互模块通过独立 Properties 类暴露完整 API 基址集合，便于嵌入第三方平台时只覆盖必要字段

## 开发者规则

- 新增配置项 → 先写 `@ConfigurationProperties` 类，再在对应环境的 `application-*.yml` 中补充
- 敏感配置 → 使用 `${VAR:default}` 形式，默认值仅用于本地开发，生产环境必须显式注入
- 修改 JWT 相关配置 → 需同步更新所有服务的 `JwtRuntimeConfig` 及 `AuthServiceImpl` 中的 `@Value` 引用
- 前端新增 API 域名 → 在 `vite.config.ts` 中添加 `VITE_*_API_URL` 并在所有 `.env.*` 文件中保持一致
