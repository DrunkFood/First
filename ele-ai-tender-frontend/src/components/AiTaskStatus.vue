<template>
  <div v-if="task" class="ai-task-status">
    <div class="task-header">
      <span class="task-type">{{ task.taskTypeName }}</span>
      <el-tag :type="statusTagType" size="small">{{ task.statusName }}</el-tag>
    </div>

    <el-progress
      v-if="showProgress"
      :percentage="progressPercent"
      :status="progressStatus"
      :stroke-width="8"
      class="task-progress"
    />

    <div v-if="task.errorMsg" class="task-error">
      <el-text type="danger" size="small">{{ task.errorMsg }}</el-text>
    </div>

    <div v-if="showActions" class="task-actions">
      <el-button
        v-if="canRetry"
        size="small"
        type="primary"
        @click="$emit('retry')"
      >
        重试
      </el-button>
      <el-button
        v-if="canSkip"
        size="small"
        @click="$emit('skip')"
      >
        跳过
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AiTaskVO, AiTaskStatus } from '@/types/ai-task'

const props = defineProps<{
  task: AiTaskVO | null
  showActions?: boolean
}>()

defineEmits<{
  retry: []
  skip: []
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

const showProgress = computed(() => {
  return props.task && ['PENDING', 'PROCESSING'].includes(props.task.status)
})

const progressPercent = computed(() => {
  if (!props.task) return 0
  if (props.task.status === 'PENDING') return 10
  if (props.task.status === 'PROCESSING') return 60
  if (props.task.status === 'COMPLETED') return 100
  return 0
})

const progressStatus = computed<'' | 'success' | 'warning' | 'exception'>(() => {
  if (!props.task) return ''
  if (props.task.status === 'COMPLETED') return 'success'
  if (props.task.status === 'FAILED') return 'exception'
  if (props.task.status === 'AI_UNAVAILABLE') return 'warning'
  return ''
})

const canRetry = computed(() => {
  return props.task && ['FAILED', 'AI_UNAVAILABLE'].includes(props.task.status)
})

const canSkip = computed(() => {
  return props.task && ['FAILED', 'AI_UNAVAILABLE', 'PENDING'].includes(props.task.status)
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

.task-actions {
  display: flex;
  gap: 8px;
}
</style>
