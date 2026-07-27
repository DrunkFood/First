# 业务需求编制 (Requirement)

> 第一阶段：创建业务需求 → AI生成需求内容 → 人工编辑审核 → 需求智能检测(2项)

## 核心数据结构

**TbRequirement** (`common/entity/core/TbRequirement.java`)

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | String | 后端仅 `IN_PROGRESS` / `COMPLETED` 两种值，**无状态机校验** |
| `content` | text | 需求正文（Markdown格式） |
| `matchMode` | String | 匹配模式：AUTO_MATCH / MANUAL_SELECT / UPLOAD |
| `matchedFileId` | Long | 匹配的历史需求/文件ID |
| `matchedSimilarity` | decimal | 匹配度评分(0-100) |
| `autoSaveContent` | text | 自动保存的草稿内容 |
| `autoSaveTime` | datetime | 自动保存时间 |
| `uploadedFileId` | Long | 上传文件ID（matchMode=UPLOAD时） |
| `projectCategory` | String | 项目类别：SMALL_TRADE / GOVERNMENT_PROCUREMENT / COMPREHENSIVE_TRADE |
| `projectType` | String | 项目类型：ENGINEERING / GOODS / SERVICE |
| `budget` | decimal | 预算价（元） |

**代码位置**:
- Controller: `core/controller/RequirementController.java`
- Service: `core/service/impl/RequirementServiceImpl.java`
- 前端页面: `frontend/src/views/requirement/` (RequirementCreate / Generate / Editor / Detect / List)

## 需求状态流转

```
IN_PROGRESS（后端默认） → COMPLETED（检测通过后可设置）
```

> **重要陷阱**: 后端 `TbRequirement.status` 仅有 `IN_PROGRESS` 和 `COMPLETED` 两种值。前端 `status-maps.ts` 定义了5种状态(DRAFT/GENERATING/PENDING_REVIEW/APPROVED/REJECTED)用于UI展示，但**后端不识别**这些状态。需求状态通过 `update()` 接口自由设置，**无状态机校验**（与项目侧 `ProjectStateMachine` 不同）。

## 三种匹配模式

### AUTO_MATCH（自动匹配）
- 调用 `POST /api/v1/ai/match/auto` 返回 Top5 历史需求
- 评分算法：项目类型40分 + 项目类别20分 + 预算范围10分(固定给) + 关键词20分(项目名称占满20分，描述占一半10分)
- 评分代码在 AI 模块的 DocumentMatchController 中

### MANUAL_SELECT（手动选择）
- `GET /api/v1/requirements/match-files` 获取匹配文件列表
- `POST /api/v1/requirements/{id}/match` 确认匹配，写入 matchMode + matchedFileId

### UPLOAD（上传文件）
- 调用文件服务 `POST /file/upload` 上传本地文件
- 格式限制：doc/docx/pdf，大小 ≤ 50MB
- 上传成功后 `uploadedFileId` 写入需求记录

## AI生成需求内容

**任务类型**: `REQUIREMENT_GENERATE`（路由到 GENERATION 场景模型）

**三步式 Agent 编排**（`ai/processor/generator/RequirementGenerator.java`）:
1. **Step 1 - 生成大纲**: AI 输出 JSON 大纲结构
2. **Step 2 - 分章并行生成**: 并发3个虚拟线程，每章独立生成详细内容
3. **Step 3 - 审查修订**: AI 输出修订列表，代码层执行精确替换

**渐进式进度推送**: 通过 `ai_task.result` 的 `contentStage` 字段推送：
- `OUTLINE_GENERATED` → `CHAPTER_GENERATING` → `DRAFT_COMPLETED` → `REVIEWING` → `COMPLETED`

**硬约束**: 全文 ≤ 5000 字（`MAX_REQUIREMENT_TOTAL_WORDS = 5000`）

**通信方式**: 异步任务队列（前端3秒轮询 `GET /ai-tasks/{id}`），非SSE

> **已知问题**: 前端期望SSE流式返回，但后端 `submitGenerate` 创建的是异步轮询任务，非SSE端点。

## 自动保存机制

| 项目 | 说明 |
|------|------|
| 定时间隔 | 2分钟（`AUTO_SAVE_INTERVAL = 2 * 60 * 1000`） |
| 触发条件 | `content` 或 `requirementName` 不为空 |
| 存储字段 | `auto_save_content` + `auto_save_time` |
| 草稿恢复 | 页面加载时自动检查，弹窗确认 |
| 清除时机 | 正式保存(`PUT /{id}`)时自动清除 `autoSaveContent=null` |

**前端代码**: `RequirementEditor.vue` 中的 `doAutoSave()` 方法

## 需求智能检测

**检测项**: 仅2项
- `DETECTION_SENSITIVE_WORD`（敏感词检测）
- `DETECTION_TYPO`（错别字检测）

均路由到 `DETECTION` 场景模型。

**检测内容**: `requirement.content`（直接检测需求正文）

**建议处理**:
- `POST /{id}/detect/{recordId}/accept?issueIndex=N` — 接受建议
- `POST /{id}/detect/{recordId}/reject?issueIndex=N` — 拒绝建议
- `POST /{id}/detect/finish` — 完成检测

> 与项目检测不同：需求检测**无状态机**、**无重试机制**、**无跳过机制**，前端本地管理状态。

## 需求到项目的衔接

```
需求完成(COMPLETED) → 创建项目(关联requirementId) → 进入需求阶段时:
  ├─ 有关联需求 → 复制 requirement.content 到 project.requirementContent（requirementSource=REFERENCE）
  └─ 无关联需求 → 触发 PROJECT_REQUIREMENT_GENERATE AI任务（requirementSource=SYSTEM_GENERATE）
```

**关键约束**:
- 项目通过 `requirementId` **单向引用**需求（`tb_requirement` 无 `projectId`）
- 进入需求阶段后，项目和需求**再无关联**，后续阶段均从 `project.requirementContent` 读取
- 衔接逻辑在 `RequirementTrigger.onEnter()` 中实现

## AI辅助能力

| 能力 | 通信方式 | 接口 | 路由场景 |
|------|---------|------|---------|
| 需求内容生成 | 异步队列(3s轮询) | `POST /requirements/{id}/generate` | GENERATION |
| AI对话辅助 | SSE流式(60s超时) | `POST /api/v1/ai/chat` | — |
| AI文本优化 | SSE流式(60s超时) | `POST /api/v1/ai/optimize` | OPTIMIZATION |
| 文档自动匹配 | 同步HTTP | `POST /api/v1/ai/match/auto` | — |

## 注意事项

1. **前后端状态不一致**: 后端仅 IN_PROGRESS/COMPLETED，前端定义了5种状态用于UI，后端不识别 DRAFT/GENERATING 等
2. **无审核流程**: `PENDING_REVIEW → APPROVED/REJECTED` 的审核API和页面未实现
3. **预算匹配算法简化**: 评分中预算维度固定给10分，未实际比较金额
4. **GENERATING状态缺失**: 前端读取 GENERATING 判断是否允许提交审核，但后端未写入此状态
