<template>
  <div class="generation-status">
    <!-- 进行中（含 COMPLETED+synced=0 等待同步） -->
    <template v-if="task && !canCreateNew">
      <div class="status-icon spinning">
        <el-icon :size="24"><Loading /></el-icon>
      </div>
      <div class="status-info">
        <div class="status-title">{{ generatingTitle }}</div>
        <div class="status-desc">{{ generatingDesc }}</div>
        <div class="progress-bar-container">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPercent + '%' }" />
          </div>
          <div class="progress-text">{{ progressPercent }}%</div>
        </div>
      </div>
    </template>
    <!-- 已完成（resultSynced=1） -->
    <template v-else-if="task && isTaskSucceeded(task)">
      <div class="status-icon completed">
        <el-icon :size="24"><CircleCheck /></el-icon>
      </div>
      <div class="status-info">
        <div class="status-title">{{ completedTitle }}</div>
        <div class="status-desc">{{ completedDesc }}</div>
        <div class="progress-bar-container">
          <div class="progress-bar">
            <div class="progress-fill" style="width: 100%" />
          </div>
          <div class="progress-text">100%</div>
        </div>
      </div>
    </template>
    <!-- 失败（含 COMPLETED+resultSynced=2 同步失败） -->
    <template v-else-if="task && (task.status === 'FAILED' || (task.status === 'COMPLETED' && task.resultSynced === 2))">
      <div class="status-icon failed">
        <el-icon :size="24"><CircleClose /></el-icon>
      </div>
      <div class="status-info">
        <div class="status-title">{{ failedTitle }}</div>
        <div class="status-desc">{{ task.errorMsg || failedDesc }}</div>
      </div>
    </template>
    <!-- 空闲：待生成 -->
    <template v-else>
      <div class="status-icon idle">
        <el-icon :size="24"><Document /></el-icon>
      </div>
      <div class="status-info">
        <div class="status-title">{{ idleTitle }}</div>
        <div class="status-desc">{{ idleDesc }}</div>
      </div>
      <slot name="idle-action" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { Loading, CircleCheck, CircleClose, Document } from '@element-plus/icons-vue'
import type { AiTaskVO } from '@/types/ai-task'
import { isTaskSucceeded } from '@/types/ai-task'

withDefaults(defineProps<{
  task: AiTaskVO | null
  canCreateNew: boolean
  progressPercent: number
  generatingTitle?: string
  generatingDesc?: string
  completedTitle?: string
  completedDesc?: string
  failedTitle?: string
  failedDesc?: string
  idleTitle?: string
  idleDesc?: string
}>(), {
  generatingTitle: '正在生成中',
  generatingDesc: 'AI正在生成内容，请稍候...',
  completedTitle: '生成完成',
  completedDesc: '内容已生成完成，您可以在下方查看和修改',
  failedTitle: '生成失败',
  failedDesc: 'AI生成过程出现异常，请重新尝试',
  idleTitle: 'AI生成',
  idleDesc: '点击按钮开始AI生成内容',
})
</script>

<style scoped lang="scss">
.generation-status {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--app-bg-tertiary);
  border-radius: var(--app-radius-sm);
  margin-bottom: 20px;
}

.status-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--app-brand-color);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;

  &.spinning {
    animation: spin 1s linear infinite;
  }

  &.completed {
    background: var(--app-color-success);
  }

  &.failed {
    background: var(--app-color-danger);
  }

  &.idle {
    background: var(--app-brand-color);
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.status-info {
  flex: 1;
  min-width: 0;
}

.status-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin-bottom: 4px;
}

.status-desc {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.progress-bar-container {
  margin-top: 12px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: var(--app-bg-elevated);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--app-brand-color), var(--app-auxiliary-color));
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: var(--app-text-secondary);
  margin-top: 4px;
  text-align: right;
}
</style>
