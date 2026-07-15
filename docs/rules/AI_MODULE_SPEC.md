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

`/chat` 支持 **AI助手替换模式**：`ChatRequest` 携带 `replaceMode=true` + `markdownContext`（编辑器完整 Markdown 上下文）+ `context`（用户选中的原文，replaceMode 时作为"用户选中的原文"注入 Prompt）。AI 回复中用固定标记 `【可替换正文开始】...【可替换正文结束】` 包裹可直接替换选中文本的内容，一次回复可包含多组标记，每组对应一个可选替换方案，由前端 `aiReplacement.ts` 解析并多方案选择。

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

### 2.4 模型连通性测试 (`/api/v1/ai/model-test`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/ai/model-test/deepseek` | 测试 DeepSeek（OpenAI 兼容）模型连通性 |
| POST | `/api/v1/ai/model-test/zhipu` | 测试智谱模型连通性 |

接口无入参，服务端硬编码两个测试模型 ID（`DEEPSEEK_TEST_MODEL_ID=8`、`ZHIPU_TEST_MODEL_ID=10`），通过 `ModelRouter.routeModel(modelId)` 绕过场景路由直建 ChatClient 发起一次同步测试调用，返回 `ModelConnectivityTestResponse`（`provider` / `modelConfigId` / `modelName` / `apiEndpoint` / `success` / `content` / `errorMessage` / `elapsedMs`）。仅测试这两个预设模型，不接收调用方指定的 modelConfigId。

## 3. 处理器体系

### 3.1 AiTaskProcessor

轮询 `ai_task` 表执行AI任务的核心调度器。根据 `task_type` 分派到对应的 Generator/Detector 执行。

### 3.2 生成器（Generator）

| 类 | 任务类型 | 职责 |
|----|----------|------|
| `RequirementGenerator` | REQUIREMENT_GENERATE / PROJECT_REQUIREMENT_GENERATE | 需求内容生成（三步式 Agent 编排 + 渐进式进度推送 + 5000 字硬约束） |
| `ReviewItemGenerator` | REVIEW_ITEM_GENERATE | 评审项生成（受 `review_config` 控制 + JSON 归一化 + AI 修复） |
| `DocumentIntegration` | DOCUMENT_INTEGRATION | 文档集成（AI语义匹配占位符与数据key + 调用File服务poi-tl生成Word） |
| `TextOptimizer` | TEXT_OPTIMIZE | 文本优化 |

#### 3.2.1 RequirementGenerator 三步式编排

需求生成从单次生成重构为 **三步式 Agent 编排**（代码注释自称"三步式"；`assembleFullContent` 为纯代码拼接，不计入 AI 调用步），全程同步调用（非流式），通过 `ai_task.result` 字段渐进式推送进度供前端轮询：

```
Step1 generateOutline       → 生成大纲 JSON（OutlineResult，含 chapters + estimatedWords）
Step2 generateChapters      → 分章并行生成详细内容（虚拟线程 + Semaphore(3) 限流，10min 超时）
   └ assembleFullContent   → 纯代码拼接全文（不调 AI）
Step3 reviewAndRefine       → AI 审查 + applyRevisions() 精确替换修订
```

- **渐进式进度推送**：每个步骤节点调用 `publishProgress()` → `AiTaskMapper.updateResult()` 更新 `ai_task.result`。`contentStage` 取值 `OUTLINE_GENERATED → CHAPTER_GENERATING → DRAFT_COMPLETED → REVIEWING → COMPLETED`，附带 `outline`、`chapters[]{status,content}`、`completedChapterCount/totalChapterCount`、`reviewStatus`。前端 `parseRequirementGenerationProgress()` 解析后实时渲染大纲骨架 + 已完成章节内容。`content` 字段在首章内容生成前保持空缺。
- **5000 字硬约束（三层）**：① Prompt 层要求大纲 `estimatedWords` 合计 ≤ 5000、章节数 5-6；② 代码层 `normalizeOutlineEstimatedWords()` 对超限大纲按比例压缩每章字数，保底 `MIN_CHAPTER_ESTIMATED_WORDS=300`；③ 章节生成 Prompt 用"硬性字数上限"强制约束，超出视为无效输出。常量 `MAX_REQUIREMENT_TOTAL_WORDS=5000`、`DEFAULT_CHAPTER_ESTIMATED_WORDS=800`。
- **降级大纲** `buildFallbackOutline()`：大纲生成失败时按 `ProjectType`（SERVICE/GOODS/ENGINEERING）使用预设章节模板兜底。
- **审查修订** `applyRevisions()` 三重防护：原文长度 < 20 跳过、原文在全文匹配次数 > 1 跳过、从后向前替换避免偏移漂移。

