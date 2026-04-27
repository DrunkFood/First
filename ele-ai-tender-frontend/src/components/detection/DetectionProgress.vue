<template>
  <div class="detection-progress">
    <!-- 居中状态图标 -->
    <div class="review-status">
      <div class="status-icon" :class="{ detecting: isDetecting, completed: !isDetecting }">
        <el-icon v-if="isDetecting" :size="40"><Clock /></el-icon>
        <el-icon v-else :size="40"><CircleCheck /></el-icon>
      </div>
      <div class="status-title">{{ isDetecting ? '正在执行智能检测' : '检测完成' }}</div>
      <div class="status-desc">
        {{ isDetecting ? '系统正在进行公平竞争检测、合规性检查和错别字检查，请稍候...' : '智能检测已完成，请查看检测报告' }}
      </div>
    </div>

    <!-- 4项检测卡片 -->
    <div v-if="progress" class="review-items">
      <div
        v-for="item in progress.items"
        :key="item.detectionType"
        class="review-item"
        :class="getItemClass(item.taskStatus)"
      >
        <div class="review-item-header">
          <div class="review-item-title">{{ item.typeName }}</div>
          <div class="review-item-status" :class="getItemClass(item.taskStatus)">
            <el-icon v-if="item.taskStatus === 'COMPLETED'" :size="16"><CircleCheck /></el-icon>
            <el-icon v-else-if="item.taskStatus === 'PROCESSING'" :size="16" class="spinning"><Loading /></el-icon>
            <el-icon v-else-if="item.taskStatus === 'FAILED'" :size="16"><CircleClose /></el-icon>
            <el-icon v-else :size="16"><Clock /></el-icon>
            {{ getStatusLabel(item.taskStatus) }}
          </div>
        </div>
        <div class="progress-bar-container">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: getPercentage(item) + '%' }"></div>
          </div>
          <div class="progress-text">{{ getPercentage(item) }}%</div>
        </div>
      </div>
    </div>

    <!-- 总进度 -->
    <div v-if="progress" class="total-progress">
      <div class="total-progress-header">
        <div class="total-progress-title">总进度</div>
        <div class="total-progress-percent">{{ totalPercentage }}%</div>
      </div>
      <div class="progress-bar-container">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: totalPercentage + '%' }"></div>
        </div>
      </div>
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
const emit = defineEmits<{ completed: [overallStatus: string] }>()

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
    emit('completed', progress.value.overallStatus)
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
  gap: 20px;
}

.review-status {
  text-align: center;
  padding: 40px 20px;
}

.status-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  color: white;

  &.detecting {
    background: var(--app-brand-color);
    animation: pulse 2s infinite;
  }

  &.completed {
    background: var(--app-color-success);
    animation: none;
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.05); opacity: 0.8; }
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.status-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin-bottom: 8px;
}

.status-desc {
  font-size: 14px;
  color: var(--app-text-secondary);
  margin-bottom: 32px;
}

.review-items {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.review-item {
  background: var(--app-bg-tertiary);
  border-radius: 8px;
  padding: 20px;
  border: 1px solid var(--app-border-light);
}

.review-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.review-item-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-primary);
}

.review-item-status {
  font-size: 13px;
  color: var(--app-text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;

  &.completed { color: var(--app-color-success); }
  &.processing { color: var(--app-brand-color); }
  &.failed { color: var(--app-color-danger); }
}

.progress-bar-container {
  margin-top: 12px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: var(--app-bg-elevated, var(--app-bg-tertiary));
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--app-brand-color), #31E3FD);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  text-align: right;
  font-size: 12px;
  color: var(--app-text-secondary);
  margin-top: 6px;
}

.total-progress {
  background: var(--app-bg-tertiary);
  border-radius: 8px;
  padding: 20px;
  border: 2px solid var(--app-brand-color);
}

.total-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.total-progress-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-primary);
}

.total-progress-percent {
  font-size: 18px;
  font-weight: 600;
  color: var(--app-brand-color);
}
</style>
