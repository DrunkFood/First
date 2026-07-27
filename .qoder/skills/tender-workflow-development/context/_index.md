# 业务知识索引 (Knowledge Map)

> 本文件是业务知识的导航地图。AI 助手首先加载本文件，根据问题关键词定位到具体领域文档，再按需加载。

## 业务领域概览

| 领域 | 目录 | 核心代码位置 | 说明 |
|------|------|-------------|------|
| [业务需求编制](#业务需求编制-requirement) | `requirement/` | `core/controller/RequirementController.java` `core/service/impl/RequirementServiceImpl.java` | 需求创建、AI生成、编辑审核、需求检测(2项) |
| [项目编制五阶段](#项目编制五阶段-project-lifecycle) | `project-lifecycle/` | `core/controller/ProjectController.java` `core/service/impl/ProjectServiceImpl.java` `frontend/.../ProjectWizard.vue` | 基础信息→需求→评审项→文档→检测 |
| [智能检测](#智能检测-detection) | `detection/` | `core/service/impl/DetectionServiceImpl.java` `ai/processor/checker/DetectionEngine.java` | 需求检测(2项) + 项目检测(4项) + 建议处理 |
| [AI任务机制](#ai任务机制-ai-task) | `ai-task/` | `ai/processor/AiTaskProcessor.java` `core/service/impl/AiTaskServiceImpl.java` `ai/processor/model/ModelRouter.java` | 异步队列、CAS抢占、模型路由、结果同步 |
| [状态机与阶段流程](#状态机与阶段流程-statemachine) | `statemachine/` | `core/statemachine/ProjectStateMachine.java` `core/statemachine/PhaseFlowController.java` `core/statemachine/trigger/*` | 项目状态转换规则 + 阶段推进 + 触发器 |
| [文档集成与导出](#文档集成与导出-document) | `document/` | `core/service/impl/DocumentIntegrationServiceImpl.java` `ai/processor/generator/DocumentIntegration.java` | Markdown模板→变量替换→Word导出 |

---

## 业务需求编制 (requirement/)

**关键词**: 需求、requirement、需求生成、需求检测、自动保存、匹配模式、AUTO_MATCH、MANUAL_SELECT、UPLOAD

**核心概念**:
- **TbRequirement**: 业务需求实体，后端仅有 IN_PROGRESS / COMPLETED 两种状态（无状态机校验）
- **三种匹配模式**: AUTO_MATCH(自动匹配Top5) / MANUAL_SELECT(手动选择) / UPLOAD(上传文件)
- **自动保存机制**: 120秒定时保存草稿到 `auto_save_content` 字段，正式保存时清除
- **需求检测**: 仅2项（敏感词+错别字），无状态机，前端本地管理状态
- **需求→项目衔接**: 项目通过 `requirementId` 单向引用需求，进入需求阶段时复制内容到 `project.requirementContent`

**详细文档**: `requirement/overview.md`

---

## 项目编制五阶段 (project-lifecycle/)

**关键词**: 项目、project、编制向导、ProjectWizard、五阶段、BASIC_INFO、REQUIREMENT、REVIEW_ITEM、DOCUMENT、DETECTION、推进、advancePhase

**核心概念**:
- **五阶段顺序推进**: BASIC_INFO(1,20%) → REQUIREMENT(2,40%) → REVIEW_ITEM(3,60%) → DOCUMENT(4,80%) → DETECTION(5,100%)
- **只能顺序推进**: `PhaseFlowController.isValidTransition()` 校验 `target.code == current.code + 1`，不可跳跃不可倒退
- **阶段触发器**: 每个阶段注册一个 `PhaseTrigger`，onEnter 自动发起AI任务，canComplete 校验推进条件
- **活跃任务拦截**: 推进前检查 `aiTaskService.hasActiveTasks()`，有正在执行的AI任务时禁止推进
- **评审方式**: INTELLIGENT(智能评审，4类评审项) / MANUAL(人工评审，仅符合性审查)

**详细文档**: `project-lifecycle/overview.md`

---

## 智能检测 (detection/)

**关键词**: 检测、detection、敏感词、错别字、政策审查、格式检测、SENSITIVE_WORD、TYPO、POLICY_REVIEW、FORMAT_CHECK、检测建议、accept、reject、skip

**核心概念**:
- **需求检测 vs 项目检测**: 需求2项(敏感词+错别字)，项目4项(+政策审查+格式检测)
- **检测内容**: `buildDetectionContent()` 拼接 `requirementContent` + 评审项标准纯文本，存 `content_snapshot`，`content_file_id` 始终为 null
- **建议处理**: accept(自动修复Word文档) / reject(保持原样) / acceptAll(批量) → 全部处理后自动转 DETECTION_PASSED
- **跳过检测**: skip() 发警告通知到消息中心，仍可发布
- **重试机制**: retry() 走状态机 DETECTION_FAILED→IN_PROGRESS→PENDING_DETECTION→DETECTING

**详细文档**: `detection/overview.md`

---

## AI任务机制 (ai-task/)

**关键词**: AI任务、ai_task、AiTaskProcessor、PENDING、PROCESSING、COMPLETED、CAS抢占、模型路由、ModelRouter、结果同步、AiTaskResultSyncScheduler、超时检查、AiTaskTimeoutChecker、降级

**核心概念**:
- **异步解耦**: core 写入 ai_task 表 → ai 模块 AiTaskProcessor 5秒轮询 CAS 抢占 → core 10秒同步结果
- **9种任务类型**: 3生成(REQUIREMENT/PROJECT_REQUIREMENT/REVIEW_ITEM_GENERATE) + 4检测 + 1文档集成 + 1文本优化
- **CAS抢占**: `casUpdateStatus(id, "PENDING", "PROCESSING")` 利用数据库原子更新避免多实例竞争
- **模型路由**: AiTaskType → AiUsageScenario(GENERATION/OPTIMIZATION/DETECTION) → 查 sup_model_route_rule → primary/fallback
- **三态同步**: resultSynced=0(未同步) → 1(已同步) → 2(同步失败，下次重试)
- **降级策略**: 60秒超时标记 AI_UNAVAILABLE → 用户可重试或跳过(SKIPPED)

**详细文档**: `ai-task/overview.md`

---

## 状态机与阶段流程 (statemachine/)

**关键词**: 状态机、ProjectStateMachine、PhaseFlowController、PhaseTrigger、触发器、状态转换、阶段推进、onEnter、onExit、canComplete

**核心概念**:
- **ProjectStateMachine**: 静态 Map 定义合法状态转换，`transition()` 方法校验并执行转换，非法转换抛 BusinessException
- **PhaseFlowController**: 管理阶段推进流程（校验→onExit→更新阶段→onEnter→联动Status），注册5个触发器
- **PhaseTrigger接口**: onEnter(进入阶段自动发起AI任务) / onExit / canComplete(校验是否可推进) / getIncompleteMessage
- **Phase-Status联动**: 进入 DETECTION 阶段时自动 DRAFT/IN_PROGRESS → PENDING_DETECTION → DETECTING
- **终态不可逆**: ARCHIVED 和 CANCELLED 的 allowed transitions 为空 Set

**详细文档**: `statemachine/overview.md`

---

## 文档集成与导出 (document/)

**关键词**: 文档集成、document、DocumentIntegration、Markdown模板、变量替换、Word导出、poi-tl、flexmark、预览、generatedFileId

**核心概念**:
- **文档生成管线**: Markdown模板 → flexmark-java解析 → 变量替换({{projectName}}等) → HTML预览 / poi-tl填充Word → .docx导出
- **generatedFileId**: 导出的 Word 文件 ID 回写到 `project.generatedFileId`，后续检测和修复都基于此文件
- **文档修复**: 检测建议 accept 时调用 `fileServiceClient.fixDocument()` 基于原文精准定位替换，生成新文件ID
- **版本快照**: 发布时自动将项目内容(JSON)写入 `tb_project_version`，支持版本对比

**详细文档**: `document/overview.md`

---

## 如何使用本索引

1. **快速定位**：根据问题的关键词，找到上表对应的领域
2. **了解概念**：阅读该领域的"核心概念"部分，建立初步理解
3. **深入学习**：打开该领域的 `overview.md` 获取代码位置、业务规则、注意事项等详细信息
4. **跨领域关联**：注意领域间的依赖关系（如项目编制依赖状态机和AI任务，检测依赖AI任务和文档修复）
