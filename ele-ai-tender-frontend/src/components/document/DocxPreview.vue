<template>
  <div class="docx-preview-wrapper">
    <div v-if="loading" class="docx-preview-loading">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <span>文档加载中...</span>
    </div>
    <div
      v-show="!loading"
      ref="containerRef"
      class="docx-preview-content"
      :style="{ zoom: zoom / 100 }"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { renderAsync } from 'docx-preview'
import { fileApi } from '@/api/file'

const props = withDefaults(defineProps<{
  fileId: number | null
  zoom?: number
}>(), {
  zoom: 100,
})
const emit = defineEmits<{
  rendered: [payload: { pageCount: number }]
}>()

const loading = ref(false)
const containerRef = ref<HTMLElement | null>(null)
let cancelled = false

const escapeHtml = (value: string) => value
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#39;')

const countRenderedPages = () => {
  const container = containerRef.value
  if (!container) return 0

  const directPages = container.querySelectorAll('.docx-wrapper > section')
  if (directPages.length > 0) return directPages.length

  const docxPages = container.querySelectorAll('section.docx')
  if (docxPages.length > 0) return docxPages.length

  return container.querySelectorAll('section').length
}

onBeforeUnmount(() => {
  cancelled = true
  if (containerRef.value) {
    containerRef.value.innerHTML = ''
  }
})

const renderDocx = async (fileId: number) => {
  loading.value = true
  try {
    const blob = await fileApi.download(fileId) as unknown as Blob
    await nextTick()
    if (cancelled) return
    if (containerRef.value) {
      containerRef.value.innerHTML = ''
      await renderAsync(blob, containerRef.value, undefined, {
        className: 'docx-preview-wrapper',
        inWrapper: true,
        ignoreWidth: false,
        ignoreHeight: false,
        ignoreFonts: false,
        breakPages: true,
        ignoreLastRenderedPageBreak: true,
        experimental: false,
        trimXmlDeclaration: true,
        debug: false,
      })
      await nextTick()
      if (!cancelled) {
        emit('rendered', { pageCount: countRenderedPages() })
      }
    }
  } catch (e) {
    if (cancelled) return
    console.error('Word文档渲染失败:', e)
    ElMessage.warning('Word文档渲染失败，请使用下载功能查看')
  } finally {
    loading.value = false
  }
}

watch(() => props.fileId, (newId) => {
  if (newId) {
    renderDocx(newId)
  } else {
    loading.value = false
    if (containerRef.value) {
      containerRef.value.innerHTML = ''
    }
    emit('rendered', { pageCount: 0 })
  }
}, { immediate: true })

const printDocument = (title = 'Word文档') => {
  if (loading.value) {
    ElMessage.warning('Word文档正在加载，请稍后再打印')
    return
  }
  if (!containerRef.value || !containerRef.value.innerHTML.trim()) {
    ElMessage.warning('暂无可打印的Word文档')
    return
  }

  const clonedContent = containerRef.value.cloneNode(true) as HTMLElement
  clonedContent.style.setProperty('zoom', '1')
  clonedContent.classList.add('docx-print-content')

  const styles = Array.from(document.querySelectorAll('style, link[rel="stylesheet"]'))
    .map(node => node.outerHTML)
    .join('\n')
  const printFrame = document.createElement('iframe')
  printFrame.style.position = 'fixed'
  printFrame.style.right = '0'
  printFrame.style.bottom = '0'
  printFrame.style.width = '0'
  printFrame.style.height = '0'
  printFrame.style.border = '0'
  printFrame.setAttribute('aria-hidden', 'true')
  document.body.appendChild(printFrame)

  const printWindow = printFrame.contentWindow
  const printDocumentRef = printWindow?.document
  if (!printWindow || !printDocumentRef) {
    document.body.removeChild(printFrame)
    ElMessage.error('打开打印窗口失败')
    return
  }

  printDocumentRef.open()
  printDocumentRef.write(`<!doctype html>
<html>
<head>
  <meta charset="UTF-8" />
  <title>${escapeHtml(title)}</title>
  ${styles}
  <style>
    @page { margin: 0; }
    html, body {
      margin: 0;
      padding: 0;
      background: #fff !important;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    .docx-print-root {
      width: 100%;
      min-height: 100%;
      background: #fff !important;
    }
    .docx-print-root .docx-preview-content,
    .docx-print-root .docx-preview-wrapper,
    .docx-print-root .docx-wrapper {
      margin: 0 auto !important;
      padding: 0 !important;
      background: #fff !important;
      box-shadow: none !important;
    }
    .docx-print-root section {
      box-shadow: none !important;
      margin: 0 auto !important;
      page-break-after: always;
      break-after: page;
    }
    .docx-print-root section:last-child {
      page-break-after: auto;
      break-after: auto;
    }
  </style>
</head>
<body>
  <div class="docx-print-root">${clonedContent.outerHTML}</div>
</body>
</html>`)
  printDocumentRef.close()

  const removeFrame = () => {
    if (printFrame.parentNode) {
      printFrame.parentNode.removeChild(printFrame)
    }
  }
  printWindow.onafterprint = removeFrame
  window.setTimeout(() => {
    printWindow.focus()
    printWindow.print()
    window.setTimeout(removeFrame, 1000)
  }, 300)
}

defineExpose({
  containerRef,
  loading,
  printDocument,
})
</script>

<style scoped>
.docx-preview-wrapper {
  position: relative;
}

.docx-preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;
  color: var(--app-text-secondary);
  font-size: 14px;
}

.docx-preview-content {
  min-height: 200px;
}

.docx-preview-content :deep(section > article) {
  background: white;
  padding: 0;
}

.docx-preview-content :deep(section) {
  box-shadow: none;
  margin-bottom: 0;
  padding: 0;
}
</style>
