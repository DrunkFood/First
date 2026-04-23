<template>
  <div class="wysiwyg-editor" :class="{ 'is-readonly': readonly }">
    <!-- 工具栏 -->
    <div v-if="!readonly" class="editor-toolbar">
      <div class="toolbar-group">
        <button
          :class="['toolbar-btn', { active: editor?.isActive('bold') }]"
          title="加粗"
          @click="editor?.chain().focus().toggleBold().run()"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="M6 4h8a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/><path d="M6 12h9a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/></svg>
        </button>
        <button
          :class="['toolbar-btn', { active: editor?.isActive('italic') }]"
          title="斜体"
          @click="editor?.chain().focus().toggleItalic().run()"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="4" x2="10" y2="4"/><line x1="14" y1="20" x2="5" y2="20"/><line x1="15" y1="4" x2="9" y2="20"/></svg>
        </button>
        <button
          :class="['toolbar-btn', { active: editor?.isActive('strike') }]"
          title="删除线"
          @click="editor?.chain().focus().toggleStrike().run()"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4H9a3 3 0 0 0-3 3v0a3 3 0 0 0 3 3h6"/><line x1="4" y1="12" x2="20" y2="12"/><path d="M15 12a3 3 0 1 1 0 6H8"/></svg>
        </button>
      </div>

      <div class="toolbar-divider" />

      <div class="toolbar-group">
        <button
          :class="['toolbar-btn', { active: editor?.isActive('heading', { level: 1 }) }]"
          title="标题1"
          @click="editor?.chain().focus().toggleHeading({ level: 1 }).run()"
        >H1</button>
        <button
          :class="['toolbar-btn', { active: editor?.isActive('heading', { level: 2 }) }]"
          title="标题2"
          @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()"
        >H2</button>
        <button
          :class="['toolbar-btn', { active: editor?.isActive('heading', { level: 3 }) }]"
          title="标题3"
          @click="editor?.chain().focus().toggleHeading({ level: 3 }).run()"
        >H3</button>
      </div>

      <div class="toolbar-divider" />

      <div class="toolbar-group">
        <button
          :class="['toolbar-btn', { active: editor?.isActive('bulletList') }]"
          title="无序列表"
          @click="editor?.chain().focus().toggleBulletList().run()"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><circle cx="4" cy="6" r="1" fill="currentColor"/><circle cx="4" cy="12" r="1" fill="currentColor"/><circle cx="4" cy="18" r="1" fill="currentColor"/></svg>
        </button>
        <button
          :class="['toolbar-btn', { active: editor?.isActive('orderedList') }]"
          title="有序列表"
          @click="editor?.chain().focus().toggleOrderedList().run()"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="10" y1="6" x2="21" y2="6"/><line x1="10" y1="12" x2="21" y2="12"/><line x1="10" y1="18" x2="21" y2="18"/><text x="3" y="8" font-size="8" fill="currentColor" stroke="none" font-family="sans-serif">1</text><text x="3" y="14" font-size="8" fill="currentColor" stroke="none" font-family="sans-serif">2</text><text x="3" y="20" font-size="8" fill="currentColor" stroke="none" font-family="sans-serif">3</text></svg>
        </button>
      </div>

      <div class="toolbar-divider" />

      <div class="toolbar-group">
        <button
          :class="['toolbar-btn', { active: editor?.isActive('blockquote') }]"
          title="引用"
          @click="editor?.chain().focus().toggleBlockquote().run()"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V21z"/><path d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2h.75c0 2.25.25 4-2.75 4v3z"/></svg>
        </button>
        <button
          class="toolbar-btn"
          title="表格"
          @click="editor?.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/><line x1="15" y1="3" x2="15" y2="21"/></svg>
        </button>
        <button
          class="toolbar-btn"
          title="分隔线"
          @click="editor?.chain().focus().setHorizontalRule().run()"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="12" x2="21" y2="12"/></svg>
        </button>
      </div>

      <div class="toolbar-divider" />

      <div class="toolbar-group">
        <button
          class="toolbar-btn"
          title="撤销"
          :disabled="!editor?.can().undo()"
          @click="editor?.chain().focus().undo().run()"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>
        </button>
        <button
          class="toolbar-btn"
          title="重做"
          :disabled="!editor?.can().redo()"
          @click="editor?.chain().focus().redo().run()"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.13-9.36L23 10"/></svg>
        </button>
      </div>
    </div>

    <!-- 编辑器内容 -->
    <EditorContent :editor="editor" class="editor-content" />
  </div>
