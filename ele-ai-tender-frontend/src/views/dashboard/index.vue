<template>
  <div class="dashboard">
    <h2 class="page-title">工作台</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: 'var(--app-brand-color)' }">
            <el-icon :size="28"><Folder /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.projectCount }}</div>
            <div class="stat-label">项目总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: 'var(--app-color-success)' }">
            <el-icon :size="28"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.requirementCount }}</div>
            <div class="stat-label">需求总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: 'var(--app-color-warning)' }">
            <el-icon :size="28"><Checked /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.detectionPassRate }}%</div>
            <div class="stat-label">检测通过率</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: 'var(--app-color-danger)' }">
            <el-icon :size="28"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.inProgressCount }}</div>
            <div class="stat-label">编制中项目</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 最近项目 -->
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>最近项目</span>
              <el-button link type="primary" @click="router.push('/project')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentProjects" stripe size="small" v-loading="loading">
            <el-table-column prop="projectName" label="项目名称" min-width="160">
              <template #default="{ row }">
                <el-link type="primary" @click="router.push(`/project/${row.id}`)">{{ row.projectName }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="projectCategory" label="类别" width="100">
              <template #default="{ row }">
                <StatusBadge :status="row.projectCategory" :typeMap="PROJECT_CATEGORY_MAP" />
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <StatusBadge :status="row.status" :typeMap="PROJECT_STATUS_MAP" />
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="160" />
          </el-table>
          <el-empty v-if="!loading && recentProjects.length === 0" description="暂无项目" :image-size="60" />
        </el-card>
      </el-col>

      <!-- 快捷操作 + 待办 -->
      <el-col :span="10">
        <el-card shadow="hover" class="quick-card">
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="router.push('/requirement/create')">
              <el-icon><Plus /></el-icon> 新建需求
            </el-button>
            <el-button @click="router.push('/project/create')">
              <el-icon><FolderAdd /></el-icon> 新建项目
            </el-button>
            <el-button @click="router.push('/requirement')">
              <el-icon><List /></el-icon> 需求列表
            </el-button>
            <el-button @click="router.push('/project')">
              <el-icon><Folder /></el-icon> 项目列表
            </el-button>
          </div>
        </el-card>

        <el-card shadow="hover" style="margin-top: 16px">
          <template #header>
            <span>待办事项</span>
          </template>
          <div class="todo-list">
            <div v-for="item in todoItems" :key="item.label" class="todo-item">
              <span class="todo-label">{{ item.label }}</span>
              <el-badge :value="item.count" :type="item.count > 0 ? 'danger' : 'info'" />
            </div>
            <el-empty v-if="todoItems.length === 0" description="暂无待办" :image-size="40" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Folder, Document, Checked, Clock, Plus, FolderAdd, List } from '@element-plus/icons-vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { PROJECT_STATUS_MAP, PROJECT_CATEGORY_MAP } from '@/constants/status-maps'
import { projectApi } from '@/api/project'
import { requirementApi } from '@/api/requirement'
import type { ProjectInfo } from '@/types/project'

const router = useRouter()
const loading = ref(false)

const stats = ref({
  projectCount: 0,
  requirementCount: 0,
  detectionPassRate: 0,
  inProgressCount: 0,
})

const recentProjects = ref<ProjectInfo[]>([])

const todoItems = ref<{ label: string; count: number }[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const [projectRes, requirementRes] = await Promise.allSettled([
      projectApi.getList({ pageNum: 1, pageSize: 5 }),
      requirementApi.getList({ pageNum: 1, pageSize: 1 }),
    ])

    if (projectRes.status === 'fulfilled') {
      const data = projectRes.value
      stats.value.projectCount = data.total
      recentProjects.value = data.records
      stats.value.inProgressCount = data.records.filter(
        (p: ProjectInfo) => p.status === 'IN_PROGRESS'
      ).length
    }

    if (requirementRes.status === 'fulfilled') {
      stats.value.requirementCount = requirementRes.value.total
    }

    const [pendingDetection, pendingReview] = await Promise.allSettled([
      projectApi.getList({ pageNum: 1, pageSize: 1, status: 'PENDING_DETECTION' }),
      requirementApi.getList({ pageNum: 1, pageSize: 1, status: 'PENDING_REVIEW' }),
    ])

    todoItems.value = []
    if (pendingDetection.status === 'fulfilled' && pendingDetection.value.total > 0) {
      todoItems.value.push({ label: '待检测项目', count: pendingDetection.value.total })
    }
    if (pendingReview.status === 'fulfilled' && pendingReview.value.total > 0) {
      todoItems.value.push({ label: '待审核需求', count: pendingReview.value.total })
    }
  } catch {
    // 静默处理
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}
.page-title {
  margin: 0 0 20px;
  font-size: 20px;
  font-weight: 600;
  color: var(--app-text-primary);
}
.stat-row {
  margin-bottom: 20px;
}
.stat-card {
  display: flex;
  align-items: center;
  transition: var(--app-transition-base);
}
.stat-card:hover {
  transform: translateY(-4px);
}
.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--app-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.stat-info {
  flex: 1;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--app-text-primary);
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: var(--app-text-tertiary);
  margin-top: 4px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.todo-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.todo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--app-border-light);
}
.todo-item:last-child {
  border-bottom: none;
}
.todo-label {
  font-size: 14px;
  color: var(--app-text-primary);
}
</style>
