<template>
  <div class="page-shell statistics-page">
    <div class="page-header-row">
      <div>
        <div class="page-title">统计分析</div>
        <div class="page-subtitle">支撑中心核心业务数据统计概览</div>
      </div>
      <el-button @click="fetchData">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div v-loading="loading">
      <!-- 数字卡片 -->
      <el-row :gutter="12" style="margin-bottom: 20px">
        <el-col :span="6" v-for="card in statCards" :key="card.label">
          <el-card shadow="never" class="stat-card">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 近7天操作趋势 -->
      <div class="trend-grid">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span style="font-weight: 600">近7天操作趋势</span>
          </template>
          <el-table :data="overview?.dailyOperations || []" border stripe>
            <el-table-column prop="date" label="日期" />
            <el-table-column prop="count" label="操作次数">
              <template #default="{ row }">
                <div class="bar-wrap">
                  <div class="bar" :style="{ width: barWidth(row.count) }" />
                  <span class="bar-text">{{ row.count }}</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="panel-card">
          <template #header>
            <span style="font-weight: 600">项目状态分布</span>
          </template>
          <el-table :data="overview?.projectStatusDist || []" border stripe>
            <el-table-column prop="statusName" label="状态" />
            <el-table-column prop="count" label="数量">
              <template #default="{ row }">
                <div class="bar-wrap">
                  <div class="bar" :style="{ width: statusBarWidth(row.count) }" />
                  <span class="bar-text">{{ row.count }}</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!overview?.projectStatusDist?.length" description="暂无项目数据" :image-size="60" />
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import type { StatisticsOverview } from '@/types/statistics'
import { statisticsApi } from '@/api/statistics'

const loading = ref(false)
const overview = ref<StatisticsOverview | null>(null)

const statCards = computed(() => {
  const o = overview.value
  if (!o) return []
  return [
    { label: '项目总数', value: o.projectCount },
    { label: '需求总数', value: o.requirementCount },
    { label: '用户总数', value: o.userCount },
    { label: '角色总数', value: o.roleCount },
    { label: '模板总数', value: o.templateCount },
    { label: '知识文档', value: o.knowledgeCount },
    { label: '模型配置', value: o.modelConfigCount },
    { label: '接入系统', value: o.accessSystemCount },
    { label: '版本总数', value: o.versionCount },
    { label: '操作日志', value: o.operationLogCount },
    { label: '政策文件', value: o.policyFileCount },
    { label: '今日操作', value: o.todayOperationCount },
    { label: '今日新建项目', value: o.todayProjectCount },
    { label: '未读消息', value: o.unreadMessageCount },
  ]
})

const maxCount = computed(() => {
  const items = overview.value?.dailyOperations || []
  return Math.max(...items.map(i => i.count), 1)
})

const maxProjectStatus = computed(() => {
  const items = overview.value?.projectStatusDist || []
  return Math.max(...items.map(i => i.count), 1)
})

const barWidth = (count: number) => {
  return `${Math.max((count / maxCount.value) * 100, 2)}%`
}

const statusBarWidth = (count: number) => {
  return `${Math.max((count / maxProjectStatus.value) * 100, 2)}%`
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await statisticsApi.getOverview()
    overview.value = res.data
  } catch (error) {
    console.error('Fetch statistics failed:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.stat-card {
  text-align: center;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-color-primary);
  line-height: 1.4;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
.trend-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.bar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}
.bar {
  height: 18px;
  background: var(--el-color-primary-light-5);
  border-radius: 4px;
  transition: width 0.3s;
}
.bar-text {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}
</style>