</template>

<script setup lang="ts">
import { watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { Markdown } from '@tiptap/markdown'
import { Table, TableRow, TableCell, TableHeader } from '@tiptap/extension-table'
import Placeholder from '@tiptap/extension-placeholder'

const modelValue = defineModel<string>({ default: '' })

const props = withDefaults(defineProps<{
  readonly?: boolean
}>(), {
  readonly: false,
})


// 防止 setContent 触发 onUpdate 导致循环
let isInternalChange = false
// SSE 流式更新节流
let pendingContent: string | null = null
let flushTimer: ReturnType<typeof setTimeout> | null = null

const editor = useEditor({
  content: modelValue.value,
  contentType: 'markdown',
  editable: !props.readonly,
  extensions: [
    StarterKit.configure({
      heading: { levels: [1, 2, 3, 4] },
    }),
    Markdown,
    Table.configure({ resizable: true }),
    TableRow,
    TableCell,
    TableHeader,
    Placeholder.configure({
      placeholder: '开始输入内容...',
    }),
  ],
  editorProps: {
    attributes: {
      class: 'wysiwyg-body',
    },
  },
  onUpdate: ({ editor: ed }) => {
    if (isInternalChange) return
    const md = ed.getMarkdown()
    modelValue.value = md
  },
})

/** 节流刷新：合并高频 content 更新 */
function scheduleFlush(newVal: string) {
  pendingContent = newVal
  if (flushTimer) return
  flushTimer = setTimeout(() => {
    flushTimer = null
    if (editor.value && pendingContent !== null) {
      const content = pendingContent
      pendingContent = null
      isInternalChange = true
      editor.value.commands.setContent(content, {
        contentType: 'markdown',
        emitUpdate: false,
      })
      // isInternalChange 在下一个微任务重置，确保 onUpdate 不会误触发
      Promise.resolve().then(() => { isInternalChange = false })
    }
  }, 200)
}

watch(modelValue, (newVal) => {
  if (!editor.value) return
  if (isInternalChange) return

  // 有待刷新内容时走节流，否则直接比对
  if (pendingContent !== null || flushTimer) {
    scheduleFlush(newVal)
    return
  }

  const current = editor.value.getMarkdown()
  if (current !== newVal) {
    isInternalChange = true
    editor.value.commands.setContent(newVal || '', {
      contentType: 'markdown',
      emitUpdate: false,
    })
    Promise.resolve().then(() => { isInternalChange = false })
  }
})

watch(() => props.readonly, (val) => {
  editor.value?.setEditable(!val)
})

// 主题：通过 CSS 类名切换，不操作编辑器实例
// 主题变量由 _tokens.scss 中的 [data-theme="light"] 控制

onBeforeUnmount(() => {
  if (flushTimer) {
    clearTimeout(flushTimer)
    flushTimer = null
  }
  editor.value?.destroy()
})
</script>

<style scoped>
.wysiwyg-editor {
  min-height: 400px;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--app-border-light);
  border-radius: var(--app-radius-sm);
  overflow: hidden;
  background: var(--app-input-bg);
  transition: var(--app-transition-base);
}

.wysiwyg-editor.is-readonly {
  border-color: transparent;
}

/* ---- 工具栏 ---- */
.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 6px 8px;
  background: var(--app-bg-tertiary);
  border-bottom: 1px solid var(--app-border-light);
  flex-shrink: 0;
  flex-wrap: wrap;
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 2px;
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: var(--app-border-medium);
  margin: 0 4px;
}

.toolbar-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 4px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--app-text-secondary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.15s ease;
}

.toolbar-btn svg {
  width: 16px;
  height: 16px;
}

.toolbar-btn:hover:not(:disabled) {
  background: var(--app-hover-state);
  color: var(--app-text-primary);
}

.toolbar-btn.active {
  background: var(--app-active-state);
  color: var(--app-brand-color);
}

