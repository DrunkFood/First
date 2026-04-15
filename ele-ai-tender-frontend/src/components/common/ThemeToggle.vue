<template>
  <el-button
    :icon="themeStore.mode === 'dark' ? Sunny : Moon"
    circle
    @click="themeStore.toggle()"
    :title="themeStore.mode === 'dark' ? '切换到浅色模式' : '切换到深色模式'"
  />
</template>

<script setup lang="ts">
import { Sunny, Moon } from '@element-plus/icons-vue'
import { useThemeStore } from '@/store/theme'

const themeStore = useThemeStore()
</script>
