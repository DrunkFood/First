# 对话上下文：WYSIWYG 编辑器迁移 (Cherry Markdown → TipTap) — 已解决

## 📋 问题背景

**项目**：招标文件AI编制工具 (`ele-ai-tender-frontend`，Vue 3 + TypeScript + Vite)

**涉及页面**：
- `/requirement/generate/:id` — 业务需求生成页（RequirementGenerate.vue）
- 项目编制流程需求阶段（PhaseRequirement.vue）
- 需求编辑页（RequirementEditor.vue）

**原始编辑器链路**：`md-editor-v3`（源码编辑）→ `cherry-markdown`（以为支持WYSIWYG，实际不支持）→ `@tiptap`（真正的WYSIWYG）

## 🔴 核心问题

1. **Cherry Markdown 不支持 WYSIWYG**：经源码分析确认，Cherry Markdown 的 `editOnly` 模式显示 Markdown 源码（CodeMirror 6 编辑器），`edit&preview` 仍需编辑源码，`previewOnly` 不可编辑。源码中搜索 `wysiwyg`/`WYSIWYG` 无任何匹配。
2. **架构本质**：Cherry Markdown 是 `Markdown源码 → Engine → HTML预览` 架构，不存在"直接在渲染结果上编辑"的能力。
3. **用户反馈**："还是可以看到Markdown的格式展示"，确认 WYSIWYG 需求未满足。

## 🎯 实现目标

- 需求未完成时：**真正的所见即所得编辑**，直接在渲染结果上修改（类似 Word 编辑体验）
- 需求已完成时（`status === 'COMPLETED'`）：纯预览只读模式
- 统一编辑器组件，3 个页面共用
- v-model 接口保持 Markdown 字符串，父组件零改动

## 🔧 技术约束

- TipTap (ProseMirror 内核) + `@tiptap/markdown` 扩展实现 Markdown 双向转换
- `@tiptap/markdown` 为 Beta 版，官方维护，API：`editor.getMarkdown()` / `setContent(md, { contentType: 'markdown' })`
- 流式更新（SSE）需 200ms 节流，避免高频 `setContent` 导致渲染卡顿
- 暗黑/浅色主题通过 CSS 变量响应 `[data-theme]` 属性切换（`_tokens.scss` 定义）
- `isInternalChange` 标志位 + `Promise.resolve().then()` 防循环

## 🚫 已尝试的方案

| 方案 | 结果 | 原因 |
|------|------|------|
| `md-editor-v3` editMode + v-show | 放弃 | 仍需编辑 Markdown 源码 |
| `md-editor-v3` 双栏模式 | 放弃 | 本质仍编辑源码，用户体验差 |
| WangEditor 富文本 | 放弃 | 输出 HTML 需额外转 Markdown |
| **Cherry Markdown WYSIWYG** | **放弃** | **Cherry Markdown 不支持 WYSIWYG，editOnly=源码编辑** |

## ✅ 最终方案：TipTap WYSIWYG 编辑器

**核心架构**：
```
SSE/外部 → Markdown字符串(v-model) → @tiptap/markdown解析 → ProseMirror文档 → WYSIWYG渲染
用户编辑 → ProseMirror变更 → @tiptap/markdown序列化 → Markdown字符串(v-model) → 父组件
```

**核心机制**：
1. `useEditor()` + StarterKit + Markdown + Table + Placeholder 扩展
2. `contentType: 'markdown'` 初始化，`editor.getMarkdown()` 序列化输出
3. `readonly=false` → `editable: true` + 工具栏显示
4. `readonly=true` → `editor.setEditable(false)` + 工具栏隐藏 + `is-readonly` CSS 类
5. `isInternalChange` + `Promise.resolve().then()` 防止 `setContent → onUpdate → modelValue → setContent` 循环
6. 200ms `scheduleFlush` 节流 SSE 流式更新
7. 主题通过 CSS 变量自动响应 `[data-theme="light"]` / `:root`（深色默认），无需操作编辑器实例

**依赖包**：

| 包 | 版本 | 用途 |
|----|------|------|
| `@tiptap/vue-3` | 3.22.4 | Vue 3 集成 (`useEditor`, `EditorContent`) |
| `@tiptap/pm` | — | ProseMirror 依赖 |
| `@tiptap/starter-kit` | 3.22.4 | 常用扩展集 |
| `@tiptap/markdown` | 3.22.4 | Markdown 双向转换（Beta，官方维护） |
| `@tiptap/extension-table` | 3.22.4 | 表格（含 TableRow/TableCell/TableHeader） |
| `@tiptap/extension-placeholder` | 3.22.4 | 空内容占位提示 |

## 📝 关键代码变更

