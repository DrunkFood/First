---
name: tender-workflow-development
description: "Provides domain knowledge for the tender document AI generation workflow (招标文件AI编制全流程). Use when: (1) developing or debugging requirement generation, project wizard phases, detection, review items, or document integration; (2) understanding project status machine / phase flow controller / trigger mechanism; (3) working with AI task queue, model routing, or result sync; (4) tracing end-to-end business flow from requirement to archived tender document. Keywords: 招标文件, 需求编制, 项目编制, 智能检测, 评审项, 文档集成, AI任务, 状态机, 阶段流程, PhaseFlowController, ProjectStateMachine, AiTaskProcessor, RequirementGenerator, DetectionEngine."
---

# Tender Workflow Development (招标文件AI编制业务流程)

## Overview

本 skill 提供"业务需求编制 → 项目编制五阶段 → 发布归档"全流程的领域知识，覆盖状态机、阶段流程控制器、AI任务异步队列、智能检测、文档集成等核心机制。所有知识基于实际代码实现，帮助 AI 助手在开发或分析招标文件编制功能时快速定位代码、理解业务约束、避免已知陷阱。

**三阶段全流程**：
1. **业务需求编制** — 创建需求 → AI生成 → 人工编辑 → 需求检测(2项)
2. **项目编制五阶段** — 基础信息 → 招标需求 → 评审项 → 文档集成 → 智能检测(4项)
3. **发布归档** — 检测通过/跳过 → 发布 → 归档(终态)

## When to Use This Skill

- **开发核心业务功能时** — 需求管理、项目编制、评审项、文档集成、检测相关的新功能开发或修改
- **调试状态/阶段流转问题时** — 项目状态不流转、阶段无法推进、触发器未执行等
- **排查AI任务异常时** — 任务卡在PENDING、结果未同步、模型路由失败、超时降级等
- **理解跨模块调用时** — core ↔ ai 通过 ai_task 表解耦、core → file/support 直接HTTP
- **端到端流程追踪时** — 从需求创建到招标文件导出的完整链路定位

## How to Use

1. **定位业务领域**：先读 `context/_index.md`，根据问题关键词找到对应领域目录
2. **深入领域知识**：进入对应领域的 `overview.md` 了解核心概念、代码位置、业务规则
3. **查阅开发规范**：`references/development_guide.md` 提供编码规范和常见陷阱
4. **人类阅读**：`show/report.html` 可在浏览器直接打开，适合团队协作时整体浏览

## Quick Reference

### 技术基线

| 维度 | 技术栈 |
|------|--------|
| JDK | 21（不兼容 8/11） |
| 后端框架 | Spring Boot 3.2.2 · MyBatis-Plus 3.5.5 |
| AI框架 | Spring AI 1.1.0 · Milvus 2.3.3 |
| 文档引擎 | poi-tl 1.12.2 · flexmark-java 0.64.0 · Apache Tika 2.9.0 |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia |
| 数据库 | MySQL 8.4.0 · Redis · Milvus |

### 后端模块清单

| 模块 | 端口 | 职责 |
|------|------|------|
| `ele-ai-tender-common` | — | 公共实体、枚举、工具类、异常 |
| `ele-ai-tender-support` | 8080 | 认证、用户、模板、模型配置、政策文件 |
| `ele-ai-tender-file` | 8081 | 文件上传下载、Word文档生成引擎 |
| `ele-ai-tender-core` | 8082 | 项目管理、需求编制、评审项、文档集成、检测、状态机 |
| `ele-ai-tender-ai` | 8083 | AI对话、知识库、文档匹配、检测引擎、模型路由 |
| `ele-ai-tender-interaction` | — | 第三方系统接入 Starter（JDK 8 兼容） |

### 核心枚举速查

| 枚举 | 值 | 代码位置 |
|------|-----|---------|
| ProjectStatus | DRAFT→IN_PROGRESS→PENDING_DETECTION→DETECTING→PASSED/FAILED/SKIPPED→PUBLISHED→ARCHIVED | `common/enums/ProjectStatus.java` |
| ProjectPhase | BASIC_INFO(1)→REQUIREMENT(2)→REVIEW_ITEM(3)→DOCUMENT(4)→DETECTION(5) | `common/enums/ProjectPhase.java` |
| AiTaskType | 9种任务类型（3生成+4检测+1文档+1优化） | `common/enums/AiTaskType.java` |
| AiTaskStatus | PENDING→PROCESSING→COMPLETED/FAILED/AI_UNAVAILABLE/SKIPPED | `common/enums/AiTaskStatus.java` |
| DetectionType | SENSITIVE_WORD / TYPO / POLICY_REVIEW / FORMAT_CHECK | `common/enums/DetectionType.java` |
| ReviewType | COMPLIANCE / TECHNICAL / CREDIT / COMMERCIAL | `common/enums/ReviewType.java` |

### 启动命令

```bash
# 后端（各子模块目录下执行）
cd ele-ai-tender-system/ele-ai-tender-support && mvn spring-boot:run    # :8080
cd ele-ai-tender-system/ele-ai-tender-file    && mvn spring-boot:run    # :8081
cd ele-ai-tender-system/ele-ai-tender-core    && mvn spring-boot:run    # :8082
cd ele-ai-tender-system/ele-ai-tender-ai      && mvn spring-boot:run    # :8083

# 前端
cd ele-ai-tender-frontend && npm run dev           # :5173
cd ele-ai-tender-support-frontend && npm run dev   # :3060
```

> **注意**: common 模块变更后必须 `mvn install -pl ele-ai-tender-common -AM`，否则依赖模块编译报错。

## File Organization

```
tender-workflow-development/
├── SKILL.md                         # 本文件（入口导航）
├── context/                         # 业务知识（核心）
│   ├── _index.md                   # 业务知识索引（知识地图）
│   ├── requirement/                # 业务需求编制
│   │   └── overview.md
│   ├── project-lifecycle/          # 项目编制五阶段
│   │   └── overview.md
│   ├── detection/                  # 智能检测
│   │   └── overview.md
│   ├── ai-task/                    # AI任务机制
│   │   └── overview.md
│   ├── statemachine/               # 状态机与阶段流程
│   │   └── overview.md
│   └── document/                   # 文档集成与导出
│       └── overview.md
├── references/                      # 开发规范
│   └── development_guide.md
└── show/                            # HTML协作文档
    └── report.html
```

## Adding New Business Knowledge

1. 在 `context/` 下创建新领域目录（如 `context/review-item/`）
2. 编写 `overview.md`，遵循"只记录AI无法推断的知识"原则
3. 更新 `context/_index.md` 添加新领域条目
4. 更新 `show/report.html` 同步展示内容
