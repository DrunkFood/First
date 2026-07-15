---
kind: configuration_system
name: Spring Boot 多环境配置与前端 Vite 环境变量体系
category: configuration_system
scope:
    - '**'
source_files:
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application-dev.yml
    - ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/config/JwtRuntimeConfig.java
    - ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/config/CallbackRestTemplateConfig.java
    - ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/client/InternalFileServiceProperties.java
    - ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/config/FileStorageConfig.java
    - ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-support/src/main/resources/application.yml
    - ele-ai-tender-frontend/.env.development
    - ele-ai-tender-frontend/.env.production
    - ele-ai-tender-support-frontend/.env.development
    - ele-ai-tender-support-frontend/.env.production
---

## 系统概述

本仓库采用 Spring Boot 原生配置机制，结合 `spring.config.import` 实现本地覆盖与环境隔离；前端使用 Vite 的 `.env.*` 文件管理构建期环境变量。整体遵循「默认值 + 环境变量 + 环境配置文件 + 用户目录本地覆盖」四层加载顺序。

## 后端配置体系（ele-ai-tender-system）

### 1. 配置文件分层结构
每个独立服务模块均遵循统一的分层：
- `application.yml` — 公共默认配置，所有运行时敏感项通过 `${ENV_VAR:default}` 占位符注入
- `application-dev.yml` / `application-test.yml` / `application-wuyx.yml` — 按环境覆盖数据库、Redis、日志级别等差异配置
- `optional:file:${user.home}/.ele-ai-tender/{module}-local.yml` — 通过 `spring.config.import` 引入用户家目录下本地覆盖文件，用于存放个人/CI 机密信息，不入库

四个核心服务（core、ai、file、support）均采用此模式，端口默认分别为 8082/8083/8081/8080，均可被 `SERVER_PORT` 环境变量覆盖。

### 2. 配置属性绑定方式
- **@Value**：简单标量配置，如 `CallbackRestTemplateConfig` 中回调超时、`JwtRuntimeConfig` 中 JWT 密钥与过期时间
- **@ConfigurationProperties(prefix = "...")**：结构化配置，典型包括：
  - `InternalFileServiceProperties`（`internal.file-service.*`）— 内部文件服务客户端 URL、路径、JWT 密钥与 Token 过期
  - `FileStorageConfig`（`file.storage.*`）— 文件存储根路径、允许类型、最大大小
  - `AccessLogProperties`（`ele-ai-tender.logging.access.*`）— 访问日志开关与阈值
- **Spring AI 原生前缀**：AI 模块直接使用 `spring.ai.openai.*` 注入 DeepSeek API Key 与 Base URL

### 3. 关键配置域
| 配置域 | 说明 | 示例键 |
|--------|------|--------|
| 数据库 | MySQL 连接串、用户名、密码 | `SPRING_DATASOURCE_URL` |
| Redis | host/port/password/database/timeout | `SPRING_DATA_REDIS_HOST` |
| JWT | secret/expiration/external-expiration | `APP_JWT_SECRET` |
| 内部服务 | 文件服务 base-url、JWT 密钥 | `INTERNAL_FILE_SERVICE_URL` |
| AI 模型 | OpenAI 兼容 API key/base-url/model | `DEEPSEEK_API_KEY` |
| Milvus | 向量库 host/port/database | `MILVUS_HOST` |
| 短信网关 | username/pwd/url/isformal | `MSG_USERNAME` |
| 文件存储 | base-path/allowed-types/max-size | `FILE_STORAGE_BASE_PATH` |
| 回调 RestTemplate | connect/read timeout | `ele-ai-tender.external.ai-task.callback-*` |

### 4. 启动时初始化
`JwtRuntimeConfig` 在构造阶段调用 `JwtUtil.configure(...)` 将配置写入静态工具类，确保同一 JVM 内签发与校验使用一致密钥与时序。

## 前端配置体系（双前端并列）

### 1. Vite 环境变量
两个前端项目（`ele-ai-tender-frontend`、`ele-ai-tender-support-frontend`）均使用 `.env.development` / `.env.production` 定义 `VITE_*` 变量，由 Vite 在构建期注入到 `import.meta.env`。

- 开发环境：指向本机各后端服务的完整 HTTP 地址
- 生产环境：统一为相对路径 `/core-api`、`/ai-api`、`/file-api`、`/support-api`，由反向代理或 Nginx 转发

### 2. 代理与路由
- `vite.config.ts` 中根据 `VITE_*_API_URL` 动态设置 dev server proxy target
- 支持中心前端额外暴露 `VITE_APP_TITLE` 用于页面标题

## 约定与规则

1. **敏感配置一律走环境变量**：数据库密码、JWT 密钥、API Key 等不得硬编码进 `application*.yml`，必须通过 `${VAR:default}` 形式注入
2. **本地覆盖文件不入版本库**：`${user.home}/.ele-ai-tender/*.yml` 仅存在于开发者机器与 CI 容器，用于存放真实凭据
3. **环境文件只放非敏感差异**：`application-dev.yml` 仅包含数据库/Redis 地址、日志级别等非机密差异
4. **新增配置优先使用 @ConfigurationProperties**：结构化配置便于 IDE 提示与校验，避免散落的 `@Value`
5. **前后端 API 基址统一通过 VITE_* 变量管理**：禁止在业务代码中硬编码后端地址

## 关键文件清单

- `ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application.yml`
- `ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application-dev.yml`
- `ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/config/JwtRuntimeConfig.java`
- `ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/config/CallbackRestTemplateConfig.java`
- `ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/client/InternalFileServiceProperties.java`
- `ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/config/FileStorageConfig.java`
- `ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application.yml`
- `ele-ai-tender-system/ele-ai-tender-support/src/main/resources/application.yml`
- `ele-ai-tender-frontend/.env.development`
- `ele-ai-tender-frontend/.env.production`
- `ele-ai-tender-support-frontend/.env.development`
- `ele-ai-tender-support-frontend/.env.production`
