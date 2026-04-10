# AI编制系统架构优化方案

基于业务需求文档和现有EleTender架构，创建完全独立的AI编制系统项目。

## 一、项目定位和架构设计

### 1.1 项目定位
**独立的AI招标文件编制系统**，可独立部署、独立运行，后续支持嵌入第三方平台（如招必得）和一体机模式。

### 1.2 整体架构图

```
┌───────────────────────────────────────────────────────────────┐
│                        前端层                                  │
├────────────────────────────┬──────────────────────────────────┤
│ ele-ai-tender-             │ ele-ai-tender-                   │
│ support-frontend :5174     │ frontend :5173                   │
│                            │                                  │
│ • 系统管理                 │ • 项目管理                       │
│ • 模板管理                 │ • 业务需求编制                   │
│ • 知识库管理               │ • AI助手                         │
│ • 统计分析                 │ • 评审项管理                     │
│ • AI服务配置               │ • 智能检测                       │
│ • 操作日志                 │ • 文档预览/导出                  │
│ • 消息中心                 │ • 版本对比                       │
└────────────┬───────────────┴────────────┬─────────────────────┘
             │                            │
             │ HTTP                       │ HTTP
    ┌────────┴────────┐          ┌────────┴────────┐
    │                 │          │                 │
    ▼                 ▼          ▼                 ▼
┌─────────┐    ┌──────────┐  ┌─────────┐    ┌──────────┐
│ support │    │  file    │  │  core   │    │   ai     │
│  :8080  │    │  :8081   │  │  :8082  │    │  :8083   │
│         │    │          │  │         │    │          │
│• 认证   │◄───│• 文件    │  │• 项目   │◄───│• AI助手  │
│• 用户   │    │• 上传    │  │• 需求   │    │• 知识库  │
│• 角色   │    │• 下载    │  │• 评审项 │    │• 检测    │
│• 菜单   │    │          │  │• 文档   │    │• 模型    │
│• 模板   │    └──────────┘  └─────────┘    └────┬─────┘
│• 知识库 │                                       │
│• 统计   │                                       │
│• 配置   │                                       │
└────┬────┘                                       │
     │                                            │
     └────────────┬───────────────────────────────┘
                  │
         ┌────────┴────────┐
         │                 │
         ▼                 ▼
   ┌──────────┐     ┌──────────┐
   │ MySQL    │     │  Redis   │
   │  db=6    │     │  db=6    │
   └──────────┘     └──────────┘

   ┌──────────┐     ┌──────────────┐
   │ Milvus   │     │  大模型API    │
   │ 向量库   │     │ DeepSeek等   │
   └──────────┘     └──────────────┘
```

### 1.3 技术栈决策

#### 后端技术栈
- **基础框架**: JDK 21 + Spring Boot 3.2.2 (与现有系统保持一致)
- **ORM框架**: MyBatis-Plus 3.5.5
- **数据库**: MySQL 8.4.0 + Redis (db=6, 独立于现有系统)
- **向量数据库**: Milvus (支持大规模向量检索) 或 Chroma (轻量级)
- **Markdown处理**: flexmark-java 0.64.0
- **Word生成**: poi-tl 1.12.0 (基于Apache POI的模板引擎)
- **大模型SDK**: 
  - OpenAI Java SDK (兼容DeepSeek等)
  - Spring AI (Spring官方AI抽象层，推荐)
- **文档解析**: Apache Tika 2.9.0 (Word/PDF解析)
- **异步处理**: Spring @Async + 线程池
- **消息队列**: RabbitMQ (可选，用于AI任务异步处理)

#### 前端技术栈
- 复用现有 `ele-tender-support-frontend` 项目骨架
- 新增依赖:
  - Markdown编辑器: md-editor-v3 或 vite-plugin-md
  - 文档预览: docx-preview 或 OnlyOffice集成
  - 版本对比: diff2html
  - AI流式响应: EventSource 或 SSE客户端

## 二、项目结构设计

### 2.1 后端项目结构 (ele-ai-tender-system)

采用**Maven多模块架构**，共7个模块，参考现有EleTender系统设计：

