# AI任务机制 (AI Task)

> 异步任务队列 + CAS抢占 + 模型路由 + 结果同步 + 降级策略

## 核心数据结构

**AiTask** (`common/entity/ai/AiTask.java`)

| 字段 | 类型 | 说明 |
|------|------|------|
| `taskType` | String | AI任务类型（见9种类型表） |
| `bizType` | String | REQUIREMENT / PROJECT / DETECTION |
| `bizId` | Long | 业务ID（需求ID/项目ID/检测记录ID） |
| `projectId` | Long | 项目ID |
| `status` | String | PENDING / PROCESSING / COMPLETED / FAILED / AI_UNAVAILABLE / SKIPPED |
| `requestParams` | text | 请求参数JSON |
| `result` | text | 结果内容JSON |
| `errorMsg` | varchar | 错误信息 |
| `retryCount` | int | 已重试次数 |
| `maxRetry` | int | 最大重试次数（默认3） |
| `timeoutMinutes` | int | 超时分钟数（默认10，各类型不同） |
| `resultSynced` | int | 0未同步 / 1已同步 / 2同步失败 |
| `fileIds` | varchar | 关联文件ID（逗号分隔） |

**代码位置**:
- 任务处理器: `ai/processor/AiTaskProcessor.java`
- 任务Service: `core/service/impl/AiTaskServiceImpl.java`
- 模型路由: `ai/processor/model/ModelRouter.java`
- 动态ChatClient工厂: `ai/processor/model/DynamicChatClientFactory.java`
- 结果同步调度器: `core/service/impl/AiTaskServiceImpl.java`（内部Scheduled方法）
- 超时检查器: core模块的 AiTaskTimeoutChecker
- 线程池: `ai/threadpool/DynamicThreadPoolManager.java`

## 异步任务队列流程

```
Core模块                          ai_task表                        AI模块
    │                                 │                               │
    │── INSERT (status=PENDING) ────→│                               │
    │←── 返回 taskId ────────────────│                               │
    │                                 │←── SELECT PENDING (10条) ─────│ (每5秒)
    │                                 │←── CAS: PENDING→PROCESSING ───│
    │                                 │                               │── 调用大模型
    │                                 │←── UPDATE status=COMPLETED ───│
    │                                 │       result=内容              │
    │                                 │       resultSynced=0          │
    │←── SELECT resultSynced=0 ──────│                               │ (每10秒)
    │── UPDATE 业务表内容 ──────────→│                               │
    │── UPDATE resultSynced=1 ──────→│                               │
    │                                 │                               │
前端←── GET /ai-tasks/{id} (3s轮询)──│                               │
```

**关键调度器**:
- `AiTaskProcessor.processPendingTasks()` — 每5秒轮询PENDING任务（`@Scheduled(fixedDelay = 5000)`）
- `AiTaskResultSyncScheduler` — 每10秒同步已完成任务结果到业务表
- `AiTaskTimeoutChecker` — 每60秒扫描超时任务，标记 AI_UNAVAILABLE

## CAS抢占机制

**AiTaskProcessor** 使用 CAS (Compare-And-Swap) 实现多实例安全的任务抢占:

```java
// 1. 先获取用户并发许可
if (!concurrencyManager.tryAcquire(userId)) {
    continue; // 并发数已满，排队等待
}

// 2. CAS抢占任务
int updated = aiTaskMapper.casUpdateStatus(task.getId(), "PENDING", "PROCESSING");
if (updated == 0) {
    // CAS失败（被其他实例抢占），归还许可
    concurrencyManager.release(userId);
    continue;
}

// 3. 提交到线程池执行
submitWithConcurrencyControl(task);
```

> **设计要点**: 先获取并发许可再CAS，避免 PROCESSING→PENDING 弹跳。CAS利用数据库原子更新（`UPDATE ... WHERE status='PENDING'`）。

## 9种AI任务类型

| 任务类型 | 说明 | 路由场景 | 超时(分) | 参数类 |
|---------|------|---------|---------|--------|
| REQUIREMENT_GENERATE | 需求生成 | GENERATION | 20 | RequirementGenerateParams |
| PROJECT_REQUIREMENT_GENERATE | 项目需求生成 | GENERATION | 20 | RequirementGenerateParams |
| REVIEW_ITEM_GENERATE | 评审项生成 | GENERATION | 10 | ReviewItemGenerateParams |
| DOCUMENT_INTEGRATION | 文档集成 | — | 10 | DocumentIntegrationParams |
| DETECTION_SENSITIVE_WORD | 敏感词检测 | DETECTION | 15 | DetectionParams |
| DETECTION_TYPO | 错别字检测 | DETECTION | 15 | DetectionParams |
| DETECTION_POLICY_REVIEW | 政策文件审查 | DETECTION | 15 | DetectionParams |
| DETECTION_FORMAT_CHECK | 格式规范检测 | DETECTION | 15 | DetectionParams |
| TEXT_OPTIMIZE | 文本优化 | OPTIMIZATION | 10 | TextOptimizeParams |

