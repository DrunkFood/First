<template>
  <el-container class="main-layout">
    <el-header class="main-header">
      <Header />
    </el-header>
    <el-container>
      <el-aside width="240px" class="main-aside">
        <Sidebar />
      </el-aside>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import Header from './components/Header.vue'
import Sidebar from './components/Sidebar.vue'
</script>

<style scoped>
.main-layout {
  height: 100vh;
}
.main-header {
  background: var(--app-header-bg);
  box-shadow: var(--app-header-shadow);
  padding: 0;
  transition: var(--app-transition-base);
}
.main-aside {
  background: var(--app-sidebar-bg);
  backdrop-filter: var(--app-backdrop-blur);
  -webkit-backdrop-filter: var(--app-backdrop-blur);
  border-right: 1px solid var(--app-border-light);
  transition: var(--app-transition-base);
}
.main-content {
  background: var(--app-bg-secondary);
  padding: 20px;
  transition: var(--app-transition-base);
}
</style>
