# CLAUDE.md - 招标文件AI编制工具

本文件定义在本仓库内工作的 Agent 协作规范。除非用户明确要求，否则优先遵循本文档。

## 项目概览

招标文件AI编制工具平台，独立部署的AI招标文件编制系统，支持嵌入第三方平台和一体机模式。

**技术基线**: JDK 21 · Spring Boot 3.2.2 · MyBatis-Plus 3.5.5 · MySQL 8.4.0

**AI技术栈**: Spring AI 1.1.0 · Milvus 2.3.3 · Apache Tika 2.9.0 · poi-tl 1.12.0 · flexmark-java 0.64.0

**前端**: 双前端项目架构
- `ele-ai-tender-support-frontend/` — 支撑中心管理后台 (端口 3060) — Vue 3 + TypeScript + Vite + Element Plus + Pinia
- `ele-ai-tender-frontend/` — AI编制业务前端 (端口 5173) — Vue 3 + TypeScript + Vite + Element Plus + Pinia + md-editor-v3 + docx-preview + diff2html

**后端**: `ele-ai-tender-system/` — Maven 多模块，7个模块

| 模块 | 端口 | 职责 | 说明 |
|------|------|------|------|
| `ele-ai-tender-common` | — | 公共实体、工具类、异常、统一响应 | JDK17+，依赖Spring Boot 3 |
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
| 业务需求 | Requirement (ai_requirement) |
| 模板 | Template (ai_template) |
| 知识库文档 | KnowledgeDocument (ai_knowledge_document) |
| 检测记录 | DetectionRecord (ai_detection_record) |
| 评审项 | ReviewItem (ai_review_item) |
| AI模型配置 | ModelConfig (ai_model_config) |

**项目类别**: LIMITED_BELOW(限额以下) / PROPERTY_TRADE(产权交易) / GOVERNMENT_PROCUREMENT(政府采购)

**项目类型**: ENGINEERING(工程) / GOODS(货物) / SERVICE(服务)

**项目状态流转**: DRAFT → IN_PROGRESS → PENDING_DETECTION → DETECTING → DETECTION_PASSED / DETECTION_FAILED → PUBLISHED → ARCHIVED / CANCELLED

**编制阶段流转**: BASIC_INFO(1) → REQUIREMENT(2) → REVIEW_ITEM(3) → DOCUMENT(4) → DETECTION(5)，由 PhaseFlowController 管控，详见 [PHASE_FLOW_SPEC.md](docs/rules/PHASE_FLOW_SPEC.md)

**检测类型**: FAIRNESS(公平性) / COMPLIANCE(合规性) / TYPO(错别字) / SENSITIVE_WORD(敏感词)

## 启动命令

**前置条件**: JDK 21（项目基于 Spring Boot 3.2.2，不兼容 JDK 8/11）

```bash
# 后端 - 首次启动需先安装依赖模块（在 ele-ai-tender-system/ 目录下执行）
mvn install -Dmaven.test.skip=true                      # 首次或依赖变更后执行

# 后端 - 启动各服务（在各子模块目录下执行）
cd ele-ai-tender-system/ele-ai-tender-support && mvn spring-boot:run    # 支撑中心 :8080
cd ele-ai-tender-system/ele-ai-tender-file    && mvn spring-boot:run    # 文件服务 :8081
cd ele-ai-tender-system/ele-ai-tender-core    && mvn spring-boot:run    # 核心业务 :8082
cd ele-ai-tender-system/ele-ai-tender-ai      && mvn spring-boot:run    # AI服务   :8083

# 前端 - 支撑中心管理后台（端口 3060）
cd ele-ai-tender-support-frontend && npm run dev

# 前端 - AI编制业务前端（端口 5173）
cd ele-ai-tender-frontend && npm run dev

# 后端 - 打包（在 ele-ai-tender-system/ 目录下执行）
mvn -pl ele-ai-tender-support -am package               # 打包单模块
```

> **注意**: 后端启动必须在**子模块目录**下执行 `mvn spring-boot:run`，不能在父POM目录用 `-pl` 启动（会因父POM无main类而报错）。首次启动前须先 `mvn install` 安装公共模块到本地仓库。

