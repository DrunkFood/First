---
kind: logging_system
name: 后端日志系统（Logback + 按模块分文件滚动）
category: logging_system
scope:
    - '**'
source_files:
    - ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/logback-spring.xml
    - ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/logging-spring.xml
    - ele-ai-tender-system/ele-ai-tender-core/src/main/resources/logback-spring.xml
    - ele-ai-tender-system/ele-ai-tender-file/src/main/resources/logback-spring.xml
    - ele-ai-tender-system/ele-ai-tender-support/src/main/resources/logback-spring.xml
---

## 1. 使用的框架与工具
- 日志门面：SLF4J（通过 Lombok `@Slf4j` 注入 `log`，或通过 `LoggerFactory.getLogger` 获取）
- 日志实现：Logback（每个 Spring Boot 子模块自带 `logback-spring.xml`）
- 无统一 logging starter，各模块独立维护自己的 Logback 配置。

## 2. 核心配置文件与位置
- AI 模块：`ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/logback-spring.xml`、`logging-spring.xml`
- Core 模块：`ele-ai-tender-system/ele-ai-tender-core/src/main/resources/logback-spring.xml`
- File 模块：`ele-ai-tender-system/ele-ai-tender-file/src/main/resources/logback-spring.xml`
- Support 模块：`ele-ai-tender-system/ele-ai-tender-support/src/main/resources/logback-spring.xml`

## 3. 架构与约定
- **输出目录**：所有模块统一输出到相对路径 `../logs/${project.name}/YYYY-MM-DD/`，便于容器编排时挂载同一卷。
- **文件拆分策略**：按级别拆分为三个 RollingFileAppender：
  - `INFO_FILE` → `${project.name}_info_%d{YYYY-MM-dd}_%i.log`
  - `WARN_FILE` → `${project.name}_warn_%d{YYYY-MM-dd}_%i.log`
  - `ERROR_FILE` → `${project.name}_error_%d{YYYY-MM-dd}_%i.log`
- **滚动策略**：`SizeAndTimeBasedRollingPolicy`，单文件上限 3GB，保留 14 天历史；AI 模块的 `logging-spring.xml` 额外使用 `totalSizeCap=300MB` 控制总大小。
- **控制台输出**：AI 模块提供彩色 pattern（`log.pattern.color`），其他模块为普通文本；开发/测试 profile 下 root logger 仅指向 console，生产 profile 才写入文件。
- **SQL 调试开关**：默认将 `com.apache.ibatis`、`java.sql.Connection/Statement/PreparedStatement` 设为 debug，便于本地排查。
- **业务专用日志**：AI 模块定义自定义 logger `AI_REQUIREMENT_GENERATION_LOG`，并映射到独立 appender `REQ_GEN_FILE`，用于记录需求生成定时任务与 prompt 等长文本，避免污染主日志。
- **日志格式**：统一 pattern `%d{YYYY-MM-dd HH:mm:ss.SSS} %-5level --- [%thread] %logger{36} - %msg%n`，包含时间、级别、线程、logger 名与消息。

## 4. 开发者应遵循的规则
- 使用 Lombok `@Slf4j` 注入 `log`，禁止直接 `System.out.println`。
- 关键流程入口/出口使用 `info`，异常与不可恢复错误使用 `error` 并附带堆栈，可复现信息用 `debug`。
- 需要单独归档的长文本（如 Prompt、大段生成结果）应通过 `AI_REQUIREMENT_GENERATION_LOG` 或新增同名 logger 输出到对应 appender，不要塞进 INFO/WARN/ERROR 主文件。
- 新增模块时复制任一现有 `logback-spring.xml`，修改 `project.name` 与 `log.path` 即可复用相同滚动策略。
- 不要在代码中硬编码日志级别过滤，如需调整请在对应模块的 `logback-spring.xml` 中通过 `<logger>` 或 `<springProfile>` 配置。