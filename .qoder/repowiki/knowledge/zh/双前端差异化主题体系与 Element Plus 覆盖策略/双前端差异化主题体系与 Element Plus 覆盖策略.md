---
kind: frontend_style
name: 双前端差异化主题体系与 Element Plus 覆盖策略
category: frontend_style
scope:
    - '**'
source_files:
    - ele-ai-tender-frontend/src/styles/index.scss
    - ele-ai-tender-frontend/src/styles/variables/_tokens.scss
    - ele-ai-tender-frontend/src/styles/variables/_element-overrides.scss
    - ele-ai-tender-frontend/src/store/theme.ts
    - ele-ai-tender-frontend/src/components/common/ThemeToggle.vue
    - ele-ai-tender-support-frontend/src/assets/styles/index.scss
---

## 系统概览
本项目包含两个并列的 Vue3 + Vite 前端应用，采用差异化的视觉风格：
- **ele-ai-tender-frontend**（编制中心）：深色默认、品牌蓝主色、完整设计 Token 体系
- **ele-ai-tender-support-frontend**（支撑中心）：浅色绿色系、轻量 CSS 变量

两者均基于 Element Plus 组件库，通过 CSS 自定义属性实现主题切换。

## 核心架构

### 1. 设计 Token 体系（编制中心）
采用 SCSS + CSS 自定义属性的分层结构：
- `styles/variables/_tokens.scss`：定义所有设计 Token（颜色、字体、圆角、阴影等），以 `--app-*` 命名空间组织
- `styles/variables/_element-overrides.scss`：将 Token 映射到 Element Plus 的 `--el-*` 变量
- `styles/variables/_spacing.scss`：间距、圆角、阴影工具类
- `styles/base/_reset.scss`：基础重置与滚动条样式
- `styles/base/_global.scss`：全局工具类（渐变按钮、卡片悬停、毛玻璃效果）
- `styles/index.scss`：统一入口，按依赖顺序导入

### 2. 主题切换机制
- 通过 Pinia store (`store/theme.ts`) 管理主题状态，写入 `document.documentElement` 的 `data-theme` 属性
- 使用 `[data-theme="light"]` 选择器覆盖默认深色 Token 值
- 支持 localStorage 持久化，页面刷新后恢复上次主题
- 提供 `ThemeToggle.vue` 组件作为切换入口

### 3. 支撑中心轻量方案
支撑中心采用更简洁的方案：
- 直接在 `assets/styles/index.scss` 中定义 `--et-*` 前缀的 CSS 变量
- 通过局部 `@import` 按需引入全局样式
- 使用 CSS 直接覆盖 Element Plus 组件样式（如 `.el-button--primary`）
- 内置响应式断点（960px）处理移动端布局

## 关键约定

### 命名规范
- 编制中心：`--app-*` 前缀的设计 Token
- 支撑中心：`--et-*` 前缀的轻量变量
- 组件内样式使用 `<style scoped>`，避免全局污染

### Element Plus 集成
- 必须在 Element Plus 样式之后导入覆盖文件
- 优先覆盖 CSS 变量而非重写类名
- 中文语言包通过 `zhCn` 配置启用

### 响应式策略
- 编制中心：未内置响应式断点，依赖组件库自身适配
- 支撑中心：在 `index.scss` 中使用 `@media (max-width: 960px)` 处理移动端

## 开发者指南
1. 新增设计 Token 时，先在 `_tokens.scss` 中定义，再在 `_element-overrides.scss` 中映射到 Element Plus
2. 主题切换仅修改 `data-theme` 属性，不要在组件中硬编码颜色值
3. 支撑中心新增样式时，遵循现有 `--et-*` 变量命名约定
4. 全局工具类应放在对应模块文件中，通过 `index.scss` 统一导出