<template>
  <div v-if="task" class="ai-task-status">
    <div class="task-header">
      <span class="task-type">{{ task.taskTypeName }}</span>
      <el-tag :type="statusTagType" size="small">{{ task.statusName }}</el-tag>
    </div>

    <el-progress
      :percentage="progressPercent"
      :status="progressStatusValue"
      :stroke-width="8"
      class="task-progress"
    />

    <div v-if="task.errorMsg" class="task-error">
      <el-text type="danger" size="small">{{ task.errorMsg }}</el-text>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AiTaskVO, AiTaskStatus } from '@/types/ai-task'
import { getTaskProgress, getProgressStatus } from '@/types/ai-task'

const props = defineProps<{
  task: AiTaskVO | null
}>()

const statusTagType = computed(() => {
  if (!props.task) return 'info'
  const map: Record<AiTaskStatus, string> = {
    PENDING: 'info',
    PROCESSING: '',
    COMPLETED: 'success',
    FAILED: 'danger',
    AI_UNAVAILABLE: 'warning',
    SKIPPED: 'info',
  }
  return map[props.task.status] || 'info'
})

const progressPercent = computed(() => {
  if (!props.task) return 0
  return getTaskProgress(props.task.status)
})

const progressStatusValue = computed(() => {
  if (!props.task) return ''
  return getProgressStatus(props.task.status)
})
</script>

<style scoped>
.ai-task-status {
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.task-type {
  font-weight: 500;
  font-size: 14px;
}

.task-progress {
  margin-bottom: 8px;
}

.task-error {
  margin-bottom: 8px;
}
</style>
