<template>
  <el-container class="main-layout">
    <el-header class="main-header">
      <Header />
    </el-header>
    <el-container>
      <el-aside width="200px" class="main-aside">
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
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0;
}
.main-aside {
  background: #001529;
}
.main-content {
  background: #f0f2f5;
  padding: 20px;
}
</style>
