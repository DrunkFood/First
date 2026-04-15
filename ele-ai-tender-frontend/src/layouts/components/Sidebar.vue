<template>
  <el-menu
    :default-active="route.path"
    background-color="#001529"
    text-color="#ffffffb3"
    active-text-color="#1890ff"
    router
  >
    <el-menu-item index="/dashboard">
      <span>首页</span>
    </el-menu-item>
    <el-sub-menu index="/requirement-group">
      <template #title><span>业务需求管理</span></template>
      <el-menu-item index="/requirement">业务需求列表</el-menu-item>
    </el-sub-menu>
    <el-sub-menu index="/project-group">
      <template #title><span>招标文件管理</span></template>
      <el-menu-item index="/project">项目列表</el-menu-item>
    </el-sub-menu>
    <el-menu-item index="/policy-file">
      <span>政策文件管理</span>
    </el-menu-item>
    <el-menu-item index="/message">
      <span>消息中心</span>
      <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" class="msg-badge" />
    </el-menu-item>
  </el-menu>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { messageApi } from '@/api/message'

const route = useRoute()
const unreadCount = ref(0)

onMounted(async () => {
  try {
    const res = await messageApi.getUnreadCount()
    unreadCount.value = res as number
  } catch { /* ignore */ }
})
</script>

<style scoped>
.msg-badge {
  margin-left: 8px;
}
.msg-badge :deep(.el-badge__content) {
  top: 8px;
}
</style>
