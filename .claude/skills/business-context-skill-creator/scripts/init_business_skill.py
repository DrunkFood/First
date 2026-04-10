#!/usr/bin/env python3
"""
业务上下文 Skill 初始化脚本

用法:
    python init_business_skill.py <skill-name> --path <output-dir> --domains <domain1,domain2,...>

示例:
    python init_business_skill.py ideagent-development --path .claude/skills/ --domains handler,agent,context,prompt

参数:
    skill-name: Skill 名称，推荐格式为 {module-name}-development
    --path: 输出目录，默认为当前目录
    --domains: 业务领域列表，用逗号分隔
    --module-name: 模块显示名称，默认从 skill-name 推断
"""

import argparse
import os
import sys
from pathlib import Path


SKILL_MD_TEMPLATE = '''---
name: {skill_name}
description: "Provides domain knowledge for {module_name} module development and analysis. Use when: (1) Analyzing, understanding, or explaining {module_name} business logic including {domains_text}; (2) Questions about {module_name} architecture, components, and data flow; (3) Developing new features or fixing bugs in {module_name}; (4) Following development conventions and best practices."
---

# {module_name} Development

## Overview

{module_name} 是 [TODO: 简要描述模块的核心功能和价值]。本 skill 为 AI 助手提供 {module_name} 模块的领域知识。

通过本 skill，AI 助手能够：
- 理解和分析 {module_name} 各模块的业务逻辑
- 遵循项目特定的开发规范和最佳实践
- 高效地开发新功能和修复 bug

## When to Use This Skill

### 业务分析场景

当用户询问以下业务模块相关问题时：

| 业务领域 | 关键词 | 对应目录 |
|---------|--------|---------|
{domain_table}

### 开发场景

1. **开发新功能**: 直接开发 {module_name} 新功能时
2. **修复 Bug**: 修复 {module_name} 相关 bug 时
3. **理解代码库**: 理解 {module_name} 代码库结构和模式时

## How to Use

### 业务知识 (context/)

`context/` 目录包含各业务模块的领域知识，按业务域组织：

```
context/
├── _index.md                 # 业务知识索引（必读）
{domain_tree}
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

**重要**: 不要一次性加载所有文件。根据当前任务需求，只加载必要的文件。

## Quick Reference

### 项目基本信息
- **服务名称**: [TODO: 服务名称]
- **主要语言**: Go
- **框架**: [TODO: 框架信息]
- **入口点**: [TODO: 入口路径]
- **IDL 路径**: [TODO: IDL路径]

### 架构分层
1. **API Layer** (`handler/`, `cmd/`): 处理 RPC/HTTP 请求
2. **Service Layer** (`service/`): 业务逻辑和编排
3. **DAL Layer** (`dal/`, `po/`): 数据访问
4. **External Layer** (`port/`): 外部依赖抽象

### 常用命令
```bash
# TODO: 添加常用命令
make build
make test
```

## File Organization

```
.claude/skills/{skill_name}/
├── SKILL.md                    # 本文档
├── context/                    # 业务知识
│   ├── _index.md              # 业务知识索引
{file_org_domains}
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
- **{module_name} 目录**: [TODO: 模块目录路径]
- **项目规则**: `.claude/rules/project_rules.md`
'''

INDEX_MD_TEMPLATE = '''# {module_name} 业务知识索引

本目录包含 {module_name} 各业务模块的领域知识。AI 助手应首先阅读本索引，然后根据用户问题按需加载对应领域的详细文档。

## 业务领域概览

| 领域 | 目录 | 代码位置 | 说明 |
|------|------|---------|------|
{overview_table}

---

{domain_sections}

---

## 如何使用本索引

1. **快速定位**: 根据用户问题中的关键词，找到对应的业务领域
2. **了解概念**: 阅读该领域的核心概念，建立基本理解
3. **深入学习**: 如需详细了解，加载对应的详细文档
4. **结合代码**: 参考"代码位置"列，在代码库中查找具体实现
'''

DOMAIN_SECTION_TEMPLATE = '''## {domain_title} ({domain}/)

**关键词**: [TODO: 添加关键词]

**核心概念**:
- **[概念A]**: [TODO: 说明]
- **[概念B]**: [TODO: 说明]

**详细文档**:
- `{domain}/overview.md` - {domain_title}概述
'''

