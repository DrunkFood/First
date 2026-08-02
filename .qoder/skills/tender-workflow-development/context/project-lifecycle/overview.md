# 项目编制五阶段 (Project Lifecycle)

> 第二阶段：基础信息 → 招标需求 → 评审项 → 文档集成 → 智能检测

## 核心数据结构

**TbProject** (`common/entity/core/TbProject.java`)

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | String | 项目状态（受 ProjectStateMachine 管控） |
| `currentPhase` | Integer | 当前编制阶段 1-5 |
| `progress` | Integer | 进度百分比 0/20/40/60/80/100 |
| `requirementId` | Long | 关联需求ID（单向引用） |
| `requirementSource` | String | 需求来源：REFERENCE / SYSTEM_GENERATE |
| `requirementContent` | text | 招标需求内容（后续阶段从此读取） |
| `templateId` | Long | 模板ID |
| `generatedFileId` | Long | 导出的Word文件ID |
| `reviewType` | String | 评审方式：INTELLIGENT / MANUAL |
| `matchMode` | String | 匹配模式（同需求侧） |
| `matchedFileId` | Long | 匹配文件ID |
| `uploadedFileId` | Long | 上传文件ID |

**代码位置**:
- Controller: `core/controller/ProjectController.java`
- Service: `core/service/impl/ProjectServiceImpl.java`
- 阶段流程: `core/statemachine/PhaseFlowController.java`
- 触发器: `core/statemachine/trigger/` (5个Trigger)
- 前端向导: `frontend/src/views/project/ProjectWizard.vue`
- 前端阶段页: `frontend/src/views/project/phases/Phase*.vue`

## 五阶段总览

| 阶段 | 枚举 | 进度 | 项目状态 | 触发器 | canComplete条件 |
|------|------|------|---------|--------|----------------|
| 基础信息录入 | BASIC_INFO(1) | 20% | DRAFT→IN_PROGRESS | BasicInfoTrigger | 项目名称+类别+类型+评审方式已填 |
| 招标需求生成 | REQUIREMENT(2) | 40% | IN_PROGRESS | RequirementTrigger | requirementId不为空 或 requirementContent不为空 |
| 评审项设置 | REVIEW_ITEM(3) | 60% | IN_PROGRESS | ReviewItemTrigger | 项目下有评审项记录 |
| 文档集成 | DOCUMENT(4) | 80% | IN_PROGRESS | DocumentTrigger | generatedFileId不为空 |
| 智能检测 | DETECTION(5) | 100% | DETECTING | DetectionPhaseTrigger | 状态为 DETECTION_PASSED 或 DETECTION_SKIPPED |

## 阶段推进机制

**PhaseFlowController.advancePhase()** 流程（`core/statemachine/PhaseFlowController.java`）:

```
1. 校验转换规则（只能推进到下一阶段，target.code == current.code + 1）
2. 校验项目下无活跃AI任务（hasActiveTasks == false）
3. 执行当前阶段 onExit
4. 更新阶段和进度（currentPhase = target.code, progress = target.progressPercent）
5. 执行目标阶段 onEnter（自动发起AI任务等）
6. 联动更新项目状态（syncProjectStatus）
```

**关键约束**:
- **只能顺序推进**: 不可跳跃、不可倒退
- **活跃任务拦截**: 有正在执行的AI任务时禁止推进（抛 `PROJECT_PHASE_ACTIVE_TASK`）
- **触发器注册**: `ProjectServiceImpl.initPhaseTriggers()` 在 `@PostConstruct` 时注册5个触发器

## 各阶段详解

### 阶段1：基础信息录入（BASIC_INFO）

**触发器**: `BasicInfoTrigger.java`
- onEnter: 无自动操作
- canComplete: 校验项目名称、类别、类型、评审方式必填

**关键逻辑**: 项目类别 + 项目类型 → 自动匹配默认模板（`is_default=true`）

**前端**: `PhaseBasicInfo.vue` — 表单填写 + 模板选择 + 历史文件匹配

**推进后**: status: DRAFT → IN_PROGRESS, phase: → REQUIREMENT

### 阶段2：招标需求生成（REQUIREMENT）

**触发器**: `RequirementTrigger.java`
- onEnter:
  - 有关联需求(`requirementId != null`) → 复制 `requirement.content` 到 `project.requirementContent`
  - 无关联需求 → 自动触发 `PROJECT_REQUIREMENT_GENERATE` AI任务
