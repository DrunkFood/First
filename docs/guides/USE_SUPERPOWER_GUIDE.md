## Superpowers 技能 · 按需触发工作流

---

## 设计目标

本配置用于指导 **Codex CLI Agent** 在使用 **Superpowers** 时采用 **按需加载（On-Demand）** 的方式，而非默认 bootstrap 全量加载。

核心目标：

* 避免技能叠加导致的人格/流程混乱
* 将技能视为“临时工作模式（Workflow Mode）”，而非永久能力
* 由 **用户明确控制** 是否、何时加载技能
* 在复杂工程场景中保持 Agent 行为稳定、可预测、可审计

---

## 核心原则

### 1 按需加载（On-Demand Only）

* 会话启动时 **不加载任何 Superpowers 技能**
* 仅在检测到明确任务场景时，**提出技能建议**
* 未经用户确认，**禁止加载任何技能**

### 2 技能 ≠ 人格

* 技能是 **阶段性工作流**，不是永久人格增强
* 技能只在其适用阶段内生效
* 阶段结束后，该技能视为“已失效”

### 3 用户主权

* 用户可拒绝加载任意技能
* 拒绝后，Agent 需继续正常工作
* 不得因“效率”或“最佳实践”强制使用技能

---

## 技能作用域与生命周期（非常重要）

### 技能作用域规则

* 每个技能仅在其对应任务阶段内生效
* 新技能加载后，如存在冲突，应以后加载的技能为主
* 不得同时让多个冲突技能主导决策

### 常见冲突示例

* `brainstorming` ↔ `executing-plans`
* `systematic-debugging` ↔ `subagent-driven-development`
* `writing-plans` 完成后，不应继续以探索性思维修改目标

---

## 技能前置条件（Preconditions）

加载技能前，Agent 必须检查是否满足前置条件：

* **superpowers:brainstorming**

    * 前置条件：需求尚不清晰 / 目标存在多个可能方案

* **superpowers:writing-plans**

    * 前置条件：已有明确目标，需要拆解步骤

* **superpowers:test-driven-development**

    * 前置条件：功能目标已确认（允许用户明确跳过规划）

* **superpowers:executing-plans**

    * 前置条件：存在已确认的执行计划

* **superpowers:verification-before-completion**

    * 前置条件：已有实现或修复结果可供验证

---

## 技能映射表

### 设计与规划类

| 触发词/场景       | 技能            | 命令                                                      |
|--------------|---------------|---------------------------------------------------------|
| 设计、头脑风暴、探索需求 | brainstorming | `superpowers-codex use-skill superpowers:brainstorming` |
| 制定计划、拆分任务    | writing-plans | `superpowers-codex use-skill superpowers:writing-plans` |

### 执行与开发类

| 触发词/场景     | 技能                          | 命令                                                                    |
|------------|-----------------------------|-----------------------------------------------------------------------|
| TDD、先写测试   | test-driven-development     | `superpowers-codex use-skill superpowers:test-driven-development`     |
| 批量执行、按计划实施 | executing-plans             | `superpowers-codex use-skill superpowers:executing-plans`             |
| 子代理、并行开发   | subagent-driven-development | `superpowers-codex use-skill superpowers:subagent-driven-development` |

### 调试与验证类

| 触发词/场景    | 技能                             | 命令                                                                       |
|-----------|--------------------------------|--------------------------------------------------------------------------|
| 调试、排查问题   | systematic-debugging           | `superpowers-codex use-skill superpowers:systematic-debugging`           |
| 验证修复、确认完成 | verification-before-completion | `superpowers-codex use-skill superpowers:verification-before-completion` |

### 协作与 Git 工作流

| 触发词/场景       | 技能                             | 命令                                                                       |
|--------------|--------------------------------|--------------------------------------------------------------------------|
| 代码审查         | requesting-code-review         | `superpowers-codex use-skill superpowers:requesting-code-review`         |
| 处理审查反馈       | receiving-code-review          | `superpowers-codex use-skill superpowers:receiving-code-review`          |
| Git worktree | using-git-worktrees            | `superpowers-codex use-skill superpowers:using-git-worktrees`            |
| 合并分支、完成开发    | finishing-a-development-branch | `superpowers-codex use-skill superpowers:finishing-a-development-branch` |

---

## 技能建议标准话术（Agent 必须遵循）

当检测到可用技能时，Agent 必须使用以下结构：

1. 识别当前任务场景
2. 说明为什么推荐该技能
3. 明确是否需要用户确认
4. 明确“不加载也可以继续”

**示例：**

> 我检测到这是一个【Bug 排查】场景。
> 建议使用 `systematic-debugging` 技能，以结构化方式定位问题根因。
> 是否加载该技能？
> （如果你不想加载，我们也可以直接继续分析。）

---

## Agent 行为规则

## 技能加载命令输出规则（Windows · 必须遵守）

当 Agent **建议加载任何 superpowers 官方技能**时，必须 **同时输出一条可直接复制执行的 PowerShell 命令**，用于由用户手动确认并执行。

规则：

* 必须使用已加入 PATH 的命令：`superpowers-codex`
* 禁止输出 `~/.codex/...` 等 Unix 或绝对路径
* 技能名必须使用完整前缀：`superpowers:<skill-name>`
* 命令必须放在 **单独的代码块** 中，格式如下：

```powershell
superpowers-codex use-skill superpowers:<skill-name>
```

如果发现用户请求的技能 **不在当前可用列表** 中，必须改为输出技能查询命令：

```powershell
superpowers-codex find-skills
```

该规则用于：

* 避免用户手误输入命令
* 避免查找路径或文档造成的时间浪费
* 保证技能加载过程可控但高效

---

## Agent 行为规则

### 必须执行

1. 分析用户输入，识别任务类型
2. 在合适时机 **主动建议** 技能
3. 加载前必须获得用户确认（除非用户明确要求）
4. 使用 `use-skill` 命令加载技能
5. 遵循技能中定义的 workflow

### 严格禁止

* 会话启动时自动 bootstrap
* 未经确认加载技能
* 同时使用多个冲突技能
* 因“最佳实践”强制技能
* 重复加载同一技能

---

## 技能管理命令

```bash
# 查看所有技能
superpowers-codex find-skills

# 加载指定技能
superpowers-codex use-skill superpowers:<skill-name>

# 一次性加载全部（极不推荐）
superpowers-codex bootstrap
```

---

## 最佳实践工作流

* 新功能开发：
  `brainstorming → writing-plans → test-driven-development → executing-plans → verification-before-completion`

* Bug 修复：
  `systematic-debugging → verification-before-completion`

* 重构：
  `systematic-debugging → test-driven-development`

---

> 本 AGENTS.md 旨在最大化 Codex + Superpowers 在真实工程中的稳定性与可控性。

