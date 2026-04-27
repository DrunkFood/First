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

const loading = ref(false)
const containerRef = ref<HTMLElement | null>(null)
let cancelled = false

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
  }
}, { immediate: true })
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

.docx-preview-content :deep(.docx-wrapper) {
  background: white;
  padding: 0;
}

.docx-preview-content :deep(.docx-wrapper > section.docx) {
  box-shadow: none;
  margin-bottom: 0;
  padding: 0;
}
</style>
