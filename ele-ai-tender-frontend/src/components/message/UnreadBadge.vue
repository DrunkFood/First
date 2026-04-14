<template>
  <div class="unread-badge" @click="$router.push('/message')">
    <el-badge :value="count" :hidden="count === 0" :max="99">
      <el-icon :size="20"><Bell /></el-icon>
    </el-badge>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { messageApi } from '@/api/message'

const count = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const loadCount = async () => {
  try {
    count.value = await messageApi.getUnreadCount()
  } catch {
    // 忽略
  }
}

onMounted(() => {
  loadCount()
  timer = setInterval(loadCount, 30000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.unread-badge {
  cursor: pointer;
  display: flex;
  align-items: center;
}
</style>
