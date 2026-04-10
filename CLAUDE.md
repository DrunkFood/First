# CLAUDE.md - 招标文件AI编制工具

本文件定义在本仓库内工作的 Agent 协作规范。除非用户明确要求，否则优先遵循本文档。

## 项目概览

招标文件AI编制工具平台，独立部署的AI招标文件编制系统，支持嵌入第三方平台和一体机模式。

**技术基线**: JDK 21 · Spring Boot 3.2.2 · MyBatis-Plus 3.5.5 · MySQL 8.4.0

**AI技术栈**: Spring AI 0.8.1 · Milvus 2.3.3 · Apache Tika 2.9.0 · poi-tl 1.12.0 · flexmark-java 0.64.0

**前端**: 双前端项目架构
- `ele-ai-tender-support-frontend/` — 支撑中心管理后台 (端口 5174) — Vue 3 + TypeScript + Vite + Element Plus + Pinia
- `ele-ai-tender-frontend/` — AI编制业务前端 (端口 5173) — Vue 3 + TypeScript + Vite + Element Plus + Pinia + md-editor-v3 + docx-preview + diff2html

**后端**: `ele-ai-tender-system/` — Maven 多模块，7个模块

| 模块 | 端口 | 职责 | 说明 |
|------|------|------|------|
| `ele-ai-tender-common` | — | 公共实体、工具类、异常、统一响应 | JDK8兼容，参考 ele-tender-common 设计 |
| `ele-ai-tender-common-interaction` | — | 交互协议 DTO/SPI/路径常量 | JDK8兼容，供第三方系统接入 |
| `ele-ai-tender-interaction` | — | 业务系统接入 Starter | JDK8兼容，自动配置、过滤器、回调处理 |
| `ele-ai-tender-support` | 8080 | 认证、用户、角色、菜单、模板管理、知识库管理、统计分析、AI服务配置、操作日志、消息中心 | 高度复用 ele-tender-support |
| `ele-ai-tender-file` | 8081 | 文件上传/下载/查询/删除 | 高度复用 ele-tender-file |
| `ele-ai-tender-core` | 8082 | 项目管理、业务需求编制、评审项管理、文档生成 | 核心业务模块 |
| `ele-ai-tender-ai` | 8083 | AI助手、知识库检索、智能检测、模型路由 | 全新开发的AI能力模块 |

## 术语规范

| 中文 | 代码术语 |
|------|----------|
| AI编制 | AiTender |
| 项目 | Project (ai_project) |
| 项目版本 | ProjectVersion (ai_project_version) |
| 业务需求 | Requirement (ai_requirement) |
| 模板 | Template (ai_template) |
| 知识库文档 | KnowledgeDocument (ai_knowledge_document) |
| 检测记录 | DetectionRecord (ai_detection_record) |
| 评审项 | ReviewItem (ai_review_item) |
| AI模型配置 | ModelConfig (ai_model_config) |

**项目类别**: LIMITED_BELOW(限额以下) / PROPERTY_TRADE(产权交易) / GOVERNMENT_PROCUREMENT(政府采购)

**项目类型**: ENGINEERING(工程) / GOODS(货物) / SERVICE(服务)
- 服务子类型: PROPERTY / IT_SERVICE / CONSULTING / MAINTENANCE

**项目状态流转**:
DRAFT → IN_PROGRESS → PENDING_DETECTION → DETECTING → DETECTION_PASSED / DETECTION_FAILED → PUBLISHED → ARCHIVED / CANCELLED

**检测类型**: FAIRNESS(公平性) / COMPLIANCE(合规性) / TYPO(错别字) / SENSITIVE_WORD(敏感词)

**AI模型类型**: LOCAL(本地微调) / CLOUD(云端大模型) / PRIVATE(私有化部署)

**AI使用场景**: GENERATION(生成) / OPTIMIZATION(优化) / DETECTION(检测)

**需求来源**: REFERENCE(参考历史) / AI_GENERATED(AI生成)

**匹配模式**: AUTO_MATCH(自动匹配) / MANUAL_SELECT(手动选择) / UPLOAD(上传)

## 启动命令

