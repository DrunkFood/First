---
name: {module-name}-development
description: "Provides domain knowledge for {ModuleName} module development and analysis. Use when: (1) Analyzing, understanding, or explaining {ModuleName} business logic including {domain1}, {domain2}, {domain3}; (2) Questions about {topic1}, {topic2}, {topic3}; (3) Developing new features or fixing bugs in {ModuleName}; (4) Following development conventions and best practices."
---

# {ModuleName} Development

## Overview

{ModuleName} 是 {简要描述模块的核心功能和价值}。本 skill 为 AI 助手提供 {ModuleName} 模块的领域知识。

通过本 skill，AI 助手能够：
- 理解和分析 {ModuleName} 各模块的业务逻辑
- 遵循项目特定的开发规范和最佳实践
- 高效地开发新功能和修复 bug

## When to Use This Skill

### 业务分析场景

当用户询问以下业务模块相关问题时：

| 业务领域 | 关键词 | 对应目录 |
|---------|--------|---------|
| **{领域1}** | 关键词1、关键词2、keyword3 | `{module}/{path1}/` |
| **{领域2}** | 关键词1、关键词2、keyword3 | `{module}/{path2}/` |
| **{领域3}** | 关键词1、关键词2、keyword3 | `{module}/{path3}/` |

### 开发场景

1. **开发新功能**: 直接开发 {ModuleName} 新功能时
2. **修复 Bug**: 修复 {ModuleName} 相关 bug 时
3. **理解代码库**: 理解 {ModuleName} 代码库结构和模式时

## How to Use

### 业务知识 (context/)

`context/` 目录包含各业务模块的领域知识，按业务域组织：

```
context/
├── _index.md                 # 业务知识索引（必读）
├── {domain1}/                # {领域1}
│   ├── overview.md          # 概述
│   └── {topic}.md           # 具体主题
├── {domain2}/                # {领域2}
│   └── ...
└── ...                       # 其他业务领域
```

**加载策略**：
1. 首先加载 `context/_index.md` 了解业务知识全貌
2. 根据用户问题，按需加载对应业务领域的文件

### 开发规范 (references/)

`references/` 目录包含开发规范和最佳实践：

| 文件 | 用途 | 何时加载 |
|------|------|---------|
| [development_guide.md](references/development_guide.md) | 开发规范与模式 | 编写代码、遵循规范时 |
| [testing_guide.md](references/testing_guide.md) | 测试策略和工具 | 编写测试代码时 |

### 渐进式加载策略

```
用户问题分析
│
├─ 业务分析类问题?
│  ├─> 加载 context/_index.md
│  └─> 根据具体领域加载对应文件
│
├─ 开发/编码类问题?
│  └─> 加载 references/development_guide.md
│
└─ 测试相关问题?
   └─> 加载 references/testing_guide.md
```

**重要**: 不要一次性加载所有文件。根据当前任务需求，只加载必要的文件，以避免上下文膨胀。

## Quick Reference

### 项目基本信息
- **服务名称**: {ServiceName} ({psm})
- **主要语言**: Go
- **框架**: {框架信息}
- **入口点**: `{入口路径}`
- **IDL 路径**: `{IDL路径}`

### 架构分层
1. **API Layer** (`handler/`, `cmd/`): 处理 RPC/HTTP 请求
2. **Service Layer** (`service/`): 业务逻辑和编排
3. **DAL Layer** (`dal/`, `po/`): 数据访问
4. **External Layer** (`port/`): 外部依赖抽象

### 常用命令
```bash
{常用命令1}
{常用命令2}
{常用命令3}
```

## File Organization

```
.claude/skills/{skill-name}/
├── SKILL.md                    # 本文档
├── context/                    # 业务知识
│   ├── _index.md              # 业务知识索引
│   ├── {domain1}/             # {领域1}
│   ├── {domain2}/             # {领域2}
│   └── ...                    # 其他领域（按需扩展）
├── references/                 # 开发规范
│   ├── development_guide.md   # 开发规范与模式
│   └── testing_guide.md       # 测试策略
└── LICENSE.txt                 # 许可证文件
```

## Adding New Business Knowledge

向 `context/` 添加新业务知识时，遵循以下规范：

1. **按业务领域组织**: 每个业务领域一个子目录
2. **提供 overview.md**: 每个领域目录应包含概述文件
3. **更新索引**: 在 `_index.md` 中添加新领域的入口
4. **保持简洁**: 只记录 AI 无法从代码中直接推断的业务规则

## Related Resources
- **{ModuleName} 目录**: `{module}/`
- **项目规则**: `.claude/rules/project_rules.md`
