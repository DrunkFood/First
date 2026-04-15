<template>
  <div class="detection-progress">
    <!-- 检测状态标题 -->
    <div class="progress-header">
      <el-icon v-if="isDetecting" size="40" class="spinning" color="var(--app-brand-color)"><Loading /></el-icon>
      <el-icon v-else size="40" color="var(--app-color-success)"><CircleCheck /></el-icon>
      <div class="progress-title">
        <h3>{{ isDetecting ? '正在执行智能检测' : '检测完成' }}</h3>
        <p v-if="isDetecting">系统正在进行公平竞争检测、合规性检查和错别字检查，请稍候...</p>
      </div>
    </div>

    <!-- 4项检测卡片 -->
    <div v-if="progress" class="progress-items">
      <div
        v-for="item in progress.items"
        :key="item.detectionType"
        class="progress-item"
        :class="getItemClass(item.taskStatus)"
      >
        <div class="item-header">
          <div class="item-left">
            <el-icon v-if="item.taskStatus === 'COMPLETED'" color="var(--app-color-success)"><CircleCheck /></el-icon>
            <el-icon v-else-if="item.taskStatus === 'PROCESSING'" class="spinning" color="var(--app-brand-color)"><Loading /></el-icon>
            <el-icon v-else-if="item.taskStatus === 'FAILED'" color="var(--app-color-danger)"><CircleClose /></el-icon>
            <el-icon v-else color="var(--app-text-tertiary)"><Clock /></el-icon>
            <span class="item-name">{{ item.typeName }}</span>
          </div>
          <el-tag :type="getStatusType(item.taskStatus)" size="small">
            {{ getStatusLabel(item.taskStatus) }}
          </el-tag>
        </div>
        <el-progress
          :percentage="getPercentage(item)"
          :status="getProgressStatus(item.taskStatus)"
          :stroke-width="8"
        />
        <div v-if="item.taskStatus === 'COMPLETED' && item.issueCount > 0" class="issue-count">
          发现 {{ item.issueCount }} 个问题
        </div>
        <div v-if="item.taskStatus === 'COMPLETED' && item.issueCount === 0" class="no-issue">
          未发现问题
        </div>
      </div>
    </div>

    <!-- 总进度 -->
    <div v-if="progress" class="total-progress">
      <div class="total-header">
        <span>总进度</span>
        <span class="total-percent">{{ totalPercentage }}%</span>
      </div>
      <el-progress
        :percentage="totalPercentage"
        :stroke-width="12"
        :status="totalPercentage === 100 ? 'success' : ''"
      />
    </div>

    <el-empty v-if="!progress" description="暂无检测数据" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { Loading, CircleCheck, CircleClose, Clock } from '@element-plus/icons-vue'
import { detectionApi } from '@/api/detection'
import type { DetectionProgressVO, DetectionItemProgress } from '@/types/detection'

const props = defineProps<{ projectId: number }>()

const progress = ref<DetectionProgressVO | null>(null)
let timer: ReturnType<typeof setInterval> | null = null

const isDetecting = computed(() => progress.value?.overallStatus === 'DETECTING')

const totalPercentage = computed(() => {
  if (!progress.value?.items?.length) return 0
  const items = progress.value.items
  const completed = items.filter(i => i.taskStatus === 'COMPLETED' || i.taskStatus === 'FAILED').length
  return Math.round((completed / items.length) * 100)
})

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

const getItemClass = (status: string) => {
  if (status === 'COMPLETED') return 'completed'
  if (status === 'PROCESSING') return 'processing'
  if (status === 'FAILED') return 'failed'
  return 'pending'
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

const getPercentage = (item: DetectionItemProgress) => {
  if (item.taskStatus === 'COMPLETED') return 100
  if (item.taskStatus === 'FAILED') return 100
  if (item.taskStatus === 'PROCESSING') return 60
  if (item.taskStatus === 'PENDING') return 10
  return 0
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

<style scoped lang="scss">
.detection-progress {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--app-bg-secondary);
  border-radius: var(--app-radius-sm);
  text-align: center;

  .progress-title {
    h3 { margin: 0 0 4px; color: var(--app-text-primary); }
    p { margin: 0; color: var(--app-text-tertiary); font-size: 14px; }
  }
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.progress-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-item {
  padding: 12px 16px;
  border: 1px solid var(--app-border-light);
  border-radius: 6px;
  background: var(--app-bg-secondary);
  transition: var(--app-transition-base);

  &.processing {
    border-left: 3px solid var(--app-brand-color);
  }

  &.completed {
    border-left: 3px solid var(--app-color-success);
  }

  &.failed {
    border-left: 3px solid var(--app-color-danger);
  }

  &.pending {
    opacity: 0.6;
  }
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.item-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-name {
  font-weight: 500;
  color: var(--app-text-primary);
}

.issue-count {
  color: var(--app-color-danger);
  font-size: 12px;
  margin-top: 4px;
}

.no-issue {
  color: var(--app-color-success);
  font-size: 12px;
  margin-top: 4px;
}

.total-progress {
  padding: 12px 16px;
  background: var(--app-bg-secondary);
  border-radius: 6px;
  border: 1px solid var(--app-brand-color);
}

.total-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: var(--app-text-primary);
  font-weight: 500;
}

.total-percent {
  color: var(--app-brand-color);
  font-size: 16px;
  font-weight: 700;
}
</style>