- canComplete: `requirementId != null` 或 `requirementContent` 不为空

**AI任务**: `PROJECT_REQUIREMENT_GENERATE`（与 `REQUIREMENT_GENERATE` 共用 `RequirementGenerator.generate()`）

**前端**: `PhaseRequirement.vue` — AI生成 + 编辑器 + AI辅助对话

### 阶段3：评审项设置（REVIEW_ITEM）

**触发器**: `ReviewItemTrigger.java`
- onEnter: 自动触发 `REVIEW_ITEM_GENERATE` AI任务
- canComplete: 项目下有评审项记录（`selectByProjectId` 非空）

**评审项三级结构**:
```
根项(level=1) → COMPLIANCE/TECHNICAL/CREDIT/COMMERCIAL
  └── 子项(level=2) → 具体评审维度
        └── 孙项(level=3) → 细化评分标准
```

**评审方式差异**:
- INTELLIGENT(智能评审): 4类评审项（符合性+技术+资信+商务）
- MANUAL(人工评审): 仅符合性审查（COMPLIANCE）

**计分模式**: SCORE(分值模式，叶子分值合计=100) / WEIGHT(权重模式，类型间权重%合计=100%)

**客观/主观**: OBJECTIVE / SUBJECTIVE，由 `ReviewTypeConfig.distinguishSubjectivity` 控制

**前端**: `PhaseReviewItem.vue` — 评审项树形结构 + AI生成 + 手动编辑

### 阶段4：文档集成（DOCUMENT）

**触发器**: `DocumentTrigger.java`
- onEnter: 无自动操作（文档集成需用户手动触发）
- canComplete: `generatedFileId` 不为空

**文档生成管线**:
```
Markdown模板 → flexmark-java解析 → 变量替换({{projectName}}/{{budget}}等) → HTML预览
                                                                       ↓
                                                               poi-tl填充Word → .docx导出 → 文件服务存储 → fileId回写
```

**前端**: `PhaseDocument.vue` — 预览 + 编辑 + 导出

**推进后**: phase: → DETECTION, status: IN_PROGRESS → PENDING_DETECTION → DETECTING

### 阶段5：智能检测（DETECTION）

**触发器**: `DetectionPhaseTrigger.java`
- onEnter: 自动提交检测（携带 context 中的 policyFileIds）
- canComplete: 状态为 `DETECTION_PASSED` 或 `DETECTION_SKIPPED`

**4项并行检测**: 敏感词 + 错别字 + 政策审查 + 格式检测

> 详细机制参见 `detection/overview.md`

## 前端路由结构

```
/project                → ProjectList（项目列表）
/project/create         → ProjectCreate（新建项目）
/project/:id            → ProjectDetail（项目详情）
/project/:id/wizard     → ProjectWizard（编制向导，含5个阶段Tab）
```

**ProjectWizard.vue** 使用 `step` 查询参数控制当前阶段：
- `?step=0` → PhaseBasicInfo
- `?step=1` → PhaseRequirement
- `?step=2` → PhaseReviewItem
- `?step=3` → PhaseDocument
- `?step=4` → PhaseDetection

## 项目状态联动

`PhaseFlowController.syncProjectStatus()` 在阶段推进时联动更新项目状态:

| 阶段 | 预期状态 | 联动逻辑 |
|------|---------|---------|
| BASIC_INFO | IN_PROGRESS | DRAFT → IN_PROGRESS |
| REQUIREMENT | IN_PROGRESS | — |
| REVIEW_ITEM | IN_PROGRESS | — |
| DOCUMENT | IN_PROGRESS | — |
| DETECTION | DETECTING | IN_PROGRESS → PENDING_DETECTION → DETECTING |

## 注意事项

1. **推进前检查活跃任务**: `hasActiveTasks()` 会阻止推进，需等AI任务完成后再推进
2. **阶段不可倒退**: 如需回到上一阶段修改，需通过状态机的 `IN_PROGRESS` 回退机制（仅检测阶段支持）
3. **触发器可能失败**: onEnter 中的 AI 任务触发可能因已有活跃任务而失败，但不会阻止阶段推进（catch + warn）
4. **项目编号自动生成**: `generateProjectCode()` 格式为 `AI-{yyyyMMddHHmmss}-{随机6位数}`
5. **项目名称唯一性**: 创建和更新时都校验项目名称唯一
