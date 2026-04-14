<template>
  <div class="phase-requirement">
    <AiUnavailableAlert
      :visible="isAiUnavailable"
      @retry="handleRetry"
      @skip="handleSkipAi"
    />

    <div class="requirement-toolbar">
      <el-button type="primary" :loading="isGenerating" @click="handleGenerate">
        AI 生成需求
      </el-button>
      <AiTaskStatus
        v-if="task"
        :task="task"
        :show-actions="true"
        @retry="handleRetry"
        @skip="handleSkipAi"
      />
      <span v-if="isSaving" class="auto-save-hint">自动保存中...</span>
      <span v-else-if="lastSaveTime" class="auto-save-hint">上次自动保存: {{ lastSaveTime }}</span>
    </div>

    <div class="editor-wrapper">
      <MarkdownEditor v-model="content" />
    </div>

    <div class="phase-actions">
      <el-button @click="$emit('prev')">上一步</el-button>
      <el-button type="primary" @click="handleSaveAndNext">保存并继续</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { requirementApi } from '@/api/requirement'
import { projectApi } from '@/api/project'
import { useTaskPolling } from '@/composables/useTaskPolling'
import { useAutoSave } from '@/composables/useAutoSave'
import AiTaskStatus from '@/components/AiTaskStatus.vue'
import AiUnavailableAlert from '@/components/AiUnavailableAlert.vue'
import MarkdownEditor from '@/components/editor/MarkdownEditor.vue'

const props = defineProps<{ projectId: number }>()
const emit = defineEmits<{ next: []; prev: [] }>()

const content = ref('')
const requirementId = ref(0)
const taskId = ref<number | null>(null)
const isGenerating = ref(false)

const { task, retry: retryTask, skip: skipTask } = useTaskPolling(taskId)

const { isSaving, lastSaveTime, startAutoSave, recoverDraft } = useAutoSave(
  requirementId,
  content,
  (id, data) => requirementApi.autoSave(id, data.content),
  (id) => requirementApi.getAutoSave(id),
  (id) => requirementApi.clearAutoSave(id),
)

const isAiUnavailable = computed(() => task.value?.status === 'AI_UNAVAILABLE')

const loadData = async () => {
  const project = await projectApi.getById(props.projectId)
  if (project.requirementId) {
    requirementId.value = project.requirementId
    const req = await requirementApi.getById(project.requirementId)
    content.value = req.content || ''
    await recoverDraft()
    startAutoSave()
  }
}

const handleGenerate = async () => {
  if (!requirementId.value) return
  isGenerating.value = true
  try {
    const res = await requirementApi.generate(requirementId.value, {})
    taskId.value = res.id
  } catch {
    ElMessage.error('提交AI生成失败')
  } finally {
    isGenerating.value = false
  }
}

const handleRetry = () => retryTask()
const handleSkipAi = () => skipTask()

const handleSaveAndNext = async () => {
  if (requirementId.value && content.value) {
    await requirementApi.update(requirementId.value, { content: content.value })
  }
  ElMessage.success('需求内容保存成功')
  emit('next')
}

onMounted(loadData)
</script>

<style scoped>
.requirement-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.auto-save-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.editor-wrapper {
  min-height: 400px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
}

.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
