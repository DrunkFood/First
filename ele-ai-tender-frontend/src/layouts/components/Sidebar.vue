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
    <el-sub-menu index="/project-group">
      <template #title><span>招标文件管理</span></template>
      <el-menu-item index="/project">项目列表</el-menu-item>
      <el-menu-item index="/project/create">新建项目</el-menu-item>
    </el-sub-menu>
    <el-sub-menu index="/requirement-group">
      <template #title><span>业务需求管理</span></template>
      <el-menu-item index="/requirement">需求列表</el-menu-item>
      <el-menu-item index="/requirement/create">新建需求</el-menu-item>
    </el-sub-menu>
    <el-menu-item index="/policy-file">
      <span>政策文件管理</span>
    </el-menu-item>
    <el-menu-item index="/message">
      <span>消息中心</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>
