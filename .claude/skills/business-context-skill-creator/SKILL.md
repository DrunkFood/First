---
name: business-context-skill-creator
description: "创建业务上下文 skill 的专用指南。当用户需要为业务模块创建领域知识 skill 时使用。触发词：创建业务 skill、创建领域知识 skill、新建上下文 skill、生成业务上下文、create business skill、create domain skill、context skill。适用于：(1) 为新业务模块创建领域知识 skill；(2) 组织业务文档和开发规范；(3) 建立渐进式知识加载体系。"
---

# Business Context Skill Creator

本 skill 提供创建业务上下文 skill 的完整指南。业务上下文 skill 是一种特殊类型的 skill，用于为 AI 助手提供特定业务模块的领域知识、开发规范和最佳实践。

## 业务上下文 Skill 的价值

业务上下文 skill 与通用 skill 的核心区别：

| 特性 | 通用 Skill | 业务上下文 Skill |
|------|-----------|-----------------|
| 目标 | 执行特定任务（如 PDF 处理） | 提供领域知识辅助开发 |
| 内容 | 脚本、工具、模板 | 业务逻辑、架构文档、开发规范 |
| 触发 | 明确的任务指令 | 业务相关问题或开发任务 |
| 加载 | 一次性加载全部 | 渐进式按需加载 |

## 创建流程

创建业务上下文 skill 分为以下步骤：

1. **分析业务模块** - 理解业务边界、核心概念、关键词
2. **设计知识结构** - 规划 context 和 references 目录
3. **初始化 skill** - 运行脚本创建目录结构
4. **编写核心文档** - 完成 SKILL.md、_index.md 等
5. **填充业务知识** - 编写各领域的详细文档
6. **验证和迭代** - 测试触发和知识加载

### Step 1: 分析业务模块

在开始前，与用户确认以下信息：

**必要信息**：
- 模块名称（如 agent、history）
- 代码目录路径（如 `agent/aiusage/`）
- 核心业务领域（如 计费、权益、限流）
- 触发关键词（用户可能使用的词汇）

**可选信息**：
- 相关的外部服务依赖
- 已有的内部文档位置
- 团队开发规范

### Step 2: 设计知识结构

业务上下文 skill 推荐采用以下目录结构：

```
{skill-name}/
├── SKILL.md                    # 入口文档
├── LICENSE.txt                 # 许可证
├── context/                    # 业务知识（核心）
│   ├── _index.md              # 业务知识索引（必需）
│   ├── {domain-1}/            # 业务领域 1
│   │   ├── overview.md
│   │   └── {topic}.md
│   ├── {domain-2}/            # 业务领域 2
│   │   └── ...
│   └── ...
└── references/                 # 开发规范
    ├── development_guide.md   # 开发规范
    └── testing_guide.md       # 测试指南
```

**关键设计原则**：

1. **context/ 目录**：存放业务领域知识，按业务域而非技术分层组织
2. **_index.md**：作为知识地图，AI 首先加载此文件定位信息
3. **references/ 目录**：存放通用开发规范，与业务无关

参见 [references/structure_guide.md](references/structure_guide.md) 了解详细结构设计。

### Step 3: 初始化 Skill

运行初始化脚本创建目录结构：

```bash
python scripts/init_business_skill.py <skill-name> --path <output-dir> --domains <domain1,domain2,...>
```

示例：
```bash
python scripts/init_business_skill.py ideagent-development \
  --path .claude/skills/ \
  --domains handler,agent,context,prompt,llm
```

### Step 4: 编写核心文档

#### 4.1 SKILL.md 编写规范

SKILL.md 是 skill 的入口，必须包含：

**Frontmatter（必需）**：
```yaml
---
name: {module-name}-development
description: "Provides domain knowledge for {ModuleName} module development and analysis. Use when: (1) ...; (2) ...; (3) ..."
---
```

**Description 编写要点**：
- 说明 skill 的作用范围
- 列出 3-5 个典型使用场景
- 包含触发关键词

**Body 内容结构**：

```markdown
# {ModuleName} Development

## Overview
[简要介绍模块的核心功能和价值]

## When to Use This Skill
[分场景说明何时使用此 skill]

## How to Use
[说明 context/ 和 references/ 的使用方法]

## Quick Reference
[项目基本信息、架构分层、常用命令]

## File Organization
[目录结构说明]

## Adding New Business Knowledge
[如何扩展此 skill]
```