```
ele-ai-tender-system/
├── pom.xml                          # 父POM，管理依赖版本
│
├── ele-ai-tender-common/            # 公共模块（JDK8兼容）
│   ├── pom.xml
│   └── src/main/java/
│       └── com/jy/eleaitender/common/
│           ├── entity/              # 公共实体类
│           │   ├── BaseEntity.java
│           │   └── PageQuery.java
│           ├── constant/            # 常量定义
│           ├── enums/               # 枚举类
│           ├── exception/           # 异常体系
│           │   ├── BusinessException.java
│           │   └── GlobalExceptionHandler.java
│           ├── result/              # 统一响应Result
│           │   └── Result.java
│           └── utils/               # 工具类
│               ├── DateUtils.java
│               └── StringUtils.java
│
├── ele-ai-tender-common-interaction/  # 交互协议模块（JDK8兼容）
│   ├── pom.xml
│   └── src/main/java/
│       └── com/jy/eleaitender/interaction/
│           ├── dto/                 # 交互DTO
│           │   ├── AiProjectDTO.java
│           │   ├── AiDetectionResultDTO.java
│           │   └── AiDocumentExportDTO.java
│           ├── spi/                 # SPI接口
│           │   ├── DocumentExportCallback.java
│           │   └── DetectionResultCallback.java
│           └── constant/            # 路径常量
│               └── InteractionPaths.java
│
├── ele-ai-tender-interaction/       # 业务系统接入Starter聚合模块（JDK8兼容）
│   ├── pom.xml
│   ├── ele-ai-tender-interaction-core/           # 核心实现
│   │   ├── pom.xml
│   │   └── src/main/java/
│   │       └── com/jy/eleaitender/interaction/core/
│   │           ├── client/             # HTTP客户端
│   │           │   └── AiTenderClient.java
│   │           ├── handler/            # 回调处理器
│   │           │   ├── DocumentExportCallbackHandler.java
│   │           │   └── DetectionResultCallbackHandler.java
│   │           └── interceptor/        # 拦截器
│   │               └── AuthenticationInterceptor.java
│   ├── ele-ai-tender-interaction-autoconfigure/  # 自动配置
│   │   ├── pom.xml
│   │   └── src/main/java/
│   │       └── com/jy/eleaitender/interaction/autoconfigure/
│   │           ├── InteractionAutoConfiguration.java
│   │           └── InteractionProperties.java
│   └── ele-ai-tender-interaction-spring-boot-starter/  # Starter入口
│       ├── pom.xml
│       └── src/main/resources/
│           └── META-INF/
│               └── spring.factories    # 自动配置注册
│
├── ele-ai-tender-support/           # 支撑中心模块 :8080
│   ├── pom.xml
│   └── src/main/java/
│       └── com/jy/eleaitender/support/
│           ├── SupportApplication.java
│           ├── controller/
│           │   ├── AuthController.java          # 认证
│           │   ├── UserController.java          # 用户管理
│           │   ├── RoleController.java          # 角色管理
│           │   ├── MenuController.java          # 菜单管理
│           │   ├── VersionController.java       # 版本管理
│           │   ├── ExternalSystemController.java # 外部系统
│           │   ├── TemplateConfigController.java # 模板管理
│           │   ├── KnowledgeConfigController.java # 知识库管理
│           │   ├── StatisticsController.java    # 统计分析
│           │   ├── AiConfigController.java      # AI服务配置
│           │   ├── AccessLogController.java     # 操作日志
│           │   └── MessageController.java       # 消息中心
│           ├── service/
│           ├── mapper/
│           └── entity/
│
├── ele-ai-tender-file/              # 文件服务模块 :8081
│   ├── pom.xml
│   └── src/main/java/
│       └── com/jy/eleaitender/file/
│           ├── FileApplication.java
│           ├── controller/
│           │   └── FileController.java
│           ├── service/
│           │   ├── FileService.java
│           │   └── impl/
│           │       └── FileServiceImpl.java
│           ├── config/
│           │   └── FileStorageConfig.java
│           └── utils/
│               └── FileUtil.java
│
├── ele-ai-tender-core/              # AI编制核心业务模块 :8082
│   ├── pom.xml
│   └── src/main/java/
│       └── com/jy/eleaitender/core/
│           ├── CoreApplication.java
│           ├── controller/          # 业务接口
│           │   ├── ProjectController.java       # 项目管理
│           │   ├── RequirementController.java   # 业务需求编制
│           │   ├── DocumentController.java      # 文档生成
│           │   └── ReviewItemController.java    # 评审项管理
│           ├── service/             # 业务逻辑
│           │   ├── ProjectService.java
│           │   ├── RequirementService.java
│           │   ├── DocumentService.java
│           │   ├── ReviewItemService.java
│           │   └── impl/
│           ├── mapper/              # 数据访问
│           ├── entity/              # 实体类
│           │   ├── Project.java
│           │   ├── ProjectVersion.java
│           │   ├── Requirement.java
│           │   ├── ReviewItem.java
│           │   └── DocumentRecord.java
│           ├── dto/
│           └── engine/              # 模板引擎
│               └── MarkdownTemplateEngine.java
│
└── ele-ai-tender-ai/                # AI服务模块 :8083
    ├── pom.xml
    └── src/main/java/
        └── com/jy/eleaitender/ai/
            ├── AiApplication.java
            ├── controller/          # AI相关接口
            │   ├── AiAssistantController.java   # AI助手
            │   ├── DetectionController.java     # 智能检测
            │   └── KnowledgeController.java     # 知识库
            ├── service/             # AI服务层
            │   ├── AiService.java               # AI编排服务
            │   ├── TextOptimizationService.java # 文本优化
            │   ├── StreamResponseService.java   # 流式响应
            │   ├── DetectionService.java        # 检测服务
            │   ├── KnowledgeService.java        # 知识库服务
            │   └── impl/
            ├── model/               # 模型管理
            │   ├── ModelRouter.java             # 模型路由
            │   ├── LocalModelClient.java        # 本地模型
            │   └── CloudModelClient.java        # 云端模型
            ├── prompt/              # 提示词管理
            │   ├── PromptTemplate.java
            │   └── PromptManager.java
            ├── checker/             # 检测器
            │   ├── FairnessChecker.java
            │   ├── ComplianceChecker.java
            │   ├── TypoChecker.java
            │   └── SensitiveWordChecker.java
            ├── knowledge/           # 知识库
            │   ├── VectorService.java           # 向量服务
            │   ├── DocumentParser.java          # 文档解析
            │   └── KnowledgeRetriever.java      # 检索服务
            └── generator/           # 文档生成
                ├── WordGenerator.java
                └── MarkdownToWordConverter.java
```

### 2.2 前端项目架构

采用**双前端项目架构**，与后端服务对应：

#### 项目一：ele-ai-tender-support-frontend（支撑中心前端）

管理后台，对应后端 support 模块 (8080)