```bash
# 前端 - 支撑中心管理后台（端口 5174）
cd ele-ai-tender-support-frontend && npm run dev

# 前端 - AI编制业务前端（端口 5173）
cd ele-ai-tender-frontend && npm run dev

# 后端（在 ele-ai-tender-system/ 目录下执行）
mvn clean test                                          # 运行所有测试
mvn -pl ele-ai-tender-support -am spring-boot:run       # 支撑中心 :8080
mvn -pl ele-ai-tender-file -am spring-boot:run          # 文件服务 :8081
mvn -pl ele-ai-tender-core -am spring-boot:run          # 核心业务 :8082
mvn -pl ele-ai-tender-ai -am spring-boot:run            # AI服务   :8083
mvn -pl ele-ai-tender-support -am package               # 打包单模块
```

> 使用 `-pl` 时必须加 `-am`，确保依赖模块先构建。

**提交前检查**: 前端 `npm run build`；后端 `mvn clean test`

## 请求链路

```
浏览器 → 支撑中心前端 (5174)
  /api/*           → rewrite(/api/v1/*) → 支撑中心 :8080  → MySQL(db=6) + Redis(db=6)
  /api/file/*      → rewrite(/api/v1/*) → 文件服务 :8081  → MySQL(db=6) + 本地磁盘

浏览器 → AI编制前端 (5173)
  /api/core/*      → rewrite(/api/v1/*) → 核心业务 :8082  → MySQL(db=6) + Redis(db=6)
  /api/ai/*        → rewrite(/api/v1/*) → AI服务   :8083  → MySQL(db=6) + Redis(db=6) + Milvus + 大模型API
  /api/file/*      → rewrite(/api/v1/*) → 文件服务 :8081  → MySQL(db=6) + 本地磁盘

业务系统 → Interaction Starter → /api/eleAiTender/interaction/*
```

认证双轨：内部用户 JWT (`type=INTERNAL`) / 外部系统 JWT (`type=EXTERNAL`)

## 编码规范

详细规范见 [CODE_CONVENTIONS.md](docs/rules/CODE_CONVENTIONS.md)，以下为高频要点速查：

- **返回结构**: `Result.success(data)` / `Result.fail(code, msg)`；交互接口用 `InteractionResult<T>`
- **实体**: 所有实体继承 `BaseEntity`（自动填充 create/modify 时间、ver、is_delete）
- **权限**: `@RequireLogin` / `@RequirePermission("xxx")`
- **命名**: Service 接口 `I*Service`；DB 表前缀 `ai_*`（本系统）/ `sup_*`(支撑) / `file_*`(文件)
- **依赖**: 新依赖版本声明在父 POM `<dependencyManagement>`
- **Interaction**: 公开 API 保持 JDK 8 兼容；协议 DTO 只放 `ele-ai-tender-common-interaction`
- **安全**: 签名用 `SignatureUtil`(HMAC-SHA256)、密码用 `PasswordUtil`(BCrypt)、字符集必须显式 UTF-8
- **前端**: Vue 3 `<script setup>`；状态走 Pinia；API 调用放 `src/api/`
- **AI接口**: 流式响应用 `SseEmitter`；AI生成内容需人工审核机制

## 基础设施

```
MySQL : 10.11.20.50:15005/ele_ai_tender（db=6，独立于现有系统 db=5）
Redis : 10.11.20.50:16879  db=6  password=test123（独立于现有系统 db=5）
Milvus: localhost:19530  collection=ai_tender_knowledge（向量数据库）
文件存储: /data/ele-ai-tender/files（独立存储路径）
大模型API: DeepSeek等云端模型 + 本地微调模型
默认管理员: admin / admin123
```

**环境变量覆盖**: `SPRING_DATASOURCE_PASSWORD` · `SPRING_REDIS_PASSWORD` · `APP_JWT_SECRET` · `DEEPSEEK_API_KEY` · `LOCAL_MODEL_KEY`

**本地配置覆盖**（各服务均支持）:
```yaml
spring.config.import: optional:file:${user.home}/.ele-ai-tender/{module}-local.yml
```

**服务专属覆盖**: `FILE_STORAGE_BASE_PATH` · `AI_CLOUD_API_KEY` · `AI_LOCAL_MODEL_ENDPOINT` · `MILVUS_COLLECTION`

**链路追踪**: 所有服务传播 `X-Trace-Id`，MDC 键 `traceId`

## 核心接口设计

