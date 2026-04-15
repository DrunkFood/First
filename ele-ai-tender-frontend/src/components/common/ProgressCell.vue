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
  const style = getComputedStyle(document.documentElement)
  if (props.percentage >= 100) return style.getPropertyValue('--app-color-success').trim()
  if (props.percentage >= 60) return style.getPropertyValue('--app-brand-color').trim()
  if (props.percentage >= 30) return style.getPropertyValue('--app-color-warning').trim()
  return style.getPropertyValue('--app-color-danger').trim()
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
  color: var(--app-text-secondary);
  min-width: 36px;
  text-align: right;
}
</style>