.toolbar-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ---- 编辑器内容 ---- */
.editor-content {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.editor-content :deep(.ProseMirror) {
  outline: none;
  min-height: 300px;
  color: var(--app-text-primary);
  font-family: var(--app-font-family);
  font-size: var(--app-font-size-body);
  line-height: 1.7;
}

/* 标题 */
.editor-content :deep(.ProseMirror h1) {
  font-size: 24px;
  font-weight: 700;
  margin: 20px 0 12px;
  color: var(--app-text-primary);
  line-height: 1.3;
}

.editor-content :deep(.ProseMirror h2) {
  font-size: 20px;
  font-weight: 600;
  margin: 16px 0 10px;
  color: var(--app-text-primary);
  line-height: 1.3;
}

.editor-content :deep(.ProseMirror h3) {
  font-size: 17px;
  font-weight: 600;
  margin: 14px 0 8px;
  color: var(--app-text-primary);
  line-height: 1.4;
}

.editor-content :deep(.ProseMirror h4) {
  font-size: 15px;
  font-weight: 600;
  margin: 12px 0 6px;
  color: var(--app-text-primary);
}

/* 段落 */
.editor-content :deep(.ProseMirror p) {
  margin: 0 0 10px;
}

/* 列表 */
.editor-content :deep(.ProseMirror ul),
.editor-content :deep(.ProseMirror ol) {
  padding-left: 24px;
  margin: 0 0 10px;
}

.editor-content :deep(.ProseMirror li) {
  margin: 2px 0;
}

.editor-content :deep(.ProseMirror li p) {
  margin: 0;
}

/* 引用 */
.editor-content :deep(.ProseMirror blockquote) {
  border-left: 3px solid var(--app-brand-color);
  padding-left: 12px;
  margin: 0 0 10px;
  color: var(--app-text-secondary);
}

/* 代码块 */
.editor-content :deep(.ProseMirror pre) {
  background: var(--app-bg-primary);
  border: 1px solid var(--app-border-light);
  border-radius: 6px;
  padding: 12px 16px;
  margin: 0 0 10px;
  overflow-x: auto;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
}

.editor-content :deep(.ProseMirror code) {
  background: var(--app-bg-tertiary);
  border-radius: 3px;
  padding: 1px 4px;
  font-size: 13px;
  font-family: 'Fira Code', 'Consolas', monospace;
}

.editor-content :deep(.ProseMirror pre code) {
  background: none;
  padding: 0;
  border-radius: 0;
}

/* 表格 */
.editor-content :deep(.ProseMirror table) {
  border-collapse: collapse;
  width: 100%;
  margin: 0 0 10px;
  overflow: hidden;
}

.editor-content :deep(.ProseMirror table td),
.editor-content :deep(.ProseMirror table th) {
  border: 1px solid var(--app-border-medium);
  padding: 8px 12px;
  position: relative;
  min-width: 80px;
  vertical-align: top;
  text-align: left;
  box-sizing: border-box;
}

.editor-content :deep(.ProseMirror table th) {
  background: var(--app-bg-tertiary);
  font-weight: 600;
  color: var(--app-text-primary);
}

.editor-content :deep(.ProseMirror table td) {
  background: var(--app-input-bg);
}

.editor-content :deep(.ProseMirror table .selectedCell) {
  background: var(--app-hover-state);
}

/* 分隔线 */
.editor-content :deep(.ProseMirror hr) {
  border: none;
  border-top: 1px solid var(--app-border-medium);
  margin: 16px 0;
}

/* 加粗/斜体/删除线 */
.editor-content :deep(.ProseMirror strong) {
  font-weight: 700;
}

.editor-content :deep(.ProseMirror em) {
  font-style: italic;
}

.editor-content :deep(.ProseMirror s) {
  text-decoration: line-through;
}

/* 占位符 */
.editor-content :deep(.ProseMirror p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  color: var(--app-text-tertiary);
  pointer-events: none;
  height: 0;
}

/* 只读模式 */
.is-readonly .editor-content {
  padding: 16px 20px;
}

.is-readonly .editor-content :deep(.ProseMirror) {
  min-height: unset;
}
</style>