```
ele-ai-tender-support-frontend/
├── package.json
├── vite.config.ts
├── index.html
└── src/
    ├── api/
    │   ├── auth.ts              # 认证接口
    │   ├── user.ts              # 用户管理
    │   ├── role.ts              # 角色管理
    │   ├── menu.ts              # 菜单管理
    │   ├── version.ts           # 版本管理
    │   ├── external.ts          # 外部系统
    │   ├── template.ts          # 模板管理
    │   ├── knowledge.ts         # 知识库管理
    │   ├── statistics.ts        # 统计分析
    │   ├── ai-config.ts         # AI服务配置
    │   ├── access-log.ts        # 操作日志
    │   └── message.ts           # 消息中心
    │
    ├── views/
    │   ├── dashboard/           # 首页仪表盘
    │   │   └── Dashboard.vue
    │   ├── system/              # 系统管理
    │   │   ├── user/            # 用户管理
    │   │   │   ├── UserList.vue
    │   │   │   └── UserForm.vue
    │   │   ├── role/            # 角色管理
    │   │   │   ├── RoleList.vue
    │   │   │   └── RoleForm.vue
    │   │   └── menu/            # 菜单管理
    │   │       └── MenuTree.vue
    │   ├── template/            # 模板管理
    │   │   ├── TemplateList.vue
    │   │   ├── TemplateEditor.vue
    │   │   └── TemplatePreview.vue
    │   ├── knowledge/           # 知识库管理
    │   │   ├── KnowledgeList.vue
    │   │   ├── KnowledgeUpload.vue
    │   │   └── KnowledgeDetail.vue
    │   ├── statistics/          # 统计分析
    │   │   ├── ProjectStats.vue
    │   │   ├── TemplateStats.vue
    │   │   └── DetectionStats.vue
    │   ├── ai-config/           # AI服务配置
    │   │   ├── ModelConfig.vue
    │   │   ├── PromptConfig.vue
    │   │   └── TokenStats.vue
    │   ├── version/             # 版本管理
    │   │   └── VersionList.vue
    │   ├── external/            # 外部系统
    │   │   └── ExternalSystemList.vue
    │   ├── log/                 # 操作日志
    │   │   └── AccessLogList.vue
    │   └── message/             # 消息中心
    │       └── MessageList.vue
    │
    ├── components/
    │   ├── layout/              # 布局组件
    │   │   ├── MainLayout.vue
    │   │   ├── Header.vue
    │   │   └── Sidebar.vue
    │   └── common/              # 通用组件
    │       ├── Pagination.vue
    │       └── SearchForm.vue
    │
    ├── router/
    │   └── index.ts             # 路由配置
    ├── store/                   # Pinia状态管理
    │   ├── index.ts
    │   └── user.ts
    ├── types/                   # TypeScript类型定义
    │   └── index.ts
    ├── utils/                   # 工具函数
    │   ├── request.ts           # Axios封装
    │   ├── auth.ts              # 认证工具
    │   └── storage.ts           # 本地存储
    ├── App.vue
    └── main.ts
```

#### 项目二：ele-ai-tender-frontend（AI编制服务前端）

业务前端，对应后端 core (8082) 和 ai (8083) 模块

```
ele-ai-tender-frontend/
├── package.json
├── vite.config.ts
├── index.html
└── src/
    ├── api/
    │   ├── project.ts           # 项目管理API
    │   ├── requirement.ts       # 业务需求API
    │   ├── review.ts            # 评审项API
    │   ├── document.ts          # 文档生成API
    │   ├── ai.ts                # AI助手API
    │   ├── detection.ts         # 智能检测API
    │   └── file.ts              # 文件服务API
    │
    ├── views/
    │   ├── project/             # 项目管理
    │   │   ├── ProjectList.vue
    │   │   ├── ProjectCreate.vue
    │   │   ├── ProjectDetail.vue
    │   │   └── components/
    │   │       ├── VersionHistory.vue
    │   │       └── VersionCompare.vue
    │   │
    │   ├── requirement/         # 业务需求编制
    │   │   ├── RequirementList.vue
    │   │   ├── RequirementCreate.vue
    │   │   ├── RequirementEditor.vue
    │   │   └── components/
    │   │       ├── TemplateMatcher.vue
    │   │       └── HistoryFileSelector.vue
    │   │
    │   ├── review/              # 评审项管理
    │   │   ├── ReviewEditor.vue
    │   │   └── components/
    │   │       └── ReviewTreeTable.vue
    │   │
    │   ├── document/            # 文档生成
    │   │   ├── DocumentPreview.vue
    │   │   └── DocumentExport.vue
    │   │
    │   └── detection/           # 智能检测
    │       ├── DetectionPanel.vue
    │       └── DetectionReport.vue
    │
    ├── components/
    │   ├── ai/                  # AI相关组件
    │   │   ├── AiTextEditor.vue      # AI文本编辑器
    │   │   ├── AiAssistant.vue       # AI助手侧边栏
    │   │   ├── StreamResponse.vue    # 流式响应组件
    │   │   └── AiSuggestions.vue     # AI建议面板
    │   ├── markdown/            # Markdown组件
    │   │   └── MarkdownEditor.vue
    │   ├── document/            # 文档组件
    │   │   └── DocumentViewer.vue
    │   └── layout/              # 布局组件
    │       ├── MainLayout.vue
    │       ├── Header.vue
    │       └── Sidebar.vue
    │
    ├── router/
    │   └── index.ts             # 路由配置
    ├── store/                   # Pinia状态管理
    │   ├── index.ts
    │   ├── user.ts
    │   ├── project.ts
    │   ├── requirement.ts
    │   └── ai.ts
    ├── types/                   # TypeScript类型定义
    │   ├── project.ts
    │   ├── requirement.ts
    │   ├── review.ts
    │   ├── detection.ts
    │   └── ai.ts
    ├── utils/                   # 工具函数
    │   ├── request.ts           # Axios封装
    │   ├── auth.ts              # 认证工具
    │   ├── storage.ts           # 本地存储
    │   └── sse.ts               # SSE流式请求封装
    ├── App.vue
    └── main.ts
```