> 同步调用而非 SSE 流式：需求/评审项生成走 `ai_task` 异步任务模式，前端通过 `useLatestTask` 轮询任务状态 + 解析 `result` 渲染渐进进度（见 `ai-task.ts`）。

#### 3.2.2 ReviewItemGenerator 生成规则

- **按 ReviewConfig 差异化**：若 `params.getReviewConfig()` 有值，仅生成 `config.getEnabledTypes()` 启用的评审类型；`generateStandard=false` 的启用类型由 core 同步处理器（`AiTaskResultSyncHandler`）保留一级节点、二级以下替换为占位节点（`itemName='详见评审文件'`）；未启用类型不生成；为 null 时回退全类型生成。
- **JSON 归一化** `normalizeReviewItemJson()`：递归搜索 AI 返回 JSON 中的评审项数组（兼容 `reviewItems`/`items`/`评审项` 等多种字段名和 `data`/`result` 等包装），并将"分类作顶层 key"的结构转换为 `{name, level, children}` 数组。
- **AI 修复** `repairReviewItemJson()`：首次结果 JSON 异常时，用 `REVIEW_ITEM_JSON_REPAIR` Prompt 再次调用 AI 仅修格式不重生内容。
- **计分硬规则**（Prompt 层）：符合性审查 score 为 0；叶子节点合计必须精确等于 100；只启用符合性审查时所有 score 为 0。

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
| `AiCallRecorder` | AI调用记录（token使用量、响应内容、模型信息写入 `ai_response_log`）。统一用 `client.prompt().messages(List<Message>)` 调用（不再用 `.system()/.user()`），绕过 Spring AI `PromptTemplate` 对 `{var}` 的变量解析，避免 JSON 示例中的花括号被误解析。提供 `buildUserPromptWithFiles()` 将文件内容注入 userPrompt（供大纲步骤单独调用） |
| `AiServiceLifecycle` | 启动/关闭时AI任务清理（重置PROCESSING→PENDING、关闭线程池） |
| `FileContentService` | 通过 Apache Tika 提取文件内容，供检测和知识库使用 |

## 4. Prompt 体系

| 类 | 职责 |
|----|------|
| `PromptBuilder` | 动态构建 User Prompt（插入需求内容、评审配置、章节字数上限等上下文），含 `buildReviewItemJsonRepair()` |
| `SystemPromptTemplates` | 所有 System Prompt 模板常量 |
| `UserPromptTemplates` | 所有 User Prompt 模板常量 |

**关键约定**：

- **单花括号**：模板内 JSON 示例直接用 `{`/`}`（不再用 `{{`/`}}` 双花括号转义），因为调用入口已改为直接传 `SystemMessage`/`UserMessage`，不经过 `PromptTemplate` 解析。
- **角色定位升级**：System Prompt 统一为"经验丰富的招标采购需求编制专家，精通《招标投标法》《政府采购需求管理办法》"。
- **需求大纲**：要求 `estimatedWords` 合计 ≤ 5000、章节数 5-6；按 `ProjectType` 差异化规划（工程→施工组织/材料品牌/质保；货物→技术规格表/样品检测/包装；服务→团队配置/SLA/交付物）；含价格分权重与资格要求建议。
- **章节生成**：`REQUIREMENT_CHAPTER_GENERATE_USER` 用"硬性字数上限：XXX字，超出视为无效输出"约束。
- **评审项**：`REVIEW_ITEM_GENERATE` 含计分口径硬规则（符合性 score=0、叶子合计=100）+ 输出格式硬要求（只输出 JSON、首字符 `{`、根节点 `reviewItems`、禁 Markdown 代码块/外层包装）。
- **JSON 修复**：`REVIEW_ITEM_JSON_REPAIR` 专修格式不重生内容。
- 旧 `REQUIREMENT_GENERATE`（单次、要求"不少于 10000 字"）已 `@Deprecated`，由三步式编排替代。

## 5. 模型路由

```
任务类型 → ModelRouter → DynamicChatClientFactory → 本地模型(LOCAL) / 云端模型(CLOUD) / 私有化模型(PRIVATE)
```