OVERVIEW_MD_TEMPLATE = '''# {domain_title}

[TODO: 一句话说明此领域的核心功能]

## 概述

[TODO: 2-3 段描述此领域的业务背景、核心价值和主要功能]

## 核心概念

| 概念 | 说明 | 代码位置 |
|------|------|---------|
| [概念A] | [TODO] | [TODO: path/file.go] |
| [概念B] | [TODO] | [TODO: path/file.go] |

## 架构

```
[TODO: 用 ASCII 图或简单流程图展示架构]

组件A → 组件B → 组件C
```

## 关键入口点

| 入口 | 说明 | 代码位置 |
|------|------|---------|
| [Handler] | [TODO] | [TODO: path/handler.go] |
| [Service] | [TODO] | [TODO: path/service.go] |

## 注意事项

- [TODO: 注意点1]
- [TODO: 注意点2]
'''

DEVELOPMENT_GUIDE_TEMPLATE = '''# {module_name} 开发规范与模式

本文档介绍 {module_name} 的核心开发规范和常见实现模式。

## 1. 开发工作流

### 1.1 标准流程

```
IDL 定义 → 代码生成 → 实现 Handler/Service/DAL → 编写测试 → 构建运行
```

### 1.2 IDL 定义与代码生成

**IDL 位置**: [TODO: api/idl/path/]

```bash
# 生成代码
make gen_idl
```

## 2. 命名规范

### 2.1 核心命名约定

| 类型 | 规范 | 示例 |
|------|------|------|
| 模块/包名 | 小写简洁 | `handler`, `service`, `dal` |
| Service 接口 | `[Module]Service` | `ChatService` |
| DAO 接口 | `[Entity]DAO` | `MessageDAO` |
| PO | `[Entity]PO` | `MessagePO` |

## 3. 分层架构实现

### 3.1 Handler 层

**职责**: 请求验证、调用 Service、错误映射

### 3.2 Service 层

**职责**: 业务逻辑、编排 DAO、调用外部服务

### 3.3 DAO 层

**职责**: 数据库访问、CRUD 操作

## 4. 错误处理

[TODO: 添加错误处理规范]

## 5. 日志规范

[TODO: 添加日志规范]

## 6. 测试

[TODO: 添加测试规范]
'''

TESTING_GUIDE_TEMPLATE = '''# {module_name} 测试指南

## 1. 测试策略

### 1.1 测试金字塔

```
        /\\
       /  \\      E2E Tests (少量)
      /────\\
     /      \\    Integration Tests (适量)
    /────────\\
   /          \\  Unit Tests (大量)
  /────────────\\
```

## 2. 单元测试

### 2.1 测试框架

使用 `testify` 框架:
```go
import (
    "testing"
    "github.com/stretchr/testify/assert"
    "github.com/stretchr/testify/mock"
)
```

### 2.2 Mock 工具

[TODO: 添加 Mock 工具说明]

## 3. 集成测试

[TODO: 添加集成测试说明]

## 4. 运行测试

```bash
# 运行所有测试
go test ./...

# 运行单元测试
go test ./... -short

# 生成覆盖率
go test ./... -coverprofile=coverage.out
```
'''

LICENSE_TEMPLATE = '''MIT License

Copyright (c) 2024

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
'''


def to_title(s: str) -> str:
    """Convert snake_case or kebab-case to Title Case"""
    return ' '.join(word.capitalize() for word in s.replace('-', '_').split('_'))