### 2.3 前端技术栈

#### 核心技术
- **框架**: Vue 3.4+ (Composition API)
- **语言**: TypeScript 5.3+
- **构建工具**: Vite 5.0+
- **状态管理**: Pinia 2.1+
- **路由**: Vue Router 4.2+
- **UI组件库**: Element Plus 2.5+

#### AI编制前端新增依赖
```json
{
  "dependencies": {
    "md-editor-v3": "^4.10.0",        // Markdown编辑器
    "docx-preview": "^0.3.1",         // Word文档预览
    "diff2html": "^3.4.45",           // 版本对比
    "highlight.js": "^11.9.0"         // 代码高亮
  }
}
```

#### 支撑中心前端依赖
与现有 `ele-tender-support-frontend` 保持一致

### 2.4 前端开发配置

#### Vite代理配置（ele-ai-tender-frontend）

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      // 代理到core模块 (8082)
      '/api/core': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/core/, '/api/v1')
      },
      // 代理到ai模块 (8083)
      '/api/ai': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/ai/, '/api/v1')
      },
      // 代理到file模块 (8081)
      '/api/file': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/file/, '/api/v1')
      }
    }
  }
})
```

#### Vite代理配置（ele-ai-tender-support-frontend）

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    port: 5174,
    proxy: {
      // 代理到file模块 (8081) — 必须在 /api 之前，否则会被 /api 先匹配
      '/api/file': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/file/, '/api/v1')
      },
      // 代理到support模块 (8080)
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api/v1')
      }
    }
  }
})
```

## 三、数据库设计

### 3.1 核心数据表

#### 项目管理相关
```sql
-- 项目表
CREATE TABLE ai_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_code VARCHAR(50) NOT NULL UNIQUE COMMENT '项目编号',
    project_name VARCHAR(100) NOT NULL COMMENT '项目名称',
    project_category VARCHAR(20) NOT NULL COMMENT '项目类别:LIMITED_BELOW/PROPERTY_TRADE/GOVERNMENT_PROCUREMENT',
    project_type VARCHAR(20) NOT NULL COMMENT '项目类型:ENGINEERING/GOODS/SERVICE',
    service_sub_type VARCHAR(50) COMMENT '服务子类型:PROPERTY/IT_SERVICE/CONSULTING/MAINTENANCE',
    budget DECIMAL(15,2) COMMENT '预算金额(万元)',
    review_type VARCHAR(20) COMMENT '评审类型:MANUAL/INTELLIGENT',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT/IN_PROGRESS/PENDING_DETECTION/DETECTING/DETECTION_PASSED/DETECTION_FAILED/PUBLISHED/ARCHIVED/CANCELLED',
    template_id BIGINT COMMENT '使用的模板ID',
    requirement_source VARCHAR(20) COMMENT '需求来源:REFERENCE/AI_GENERATED',
    requirement_content TEXT COMMENT '招标需求内容',
    creator_id BIGINT COMMENT '创建人ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME COMMENT '删除时间(软删除)',
    INDEX idx_status (status),
    INDEX idx_category (project_category),
    INDEX idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI编制项目表';

-- 项目版本表
CREATE TABLE ai_project_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    version_no INT NOT NULL COMMENT '版本号',
    content_snapshot JSON COMMENT '内容快照',
    change_description VARCHAR(500) COMMENT '变更说明',
    creator_id BIGINT COMMENT '创建人ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_version (project_id, version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目版本表';
```

#### 业务需求相关
```sql
-- 业务需求表
CREATE TABLE ai_requirement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    requirement_name VARCHAR(100) NOT NULL COMMENT '需求名称',
    project_category VARCHAR(20) NOT NULL COMMENT '项目类别',
    project_type VARCHAR(20) NOT NULL COMMENT '项目类型',
    budget DECIMAL(15,2) COMMENT '预算价(万元)',
    requirement_description VARCHAR(500) COMMENT '需求描述',
    match_mode VARCHAR(20) COMMENT '匹配模式:AUTO_MATCH/MANUAL_SELECT/UPLOAD',
    matched_file_id BIGINT COMMENT '匹配的历史文件ID',
    matched_similarity DECIMAL(5,2) COMMENT '匹配度百分比',
    uploaded_file_id BIGINT COMMENT '上传的文件ID',
    content TEXT COMMENT '业务需求内容',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态',
    creator_id BIGINT COMMENT '创建人ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务需求表';
```

#### 模板管理相关
```sql
-- 模板表
CREATE TABLE ai_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_code VARCHAR(50) NOT NULL UNIQUE COMMENT '模板编码',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    project_category VARCHAR(20) NOT NULL COMMENT '适用项目类别',
    project_type VARCHAR(20) NOT NULL COMMENT '适用项目类型',
    content LONGTEXT NOT NULL COMMENT '模板内容(Markdown格式)',
    structure_definition JSON COMMENT '模板结构定义',
    version_no INT DEFAULT 1 COMMENT '版本号',
    is_default TINYINT(1) DEFAULT 0 COMMENT '是否默认模板',
    status VARCHAR(20) DEFAULT 'ENABLED' COMMENT '状态:ENABLED/DISABLED',
    creator_id BIGINT COMMENT '创建人ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category_type (project_category, project_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标文件模板表';
```

