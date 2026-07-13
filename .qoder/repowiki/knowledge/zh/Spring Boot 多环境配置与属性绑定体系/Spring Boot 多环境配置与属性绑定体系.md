---
kind: configuration_system
name: Spring Boot 多环境配置与属性绑定体系
category: configuration_system
scope:
    - '**'
source_files:
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-file/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-support/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application-dev.yml
    - ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/config/FileStorageConfig.java
    - ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/config/SmsGatewayProperties.java
    - ele-ai-tender-frontend/.env.development
    - ele-ai-tender-support-frontend/.env.development
---

## 系统概览

本仓库采用 **Spring Boot 原生配置文件 + 环境变量覆盖** 的配置体系，通过 `application.yml` + `application-{profile}.yml` + 用户目录 local 覆盖文件实现多环境、可插拔的配置加载。前端使用 Vite 的 `.env.*` 文件管理 API 地址等运行时参数。

## 核心机制

### 1. 配置文件分层（由低到高优先级）

- **默认配置**：各模块 `src/main/resources/application.yml`，定义所有键及 `${VAR:default}` 形式的默认值
- **环境覆盖**：`application-dev.yml` / `application-test.yml` / `application-wuyx.yml`，按 Spring Profile 激活
- **本地覆盖**：通过 `spring.config.import: optional:file:${user.home}/.ele-ai-tender/{module}-local.yml` 引入用户家目录下独立文件，用于存放敏感信息（数据库密码、JWT Secret 等），不入库

### 2. 环境变量命名规范

| 类别 | 变量前缀 | 示例 |
|------|----------|------|
| 服务端口 | `SERVER_PORT` | `8082` |
| 数据库连接 | `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | MySQL JDBC URL |
| Redis | `SPRING_DATA_REDIS_HOST/PORT/DATABASE/PASSWORD` | 单机 Redis |
| JWT 密钥 | `APP_JWT_SECRET` | 统一 256-bit 密钥 |
| 内部服务调用 | `INTERNAL_FILE_SERVICE_URL` | 文件服务 base-url |
| AI 模型 | `DEEPSEEK_API_KEY` / `DEEPSEEK_BASE_URL` | OpenAI 兼容接口 |
| Milvus 向量库 | `MILVUS_HOST/PORT/DATABASE` | 语义检索 |
| 短信网关 | `MSG_USERNAME/PWD/URL/ISFORMAL` | 第三方短信平台 |
| 文件存储路径 | `FILE_STORAGE_BASE_PATH` | 本地磁盘根目录 |

### 3. 配置属性绑定方式

- **`@ConfigurationProperties(prefix = "...")` + `@Component`**：如 `FileStorageConfig`（`file.storage.*`）、`SmsGatewayProperties`（`msg.*`）
- **`@EnableConfigurationProperties(...)`**：在配置类中显式启用，如 `SmsGatewayConfig`、`InternalFileServiceClientAutoConfiguration`
- **Spring Boot 内置属性**：`server.*`、`spring.datasource.*`、`spring.data.redis.*`、`mybatis-plus.*`、`jwt.*`、`internal.file-service.*`

### 4. 多环境 Profile 策略

每个后端模块均提供三套 profile：
- `dev`：开发环境，指向共享测试库 `10.11.20.42`
- `test`：测试环境，指向 `10.11.20.50`
- `wuyx`：预生产/灰度环境，复用 test 网络段

Profile 通过 Jenkinsfile 打包时以 `-Dspring.profiles.active=xxx` 注入，或通过 `--spring.profiles.active=xxx` 启动参数覆盖。

### 5. 前端配置（Vite）

双前端各自维护 `.env.development` / `.env.production` / `.env.test`，通过 `VITE_*_API_URL` 常量暴露给业务代码：
- `ele-ai-tender-frontend`：`VITE_CORE_API_URL`、`VITE_AI_API_URL`、`VITE_FILE_API_URL`、`VITE_SUPPORT_API_URL`
- `ele-ai-tender-support-frontend`：`VITE_SUPPORT_API_URL`、`VITE_FILE_API_URL`

## 关键文件

- `ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application.yml` — 核心业务默认配置
- `ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application.yml` — AI 服务默认配置（含 DeepSeek/Milvus）
- `ele-ai-tender-system/ele-ai-tender-file/src/main/resources/application.yml` — 文件服务默认配置（含存储路径）
- `ele-ai-tender-system/ele-ai-tender-support/src/main/resources/application.yml` — 支撑中心默认配置（含短信网关）
- 各模块 `application-{dev,test,wuyx}.yml` — 环境差异覆盖
- `ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/config/FileStorageConfig.java` — 文件存储属性绑定
- `ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/config/SmsGatewayProperties.java` — 短信网关属性绑定
- `ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/client/InternalFileServiceProperties.java` — 内部文件服务客户端属性
- `ele-ai-tender-interaction/.../EleAiTenderInteractionProperties.java` — 交互 SPI 配置属性
- `ele-ai-tender-frontend/.env.development` — 主前端 API 地址
- `ele-ai-tender-support-frontend/.env.development` — 支撑前端 API 地址

## 开发者约定

1. **新增配置项**：优先使用 `${ENV_VAR:default}` 形式在 `application.yml` 声明，避免硬编码；敏感字段必须走环境变量或 local 覆盖文件
2. **属性类**：使用 `@ConfigurationProperties(prefix = "...")` 集中管理，配合 `@Data` 简化 getter/setter
3. **Profile 隔离**：不同环境的差异化配置放入对应 `application-{profile}.yml`，不要混入默认配置
4. **本地调试**：将个人敏感配置放在 `${user.home}/.ele-ai-tender/{module}-local.yml`，该文件已在 `.gitignore` 中排除
5. **前端 API 地址**：通过 `VITE_*_API_URL` 注入，禁止在源码中写死后端地址
