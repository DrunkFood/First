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
      <template v-if="integrated && fileId">
        <DocxPreview ref="docxPreviewRef" :file-id="fileId" />
      </template>
      <el-empty v-else-if="!loading" description="暂无文档内容" />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Printer, Close } from '@element-plus/icons-vue'
import { documentApi } from '@/api/document'
import { fileApi } from '@/api/file'
import DocxPreview from '@/components/document/DocxPreview.vue'

const props = defineProps<{
  modelValue: boolean
  projectId: number
  generatedFileId?: number | null
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const loading = ref(false)
const downloadLoading = ref(false)
const fileId = ref<number | null>(null)
const integrated = ref(false)
const docxPreviewRef = ref<InstanceType<typeof DocxPreview> | null>(null)
const projectName = ref('文档')

async function handleOpen() {
  if (!props.projectId) return
  loading.value = true
  try {
    const data = await documentApi.getPreview(props.projectId)
    fileId.value = data.generatedFileId
    integrated.value = data.integrated
    projectName.value = data.projectName || '文档'
  } catch {
    fileId.value = null
    integrated.value = false
    ElMessage.error('获取文档预览失败')
  } finally {
    loading.value = false
  }
}

async function handleDownload() {
  const targetFileId = fileId.value || props.generatedFileId
  if (!targetFileId) {
    ElMessage.warning('文档尚未生成，无法下载')
    return
  }
  downloadLoading.value = true
  try {
    const blob = await fileApi.download(targetFileId)
    const url = window.URL.createObjectURL(blob as Blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${projectName.value}.docx`
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
  const targetFileId = fileId.value || props.generatedFileId
  if (!targetFileId) {
    ElMessage.warning('文档尚未生成，无法打印')
    return
  }
  docxPreviewRef.value?.printDocument(`${projectName.value}.docx`)
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
</style>
