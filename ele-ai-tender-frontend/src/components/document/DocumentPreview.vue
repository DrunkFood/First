<template>
  <el-dialog
    :model-value="modelValue"
    title="文档预览"
    fullscreen
    @update:model-value="$emit('update:modelValue', $event)"
    @open="handleOpen"
  >
    <template #header>
      <div class="preview-header">
        <span class="preview-title">文档预览</span>
        <div class="preview-toolbar">
          <el-button :icon="Download" @click="handleDownload" :loading="downloadLoading">下载</el-button>
          <el-button :icon="Printer" @click="handlePrint">打印</el-button>
          <el-button :icon="Close" @click="$emit('update:modelValue', false)">关闭</el-button>
        </div>
      </div>
    </template>

    <div v-loading="loading" class="document-preview">
      <div v-if="previewData" class="preview-content" v-html="previewData.htmlContent"></div>
      <el-empty v-else-if="!loading" description="暂无文档内容" />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Printer, Close } from '@element-plus/icons-vue'
import { documentApi } from '@/api/document'
import type { DocumentPreviewVO } from '@/types/document'

const props = defineProps<{
  modelValue: boolean
  projectId: number
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const loading = ref(false)
const downloadLoading = ref(false)
const previewData = ref<DocumentPreviewVO | null>(null)

async function handleOpen() {
  if (!props.projectId) return
  loading.value = true
  try {
    previewData.value = await documentApi.getPreview(props.projectId)
  } catch {
    previewData.value = null
    ElMessage.error('获取文档预览失败')
  } finally {
    loading.value = false
  }
}

async function handleDownload() {
  downloadLoading.value = true
  try {
    const blob = await documentApi.exportWord(props.projectId)
    const url = window.URL.createObjectURL(blob as Blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${previewData.value?.projectName || '文档'}.docx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('文档下载成功')
  } catch {
    ElMessage.error('文档下载失败')
  } finally {
    downloadLoading.value = false
  }
}

function handlePrint() {
  const contentEl = document.querySelector('.preview-content')
  if (!contentEl) return
  const printWindow = window.open('', '_blank')
  if (!printWindow) return
  printWindow.document.write(`
    <html>
      <head><title>打印预览</title></head>
      <body>${contentEl.innerHTML}</body>
    </html>
  `)
  printWindow.document.close()
  printWindow.print()
  printWindow.close()
}
</script>

<style scoped>
.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.preview-title {
  font-size: 18px;
  font-weight: 600;
}
.preview-toolbar {
  display: flex;
  gap: 8px;
}
.document-preview {
  min-height: 400px;
}
.preview-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
  background: var(--app-bg-primary);
  line-height: 1.8;
  color: var(--app-text-primary);
}
.preview-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}
.preview-content :deep(td),
.preview-content :deep(th) {
  border: 1px solid var(--app-border-medium);
  padding: 8px 12px;
}
.preview-content :deep(h1),
.preview-content :deep(h2),
.preview-content :deep(h3) {
  margin: 16px 0 8px;
}
</style>
