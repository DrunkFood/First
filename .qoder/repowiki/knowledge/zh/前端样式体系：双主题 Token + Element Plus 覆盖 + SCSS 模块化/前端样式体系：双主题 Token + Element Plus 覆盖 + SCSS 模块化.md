---
kind: frontend_style
name: 前端样式体系：双主题 Token + Element Plus 覆盖 + SCSS 模块化
slug: frontend_style
category: frontend_style
scope:
    - '**'
---

## 1. 系统与方法论

两个前端应用（编制中心 `ele-ai-tender-frontend`、支撑中心 `ele-ai-tender-support-frontend`）均采用 **Vue3 + Vite + TypeScript + Element Plus** 技术栈，样式方案以 **SCSS + CSS 自定义属性（CSS Variables）** 为核心，通过设计 Token 驱动全局主题与深色/浅色模式切换。

- 构建工具：Vite 7，使用 Sass 编译 SCSS。
- UI 组件库：Element Plus 2.x，通过 CSS 变量覆盖其默认主题。
- 状态管理：Pinia 管理主题模式，运行时切换 `data-theme` 属性。
- 编辑器：主应用额外集成 TipTap 3 与 md-editor-v3，用于需求编辑与 Markdown 预览。

## 2. 核心文件与包

| 模块 | 关键路径 | 作用 |
|------|----------|------|
| 编制中心 | `src/styles/index.scss` | 样式入口，按序导入 token → EP 覆盖 → 字体 → 间距 → 重置 → 全局工具类 |
| 编制中心 | `src/styles/variables/_tokens.scss` | 全部设计 Token（品牌色、背景、文字、阴影、圆角、过渡等），默认深色，`[data-theme="light"]` 覆盖为浅色 |
| 编制中心 | `src/styles/variables/_element-overrides.scss` | 将 `--el-*` 变量映射到 `--app-*` Token，实现 EP 主题与业务 Token 解耦 |
| 编制中心 | `src/styles/base/_reset.scss` | html/body 基础重置、滚动条、选择高亮 |
| 编制中心 | `src/styles/base/_global.scss` | 全局工具类：`.gradient-btn`、`.card-hover`、`.glass-effect`、`.status-badge*` |
| 编制中心 | `src/store/theme.ts` | Pinia store，监听 `mode` 写入 `document.documentElement.setAttribute('data-theme', ...)` 并持久化到 localStorage |
| 支撑中心 | `src/assets/styles/index.scss` | 独立 Token 体系（`--et-*` 前缀），绿色系品牌色，直接覆盖 `.el-*` 类名 |
| 两项目 | `package.json` | 依赖声明：vue3、vite、sass、element-plus、pinia、axios 等 |

## 3. 架构与约定

### 3.1 Token 分层结构（编制中心）

```
styles/
├── variables/
│   ├── _tokens.scss          # 业务 Token（--app-*）
│   ├── _element-overrides.scss # EP 变量桥接（--el-* ← --app-*）
│   ├── _typography.scss      # 字体规范
│   └── _spacing.scss         # 间距工具
├── base/
│   ├── _reset.scss           # 浏览器差异重置
│   └── _global.scss          # 全局工具类
└── index.scss                # 统一入口，固定导入顺序
```

- **Token 命名空间**：所有业务级变量以 `--app-` 前缀，避免与第三方库冲突。
- **主题切换机制**：默认 `:root` 定义深色值；`[data-theme="light"]` 覆盖同名变量，无需 JS 动态注入样式。
- **EP 主题桥接**：`_element-overrides.scss` 在 `index.scss` 中排在 `_tokens.scss` 之后，确保 `var(--app-brand-color)` 已解析后再赋值给 `--el-color-primary`。

### 3.2 支撑中心差异化风格

支撑中心未复用编制中心的 Token 体系，而是自建 `--et-*` 命名空间（`assets/styles/index.scss`），采用绿色品牌色（`#0f8a5f`）和更柔和的卡片/表格样式，并通过直接覆盖 `.el-button--primary`、`.el-table` 等类名实现视觉统一。

### 3.3 响应式策略

- 编制中心：主要依赖 CSS 变量与 Flex/Grid 布局，未在样式层发现显式断点。
- 支撑中心：在 `@media (max-width: 960px)` 下调整页面内边距、头部排列与表单布局。

## 4. 开发者应遵循的规则

1. **新增颜色/尺寸必须走 Token**  
   在 `_tokens.scss` 中以 `--app-*` 前缀声明，禁止在组件中硬编码十六进制色值或像素值。

2. **主题切换只改 `data-theme`**  
   通过 `useThemeStore.toggle()` 切换，不要在组件里直接操作 DOM 设置 style。

3. **覆盖 Element Plus 样式优先用 CSS 变量**  
   需要修改 EP 组件外观时，在 `_element-overrides.scss` 中重写 `--el-*` 变量，而非写大量 `!important` 规则。

4. **全局工具类集中维护**  
   通用效果（渐变按钮、毛玻璃、状态徽章）放入 `base/_global.scss`，组件内仅引用类名。

5. **支撑中心保持独立 Token 命名空间**  
   新增变量使用 `--et-*` 前缀，避免与编制中心 Token 混用。

6. **SCSS 导入顺序不可乱**  
   严格遵循 `index.scss` 中的 6 步顺序：token → element-overrides → typography → spacing → reset → global。
