# 外部系统集成API

<cite>
**本文引用的文件**
- [INTERACTION_INTEGRATION_SPEC.md](file://docs/rules/INTERACTION_INTEGRATION_SPEC.md)
- [InteractionApiPaths.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/constant/InteractionApiPaths.java)
- [InteractionSignatureInterceptor.java](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/web/InteractionSignatureInterceptor.java)
- [InteractionSignatureUtil.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/util/InteractionSignatureUtil.java)
- [ExternalAiTaskController.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java)
- [ExternalAiTaskService.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java)
- [ExternalSignatureInterceptor.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/config/ExternalSignatureInterceptor.java)
- [ExternalApiWebConfig.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/config/ExternalApiWebConfig.java)
- [InteractionHeaderConstants.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/constant/InteractionHeaderConstants.java)
- [AiTaskExternalCallback.java](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/ai/AiTaskExternalCallback.java)
- [AiTaskCreateRequest.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskCreateRequest.java)
- [AiTaskQueryResponse.java](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskQueryResponse.java)
- [ResponseCode.java](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/enums/ResponseCode.java)
- [pom.xml（interaction-core）](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-core/pom.xml)
- [pom.xml（common-interaction）](file://ele-ai-tender-system/ele-ai-tender-common-interaction/pom.xml)
- [pom.xml（interaction聚合）](file://ele-ai-tender-system/ele-ai-tender-interaction/pom.xml)
- [CLAUDE.md](file://CLAUDE.md)
- [PROJECT_SPEC_FINAL.md](file://docs/rules/PROJECT_SPEC_FINAL.md)
</cite>

## 更新摘要
**变更内容**
- 新增外部AI任务接口模块，提供独立的X-App-Key认证机制
- 实现数据所有权验证（verifyOwnership），确保外部系统只能访问自己创建的任务
- 增强安全隔离机制，防止跨系统数据泄露
- 完善外部API签名验证拦截器配置

## 目录
1. [简介](#简介)
2. [项目结构](#项目结构)
3. [核心组件](#核心组件)
4. [架构总览](#架构总览)
5. [详细组件分析](#详细组件分析)
6. [依赖分析](#依赖分析)
7. [性能考虑](#性能考虑)
8. [故障排查指南](#故障排查指南)
9. [结论](#结论)
10. [附录](#附录)

## 简介
本文件为"外部系统集成模块"的对外API接口文档，聚焦与第三方系统对接能力，包括：
- CA证书管理
- 标书解密
- 投标文件推送
- **外部AI任务管理接口**（新增）
- SPI插件架构与扩展点使用
- 接口签名验证、数据加密传输、回调通知机制的安全保障
- 标准数据交换格式、错误码规范与重试机制
- 系统集成配置、监控日志与故障排查方法

该集成体系由协议库与Starter组成，提供统一前缀的固定路径、出站客户端能力以及可插拔的业务SPI实现。**现已支持外部系统通过X-App-Key认证调用AI任务服务，并具备严格的数据所有权验证机制。**

## 项目结构
交互集成相关代码分布在以下模块：
- ele-ai-tender-common-interaction：对外协议DTO、常量、工具类（JDK 8兼容）
- ele-ai-tender-interaction-core：出站HTTP客户端与自动装配支持
- ele-ai-tender-interaction-autoconfigure：Web拦截器、自动配置等
- ele-ai-tender-interaction-spring-boot-starter：Starter入口，便于业务系统引入
- **ele-ai-tender-core：外部AI任务控制器和服务实现（新增）**

```mermaid
graph TB
subgraph "交互集成模块"
CI["common-interaction<br/>协议与工具"]
IC["interaction-core<br/>出站客户端"]
AC["autoconfigure<br/>拦截器/自动装配"]
SB["spring-boot-starter<br/>Starter入口"]
end
subgraph "外部AI任务模块"
EATC["ExternalAiTaskController<br/>外部AI任务控制器"]
EATS["ExternalAiTaskService<br/>外部AI任务服务"]
ESIG["ExternalSignatureInterceptor<br/>外部API签名拦截器"]
ECONF["ExternalApiWebConfig<br/>外部API配置"]
end
CI --> IC
IC --> AC
AC --> SB
EATC --> EATS
ECONF --> ESIG
ESIG --> EATC
```

**图表来源**
- [pom.xml（interaction聚合）:43-47](file://ele-ai-tender-system/ele-ai-tender-interaction/pom.xml#L43-L47)
- [pom.xml（interaction-core）:19-37](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-core/pom.xml#L19-L37)
- [pom.xml（common-interaction）:19-21](file://ele-ai-tender-system/ele-ai-tender-common-interaction/pom.xml#L19-L21)
- [ExternalApiWebConfig.java:28-31](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/config/ExternalApiWebConfig.java#L28-L31)

章节来源
- [INTERACTION_INTEGRATION_SPEC.md:1-13](file://docs/rules/INTERACTION_INTEGRATION_SPEC.md#L1-L13)
- [PROJECT_SPEC_FINAL.md:29-44](file://docs/rules/PROJECT_SPEC_FINAL.md#L29-L44)

## 核心组件
- 固定路径与接口清单：统一前缀 /api/eleAiTender/interaction，包含身份、项目信息、CA密钥查询及多类回调接口
- **外部AI任务接口**：独立的前缀 /api/external/ai-tasks，提供AI任务的创建和查询功能
- 出站客户端能力：获取external token、用户信息、文件操作、投标文件预存、提交/查询解密、编制入口URL构造
- **双重安全机制**：
  - 传统HMAC-SHA256签名校验（/api/eleAiTender/interaction）
  - **X-App-Key认证机制（/api/external/ai-tasks）**
- **数据所有权验证**：确保外部系统只能访问自己创建的任务，防止跨系统数据泄露
- SPI扩展点：业务系统需实现若干服务接口以完成具体业务逻辑
- 配置项：统一前缀 ele-ai-tender.interaction，涵盖基础URL、认证、文件、加解密与投标相关路径

章节来源
- [INTERACTION_INTEGRATION_SPEC.md:19-76](file://docs/rules/INTERACTION_INTEGRATION_SPEC.md#L19-L76)
- [InteractionApiPaths.java:1-13](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/constant/InteractionApiPaths.java#L1-L13)
- [ExternalAiTaskController.java:22](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L22)

## 架构总览
交互集成采用"协议单源 + Starter自动装配 + 业务SPI实现"的分层设计。调用方通过Starter启用后，自动注册拦截器进行签名校验，随后路由到控制器并委托给业务SPI处理；出站侧通过core提供的客户端访问第三方系统。**外部AI任务接口采用独立的认证和数据隔离机制，确保多租户环境下的数据安全。**

```mermaid
sequenceDiagram
participant Ext as "外部系统"
participant Web as "Spring MVC"
participant Intc as "ExternalSignatureInterceptor"
participant Ctrl as "ExternalAiTaskController"
participant Service as "ExternalAiTaskService"
participant Verify as "verifyOwnership"
participant DB as "数据库"
Ext->>Web : "HTTP 请求(携带X-App-Key/X-Timestamp/X-Signature)"
Web->>Intc : "进入外部API拦截器"
Intc->>Intc : "验证X-App-Key有效性"
Intc->>Intc : "验证时间戳和签名"
Intc-->>Web : "校验通过"
Web->>Ctrl : "路由到AI任务控制器"
Ctrl->>Service : "调用业务服务"
Service->>Verify : "验证数据所有权"
Verify->>DB : "查询任务归属关系"
DB-->>Verify : "返回查询结果"
Verify-->>Service : "所有权验证通过"
Service-->>Ctrl : "返回任务数据"
Ctrl-->>Ext : "响应结果"
```

**图表来源**
- [ExternalSignatureInterceptor.java:29-72](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/config/ExternalSignatureInterceptor.java#L29-L72)
- [ExternalAiTaskController.java:37-43](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L37-L43)
- [ExternalAiTaskService.java:110-114](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L110-L114)
- [ExternalApiWebConfig.java:28-31](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/config/ExternalApiWebConfig.java#L28-L31)

## 详细组件分析

### 固定接口与路径
- **传统交互接口前缀**：/api/eleAiTender/interaction
- **外部AI任务接口前缀**：/api/external/ai-tasks（新增）
- 当前固定接口：
  - GET /identity/current
  - POST /projects/basic-info
  - POST /bid-record-schemes/query
  - POST /ca-keys/query
  - POST /callbacks/tender-pdf
  - POST /callbacks/tender-package
  - POST /callbacks/bid-document-result
  - POST /callbacks/bid-decrypt-result
  - **POST /api/external/ai-tasks（新增）**
  - **GET /api/external/ai-tasks/{taskId}（新增）**
  - **GET /api/external/ai-tasks/{taskId}/status（新增）**

说明：
- 以上路径在规范中明确定义，作为对外接入的统一边界
- 协议DTO与常量位于common-interaction模块，保持单源维护
- **外部AI任务接口采用独立的路径前缀，便于权限管理和安全控制**

章节来源
- [INTERACTION_INTEGRATION_SPEC.md:19-33](file://docs/rules/INTERACTION_INTEGRATION_SPEC.md#L19-L33)
- [InteractionApiPaths.java:1-13](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/constant/InteractionApiPaths.java#L1-L13)
- [ExternalAiTaskController.java:22](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L22)

### 安全与签名校验

#### 传统交互接口安全机制
- 签名算法：HmacSHA256，内容拼接 appKey + timestamp，输出大写
- 时间戳有效期：默认5分钟，防止重放攻击
- 请求头要求：必须包含 appKey、timestamp、signature
- 校验失败：抛出交互异常并返回参数错误或签名错误码

#### 外部AI任务接口安全机制（新增）
- **认证方式**：X-App-Key + X-Timestamp + X-Signature
- **拦截器**：ExternalSignatureInterceptor专门处理/api/external/ai-tasks/**路径
- **系统验证**：检查appKey对应的系统是否存在、是否启用、是否过期
- **签名验证**：使用相同的HmacSHA256算法进行签名校验
- **数据隔离**：通过verifyOwnership方法确保外部系统只能访问自己创建的任务

```mermaid
flowchart TD
Start(["进入外部API拦截器"]) --> CheckHeaders["检查请求头(X-App-Key/X-Timestamp/X-Signature)是否完整"]
CheckHeaders --> |不完整| ParamError["返回400错误：请求头参数不完整"]
CheckHeaders --> ParseTs["解析时间戳"]
ParseTs --> TsValid{"时间戳是否在有效期内"}
TsValid --> |否| SignError["返回400错误：时间戳格式错误"]
TsValid --> |是| QuerySystem["根据X-App-Key查询系统信息"]
QuerySystem --> SystemExists{"系统是否存在且启用"}
SystemExists --> |否| AuthError["返回401错误：接入系统不存在/已禁用/已过期"]
SystemExists --> |是| VerifySig["计算签名并与请求签名比对"]
VerifySig --> |不一致| SigFail["返回401错误：签名验证失败"]
VerifySig --> |一致| Pass["放行至控制器"]
Pass --> DataVerify["控制器调用verifyOwnership验证数据所有权"]
DataVerify --> OwnershipCheck{"任务是否属于该外部系统"}
OwnershipCheck --> |否| Forbidden["返回403错误：无权查询此任务"]
OwnershipCheck --> |是| Success["返回成功响应"]
```

**图表来源**
- [ExternalSignatureInterceptor.java:29-72](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/config/ExternalSignatureInterceptor.java#L29-L72)
- [ExternalAiTaskService.java:126-135](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L126-L135)

章节来源
- [InteractionSignatureInterceptor.java:29-55](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/web/InteractionSignatureInterceptor.java#L29-L55)
- [InteractionSignatureUtil.java:18-36](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/util/InteractionSignatureUtil.java#L18-L36)
- [ExternalSignatureInterceptor.java:29-72](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/config/ExternalSignatureInterceptor.java#L29-L72)
- [ExternalAiTaskService.java:126-135](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L126-L135)

### 外部AI任务接口详解（新增）

#### 接口概览
外部AI任务接口提供独立的AI任务管理能力，供外部系统通过X-App-Key认证调用：

- **创建AI任务**：POST /api/external/ai-tasks
- **查询任务详情**：GET /api/external/ai-tasks/{taskId}
- **查询任务状态**：GET /api/external/ai-tasks/{taskId}/status

#### 请求认证
所有接口必须在请求头中包含：
- `X-App-Key`：应用标识符
- `X-Timestamp`：请求时间戳（毫秒）
- `X-Signature`：HmacSHA256签名值

#### 数据所有权验证机制
**新增的verifyOwnership方法**确保外部系统只能访问自己创建的任务：

```java
private void verifyOwnership(String appKey, Long taskId) {
    long count = callbackMapper.selectCount(
        new LambdaQueryWrapper<AiTaskExternalCallback>()
            .eq(AiTaskExternalCallback::getTaskId, taskId)
            .eq(AiTaskExternalCallback::getAppKey, appKey)
    );
    if (count == 0) {
        throw new BusinessException(ResponseCode.FORBIDDEN, "无权查询此任务");
    }
}
```

**工作原理**：
1. 每次创建任务时，都会记录任务ID与appKey的关联关系到ai_task_external_callback表
2. 查询任务时，验证请求的appKey是否与任务创建时的appKey匹配
3. 不匹配则返回403禁止访问错误，防止跨系统数据泄露

#### 数据模型
- **AiTaskCreateRequest**：任务创建请求，包含任务类型、业务ID、业务类型、请求参数等
- **AiTaskQueryResponse**：任务查询响应，包含任务状态、执行结果、错误信息等
- **AiTaskExternalCallback**：外部回调记录，用于数据隔离和回调追踪

章节来源
- [ExternalAiTaskController.java:29-51](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L29-L51)
- [ExternalAiTaskService.java:52-135](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L52-L135)
- [AiTaskCreateRequest.java:11-39](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskCreateRequest.java#L11-L39)
- [AiTaskQueryResponse.java:11-79](file://ele-ai-tender-system/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskQueryResponse.java#L11-L79)
- [AiTaskExternalCallback.java:18-37](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/ai/AiTaskExternalCallback.java#L18-L37)

### SPI插件架构与扩展点
- 定位：业务系统必须实现的SPI用于承载具体业务逻辑
- 必须实现的SPI列表：
  - InteractionIdentityService
  - InteractionProjectInfoService
  - InteractionBidRecordSchemeService
  - InteractionCaKeysInfoService
  - InteractionTenderPdfReceiveService
  - InteractionTenderPackageReceiveService
  - InteractionBidDocumentResultReceiveService
  - InteractionBidDecryptResultReceiveService

使用方式：
- 在业务系统中实现上述接口并通过Spring容器注册
- 控制器或编排层根据请求类型选择对应SPI进行处理
- 协议DTO仅在controller/facade边界做显式转换，service层不直接透传协议DTO

章节来源
- [INTERACTION_INTEGRATION_SPEC.md:34-43](file://docs/rules/INTERACTION_INTEGRATION_SPEC.md#L34-L43)
- [INTERACTION_INTEGRATION_SPEC.md:78-83](file://docs/rules/INTERACTION_INTEGRATION_SPEC.md#L78-L83)

### 出站客户端能力
- 能力清单：
  - 获取 external token
  - 获取当前外部用户信息
  - 查询文件信息
  - 下载文件
  - 上传文件
  - 推送投标文件预存
  - 提交解密请求
  - 查询解密状态
  - 构造招标文件编制入口 URL

说明：
- 出站能力由 interaction-core 提供，基于 Spring Web 与 JSON 序列化
- 业务系统可通过注入相应客户端发起对第三方系统的调用

章节来源
- [INTERACTION_INTEGRATION_SPEC.md:45-55](file://docs/rules/INTERACTION_INTEGRATION_SPEC.md#L45-L55)
- [pom.xml（interaction-core）:19-37](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-core/pom.xml#L19-L37)

### 配置项与环境
- 统一配置前缀：ele-ai-tender.interaction
- 关键配置项：
  - api-base-url、page-base-url
  - app-key、app-secret
  - token-path、user-info-path
  - file-base-url、file-info-path、file-download-path、file-upload-path
  - crypto-base-url
  - bid-document-push-path、bid-decrypt-submit-path、bid-decrypt-status-path

建议：
- 将敏感配置（如app-secret）放入环境变量或独立配置文件
- 不同环境（dev/test/prod）分别维护配置，避免硬编码

章节来源
- [INTERACTION_INTEGRATION_SPEC.md:57-76](file://docs/rules/INTERACTION_INTEGRATION_SPEC.md#L57-L76)
- [CLAUDE.md:144-149](file://CLAUDE.md#L144-L149)

### 数据交换格式与错误码
- 数据交换格式：
  - 协议DTO单源维护于 common-interaction
  - controller/facade边界做显式mapper转换
  - service层不透传协议DTO
- 错误码与异常：
  - 交互接口使用统一的交互结果包装
  - 签名/参数错误通过交互异常返回，包含明确的错误码
  - **外部AI任务接口使用ResponseCode枚举中的FORBIDDEN(403)表示无权限访问**

章节来源
- [INTERACTION_INTEGRATION_SPEC.md:78-83](file://docs/rules/INTERACTION_INTEGRATION_SPEC.md#L78-L83)
- [CLAUDE.md:119-131](file://CLAUDE.md#L119-131)
- [ResponseCode.java:15](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/enums/ResponseCode.java#L15)

### 重试机制与幂等性
- 重试策略：
  - 针对网络抖动或第三方瞬时不可用，建议在出站客户端或服务编排层增加指数退避重试
  - 对幂等接口（如查询、状态同步）可安全重试；非幂等接口需谨慎
- 幂等性建议：
  - 回调接口应支持幂等处理（例如基于唯一流水号去重）
  - 结合Redis或数据库唯一约束保证重复消息不造成副作用
- **外部AI任务数据隔离**：
  - 每个外部系统的任务数据完全隔离，无法跨系统访问
  - 通过ai_task_external_callback表维护任务与外部系统的关联关系

[本节为通用指导，不直接分析具体文件]

## 依赖分析
交互集成模块之间的依赖关系如下：
- interaction-core 依赖 common-interaction
- autoconfigure 依赖 core 与 common-interaction
- starter 聚合各子模块，便于业务系统一键引入
- **外部AI任务模块依赖core模块，实现独立的认证和数据隔离机制**

```mermaid
graph LR
Common["common-interaction"] --> Core["interaction-core"]
Core --> Auto["autoconfigure"]
Auto --> Starter["spring-boot-starter"]
Core --> ExternalAI["ExternalAiTaskModule<br/>外部AI任务模块"]
ExternalAI --> Common
ExternalAI --> Core
```

**图表来源**
- [pom.xml（interaction-core）:19-37](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-core/pom.xml#L19-L37)
- [pom.xml（interaction聚合）:43-47](file://ele-ai-tender-system/ele-ai-tender-interaction/pom.xml#L43-L47)
- [ExternalAiTaskController.java:9](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L9)

章节来源
- [pom.xml（interaction-core）:19-37](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-core/pom.xml#L19-L37)
- [pom.xml（interaction聚合）:43-47](file://ele-ai-tender-system/ele-ai-tender-interaction/pom.xml#L43-L47)

## 性能考虑
- 签名计算开销较小，但应避免在热点路径上频繁创建对象
- 时间戳校验应在拦截器早期执行，尽早拒绝无效请求
- 出站客户端连接池与超时参数应根据第三方系统SLA合理配置
- 回调接口建议异步落库与后续处理解耦，提升吞吐
- **外部AI任务数据所有权验证**：
  - verifyOwnership方法通过数据库查询验证任务归属，建议为ai_task_external_callback表的task_id和app_key字段建立联合索引
  - 查询性能优化：使用LambdaQueryWrapper的条件查询，避免全表扫描

[本节为通用指导，不直接分析具体文件]

## 故障排查指南
常见问题与定位要点：
- 签名错误：
  - 检查appKey、appSecret是否与Starter配置一致
  - 确认客户端与服务端时间差在5分钟内
  - 核对签名算法与大小写规则
- 参数不完整：
  - 确保请求头包含appKey、timestamp、signature
- **外部AI任务接口特定问题**：
  - **X-App-Key认证失败**：检查外部系统配置是否正确，确认系统状态为启用且未过期
  - **403无权限错误**：确认请求的appKey与任务创建时的appKey一致，检查ai_task_external_callback表中的数据关联
  - **任务不存在**：确认taskId有效且属于当前外部系统
- 回调未触发：
  - 检查回调路径配置是否正确
  - 查看业务SPI实现是否注册成功
  - 关注链路追踪ID（X-Trace-Id）与MDC traceId
- 出站失败：
  - 核对base-url与path配置
  - 检查网络连通性与第三方系统状态
  - 查看客户端日志与重试次数

章节来源
- [InteractionSignatureInterceptor.java:29-55](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/web/InteractionSignatureInterceptor.java#L29-L55)
- [ExternalSignatureInterceptor.java:29-72](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/config/ExternalSignatureInterceptor.java#L29-L72)
- [ExternalAiTaskService.java:126-135](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L126-L135)
- [CLAUDE.md:144-151](file://CLAUDE.md#L144-L151)

## 结论
外部系统集成模块通过统一的协议与Starter，提供了标准化的对外接口、安全的签名校验、可扩展的SPI架构以及完善的出站客户端能力。**新增的外部AI任务接口模块进一步增强了系统的安全性，通过X-App-Key认证机制和严格的数据所有权验证，确保多租户环境下的数据隔离和安全访问。**遵循本规范可实现与第三方系统在CA证书管理、标书解密、投标文件推送、AI任务管理等场景的稳定对接，并具备良好的可观测性与可维护性。

## 附录

### 接口清单速查
- **传统交互接口前缀**：/api/eleAiTender/interaction
- **外部AI任务接口前缀**：/api/external/ai-tasks（新增）
- 固定接口：
  - GET /identity/current
  - POST /projects/basic-info
  - POST /bid-record-schemes/query
  - POST /ca-keys/query
  - POST /callbacks/tender-pdf
  - POST /callbacks/tender-package
  - POST /callbacks/bid-document-result
  - POST /callbacks/bid-decrypt-result
  - **POST /api/external/ai-tasks（新增）**
  - **GET /api/external/ai-tasks/{taskId}（新增）**
  - **GET /api/external/ai-tasks/{taskId}/status（新增）**

章节来源
- [INTERACTION_INTEGRATION_SPEC.md:19-33](file://docs/rules/INTERACTION_INTEGRATION_SPEC.md#L19-L33)
- [ExternalAiTaskController.java:22](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/controller/external/ExternalAiTaskController.java#L22)

### 配置项速查
- 统一配置前缀：ele-ai-tender.interaction
- 关键项：
  - api-base-url、page-base-url
  - app-key、app-secret
  - token-path、user-info-path
  - file-base-url、file-info-path、file-download-path、file-upload-path
  - crypto-base-url
  - bid-document-push-path、bid-decrypt-submit-path、bid-decrypt-status-path

章节来源
- [INTERACTION_INTEGRATION_SPEC.md:57-76](file://docs/rules/INTERACTION_INTEGRATION_SPEC.md#L57-L76)

### 外部AI任务接口认证要求（新增）
- **请求头要求**：
  - X-App-Key：应用标识符（必填）
  - X-Timestamp：请求时间戳，毫秒级（必填）
  - X-Signature：HmacSHA256签名值（必填）
- **认证流程**：
  1. 验证请求头完整性
  2. 检查时间戳有效性
  3. 验证appKey对应的系统状态
  4. 验证签名正确性
  5. 验证数据所有权（查询接口）
- **错误码**：
  - 400：请求头参数不完整或时间戳格式错误
  - 401：接入系统不存在/已禁用/已过期或签名验证失败
  - 403：无权查询此任务（数据所有权验证失败）

章节来源
- [ExternalSignatureInterceptor.java:29-72](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/config/ExternalSignatureInterceptor.java#L29-L72)
- [ExternalAiTaskService.java:126-135](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java#L126-L135)
- [ResponseCode.java:13-15](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/enums/ResponseCode.java#L13-L15)