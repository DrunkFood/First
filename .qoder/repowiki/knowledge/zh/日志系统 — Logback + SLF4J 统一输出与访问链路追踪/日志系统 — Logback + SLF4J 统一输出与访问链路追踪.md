---
kind: logging_system
name: 日志系统 — Logback + SLF4J 统一输出与访问链路追踪
slug: logging_system
category: logging_system
scope:
    - '**'
---

## 1. 使用的框架与工具
- 日志门面：SLF4J，通过 Lombok `@Slf4j` 注入 logger。
- 日志实现：Logback（每个 Spring Boot 子模块均自带 `logback-spring.xml`）。
- 链路追踪：基于 SLF4J MDC 的轻量 traceId 方案，由 common 模块提供通用能力。
- 访问日志：自定义 HTTP 过滤器 + 可选异步落库，支持 SSE 流式请求特殊处理。
- 敏感信息脱敏：提供 `SensitiveLogMasker` 工具类。

## 2. 核心文件与包
- 公共日志能力（common 模块）
  - `com.jy.eleaitender.common.logging.TraceContext` / `TraceConstants`：MDC traceId 生成、获取、清理。
  - `HttpRequestLogFilter`：统一拦截 HTTP 请求，补 traceId、记录入参/出参耗时并持久化。
  - `LoggingAutoConfiguration`：自动注册 Filter、异步写入线程池、可插拔的 AccessLog 持久化服务。
  - `AccessLogProperties`：`ele-ai-tender.logging.access.*` 配置项开关与长度限制。
  - `DbAccessLogPersistenceService`：默认将访问日志异步写入 `sys_access_log` 表。
  - `HttpAccessLogSupport`：构建结构化访问日志消息体。
  - `SensitiveLogMasker`：token/secret 等字段脱敏。
- 各业务模块 Logback 配置
  - `ele-ai-tender-core/src/main/resources/logback-spring.xml`
  - `ele-ai-tender-ai/src/main/resources/logback-spring.xml`（额外定义 `REQ_GEN_FILE` 专用 appender，并通过 `AI_REQUIREMENT_GENERATION_LOG` logger 路由）
  - `ele-ai-tender-file`、`ele-ai-tender-support` 等模块各自拥有相同结构的 `logback-spring.xml`。
- 应用层 logging 配置
  - 各模块 `application.yml` 中通过 `logging.pattern.level` 注入 `[traceId:%X{traceId:-}]`，并通过 `logging.level` 控制包级日志级别。

## 3. 架构与约定
- 输出结构
  - 控制台与文件共用 pattern：`%d{YYYY-MM-dd HH:mm:ss.SSS} %-5level --- [%thread] %logger{36} - %msg%n`。
  - 在 Spring `logging.pattern.level` 前缀追加 `[traceId:%X{traceId:-}]`，使每条日志都带 traceId。
- 文件滚动策略
  - 按 INFO/WARN/ERROR 分文件输出，使用 `SizeAndTimeBasedRollingPolicy`，单文件最大 3GB，保留 14 天。
  - AI 模块额外维护 `requirement_generation` 独立日志文件，用于需求生成流程的可观测性。
- 访问日志链路
  - `HttpRequestLogFilter` 在最高优先级执行，从请求头读取或生成 traceId，放入 MDC，并在响应返回前构造 `SysAccessLog` 对象。
  - 若未开启持久化则仅打印；开启后通过 `AccessLogPersistenceService` 异步写入数据库，失败时降级为同步。
  - SSE 请求不走 `ContentCachingResponseWrapper`，避免流事件丢失，单独以 `[SSE] ...` 格式记录。
- 线程上下文隔离
  - `TraceContext.clear()` 在 finally 中调用，防止线程复用导致 traceId 泄漏。
- 测试友好
  - AI 模块单元测试直接操作 `ch.qos.logback.classic.Logger` 与 `ListAppender`，验证特定日志是否输出。

## 4. 开发者应遵循的规则
- 使用 Lombok `@Slf4j` 注入 logger，禁止自行 `LoggerFactory.getLogger`（除 AI 模块显式使用 `AI_REQUIREMENT_GENERATION_LOG` 路由到独立文件的场景）。
- 所有业务日志必须包含 traceId（由 MDC 自动注入），不要手动拼接。
- 需要记录用户请求/响应体或关键业务参数时，优先使用 `HttpAccessLogSupport` 生成的结构化消息，或使用 `SensitiveLogMasker` 对 token、密码等敏感字段脱敏后再输出。
- 新增业务专属日志文件时，复制现有模块的 `logback-spring.xml` 模板，新增一个 RollingFileAppender 并声明对应 logger name，保持 3GB/14 天滚动策略一致。
- 如需调整日志级别，仅在对应模块的 `application.yml` 中修改 `logging.level`，不要硬编码到代码里。
- 访问日志开关与行为通过 `ele-ai-tender.logging.access.*` 配置，生产环境建议开启 `persist-enabled=true` 以便审计与排障。