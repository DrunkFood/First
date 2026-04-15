<template>
  <el-tag
    :type="customColor ? undefined : tagType"
    :effect="effect"
    :color="customColor"
    :class="{ 'is-pulse': pulse }"
    size="small"
    :style="customColor ? { color: '#fff', borderColor: customColor } : {}"
  >
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface StatusMap {
  label: string
  type?: '' | 'success' | 'warning' | 'info' | 'danger'
  color?: string
  pulse?: boolean
}

const props = defineProps<{
  status: string
  typeMap: Record<string, StatusMap>
}>()

const config = computed(() => props.typeMap[props.status] || { label: props.status, type: 'info' as const, color: undefined, pulse: undefined })
const label = computed(() => config.value.label)
const tagType = computed(() => config.value.type || 'info')
const customColor = computed(() => config.value.color)
const effect = computed(() => 'light')
const pulse = computed(() => config.value.pulse)
</script>

<style scoped>
.is-pulse {
  animation: pulse-anim 2s ease-in-out infinite;
}
@keyframes pulse-anim {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
