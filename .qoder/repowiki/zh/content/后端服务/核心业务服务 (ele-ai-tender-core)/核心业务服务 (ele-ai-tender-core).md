# 核心业务服务 (ele-ai-tender-core)

<cite>
**本文引用的文件**   
- [CORE_MODULE_SPEC.md](file://docs/rules/CORE_MODULE_SPEC.md)
- [PHASE_FLOW_SPEC.md](file://docs/rules/PHASE_FLOW_SPEC.md)
- [CoreApplication.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/CoreApplication.java)
- [ProjectController.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/ProjectController.java)
- [ProjectServiceImpl.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectServiceImpl.java)
- [PhaseFlowController.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/PhaseFlowController.java)
- [ProjectStateMachine.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/ProjectStateMachine.java)
- [DocumentDataAssembler.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/engine/DocumentDataAssembler.java)
- [WordTemplateEngine.java](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/engine/WordTemplateEngine.java)
- [IProjectVersionService.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IProjectVersionService.java)
- [ProjectVersionServiceImpl.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectVersionServiceImpl.java)
- [pom.xml](file://ele-ai-tender-system/ele-ai-tender-core/pom.xml)
- [父级pom.xml](file://ele-ai-tender-system/pom.xml)
- [ExternalAiTaskController.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java)
- [ExternalAiTaskService.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java)
</cite>

## 更新摘要
**变更内容**   
- 更新了服务层架构章节，详细说明MyBatis-Plus IService统一扩展和ServiceImpl继承的重构成果
- 新增了服务接口标准化设计模式分析
- 完善了代码一致性和可维护性提升的技术说明
- 增强了服务层重构对现有架构的影响评估

## 目录
1. [简介](#简介)
2. [项目结构](#项目结构)
3. [核心组件](#核心组件)
4. [架构总览](#架构总览)
5. [详细组件分析](#详细组件分析)
6. [依赖关系分析](#依赖关系分析)
7. [性能考虑](#性能考虑)
8. [故障排查指南](#故障排查指南)
9. [结论](#结论)
10. [附录](#附录)

## 简介
本技术文档聚焦于核心业务服务 ele-ai-tender-core，围绕项目管理、文档版本控制、业务流程编排等关键能力展开。重点说明：
- 状态机模式在项目生命周期管理中的应用（阶段推进、触发器机制、事件驱动）
- 文档集成引擎与模板引擎协作流程（数据组装、Word 生成）
- 服务间通信、事务管理与异步任务处理的关键实现
- **优化**：采用集中式依赖版本管理，提升可维护性和构建稳定性
- **新增**：服务层架构重构，统一扩展MyBatis-Plus IService，实现类继承ServiceImpl，显著提升代码一致性和可维护性
- 性能优化策略与常见问题的排障方法

## 项目结构
核心模块采用分层与领域划分相结合的组织方式：
- 启动与装配：Spring Boot 启动类、Mapper 扫描、调度开关
- 控制器层：对外暴露 /api/v1 接口，包含外部API接口
- 服务层：业务编排、状态机协调、AI 任务编排、外部系统集成，**已统一采用MyBatis-Plus标准架构**
- 状态机：阶段流程控制器 + 项目状态机 + 各阶段触发器
- 引擎层：文档数据组装器、模板渲染（与 file 模块协作）
- 版本控制：项目版本快照服务

```mermaid
graph TB
subgraph "核心服务"
APP["CoreApplication<br/>启动与装配"]
CTRL["ProjectController<br/>项目接口"]
EXT_CTRL["ExternalAiTaskController<br/>外部API接口"]
SVC["ProjectServiceImpl<br/>业务编排(ServiceImpl)"]
EXT_SVC["ExternalAiTaskService<br/>外部任务适配(ServiceImpl)"]
FSM["PhaseFlowController<br/>阶段流程控制器"]
SM["ProjectStateMachine<br/>状态机"]
ASSEMBLER["DocumentDataAssembler<br/>文档数据组装"]
VER_SVC["IProjectVersionService / ProjectVersionServiceImpl<br/>版本快照(ServiceImpl)"]
end
subgraph "外部协作"
AI["AI 模块(AiTask)<br/>异步生成"]
FILE["File 模块(WordTemplateEngine)<br/>模板渲染"]
DB["数据库(tb_*)"]
COMMON["ele-ai-tender-common<br/>统一版本管理"]
INTERACTION["ele-ai-tender-common-interaction<br/>统一版本管理"]
MP["MyBatis-Plus<br/>IService/ServiceImpl"]
end
APP --> CTRL
APP --> EXT_CTRL
CTRL --> SVC
EXT_CTRL --> EXT_SVC
SVC --> FSM
SVC --> SM
SVC --> AI
SVC --> VER_SVC
SVC --> ASSEMBLER
EXT_SVC --> COMMON
EXT_SVC --> INTERACTION
ASSEMBLER --> FILE
SVC --> DB
VER_SVC --> MP
SVC --> MP
EXT_SVC --> MP
```

**图表来源**
- [CoreApplication.java:12-15](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/CoreApplication.java#L12-L15)
- [ExternalAiTaskController.java:21-24](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L21-L24)
- [ExternalAiTaskService.java:31-33](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L31-L33)

**章节来源**
- [CORE_MODULE_SPEC.md:1-358](file://docs/rules/CORE_MODULE_SPEC.md#L1-L358)
- [PHASE_FLOW_SPEC.md:58-70](file://docs/rules/PHASE_FLOW_SPEC.md#L58-L70)

## 核心组件
- 项目服务：负责项目创建、更新、删除、分页查询、阶段推进、状态变更、需求生成任务提交等，**已重构为继承ServiceImpl的标准架构**
- 外部AI任务服务：提供外部系统集成的AI任务管理能力，支持签名认证和数据隔离，**采用统一的IService接口规范**
- 阶段流程控制器：注册并执行阶段触发器，校验阶段转换规则，联动项目状态
- 项目状态机：定义合法的状态转换集合，提供转换与合法性检查
- 文档数据组装器：聚合项目基础信息、需求内容、评审项，输出结构化 FillData 供模板渲染
- Word 模板引擎：基于 poi-tl 的模板填充，支持 Markdown 渲染与修订标记清理
- 版本快照服务：按项目维度创建版本快照，记录关键内容变化，**遵循MyBatis-Plus标准服务架构**

**章节来源**
- [ProjectServiceImpl.java:1-340](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectServiceImpl.java#L1-L340)
- [ExternalAiTaskService.java:27-33](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L27-L33)
- [PhaseFlowController.java:1-176](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/PhaseFlowController.java#L1-L176)
- [ProjectStateMachine.java:1-60](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/ProjectStateMachine.java#L1-L60)
- [DocumentDataAssembler.java:1-267](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/engine/DocumentDataAssembler.java#L1-L267)
- [WordTemplateEngine.java:1-121](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/engine/WordTemplateEngine.java#L1-L121)
- [IProjectVersionService.java:1-22](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IProjectVersionService.java#L1-L22)
- [ProjectVersionServiceImpl.java:38-65](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectVersionServiceImpl.java#L38-L65)

## 架构总览
核心服务通过控制器接收请求，交由服务层进行业务编排；阶段推进由状态机与触发器协同完成；文档生成由数据组装器与模板引擎协作；AI 任务以异步方式解耦到 AI 模块；版本快照在关键节点持久化。**新增的外部API接口支持第三方系统集成，提供严格的数据隔离和权限控制**。服务层已全面采用MyBatis-Plus标准架构，提升了代码的一致性和可维护性。

```mermaid
sequenceDiagram
participant Client as "客户端"
participant ExtClient as "外部系统"
participant Controller as "ProjectController"
participant ExtController as "ExternalAiTaskController"
participant Service as "ProjectServiceImpl(ServiceImpl)"
participant ExtService as "ExternalAiTaskService(ServiceImpl)"
participant PhaseCtrl as "PhaseFlowController"
participant StateMachine as "ProjectStateMachine"
participant AI as "AI 模块(AiTask)"
participant VerSvc as "ProjectVersionServiceImpl(ServiceImpl)"
participant Assembler as "DocumentDataAssembler"
participant FileEng as "WordTemplateEngine"
participant MP as "MyBatis-Plus IService"
Client->>Controller : "PUT /projects/{id}/phase"
Controller->>Service : "advancePhase(id, targetPhase, context)"
Service->>PhaseCtrl : "advancePhase(project, target, context)"
PhaseCtrl->>PhaseCtrl : "校验阶段转换/活跃任务"
PhaseCtrl->>PhaseCtrl : "执行当前阶段onExit"
PhaseCtrl->>PhaseCtrl : "更新阶段+进度"
PhaseCtrl->>PhaseCtrl : "执行目标阶段onEnter"
PhaseCtrl->>StateMachine : "syncProjectStatus(联动状态)"
Service-->>Controller : "成功"
Note over Service,AI : "进入需求阶段时可能触发AI生成任务"
Service->>AI : "createTask(PROJECT_REQUIREMENT_GENERATE, ...)"
Note over Service,VerSvc : "导出或发布前可创建版本快照"
Service->>VerSvc : "createVersion(projectId, changeDescription)"
Note over Service,Assembler : "文档集成前组装数据"
Service->>Assembler : "assemble(projectId)"
Assembler->>FileEng : "render(templateStream, data, markdownKeys)"
ExtClient->>ExtController : "POST /api/external/ai-tasks"
ExtController->>ExtService : "createTask(appKey, request)"
ExtService->>ExtService : "验证appKey/参数校验"
ExtService->>AI : "创建内部AI任务"
ExtService->>ExtService : "创建回调记录(数据隔离)"
Note over Service,MP : "所有服务实现继承ServiceImpl，使用IService接口"
```

**图表来源**
- [ProjectController.java:112-118](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/ProjectController.java#L112-L118)
- [ExternalAiTaskController.java:29-35](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L29-L35)
- [ExternalAiTaskService.java:52-105](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L52-L105)
- [PhaseFlowController.java:57-96](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/PhaseFlowController.java#L57-L96)
- [ProjectStateMachine.java:37-45](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/ProjectStateMachine.java#L37-L45)
- [ProjectVersionServiceImpl.java:48-65](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectVersionServiceImpl.java#L48-L65)
- [DocumentDataAssembler.java:47-99](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/engine/DocumentDataAssembler.java#L47-L99)
- [WordTemplateEngine.java:42-63](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/engine/WordTemplateEngine.java#L42-L63)

## 详细组件分析

### 服务层架构重构 - MyBatis-Plus标准化
**新增** 服务层已完成全面的架构重构，所有服务接口统一扩展MyBatis-Plus IService，实现类继承ServiceImpl，显著提升了代码一致性和可维护性：

#### 统一接口规范
- 所有服务接口继承 `com.baomidou.mybatisplus.extension.service.IService<T>`
- 所有服务实现类继承 `com.baomidou.mybatisplus.extension.service.impl.ServiceImpl<M, T>`
- 自动获得CRUD操作、分页查询、批量操作等通用能力
- 减少样板代码，提高开发效率

#### 架构优势
- **代码一致性**：所有服务遵循相同的架构模式，便于团队协作
- **功能增强**：利用MyBatis-Plus提供的丰富功能，如条件构造器、Lambda表达式
- **维护性提升**：标准化的接口设计降低学习成本和维护难度
- **扩展性增强**：便于添加自定义方法和拦截器

```mermaid
classDiagram
class IService~T~ {
<<interface>>
+getById(id) T
+list() T[]
+save(entity) boolean
+updateById(entity) boolean
+removeById(id) boolean
}
class ServiceImpl~M,T~ {
<<abstract class>>
protected M baseMapper
+getById(id) T
+list() T[]
+save(entity) boolean
+updateById(entity) boolean
+removeById(id) boolean
}
class IProjectVersionService {
<<interface>>
+getByProjectId(projectId) TbProjectVersion[]
+createVersion(projectId, changeDescription) TbProjectVersion
}
class ProjectVersionServiceImpl {
+getByProjectId(projectId) TbProjectVersion[]
+createVersion(projectId, changeDescription) TbProjectVersion
}
IProjectVersionService --|> IService~TbProjectVersion~
ProjectVersionServiceImpl --|> ServiceImpl~TbProjectVersionMapper, TbProjectVersion~
```

**图表来源**
- [IProjectVersionService.java:1-22](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IProjectVersionService.java#L1-L22)
- [ProjectVersionServiceImpl.java:38-65](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectVersionServiceImpl.java#L38-L65)

#### 重构影响评估
- **向后兼容**：现有调用方无需修改，接口行为保持一致
- **性能优化**：利用MyBatis-Plus的缓存和连接池优化
- **测试友好**：标准化的接口便于Mock和单元测试
- **监控增强**：便于集成统一的日志记录和性能监控

**章节来源**
- [IProjectVersionService.java:1-22](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IProjectVersionService.java#L1-L22)
- [ProjectVersionServiceImpl.java:38-65](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectVersionServiceImpl.java#L38-L65)

### 项目状态机与阶段流程
- 状态机定义合法的状态转换集合，禁止非法跳转
- 阶段流程控制器维护阶段触发器映射，顺序推进阶段，并在进入/离开阶段时执行钩子逻辑
- 阶段与状态联动：进入编制阶段→IN_PROGRESS，进入检测阶段→DETECTING（中间经 PENDING_DETECTION）

```mermaid
classDiagram
class ProjectStateMachine {
+transition(project, target) void
+canTransition(currentStatusCode, target) boolean
}
class PhaseFlowController {
-Map~ProjectPhase, PhaseTrigger~ triggers
+registerTrigger(phase, trigger) void
+advancePhase(project, target, context) void
+canAdvance(project, target) boolean
-validateTransition(current, target) void
-isValidTransition(current, target) boolean
-syncProjectStatus(project, target) void
}
class ProjectServiceImpl {
+advancePhase(projectId, targetPhase, context) void
+changeStatus(projectId, targetStatus) void
+generateRequirement(projectId) AiTask
}
ProjectServiceImpl --> PhaseFlowController : "调用"
PhaseFlowController --> ProjectStateMachine : "联动状态"
```

**图表来源**
- [ProjectStateMachine.java:19-45](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/ProjectStateMachine.java#L19-L45)
- [PhaseFlowController.java:24-96](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/PhaseFlowController.java#L24-L96)
- [ProjectServiceImpl.java:232-278](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectServiceImpl.java#L232-L278)

**章节来源**
- [ProjectStateMachine.java:1-60](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/ProjectStateMachine.java#L1-L60)
- [PhaseFlowController.java:1-176](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/PhaseFlowController.java#L1-L176)
- [ProjectServiceImpl.java:1-340](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectServiceImpl.java#L1-L340)
- [PHASE_FLOW_SPEC.md:58-70](file://docs/rules/PHASE_FLOW_SPEC.md#L58-L70)

### 外部AI任务集成服务
**新增** 外部AI任务集成服务为第三方系统提供安全的AI任务管理能力：
- 基于AppKey的签名认证机制
- 严格的数据隔离，确保外部系统只能访问自己的任务
- 完整的任务生命周期管理，包括创建、查询、状态跟踪
- 自动化的回调记录和重试机制
- **采用统一的IService接口规范，提升代码一致性**

```mermaid
flowchart TD
A["外部系统调用"] --> B["ExternalAiTaskController"]
B --> C["ExternalAiTaskService(ServiceImpl)"]
C --> D["验证AppKey和权限"]
D --> E["参数校验和类型转换"]
E --> F["创建内部AI任务"]
F --> G["创建回调记录(数据隔离)"]
G --> H["返回任务信息"]
H --> I["外部系统获取结果"]
```

**图表来源**
- [ExternalAiTaskController.java:29-51](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L29-L51)
- [ExternalAiTaskService.java:52-105](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L52-L105)

**章节来源**
- [ExternalAiTaskController.java:1-59](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L1-L59)
- [ExternalAiTaskService.java:1-166](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L1-L166)

### 项目生命周期与阶段推进时序
- 控制器接收阶段推进请求
- 服务层加载项目实体，委托阶段流程控制器执行推进
- 控制器返回结果给前端

```mermaid
sequenceDiagram
participant C as "客户端"
participant Ctrl as "ProjectController"
participant Svc as "ProjectServiceImpl(ServiceImpl)"
participant Flow as "PhaseFlowController"
participant SM as "ProjectStateMachine"
C->>Ctrl : "PUT /projects/{id}/phase"
Ctrl->>Svc : "advancePhase(id, targetPhase, context)"
Svc->>Flow : "advancePhase(project, target, context)"
Flow->>Flow : "校验转换/活跃任务/onExit"
Flow->>SM : "syncProjectStatus(必要时)"
Flow-->>Svc : "推进完成"
Svc-->>Ctrl : "保存项目更新"
Ctrl-->>C : "200 OK"
```

**图表来源**
- [ProjectController.java:112-118](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/ProjectController.java#L112-L118)
- [ProjectServiceImpl.java:232-239](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectServiceImpl.java#L232-L239)
- [PhaseFlowController.java:57-96](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/PhaseFlowController.java#L57-L96)
- [ProjectStateMachine.java:37-45](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/ProjectStateMachine.java#L37-L45)

### 文档集成与模板渲染流程
- 数据组装器从项目、评审项、模板配置中收集数据，构建 FillData 列表
- 模板引擎使用 poi-tl 渲染文本、图片与 Markdown，表格由 TableGenerator 处理
- 生成完成后固化版本快照

```mermaid
flowchart TD
Start(["开始"]) --> LoadProj["加载项目与评审项"]
LoadProj --> BuildFill["构建 FillData 列表"]
BuildFill --> Render["WordTemplateEngine.render()"]
Render --> SaveVer["创建版本快照"]
SaveVer --> End(["结束"])
```

**图表来源**
- [DocumentDataAssembler.java:47-99](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/engine/DocumentDataAssembler.java#L47-L99)
- [WordTemplateEngine.java:42-63](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/engine/WordTemplateEngine.java#L42-L63)
- [ProjectVersionServiceImpl.java:48-65](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectVersionServiceImpl.java#L48-L65)

### 项目版本控制
- 版本快照服务根据项目 ID 获取历史版本
- 创建版本时序列化关键内容快照（如生成的文件ID、状态），递增版本号并持久化
- **采用MyBatis-Plus IService接口，提供标准的CRUD操作**

```mermaid
classDiagram
class IProjectVersionService {
+getByProjectId(projectId) TbProjectVersion[]
+createVersion(projectId, changeDescription) TbProjectVersion
}
class ProjectVersionServiceImpl {
+getByProjectId(projectId) TbProjectVersion[]
+createVersion(projectId, changeDescription) TbProjectVersion
}
IProjectVersionService <|.. ProjectVersionServiceImpl
```

**图表来源**
- [IProjectVersionService.java:1-22](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IProjectVersionService.java#L1-L22)
- [ProjectVersionServiceImpl.java:38-65](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectVersionServiceImpl.java#L38-L65)

**章节来源**
- [IProjectVersionService.java:1-22](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IProjectVersionService.java#L1-L22)
- [ProjectVersionServiceImpl.java:38-65](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectVersionServiceImpl.java#L38-L65)

### 复杂逻辑流程图：评审项汇总表格构建
- 按评审类型分组（资信标、技术标、商务标）
- 计算类别总分与权重标签
- 扁平化叶子节点，生成表格行数据

```mermaid
flowchart TD
A["开始"] --> B["读取评审项并按类型分组"]
B --> C{"是否启用主观/客观区分?"}
C --> |是| D["计算类别总分/权重标签"]
C --> |否| E["仅计算类别总分"]
D --> F["遍历一级节点，递归扁平化子节点"]
E --> F
F --> G["生成表格行(类别名/标准/满分/属性/目录)"]
G --> H["结束"]
```

**图表来源**
- [DocumentDataAssembler.java:170-214](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/engine/DocumentDataAssembler.java#L170-L214)
- [DocumentDataAssembler.java:216-236](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/engine/DocumentDataAssembler.java#L216-L236)

**章节来源**
- [DocumentDataAssembler.java:1-267](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/engine/DocumentDataAssembler.java#L1-L267)

## 依赖关系分析
- 控制器依赖服务层，服务层依赖状态机与触发器、AI 任务服务、版本服务、数据组装器
- 数据组装器依赖评审项 Mapper 与项目模板服务
- 模板引擎位于 file 模块，core 模块通过数据组装器与其协作
- **优化**：采用集中式依赖版本管理，所有内部模块依赖都通过父POM的dependencyManagement统一管理
- **优化**：common 和 common-interaction 模块作为核心依赖，提供统一的DTO、枚举和工具类
- **新增**：所有服务层统一依赖MyBatis-Plus框架，提供标准化的数据访问能力

```mermaid
graph LR
PCtrl["ProjectController"] --> PSvc["ProjectServiceImpl(ServiceImpl)"]
EXT_CTRL["ExternalAiTaskController"] --> EXT_SVC["ExternalAiTaskService(ServiceImpl)"]
PSvc --> PFlow["PhaseFlowController"]
PSvc --> PState["ProjectStateMachine"]
PSvc --> AI["AiTask 服务"]
PSvc --> Ver["ProjectVersionServiceImpl(ServiceImpl)"]
PSvc --> Asm["DocumentDataAssembler"]
EXT_SVC --> COMMON["ele-ai-tender-common<br/>集中版本管理"]
EXT_SVC --> INTERACTION["ele-ai-tender-common-interaction<br/>集中版本管理"]
Asm --> WTE["WordTemplateEngine(file)"]
PSvc --> MP["MyBatis-Plus IService/ServiceImpl"]
EXT_SVC --> MP
Ver --> MP
```

**图表来源**
- [ProjectController.java:98-134](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/ProjectController.java#L98-L134)
- [ExternalAiTaskController.java:21-24](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L21-L24)
- [ExternalAiTaskService.java:11-13](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L11-L13)
- [ProjectServiceImpl.java:1-340](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectServiceImpl.java#L1-L340)
- [PhaseFlowController.java:1-176](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/PhaseFlowController.java#L1-L176)
- [ProjectStateMachine.java:1-60](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/statemachine/ProjectStateMachine.java#L1-L60)
- [DocumentDataAssembler.java:1-267](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/engine/DocumentDataAssembler.java#L1-L267)
- [WordTemplateEngine.java:1-121](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/engine/WordTemplateEngine.java#L1-L121)

**章节来源**
- [CORE_MODULE_SPEC.md:1-358](file://docs/rules/CORE_MODULE_SPEC.md#L1-L358)

### 集中式依赖版本管理优化
**优化** 核心服务实现了集中式的依赖版本管理机制：

#### 统一版本管理
- `ele-ai-tender-common` 和 `ele-ai-tender-common-interaction` 依赖不再显式声明版本号
- 所有内部模块版本由父POM的dependencyManagement统一管理
- 使用`${project.version}`变量确保版本一致性

#### 架构优势
- **消除版本冲突**：避免不同模块引入不同版本的相同依赖
- **简化维护**：只需在父POM中修改一次版本即可影响所有子模块
- **提高可重复性**：确保构建环境的稳定性和可重现性
- **增强可追溯性**：清晰的版本依赖关系便于问题定位

#### 构建稳定性提升
- 减少Maven依赖解析的不确定性
- 支持CI/CD流水线的稳定执行
- 便于多环境的一致性部署

**章节来源**
- [pom.xml:21-28](file://ele-ai-tender-system/ele-ai-tender-core/pom.xml#L21-L28)
- [父级pom.xml:78-123](file://ele-ai-tender-system/pom.xml#L78-L123)

## 性能考虑
- 阶段推进前检查活跃 AI 任务，避免并发冲突与资源争用
- 文档渲染前清理修订标记，减少 poi-tl 重构时的异常与回退开销
- 评审项汇总采用流式分组与扁平化遍历，降低内存占用
- 版本快照仅在关键节点创建，避免频繁写入
- 建议对大文档渲染与 AI 任务执行采用异步与限流策略（参考 AI 模块线程池配置）
- **优化**：外部API调用增加缓存和限流保护，防止恶意请求影响系统性能
- **新增**：MyBatis-Plus连接池优化，合理使用缓存和批量操作提升性能

## 故障排查指南
- 项目状态异常：检查 tb_project.status 与状态机转换规则，确认是否允许该转换
- 阶段推进失败：查看阶段流程日志，确认是否存在活跃 AI 任务或当前阶段未完成
- 需求阶段内容为空：核对 tb_project.requirement_id 是否为空；若为空，检查对应 AI 任务状态
- AI 生成失败：检查模型配置与 Token 用量上限，关注任务终态与结果同步标志
- 评审项结构错误：检查 parent_id 与 level 层级关系是否符合三级结构约束
- 检测相关问题：参考检测全链路规范定位问题
- **优化**：外部API调用失败：检查AppKey配置和权限设置，确认回调URL可达性
- **优化**：依赖版本冲突：检查Maven依赖树，确认没有版本不一致的依赖引入
- **优化**：构建失败：验证本地Maven仓库中的依赖版本是否与pom.xml声明一致
- **新增**：集中式版本管理问题：检查父POM的dependencyManagement配置是否正确
- **新增**：MyBatis-Plus相关问题：检查IService接口实现是否正确，确认Mapper注入是否正常
- **新增**：服务层重构问题：验证ServiceImpl继承关系，检查泛型类型是否正确

**章节来源**
- [CORE_MODULE_SPEC.md:342-350](file://docs/rules/CORE_MODULE_SPEC.md#L342-L350)

## 结论
核心业务服务通过清晰的分层与状态机模式，实现了项目生命周期的强一致流转与可扩展的阶段触发机制。文档集成与模板渲染在 core 与 file 模块之间解耦协作，结合版本快照保障可追溯性。配合 AI 任务的异步编排与完善的排障指引，系统具备高可用与易维护的特性。**优化的集中式依赖版本管理进一步提升了系统的可维护性和构建稳定性**。**最新的服务层架构重构采用MyBatis-Plus标准模式，显著提升了代码一致性和可维护性，为后续功能扩展奠定了坚实基础**。

## 附录
- 接口清单与表结构详见核心模块规范文档
- 阶段流程控制器与触发器职责详见阶段流程规范
- **优化**：集中式依赖版本管理策略详见父POM配置文件
- **新增**：外部API集成规范详见interaction模块文档
- **新增**：MyBatis-Plus服务层架构规范详见相关技术文档

**章节来源**
- [CORE_MODULE_SPEC.md:1-358](file://docs/rules/CORE_MODULE_SPEC.md#L1-L358)
- [PHASE_FLOW_SPEC.md:58-70](file://docs/rules/PHASE_FLOW_SPEC.md#L58-L70)