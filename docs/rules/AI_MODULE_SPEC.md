# AI服务模块规范

## 1. 定位

`ele-ai-tender-ai` (端口 8083) 负责 AI 能力提供：AI助手对话、知识库管理、文档匹配、智能检测、模型路由。

核心职责边界：
- **core模块**: 负责"何时生成"——组装参数、写入AI任务到 `ai_task` 表、管理文档记录
- **ai模块**: 负责"如何生成"——AiTaskProcessor轮询任务、Markdown→Word转换、模板渲染、检测执行
- **调用链路**: `core Service` → 写入 `ai_task` → `AiTaskProcessor` 轮询执行 → `core AiTaskResultSyncHandler` 读取结果

## 2. 当前接口（`/api/v1`）

### 2.1 AI助手 (`/api/v1/ai`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/ai/chat` | AI对话（SSE流式响应） |
| POST | `/api/v1/ai/optimize` | 文本优化（SSE流式响应） |
| POST | `/api/v1/ai/suggest` | 获取AI建议（同步响应） |

### 2.2 文档匹配 (`/api/v1/ai/match`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/ai/match/auto` | 自动匹配历史需求 |
| POST | `/api/v1/ai/match/manual` | 手动选择匹配（返回候选列表） |

### 2.3 知识库 (`/api/v1/knowledge/documents`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/knowledge/documents` | 查询知识文档列表 |
| GET | `/api/v1/knowledge/documents/{id}` | 获取知识文档详情 |
| POST | `/api/v1/knowledge/documents` | 上传知识文档 |
| DELETE | `/api/v1/knowledge/documents/{id}` | 删除知识文档 |

## 3. 处理器体系

### 3.1 AiTaskProcessor

轮询 `ai_task` 表执行AI任务的核心调度器。根据 `task_type` 分派到对应的 Generator/Detector 执行。

### 3.2 生成器（Generator）

| 类 | 任务类型 | 职责 |
|----|----------|------|
| `RequirementGenerator` | REQUIREMENT_GENERATE / PROJECT_REQUIREMENT_GENERATE | 需求内容生成 |
| `ReviewItemGenerator` | REVIEW_ITEM_GENERATE | 评审项生成（受 review_config 控制） |
| `DocumentIntegration` | DOCUMENT_INTEGRATION | 文档集成（AI语义匹配占位符与数据key + 调用File服务poi-tl生成Word） |
| `TextOptimizer` | TEXT_OPTIMIZE | 文本优化 |

### 3.3 检测器（Detector）

| 类 | 任务类型 | 职责 |
|----|----------|------|
| `DetectionEngine` | — | 检测引擎编排器，协调4种检测并行执行 |
| `BaseDetector` | — | 检测器抽象基类 |
| `SensitiveWordDetector` | DETECTION_SENSITIVE_WORD | 敏感词检测 |
| `TypoDetector` | DETECTION_TYPO | 错别字检测 |
| `PolicyReviewDetector` | DETECTION_POLICY_REVIEW | 政策文件审查 |
| `FormatCheckDetector` | DETECTION_FORMAT_CHECK | 格式规范检测 |

检测全链路详见 [DETECTION_FLOW_SPEC.md](DETECTION_FLOW_SPEC.md)。

### 3.4 辅助类

| 类 | 职责 |
|----|------|
| `GenerateResultParser` | AI输出解析器（JSON/Markdown） |
| `AiCallRecorder` | AI调用记录（token使用量、响应内容、模型信息写入 `ai_response_log`） |
| `AiServiceLifecycle` | 启动/关闭时AI任务清理（重置PROCESSING→PENDING、关闭线程池） |
| `FileContentService` | 通过 Apache Tika 提取文件内容，供检测和知识库使用 |

## 4. Prompt 体系

| 类 | 职责 |
|----|------|
| `PromptBuilder` | 动态构建 User Prompt（插入需求内容、评审配置等上下文） |
| `PromptTemplates` | 所有 System/User Prompt 模板常量 |

## 5. 模型路由

```
任务类型 → ModelRouter → DynamicChatClientFactory → 本地模型(LOCAL) / 云端模型(CLOUD) / 私有化模型(PRIVATE)
```

| 类 | 职责 |
|----|------|
| `ModelRouter` | 根据任务类型和使用场景路由到主/备模型 |
| `DynamicChatClientFactory` | 动态创建 ChatClient 实例 |
| `ModelConfigCacheService` | 缓存模型配置，定期从 `sup_model_config` + `sup_model_route_rule` 刷新 |

## 6. 线程池架构

### 6.1 DynamicThreadPoolManager

DB轮询+Hash检测模式的动态线程池：
- 定期从 `sup_sys_parameter` 读取线程池配置
- 配置变更时通过 Hash 检测热更新线程池参数（core/max/queue）
- 任务执行使用 Semaphore 控制并发

### 6.2 UserConcurrencyManager

用户级并发控制：
- Semaphore 双层控制：全局并发 + 单用户并发
- 活跃任务计数跟踪
- 同一用户同时执行的AI任务数受上限约束

### 6.3 ThreadPoolProperties

线程池配置 POJO，映射 `sup_sys_parameter` 中的参数组。

## 7. 知识库向量化流程

```
文档上传 → Apache Tika解析 → 文本分块(500-1000字/块) → Embedding API向量化 → Milvus存储 → 相似度检索 → Top-K返回
```

## 8. AI任务表（`ai_task`）

| 字段 | 说明 |
|------|------|
| `task_type` | 任务类型（AiTaskType 枚举） |
| `project_id` | 关联项目ID |
| `biz_id` | 业务实体ID |
| `biz_type` | 业务实体类型 |
| `request_params` | 请求参数(JSON) |
| `file_ids` | 关联文件ID列表(JSON) |
| `status` | 任务状态（PENDING/PROCESSING/COMPLETED/FAILED/AI_UNAVAILABLE/SKIPPED） |
| `result` | 执行结果(JSON) |
| `error_msg` | 错误信息 |
| `retry_count` | 已重试次数 |
| `max_retry` | 最大重试次数 |
| `started_at` | 开始时间 |
| `completed_at` | 完成时间 |
| `timeout_minutes` | 超时时间 |
| `result_synced` | 结果同步状态（0-未同步 1-已同步 2-同步失败） |

## 9. 排障原则

- **AI任务卡在PROCESSING** → 查 `ai_task.started_at`，检查是否超时；检查 AiTaskProcessor 日志是否报错；可用 `POST /v1/ai-tasks/{id}/skip` 手动跳过
- **模型调用失败** → 查 `sup_model_config` 配置，检查 `sup_model_route_rule` 路由规则，确认 api_key 和 endpoint 正确
- **知识库检索不准** → 查 `ai_knowledge_document.vector_ids`，确认向量化是否完成
- **线程池任务堆积** → 查 `sup_sys_parameter` 线程池参数，检查 DynamicThreadPoolManager 日志
- **用户并发超限** → 检查 UserConcurrencyManager 的 Semaphore 许可数和活跃计数
- **Tika解析失败** → 查 FileContentService 日志，确认文件格式是否受支持

## 相关规范

- [CORE_MODULE_SPEC.md](CORE_MODULE_SPEC.md) — 核心业务模块规范
- [DETECTION_FLOW_SPEC.md](DETECTION_FLOW_SPEC.md) — 检测全链路规范
- [FILE_SERVICE_SPEC.md](FILE_SERVICE_SPEC.md) — 文件服务模块规范
