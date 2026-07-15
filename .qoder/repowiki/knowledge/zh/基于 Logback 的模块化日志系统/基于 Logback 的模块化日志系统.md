---
kind: logging_system
name: 基于 Logback 的模块化日志系统
category: logging_system
scope:
    - '**'
source_files:
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/logback-spring.xml
    - ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/logback-spring.xml
    - ele-ai-tender-system/ele-ai-tender-file/src/main/resources/logback-spring.xml
    - ele-ai-tender-system/ele-ai-tender-support/src/main/resources/application.yml
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application.yml
---

## 系统概述

本项目采用 **Logback** 作为后端日志框架，通过每个 Spring Boot 子模块独立配置 `logback-spring.xml` 实现按模块隔离的日志输出。前端（Vite + Vue）仅使用浏览器控制台日志，无统一前端日志系统。

## 核心架构与约定

### 1. 日志框架与依赖
- 后端：Logback（Spring Boot 默认集成），配合 Lombok `@Slf4j` 注解注入 Logger
- 前端：无统一日志框架，仅在开发时通过浏览器控制台调试

### 2. 日志文件结构
每个服务模块在 `src/main/resources/logback-spring.xml` 中定义独立的 Appender：
- **INFO_FILE**：info 级别日志，按天滚动，最大 3GB，保留 14 天
- **WARN_FILE**：warn 级别日志，同策略
- **ERROR_FILE**：error 级别日志，同策略
- **REQ_GEN_FILE**（仅 AI 模块）：需求生成专用日志，便于追踪 AI 任务执行链路
- 所有日志输出到 `../logs/{project.name}/YYYY-MM-DD/` 目录结构

### 3. 日志格式规范
统一 pattern：`%d{YYYY-MM-dd HH:mm:ss.SSS} %-5level --- [%thread] %logger{36} - %msg%n`
- 包含时间戳、级别、线程名、Logger 名称、消息内容
- core 模块额外通过 `logging.pattern.level` 注入 traceId 字段用于链路追踪

### 4. 日志级别策略
- 根级别：`info`（生产环境）
- 业务包：`com.jy.eleaitender.*` 设为 `debug`（可通过 application.yml 覆盖）
- SQL 日志：MyBatis 相关包设为 `debug`，core 模块使用 `StdOutImpl` 直接输出到控制台
- AI 模块对 `AI_REQUIREMENT_GENERATION_LOG` 单独配置 appender，不传播到 root

### 5. 开发者使用约定
- 使用 Lombok `@Slf4j` 注解获取 logger 实例
- 关键路径使用 info/debug，异常使用 error 并附带堆栈
- 业务异常分类记录：认证异常、AI 服务不可用、参数校验失败等
- 避免在循环中频繁打印 debug 日志，注意性能影响

## 关键文件
- `ele-ai-tender-core/src/main/resources/logback-spring.xml` — Core 模块日志配置
- `ele-ai-tender-ai/src/main/resources/logback-spring.xml` — AI 模块日志配置（含专用 REQ_GEN_FILE）
- `ele-ai-tender-file/src/main/resources/logback-spring.xml` — File 模块日志配置
- `ele-ai-tender-support/src/main/resources/application.yml` — Support 模块日志级别配置
- `ele-ai-tender-core/src/main/resources/application.yml` — Core 模块 traceId 模式配置

## 注意事项
- 各模块 logback 配置高度一致，新增模块需复制模板并修改 project.name
- 大对象或敏感信息不应直接打印到日志，需脱敏处理
- 当前未实现结构化日志（JSON 格式），如需对接 ELK 等日志平台需改造 pattern