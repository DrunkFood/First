<template>
  <div class="detection-progress">
    <h4>检测进度</h4>
    <div v-if="progress" class="progress-items">
      <div v-for="item in progress.items" :key="item.detectionType" class="progress-item">
        <div class="item-header">
          <span>{{ item.typeName }}</span>
          <el-tag :type="getStatusType(item.taskStatus)" size="small">
            {{ getStatusLabel(item.taskStatus) }}
          </el-tag>
        </div>
        <el-progress
          :percentage="getPercentage(item.taskStatus)"
          :status="getProgressStatus(item.taskStatus)"
          :stroke-width="6"
        />
        <div v-if="item.issueCount > 0" class="issue-count">
          发现 {{ item.issueCount }} 个问题
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无检测数据" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { detectionApi } from '@/api/detection'
import type { DetectionProgressVO } from '@/types/detection'

const props = defineProps<{ projectId: number }>()

const progress = ref<DetectionProgressVO | null>(null)
let timer: ReturnType<typeof setInterval> | null = null

const loadProgress = async () => {
  progress.value = await detectionApi.getProgress(props.projectId)
  if (progress.value?.overallStatus !== 'DETECTING') {
    stopPolling()
  }
}

const startPolling = () => {
  timer = setInterval(loadProgress, 3000)
}

const stopPolling = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    PENDING: 'info', PROCESSING: '', COMPLETED: 'success',
    FAILED: 'danger', AI_UNAVAILABLE: 'warning',
  }
  return map[status] || 'info'
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '等待中', PROCESSING: '检测中', COMPLETED: '已完成',
    FAILED: '失败', AI_UNAVAILABLE: 'AI不可用',
  }
  return map[status] || status
}

const getPercentage = (status: string) => {
  const map: Record<string, number> = {
    PENDING: 10, PROCESSING: 60, COMPLETED: 100, FAILED: 100, AI_UNAVAILABLE: 0,
  }
  return map[status] || 0
}

const getProgressStatus = (status: string): '' | 'success' | 'warning' | 'exception' => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'exception'
  if (status === 'AI_UNAVAILABLE') return 'warning'
  return ''
}

onMounted(() => {
  loadProgress()
  startPolling()
})

onBeforeUnmount(stopPolling)
</script>

<style scoped>
.detection-progress h4 {
  margin-bottom: 16px;
}

.progress-item {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.issue-count {
  color: var(--el-color-danger);
  font-size: 12px;
  margin-top: 4px;
}
</style>
