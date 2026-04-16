<template>
  <div class="progress-cell">
    <el-progress :percentage="percentage" :stroke-width="8" :color="color" />
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
.progress-cell :deep(.el-progress) {
  flex: 1;
}
</style>