#### 知识库相关
```sql
-- 知识库文档表
CREATE TABLE ai_knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_name VARCHAR(200) NOT NULL COMMENT '文档名称',
    doc_category VARCHAR(50) COMMENT '文档类别:POLICY/HISTORY_TEMPLATE/STANDARD',
    file_id BIGINT NOT NULL COMMENT '文件ID(关联文件服务)',
    file_type VARCHAR(20) COMMENT '文件类型:DOC/DOCX/PDF',
    content TEXT COMMENT '文档文本内容',
    vector_collection VARCHAR(100) COMMENT '向量集合名称',
    vector_ids JSON COMMENT '向量ID列表',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE/ARCHIVED',
    creator_id BIGINT COMMENT '创建人ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (doc_category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';
```

#### 智能检测相关
```sql
-- 检测记录表
CREATE TABLE ai_detection_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    detection_type VARCHAR(50) COMMENT '检测类型:FAIRNESS/COMPLIANCE/TYPO/SENSITIVE_WORD',
    content_snapshot TEXT COMMENT '检测内容快照',
    result JSON COMMENT '检测结果',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态:PENDING/RUNNING/PASSED/FAILED',
    started_at DATETIME COMMENT '开始时间',
    completed_at DATETIME COMMENT '完成时间',
    creator_id BIGINT COMMENT '创建人ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project (project_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测记录表';
```

#### 评审项相关
```sql
-- 评审项表
CREATE TABLE ai_review_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID(0表示顶级)',
    level INT DEFAULT 1 COMMENT '层级:1/2/3',
    item_name VARCHAR(200) NOT NULL COMMENT '评审项名称',
    item_content TEXT COMMENT '评审项内容',
    sort_order INT DEFAULT 0 COMMENT '排序',
    creator_id BIGINT COMMENT '创建人ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project (project_id),
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评审项表';
```

#### AI模型配置相关
```sql
-- AI模型配置表
CREATE TABLE ai_model_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    model_type VARCHAR(20) NOT NULL COMMENT '模型类型:LOCAL/CLOUD/PRIVATE',
    api_endpoint VARCHAR(500) COMMENT 'API端点',
    api_key VARCHAR(500) COMMENT 'API密钥(加密存储)',
    model_params JSON COMMENT '模型参数',
    usage_scenario VARCHAR(50) COMMENT '使用场景:GENERATION/OPTIMIZATION/DETECTION',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    token_usage BIGINT DEFAULT 0 COMMENT 'Token使用量',
    cost DECIMAL(10,2) DEFAULT 0.00 COMMENT '累计费用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';
```

### 3.2 Redis数据结构

```
# AI任务队列
ai:task:queue:{task_id} - Hash存储任务信息
ai:task:status:{task_id} - String存储任务状态

# AI助手会话
ai:assistant:session:{session_id} - Hash存储会话上下文
ai:assistant:history:{session_id} - List存储对话历史

# 检测缓存
ai:detection:result:{project_id} - Hash存储检测结果

# Token限流
ai:token:limit:{user_id}:{date} - String存储当日Token使用量
```

## 四、核心接口设计

### 4.1 项目管理接口

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

### 4.2 业务需求接口

```
POST   /api/v1/requirements                # 创建业务需求
GET    /api/v1/requirements                # 查询需求列表
GET    /api/v1/requirements/{id}           # 获取需求详情
PUT    /api/v1/requirements/{id}           # 更新需求
POST   /api/v1/requirements/{id}/match     # 匹配历史模板
POST   /api/v1/requirements/{id}/generate  # AI生成需求初稿
POST   /api/v1/requirements/{id}/submit    # 提交审核
```

### 4.3 模板管理接口

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

### 4.4 AI助手接口

```
POST   /api/v1/ai/optimize                 # 文本优化(SSE流式)
POST   /api/v1/ai/suggest                  # 获取AI建议
POST   /api/v1/ai/generate                 # AI内容生成
POST   /api/v1/ai/chat                     # 对话式AI助手
```

### 4.5 智能检测接口

```
POST   /api/v1/detection/start             # 启动检测
GET    /api/v1/detection/{id}/status       # 查询检测状态
GET    /api/v1/detection/{id}/result       # 获取检测结果
POST   /api/v1/detection/{id}/confirm      # 确认检测结果
```

### 4.6 知识库接口

```
POST   /api/v1/knowledge/documents         # 上传知识文档
GET    /api/v1/knowledge/documents         # 查询知识文档列表
DELETE /api/v1/knowledge/documents/{id}    # 删除知识文档
POST   /api/v1/knowledge/retrieve          # 检索知识(向量检索)
POST   /api/v1/knowledge/vectorize         # 手动触发向量化
```

### 4.7 评审项接口

```
POST   /api/v1/review-items                # 创建评审项
GET    /api/v1/review-items/{projectId}    # 查询评审项树
PUT    /api/v1/review-items/{id}           # 更新评审项
DELETE /api/v1/review-items/{id}           # 删除评审项
POST   /api/v1/review-items/generate       # AI生成评审项
GET    /api/v1/review-items/{projectId}/export  # 导出评审项JSON
```

## 五、与现有系统集成方案

### 5.1 模块职责和端口分配