### 项目管理
```
POST   /api/v1/projects                    # 创建项目
GET    /api/v1/projects                    # 查询项目列表
GET    /api/v1/projects/{id}               # 获取项目详情
PUT    /api/v1/projects/{id}               # 更新项目
DELETE /api/v1/projects                    # 批量删除项目
POST   /api/v1/projects/{id}/generate      # AI生成招标文件
GET    /api/v1/projects/{id}/versions      # 获取版本历史
GET    /api/v1/projects/{id}/versions/compare  # 版本对比
POST   /api/v1/projects/{id}/export        # 导出Word文档
POST   /api/v1/projects/{id}/publish       # 发布项目
```

### 业务需求
```
POST   /api/v1/requirements                # 创建业务需求
GET    /api/v1/requirements                # 查询需求列表
GET    /api/v1/requirements/{id}           # 获取需求详情
PUT    /api/v1/requirements/{id}           # 更新需求
POST   /api/v1/requirements/{id}/match     # 匹配历史模板
POST   /api/v1/requirements/{id}/generate  # AI生成需求初稿
POST   /api/v1/requirements/{id}/submit    # 提交审核
```

### AI助手
```
POST   /api/v1/ai/optimize                 # 文本优化(SSE流式)
POST   /api/v1/ai/suggest                  # 获取AI建议
POST   /api/v1/ai/generate                 # AI内容生成
POST   /api/v1/ai/chat                     # 对话式AI助手
```

### 智能检测
```
POST   /api/v1/detection/start             # 启动检测
GET    /api/v1/detection/{id}/status       # 查询检测状态
GET    /api/v1/detection/{id}/result       # 获取检测结果
POST   /api/v1/detection/{id}/confirm      # 确认检测结果
```

### 知识库
```
POST   /api/v1/knowledge/documents         # 上传知识文档
GET    /api/v1/knowledge/documents         # 查询知识文档列表
DELETE /api/v1/knowledge/documents/{id}    # 删除知识文档
POST   /api/v1/knowledge/retrieve          # 检索知识(向量检索)
POST   /api/v1/knowledge/vectorize         # 手动触发向量化
```

### 评审项
```
POST   /api/v1/review-items                # 创建评审项
GET    /api/v1/review-items/{projectId}    # 查询评审项树
PUT    /api/v1/review-items/{id}           # 更新评审项
DELETE /api/v1/review-items/{id}           # 删除评审项
POST   /api/v1/review-items/generate       # AI生成评审项
GET    /api/v1/review-items/{projectId}/export  # 导出评审项JSON
```

### 模板管理
```
POST   /api/v1/templates                   # 创建模板
GET    /api/v1/templates                   # 查询模板列表
GET    /api/v1/templates/{id}              # 获取模板详情
PUT    /api/v1/templates/{id}              # 更新模板
DELETE /api/v1/templates/{id}              # 删除模板
POST   /api/v1/templates/{id}/set-default  # 设为默认模板
POST   /api/v1/templates/import            # 导入模板
GET    /api/v1/templates/{id}/export       # 导出模板
```

## 数据库设计

### 表前缀规范
- `ai_*` — AI编制系统核心表
- `sup_*` — 支撑中心表（认证、用户、角色等）
- `file_*` — 文件服务表

### 核心数据表

| 表名 | 说明 |
|------|------|
| `ai_project` | 项目表（编号、名称、类别、类型、预算、状态、模板关联） |
| `ai_project_version` | 项目版本表（版本号、内容快照JSON、变更说明） |
| `ai_requirement` | 业务需求表（需求描述、匹配模式、匹配度、内容） |
| `ai_template` | 模板表（Markdown内容、结构定义JSON、版本号、默认标记） |
| `ai_knowledge_document` | 知识库文档表（文档类别、向量集合、向量ID列表） |
| `ai_detection_record` | 检测记录表（检测类型、内容快照、结果JSON、状态） |
| `ai_review_item` | 评审项表（三级嵌套结构：parent_id + level） |
| `ai_model_config` | AI模型配置表（模型类型、API端点/密钥、参数JSON、Token用量） |

### Redis 数据结构

