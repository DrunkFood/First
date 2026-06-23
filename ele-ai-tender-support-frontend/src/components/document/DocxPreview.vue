<template>
  <div class="docx-preview">
    <div v-if="loading" class="docx-preview__loading">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <span>文档加载中...</span>
    </div>
    <div
      v-show="!loading"
      ref="containerRef"
      class="docx-preview__content"
    />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { renderAsync } from 'docx-preview'
import { fileApi } from '@/api/file'

const props = defineProps<{
  fileId: number | string | null | undefined
}>()

const loading = ref(false)
const containerRef = ref<HTMLElement | null>(null)
let disposed = false

const clearPreview = () => {
  if (containerRef.value) {
    containerRef.value.innerHTML = ''
  }
}

const renderDocx = async (fileId: number | string) => {
  loading.value = true
  try {
    const blob = await fileApi.download(fileId)
    await nextTick()
    if (disposed) return
    clearPreview()
    if (containerRef.value) {
      await renderAsync(blob, containerRef.value, undefined, {
        className: 'docx-preview-document',
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
    }
  } catch (error) {
    if (!disposed) {
      console.error('Word preview failed:', error)
      ElMessage.warning('Word文档预览失败，请下载后查看')
    }
  } finally {
    if (!disposed) {
      loading.value = false
    }
  }
}

watch(
  () => props.fileId,
  (fileId) => {
    clearPreview()
    if (fileId) {
      renderDocx(fileId)
    } else {
      loading.value = false
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  disposed = true
  clearPreview()
})
</script>

<style scoped lang="scss">
.docx-preview {
  min-height: 420px;
  background: #f5f7fa;
  overflow: auto;
}

.docx-preview__loading {
  min-height: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #606266;
  font-size: 14px;
}

.docx-preview__content {
  min-height: 420px;
  padding: 16px;
}

.docx-preview__content :deep(.docx-wrapper) {
  background: transparent;
  padding: 0;
}

.docx-preview__content :deep(section.docx-preview-document) {
  margin: 0 auto 16px;
  box-shadow: 0 2px 12px rgba(31, 35, 41, 0.12);
}
</style>
