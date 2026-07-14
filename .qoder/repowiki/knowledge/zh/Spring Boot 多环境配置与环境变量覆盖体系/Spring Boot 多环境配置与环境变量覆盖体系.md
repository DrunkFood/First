---
kind: configuration_system
name: Spring Boot 多环境配置与环境变量覆盖体系
category: configuration_system
scope:
    - '**'
source_files:
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application-dev.yml
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application-test.yml
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application-wuyx.yml
    - ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application-dev.yml
    - ele-ai-tender-system/ele-ai-tender-file/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-support/src/main/resources/application.yml
    - ele-ai-tender-frontend/.env.development
    - ele-ai-tender-frontend/.env.production
    - ele-ai-tender-frontend/vite.config.ts
    - ele-ai-tender-support-frontend/.env.development
    - ele-ai-tender-support-frontend/.env.production
    - ele-ai-tender-support-frontend/vite.config.ts
    - docs/guides/ele-ai-tender-core.yml
    - docs/guides/ele-ai-tender-ai.yml
---

本仓库采用 Spring Boot 标准的多环境 YAML 配置方案，结合环境变量与外部化配置实现运行时配置管理。后端各子模块（core、ai、file、support）均遵循统一的资源配置约定：每个模块在 `src/main/resources/` 下提供 `application.yml` 作为基础配置，并通过 `application-dev.yml`、`application-test.yml`、`application-wuyx.yml` 等 profile 文件按环境差异化覆盖；同时通过 `logback-spring.xml` 和 `logging-spring.xml` 分离日志配置。前端双工程（ele-ai-tender-frontend、ele-ai-tender-support-frontend）使用 Vite 的 `.env` 系列文件（`.env.development`、`.env.production`、`.env.test`、`.env.localdev`）配合 `vite.config.ts` 中的 `import.meta.env` 进行构建期配置注入。配置加载优先级遵循 Spring Boot 默认顺序：命令行参数 > 环境变量 > `application-{profile}.yml` > `application.yml`，并在 CI 流水线（Jenkinsfile-*.groovy）中通过 `-Dspring.profiles.active=xxx` 或 `-Dspring.config.location=` 指定运行期 profile。文档中心 `docs/guides/` 下的 `ele-ai-tender-core.yml`、`ele-ai-tender-ai.yml`、`ele-ai-tender-file.yml`、`ele-ai-tender-support.yml` 为各服务部署时的参考配置模板，用于容器化部署时挂载外部配置文件。开发者新增配置项应优先放入对应模块的 `application.yml` 并给出默认值，敏感信息通过环境变量注入，禁止将密钥硬编码进版本库。