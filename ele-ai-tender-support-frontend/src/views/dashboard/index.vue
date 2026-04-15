<template>
  <div class="page-shell dashboard-page">
    <div class="page-header-row">
      <div>
        <div class="page-title">运营总览</div>
        <div class="page-subtitle">聚焦当前系统状态与常用操作入口</div>
      </div>
      <el-button @click="fetchData" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div v-loading="loading">
      <!-- 核心指标 -->
      <div class="stats-grid">
        <el-card v-for="item in coreStatCards" :key="item.label" shadow="never" class="panel-card stat-card">
          <div class="stat-head">
            <div class="icon-box" :class="item.className">
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </div>
            <div class="stat-meta">
              <div class="meta-label">{{ item.label }}</div>
              <div class="meta-value">{{ item.value }}</div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 待办提醒 -->
      <div v-if="todoItems.length > 0" class="todo-bar">
        <el-card shadow="never" class="panel-card todo-card">
          <div class="todo-list">
            <div v-for="item in todoItems" :key="item.label" class="todo-item" :class="item.level">
              <el-icon :size="16"><component :is="item.icon" /></el-icon>
              <span class="todo-label">{{ item.label }}</span>
              <el-tag :type="item.tagType" round size="small">{{ item.count }}</el-tag>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 辅助指标 -->
      <div class="stats-grid secondary">
        <el-card v-for="item in secondaryStatCards" :key="item.label" shadow="never" class="panel-card stat-card small">
          <div class="stat-simple">
            <span class="simple-label">{{ item.label }}</span>
            <span class="simple-value" :class="item.highlight">{{ item.value }}</span>
          </div>
        </el-card>
      </div>

      <!-- 趋势 + 项目状态 + 快捷入口 -->
      <div class="bottom-grid">
        <!-- 近7天操作趋势 -->
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span>近7天操作趋势</span>
          </template>
          <el-table :data="overview?.dailyOperations || []" border stripe size="small">
            <el-table-column prop="date" label="日期" width="120" />
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

        <!-- 项目状态分布 -->
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span>项目状态分布</span>
          </template>
          <el-table :data="overview?.projectStatusDist || []" border stripe size="small">
            <el-table-column prop="statusName" label="状态" />
            <el-table-column prop="count" label="数量">
              <template #default="{ row }">
                <div class="bar-wrap">
                  <div class="status-bar" :class="statusClass(row.status)" :style="{ width: statusBarWidth(row.count) }" />
                  <span class="bar-text">{{ row.count }}</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!overview?.projectStatusDist?.length" description="暂无项目数据" :image-size="60" />
        </el-card>

        <!-- 快捷入口 + 系统信息 -->
        <div class="right-col">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <span>快捷入口</span>
            </template>
            <div class="quick-grid">
              <button class="quick-tile" @click="$router.push('/system/user')">
                <el-icon><User /></el-icon>
                <span>用户管理</span>
              </button>
              <button class="quick-tile" @click="$router.push('/system/role')">
                <el-icon><UserFilled /></el-icon>
                <span>角色管理</span>
              </button>
              <button class="quick-tile" @click="$router.push('/template')">
                <el-icon><Notebook /></el-icon>
                <span>模板管理</span>
              </button>
              <button class="quick-tile" @click="$router.push('/knowledge')">
                <el-icon><Collection /></el-icon>
                <span>知识库</span>
              </button>
              <button class="quick-tile" @click="$router.push('/external')">
                <el-icon><Connection /></el-icon>
                <span>接入系统</span>
              </button>
              <button class="quick-tile" @click="$router.push('/message')">
                <el-icon><Bell /></el-icon>
                <span>消息中心</span>
              </button>
            </div>
          </el-card>

          <el-card shadow="never" class="panel-card system-info-card">
            <template #header>
              <span>系统信息</span>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="系统名称">EleAITender 支撑中心</el-descriptions-item>
              <el-descriptions-item label="系统版本">v1.0.0</el-descriptions-item>
              <el-descriptions-item label="后端框架">Spring Boot 3.2</el-descriptions-item>
              <el-descriptions-item label="前端框架">Vue 3 + Element Plus</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  User, UserFilled, Connection, FolderOpened,
  Collection, Bell, Refresh, Notebook, Warning, Clock, CircleClose,
} from '@element-plus/icons-vue'
import type { StatisticsOverview } from '@/types/statistics'
import { statisticsApi } from '@/api/statistics'

const loading = ref(false)
const overview = ref<StatisticsOverview | null>(null)

const coreStatCards = computed(() => {
  const o = overview.value
  if (!o) return []
  return [
    { label: '项目总数', value: o.projectCount, icon: FolderOpened, className: 'projects' },
    { label: '需求总数', value: o.requirementCount, icon: Collection, className: 'requirements' },
    { label: '用户总数', value: o.userCount, icon: User, className: 'users' },
    { label: '模板总数', value: o.templateCount, icon: Notebook, className: 'templates' },
  ]
})

const secondaryStatCards = computed(() => {
  const o = overview.value
  if (!o) return []
  return [
    { label: '知识文档', value: o.knowledgeCount, highlight: '' },
    { label: '接入系统', value: o.accessSystemCount, highlight: '' },
    { label: '模型配置', value: o.modelConfigCount, highlight: '' },
    { label: '今日新建项目', value: o.todayProjectCount, highlight: 'accent' },
    { label: '今日操作', value: o.todayOperationCount, highlight: 'accent' },
    { label: '未读消息', value: o.unreadMessageCount, highlight: o.unreadMessageCount > 0 ? 'warn' : '' },
    { label: '角色总数', value: o.roleCount, highlight: '' },
    { label: '版本总数', value: o.versionCount, highlight: '' },
  ]
})