def create_skill(skill_name: str, output_path: str, domains: list, module_name: str = None):
    """Create a new business context skill"""
    
    if module_name is None:
        module_name = to_title(skill_name.replace('-development', ''))
    
    skill_dir = Path(output_path) / skill_name
    context_dir = skill_dir / 'context'
    references_dir = skill_dir / 'references'
    
    print(f"Creating skill: {skill_name}")
    print(f"Output directory: {skill_dir}")
    print(f"Domains: {', '.join(domains)}")
    
    skill_dir.mkdir(parents=True, exist_ok=True)
    context_dir.mkdir(exist_ok=True)
    references_dir.mkdir(exist_ok=True)
    
    for domain in domains:
        domain_dir = context_dir / domain
        domain_dir.mkdir(exist_ok=True)
        
        overview_content = OVERVIEW_MD_TEMPLATE.format(
            domain_title=to_title(domain)
        )
        (domain_dir / 'overview.md').write_text(overview_content)
    
    domains_text = ', '.join(domains)
    domain_table = '\n'.join([
        f'| **{to_title(d)}** | [TODO: 关键词] | `[TODO: path]/{d}/` |'
        for d in domains
    ])
    domain_tree = '\n'.join([
        f'├── {d}/                # {to_title(d)}\n│   └── overview.md'
        for d in domains
    ])
    file_org_domains = '\n'.join([
        f'│   ├── {d}/                 # {to_title(d)}'
        for d in domains
    ])
    
    skill_md_content = SKILL_MD_TEMPLATE.format(
        skill_name=skill_name,
        module_name=module_name,
        domains_text=domains_text,
        domain_table=domain_table,
        domain_tree=domain_tree,
        file_org_domains=file_org_domains
    )
    (skill_dir / 'SKILL.md').write_text(skill_md_content)
    
    overview_table = '\n'.join([
        f'| [{to_title(d)}](#{d}) | `{d}/` | [TODO: 代码位置] | [TODO: 说明] |'
        for d in domains
    ])
    domain_sections = '\n\n'.join([
        DOMAIN_SECTION_TEMPLATE.format(
            domain=d,
            domain_title=to_title(d)
        )
        for d in domains
    ])
    
    index_md_content = INDEX_MD_TEMPLATE.format(
        module_name=module_name,
        overview_table=overview_table,
        domain_sections=domain_sections
    )
    (context_dir / '_index.md').write_text(index_md_content)
    
    dev_guide_content = DEVELOPMENT_GUIDE_TEMPLATE.format(module_name=module_name)
    (references_dir / 'development_guide.md').write_text(dev_guide_content)
    
    testing_guide_content = TESTING_GUIDE_TEMPLATE.format(module_name=module_name)
    (references_dir / 'testing_guide.md').write_text(testing_guide_content)
    
    (skill_dir / 'LICENSE.txt').write_text(LICENSE_TEMPLATE)
    
    print(f"\n✅ Skill created successfully!")
    print(f"\nCreated files:")
    print(f"  {skill_dir}/SKILL.md")
    print(f"  {skill_dir}/LICENSE.txt")
    print(f"  {skill_dir}/context/_index.md")
    for d in domains:
        print(f"  {skill_dir}/context/{d}/overview.md")
    print(f"  {skill_dir}/references/development_guide.md")
    print(f"  {skill_dir}/references/testing_guide.md")
    
    print(f"\n📝 Next steps:")
    print(f"  1. Edit SKILL.md to customize the description and triggers")
    print(f"  2. Update context/_index.md with domain keywords and concepts")
    print(f"  3. Fill in the domain overview files with business knowledge")
    print(f"  4. Add development and testing guides specific to your module")


def main():
    parser = argparse.ArgumentParser(
        description='Initialize a new business context skill',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
Examples:
  %(prog)s ideagent-development --path .claude/skills/ --domains handler,agent,context
  %(prog)s mymodule-development --domains billing,quota --module-name "MyModule"
        '''
    )
    parser.add_argument('skill_name', help='Name of the skill (e.g., ideagent-development)')
    parser.add_argument('--path', default='.', help='Output directory (default: current directory)')
    parser.add_argument('--domains', required=True, help='Comma-separated list of business domains')
    parser.add_argument('--module-name', help='Display name for the module (default: derived from skill name)')
    
    args = parser.parse_args()
    
    domains = [d.strip() for d in args.domains.split(',') if d.strip()]
    if not domains:
        print("Error: At least one domain is required", file=sys.stderr)
        sys.exit(1)
    
    create_skill(
        skill_name=args.skill_name,
        output_path=args.path,
        domains=domains,
        module_name=args.module_name
    )


if __name__ == '__main__':
    main()