```
ai:task:queue:{task_id}        - Hash  AI任务队列
ai:task:status:{task_id}       - String 任务状态
ai:assistant:session:{id}      - Hash  AI助手会话上下文
ai:assistant:history:{id}      - List  对话历史
ai:detection:result:{proj_id}  - Hash  检测结果缓存
ai:token:limit:{user_id}:{date} - String 当日Token使用量
```

## AI能力架构

### 混合模型路由

```
任务类型 → ModelRouter → 本地模型(生成类) / 云端模型(优化/检测类)
```

- **本地模型**: 处理大批量生成任务，降低Token成本
- **云端模型**(DeepSeek等): 处理优化和检测任务，保证质量
- **私有模型**: 支持私有化部署场景

### 知识库向量化流程

```
文档上传 → Apache Tika解析 → 文本分块(500-1000字/块) → Embedding API向量化 → Milvus存储 → 相似度检索 → Top-K返回
```

### 流式响应

AI助手和文本优化接口使用 SSE (Server-Sent Events) 实现流式响应，前端通过 `EventSource` 接收。

### 文档生成

Markdown模板 → flexmark-java解析 → poi-tl填充Word模板 → 导出.docx

## 数据隔离策略

| 资源 | 现有EleTender系统 | AI编制系统 | 说明 |
|------|-----------------|-----------|------|
| MySQL | db=5 | db=6 | 独立数据库 |
| Redis | db=5 | db=6 | 独立缓存空间 |
| 文件存储 | /data/ele-tender/files | /data/ele-ai-tender/files | 独立存储路径 |
| 用户体系 | 复用 | 复用 | 共享支撑中心用户表 |

## 与现有EleTender系统集成

### 依赖复用

```xml
<!-- 复用现有common模块（通过Maven依赖） -->
<dependency>
    <groupId>com.jy.eletender</groupId>
    <artifactId>ele-tender-common</artifactId>
    <version>1.1.2-SNAPSHOT</version>
</dependency>
<!-- 复用文件服务客户端 -->
<dependency>
    <groupId>com.jy.eletender</groupId>
    <artifactId>ele-tender-file-client</artifactId>
    <version>1.1.2-SNAPSHOT</version>
</dependency>
<!-- 复用交互协议 -->
<dependency>
    <groupId>com.jy.eletender</groupId>
    <artifactId>ele-tender-common-interaction</artifactId>
    <version>1.1.3-SNAPSHOT</version>
</dependency>
```

### 服务调用关系

- **core模块 → support模块**: 用户认证、权限校验
- **core模块 → file模块**: 文件上传/下载
- **ai模块 → file模块**: 知识库文件管理
- **core模块 → ai模块**: AI生成、文本优化、智能检测

## 文档规范

每次执行任务前整理需求 → `docs/projects/`；生成实施计划 → `docs/plans/`

**重要**: 所有文档必须严格放到以下目录，禁止创建 `docs/superpowers/`、`docs/specs/` 等非规范路径。

| 目录 | 用途 |
|------|------|
| `docs/projects/` | 需求整理、问题分析（包括 brainstorming/spec 产出） |
| `docs/plans/` | 实施计划（对应 projects/） |
| `docs/guides/` | 集成指南、接口文档 |
| `docs/rules/` | 系统规范（权威参考） |

**规范文档索引**（需要深入了解时阅读）:

| 规范 | 文件 |
|------|------|
| **编码规范**（数据库字段、接口格式、分层、异常、安全） | [CODE_CONVENTIONS.md](docs/rules/CODE_CONVENTIONS.md) |
| 全局项目规范（模块、端口、安全、日志、文档治理） | [PROJECT_SPEC_FINAL.md](docs/rules/PROJECT_SPEC_FINAL.md) |
| AI编制系统架构优化方案 | [AI编制系统架构优化_0e4bb37a.md](docs/plans/AI编制系统架构优化_0e4bb37a.md) |
| 文件服务规范 | [FILE_SERVICE_SPEC.md](docs/rules/FILE_SERVICE_SPEC.md) |
| 支撑中心规范 | [SUPPORT_SYSTEM_SPEC.md](docs/rules/SUPPORT_SYSTEM_SPEC.md) |
| 交互集成规范 | [INTERACTION_INTEGRATION_SPEC.md](docs/rules/INTERACTION_INTEGRATION_SPEC.md) |

> 代码实现 > `docs/rules/` > 其他文档，三者冲突时以代码为准。