**提交前检查**: 前端 `npm run build`；后端 `mvn clean test`

## 请求链路

```
浏览器 → 支撑中心前端 (3060)
  /support-api/*   → rewrite(/api/*)    → 支撑中心 :8080  → MySQL(db=6) + Redis(db=6)
  /file-api/*      → 直接转发            → 文件服务 :8081  → MySQL(db=6) + 本地磁盘

浏览器 → AI编制前端 (5173)
  /core-api/*      → rewrite(/api/*)    → 核心业务 :8082  → MySQL(db=6) + Redis(db=6)
  /ai-api/*        → rewrite(/api/*)    → AI服务   :8083  → MySQL(db=6) + Redis(db=6) + Milvus + 大模型API
  /file-api/*      → 直接转发            → 文件服务 :8081  → MySQL(db=6) + 本地磁盘
  /support-api/*   → rewrite(/api/*)    → 支撑中心 :8080  → MySQL(db=6) + Redis(db=6)

业务系统 → Interaction Starter → /api/eleAiTender/interaction/*
```

认证双轨：内部用户 JWT (`type=INTERNAL`) / 外部系统 JWT (`type=EXTERNAL`)

## 编码规范速查

详细规范见 [CODE_CONVENTIONS.md](docs/rules/CODE_CONVENTIONS.md)，以下为高频要点：

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

**链路追踪**: 所有服务传播 `X-Trace-Id`，MDC 键 `traceId`

## AI能力架构

### 混合模型路由

```
任务类型 → ModelRouter → 本地模型(生成类) / 云端模型(优化/检测类)
```

- **本地模型**: 处理大批量生成任务，降低Token成本
- **云端模型**(DeepSeek等): 处理优化和检测任务，保证质量

### 知识库向量化流程

```
文档上传 → Apache Tika解析 → 文本分块(500-1000字/块) → Embedding API向量化 → Milvus存储 → 相似度检索 → Top-K返回
```

### 流式响应

AI助手和文本优化接口使用 SSE (Server-Sent Events) 实现流式响应，前端通过 `EventSource` 接收。

### 文档生成

Markdown模板 → flexmark-java解析 → poi-tl填充Word模板 → 导出.docx

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

## 规范文档索引（按需加载）

> 以下为详细规范文档，Agent 根据任务需要按需加载，不要一次性读取所有文档。

| 规范 | 文件 | 加载时机 |
|------|------|----------|
| **编码规范**（数据库字段、接口格式、分层、异常、安全） | [CODE_CONVENTIONS.md](docs/rules/CODE_CONVENTIONS.md) | 写代码时参考 |
| **全局项目规范**（模块、端口、安全、日志、文档治理） | [PROJECT_SPEC_FINAL.md](docs/rules/PROJECT_SPEC_FINAL.md) | 了解项目整体约束时参考 |
| **AI编制系统规范**（项目、需求、模板、评审项、检测） | [AI_TENDER_SYSTEM_SPEC.md](docs/rules/AI_TENDER_SYSTEM_SPEC.md) | 开发AI编制业务功能时参考 |
| **阶段流程控制器规范**（PhaseTrigger/PhaseFlowController/触发器/Phase-Status联动/前端集成） | [PHASE_FLOW_SPEC.md](docs/rules/PHASE_FLOW_SPEC.md) | 开发编制阶段流转、阶段触发器、AI任务自动触发时参考 |
| **文件服务规范** | [FILE_SERVICE_SPEC.md](docs/rules/FILE_SERVICE_SPEC.md) | 开发文件相关功能时参考 |
| **支撑中心规范** | [SUPPORT_SYSTEM_SPEC.md](docs/rules/SUPPORT_SYSTEM_SPEC.md) | 开发认证、权限、用户管理时参考 |
| **交互集成规范** | [INTERACTION_INTEGRATION_SPEC.md](docs/rules/INTERACTION_INTEGRATION_SPEC.md) | 开发第三方系统接入时参考 |

> 代码实现 > `docs/rules/` > 其他文档，三者冲突时以代码为准。
