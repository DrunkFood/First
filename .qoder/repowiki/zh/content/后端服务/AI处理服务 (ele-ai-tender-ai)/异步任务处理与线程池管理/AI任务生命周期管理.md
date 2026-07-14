# AI任务生命周期管理

<cite>
**本文引用的文件**   
- [AiTaskProcessor.java](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/AiTaskProcessor.java)
- [DynamicThreadPoolManager.java](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/DynamicThreadPoolManager.java)
- [UserConcurrencyManager.java](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/UserConcurrencyManager.java)
- [AiTaskServiceImpl.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/AiTaskServiceImpl.java)
- [IAiTaskService.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IAiTaskService.java)
- [AiTaskResultSyncScheduler.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncScheduler.java)
- [AiTaskResultSyncHandler.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncHandler.java)
- [AiTaskResultCallbackHandler.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultCallbackHandler.java)
- [AiTask.java](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/ai/AiTask.java)
- [AiTaskStatus.java](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/enums/AiTaskStatus.java)
- [AiTaskSource.java](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/enums/AiTaskSource.java)
- [AiTaskResultCallbackRequest.java](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskResultCallbackRequest.java)
- [AiUnavailableException.java](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/exception/AiUnavailableException.java)
- [AiErrorContentException.java](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/exception/AiErrorContentException.java)
- [AiSyncedException.java](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/exception/AiSyncedException.java)
- [application.yml](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application.yml)
- [CORE_MODULE_SPEC.md](file://docs/rules/CORE_MODULE_SPEC.md)
</cite>

## 更新摘要
**变更内容**   
- 新增基于systemId的智能同步策略：内部任务(systemId=0)使用本地同步，外部任务(systemId≠0)触发回调推送
- 新增AiTaskResultCallbackHandler回调处理器，实现外部系统结果推送机制
- AiTask实体新增systemId字段，支持任务来源标识
- 增强结果同步系统的可扩展性和跨系统集成能力

## 目录
1. [简介](#简介)
2. [项目结构](#项目结构)
3. [核心组件](#核心组件)
4. [架构总览](#架构总览)
5. [详细组件分析](#详细组件分析)
6. [依赖关系分析](#依赖关系分析)
7. [性能与容量规划](#性能与容量规划)
8. [故障诊断与排错指南](#故障诊断与排错指南)
9. [结论](#结论)
10. [附录](#附录)

## 简介
本技术文档围绕AI任务生命周期管理系统，系统性阐述从任务创建、提交、执行、监控、重试到完成的全链路机制。重点解析AiTaskProcessor的任务调度模型、状态机设计、异步执行与超时控制、错误处理策略，以及并发控制、负载均衡与资源隔离等高级能力；同时覆盖基于systemId的智能结果同步、监控接口、日志记录等运维支撑能力，并提供调试、优化与排障实践建议。

## 项目结构
系统采用多模块分层组织：
- 公共层（common）：定义任务实体、枚举、异常与通用DTO
- AI服务层（ai）：负责任务轮询、分发执行、线程池与并发控制
- 核心业务层（core）：提供任务服务接口、结果同步调度与处理器
- 前端（frontend/support-frontend）：展示任务进度与状态

```mermaid
graph TB
subgraph "AI服务层"
A["AiTaskProcessor<br/>定时拉取PENDING任务"]
B["DynamicThreadPoolManager<br/>动态线程池"]
C["UserConcurrencyManager<br/>用户级并发控制"]
end
subgraph "核心业务层"
D["IAiTaskService / AiTaskServiceImpl<br/>任务创建/查询/跳过/超时标记"]
E["AiTaskResultSyncScheduler<br/>扫描未同步终态任务"]
F["AiTaskResultSyncHandler<br/>按类型同步结果到业务表"]
G["AiTaskResultCallbackHandler<br/>外部任务回调推送"]
end
subgraph "公共层"
H["AiTask 实体(含systemId)"]
I["AiTaskStatus 枚举"]
J["AiTaskSource 枚举"]
K["AiUnavailableException / AiErrorContentException / AiSyncedException"]
L["AiTaskResultCallbackRequest DTO"]
end
A --> B
A --> C
A --> H
A --> I
A --> K
D --> H
D --> I
E --> F
E --> G
F --> H
G --> L
```

**图表来源**
- [AiTaskProcessor.java:1-196](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/AiTaskProcessor.java#L1-L196)
- [DynamicThreadPoolManager.java:1-190](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/DynamicThreadPoolManager.java#L1-L190)
- [UserConcurrencyManager.java:1-131](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/UserConcurrencyManager.java#L1-L131)
- [IAiTaskService.java:1-43](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IAiTaskService.java#L1-L43)
- [AiTaskResultSyncScheduler.java:1-72](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncScheduler.java#L1-L72)
- [AiTaskResultSyncHandler.java:1-790](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncHandler.java#L1-L790)
- [AiTaskResultCallbackHandler.java:1-98](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultCallbackHandler.java#L1-L98)
- [AiTask.java:1-83](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/ai/AiTask.java#L1-L83)
- [AiTaskStatus.java:1-46](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/enums/AiTaskStatus.java#L1-L46)
- [AiTaskSource.java:1-29](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/enums/AiTaskSource.java#L1-L29)
- [AiTaskResultCallbackRequest.java:1-55](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskResultCallbackRequest.java#L1-L55)

章节来源
- [CORE_MODULE_SPEC.md:155-195](file://docs/rules/CORE_MODULE_SPEC.md#L155-L195)

## 核心组件
- 任务调度器：定时轮询待处理任务，进行并发许可校验与CAS状态更新，提交至动态线程池执行，并统一捕获异常落库。
- 动态线程池：支持从数据库参数热更新核心/最大线程数、队列容量、保活时间、任务超时等，具备优雅关闭与重建能力。
- 并发控制：全局+用户级双维度并发限制，基于信号量实现"排队等待"，避免PROCESSING→PENDING弹跳。
- 任务服务：提供任务创建（防重）、状态查询、跳过、超时标记、最新任务查询、活跃任务检查等。
- **智能结果同步**：基于systemId区分任务来源，内部任务使用本地同步，外部任务触发回调推送。
- **回调推送机制**：为外部系统提供安全可靠的HTTP回调推送，支持签名验证和错误处理。
- 实体与状态：统一的AiTask实体与AiTaskStatus枚举，明确终态与可重试语义，新增systemId标识任务来源。

**章节来源**
- [AiTaskProcessor.java:1-196](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/AiTaskProcessor.java#L1-L196)
- [DynamicThreadPoolManager.java:1-190](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/DynamicThreadPoolManager.java#L1-L190)
- [UserConcurrencyManager.java:1-131](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/UserConcurrencyManager.java#L1-L131)
- [IAiTaskService.java:1-43](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IAiTaskService.java#L1-L43)
- [AiTaskResultSyncScheduler.java:1-72](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncScheduler.java#L1-L72)
- [AiTaskResultSyncHandler.java:1-790](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncHandler.java#L1-L790)
- [AiTaskResultCallbackHandler.java:1-98](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultCallbackHandler.java#L1-L98)
- [AiTask.java:1-83](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/ai/AiTask.java#L1-L83)
- [AiTaskStatus.java:1-46](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/enums/AiTaskStatus.java#L1-L46)

## 架构总览
整体流程分为"调度-执行-智能同步"三段式：
- 调度：每5秒轮询PENDING任务，先做并发许可校验，再CAS改为PROCESSING，随后提交执行。
- 执行：根据任务类型分发给具体处理器，带超时控制与异常分类处理。
- **智能同步**：每10秒扫描未同步的终态任务，基于systemId自动选择同步策略：内部任务本地同步，外部任务回调推送。

```mermaid
sequenceDiagram
participant DB as "数据库(ai_task)"
participant Proc as "AiTaskProcessor"
participant Pool as "DynamicThreadPoolManager"
participant Exec as "各处理器(需求/评审/检测/集成)"
participant SyncSch as "AiTaskResultSyncScheduler"
participant LocalSync as "AiTaskResultSyncHandler"
participant Callback as "AiTaskResultCallbackHandler"
participant Biz as "业务表(需求/评审/检测/项目)"
participant External as "外部系统"
Note over Proc,DB : 每5秒轮询PENDING任务
Proc->>DB : 选择PENDING任务(批量)
Proc->>Proc : 并发许可校验(CAS改PROCESSING)
Proc->>Pool : 提交任务(带超时)
Pool-->>Exec : 执行dispatch(task)
Exec-->>Pool : 返回结果或抛异常
Pool-->>Proc : 回调处理结果/异常
Proc->>DB : 更新COMPLETED/FAILED/AI_UNAVAILABLE
Note over SyncSch,DB : 每10秒扫描未同步终态任务
SyncSch->>DB : 选择未同步终态任务
SyncSch->>SyncSch : 判断systemId
alt systemId = 0 (内部任务)
SyncSch->>LocalSync : sync(task)
LocalSync->>Biz : 按类型写入业务数据
LocalSync-->>SyncSch : 成功/失败
else systemId ≠ 0 (外部任务)
SyncSch->>Callback : callback(task)
Callback->>External : HTTP回调推送
External-->>Callback : 响应结果
Callback-->>SyncSch : 成功/失败
end
SyncSch->>DB : 标记result_synced=1/2
```

**图表来源**
- [AiTaskProcessor.java:57-101](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/AiTaskProcessor.java#L57-L101)
- [AiTaskProcessor.java:127-167](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/AiTaskProcessor.java#L127-L167)
- [AiTaskResultSyncScheduler.java:33-70](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncScheduler.java#L33-L70)
- [AiTaskResultSyncHandler.java:72-104](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncHandler.java#L72-L104)
- [AiTaskResultCallbackHandler.java:44-84](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultCallbackHandler.java#L44-L84)

## 详细组件分析

### 任务调度器 AiTaskProcessor
- 轮询策略：固定延迟5秒拉取PENDING任务，批量上限可控。
- 并发控制：优先获取用户级与全局并发许可，再进行CAS状态变更，避免重复抢占与状态弹跳。
- 执行路径：无用户ID任务直接提交；有用户ID任务通过Future+超时控制，统一在回调中落库。
- 异常分类：区分AI不可用、内容异常、普通异常，分别落库为不同终态。
- 任务分发：按AiTaskType路由到文本优化、需求生成、评审项生成、文档集成、检测引擎等处理器。

```mermaid
flowchart TD
Start(["进入processPendingTasks"]) --> Fetch["批量查询PENDING任务"]
Fetch --> Empty{"是否空?"}
Empty --> |是| End(["结束本轮"])
Empty --> |否| ForEach["遍历任务"]
ForEach --> HasUser{"是否有userId?"}
HasUser --> |否| CASDirect["CAS PENDING->PROCESSING"]
CASDirect --> SubmitDirect["直接提交执行"]
HasUser --> |是| Acquire["tryAcquire(userId)"]
Acquire --> AcqOk{"获得许可?"}
AcqOk --> |否| Skip["跳过本轮继续下一个"]
AcqOk --> |是| CASProc["CAS PENDING->PROCESSING"]
CASProc --> CasOk{"CAS成功?"}
CasOk --> |否| Release["释放并发许可"] --> Skip
CasOk --> |是| SubmitCtrl["提交执行(带超时)"]
SubmitCtrl --> Done(["结束"])
```

**图表来源**
- [AiTaskProcessor.java:57-101](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/AiTaskProcessor.java#L57-L101)
- [AiTaskProcessor.java:127-167](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/AiTaskProcessor.java#L127-L167)

章节来源
- [AiTaskProcessor.java:1-196](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/AiTaskProcessor.java#L1-L196)

### 动态线程池 DynamicThreadPoolManager
- 参数来源：从系统参数表读取AI_THREAD_POOL组配置，启动时初始化，运行时每10秒轮询Hash变化自动刷新。
- 热更新策略：队列容量变更则重建线程池；否则调整核心/最大线程数与保活时间；同时联动并发控制参数刷新。
- 拒绝策略：CallerRunsPolicy，保证背压与稳定性。
- 指标暴露：提供活跃数、池大小、队列长度等统计方法。

```mermaid
classDiagram
class DynamicThreadPoolManager {
+submit(task) Future
+execute(task) void
+getActiveCount() int
+getPoolSize() int
+getQueueSize() int
-applyRefresh(newProps) void
-createExecutor(props) ThreadPoolExecutor
-loadFromDb() ThreadPoolProperties
}
class UserConcurrencyManager {
+tryReserve(userId) boolean
+tryAcquire(userId) boolean
+release(userId) void
+refresh(newProps) void
}
DynamicThreadPoolManager --> UserConcurrencyManager : "热更新联动"
```

**图表来源**
- [DynamicThreadPoolManager.java:1-190](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/DynamicThreadPoolManager.java#L1-L190)
- [UserConcurrencyManager.java:1-131](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/UserConcurrencyManager.java#L1-L131)

章节来源
- [DynamicThreadPoolManager.java:1-190](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/DynamicThreadPoolManager.java#L1-L190)

### 并发控制 UserConcurrencyManager
- 双维度控制：
  - 发起数：基于数据库统计PENDING+PROCESSING数量，超限直接拒绝。
  - 并发数：基于Semaphore控制PROCESSING数量，满则排队等待。
- 热更新：新许可数=新上限-当前活跃数，确保刷新后并发控制不失效。
- 使用场景：在调度器中先获取并发许可，再进行CAS状态更新，避免状态弹跳。

```mermaid
classDiagram
class UserConcurrencyManager {
-globalSemaphore : Semaphore
-userSemaphores : ConcurrentHashMap~Long, Entry~
-properties : ThreadPoolProperties
+tryReserve(userId) boolean
+tryAcquire(userId) boolean
+release(userId) void
+refresh(newProps) void
+getGlobalActiveCount() int
}
class ThreadPoolProperties {
+getGlobalMaxConcurrentTasks() int
+getUserMaxConcurrentTasks() int
+getGlobalMaxPendingTasks() int
+getUserMaxPendingTasks() int
}
UserConcurrencyManager --> ThreadPoolProperties : "读取参数"
```

**图表来源**
- [UserConcurrencyManager.java:1-131](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/UserConcurrencyManager.java#L1-L131)

章节来源
- [UserConcurrencyManager.java:1-131](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/UserConcurrencyManager.java#L1-L131)

### 任务服务 IAiTaskService / AiTaskServiceImpl
- 创建任务：防重复提交（同一业务同一类型不允许活跃任务），设置默认重试次数与超时，序列化请求参数。
- 查询状态：封装VO，包含任务类型/状态名称映射。
- 跳过任务：仅允许非终态或特定终态下操作。
- 超时标记：定时任务调用，将超时任务标记为AI_UNAVAILABLE。
- 活跃任务检查：用于页面加载时判断是否已有进行中任务。

```mermaid
sequenceDiagram
participant Client as "调用方"
participant Service as "AiTaskServiceImpl"
participant Mapper as "AiTaskMapper"
Client->>Service : createTask(type, projectId, bizId, bizType, params, fileIds)
Service->>Mapper : selectActiveTask(...)
Mapper-->>Service : activeTask?
alt 存在活跃任务
Service-->>Client : 抛出已处理异常
else 不存在
Service->>Service : 构造AiTask并设置默认值
Service->>Mapper : insert(task)
Service-->>Client : 返回任务
end
```

**图表来源**
- [AiTaskServiceImpl.java:31-62](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/AiTaskServiceImpl.java#L31-L62)
- [IAiTaskService.java:11-43](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IAiTaskService.java#L11-L43)

章节来源
- [IAiTaskService.java:1-43](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/IAiTaskService.java#L1-L43)
- [AiTaskServiceImpl.java:1-148](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/AiTaskServiceImpl.java#L1-L148)

### 智能结果同步 AiTaskResultSyncScheduler

**更新** 新增基于systemId的智能同步策略，支持内部任务和外部任务的差异化处理

- 调度器：每10秒扫描未同步的终态任务，CAS抢占后根据systemId选择同步策略。
- **智能路由**：systemId=0的内部任务使用本地同步处理器，systemId≠0的外部任务触发回调推送。
- 本地同步：按任务类型分发到对应同步逻辑，包括需求生成、评审项生成、文档集成、检测记录等。
- 回调推送：为外部系统提供安全的HTTP回调推送，支持签名验证和错误处理。
- 业务联动：检测完成后汇总问题数并驱动项目状态机流转，发送消息通知，必要时自动创建版本快照。

```mermaid
sequenceDiagram
participant Sch as "AiTaskResultSyncScheduler"
participant Mapper as "AiTaskMapper"
participant LocalSync as "AiTaskResultSyncHandler"
participant Callback as "AiTaskResultCallbackHandler"
participant Biz as "业务表"
participant External as "外部系统"
Sch->>Mapper : selectUnsyncedTasks(limit)
Mapper-->>Sch : 列表
loop 遍历任务
Sch->>Mapper : markSyncing(id)
alt 抢占成功
Sch->>Sch : 判断task.getSystemId()
alt systemId = 0 (内部任务)
Sch->>LocalSync : sync(task)
LocalSync->>Biz : 按类型写入/更新
LocalSync-->>Sch : 成功
else systemId ≠ 0 (外部任务)
Sch->>Callback : callback(task)
Callback->>External : HTTP回调推送
External-->>Callback : 响应结果
Callback-->>Sch : 成功/失败
end
Sch->>Mapper : markSuccessSynced(id)
else 抢占失败
Sch-->>Sch : 跳过
end
end
```

**图表来源**
- [AiTaskResultSyncScheduler.java:33-70](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncScheduler.java#L33-L70)
- [AiTaskResultSyncHandler.java:72-104](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncHandler.java#L72-L104)
- [AiTaskResultCallbackHandler.java:44-84](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultCallbackHandler.java#L44-L84)

章节来源
- [AiTaskResultSyncScheduler.java:1-72](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncScheduler.java#L1-L72)
- [AiTaskResultSyncHandler.java:1-790](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncHandler.java#L1-L790)

### 回调推送处理器 AiTaskResultCallbackHandler

**新增** 专门处理外部任务结果回调推送的组件

- 外部系统集成：通过systemId查询外部系统配置，构建安全的HTTP回调请求。
- 签名验证：使用AppKey和AppSecret生成签名，确保回调请求的完整性和真实性。
- 请求构建：组装任务ID、类型、业务ID、状态、结果、错误信息等回调数据。
- 错误处理：对回调失败进行异常包装，便于上层统一处理和重试。
- 标准化格式：使用统一的AiTaskResultCallbackRequest DTO，确保数据结构一致性。

```mermaid
classDiagram
class AiTaskResultCallbackHandler {
+callback(AiTask task) void
-buildCallbackUrl(String systemUrl) String
-formatDate(Date date) String
-accessSystemQueryMapper : SysAccessSystemQueryMapper
-callbackRestTemplate : RestTemplate
}
class SysAccessSystem {
+id : Long
+systemUrl : String
+appKey : String
+appSecret : String
}
class AiTaskResultCallbackRequest {
+taskId : Long
+taskType : String
+bizId : String
+bizType : String
+status : String
+result : String
+errorMsg : String
+completedAt : String
}
AiTaskResultCallbackHandler --> SysAccessSystem : "查询配置"
AiTaskResultCallbackHandler --> AiTaskResultCallbackRequest : "构建请求"
```

**图表来源**
- [AiTaskResultCallbackHandler.java:28-97](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultCallbackHandler.java#L28-L97)
- [AiTaskResultCallbackRequest.java:10-54](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskResultCallbackRequest.java#L10-L54)

章节来源
- [AiTaskResultCallbackHandler.java:1-98](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultCallbackHandler.java#L1-L98)

### 任务实体与状态机

**更新** AiTask实体新增systemId字段，支持任务来源标识

- 实体字段：任务类型、**任务来源(systemId)**、关联项目/业务ID、请求参数JSON、文件ID列表、状态、结果JSON、错误信息、重试计数、最大重试、开始/完成时间、超时分钟、结果同步标志。
- 状态机：PENDING → PROCESSING → COMPLETED/FAILED/AI_UNAVAILABLE/SKIPPED；其中FAILED与AI_UNAVAILABLE可重试。
- 规则约束：终态判定、可重试判定、结果同步双维度（status + result_synced）。
- **任务来源**：systemId=0表示内部任务，systemId≠0表示外部任务，用于智能同步策略路由。

```mermaid
stateDiagram-v2
[*] --> PENDING
PENDING --> PROCESSING : "CAS成功"
PROCESSING --> COMPLETED : "正常完成"
PROCESSING --> FAILED : "异常/超时"
PROCESSING --> AI_UNAVAILABLE : "AI不可用"
PROCESSING --> SKIPPED : "用户跳过"
COMPLETED --> [*]
FAILED --> [*]
AI_UNAVAILABLE --> [*]
SKIPPED --> [*]
```

**图表来源**
- [AiTaskStatus.java:11-45](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/enums/AiTaskStatus.java#L11-L45)
- [CORE_MODULE_SPEC.md:187-195](file://docs/rules/CORE_MODULE_SPEC.md#L187-L195)

章节来源
- [AiTask.java:1-83](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/ai/AiTask.java#L1-L83)
- [AiTaskStatus.java:1-46](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/enums/AiTaskStatus.java#L1-L46)
- [AiTaskSource.java:1-29](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/enums/AiTaskSource.java#L1-L29)
- [CORE_MODULE_SPEC.md:187-195](file://docs/rules/CORE_MODULE_SPEC.md#L187-L195)

## 依赖关系分析
- 组件耦合：
  - AiTaskProcessor依赖线程池与并发管理器，解耦了具体处理器实现。
  - 结果同步调度与处理器独立于执行阶段，降低执行期复杂度。
  - **新增回调处理器**：AiTaskResultCallbackHandler独立处理外部系统集成，与本地同步处理器并行工作。
- 外部依赖：
  - 数据库：任务表、业务表、系统参数表、外部系统配置表。
  - Redis：应用配置中启用，可用于扩展限流/缓存（当前代码未直接使用）。
  - **HTTP客户端**：回调推送使用RestTemplate进行HTTP通信。
- 潜在循环依赖：未发现直接循环引用；线程池与并发控制通过属性注入与懒加载规避。

```mermaid
graph LR
Processor["AiTaskProcessor"] --> Pool["DynamicThreadPoolManager"]
Processor --> Concurrency["UserConcurrencyManager"]
Processor --> Entity["AiTask"]
Processor --> Status["AiTaskStatus"]
SyncSch["AiTaskResultSyncScheduler"] --> SyncH["AiTaskResultSyncHandler"]
SyncSch --> Callback["AiTaskResultCallbackHandler"]
SyncH --> Entity
Callback --> Entity
Callback --> Request["AiTaskResultCallbackRequest"]
Service["AiTaskServiceImpl"] --> Entity
Service --> Status
```

**图表来源**
- [AiTaskProcessor.java:1-196](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/AiTaskProcessor.java#L1-L196)
- [DynamicThreadPoolManager.java:1-190](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/DynamicThreadPoolManager.java#L1-L190)
- [UserConcurrencyManager.java:1-131](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/UserConcurrencyManager.java#L1-L131)
- [AiTaskResultSyncScheduler.java:1-72](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncScheduler.java#L1-L72)
- [AiTaskResultSyncHandler.java:1-790](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncHandler.java#L1-L790)
- [AiTaskResultCallbackHandler.java:1-98](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultCallbackHandler.java#L1-L98)
- [AiTaskServiceImpl.java:1-148](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/AiTaskServiceImpl.java#L1-L148)
- [AiTask.java:1-83](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/ai/AiTask.java#L1-L83)
- [AiTaskStatus.java:1-46](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/enums/AiTaskStatus.java#L1-L46)
- [AiTaskResultCallbackRequest.java:1-55](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskResultCallbackRequest.java#L1-L55)

## 性能与容量规划
- 线程池参数：
  - 核心/最大线程数、队列容量、保活时间、任务超时分钟均可通过系统参数表热更新。
  - 建议依据CPU核数与IO密集程度调优，队列容量需结合峰值QPS评估。
- 并发控制：
  - 全局并发与用户并发分开限制，避免单用户独占资源。
  - 发起数限制防止雪崩，配合排队等待提升吞吐稳定性。
- 超时控制：
  - 任务级超时优先于全局配置，避免长尾任务阻塞。
- 资源隔离：
  - 用户级并发信号量实现软隔离；未来可扩展为按租户/项目维度的隔离。
- **回调性能**：
  - 外部系统回调使用独立的RestTemplate实例，避免影响主业务流程。
  - 回调失败不影响主流程，通过重试机制保证最终一致性。
- 监控指标：
  - 线程池活跃数、池大小、队列长度；任务成功率、失败率、平均耗时、超时率。
  - **新增回调指标**：回调成功率、失败率、平均响应时间、外部系统可用性。

**章节来源**
- [DynamicThreadPoolManager.java:1-190](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/DynamicThreadPoolManager.java#L1-L190)
- [UserConcurrencyManager.java:1-131](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/UserConcurrencyManager.java#L1-L131)
- [application.yml:1-72](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application.yml#L1-L72)

## 故障诊断与排错指南
- 常见问题定位
  - 任务卡住：检查是否处于PROCESSING且长时间无进展，确认是否被其他实例抢占或并发已满。
  - 超时失败：核对任务超时配置与业务实际耗时，适当增大超时或拆分任务。
  - AI不可用：关注AI服务健康与模型路由可用性，必要时降级或切换备用模型。
  - **结果同步问题**：
    - 内部任务：检查AiTaskResultSyncHandler的执行日志和业务表写入权限。
    - 外部任务：检查AiTaskResultCallbackHandler的回调推送日志和外部系统可达性。
    - 查看result_synced标志位，排查同步处理器异常与业务表写入权限。
  - **回调推送问题**：
    - 检查外部系统配置是否正确（system_url、app_key、app_secret）。
    - 验证网络连通性和防火墙规则。
    - 检查签名生成和验证逻辑。
- 关键日志与断点
  - 调度器日志：轮询批次、并发许可、CAS结果、提交执行、完成/失败。
  - 线程池日志：参数变更、重建过程、关闭过程。
  - **同步日志**：未同步任务发现、同步成功/失败、业务状态联动。
  - **回调日志**：外部系统配置查询、回调请求构建、签名生成、HTTP推送、响应处理。
- 恢复策略
  - 重启清理：服务启动/关闭时清理残留PROCESSING任务，避免僵尸任务。
  - 重试与跳过：对可重试状态支持重试；用户可主动跳过任务以推进流程。
  - 幂等与去重：创建任务前检查活跃任务，避免重复提交。
  - **回调重试**：外部系统回调失败时，可通过重新扫描未同步任务进行重试。

**章节来源**
- [AiTaskProcessor.java:103-167](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/AiTaskProcessor.java#L103-L167)
- [DynamicThreadPoolManager.java:46-82](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/DynamicThreadPoolManager.java#L46-L82)
- [AiTaskResultSyncScheduler.java:33-70](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncScheduler.java#L33-L70)
- [AiTaskResultSyncHandler.java:72-104](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultSyncHandler.java#L72-L104)
- [AiTaskResultCallbackHandler.java:44-84](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/scheduler/AiTaskResultCallbackHandler.java#L44-L84)
- [AiTaskServiceImpl.java:73-99](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/AiTaskServiceImpl.java#L73-L99)

## 结论
本系统通过"调度-执行-智能同步"三段式架构实现了AI任务的稳定、可控、可观测的生命周期管理。借助动态线程池与用户级并发控制，系统在保障吞吐的同时有效抑制热点与雪崩风险；**基于systemId的智能结果同步机制**确保了内部任务和外部任务的差异化处理，提升了系统的可扩展性和跨系统集成能力；结果同步与状态机联动确保了业务一致性与用户体验。后续可在重试策略精细化、指标采集完善、资源隔离增强等方面持续演进。

## 附录
- 配置参考
  - 应用端口、数据源、Redis、AI模型基础URL、JWT、内部文件服务地址、日志级别等。
- 相关规范
  - 项目状态流转、编制阶段流转、AI任务进度与终态规则等。
- **外部系统集成规范**
  - 回调接口规范、签名算法、错误码定义、重试策略等。

**章节来源**
- [application.yml:1-72](file://ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application.yml#L1-L72)
- [CORE_MODULE_SPEC.md:155-195](file://docs/rules/CORE_MODULE_SPEC.md#L155-L195)