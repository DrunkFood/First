<template>
  <div class="dashboard">
    <h2>欢迎使用AI招标文件编制系统</h2>
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ projectCount }}</div>
            <div class="stat-label">项目总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ requirementCount }}</div>
            <div class="stat-label">需求总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ reviewCount }}</div>
            <div class="stat-label">评审项总数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const projectCount = ref(0)
const requirementCount = ref(0)
const reviewCount = ref(0)
</script>

<style scoped>
.dashboard {
  padding: 20px;
}
.stats-row {
  margin-top: 20px;
}
.stat-item {
  text-align: center;
}
.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #1890ff;
}
.stat-label {
  margin-top: 8px;
  color: #666;
}
</style>
