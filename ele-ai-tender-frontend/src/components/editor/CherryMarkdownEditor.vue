<template>
  <div ref="editorRef" class="cherry-markdown-editor"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import Cherry from 'cherry-markdown'
import 'cherry-markdown/dist/cherry-markdown.css'
import { useThemeStore } from '@/store/theme'

const modelValue = defineModel<string>({ default: '' })

const props = withDefaults(defineProps<{
  readonly?: boolean
}>(), {
  readonly: false,
})

const editorRef = ref<HTMLDivElement>()
const themeStore = useThemeStore()

let cherryInstance: InstanceType<typeof Cherry> | null = null
// 防止 setMarkdown 触发 afterChange 导致无限循环
let isInternalChange = false
// 流式更新节流：合并高频 content 赋值，减少 setMarkdown 调用
let pendingContent: string | null = null
let flushTimer: ReturnType<typeof setTimeout> | null = null

onMounted(() => {
  initEditor()
})

function initEditor() {
  if (!editorRef.value) return

  const containerId = 'cherry-' + crypto.randomUUID()
  editorRef.value.id = containerId

  cherryInstance = new Cherry({
    id: containerId,
    value: modelValue.value,
    editor: {
      defaultModel: props.readonly ? 'previewOnly' : 'editOnly',
      height: '100%',
    },
    isPreviewOnly: props.readonly,
    toolbars: {
      toolbar: props.readonly ? [] : [
        'bold', 'italic', 'strikethrough', '|',
        'color', 'header', '|',
        'list', 'insert', '|',
        'graph', 'settings',
      ],
    },
    callback: {
      afterChange: (text: string) => {
        // isInternalChange=true 说明是 setMarkdown 触发的，在回调内重置标志位并跳过
        if (isInternalChange) {
          isInternalChange = false
          return
        }
        modelValue.value = text
      },
    },
  })

  if (themeStore.mode === 'dark') {
    cherryInstance.setTheme('dark')
  }
}

/** 节流刷新：将高频 content 更新合并，最多每 200ms 刷新一次编辑器 */
function scheduleFlush(newVal: string) {
  pendingContent = newVal
  if (flushTimer) return
  flushTimer = setTimeout(() => {
    flushTimer = null
    if (cherryInstance && pendingContent !== null) {
      const content = pendingContent
      pendingContent = null
      isInternalChange = true
      cherryInstance.setMarkdown(content, true)
      // isInternalChange 在 afterChange 回调中重置
    }
  }, 200)
}

watch(modelValue, (newVal) => {
  if (!cherryInstance) return
  // 直接赋值场景（如页面加载）立即刷新，流式更新场景节流刷新
  if (pendingContent === null && !flushTimer) {
    const current = cherryInstance.getMarkdown()
    if (current !== newVal) {
      isInternalChange = true
      cherryInstance.setMarkdown(newVal || '', true)
    }
  } else {
    scheduleFlush(newVal)
  }
})

watch(() => props.readonly, (isReadonly) => {
  if (!cherryInstance) return
  cherryInstance.switchModel(isReadonly ? 'previewOnly' : 'editOnly', !isReadonly)
})

watch(() => themeStore.mode, (mode) => {
  if (!cherryInstance) return
  cherryInstance.setTheme(mode === 'dark' ? 'dark' : 'light')
})

onBeforeUnmount(() => {
  if (flushTimer) {
    clearTimeout(flushTimer)
    flushTimer = null
  }
  if (cherryInstance) {
    cherryInstance.destroy()
    cherryInstance = null
  }
})
</script>

<style scoped>
.cherry-markdown-editor {
  min-height: 400px;
  width: 100%;
  height: 100%;
}

.cherry-markdown-editor :deep(.cherry) {
  height: 100%;
}

.cherry-markdown-editor :deep(.cherry-editor) {
  height: 100%;
}

.cherry-markdown-editor :deep(.cherry-preview) {
  padding: 16px;
}
</style>