| 模块 | 端口 | 职责 | 复用策略 |
|------|------|------|----------|
| **ele-ai-tender-common** | — | 公共实体、工具类、异常、统一响应 | 参考ele-tender-common设计 |
| **ele-ai-tender-common-interaction** | — | 交互协议DTO/SPI/路径常量 | 参考ele-tender-common-interaction |
| **ele-ai-tender-interaction** | — | 业务系统接入Starter（含core/autoconfigure/spring-boot-starter三子模块，JDK8兼容） | 参考ele-tender-interaction |
| **ele-ai-tender-support** | 8080 | 认证、用户、角色、菜单、版本、外部系统、模板管理、知识库管理、统计分析、AI服务配置、操作日志、消息中心 | 高度复用ele-tender-support |
| **ele-ai-tender-file** | 8081 | 文件上传/下载/查询/删除 | 高度复用ele-tender-file |
| **ele-ai-tender-core** | 8082 | AI编制服务核心业务（业务需求编制、项目管理、文档生成、评审项管理） | 参考ele-tender-tender-document |
| **ele-ai-tender-ai** | 8083 | AI服务（模型调用、知识库、智能检测） | 全新开发 |

### 5.2 复用策略

本项目为完全独立系统，**不直接依赖现有EleTender模块**，而是参考其设计、高度复用其代码实现：

#### 高度复用（复制代码并适配）
参考现有模块的设计和核心代码，复制到新项目中并根据AI编制场景适配：

| 源模块 | 复用到 | 复用内容 | 适配点 |
|--------|--------|----------|--------|
| ele-tender-common | ele-ai-tender-common | JWT认证、BCrypt密码、统一响应Result、异常体系、工具类 | 包名改为com.jy.eleaitender.common |
| ele-tender-support | ele-ai-tender-support | 用户管理、角色管理、菜单权限、操作日志 | 新增模板管理、知识库管理、AI服务配置等管理端功能 |
| ele-tender-file | ele-ai-tender-file | 文件上传/下载/秒传、SHA256校验 | 存储路径改为/data/ele-ai-tender/files |
| ele-tender-common-interaction | ele-ai-tender-common-interaction | 交互协议DTO、SPI接口、路径常量 | 新增AI相关DTO（AiProjectDTO等） |
| ele-tender-interaction | ele-ai-tender-interaction | Starter三子模块结构（core/autoconfigure/spring-boot-starter）、自动配置、过滤器、回调处理 | 新增AI服务回调接口 |
| ele-tender-tender-document | ele-ai-tender-core | 编制流程状态机、评审规则树结构、版本快照机制 | 简化流程、增加AI生成能力 |
| 前端骨架 | ele-ai-tender-*-frontend | Vue3+TS+Pinia+Element Plus项目结构 | 新增AI编辑器、检测面板等页面 |

#### 参考设计（重新实现）

| 源模块 | 参考内容 | 新实现 |
|--------|----------|--------|
| ele-tender-crypto | AES加密框架 | AI系统专用密钥加密（API Key存储） |
| 交互层SPI | 回调机制 | AI生成回调、知识库查询接口 |

#### 服务调用（通过HTTP API）
- **core模块调用support模块**: 用户认证、权限校验
- **core模块调用file模块**: 文件上传/下载
- **ai模块调用file模块**: 知识库文件管理
- **core模块调用ai模块**: AI生成、文本优化、智能检测

### 5.3 数据隔离策略

| 资源 | 现有系统 | AI系统 | 说明 |
|------|---------|--------|------|
| MySQL | db=5 | db=6 | 独立数据库，避免数据污染 |
| Redis | db=5 | db=6 | 独立缓存空间 |
| 文件存储 | /data/ele-tender/files | /data/ele-ai-tender/files | 独立存储路径 |
| 用户体系 | 复用 | 复用 | 共享ele-tender-support用户表 |

### 5.4 交互协议扩展

在 `ele-tender-common-interaction` 中新增AI相关DTO：

```java
// AiProjectDTO.java
public class AiProjectDTO {
    private String projectCode;
    private String projectName;
    private String projectCategory;
    private String projectType;
    private BigDecimal budget;
    private String documentContent;
    private List<ReviewItemDTO> reviewItems;
    // ... getters/setters
}

// AiDetectionResultDTO.java
public class AiDetectionResultDTO {
    private String projectId;
    private String detectionType;
    private Boolean passed;
    private List<DetectionIssue> issues;
    // ... getters/setters
}
```

## 六、AI能力实现方案

### 6.1 混合模型架构

```java
// ModelRouter.java - 模型路由
@Service
public class ModelRouter {
    
    @Autowired
    private LocalModelClient localModelClient;    // 本地微调模型
    @Autowired
    private CloudModelClient cloudModelClient;    // 云端大模型
    
    /**
     * 根据任务类型路由到不同模型
     */
    public AiResponse route(AiTask task) {
        return switch (task.getType()) {
            case GENERATION -> localModelClient.generate(task);  // 生成类任务用本地模型
            case OPTIMIZATION -> cloudModelClient.optimize(task); // 优化类任务用云端模型
            case DETECTION -> cloudModelClient.detect(task);      // 检测类任务用云端模型
        };
    }
}
```

### 6.2 流式响应实现

```java
// StreamResponseService.java
@Service
public class StreamResponseService {
    
    public SseEmitter streamOptimize(String text, String instruction) {
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        
        // 异步调用AI模型
        CompletableFuture.runAsync(() -> {
            try {
                cloudModelClient.streamChat(text, instruction, chunk -> {
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
}
```

### 6.3 知识库向量化流程

