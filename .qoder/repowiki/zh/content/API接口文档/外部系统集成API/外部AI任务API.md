# 外部AI任务API

<cite>
**本文引用的文件**   
- [ExternalAiTaskController.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java)
- [ExternalAiTaskService.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java)
- [ExternalAuthController.java](file://ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/controller/ExternalAuthController.java)
- [InteractionAiTaskResultCallbackController.java](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/controller/InteractionAiTaskResultCallbackController.java)
- [AiTaskCreateRequest.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskCreateRequest.java)
- [AiTaskCreateResponse.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskCreateResponse.java)
- [AiTaskQueryResponse.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskQueryResponse.java)
- [AiTaskResultCallbackRequest.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskResultCallbackRequest.java)
- [ExternalTokenRequest.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/ExternalTokenRequest.java)
- [ExternalTokenResponse.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/ExternalTokenResponse.java)
- [InteractionResult.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/InteractionResult.java)
- [InteractionResponseCode.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/enums/InteractionResponseCode.java)
- [InteractionAiTaskTypeEnum.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/enums/InteractionAiTaskTypeEnum.java)
- [InteractionAiTaskStatusEnum.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/enums/InteractionAiTaskStatusEnum.java)
</cite>

## 更新摘要
**变更内容**   
- 新增完整的外部AI任务RESTful API接口，包括任务创建、查询、状态检查等功能
- 替代原有的标书处理API，提供更完善的异步任务处理能力
- 增强了数据隔离和权限控制机制
- 完善了回调处理和事件日志记录功能

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
本文件面向"外部系统"与"电子标平台"之间的集成，聚焦于"外部AI任务API"的接口契约、认证鉴权、任务生命周期与回调机制。通过统一的签名认证、异步任务创建与查询、以及终态结果回调，外部系统可安全地触发需求生成、评审项生成、文档集成、敏感词检测等AI能力，并可靠地获取执行状态与结果。

**更新** 新增了完整的外部AI任务RESTful API接口，包括任务创建、查询、状态检查等核心功能，替代了原有的标书处理API，提供了更完善的异步任务处理能力。

## 项目结构
围绕外部AI任务API，相关代码分布在以下模块：
- 核心服务对外暴露的任务控制层（创建、查询）
- 支持服务提供的外部认证与验签入口
- 交互自动配置提供的结果回调接收端点
- 公共交互DTO与枚举定义（请求/响应、统一响应、任务类型与状态）

```mermaid
graph TB
subgraph "核心服务"
C1["ExternalAiTaskController<br/>外部AI任务API控制器"]
C2["ExternalAiTaskService<br/>外部AI任务服务"]
end
subgraph "支持服务"
S1["ExternalAuthController<br/>外部系统认证"]
end
subgraph "交互自动配置"
I1["InteractionAiTaskResultCallbackController<br/>结果回调接收"]
end
subgraph "公共交互包"
D1["AiTaskCreateRequest / AiTaskCreateResponse"]
D2["AiTaskQueryResponse"]
D3["AiTaskResultCallbackRequest"]
D4["ExternalTokenRequest / ExternalTokenResponse"]
R1["InteractionResult / InteractionResponseCode"]
E1["InteractionAiTaskTypeEnum"]
E2["InteractionAiTaskStatusEnum"]
end
C1 --> C2
C1 --> D1
C1 --> D2
S1 --> D4
I1 --> D3
I1 --> R1
C2 -.-> E1
C2 -.-> E2
S1 -.-> R1
```

**图示来源**
- [ExternalAiTaskController.java:21-57](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L21-L57)
- [ExternalAiTaskService.java:31-165](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L31-L165)
- [ExternalAuthController.java:27-107](file://ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/controller/ExternalAuthController.java#L27-L107)
- [InteractionAiTaskResultCallbackController.java:17-52](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/controller/InteractionAiTaskResultCallbackController.java#L17-L52)

## 核心组件
- **外部AI任务控制器**：提供创建任务、查询任务详情与状态的REST接口，要求携带应用Key进行访问控制。
- **外部AI任务服务**：实现任务创建的业务逻辑，包括参数校验、类型转换、数据隔离验证等。
- **外部系统认证控制器**：提供基于签名认证的Token获取与验签辅助接口，用于外部系统身份与权限校验。
- **结果回调控制器**：接收AI任务终态结果的回调，包含入参校验与事件日志记录。
- **公共交互DTO与枚举**：定义任务创建/查询/回调的请求与响应结构、统一响应体、任务类型与状态语义。

**更新** 新增了ExternalAiTaskService服务层，实现了完整的业务逻辑处理和数据隔离机制。

## 架构总览
外部系统与平台的交互分为三类：
- **认证与鉴权**：通过签名换取Token，或进行验签辅助检查。
- **任务编排**：创建AI任务、查询任务详情与状态。
- **结果回传**：AI处理完成后，以终态回调通知调用方。

```mermaid
sequenceDiagram
participant Ext as "外部系统"
participant Auth as "ExternalAuthController"
participant TaskCtrl as "ExternalAiTaskController"
participant TaskSvc as "ExternalAiTaskService"
participant Callback as "InteractionAiTaskResultCallbackController"
Note over Ext,Auth : 认证阶段
Ext->>Auth : POST /api/external/token<br/>Header : X-App-Key, X-Timestamp, X-Signature
Auth-->>Ext : Result<ExternalTokenResponse>
Note over Ext,TaskCtrl : 任务管理阶段
Ext->>TaskCtrl : POST /api/external/ai-tasks<br/>Header : X-App-Key
TaskCtrl->>TaskSvc : createTask(appKey, request)
TaskSvc-->>TaskCtrl : AiTaskCreateResponse
TaskCtrl-->>Ext : Result<AiTaskCreateResponse>
Ext->>TaskCtrl : GET /api/external/ai-tasks/{taskId}
TaskCtrl->>TaskSvc : getTask(appKey, taskId)
TaskSvc-->>TaskCtrl : AiTaskQueryResponse
TaskCtrl-->>Ext : Result<AiTaskQueryResponse>
Ext->>TaskCtrl : GET /api/external/ai-tasks/{taskId}/status
TaskCtrl->>TaskSvc : getTaskStatus(appKey, taskId)
TaskSvc-->>TaskCtrl : AiTaskQueryResponse
TaskCtrl-->>Ext : Result<AiTaskQueryResponse>
Note over Ext,Callback : 结果回调阶段
Ext->>Callback : POST /callbacks/ai-task-result<br/>Body : AiTaskResultCallbackRequest
Callback-->>Ext : InteractionResult<Void>
```

**图示来源**
- [ExternalAuthController.java:42-74](file://ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/controller/ExternalAuthController.java#L42-L74)
- [ExternalAiTaskController.java:29-51](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L29-L51)
- [ExternalAiTaskService.java:52-121](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L52-L121)
- [InteractionAiTaskResultCallbackController.java:32-51](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/controller/InteractionAiTaskResultCallbackController.java#L32-L51)

## 详细组件分析

### 外部AI任务控制器（ExternalAiTaskController）
职责：
- 提供创建AI任务的POST接口，需携带X-App-Key头。
- 提供按任务ID查询详情的GET接口。
- 提供按任务ID查询状态的GET接口。
- 对任务ID进行基础合法性校验。

关键要点：
- 所有接口均返回统一包装Result对象。
- 任务ID校验失败将抛出业务异常。
- 使用Swagger注解提供API文档。

```mermaid
classDiagram
class ExternalAiTaskController {
+createTask(appKey, request) Result~AiTaskCreateResponse~
+getTask(appKey, taskId) Result~AiTaskQueryResponse~
+getTaskStatus(appKey, taskId) Result~AiTaskQueryResponse~
-validateTaskId(taskId) void
}
class ExternalAiTaskService
ExternalAiTaskController --> ExternalAiTaskService : "调用"
```

**图示来源**
- [ExternalAiTaskController.java:21-57](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L21-L57)

**章节来源**
- [ExternalAiTaskController.java:1-59](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L1-L59)

### 外部AI任务服务（ExternalAiTaskService）
职责：
- 实现外部AI任务的完整业务逻辑。
- 处理任务类型转换和参数反序列化。
- 实现数据隔离和权限验证。
- 管理外部回调记录和重试机制。

关键要点：
- 支持多种任务类型的动态解析和处理。
- 基于appKey的数据隔离机制确保安全性。
- 自动创建回调记录用于追踪和管理。
- 提供时间格式化工具方法。

```mermaid
flowchart TD
Start(["创建AI任务"]) --> ValidateReq["验证请求参数"]
ValidateReq --> ConvertType["转换任务类型"]
ConvertType --> ParseParams["解析请求参数JSON"]
ParseParams --> CreateTask["创建内部AI任务"]
CreateTask --> CreateCallback["创建外部回调记录"]
CreateCallback --> BuildResponse["构建响应对象"]
BuildResponse --> End(["返回响应"])
```

**图示来源**
- [ExternalAiTaskService.java:52-105](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L52-L105)

**章节来源**
- [ExternalAiTaskService.java:1-166](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L1-L166)

### 外部系统认证控制器（ExternalAuthController）
职责：
- 提供外部系统用户换取Token的接口，需携带X-App-Key、X-Timestamp、X-Signature三个请求头。
- 提供验签辅助接口，便于联调时验证签名配置。
- 在已登录上下文下，返回当前外部用户信息。

关键要点：
- 先校验请求头完整性，再进行签名验证。
- 验签失败抛出认证异常。
- Token响应包含token、过期时间与tokenType。

```mermaid
flowchart TD
Start(["进入 /api/external/token"]) --> CheckHeaders["校验请求头完整性"]
CheckHeaders --> Valid{"完整?"}
Valid -- "否" --> ThrowParamErr["抛出参数错误异常"]
Valid -- "是" --> VerifySig["验证签名"]
VerifySig --> SigOk{"验签通过?"}
SigOk -- "否" --> ThrowSigErr["抛出签名错误异常"]
SigOk -- "是" --> IssueToken["签发外部Token"]
IssueToken --> ReturnResp["返回Result<ExternalTokenResponse>"]
ThrowParamErr --> End(["结束"])
ThrowSigErr --> End
ReturnResp --> End
```

**图示来源**
- [ExternalAuthController.java:42-74](file://ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/controller/ExternalAuthController.java#L42-L74)

**章节来源**
- [ExternalAuthController.java:1-108](file://ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/controller/ExternalAuthController.java#L1-L108)

### 结果回调控制器（InteractionAiTaskResultCallbackController）
职责：
- 接收AI任务终态结果回调。
- 对回调请求进行入参校验。
- 记录入站事件日志（成功/失败）。

关键要点：
- 使用统一响应InteractionResult。
- 回调路径由常量定义，避免硬编码。
- 支持异常处理和事件日志记录。

```mermaid
sequenceDiagram
participant Ext as "外部系统"
participant Ctrl as "InteractionAiTaskResultCallbackController"
participant SPI as "InteractionAiTaskResultReceiveService"
participant Log as "InteractionEventLogger"
Ext->>Ctrl : POST /callbacks/ai-task-result
Ctrl->>Ctrl : 校验请求体
Ctrl->>SPI : receive(request)
SPI-->>Ctrl : 完成
Ctrl->>Log : logInbound("callbacks/ai-task-result", request, result, error)
Ctrl-->>Ext : InteractionResult<Void>
```

**图示来源**
- [InteractionAiTaskResultCallbackController.java:32-51](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/controller/InteractionAiTaskResultCallbackController.java#L32-L51)

**章节来源**
- [InteractionAiTaskResultCallbackController.java:1-53](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/controller/InteractionAiTaskResultCallbackController.java#L1-L53)

### 数据模型与枚举
- **任务创建请求/响应**：包含任务类型、业务标识、业务类型、请求参数JSON、关联文件ID列表；响应包含任务ID、任务类型、状态与创建时间。
- **任务查询响应**：包含任务ID、类型、业务标识、业务类型、状态及名称、执行结果JSON、错误信息、重试次数与上限、开始与完成时间、创建时间。
- **结果回调请求**：包含任务ID、类型、终态状态、执行结果JSON、错误信息与完成时间。
- **外部Token请求/响应**：请求包含用户名、用户ID、企业名称、企业ID与企业编码；响应包含token、过期时间与tokenType。
- **统一响应InteractionResult**：封装code、message、data与timestamp，并提供便捷工厂方法。
- **响应码InteractionResponseCode**：定义成功、失败、参数错误、签名错误等标准码。
- **任务类型InteractionAiTaskTypeEnum**：定义需求生成、评审项生成、文档集成、敏感词检测、错别字检测、政策文件审查、格式规范检测、文本优化等。
- **任务状态InteractionAiTaskStatusEnum**：定义待处理、处理中、已完成、失败、AI服务不可用、已跳过，并提供是否终态与是否可重试的判断。

```mermaid
erDiagram
AI_TASK_CREATE_REQUEST {
string taskType
long bizId
string bizType
string requestParams
string fileIds
}
AI_TASK_CREATE_RESPONSE {
long taskId
string taskType
string status
string createTime
}
AI_TASK_QUERY_RESPONSE {
long taskId
string taskType
string bizId
string bizType
string status
string statusName
string result
string errorMsg
int retryCount
int maxRetry
string startedAt
string completedAt
string createTime
}
AI_TASK_RESULT_CALLBACK_REQUEST {
long taskId
string taskType
string status
string result
string errorMsg
string completedAt
}
EXTERNAL_TOKEN_REQUEST {
string userName
string userId
string enterpriseName
string enterpriseId
string enterpriseCode
}
EXTERNAL_TOKEN_RESPONSE {
string token
long expireIn
string tokenType
}
```

**图示来源**
- [AiTaskCreateRequest.java:1-40](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskCreateRequest.java#L1-L40)
- [AiTaskCreateResponse.java:1-35](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskCreateResponse.java#L1-L35)
- [AiTaskQueryResponse.java:1-80](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskQueryResponse.java#L1-L80)
- [AiTaskResultCallbackRequest.java:1-45](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskResultCallbackRequest.java#L1-L45)
- [ExternalTokenRequest.java:1-21](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/ExternalTokenRequest.java#L1-L21)
- [ExternalTokenResponse.java:1-27](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/ExternalTokenResponse.java#L1-L27)

**章节来源**
- [AiTaskCreateRequest.java:1-40](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskCreateRequest.java#L1-L40)
- [AiTaskCreateResponse.java:1-35](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskCreateResponse.java#L1-L35)
- [AiTaskQueryResponse.java:1-80](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskQueryResponse.java#L1-L80)
- [AiTaskResultCallbackRequest.java:1-45](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskResultCallbackRequest.java#L1-L45)
- [ExternalTokenRequest.java:1-21](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/ExternalTokenRequest.java#L1-L21)
- [ExternalTokenResponse.java:1-27](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/ExternalTokenResponse.java#L1-L27)
- [InteractionResult.java:1-66](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/InteractionResult.java#L1-L66)
- [InteractionResponseCode.java:1-37](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/enums/InteractionResponseCode.java#L1-L37)
- [InteractionAiTaskTypeEnum.java:1-38](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/enums/InteractionAiTaskTypeEnum.java#L1-L38)
- [InteractionAiTaskStatusEnum.java:1-49](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/enums/InteractionAiTaskStatusEnum.java#L1-L49)

## 依赖关系分析
- **控制器到服务**：ExternalAiTaskController依赖ExternalAiTaskService，负责具体业务编排。
- **控制器到DTO/枚举**：各控制器直接消费公共交互包中的DTO与枚举，确保跨模块契约一致。
- **回调链路**：回调控制器依赖SPI接收服务与事件日志器，实现解耦与可观测性。
- **数据隔离**：ExternalAiTaskService通过callbackMapper实现基于appKey的数据隔离。

```mermaid
graph LR
Controller["ExternalAiTaskController"] --> Service["ExternalAiTaskService"]
Controller --> DTOs["AiTask* DTOs"]
Controller --> Enums["InteractionAiTask* Enum"]
Service --> CallbackMapper["AiTaskExternalCallbackMapper"]
Service --> AccessSystemMapper["SysAccessSystemQueryMapper"]
CallbackCtrl["InteractionAiTaskResultCallbackController"] --> SPI["InteractionAiTaskResultReceiveService"]
CallbackCtrl --> EventLog["InteractionEventLogger"]
CallbackCtrl --> IR["InteractionResult"]
```

**图示来源**
- [ExternalAiTaskController.java:21-57](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L21-L57)
- [ExternalAiTaskService.java:37-47](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L37-L47)
- [InteractionAiTaskResultCallbackController.java:18-51](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/controller/InteractionAiTaskResultCallbackController.java#L18-L51)

**章节来源**
- [ExternalAiTaskController.java:1-59](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L1-L59)
- [ExternalAiTaskService.java:1-166](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L1-L166)
- [InteractionAiTaskResultCallbackController.java:1-53](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/controller/InteractionAiTaskResultCallbackController.java#L1-L53)

## 性能考虑
- **异步任务**：AI任务通常耗时较长，建议采用异步创建+轮询/回调模式，避免长连接阻塞。
- **幂等与重试**：外部系统在重试时应保证幂等（如基于taskId与taskType组合），服务端应记录最大重试次数与已重试次数。
- **限流与熔断**：对高频任务创建与查询接口实施限流策略，防止资源耗尽。
- **日志与追踪**：在回调链路中记录入站事件，便于问题定位与性能分析。
- **数据隔离**：基于appKey的数据隔离机制提高了查询性能和安全性。

**更新** 新增了基于appKey的数据隔离机制，提升了查询性能和安全性。

## 故障排查指南
- **签名错误**：当外部系统获取Token或调用受保护接口时，若出现签名错误，请检查X-App-Key、X-Timestamp、X-Signature是否正确且时间戳有效。
- **参数错误**：任务ID必须大于0；请求头缺失或不完整会触发参数错误。
- **回调失败**：确认回调路径与请求体字段是否符合约定；关注事件日志输出，定位入站请求与异常堆栈。
- **状态判断**：根据任务状态枚举判断是否为终态，非终态时可继续轮询或等待回调。
- **权限错误**：如果出现"无权查询此任务"错误，检查appKey是否与任务创建时使用的appKey一致。

**更新** 新增了权限错误的排查指南，帮助解决数据隔离相关的权限问题。

**章节来源**
- [ExternalAuthController.java:52-74](file://ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/controller/ExternalAuthController.java#L52-L74)
- [ExternalAiTaskController.java:53-57](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L53-L57)
- [ExternalAiTaskService.java:126-135](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L126-L135)
- [InteractionAiTaskResultCallbackController.java:36-51](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/controller/InteractionAiTaskResultCallbackController.java#L36-L51)
- [InteractionAiTaskStatusEnum.java:35-47](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/enums/InteractionAiTaskStatusEnum.java#L35-L47)

## 结论
外部AI任务API通过清晰的认证鉴权、统一的DTO与枚举契约、以及可靠的回调机制，为外部系统提供了稳定、可扩展的AI能力接入方式。新增的服务层实现了完整的业务逻辑处理和数据隔离机制，进一步提升了系统的安全性和可维护性。建议在集成过程中严格遵循签名规范、幂等设计与重试策略，并结合日志与监控提升可观测性与稳定性。

**更新** 新增的服务层和数据隔离机制显著提升了系统的完整性和安全性，为外部系统集成提供了更加稳定和可靠的API接口。

## 附录

### API清单与说明
- **外部系统认证**
  - POST /api/external/token：外部系统用户换取Token，需携带X-App-Key、X-Timestamp、X-Signature。
  - GET /api/external/verify：验签辅助接口，便于联调。
  - GET /api/external/userinfo：获取当前外部用户信息（需登录上下文）。
- **外部AI任务**
  - POST /api/external/ai-tasks：创建AI任务，需携带X-App-Key。
  - GET /api/external/ai-tasks/{taskId}：查询任务详情。
  - GET /api/external/ai-tasks/{taskId}/status：查询任务状态。
- **结果回调**
  - POST /callbacks/ai-task-result：接收AI任务终态结果回调。

**更新** 新增了完整的RESTful API接口，包括任务创建、查询和状态检查功能。

**章节来源**
- [ExternalAuthController.java:42-106](file://ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/controller/ExternalAuthController.java#L42-L106)
- [ExternalAiTaskController.java:29-51](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L29-L51)
- [InteractionAiTaskResultCallbackController.java:32-51](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/controller/InteractionAiTaskResultCallbackController.java#L32-L51)