# AI任务管理系统

<cite>
**本文引用的文件**   
- [CLAUDE.md](file://CLAUDE.md)
- [ele-ai-tender-system/pom.xml](file://ele-ai-tender-system/pom.xml)
- [ele-ai-tender-frontend/package.json](file://ele-ai-tender-frontend/package.json)
- [ele-ai-tender-support-frontend/package.json](file://ele-ai-tender-support-frontend/package.json)
- [ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/CoreApplication.java](file://ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/CoreApplication.java)
- [ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/AiApplication.java](file://ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/AiApplication.java)
- [ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/SupportApplication.java](file://ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/SupportApplication.java)
- [ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/FileApplication.java](file://ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/FileApplication.java)
- [ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiTaskClient.java](file://ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiTaskClient.java)
- [ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiExternalAuthClient.java](file://ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiExternalAuthClient.java)
- [ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/EleAiTenderInteractionAutoConfiguration.java](file://ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/EleAiTenderInteractionAutoConfiguration.java)
- [ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskCreateRequest.java](file://ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/AiTaskCreateRequest.java)
- [ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiExternalUserInfoClient.java](file://ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiExternalUserInfoClient.java)
- [ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiFileClient.java](file://ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiFileClient.java)
- [ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/dto/response/AiTaskVO.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/dto/response/AiTaskVO.java)
- [ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/ai/AiTask.java](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/ai/AiTask.java)
- [ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/AiTaskServiceImpl.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/AiTaskServiceImpl.java)
- [ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java)
- [sql/20260713_bizId字段改为str.sql](file://sql/20260713_bizId字段改为str.sql)
</cite>

## 更新摘要
**变更内容**   
- 修复了AiTaskClient中的fileIds参数类型错误，将List<String>改为List<Long>以匹配数据库ID类型
- 改进了字符串拼接方法，优化了文件ID列表的处理逻辑
- 移除了冗余的任务状态查询功能，简化了API接口设计
- 增强了类型安全性，避免潜在的运行时类型转换异常

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
本系统为"招标文件AI编制工具"，支持独立部署与嵌入第三方平台，提供项目管理、需求编制、智能检测、文档生成、知识库与模型路由等能力。后端采用Spring Boot 3.2.2 + MyBatis-Plus 3.5.5 + MySQL 8.4.0；AI侧集成Spring AI 1.1.0、Milvus向量库、Apache Tika、poi-tl、flexmark-java等；前端包含两个Vue 3应用：支撑中心管理后台与AI编制业务前端。

## 项目结构
仓库采用前后端分离与多模块后端架构：
- 前端
  - ele-ai-tender-frontend：AI编制业务前端（端口5173）
  - ele-ai-tender-support-frontend：支撑中心管理后台（端口3060）
- 后端（Maven多模块）
  - ele-ai-tender-common：公共实体、工具类、异常、统一响应
  - ele-ai-tender-common-interaction：交互协议DTO/SPI/路径常量
  - ele-ai-tender-interaction：业务系统接入Starter（core/autoconfigure/starter）
  - ele-ai-tender-support：认证、用户、角色、菜单、模板配置、知识库配置、模型配置与路由、系统参数、政策文件、消息通知、统计分析、访问/操作日志、版本管理（:8080）
  - ele-ai-tender-file：文件上传/下载/查询/删除、文档生成（Markdown→Word引擎）（:8081）
  - ele-ai-tender-core：项目管理、业务需求编制、AI编制任务、AI内容反馈、检测管理、评审项管理、文档集成、用户消息、政策文件（用户级）、项目模板快照（:8082）
  - ele-ai-tender-ai：AI对话、知识库管理、文档匹配、智能检测、模型路由、模型连通性测试（:8083）

```mermaid
graph TB
subgraph "前端"
FE_CORE["AI编制前端<br/>端口5173"]
FE_SUPPORT["支撑中心前端<br/>端口3060"]
end
subgraph "后端服务"
SVC_SUPPORT["支撑中心 :8080"]
SVC_FILE["文件服务 :8081"]
SVC_CORE["核心业务 :8082"]
SVC_AI["AI服务 :8083"]
end
subgraph "基础设施"
DB["MySQL"]
REDIS["Redis"]
MILVUS["Milvus 向量库"]
DISK["本地磁盘(文件存储)"]
LLM["大模型API"]
end
FE_CORE --> SVC_CORE
FE_CORE --> SVC_AI
FE_CORE --> SVC_FILE
FE_CORE --> SVC_SUPPORT
FE_SUPPORT --> SVC_SUPPORT
FE_SUPPORT --> SVC_FILE
SVC_CORE --> DB
SVC_CORE --> REDIS
SVC_CORE -.异步任务.-> SVC_AI
SVC_AI --> DB
SVC_AI --> REDIS
SVC_AI --> MILVUS
SVC_AI --> LLM
SVC_FILE --> DB
SVC_FILE --> DISK
SVC_SUPPORT --> DB
SVC_SUPPORT --> REDIS
```

图表来源
- [CLAUDE.md](file://CLAUDE.md)

章节来源
- [CLAUDE.md](file://CLAUDE.md)

## 核心组件
- 启动入口
  - 核心业务：CoreApplication
  - AI服务：AiApplication
  - 支撑中心：SupportApplication
  - 文件服务：FileApplication
- 技术栈与依赖治理
  - 父POM集中管理Spring Boot、MyBatis-Plus、JWT、Hutool、MySQL、Lombok、SpringDoc、Redisson、Spring AI、Milvus、Tika、poi-tl、flexmark等版本
- 前端工程
  - AI编制前端与支撑中心前端均基于Vue 3 + TypeScript + Vite + Element Plus + Pinia

章节来源
- [ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/CoreApplication.java](file://ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/CoreApplication.java)
- [ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/AiApplication.java](file://ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/AiApplication.java)
- [ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/SupportApplication.java](file://ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/SupportApplication.java)
- [ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/FileApplication.java](file://ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/FileApplication.java)
- [ele-ai-tender-system/pom.xml](file://ele-ai-tender-system/pom.xml)
- [ele-ai-tender-frontend/package.json](file://ele-ai-tender-frontend/package.json)
- [ele-ai-tender-support-frontend/package.json](file://ele-ai-tender-support-frontend/package.json)

## 架构总览
请求链路与服务职责
- 浏览器 → 支撑中心前端(3060)
  - /support-api/* → rewrite(/api/*) → 支撑中心:8080 → MySQL(db=6)+Redis(db=6)
  - /file-api/* → 直接转发 → 文件服务:8081 → MySQL(db=6)+本地磁盘
- 浏览器 → AI编制前端(5173)
  - /core-api/* → rewrite(/api/*) → 核心业务:8082 → MySQL(db=6)+Redis(db=6)
  - /ai-api/* → rewrite(/api/*) → AI服务:8083 → MySQL(db=6)+Redis(db=6)+Milvus+大模型API
  - /file-api/* → rewrite(/api/*) → 文件服务:8081 → MySQL(db=6)+本地磁盘
  - /support-api/* → rewrite(/api/*) → 支撑中心:8080 → MySQL(db=6)+Redis(db=6)
- 业务系统 → Interaction Starter → /api/eleAiTender/interaction/*

```mermaid
sequenceDiagram
participant U as "用户浏览器"
participant FE as "AI编制前端"
participant CORE as "核心业务 : 8082"
participant AI as "AI服务 : 8083"
participant FILE as "文件服务 : 8081"
participant SUP as "支撑中心 : 8080"
participant DB as "MySQL"
participant REDIS as "Redis"
participant MIL as "Milvus"
participant LLM as "大模型API"
U->>FE : 打开页面/发起操作
FE->>CORE : 调用 /core-api/*
CORE->>DB : 读写项目/需求/任务等
CORE->>REDIS : 缓存/锁/状态
CORE-->>FE : 返回结果
FE->>AI : 调用 /ai-api/* (对话/检测/匹配)
AI->>DB : 读写知识库/日志/任务
AI->>REDIS : 缓存/限流
AI->>MIL : 向量检索
AI->>LLM : 调用大模型
AI-->>FE : 返回结果/流式SSE
FE->>FILE : 调用 /file-api/* (上传/下载/预览)
FILE->>DB : 文件元数据
FILE->>DISK : 文件IO
FILE-->>FE : 返回文件URL/内容
FE->>SUP : 调用 /support-api/* (认证/权限/配置)
SUP->>DB : 用户/角色/菜单/模板/模型配置
SUP->>REDIS : 会话/缓存
SUP-->>FE : 返回鉴权/配置
```

图表来源
- [CLAUDE.md](file://CLAUDE.md)

章节来源
- [CLAUDE.md](file://CLAUDE.md)

## 详细组件分析

### 启动与装配
- 各服务通过独立的Spring Boot启动类完成扫描与初始化，启用定时任务（@EnableScheduling），并指定Mapper扫描包范围。
- 核心业务与AI服务在启动时打印服务名，便于运维识别。

```mermaid
classDiagram
class CoreApplication {
+main(args)
}
class AiApplication {
+main(args)
}
class SupportApplication {
+main(args)
}
class FileApplication {
+main(args)
}
CoreApplication <.. AiApplication : "异步解耦(任务表)"
CoreApplication <.. FileApplication : "文件接口"
CoreApplication <.. SupportApplication : "认证/权限"
AiApplication <.. FileApplication : "知识库文件"
```

图表来源
- [ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/CoreApplication.java](file://ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/CoreApplication.java)
- [ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/AiApplication.java](file://ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/AiApplication.java)
- [ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/SupportApplication.java](file://ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/SupportApplication.java)
- [ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/FileApplication.java](file://ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/FileApplication.java)

章节来源
- [ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/CoreApplication.java](file://ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/CoreApplication.java)
- [ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/AiApplication.java](file://ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/AiApplication.java)
- [ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/SupportApplication.java](file://ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/SupportApplication.java)
- [ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/FileApplication.java](file://ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/FileApplication.java)

### 构建与依赖治理
- 父POM统一管理所有子模块与外部依赖版本，确保编译一致性与可维护性。
- 关键依赖包括Spring Boot 3.2.2、MyBatis-Plus 3.5.5、JJWT 0.12.5、Hutool 5.8.25、MySQL 8.4.0、Lombok 1.18.34、SpringDoc 2.3.0、Redisson 3.27.2、Spring AI 1.1.0、Milvus SDK 2.3.3、Apache Tika 2.9.0、poi-tl 1.12.2、flexmark 0.64.0等。

```mermaid
flowchart TD
A["父POM定义版本"] --> B["子模块引用依赖"]
B --> C["统一编译/打包/插件配置"]
C --> D["保证跨模块一致性"]
```

图表来源
- [ele-ai-tender-system/pom.xml](file://ele-ai-tender-system/pom.xml)

章节来源
- [ele-ai-tender-system/pom.xml](file://ele-ai-tender-system/pom.xml)

### 前端工程概览
- AI编制前端与支撑中心前端均采用Vue 3 + TypeScript + Vite + Element Plus + Pinia，脚本命令支持dev/build/test/local等多环境。
- 依赖差异：AI编制前端额外引入md-editor-v3、docx-preview、diff2html等用于文档编辑与对比预览。

```mermaid
graph LR
FE_CORE_PKG["AI编制前端 package.json"] --> FE_CORE_DEPS["Vue3/TS/Vite/ElementPlus/Pinia"]
FE_CORE_PKG --> FE_CORE_EXT["md-editor-v3/docx-preview/diff2html"]
FE_SUP_PKG["支撑中心前端 package.json"] --> FE_SUP_DEPS["Vue3/TS/Vite/ElementPlus/Pinia"]
```

图表来源
- [ele-ai-tender-frontend/package.json](file://ele-ai-tender-frontend/package.json)
- [ele-ai-tender-support-frontend/package.json](file://ele-ai-tender-support-frontend/package.json)

章节来源
- [ele-ai-tender-frontend/package.json](file://ele-ai-tender-frontend/package.json)
- [ele-ai-tender-support-frontend/package.json](file://ele-ai-tender-support-frontend/package.json)

### 业务流程要点（概念性说明）
- 项目状态流转：DRAFT → IN_PROGRESS → PENDING_DETECTION → DETECTING → DETECTION_PASSED/DETENTION_FAILED/DETENTION_SKIPPED → PUBLISHED → ARCHIVED/CANCELLED
- 编制阶段流转：BASIC_INFO(1) → REQUIREMENT(2) → REVIEW_ITEM(3) → DOCUMENT(4) → DETECTION(5)，由阶段流程控制器管控
- 项目-需求关系：进入需求阶段时复制需求内容到项目字段，后续阶段从项目字段读取
- 检测类型：敏感词、错别字、政策文件审查、格式规范检测；仅检测系统生成的招标需求内容与评审项标准纯文本
- 需求生成模式：三步式Agent编排（大纲→分章并行→审查修订），通过ai_task.result渐进推送进度，全文硬约束5000字内
- 评审类型：符合性审查、技术标评审、资信标评审、商务评审

[本节为概念性说明，不直接分析具体源码文件]

### 任务来源追踪系统增强

**已更新** 增强任务来源追踪能力，新增systemId字段以支持更好的审计追踪和系统来源区分

#### 数据库结构变更
- ai_task表新增system_id字段，类型为bigint，位于task_type字段之后
- biz_id字段类型从bigint修改为varchar(64)，支持更灵活的业务ID格式
- sup_access_system表的system_name和app_key字段长度调整为50字符

#### AiTaskVO响应对象增强
- 新增systemId字段，用于标识任务发起的系统ID
- 该字段在后端转换过程中直接从AiTask实体映射到VO对象
- 支持区分内部任务(systemId=0)和外部系统任务(systemId>0)

#### 任务创建流程优化
- createInternalTask方法：内部任务systemId固定为0L
- createExternalTask方法：接收外部系统传入的systemId参数
- ExternalAiTaskService自动从SecurityContextHolder获取当前系统ID并传递给任务创建逻辑

#### 审计追踪能力
- 支持追溯每个任务的原始发起系统
- 便于问题排查和系统间协作监控
- 为外部系统集成提供完整的审计日志支持

```mermaid
classDiagram
class AiTask {
+Long id
+String taskType
+Long systemId
+Long projectId
+String bizId
+String bizType
+String status
+String result
+Date createTime
}
class AiTaskVO {
+Long id
+String taskType
+Long systemId
+Long projectId
+Number bizId
+String bizType
+String status
+String result
+Date createTime
}
class AiTaskServiceImpl {
+createInternalTask()
+createExternalTask()
+getTask()
+toVO()
}
AiTask --> AiTaskVO : "转换映射"
AiTaskServiceImpl --> AiTask : "CRUD操作"
AiTaskServiceImpl --> AiTaskVO : "VO转换"
```

图表来源
- [ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/ai/AiTask.java](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/ai/AiTask.java)
- [ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/dto/response/AiTaskVO.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/dto/response/AiTaskVO.java)
- [ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/AiTaskServiceImpl.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/AiTaskServiceImpl.java)

**章节来源**
- [ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/dto/response/AiTaskVO.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/dto/response/AiTaskVO.java)
- [ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/ai/AiTask.java](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/ai/AiTask.java)
- [ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/AiTaskServiceImpl.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/AiTaskServiceImpl.java)
- [ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java](file://ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/external/ExternalAiTaskService.java)
- [sql/20260713_bizId字段改为str.sql](file://sql/20260713_bizId字段改为str.sql)

### 交互客户端架构更新

**已更新** 修复了AiTaskClient中的类型安全问题，优化了文件ID处理逻辑

#### 外部认证客户端
- **AiExternalAuthClient**：专门用于AI系统的认证客户端，提供获取外部token的能力
- 通过`/external/token`接口为业务系统用户换取可跳转使用的外部token
- 集成了请求签名验证机制，确保API调用的安全性

#### AI任务客户端
- **AiTaskClient**：封装AI任务的创建、查询能力
- **类型安全修复**：文件ID参数类型已从List<String>修正为List<Long>，确保与数据库ID类型一致
  - `createTask(authorization, request, fileIds)`：接受List<Long>类型的文件ID列表，自动转换为逗号分隔的字符串格式
  - `createTask(authorization, request)`：传统方法，需要手动设置request中的fileIds字段
- 外部系统需先通过`AiExternalAuthClient.getExternalToken`获取JWT令牌
- 再将令牌传入本类各方法的`authorization`参数进行认证

#### 外部用户信息客户端
- **AiExternalUserInfoClient**：获取当前外部用户信息的客户端
- 通过`/external/userinfo`接口透传Authorization头获取用户信息

#### 文件客户端
- **AiFileClient**：封装文件信息查询、下载和上传能力
- 支持多种文件操作：getFileInfo、downloadFile、uploadFile（byte[]和Path两种方式）
- 同样需要JWT令牌进行认证

#### 自动配置注册
- 通过`EleAiTenderInteractionAutoConfiguration`自动注册各类客户端Bean
- 支持条件化配置，可根据需要启用或禁用特定功能

```mermaid
classDiagram
class EleAiTenderInteractionAutoConfiguration {
+aiExternalAuthClient()
+aiTaskClient()
+aiExternalUserInfoClient()
+aiFileClient()
}
class AiExternalAuthClient {
+getExternalToken(request)
}
class AiTaskClient {
+createTask(authorization, request, fileIds : List<Long>)
+createTask(authorization, request)
+getTask(authorization, taskId)
}
class AiExternalUserInfoClient {
+getCurrentExternalUser(authorization)
}
class AiFileClient {
+getFileInfo(authorization, fileId)
+downloadFile(authorization, fileId)
+uploadFile(authorization, content, fileName, bizType)
+uploadFile(authorization, filePath, bizType)
}
EleAiTenderInteractionAutoConfiguration --> AiExternalAuthClient
EleAiTenderInteractionAutoConfiguration --> AiTaskClient
EleAiTenderInteractionAutoConfiguration --> AiExternalUserInfoClient
EleAiTenderInteractionAutoConfiguration --> AiFileClient
AiTaskClient --> AiExternalAuthClient : "使用JWT令牌"
AiFileClient --> AiExternalAuthClient : "使用JWT令牌"
AiExternalUserInfoClient --> AiExternalAuthClient : "使用JWT令牌"
```

图表来源
- [ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/EleAiTenderInteractionAutoConfiguration.java](file://ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/EleAiTenderInteractionAutoConfiguration.java)
- [ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiExternalAuthClient.java](file://ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiExternalAuthClient.java)
- [ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiTaskClient.java](file://ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiTaskClient.java)
- [ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiExternalUserInfoClient.java](file://ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiExternalUserInfoClient.java)
- [ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiFileClient.java](file://ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiFileClient.java)

**章节来源**
- [ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/EleAiTenderInteractionAutoConfiguration.java](file://ele-ai-tender-interaction-autoconfigure/src/main/java/com/jy/eleaitender/interaction/autoconfigure/EleAiTenderInteractionAutoConfiguration.java)
- [ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiExternalAuthClient.java](file://ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiExternalAuthClient.java)
- [ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiTaskClient.java](file://ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiTaskClient.java)
- [ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiExternalUserInfoClient.java](file://ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiExternalUserInfoClient.java)
- [ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiFileClient.java](file://ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiFileClient.java)

## 依赖关系分析
- 服务间调用关系
  - core → support：用户认证、权限校验
  - core → file：文件上传/下载
  - ai → file：知识库文件管理
  - core ↔ ai：通过ai_task表异步解耦（core写入任务 → 处理器轮询执行 → core读取结果），无直接HTTP调用
- 数据库与中间件
  - MySQL：db=6
  - Redis：db=6
  - Milvus：向量检索
  - 本地磁盘：文件存储

```mermaid
graph TB
CORE["核心业务 :8082"] --> SUP["支撑中心 :8080"]
CORE --> FILE["文件服务 :8081"]
AI["AI服务 :8083"] --> FILE
CORE -.异步任务.-> AI
CORE --> DB["MySQL db=6"]
CORE --> REDIS["Redis db=6"]
AI --> DB
AI --> REDIS
AI --> MIL["Milvus"]
FILE --> DB
FILE --> DISK["本地磁盘"]
```

图表来源
- [CLAUDE.md](file://CLAUDE.md)

章节来源
- [CLAUDE.md](file://CLAUDE.md)

## 性能考虑
- 线程池与并发控制：AI服务提供动态线程池管理与用户并发度控制，避免大模型调用阻塞主线程
- 异步解耦：core与ai通过任务表异步协作，降低耦合与峰值压力
- 缓存与锁：Redis用于热点数据缓存与分布式锁，减少重复计算与竞争
- 文档处理：使用poi-tl与flexmark进行高效渲染与转换，结合内存优化策略避免OOM
- 流式响应：AI对话与长任务采用SSE流式输出，提升用户体验与资源利用率

[本节为通用指导，不直接分析具体源码文件]

## 故障排查指南
- 启动问题
  - 确认JDK版本为21，且在各子模块目录下执行mvn spring-boot:run
  - 首次或公共模块变更后需先install common，再对各依赖模块clean compile
- 端口冲突
  - 支撑中心:8080、文件服务:8081、核心业务:8082、AI服务:8083
- 依赖不一致
  - 修改common后必须重新install，并在依赖模块执行clean compile，避免运行时找不到符号
- 重启流程
  - kill全部进程 → install common → 各模块clean compile → 按顺序启动（support/file可并行，core/ai可并行）
- 前端构建
  - npm run build前检查依赖安装与环境变量，必要时切换mode（localdev/test）
- 任务来源追踪问题
  - 检查ai_task表的system_id字段是否正确填充
  - 验证外部系统JWT令牌中是否包含正确的systemId信息
  - 确认SecurityContextHolder能正确获取当前系统上下文
- 文件ID类型问题
  - 确保传递的文件ID为Long类型而非String类型
  - 检查数据库中的文件ID字段类型是否为bigint
  - 验证前端传递的参数类型是否与后端接口定义一致

章节来源
- [CLAUDE.md](file://CLAUDE.md)

## 结论
本系统以清晰的分层与模块化设计实现招标文件AI编制的端到端能力：支撑中心负责基础治理，文件服务专注文档与模板渲染，核心业务编排项目与需求生命周期并通过任务表与AI服务异步协作，AI服务承载对话、检测、匹配与模型路由。配合双前端与完善的依赖治理，系统在可维护性、扩展性与性能方面具备良好基础。

**更新亮点**：本次更新主要修复了AiTaskClient中的类型安全问题，将fileIds参数类型从List<String>修正为List<Long>，确保了与数据库ID类型的一致性。同时移除了冗余的任务状态查询功能，简化了API接口设计。这些改进提升了系统的类型安全性和代码质量，避免了潜在的运行时类型转换异常。

## 附录
- 环境变量覆盖：SPRING_DATASOURCE_PASSWORD、SPRING_REDIS_PASSWORD、APP_JWT_SECRET、DEEPSEEK_API_KEY、LOCAL_MODEL_KEY
- 本地配置覆盖：spring.config.import: optional:file:${user.home}/.ele-ai-tender/{module}-local.yml
- 链路追踪：X-Trace-Id传播，MDC键traceId
- 默认管理员：admin / 123456

章节来源
- [CLAUDE.md](file://CLAUDE.md)