```
1. 文档上传 → Apache Tika解析文本
2. 文本分块 → 按段落/章节切分(500-1000字/块)
3. 向量化 → 调用Embedding API生成向量
4. 存储 → 向量存入Milvus，元数据存入MySQL
5. 检索 → 用户查询 → Embedding → 向量相似度检索 → 返回Top-K相关片段
```

## 七、实施路径

### 阶段一：项目初始化（1-2周）
- [ ] 后端：创建 `ele-ai-tender-system` 父POM
- [ ] 后端：搭建7个模块基础结构（common、common-interaction、interaction、support、file、core、ai）
- [ ] 后端：配置MySQL/Redis/Milvus连接
- [ ] 后端：support模块实现认证、用户、角色、菜单等基础功能
- [ ] 后端：file模块实现文件上传/下载服务
- [ ] 前端：初始化 `ele-ai-tender-support-frontend`（复制ele-tender-support-frontend）
- [ ] 前端：初始化 `ele-ai-tender-frontend`（基于Vue3+TS+Vite）
- [ ] 前端：配置Vite代理、路由、状态管理
- [ ] 配置开发环境和CI/CD

### 阶段二：核心业务开发（3-4周）
- [ ] 后端core模块：项目管理（CRUD + 版本管理）
- [ ] 后端core模块：业务需求编制（需求创建 + 历史匹配）
- [ ] 后端core模块：评审项管理（三级嵌套结构）
- [ ] 后端core模块：文档生成（Markdown → Word转换）
- [ ] 后端support模块：模板管理、知识库管理配置接口
- [ ] 前端support-frontend：模板管理、知识库管理页面
- [ ] 前端frontend：项目管理页面（列表、创建、详情）
- [ ] 前端frontend：业务需求编制页面

### 阶段三：AI能力开发（2-3周）
- [ ] 后端ai模块：集成DeepSeek等云端模型
- [ ] 后端ai模块：AI助手（文本优化 + 实时建议 + 流式响应）
- [ ] 后端ai模块：知识库（文档上传 + 向量化 + 检索）
- [ ] 后端ai模块：智能检测（公平性、合规性、错别字、敏感词）
- [ ] 后端ai模块：模型路由和提示词管理
- [ ] 前端frontend：AI文本编辑器组件
- [ ] 前端frontend：AI助手侧边栏
- [ ] 前端frontend：智能检测面板
- [ ] 前端frontend：版本对比功能

### 阶段四：联调优化（2周）
- [ ] 模块间接口联调（core调用ai服务）
- [ ] 前端frontend：文档预览和导出功能
- [ ] 前端support-frontend：统计分析页面
- [ ] 前端support-frontend：AI服务配置页面
- [ ] 性能优化（缓存、异步处理）
- [ ] 安全加固（Token限流、内容审核）
- [ ] 前端响应式适配

### 阶段五：测试上线（1-2周）
- [ ] 单元测试、集成测试
- [ ] 压力测试（AI并发调用）
- [ ] 用户验收测试
- [ ] 生产环境部署
- [ ] 监控告警配置

## 八、关键技术难点和解决方案

### 8.1 Markdown转Word格式保真
**问题**: Markdown转Word时表格、样式丢失  
**方案**: 
- 使用poi-tl模板引擎，预定义Word模板
- Markdown解析后填充到模板占位符
- 自定义样式映射规则（标题层级、表格样式）

### 8.2 AI生成内容质量控制
**问题**: AI生成内容可能存在格式混乱、内容不准确  
**方案**:
- 提示词模板化，限制输出格式（JSON Schema约束）
- 后处理校验（格式检查、必填项验证）
- 人工审核机制（生成后必须人工确认）
- 版本追溯（保留所有AI生成历史）

### 8.3 大模型成本控制
**问题**: 外网调用Token消耗大  
**方案**:
- 生成本地化（本地微调模型处理大批量生成）
- 结果缓存（相同输入直接返回缓存结果）
- Token配额管理（按用户/项目设置上限）
- 调用监控和告警

### 8.4 向量检索性能
**问题**: 知识库文档增多后检索变慢  
**方案**:
- Milvus分布式部署（支持亿级向量）
- 向量索引优化（IVF_FLAT/HNSW）
- 检索结果缓存（Redis缓存高频查询）
- 定期重建索引

## 九、安全和合规

### 9.1 数据安全
- 敏感信息加密存储（API密钥、用户数据）
- 传输加密（HTTPS + API签名）
- 访问控制（RBAC权限模型）
- 操作审计（日志记录所有关键操作）

### 9.2 内容安全
- AI生成内容审核（敏感词过滤 + 人工审核）
- 强制脱敏（检测环节隐藏项目名称、预算等）
- 内容溯源（保留AI生成记录）

### 9.3 模型安全
- API密钥加密存储（AES加密）
- 调用频率限制（Redis计数器）
- 异常调用检测（风控告警）

## 十、监控和运维

### 10.1 监控指标
- AI接口响应时间
- Token使用量和费用
- 检测通过率
- 知识库检索准确率
- 系统可用性

### 10.2 日志规范
- 业务日志（项目操作、文档生成）
- AI调用日志（模型、Token、耗时、费用）
- 错误日志（异常堆栈、上下文）
- 审计日志（用户操作、权限变更）

### 10.3 告警策略
- AI接口响应时间 > 30s
- 单日Token消耗 > 阈值
- 检测失败率 > 10%
- 系统错误率 > 5%

## 十一、项目配置示例

