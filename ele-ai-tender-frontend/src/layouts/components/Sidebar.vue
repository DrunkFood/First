<template>
  <el-menu
    :default-active="route.path"
    background-color="#001529"
    text-color="#ffffffb3"
    active-text-color="#1890ff"
    router
  >
    <el-menu-item index="/">
      <span>首页</span>
    </el-menu-item>
    <el-menu-item index="/project">
      <span>项目管理</span>
    </el-menu-item>
    <el-menu-item index="/requirement">
      <span>需求编制</span>
    </el-menu-item>
    <el-menu-item index="/review">
      <span>评审项管理</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>
