<template>
  <div class="header-container">
    <div class="logo">AI招标文件编制系统</div>
    <div class="user-info">
      <el-dropdown>
        <span class="user-name">管理员</span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { removeToken } from '@/utils/auth'
import { useRouter } from 'vue-router'

const router = useRouter()

function handleLogout() {
  removeToken()
  router.push('/login')
}
</script>

<style scoped>
.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  padding: 0 20px;
}
.logo {
  font-size: 18px;
  font-weight: bold;
  color: #1890ff;
}
.user-name {
  cursor: pointer;
  color: #666;
}
</style>
