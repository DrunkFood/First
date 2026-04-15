<template>
  <div class="progress-cell">
    <el-progress :percentage="percentage" :stroke-width="8" :color="color" />
    <span class="progress-text">{{ percentage }}%</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  percentage: number
}>()

const color = computed(() => {
  if (props.percentage >= 100) return '#67c23a'
  if (props.percentage >= 60) return '#409eff'
  if (props.percentage >= 30) return '#e6a23c'
  return '#f56c6c'
})
</script>

<style scoped>
.progress-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.progress-cell :deep(.el-progress) {
  flex: 1;
}
.progress-text {
  font-size: 12px;
  color: #606266;
  min-width: 36px;
  text-align: right;
}
</style>
