---
kind: configuration_system
name: Spring Boot 多环境配置与环境变量覆盖体系
category: configuration_system
scope:
    - '**'
source_files:
    - ele-ai-tender-system/pom.xml
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-support/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-file/src/main/resources/application.yml
    - ele-ai-tender-frontend/.env.development
    - ele-ai-tender-frontend/.env.production
    - ele-ai-tender-support-frontend/.env.development
    - ele-ai-tender-support-frontend/.env.production
    - Jenkinsfile-core.groovy
    - Jenkinsfile-ai.groovy
    - Jenkinsfile-support.groovy
    - Jenkinsfile-file.groovy
---

本仓库采用 Spring Boot 标准的多环境 YAML 配置文件 + 环境变量覆盖机制，配合 Maven profile 与 Jenkinsfile 流水线完成不同运行形态（独立部署/嵌入第三方/一体机）的配置切换。

**1. 使用的系统与工具**
- Spring Boot `application.yml` + `application-{profile}.yml` 多环境配置
- Spring Profile 激活：通过 `spring.profiles.active` 或环境变量 `SPRING_PROFILES_ACTIVE` 控制
- 环境变量覆盖：所有 `application.yml` 中的属性均可被同名环境变量覆盖（如 `DB_URL` → `db.url`）
- Maven profile：在聚合工程 `ele-ai-tender-system/pom.xml` 中定义 dev/test/wuyx 等 profile，用于打包时选择默认 profile
- Jenkinsfile 流水线：各模块的 `Jenkinsfile-*.groovy` 通过 `-Dspring.profiles.active=xxx` 或传入环境变量指定运行时 profile

**2. 关键文件与包**
- 后端各子模块 `src/main/resources/application.yml` 及对应 `application-dev.yml`、`application-test.yml`、`application-wuyx.yml`（core/ai/support/file 四个服务均遵循此结构）
- 聚合工程根 `pom.xml` 中 `<profiles>` 段，定义各 profile 的默认激活 profile 与依赖版本
- 各模块 `Jenkinsfile-*.groovy` 中构建参数 `-P${PROFILE}` 与 `SPRING_PROFILES_ACTIVE` 传递逻辑
- 前端 `.env` / `.env.development` / `.env.production` / `.env.test` 等多环境 Vite 环境变量文件（`ele-ai-tender-frontend/.env*`、`ele-ai-tender-support-frontend/.env*`）
- `docs/guides/ele-ai-tender-core.yml` 等示例配置文档，作为外部化配置参考

**3. 架构与约定**
- 每个可部署服务（core/ai/support/file）拥有独立的 `application.yml` + 多环境变体，避免跨服务配置污染
- 敏感信息（数据库密码、第三方 API Key、短信网关密钥等）一律通过环境变量注入，不写入任何 yml 文件
- 非敏感但环境差异大的配置（如 Redis 地址、MinIO 端点、AI 模型 URL）放在 `application-{profile}.yml` 中，由 Maven profile 选择默认值
- 前端使用 Vite 的 `import.meta.env.VITE_*` 读取 `.env.*` 文件，按 `VITE_APP_ENV` 自动加载对应环境配置
- 统一日志配置通过 `logback-spring.xml` + `logging-spring.xml` 引入，支持按 profile 切换日志级别与输出路径

**4. 开发者应遵循的规则**
- 新增配置项优先放入 `application.yml` 并提供默认值，仅在确实需要区分环境时才拆到 `application-{profile}.yml` 中
- 所有密码、Token、私钥类配置必须通过环境变量提供，禁止硬编码或写入版本库
- 修改 Maven profile 后需同步更新对应 `Jenkinsfile-*.groovy` 中的构建参数
- 前端新增环境变量必须以 `VITE_` 前缀命名，并在 `tsconfig.app.json` 的 `types` 中声明类型
- 本地开发推荐通过 IDE Run Configuration 设置 `SPRING_PROFILES_ACTIVE=dev`，或通过 `--spring.profiles.active=dev` 启动参数覆盖