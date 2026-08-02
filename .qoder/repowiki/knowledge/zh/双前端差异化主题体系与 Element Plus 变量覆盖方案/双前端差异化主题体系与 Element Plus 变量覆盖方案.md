---
kind: frontend_style
name: 双前端差异化主题体系与 Element Plus 变量覆盖方案
category: frontend_style
scope:
    - '**'
source_files:
    - ele-ai-tender-frontend/src/styles/index.scss
    - ele-ai-tender-frontend/src/styles/variables/_tokens.scss
    - ele-ai-tender-frontend/src/styles/variables/_element-overrides.scss
    - ele-ai-tender-frontend/src/styles/base/_reset.scss
    - ele-ai-tender-frontend/src/styles/base/_global.scss
    - ele-ai-tender-frontend/src/store/theme.ts
    - ele-ai-tender-frontend/src/components/common/ThemeToggle.vue
    - ele-ai-tender-support-frontend/src/assets/styles/index.scss
---

## 系统概述
本项目包含两个并列的 Vue3 + Vite 前端应用，采用不同的视觉风格与样式架构：
- **ele-ai-tender-frontend**（编制中心）：深色/浅色双主题、完整设计 Token 体系、Element Plus 变量级覆盖
- **ele-ai-tender-support-frontend**（支撑中心）：浅色系独立主题、轻量 CSS 变量 + 工具类

## 核心样式架构

### 1. 设计 Token 体系（编制中心）
位于 `src/styles/variables/_tokens.scss`，通过 CSS 自定义属性定义完整设计语言：
- 品牌色阶：`--app-brand-color` 及其 light/dark 变体
- 背景层级：primary/secondary/tertiary/elevated 四级背景
- 文字层级：primary/secondary/tertiary 三级文字
- 状态色：success/warning/danger/info 及对应半透明背景
- 阴影体系：sm/md/lg 三级阴影
- 圆角：sm(8px)/md(12px)/lg(16px)
- 字体规范：PingFang SC / Microsoft YaHei 中文字体栈

### 2. 主题切换机制
基于 `data-theme` 属性的 CSS 选择器覆盖策略：
- 默认值定义在 `:root`（深色模式）
- `[data-theme="light"]` 覆盖为浅色模式
- Pinia store (`store/theme.ts`) 管理主题状态并持久化到 localStorage
- `components/common/ThemeToggle.vue` 提供切换入口

### 3. Element Plus 集成
通过 `variables/_element-overrides.scss` 将应用 Token 映射到 Element Plus CSS 变量：
- 主色、文字、边框、填充、背景全部通过 `var(--app-*)` 引用
- 确保组件库与应用主题完全同步
- 必须在 Element Plus 样式之后导入才能生效

### 4. 样式组织约定
```scss
styles/
├── index.scss          # 统一入口，按依赖顺序导入
├── variables/          # 设计 Token 与变量
│   ├── _tokens.scss    # 核心设计 Token
│   ├── _element-overrides.scss  # Element Plus 覆盖
│   ├── _spacing.scss   # 间距/圆角/阴影工具类
│   └── _typography.scss # 字体规范
└── base/              # 基础样式
    ├── _reset.scss     # 浏览器重置
    └── _global.scss    # 全局工具类（gradient-btn, glass-effect等）
```

### 5. 支撑中心独立主题
`support-frontend` 使用独立的绿色系主题：
- CSS 变量前缀 `--et-*`（如 `--et-primary`, `--et-bg`）
- 浅色系设计，无深色模式支持
- 直接在 `assets/styles/index.scss` 中定义，结构更简单
- 通过 `.el-button--primary` 等类名直接覆盖 Element Plus 样式

## 开发约定
1. **新增颜色必须定义在 `_tokens.scss`**，禁止在组件中硬编码颜色值
2. **使用 CSS 变量而非 Sass 变量**，确保运行时主题切换能力
3. **Element Plus 组件样式覆盖**优先通过 CSS 变量，其次才用类名覆盖
4. **工具类命名**遵循 BEM 风格：`.radius-md`, `.shadow-lg`, `.glass-effect`
5. **响应式断点**：支撑中心使用 `@media (max-width: 960px)` 作为移动端断点
6. **滚动条样式**：两个前端都自定义了 `::-webkit-scrollbar` 以匹配主题