### 新增文件
- `src/components/editor/WysiwygEditor.vue` — TipTap WYSIWYG 编辑器组件

### 修改文件

| 文件 | 变更 |
|------|------|
| `RequirementGenerate.vue` | import CherryMarkdownEditor → WysiwygEditor；模板标签替换 |
| `RequirementEditor.vue` | import CherryMarkdownEditor → WysiwygEditor；模板标签替换 |
| `PhaseRequirement.vue` | import CherryMarkdownEditor → WysiwygEditor；模板标签替换 |
| `package.json` | 添加 6 个 TipTap 依赖 |

### 保留但不再使用
- `src/components/editor/CherryMarkdownEditor.vue` — 暂不删除，降级参考
- `src/components/editor/MarkdownEditor.vue` — md-editor-v3 封装，保留
- `cherry-markdown` 依赖未删除，确认无其他使用后可清理
- `md-editor-v3` 依赖未删除

## 🎯 当前进度

| 项目 | 状态 |
|------|------|
| WysiwygEditor 组件开发 | ✅ 已完成 |
| 3 个页面迁移 | ✅ 已完成 |
| isInternalChange 防循环机制 | ✅ 已完成 |
| SSE 流式更新节流 | ✅ 已完成 |
| 类型检查 (vue-tsc --noEmit) | ✅ 通过 |
| 构建验证 (npm run build) | ✅ 通过 |
| WYSIWYG 渲染效果 | ✅ 浏览器验证通过 |
| 编辑功能 | ✅ contentEditable=true，可输入 |
| 只读模式 | ✅ contentEditable=false，工具栏隐藏 |
| 暗黑/浅色主题 | ✅ CSS 变量自动响应 |
| 工具栏按钮功能 | ✅ 已渲染（加粗/斜体/H1-H3/列表/引用/表格/撤销重做） |

## 💡 使用方法

```vue
<!-- 可编辑模式 -->
<WysiwygEditor v-model="content" />

<!-- 只读预览模式 -->
<WysiwygEditor v-model="content" :readonly="true" />
```

**注意事项**：
- v-model 类型为 `string`（Markdown 字符串），与 CherryMarkdownEditor 完全兼容
- TipTap `@tiptap/markdown` 的 `Markdown.configure()` 不接受 `html`/`tightLists` 选项（TypeScript 报错），直接用 `Markdown` 无配置
- `Table` 扩展的 `TableRow/TableCell/TableHeader` 从 `@tiptap/extension-table` 统一导出，不需要单独安装 `@tiptap/extension-table-row` 等
- 流式更新自动节流（200ms），无需外部处理
- `setContent` 后 `isInternalChange` 通过 `Promise.resolve().then()` 在微任务重置，比 Cherry 的在 afterChange 回调内重置更可靠
- 主题不需要操作编辑器实例，完全依赖 CSS 变量 + `[data-theme]` 属性

## 🐛 已知问题和待解决

1. **Cherry Markdown 残留**：`cherry-markdown` 依赖和 `CherryMarkdownEditor.vue` 仍保留，确认无其他使用后可清理
2. **md-editor-v3 残留**：依赖和 `MarkdownEditor.vue` 仍保留，确认无其他页面使用后可移除
3. **AiAssistantSidebar 大 chunk**：构建产物 `AiAssistantSidebar-*.js` 达 473KB（gzip 150KB），可能需要代码分割
4. **@tiptap/markdown Beta 状态**：官方标记为 Beta，API 可能变化，需关注版本更新
5. **工具栏按钮功能未逐个验证**：浏览器中确认了渲染，但未逐个点击测试每个工具栏按钮

## 🚀 下一步计划

1. **清理残留依赖**：确认 `cherry-markdown` 和 `md-editor-v3` 无其他使用后移除
2. **工具栏完整测试**：逐个验证加粗/斜体/标题/列表/表格等工具栏按钮功能
3. **SSE 流式生成验证**：触发 AI 生成，验证流式更新时编辑器的渲染效果
4. **性能优化**：AiAssistantSidebar 代码分割

## 📝 备注

- Cherry Markdown 源码分析结论：`editOnly` 模式下 Previewer.editOnly() 方法隐藏预览区并扩展编辑器至 100%，编辑器始终显示 Markdown 源码（CodeMirror 6），不存在 WYSIWYG 配置项
- TipTap 是 headless 编辑器，所有 UI（工具栏、样式）需要自行构建
- 项目使用 `_tokens.scss` 定义 CSS 变量体系，深色模式为默认值，浅色模式通过 `[data-theme="light"]` 覆盖
- TipTap 官方文档：https://tiptap.dev/docs/editor/getting-started/install/vue3
- @tiptap/markdown 文档：https://tiptap.dev/docs/editor/markdown
