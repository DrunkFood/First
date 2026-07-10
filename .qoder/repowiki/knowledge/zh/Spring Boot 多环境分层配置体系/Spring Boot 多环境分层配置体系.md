---
kind: configuration_system
name: Spring Boot 多环境分层配置体系
category: configuration_system
scope:
    - '**'
source_files:
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application-dev.yml
    - ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-file/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-support/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/logback-spring.xml
    - ele-ai-tender-frontend/.env.development
    - ele-ai-tender-frontend/.env.production
    - ele-ai-tender-support-frontend/.env.development
    - docs/guides/ele-ai-tender-core.yml
---

## 系统概述

本项目采用 Spring Boot 原生配置机制，结合 Maven 聚合工程的多模块架构，构建了统一的后端配置体系。所有后端服务（core、ai、file、support）遵循一致的配置约定，通过 application.yml + 环境 profile + 用户本地覆盖文件三层叠加的方式管理运行时配置。

## 核心架构与加载顺序

配置加载优先级从高到低：
1. 用户本地覆盖文件：optional:file:${user.home}/.ele-ai-tender/{module}-local.yml（每个模块独立）
2. 环境变量：${VAR_NAME:default} 形式的占位符
3. 环境 Profile：application-{dev|test|wuyx}.yml
4. 主配置文件：application.yml

每个模块的 application.yml 都通过 spring.config.import 引入可选的用户级覆盖文件，实现代码不可变、配置可插拔的部署策略。

## 关键文件与结构

### 后端服务配置
- ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application.yml — 核心业务默认配置（端口 8082）
- ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application.yml — AI 服务默认配置（端口 8083）
- ele-ai-tender-system/ele-ai-tender-file/src/main/resources/application.yml — 文件服务默认配置（端口 8081）
- ele-ai-tender-system/ele-ai-tender-support/src/main/resources/application.yml — 支撑中心默认配置（端口 8080）
- 各模块对应的 application-dev.yml / application-test.yml / application-wuyx.yml

### 前端配置
- ele-ai-tender-frontend/.env* — Vite 构建期环境变量（.env.development、.env.production、.env.test 等）
- ele-ai-tender-support-frontend/.env* — 支撑前端独立的环境变量

### 日志配置
- 各模块 logback-spring.xml — 按 INFO/WARN/ERROR 分文件滚动输出，保留 14 天，单文件最大 3GB

## 配置约定与规范

### 环境变量命名规范
- 数据库：SPRING_DATASOURCE_URL / SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD
- Redis：SPRING_DATA_REDIS_HOST / SPRING_DATA_REDIS_PORT / SPRING_DATA_REDIS_PASSWORD / SPRING_DATA_REDIS_DATABASE
- JWT 密钥：APP_JWT_SECRET（所有模块共享同一密钥）
- 内部服务调用：INTERNAL_FILE_SERVICE_URL
- AI 服务：DEEPSEEK_API_KEY / DEEPSEEK_BASE_URL
- 向量数据库：MILVUS_HOST / MILVUS_PORT / MILVUS_DATABASE
- 短信网关：MSG_USERNAME / MSG_PWD / MSG_EXTEND / MSG_URL / MSG_ISFORMAL
- 文件存储：FILE_STORAGE_BASE_PATH

### 数据源隔离
- core/ai/file 模块使用表前缀 ai_
- support 模块使用表前缀 sup_
- file 模块使用表前缀 file_
- 所有模块共享同一 MySQL 实例的不同 database（或同一库不同表前缀）

### 安全与敏感信息
- JWT 密钥通过环境变量注入，禁止硬编码
- 外部服务地址（文件服务、AI 服务）通过环境变量配置
- 支持通过 ${user.home}/.ele-ai-tender/{module}-local.yml 在开发机本地覆盖配置

### 前端构建配置
- Vite 通过 .env.* 文件管理 API 基础路径、功能开关等构建期常量
- 双前端（编制前端、支撑前端）各自维护独立的环境变量文件

## 开发者注意事项

1. 新增配置项：在 application.yml 中定义带默认值的占位符，在对应环境的 application-{profile}.yml 中提供具体值
2. 敏感配置：必须通过环境变量注入，不得提交到版本控制
3. 本地调试：在 ~/.ele-ai-tender/ 下创建 {module}-local.yml 覆盖特定配置
4. 跨模块依赖：core/ai/support 通过 internal.file-service.base-url 访问文件服务，需确保网络可达
5. 日志级别：生产环境建议关闭 SQL 日志，仅保留 WARN/ERROR；开发环境可使用 StdOutImpl 输出 SQL