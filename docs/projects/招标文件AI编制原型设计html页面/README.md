# README.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

招标文件AI编制系统（Bidding Document AI Compilation System）—— 当前为纯前端静态HTML原型阶段，所有25个页面均为自包含的单文件，数据全部为模拟/mock，无后端服务。

## 开发方式

无构建工具、无包管理器、无框架依赖。直接用浏览器打开 `pages/` 目录下的 HTML 文件即可预览。修改任何页面只需编辑对应的 HTML 文件。

```bash
# 本地预览（任选一种）
# 方式1: 直接双击 pages/login.html 用浏览器打开
# 方式2: 使用简易HTTP服务器
npx serve pages -p 3000
python -m http.server 3000 -d pages
```

入口页面：`pages/login.html`

## 架构要点

### 单文件自包含模式

每个 HTML 页面独立包含全部 CSS（`<style>`块）和 JavaScript（`<script>`块），页面间通过相对 URL 链接导航。这意味着：

- **修改一处样式需要同步所有页面** —— 例如 CSS 自定义属性（主题色变量）在每个文件中都有副本
- **没有共享组件机制** —— 侧边栏、顶部栏等公共布局在每个页面中重复
- **所有数据交互均为 mock** —— 如 `login.html` 用 `Math.random() > 0.5` 模拟新用户判断

### 主题系统

深色模式为默认，浅色模式通过 `@media (prefers-color-scheme: light)` 覆盖。约30+个 CSS 自定义属性在 `:root` 中定义，所有页面保持一致。

### 页面布局模式

除 `login.html` 外，所有页面采用统一布局：
- 固定左侧边栏（220px）+ 顶部栏（面包屑/用户操作）+ 主内容区
- 侧边栏导航项根据用户端/管理端权限显示不同菜单

## 页面导航关系

```
login.html → policy-file-list.html（登录后默认跳转）

业务需求模块: business-requirement-list → create → generate → edit/review
项目管理模块: project-list → create → init → detail → integration/review/requirement
模板管理模块: template-list → create/edit/detail
辅助页面: statistics, system-settings, message-center, project-catalog, review-progress, review-report
```

## 业务领域

- **项目类别**：限额以下、产权交易、政府采购
- **项目类型**：工程类、货物类、服务类（物业/IT服务/咨询服务/维保服务）
- **文档状态流**：草案 → 编制中 → 待检测 → 检测中 → 检测通过/未通过 → 已发布 → 已归档
- **双权限体系**：用户端（项目管理、需求编制）+ 管理端（模板管理、知识库、统计）
- **AI模型策略**：生成类任务用本地微调模型，修改/检测类任务用公有大模型（DeepSeek）

## 计划中的技术栈（非当前实现）

当前为 HTML 原型，正式开发将迁移至：
- **前端**：Vue3 + TypeScript + Pinia + Element Plus
- **后端**：JDK 21 + Spring Boot 3.2.2 + MyBatis-Plus 3.5.5 + MySQL 8.4.0 + Redis
- **AI**：向量数据库（Milvus/Chroma）+ OpenAI兼容API + poi-tl（Word生成）
- **复用**：约60%来自现有 EleTender 平台（认证、文件服务、权限框架等）

## 修改注意事项

- 修改 CSS 变量时，务必同步所有25个页面的 `:root` 定义
- 新增页面需遵循现有布局结构和主题变量命名规范
- mock 数据逻辑集中在各页面的 `<script>` 块末尾，搜索 `模拟` 或 `mock` 可定位
- 登录后重定向目标硬编码在 `login.html` 中
