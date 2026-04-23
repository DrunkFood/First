# 对话上下文：Cherry Markdown WYSIWYG 编辑器迁移 — 待确认

## 📋 问题背景

**项目**：招标文件AI编制工具 (`ele-ai-tender-frontend`，Vue 3 + TypeScript + Vite)

**涉及页面**：
- `/requirement/generate/:id` — 业务需求生成页（RequirementGenerate.vue）
- 项目编制流程需求阶段（PhaseRequirement.vue）
- 需求编辑页（RequirementEditor.vue）

**原始编辑器**：`md-editor-v3`，编辑模式显示 Markdown 源码，预览模式只读渲染

## 🔴 核心问题

1. **需求内容不可编辑**：`editMode` 默认 `false`（预览模式），用户无法编辑内容
2. **无只读控制**：MarkdownEditor 没有 `disabled` prop，不根据需求完成状态控制可编辑性
3. **编辑体验差**：编辑模式显示 Markdown 源码（反人类），预览模式好看但不可编辑

**业务规则**：只有跳过检测或完成检测后，需求状态为 `COMPLETED` 时才不可编辑

## 🎯 实现目标

- 需求未完成时：所见即所得编辑，直接在渲染结果上修改
- 需求已完成时（`status === 'COMPLETED'`）：纯预览只读模式
- 统一编辑器组件，3 个页面共用

## 🔧 技术约束

- Cherry Markdown v0.11.0，WYSIWYG 模式 + previewOnly 模式
- `afterChange` 回调签名 `(text: string, html: string) => void`
- `switchModel(model: 'editOnly' | 'previewOnly' | 'edit&preview')` 动态切换
- `setMarkdown(content, keepCursor?)` 设置内容
- `setTheme(theme)` 切换主题（'dark'/'light'）
- `destroy()` 销毁实例
- 编辑器容器需唯一 ID（用 `crypto.randomUUID()`）
- 流式更新（SSE）需节流，否则 WYSIWYG 渲染卡顿

## 🚫 已尝试的方案

| 方案 | 结果 | 原因 |
|------|------|------|
| `editMode = true` + `v-show` 切换 | 放弃 | 仍需编辑 Markdown 源码 |
| md-editor-v3 双栏模式 | 放弃 | 本质仍编辑源码，用户体验差 |
| WangEditor 富文本 | 放弃 | 输出 HTML 需额外转 Markdown |

## ✅ 当前方案：Cherry Markdown WYSIWYG

**核心机制**：
1. `readonly=false` → `editOnly` WYSIWYG 模式，直接编辑渲染结果
2. `readonly=true` → `previewOnly` 纯预览模式
3. `isInternalChange` 标志位在 `afterChange` 回调内重置（非 `nextTick`），防止 `setMarkdown → afterChange → modelValue → setMarkdown` 无限循环
4. 流式更新 200ms 节流（`scheduleFlush`），避免高频 `setMarkdown` 导致渲染卡顿
5. 主题跟随 `useThemeStore().mode` 动态切换

## 📝 关键代码变更

### 新增文件
- `src/components/editor/CherryMarkdownEditor.vue` — Cherry Markdown 封装组件

### 修改文件
| 文件 | 变更 |
|------|------|
| `package.json` | 添加 `cherry-markdown: ^0.11.0` |
| `RequirementGenerate.vue` | MarkdownEditor+MdPreview → CherryMarkdownEditor，`readonly` 由 `isRequirementCompleted` 控制；保存/编辑按钮 `v-if="!isRequirementCompleted"`；AI助手 `v-if="!isRequirementCompleted"`；底部按钮条件分支 |
| `PhaseRequirement.vue` | MarkdownEditor+MdPreview → CherryMarkdownEditor，`:readonly="readonly"`；补充 `setActive` 解构；移除 `useThemeStore`/`excludeToolbars` |
| `RequirementEditor.vue` | MarkdownEditor → CherryMarkdownEditor |
| `AiChatPanel.vue` | `msg.uid` 空值检查（预存 bug 修复） |
| `PhaseBasicInfo.vue` | 补充缺失 `buildTreeData` 函数 + `WordChapter` 导入（预存 bug 修复） |

### 保留但不再活跃使用
- `src/components/editor/MarkdownEditor.vue` — 保留作为降级方案
- `md-editor-v3` 依赖未删除

## 🎯 当前进度

| 项目 | 状态 |
|------|------|
| CherryMarkdownEditor 组件开发 | ✅ 已完成 |
| 3 个页面迁移 | ✅ 已完成 |
| isInternalChange 防循环机制 | ✅ 已完成 |
| SSE 流式更新节流 | ✅ 已完成 |
| 类型检查 (vue-tsc --noEmit) | ✅ 通过 |
| 构建验证 (npm run build) | ✅ 通过 |
| **浏览器可视化验证** | ⏳ 待确认（浏览器 MCP 锁定未验证） |
| 暗黑主题切换验证 | ⏳ 待确认 |
| readonly 动态切换验证 | ⏳ 待确认 |
| WYSIWYG 编辑体验验证 | ⏳ 待确认 |

## 💡 使用方法

```vue
<!-- 可编辑模式 -->
<CherryMarkdownEditor v-model="content" />

<!-- 只读预览模式 -->
<CherryMarkdownEditor v-model="content" :readonly="true" />
```

**注意事项**：
- 组件使用 `crypto.randomUUID()` 生成容器 ID，需浏览器支持
- 流式更新自动节流（200ms），无需外部处理
- `afterChange` 回调内部处理了防循环，外部 `v-model` 赋值安全
- 销毁时自动调用 `cherryInstance.destroy()` 清理

## 🐛 已知问题和待解决

1. **浏览器可视化未验证**：Playwright MCP 锁定问题未解决，Cherry Markdown 实际渲染效果、WYSIWYG 编辑体验、暗黑主题切换均未在浏览器中确认
2. **setTheme 参数未确认**：`setTheme('dark')`/`setTheme('light')` 的参数是否正确取决于 Cherry v0.11.0 的默认主题配置，未经实际验证
3. **md-editor-v3 残留**：依赖和 MarkdownEditor.vue 仍保留，确认无其他使用后可清理
4. **AiAssistantSidebar 大 chunk**：构建产物 `AiAssistantSidebar-*.js` 达 5.5MB，可能需要代码分割

## 🚀 下一步计划

1. **浏览器验证**（优先级最高）：
   - 打开 `http://localhost:5173/requirement/generate/1000025` 确认 WYSIWYG 编辑体验
   - 验证只读/编辑模式切换
   - 验证暗黑主题切换
   - 验证 AI 生成后内容加载
2. **清理 md-editor-v3 依赖**：确认无其他页面使用后移除
3. **性能优化**：AiAssistantSidebar 代码分割

## 📝 备注

- Cherry Markdown 文档：https://tencent.github.io/cherry-markdown/
- Cherry Markdown GitHub：https://github.com/Tencent/cherry-markdown
- 代码审查发现了 `isInternalChange` 竞态条件和 SSE 流式性能问题，均已修复
- 修复了两个预存 bug（AiChatPanel uid 空值、PhaseBasicInfo buildTreeData 缺失）