/** 待办事项：只展示数量 > 0 的项目 */
const todoItems = computed(() => {
  const o = overview.value
  if (!o) return []
  const items: { label: string; count: number; icon: typeof Warning; level: string; tagType: 'danger' | 'warning' | 'info' }[] = []
  if (o.pendingDetectionCount > 0) {
    items.push({ label: '待检测项目', count: o.pendingDetectionCount, icon: Clock, level: 'warn', tagType: 'warning' })
  }
  if (o.detectionFailedCount > 0) {
    items.push({ label: '检测未通过', count: o.detectionFailedCount, icon: CircleClose, level: 'danger', tagType: 'danger' })
  }
  if (o.unreadMessageCount > 0) {
    items.push({ label: '未读消息', count: o.unreadMessageCount, icon: Bell, level: 'info', tagType: 'info' })
  }
  if (o.inProgressCount > 0) {
    items.push({ label: '编制中项目', count: o.inProgressCount, icon: Warning, level: 'accent', tagType: 'info' })
  }
  return items
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

/** 根据项目状态返回对应的样式类 */
const statusClass = (status: string): string => {
  const map: Record<string, string> = {
    DRAFT: 'status-draft',
    IN_PROGRESS: 'status-progress',
    PENDING_DETECTION: 'status-pending',
    DETECTING: 'status-detecting',
    DETECTION_PASSED: 'status-passed',
    DETECTION_FAILED: 'status-failed',
    PUBLISHED: 'status-published',
    ARCHIVED: 'status-archived',
    CANCELLED: 'status-cancelled',
  }
  return map[status] || 'status-draft'
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

<style scoped lang="scss">
.dashboard-page {
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
    margin-bottom: 14px;

    &.secondary {
      grid-template-columns: repeat(4, minmax(0, 1fr));
    }
  }

  .stat-card {
    border: none;

    &.small {
      :deep(.el-card__body) {
        padding: 12px 16px;
      }
    }
  }

  .stat-head {
    display: flex;
    gap: 12px;
    align-items: center;
  }

  .icon-box {
    width: 42px;
    height: 42px;
    border-radius: 12px;
    display: grid;
    place-items: center;
    color: #fff;

    &.projects {
      background: linear-gradient(130deg, #197b55, #0f8a5f);
    }

    &.requirements {
      background: linear-gradient(130deg, #2f9068, #16806a);
    }

    &.users {
      background: linear-gradient(130deg, #aa7a2d, #c17814);
    }

    &.templates {
      background: linear-gradient(130deg, #8d5f25, #a96f12);
    }
  }

  .meta-label {
    font-size: 12px;
    color: var(--et-text-weak, #909399);
  }

  .meta-value {
    margin-top: 2px;
    font-size: 28px;
    font-weight: 700;
    color: #173528;
  }

  .stat-simple {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .simple-label {
    font-size: 13px;
    color: var(--et-text-weak, #909399);
  }

  .simple-value {
    font-size: 18px;
    font-weight: 700;
    color: #173528;

    &.accent {
      color: var(--el-color-primary, #197b55);
    }

    &.warn {
      color: var(--el-color-warning, #e6a23c);
    }
  }

  .todo-bar {
    margin-bottom: 14px;
  }

  .todo-card {
    border: none;
    background: linear-gradient(135deg, #fef9f0 0%, #fdf6ec 100%);

    :deep(.el-card__body) {
      padding: 10px 16px;
    }
  }

  .todo-list {
    display: flex;
    gap: 24px;
    flex-wrap: wrap;
  }

  .todo-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;

    &.warn {
      color: var(--el-color-warning, #e6a23c);
    }

    &.danger {
      color: var(--el-color-danger, #f56c6c);
    }

    &.accent {
      color: var(--el-color-primary, #197b55);
    }

    &.info {
      color: var(--el-color-info, #909399);
    }
  }

  .todo-label {
    white-space: nowrap;
  }

  .bottom-grid {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr;
    gap: 14px;
  }

  .right-col {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .system-info-card {
    flex: 1;
  }

  .quick-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .quick-tile {
    border: 1px solid #d3e1da;
    border-radius: 12px;
    background: #f7fcf9;
    color: #1c392b;
    height: 54px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    cursor: pointer;
    transition: 0.2s ease;

    &:hover {
      border-color: #8fbca7;
      background: #eef8f3;
      transform: translateY(-1px);
    }
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

  .status-bar {
    height: 18px;
    border-radius: 4px;
    transition: width 0.3s;

    &.status-draft {
      background: #c0c4cc;
    }

    &.status-progress {
      background: #409eff;
    }

    &.status-pending {
      background: #e6a23c;
    }

    &.status-detecting {
      background: #f56c6c;
    }

    &.status-passed {
      background: #67c23a;
    }

    &.status-failed {
      background: #f56c6c;
    }

    &.status-published {
      background: #197b55;
    }

    &.status-archived {
      background: #909399;
    }

    &.status-cancelled {
      background: #c0c4cc;
    }
  }

  .bar-text {
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
  }
}

@media (max-width: 1200px) {
  .dashboard-page {
    .bottom-grid {
      grid-template-columns: 1fr 1fr;
    }
  }
}

@media (max-width: 1100px) {
  .dashboard-page {
    .stats-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .bottom-grid {
      grid-template-columns: 1fr;
    }
  }
}

@media (max-width: 640px) {
  .dashboard-page .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
