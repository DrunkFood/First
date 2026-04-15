<template>
  <router-view />
</template>

<script setup lang="ts">
import { useThemeStore } from '@/store/theme'

// 初始化主题 store，确保 watch 生效
useThemeStore()
</script>
