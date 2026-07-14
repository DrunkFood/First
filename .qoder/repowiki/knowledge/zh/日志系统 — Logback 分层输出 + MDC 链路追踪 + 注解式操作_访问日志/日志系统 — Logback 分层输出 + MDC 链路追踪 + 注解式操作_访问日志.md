---
kind: logging_system
name: 日志系统 — Logback 分层输出 + MDC 链路追踪 + 注解式操作/访问日志
category: logging_system
scope:
    - '**'
source_files:
    - ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/logging/LoggingAutoConfiguration.java
    - ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/logging/HttpRequestLogFilter.java
    - ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/logging/TraceContext.java
    - ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/logging/SensitiveLogMasker.java
    - ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/logging/OperationLog.java
    - ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/aspect/OperationLogAspect.java
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/logback-spring.xml
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/logging-spring.xml
    - ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/logback-spring.xml
    - ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/generator/RequirementGenerator.java
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/application.yml
---

## 1. 使用的框架与工具
- 日志实现：Logback（每个子模块各自提供 logback-spring.xml / logging-spring.xml）
- 日志门面：SLF4J，业务代码统一通过 @Slf4j 或 LoggerFactory.getLogger(...) 使用
- 链路追踪：基于 SLF4J MDC 的 traceId，由公共包 com.jy.eleaitender.common.logging 提供
- 结构化/脱敏：SensitiveLogMasker 提供 token、secret 等字段的掩码能力
- 注解式日志：@OperationLog + AOP 切面将关键接口操作持久化到数据库
- 访问日志：HttpRequestLogFilter 拦截 HTTP 请求，采集入参/出参、耗时、状态码并落库

## 2. 核心文件与位置
- 通用日志基础设施（common 模块）
  - ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/logging/LoggingAutoConfiguration.java
  - .../logging/HttpRequestLogFilter.java
  - .../logging/TraceContext.java、.../logging/TraceConstants.java
  - .../logging/SensitiveLogMasker.java
  - .../logging/AccessLogProperties.java、.../logging/AccessLogPersistenceService.java、.../logging/DbAccessLogPersistenceService.java、.../logging/HttpAccessLogSupport.java
  - .../logging/OperationLog.java（注解定义）
- 操作日志切面（support 模块）
  - ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/aspect/OperationLogAspect.java
- 各应用模块的 Logback 配置（示例）
  - ele-ai-tender-core/src/main/resources/logback-spring.xml、.../resources/logging-spring.xml
  - ele-ai-tender-ai/src/main/resources/logback-spring.xml、.../resources/logging-spring.xml
- AI 专用日志 logger
  - ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/generator/RequirementGenerator.java（使用 AI_REQUIREMENT_GENERATION_LOG）
- Spring Boot 日志级别与 pattern 覆盖
  - ele-ai-tender-core/src/main/resources/application.yml（logging.pattern.level 注入 traceId）

## 3. 架构与约定
### 3.1 日志输出结构（按模块独立）
- 每个后端子模块在 src/main/resources 下自带一份 logback-spring.xml / logging-spring.xml，定义：
  - 控制台 appender（开发环境彩色输出）
  - 按级别分文件：INFO_FILE、WARN_FILE、ERROR_FILE，采用 SizeAndTimeBasedRollingPolicy，单文件上限 3GB，保留 14 天
  - 日志路径统一为 ${log.path}/${project.name}/%d{YYYY-MM-dd}/，即 ../logs/<模块名>/<日期>/ 目录结构
- AI 模块额外定义 REQ_GEN_FILE，并通过自定义 logger 名称 AI_REQUIREMENT_GENERATION_LOG 单独滚动输出需求生成过程日志。

### 3.2 链路追踪（traceId）
- 入口：HttpRequestLogFilter 从请求头读取 X-Trace-Id（常量见 TraceConstants.TRACE_ID_HEADER），不存在则调用 TraceContext.initTraceId() 生成 UUID 并写入 MDC；响应头也回写该 traceId。
- 格式：application.yml 中 logging.pattern.level 使用 %X{traceId:-}，使所有日志行自动携带 traceId。
- 清理：过滤器 finally 块中调用 TraceContext.clear() 避免线程池复用导致污染。

### 3.3 访问日志（HTTP Access Log）
- LoggingAutoConfiguration 以最高优先级注册 HttpRequestLogFilter，对非 SSE 请求用 ContentCachingRequestWrapper / ContentCachingResponseWrapper 缓存请求/响应体，构建 SysAccessLog 实体后：
  - 先通过 HttpAccessLogSupport.buildLogMessage(...) 打印一行结构化 INFO 日志
  - 再通过 AccessLogPersistenceService 异步（默认单线程 executor）持久化到 sys_access_log 表
- SSE 流式请求不走 body 缓存，仅记录方法、URI、耗时与 traceId，避免事件丢失。
- 是否启用访问日志、是否落库、是否异步落库均由 ele-ai-tender.logging.access.* 属性控制。

### 3.4 操作日志（@OperationLog）
- 在 support 模块的 OperationLogAspect 中通过 @Around("@annotation(OperationLog)") 拦截标注方法，提取用户信息、方法签名、参数（过滤 servlet 对象、限制长度）、IP、执行耗时，异步写入 sys_operation_log 表。
- 业务 Controller 通过 @OperationLog("创建菜单") 等描述性值声明需要审计的操作。

### 3.5 敏感信息脱敏
- 提供 SensitiveLogMasker.maskToken、maskSecret 等方法，在构造日志消息前对敏感字段进行前后缀保留、中间替换为 *** 的处理。

## 4. 开发者应遵循的规则
1. 不要直接 new Logger：优先使用 Lombok @Slf4j；仅在需要自定义 logger 名称（如 AI_REQUIREMENT_GENERATION_LOG）时才用 LoggerFactory.getLogger(...)
2. 保持 traceId 贯穿全链路：跨服务调用时带上 X-Trace-Id 请求头；本地线程内通过 TraceContext.getTraceId() 获取，不要在业务中手动 put/remove MDC
3. 日志级别规范：
   - INFO：业务流程关键点、外部调用摘要
   - WARN：可恢复异常、降级、重试
   - ERROR：不可恢复错误、堆栈必须完整
   - DEBUG：仅用于本地调试，生产默认关闭（可通过 profile 调整）
4. 禁止直接拼接大对象：记录请求/响应体一律走 HttpAccessLogSupport 或 ObjectMapper 序列化，避免阻塞；参数过长会被截断
5. 敏感字段必须脱敏：token、密码、密钥等在使用 SensitiveLogMasker 处理后再拼入日志
6. 新增业务专属日志文件：参照 AI 模块的 AI_REQUIREMENT_GENERATION_LOG 模式，先在 logback-spring.xml 中定义新 appender 和 logger，再在代码中使用对应 logger 名称
7. SSE 场景注意：长连接/流式响应不要包装 response body，参考 HttpRequestLogFilter.isSseRequest 的判断逻辑
8. 访问日志开关：通过 ele-ai-tender.logging.access.enabled/persist-enabled/async-persist 控制，部署时按需关闭以提升性能