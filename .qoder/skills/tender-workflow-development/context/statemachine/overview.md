# 状态机与阶段流程 (State Machine & Phase Flow)

> ProjectStateMachine（状态转换规则）+ PhaseFlowController（阶段推进）+ PhaseTrigger（阶段触发器）

## 核心组件关系

```
ProjectStateMachine          PhaseFlowController           PhaseTrigger(接口)
  ├─ 状态转换规则 Map          ├─ 阶段触发器注册表            ├─ onEnter(进入阶段)
  ├─ transition() 校验+执行    ├─ advancePhase() 推进流程     ├─ onExit(离开阶段)
  └─ canTransition() 检查      ├─ canAdvance() 检查           ├─ canComplete(完成校验)
                              ├─ validateTransition()        └─ getIncompleteMessage()
                              └─ syncProjectStatus() 联动
                                     │
                              ┌──────┴──────┐
                              │  5个触发器   │
                              ├─────────────┤
                              │ BasicInfoTrigger    │
                              │ RequirementTrigger  │
                              │ ReviewItemTrigger   │
                              │ DocumentTrigger     │
                              │ DetectionPhaseTrigger│
                              └─────────────┘
```

**代码位置**:
- 状态机: `core/statemachine/ProjectStateMachine.java`
- 阶段流程控制器: `core/statemachine/PhaseFlowController.java`
- 触发器接口: `core/statemachine/PhaseTrigger.java`
- 触发器实现: `core/statemachine/trigger/` (5个)
- 枚举: `common/enums/ProjectStatus.java` / `common/enums/ProjectPhase.java`

## ProjectStateMachine（项目状态机）

**文件**: `core/statemachine/ProjectStateMachine.java`

静态 Map 定义合法状态转换规则:

| 当前状态 | 允许转换到 |
|---------|-----------|
| DRAFT | IN_PROGRESS, CANCELLED |
| IN_PROGRESS | PENDING_DETECTION, CANCELLED |
| PENDING_DETECTION | DETECTING, IN_PROGRESS, CANCELLED |
| DETECTING | DETECTION_PASSED, DETECTION_FAILED, DETECTION_SKIPPED |
| DETECTION_PASSED | PUBLISHED, IN_PROGRESS |
| DETECTION_FAILED | IN_PROGRESS, DETECTION_PASSED |
| DETECTION_SKIPPED | PUBLISHED, IN_PROGRESS |
| PUBLISHED | ARCHIVED |
| **ARCHIVED** | **(空，终态不可逆)** |
| **CANCELLED** | **(空，终态不可逆)** |

**核心方法**:
- `transition(project, target)` — 校验并执行状态转换，非法转换抛 `BusinessException(PROJECT_STATUS_ERROR)`
- `canTransition(currentCode, target)` — 检查转换是否合法，返回 boolean

> **重要**: ARCHIVED 和 CANCELLED 是终态，其 allowed transitions 为空 Set，任何转换都会抛异常。

## PhaseFlowController（阶段流程控制器）

**文件**: `core/statemachine/PhaseFlowController.java`

### advancePhase() 推进流程

```
advancePhase(project, target, context):
  1. 校验 currentPhase 不为空
  2. 校验转换规则: isValidTransition(current, target)
     → target.code == current.code + 1（只能推进到下一阶段）
  3. 校验无活跃AI任务: aiTaskService.hasActiveTasks(projectId)
     → 有活跃任务则抛 PROJECT_PHASE_ACTIVE_TASK
  4. 执行当前阶段 onExit:
     - currentTrigger.canComplete(project) → 不满足则抛 PROJECT_PHASE_ERROR
     - currentTrigger.onExit(project)
  5. 更新阶段和进度:
     - project.currentPhase = target.code
     - project.progress = target.progressPercent (code * 20)
  6. 执行目标阶段 onEnter:
     - targetTrigger.onEnter(project, context) → 自动发起AI任务等
  7. 联动更新项目状态: syncProjectStatus(project, target)
```

### canAdvance() 检查

```
canAdvance(project, target):
  1. isValidTransition(current, target) → 必须是下一阶段
  2. currentTrigger == null || currentTrigger.canComplete(project) → 当前阶段可完成
```

### syncProjectStatus() 状态联动

Phase → Status 联动映射:

| 阶段 | 预期状态 | 联动逻辑 |
|------|---------|---------|
| BASIC_INFO | IN_PROGRESS | DRAFT → IN_PROGRESS |
| REQUIREMENT | IN_PROGRESS | — |
| REVIEW_ITEM | IN_PROGRESS | — |
| DOCUMENT | IN_PROGRESS | — |
| DETECTION | DETECTING | IN_PROGRESS → PENDING_DETECTION → DETECTING |

> **特殊处理**: 进入 DETECTION 阶段时，如果状态已被 onEnter 触发器（DetectionPhaseTrigger）设为 DETECTING，则跳过状态联动。

## PhaseTrigger 接口

**文件**: `core/statemachine/PhaseTrigger.java`