参见 [templates/SKILL_TEMPLATE.md](templates/SKILL_TEMPLATE.md) 获取完整模板。

#### 4.2 _index.md 编写规范

`context/_index.md` 是业务知识的索引，作为 AI 定位知识的地图：

**必要组成部分**：

1. **业务领域概览表**
```markdown
| 领域 | 目录 | 代码位置 | 说明 |
|------|------|---------|------|
| [领域名](#锚点) | `{domain}/` | `module/path/` | 简要说明 |
```

2. **每个领域的详细条目**
```markdown
## {领域名} ({domain}/)

**关键词**: 关键词1、关键词2、keyword3、keyword4

**核心概念**:
- **概念A**: 简要说明
- **概念B**: 简要说明

**详细文档**:
- `{domain}/overview.md` - 概述
- `{domain}/topic.md` - 具体主题
```

3. **使用说明**
```markdown
## 如何使用本索引
1. 快速定位...
2. 了解概念...
3. 深入学习...
```

参见 [templates/INDEX_TEMPLATE.md](templates/INDEX_TEMPLATE.md) 获取完整模板。

### Step 5: 填充业务知识

编写业务领域文档时遵循以下原则：

**只记录 AI 无法推断的知识**：
- ✅ 业务规则和约定（如"Free 用户不包括 Trial"）
- ✅ 隐含的数据流和依赖关系
- ✅ 历史决策和遗留问题
- ✅ 关键代码位置和入口点
- ❌ 可从代码直接读取的信息
- ❌ 标准库或框架的使用方法

**文档结构建议**：
```markdown
# {主题名}

[一句话说明本文档内容]

## {核心概念/数据结构}

[表格或列表形式呈现]

**代码位置**: `path/to/file.go` - `FunctionName`

## {业务逻辑/流程}

[用流程图、序列图或步骤列表说明]

## {注意事项/易错点}

[列出开发时需要注意的点]
```

参见 [references/writing_guide.md](references/writing_guide.md) 了解文档编写最佳实践。

### Step 6: 验证和迭代

1. **验证触发**：测试各种关键词是否正确触发 skill
2. **验证加载**：确认 AI 能正确定位和加载所需文档
3. **验证有效性**：在实际开发任务中测试知识的有效性

## 渐进式加载策略

业务上下文 skill 采用三级加载机制：

```
Level 1: Metadata (name + description)
         ├── 始终在上下文中
         └── 用于判断是否触发 skill

Level 2: SKILL.md body
         ├── 触发后加载
         └── 提供入口导航

Level 3: context/ 和 references/ 文件
         ├── 按需加载
         └── AI 根据用户问题选择性读取
```

**加载决策流程**：
```
用户问题 → SKILL.md → context/_index.md → 定位领域 → 加载具体文档
```

## 示例 Skills

本项目中的业务上下文 skill 示例：

| Skill | 适用模块 | 特点 |
|-------|---------|------|
| `idecopilot-development` | IDECopilot 基础服务 | 商业化、配置、LLM 接入 |
| `ideagent-development` | IDEAgent | Agent 架构、Handler、工具调用 |
| `tob-development` | ToB 企业版 | 企业模型、企业计费、部署环境 |
| `context-gateway` | 向量检索网关 | 组件管道、存储层、SDK |

可以参考这些 skill 的实现了解最佳实践。

## Quick Reference

### 必要文件清单

- [ ] `SKILL.md` - 入口文档
- [ ] `LICENSE.txt` - 许可证
- [ ] `context/_index.md` - 业务知识索引

### 可选文件

- [ ] `context/{domain}/overview.md` - 领域概述
- [ ] `context/{domain}/{topic}.md` - 具体主题
- [ ] `references/development_guide.md` - 开发规范
- [ ] `references/testing_guide.md` - 测试指南

### 文件大小建议

| 文件 | 建议行数 | 说明 |
|------|---------|------|
| SKILL.md | < 200 行 | 入口导航，不要堆积内容 |
| _index.md | < 300 行 | 索引地图，每领域 20-40 行 |
| 领域文档 | < 200 行 | 单一主题，超过则拆分 |
