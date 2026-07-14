---
kind: configuration_system
name: Spring Boot 多环境 YAML 配置体系
category: configuration_system
scope:
    - '**'
source_files:
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-file/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-support/src/main/resources/application.yml
    - ele-ai-tender-frontend/.env.development
    - ele-ai-tender-frontend/.env.production
    - ele-ai-tender-support-frontend/.env.development
    - ele-ai-tender-support-frontend/.env.production
    - docs/guides/ele-ai-tender-core.yml
---

本仓库采用 Spring Boot 标准的多环境配置文件体系，每个后端子模块（core、ai、file、support）均独立维护自己的 application.yml 与环境变体文件，通过 spring.profiles.active 切换运行环境。前端使用 Vite 的 .env 系列文件配合 vite.config.ts 注入构建时环境变量。

关键位置：后端各模块 resources/application.yml 及 dev/test/wuyx 变体；前端 ele-ai-tender-frontend/.env* 与 ele-ai-tender-support-frontend/.env*；部署示例见 docs/guides/ele-ai-tender-*.yml。

约定：每个子模块是独立可部署应用，不共享全局 application.yml；环境命名统一为 dev、test、wuyx；日志通过 logback-spring.xml + logging-spring.xml 管理；敏感信息通过外部参数或容器环境变量覆盖，禁止写入版本库。