```java
public interface PhaseTrigger {
    // 进入阶段时执行（自动发起AI任务等）
    default void onEnter(TbProject project, Map<String, Object> context) {}

    // 离开阶段时执行
    default void onExit(TbProject project) {}

    // 检查当前阶段是否可以完成（是否满足推进条件）
    boolean canComplete(TbProject project);

    // 不能完成时的提示信息
    default String getIncompleteMessage() { return "当前阶段尚未完成"; }
}
```

## 5个触发器详解

### 1. BasicInfoTrigger
**文件**: `core/statemachine/trigger/BasicInfoTrigger.java`
- **onEnter**: 无操作
- **canComplete**: 校验项目名称、类别、类型、评审方式必填
- **提示**: "请完善项目基础信息"

### 2. RequirementTrigger
**文件**: `core/statemachine/trigger/RequirementTrigger.java`
- **onEnter**:
  - 有关联需求(`requirementId != null`) → 复制 `requirement.content` 到 `project.requirementContent`
  - 无关联需求 → 自动触发 `PROJECT_REQUIREMENT_GENERATE` AI任务
- **canComplete**: `requirementId != null` 或 `requirementContent` 不为空
- **提示**: "请先完成招标需求生成或手动填写需求内容"

### 3. ReviewItemTrigger
**文件**: `core/statemachine/trigger/ReviewItemTrigger.java`
- **onEnter**: 自动触发 `REVIEW_ITEM_GENERATE` AI任务
- **canComplete**: 项目下有评审项记录（`selectByProjectId` 非空）
- **提示**: "请先设置评审项"

### 4. DocumentTrigger
**文件**: `core/statemachine/trigger/DocumentTrigger.java`
- **onEnter**: 无操作（文档集成需用户手动触发）
- **canComplete**: `generatedFileId` 不为空
- **提示**: "请先完成文档集成"

### 5. DetectionPhaseTrigger
**文件**: `core/statemachine/trigger/DetectionPhaseTrigger.java`
- **onEnter**: 自动提交检测（从 context 提取 policyFileIds，调用 `detectionService.submit()`）
- **canComplete**: 状态为 `DETECTION_PASSED` 或 `DETECTION_SKIPPED`
- **提示**: "请等待智能检测完成或跳过检测"

## 触发器注册

**ProjectServiceImpl.initPhaseTriggers()** (`@PostConstruct`):

```java
phaseFlowController.registerTrigger(ProjectPhase.BASIC_INFO, basicInfoTrigger);
phaseFlowController.registerTrigger(ProjectPhase.REQUIREMENT, requirementTrigger);
phaseFlowController.registerTrigger(ProjectPhase.REVIEW_ITEM, reviewItemTrigger);
phaseFlowController.registerTrigger(ProjectPhase.DOCUMENT, documentTrigger);
phaseFlowController.registerTrigger(ProjectPhase.DETECTION, detectionPhaseTrigger);
```

> **注意**: 触发器使用 `@Lazy` 注入避免循环依赖。

## 关键枚举

### ProjectStatus (`common/enums/ProjectStatus.java`)
```java
DRAFT("DRAFT", "草稿")
IN_PROGRESS("IN_PROGRESS", "编制中")
PENDING_DETECTION("PENDING_DETECTION", "待检测")
DETECTING("DETECTING", "检测中")
DETECTION_PASSED("DETECTION_PASSED", "检测通过")
DETECTION_FAILED("DETECTION_FAILED", "检测未通过")
DETECTION_SKIPPED("DETECTION_SKIPPED", "已跳过检测")
PUBLISHED("PUBLISHED", "已发布")
ARCHIVED("ARCHIVED", "已归档")  // 终态
CANCELLED("CANCELLED", "已取消")  // 终态
```

### ProjectPhase (`common/enums/ProjectPhase.java`)
```java
BASIC_INFO(1, "基础信息录入")      // progress=20%
REQUIREMENT(2, "需求生成")         // progress=40%
REVIEW_ITEM(3, "评审项设置")       // progress=60%
DOCUMENT(4, "文档集成")            // progress=80%
DETECTION(5, "智能检测")           // progress=100%
// getProgressPercent() = code * 20
```

## 注意事项

1. **只能顺序推进**: `isValidTransition()` 校验 `target.code == current.code + 1`，不可跳跃不可倒退
2. **活跃任务拦截**: 推进前必须等AI任务完成，否则抛 `PROJECT_PHASE_ACTIVE_TASK`
3. **触发器失败不阻断**: onEnter 中的 AI 任务触发失败（catch + warn），不会阻止阶段推进
4. **DETECTION阶段特殊**: onEnter 会自动提交检测并走状态机 PENDING_DETECTION → DETECTING
5. **DETECTION_FAILED 可回退**: 可通过 `IN_PROGRESS` 回退后重新检测
6. **终态不可逆**: ARCHIVED 和 CANCELLED 无法再转换到任何状态
7. **需求侧无状态机**: TbRequirement 的 status 无状态机管控，通过 `update()` 自由设置（与项目侧不同）