| 类 | 职责 |
|----|------|
| `ModelRouter` | 根据任务类型和使用场景路由到主/备模型。`route(AiTaskType)`/`route(AiUsageScenario)` 走场景优先级路由；`routeModel(Long modelId)` 绕过路由按指定 modelId 直建 ChatClient（供连通性测试） |
| `DynamicChatClientFactory` | 动态创建 ChatClient 实例 |
| `ModelConfigCacheService` | 缓存模型配置，定期从 `sup_model_config` + `sup_model_route_rule` 刷新 |

**默认模型**：`application.yml` 通过 OpenAI 兼容接口接入 DeepSeek（`base-url=https://api.deepseek.com`，`model=deepseek-chat`，`api-key=${DEEPSEEK_API_KEY}`）。模型本身不硬编码，完全由 `sup_model_config` + `sup_model_route_rule` 数据库配置驱动。

**AiUsageScenario 枚举值**：`GENERATION`（内容生成）、`OPTIMIZATION`（文本优化）、`DETECTION`（智能检测）、`CHAT`（AI对话）。

**AiTaskSource 枚举值**：`INTERNAL`（内部任务，由 core 模块写入 `ai_task` 表）、`EXTERNAL`（外部任务，由外部系统通过 `/api/external/ai-tasks` 创建）。

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
| `system_id` | 任务来源系统ID（`sup_access_system.id`，内部任务为 null） |
| `project_id` | 关联项目ID |
| `biz_id` | 业务实体ID |
| `biz_type` | 业务实体类型 |
| `request_params` | 请求参数(JSON) |
| `file_ids` | 关联文件ID列表(逗号分隔) |
| `status` | 任务状态（PENDING/PROCESSING/COMPLETED/FAILED/AI_UNAVAILABLE/SKIPPED） |
| `result` | 执行结果(JSON) |
| `error_msg` | 错误信息 |
| `retry_count` | 已重试次数 |
| `max_retry` | 最大重试次数 |
| `started_at` | 开始时间 |
| `completed_at` | 完成时间 |
| `timeout_minutes` | 超时时间 |
| `result_synced` | 结果同步状态（0-未同步 1-已同步 2-同步失败 3-同步中） |
| `synced_at` | 同步时间 |

## 9. 排障原则

- **AI任务卡在PROCESSING** → 查 `ai_task.started_at`，检查是否超时；检查 AiTaskProcessor 日志是否报错；可用 `POST /v1/ai-tasks/{id}/skip` 手动跳过
- **模型调用失败** → 查 `sup_model_config` 配置，检查 `sup_model_route_rule` 路由规则，确认 api_key 和 endpoint 正确
- **知识库检索不准** → 查 `ai_knowledge_document.vector_ids`，确认向量化是否完成
- **线程池任务堆积** → 查 `sup_sys_parameter` 线程池参数，检查 DynamicThreadPoolManager 日志
- **用户并发超限** → 检查 UserConcurrencyManager 的 Semaphore 许可数和活跃计数
- **Tika解析失败** → 查 FileContentService 日志，确认文件格式是否受支持
- **需求生成卡住/字数异常** → 查专用日志 `logs/ele-ai-tender-ai/{date}/ele-ai-tender-ai_requirement_generation_*.log`（Logger `AI_REQUIREMENT_GENERATION_LOG`，含三步式各阶段计时与 Prompt），检查 `ai_task.result` 的 `contentStage` 推进

## 10. 配置要点

- **JWT 过期**：`APP_JWT_EXPIRATION` 默认 `43200` 秒（12 小时，原 7200）。
- **需求生成专用日志**：`logback-spring.xml` 新增 `REQ_GEN_FILE` Appender + `AI_REQUIREMENT_GENERATION_LOG` Logger（`additivity=false`，独立文件、不向 root 传播），专供 `RequirementGenerator` 各阶段计时与 Prompt 记录。
- **日志路径**：`fileNamePattern` 去掉 `../` 前缀，直接写项目相对 `logs/` 目录。
- **GlobalExceptionHandler**：所有 `log.error` 补全异常堆栈参数；core 模块未处理异常消息脱敏为固定文案"系统异常，请联系管理员"。

## 相关规范

- [CORE_MODULE_SPEC.md](CORE_MODULE_SPEC.md) — 核心业务模块规范
- [DETECTION_FLOW_SPEC.md](DETECTION_FLOW_SPEC.md) — 检测全链路规范
- [FILE_SERVICE_SPEC.md](FILE_SERVICE_SPEC.md) — 文件服务模块规范