**任务分发** (`AiTaskProcessor.dispatch()`):
```java
switch (taskType) {
    case TEXT_OPTIMIZE -> textOptimizer.optimize(task);
    case REQUIREMENT_GENERATE, PROJECT_REQUIREMENT_GENERATE -> requirementGenerator.generate(task);
    case REVIEW_ITEM_GENERATE -> reviewItemGenerator.generate(task);
    case DOCUMENT_INTEGRATION -> documentIntegration.integration(task);
    case DETECTION_SENSITIVE_WORD, DETECTION_TYPO, DETECTION_POLICY_REVIEW, DETECTION_FORMAT_CHECK
        -> detectionEngine.detect(task);
}
```

## 任务状态流转

```
PENDING → PROCESSING → COMPLETED (resultSynced=0 → 同步后=1)
                     → FAILED（可重试，retryCount < maxRetry）
                     → AI_UNAVAILABLE（AiTaskTimeoutChecker 60s扫描超时任务）
                     → SKIPPED（用户主动跳过）
```

**AiTaskStatus 枚举** (`common/enums/AiTaskStatus.java`):
- PENDING — 等待处理
- PROCESSING — 处理中
- COMPLETED — 已完成
- FAILED — 失败
- AI_UNAVAILABLE — AI服务不可用
- SKIPPED — 已跳过

## 模型路由机制

**ModelRouter** (`ai/processor/model/ModelRouter.java`):

```
AiTaskType → AiUsageScenario.resolveScenario() → 使用场景
  ├─ GENERATION → 生成类模型（需求生成/评审项生成）
  ├─ OPTIMIZATION → 优化类模型（文本优化）
  └─ DETECTION → 检测类模型（4项检测）
       ↓
查询 sup_model_route_rule（按priority升序）
  ├─ 尝试 primaryModelId
  │    ├─ 可用 → DynamicChatClientFactory 创建 ChatClient
  │    └─ 不可用 → 尝试 fallbackModelId
  │         ├─ 可用 → 创建 ChatClient
  │         └─ 不可用 → 抛出 AiUnavailableException
  └─ 供应商类型
       ├─ OPENAI → OpenAI兼容协议（DeepSeek/GPT/vLLM/Ollama）
       └─ ZHIPU → 智谱AI SDK 专属调用
```

**AiUsageScenario** (`common/enums/AiUsageScenario.java`):
- GENERATION — 生成场景
- OPTIMIZATION — 优化场景
- DETECTION — 检测场景

## 结果同步机制

**AiTaskResultSyncScheduler** (core模块，每10秒扫描):

```
SELECT WHERE status=COMPLETED AND resultSynced=0
  ↓
根据任务类型同步到业务表:
  REQUIREMENT_GENERATE           → 更新 tb_requirement.content
  PROJECT_REQUIREMENT_GENERATE   → 更新 tb_project.requirement_content
  REVIEW_ITEM_GENERATE           → 插入 tb_project_review_item
  DETECTION_*                    → 更新 tb_detection_record.result
  DOCUMENT_INTEGRATION           → 更新 project.generatedFileId
  ↓
resultSynced=1（成功）或 resultSynced=2（失败，下次重试）
```

## 降级策略

```
AI任务提交
  ├─ AI模块拉取 → 正常执行
  │    ├─ 成功 → COMPLETED, resultSynced=0 → 同步后=1
  │    └─ 失败 → FAILED → retryCount < maxRetry? → 用户重试
  └─ AI模块未拉取 → AiTaskTimeoutChecker 60s扫描超时 → AI_UNAVAILABLE
       ↓
用户操作:
  ├─ 重试 → 重新提交任务
  └─ 跳过 → SKIPPED → 降级手动输入
```

## 并发控制

**UserConcurrencyManager** (`ai/threadpool/UserConcurrencyManager.java`):
- 按用户维度控制并发任务数
- `tryAcquire(userId)` — 获取并发许可
- `release(userId)` — 归还许可
- CAS前获取许可，避免 PROCESSING→PENDING 弹跳

**DynamicThreadPoolManager** (`ai/threadpool/DynamicThreadPoolManager.java`):
- 动态线程池，支持运行时调整参数
- 任务超时控制（`future.get(timeoutMinutes, TimeUnit.MINUTES)`）
- 超时自动取消（`future.cancel(true)`）

**分章生成并发**: `RequirementGenerator` 中分章并行生成最大并发数为3（`CHAPTER_GENERATION_CONCURRENCY = 3`），使用JDK21虚拟线程。

## 注意事项

1. **CAS先获取许可**: 先 `tryAcquire` 再 CAS，CAS失败后必须 `release`，否则许可泄漏
2. **resultSynced 三态**: 0未同步/1已同步/2失败，失败状态会在下次扫描时重试
3. **超时默认10分钟**: 各任务类型有不同超时（见9种类型表），`timeoutMinutes` 可在任务级别覆盖
4. **无用户任务直接执行**: `createId == null || createId == 0` 的任务跳过并发控制直接提交
5. **错误信息截断**: `truncateErrorMsg()` 限制错误信息 ≤ 500 字符
6. **AiErrorContentException**: AI输出内容异常（如格式错误），单独处理记录 content 字段
7. **common变更后必须install**: 修改 common 模块后必须 `mvn install -pl ele-ai-tender-common -AM`，否则依赖模块编译报错
