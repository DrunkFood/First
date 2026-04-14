# tender-document-tool

招标文件AI编制工具主仓库，包含AI编制系统的前后端完整实现。系统支持独立部署、嵌入第三方平台和一体机模式。

## 项目简介

招标文件AI编制工具是一个基于AI的招标文件智能编制平台，提供项目管理、业务需求编制、AI内容生成、智能检测、知识库管理等核心能力。

**技术基线**: JDK 21 · Spring Boot 3.2.2 · MyBatis-Plus 3.5.5 · MySQL 8.4.0

**AI技术栈**: Spring AI 1.1.0 · Milvus 2.3.3 · Apache Tika 2.9.0 · poi-tl 1.12.0 · flexmark-java 0.64.0

## 仓库结构

### 前端项目

- `ele-ai-tender-support-frontend/` — 支撑中心管理后台 (端口 5174)
  - Vue 3 + TypeScript + Vite + Element Plus + Pinia
- `ele-ai-tender-frontend/` — AI编制业务前端 (端口 5173)
  - Vue 3 + TypeScript + Vite + Element Plus + Pinia
  - 特色依赖: md-editor-v3, docx-preview, diff2html

### 后端服务

`ele-ai-tender-system/` — Maven 多模块工程，7个模块

| 模块 | 端口 | 职责 |
|------|------|------|
| `ele-ai-tender-common` | — | 公共实体、工具类、异常、统一响应 |
| `ele-ai-tender-common-interaction` | — | 交互协议 DTO/SPI/路径常量 (JDK8兼容) |
| `ele-ai-tender-interaction` | — | 业务系统接入 Starter (JDK8兼容) |
| `ele-ai-tender-support` | 8080 | 认证、用户、角色、菜单、模板管理、知识库管理、统计分析、AI服务配置、操作日志、消息中心 |
| `ele-ai-tender-file` | 8081 | 文件上传/下载/查询/删除 |
| `ele-ai-tender-core` | 8082 | 项目管理、业务需求编制、评审项管理、文档生成 |
| `ele-ai-tender-ai` | 8083 | AI助手、知识库检索、智能检测、模型路由 |

### 文档

- `docs/rules/` — 系统规范与模块规范
- `docs/guides/` — 接入指南、工具说明
- `docs/projects/` — 需求整理、分析记录
- `docs/plans/` — 实施计划

## 核心能力

系统功能划分：
- 支撑中心ele-ai-tender-support负责系统管理和系统配置。
- 招标文件编制中心ele-ai-tender-core负责业务基础交互，流程交互，文档格式转换。
- ai服务ele-ai-tender-ai负责所有与ai交互的内容。

注意！ai服务为招标文件编制中心的业务流程提供ai业务增强，当ai服务无法启动时也不影响招标文件编制中心的业务流程。

### 支撑中心 (8080)
- 用户认证（JWT + Redis双校验）
- 用户/角色/菜单管理（RBAC权限模型）
- 系统参数管理
- AI服务配置（模型配置、Token管理、模型路由（根据ai服务中用到的模型进行智能路由管理））
- 系统政策文件管理（政策文件的上传、分类、查看、删除）
- 模板管理（Markdown模板CRUD）
- 知识库管理（文档上传、分类管理）
- 统计分析（项目、模板、检测统计）
- 系统消息中心管理
- 操作日志

### 编制中心 (8082)
- 业务需求编制（需求创建、需求编辑、需求删除，调用AI服务实现需求匹配、对话式微调、智能检测）
- 项目管理（CRUD + 版本管理 + 文件编制（文件编制流程：1.基础信息录入 2.详细需求生成 3.评审项设置（支持多层嵌套结构） 4.文档集成（文档生成（Markdown → Word转换））5.智能检测（调用AI服务,智能检测））
- 用户政策文件管理（政策文件的上传、分类、查看、删除）
- 用户消息中心（项目检测通知、文档集成通知、项目创建通知、模板更新通知、系统维护通知等）

### 文件服务 (8081)
- 通用文件上传/下载/查询/删除
- 文件SHA-256摘要校验
- 最大文件大小500MB

### AI服务 (8083)
- AI助手（对话式助手、文本优化、SSE流式响应）
- 知识库（文档上传、向量化、Milvus检索）
- 文档匹配（历史业务需求匹配）
- 文档生成（详细需求生成）
- 智能检测（公平性、合规性、错别字、敏感词等，根据内容自动生成检查项）
- 模型路由（本地模型/云端模型智能路由）

### 交互层 (Starter)
- 业务系统接入协议
- SPI接口与回调处理
- 统一客户端（出站调用）
- JDK 8兼容，支持Spring Boot 2.7.x+

## 前端代理

### AI编制前端 (5173)

| 前端路径 | 目标服务 | 路径重写 |
|---------|---------|---------|
| `/core-api/*` | core :8082 | `/core-api/` → `/api/` |
| `/ai-api/*` | ai :8083 | `/ai-api/` → `/api/` |
| `/file-api/*` | file :8081 | 无重写 |
| `/support-api/*` | support :8080 | `/support-api/` → `/api/` |

### 支撑中心前端 (5174)

| 前端路径 | 目标服务 | 路径重写 |
|---------|---------|---------|
| `/support-api/*` | support :8080 | `/support-api/` → `/api/` |
| `/file-api/*` | file :8081 | 无重写 |

## 快速启动

### 前端

```bash
# 支撑中心管理后台（端口 5174）
cd ele-ai-tender-support-frontend
npm install
npm run dev

# AI编制业务前端（端口 5173）
cd ele-ai-tender-frontend
npm install
npm run dev
```

### 后端

```bash
cd ele-ai-tender-system

# 运行所有测试
mvn clean test

# 启动支撑中心
mvn -pl ele-ai-tender-support -am spring-boot:run

# 启动文件服务
mvn -pl ele-ai-tender-file -am spring-boot:run

# 启动核心业务
mvn -pl ele-ai-tender-core -am spring-boot:run

# 启动AI服务
mvn -pl ele-ai-tender-ai -am spring-boot:run
```

### 测试与打包

```bash
cd ele-ai-tender-system

# 运行所有测试
mvn clean test

# 打包单模块
mvn -pl ele-ai-tender-support -am package

# 构建交互 starter
mvn -pl ele-ai-tender-common-interaction,ele-ai-tender-interaction/ele-ai-tender-interaction-core,ele-ai-tender-interaction/ele-ai-tender-interaction-autoconfigure,ele-ai-tender-interaction/ele-ai-tender-interaction-spring-boot-starter -am package -DskipTests
```

## 关键规范入口

详细架构设计见 [系统架构设计文档](docs/guides/系统架构设计文档.md)

## 接入与开发文档

- 业务系统接入 starter：`docs/guides/业务系统接入手册.md`
