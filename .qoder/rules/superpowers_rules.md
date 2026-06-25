---
description: "使用 Superpowers Skills 时，根据任务复杂度自动选择superpowers不同层级的流程"
---

# **Superpowers 分层工作法**

Superpowers是一个完整的AI编码代理软件开发工作流，让AI从"会写代码"升级为"懂软件工程"，强制执行测试驱动开发、需求澄清、任务拆解、代码审查等完整开发流程。
但不是所有任务都要求走完整工程化流程，而是根据任务复杂度自动选择不同层级的流程。

## **分层判断标准**

判断顺序固定为

1. 风险
2. 协作面
3. 回滚成本
4. 持续时间
5. 改动范围

不要只按代码行数判断任务大小

## **小项目流程**

满足以下大部分条件时 走小项目流程

- 单人可完成
- 改动集中在 1 到 3 个文件
- 半天到 1 天内可完成
- 风险低
- 回滚成本低

**默认调用**

- brainstorming
- systematic-debugging
- verification-before-completion

**默认不调用**

- writing-plans
- using-git-worktrees
- subagent-driven-development
- test-driven-development
- requesting-code-review

**执行要求**

- 先澄清目标和边界
- 直接执行最小改动
- 完成前必须验证

## **中项目流程**

满足以下大部分条件时 走中项目流程

- 涉及多个模块
- 需要 2 到 5 天推进
- 需要阶段计划
- 有一定回归风险
- 需要和同事或产品对齐

**默认调用**

- brainstorming
- writing-plans
- executing-plans
- requesting-code-review
- verification-before-completion

**按需调用**

- systematic-debugging
- test-driven-development

**执行要求**

- 必须先写可执行 plan
- 必须分阶段推进
- 每一阶段结束后要做复核

## **大项目流程**

满足以下大部分条件时 走大项目流程

- 跨模块
- 跨团队
- 周期长
- 风险高
- 回滚成本高
- 需要并行推进

**默认调用**

- brainstorming
- using-git-worktrees
- writing-plans
- subagent-driven-development
- test-driven-development
- requesting-code-review
- finishing-a-development-branch
- verification-before-completion

**执行要求**

- 不允许跳过 plan
- 不允许跳过验证
- 不允许省略 review
- 并行推进时必须明确边界

## **流程升级规则**

如果任务执行过程中出现以下任一情况 要主动升级流程层级

- 改动范围扩大
- 风险明显升高
- 需要额外协作
- 需要隔离工作区
- 需要测试闭环

## **任务完成条件**

任务只有在以下条件满足时 才能视为完成

- 目标已实现
- 没有突破任务边界
- 已给出验证证据
- 风险和未完成项已说明

如果没有验证证据 不能直接宣称完成
