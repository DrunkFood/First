<template>
  <div class="phase-document">
    <div class="doc-toolbar">
      <el-button type="primary" :loading="isIntegrating" @click="handleIntegrate">
        执行文档集成
      </el-button>
      <el-button :disabled="!preview?.integrated" @click="handleExport">
        导出Word
      </el-button>
    </div>

    <div v-if="preview?.integrated" class="doc-preview">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="HTML预览" name="html">
          <div class="html-preview" v-html="preview.htmlContent" />
        </el-tab-pane>
        <el-tab-pane label="Markdown编辑" name="markdown">
          <MarkdownEditor v-model="markdownContent" />
          <el-button type="primary" class="save-btn" @click="handleSaveEdit">
            保存修改
          </el-button>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-empty v-else description="请先执行文档集成" />

    <div class="phase-actions">
      <el-button @click="$emit('prev')">上一步</el-button>
      <el-button type="primary" :disabled="!preview?.integrated" @click="$emit('next')">
        继续到检测
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { documentApi } from '@/api/document'
import type { DocumentPreviewVO } from '@/types/document'
import MarkdownEditor from '@/components/editor/MarkdownEditor.vue'

const props = defineProps<{ projectId: number }>()
defineEmits<{ next: []; prev: [] }>()

const preview = ref<DocumentPreviewVO | null>(null)
const markdownContent = ref('')
const activeTab = ref('html')
const isIntegrating = ref(false)

const loadPreview = async () => {
  preview.value = await documentApi.getPreview(props.projectId)
  if (preview.value?.markdownContent) {
    markdownContent.value = preview.value.markdownContent
  }
}

const handleIntegrate = async () => {
  isIntegrating.value = true
  try {
    preview.value = await documentApi.integrate(props.projectId)
    if (preview.value?.markdownContent) {
      markdownContent.value = preview.value.markdownContent
    }
    ElMessage.success('文档集成完成')
  } catch {
    ElMessage.error('文档集成失败')
  } finally {
    isIntegrating.value = false
  }
}

const handleExport = async () => {
  try {
    const blob = await documentApi.exportWord(props.projectId) as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '招标文件.docx'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

const handleSaveEdit = async () => {
  await documentApi.editContent(props.projectId, markdownContent.value)
  ElMessage.success('保存成功')
  await loadPreview()
  activeTab.value = 'html'
}

onMounted(loadPreview)
</script>

<style scoped>
.doc-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.html-preview {
  padding: 20px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  min-height: 400px;
  background: #fff;
}

.save-btn {
  margin-top: 12px;
}

.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
