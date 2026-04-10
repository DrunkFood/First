# 业务上下文 Skill 结构设计指南

本文档详细说明业务上下文 skill 的目录结构设计原则。

## 目录结构概览

```
{skill-name}/
├── SKILL.md                    # 入口文档（必需）
├── LICENSE.txt                 # 许可证（必需）
├── context/                    # 业务知识
│   ├── _index.md              # 知识索引（必需）
│   ├── {domain-1}/            # 业务领域目录
│   │   ├── overview.md        # 领域概述
│   │   ├── {topic-1}.md       # 具体主题
│   │   └── {topic-2}.md
│   └── {domain-2}/
│       └── ...
└── references/                 # 开发规范
    ├── development_guide.md   # 开发规范
    └── testing_guide.md       # 测试指南
```

## context/ 目录设计

### 组织原则

**按业务域组织，而非按技术层**：

```
✅ 正确的组织方式：
context/
├── billing/           # 计费领域
├── entitlement/       # 权益领域
└── queue/             # 排队领域

❌ 错误的组织方式：
context/
├── handler/           # Handler 层
├── service/           # Service 层
└── dal/               # DAL 层
```

### _index.md 的作用

`_index.md` 是业务知识的入口点，AI 助手在处理用户问题时：

1. 首先读取 `_index.md` 了解知识全貌
2. 根据用户问题中的关键词定位领域
3. 按需读取对应领域的详细文档

**_index.md 必须包含**：

| 元素 | 说明 | 示例 |
|------|------|------|
| 领域概览表 | 列出所有领域及其代码位置 | 见模板 |
| 关键词映射 | 每个领域的触发关键词 | 计费、billing、quota |
| 核心概念 | 领域的关键概念简介 | UserPayIdentity、Entitlement |
| 文档链接 | 指向详细文档的链接 | `billing/overview.md` |

### 领域目录命名

| 推荐 | 不推荐 | 原因 |
|------|--------|------|
| `billing/` | `billing-related/` | 简洁 |
| `tob/` | `to-b/` | 一致性 |
| `llm/` | `large-language-model/` | 常用缩写 |

### 领域文档结构

每个领域目录应包含：

```
{domain}/
├── overview.md        # 领域概述（推荐）
├── {core-topic}.md    # 核心主题
├── {sub-topic-1}.md   # 子主题
└── {sub-topic-2}.md   # 子主题
```

**overview.md** 应包含：
- 领域的业务背景
- 与其他领域的关系
- 关键入口点和代码位置
- 常见问题和解决方案

## references/ 目录设计

references/ 存放与业务无关的通用开发规范：

| 文件 | 内容 | 何时加载 |
|------|------|---------|
| `development_guide.md` | 代码规范、分层架构、错误处理 | 编写代码时 |
| `testing_guide.md` | 测试策略、Mock 工具、覆盖率 | 编写测试时 |

**注意**：
- references/ 中的内容应该是项目通用的
- 与业务相关的规范应放在 context/ 中

## 文件大小控制

为避免上下文膨胀，遵循以下限制：

| 文件类型 | 最大行数 | 超出处理方式 |
|---------|---------|-------------|
| SKILL.md | 200 行 | 拆分到 references/ |
| _index.md | 300 行 | 减少每领域描述 |
| 领域文档 | 200 行 | 拆分为多个文件 |

## 渐进式披露

设计文档时考虑三级加载：

```
Level 1: SKILL.md (始终加载)
         └── 入口导航，快速参考

Level 2: _index.md (按需加载)
         └── 知识地图，定位信息

Level 3: 详细文档 (按需加载)
         └── 具体知识，深入学习
```

## 示例：idecopilot-development

```
idecopilot-development/
├── SKILL.md
├── LICENSE.txt
├── context/
│   ├── _index.md                    # 业务知识索引
│   ├── commercialization/           # 商业化领域
│   │   ├── user_identity.md        # 用户身份
│   │   └── usage_dataflow.md       # 用量数据流
│   ├── configuration/               # 配置领域
│   │   └── model_config.md         # 模型配置
│   ├── llm/                         # LLM 接入
│   │   └── overview.md
│   └── tob/                         # 企业版
│       ├── overview.md
│       ├── tenant_model.md
│       └── tob_billing.md
└── references/
    ├── development_guide.md         # 开发规范
    └── testing_guide.md             # 测试指南
```

## 反模式

### 1. 过深的目录嵌套

```
❌ context/billing/user/payment/history/details.md
✅ context/billing/payment_history.md
```

### 2. 重复的内容

```
❌ SKILL.md 和 _index.md 中都有完整的领域介绍
✅ SKILL.md 只有入口链接，_index.md 有详细介绍
```

### 3. 过大的单一文件

```
❌ context/billing/everything.md (500+ 行)
✅ context/billing/overview.md + pricing.md + quota.md
```

### 4. 与业务无关的技术文档

```
❌ context/golang_basics.md
❌ context/gorm_tutorial.md
✅ references/development_guide.md (只包含项目特定规范)
```