### 11.1 父POM关键配置

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.jy.eleaitender</groupId>
    <artifactId>ele-ai-tender-system</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>Ele AI Tender System</name>
    <description>招标文件AI编制系统</description>

    <modules>
        <module>ele-ai-tender-common</module>
        <module>ele-ai-tender-common-interaction</module>
        <module>ele-ai-tender-interaction</module>
        <module>ele-ai-tender-support</module>
        <module>ele-ai-tender-file</module>
        <module>ele-ai-tender-core</module>
        <module>ele-ai-tender-ai</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <spring-boot.version>3.2.2</spring-boot.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <mysql.version>8.4.0</mysql.version>
        
        <!-- AI相关依赖 -->
        <spring-ai.version>0.8.1</spring-ai.version>
        <milvus-sdk.version>2.3.3</milvus-sdk.version>
        <tika.version>2.9.0</tika.version>
        <poi-tl.version>1.12.0</poi-tl.version>
        <flexmark.version>0.64.0</flexmark.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- 内部模块 -->
            <dependency>
                <groupId>com.jy.eleaitender</groupId>
                <artifactId>ele-ai-tender-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.jy.eleaitender</groupId>
                <artifactId>ele-ai-tender-common-interaction</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.jy.eleaitender</groupId>
                <artifactId>ele-ai-tender-interaction-core</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.jy.eleaitender</groupId>
                <artifactId>ele-ai-tender-interaction-autoconfigure</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.jy.eleaitender</groupId>
                <artifactId>ele-ai-tender-interaction-spring-boot-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.jy.eleaitender</groupId>
                <artifactId>ele-ai-tender-support</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.jy.eleaitender</groupId>
                <artifactId>ele-ai-tender-file</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.jy.eleaitender</groupId>
                <artifactId>ele-ai-tender-core</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.jy.eleaitender</groupId>
                <artifactId>ele-ai-tender-ai</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 11.2 核心配置文件

```yaml
# application.yml
spring:
  application:
    name: ele-ai-tender
  
  datasource:
    url: jdbc:mysql://localhost:3306/ele_ai_tender?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}
    
  data:
    redis:
      host: localhost
      port: 6379
      database: 6
      password: ${REDIS_PASSWORD}

# AI模型配置
ai:
  models:
    local:
      endpoint: http://localhost:8000/v1
      api-key: ${LOCAL_MODEL_KEY}
    cloud:
      provider: deepseek
      endpoint: https://api.deepseek.com/v1
      api-key: ${DEEPSEEK_API_KEY}
  
  # Token配额
  token:
    daily-limit: 100000
    warning-threshold: 80000

# Milvus配置
milvus:
  host: localhost
  port: 19530
  collection: ai_tender_knowledge

# 文件服务配置
file:
  service:
    endpoint: http://localhost:8081
    base-path: /data/ele-ai-tender/files
```

## 十二、总结

### 架构优势
1. **架构清晰**: 后端7个模块 + 前端2个项目，职责明确，四层分离
2. **独立部署**: 各服务可独立部署和扩容（support:8080, file:8081, core:8082, ai:8083）
3. **前后端分离**: 支撑中心前端和AI编制前端独立开发和部署
4. **高度复用**: 参考EleTender现有架构设计，复制核心代码并适配，无Maven依赖耦合
5. **弹性扩展**: AI服务可独立扩容，支持多模型切换
6. **数据隔离**: 独立数据库和缓存，避免数据污染
7. **外部接入**: interaction模块支持第三方系统接入（JDK8兼容）
8. **渐进式演进**: 一期聚焦核心功能，后续逐步完善

### 模块设计原则

#### 后端模块
- **ele-ai-tender-common**: 公共实体、工具类、异常体系（JDK8兼容）
- **ele-ai-tender-common-interaction**: 交互协议DTO/SPI（JDK8兼容，供第三方系统接入）
- **ele-ai-tender-interaction**: 业务系统接入Starter聚合模块（含core/autoconfigure/spring-boot-starter三个子模块，JDK8兼容）
- **ele-ai-tender-support** (8080): 支撑中心（认证、权限、模板配置、知识库配置、统计等）
- **ele-ai-tender-file** (8081): 文件服务（独立部署，供所有模块调用）
- **ele-ai-tender-core** (8082): 核心业务（项目、需求、评审项、文档生成）
- **ele-ai-tender-ai** (8083): AI能力（模型调用、知识库检索、智能检测）

#### 前端项目
- **ele-ai-tender-support-frontend** (5174): 支撑中心管理后台，对应support模块
- **ele-ai-tender-frontend** (5173): AI编制业务前端，对应core和ai模块

### 关键决策
1. **技术栈选择**: Spring AI（官方抽象层）+ Milvus（向量检索）+ poi-tl（Word生成）
2. **部署模式**: 多服务独立部署（support:8080, file:8081, core:8082, ai:8083）
3. **前端架构**: 双前端项目（支撑中心5174 + AI编制5173）
4. **AI策略**: 混合模型架构（生成本地化 + 优化云端化）
5. **数据库**: 独立MySQL实例（db=6），独立Redis（db=6）
6. **模块粒度**: 参考EleTender现有架构，分为支撑、文件、核心业务、AI服务四层；高度复用现有代码，无Maven依赖耦合
7. **JDK兼容**: common和interaction模块JDK8兼容，供外部系统使用

### 风险控制
1. **技术风险**: 向量检索和Markdown转Word需技术预研（1周POC）
2. **成本风险**: Token消耗需严格监控和限流
3. **质量风险**: AI生成内容需人工审核机制
4. **进度风险**: 一期聚焦四大核心模块，确保五一前上线
