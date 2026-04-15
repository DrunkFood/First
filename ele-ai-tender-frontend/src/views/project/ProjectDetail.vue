<template>
  <div class="project-detail" v-loading="pageLoading">
    <!-- 顶部信息区 -->
    <div class="detail-header">
      <div class="header-top">
        <el-button :icon="ArrowLeft" @click="router.back()" text>返回</el-button>
        <div class="header-title">
          <h2>{{ project?.projectName || '加载中...' }}</h2>
          <StatusBadge
            v-if="project?.status"
            :status="project.status"
            :type-map="PROJECT_STATUS_MAP"
          />
        </div>
      </div>
      <div class="header-meta">
        <span>项目编号：{{ project?.projectCode || '-' }}</span>
        <el-divider direction="vertical" />
        <span>类别：{{ projectCategoryLabel }}</span>
        <el-divider direction="vertical" />
        <span>类型：{{ projectTypeLabel }}</span>
        <el-divider direction="vertical" />
        <span>预算：{{ formatBudgetWanYuan(project?.budget) }}</span>
        <el-divider direction="vertical" />
        <span>评审类型：{{ project?.reviewType || '-' }}</span>
        <el-divider direction="vertical" />
        <span>创建时间：{{ project?.createTime || '-' }}</span>
        <el-divider direction="vertical" />
        <span>创建人：{{ project?.createName || '-' }}</span>
      </div>
    </div>

    <!-- Tab 区 -->
    <el-tabs v-model="activeTab" class="detail-tabs">
      <!-- Tab1: 基本信息 -->
      <el-tab-pane label="基本信息" name="basic">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目编号">{{ project?.projectCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目类别">{{ projectCategoryLabel }}</el-descriptions-item>
          <el-descriptions-item label="项目类型">{{ projectTypeLabel }}</el-descriptions-item>
          <el-descriptions-item label="预算金额">{{ formatBudgetWanYuan(project?.budget) }}</el-descriptions-item>
          <el-descriptions-item label="评审类型">{{ project?.reviewType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="招标单位">{{ project?.tenderUnit || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ project?.contactPerson || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ project?.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目描述" :span="2">{{ project?.projectDescription || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ project?.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ project?.createName || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 发布区域 -->
        <div v-if="canPublishOrPublished" class="action-section">
          <h4>文档操作</h4>
          <div class="action-buttons">
            <el-tooltip content="功能开发中" placement="top">
              <el-button type="warning" disabled>回传至交易系统</el-button>
            </el-tooltip>
            <el-button type="primary" :icon="View" @click="showPreview = true">预览文档</el-button>
            <el-button type="success" :icon="Download" @click="handleExport" :loading="exportLoading">导出文档</el-button>
          </div>
        </div>

        <!-- 操作按钮区域 -->
        <div class="action-section">
          <h4>项目操作</h4>
          <div class="action-buttons">
            <el-button
              v-if="project?.status === 'DETECTION_PASSED'"
              type="success"
              :icon="Promotion"
              @click="handleAction('publish')"
            >
              发布项目
            </el-button>
            <el-button
              v-if="project?.status === 'PUBLISHED'"
              type="info"
              :icon="FolderChecked"
              @click="handleAction('archive')"
            >
              归档项目
            </el-button>
            <el-button
              v-if="canCancel"
              type="danger"
              :icon="CircleClose"
              @click="handleAction('cancel')"
            >
              取消项目
            </el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- Tab2: 文档信息 -->
      <el-tab-pane label="文档信息" name="document">
        <div class="document-section">
          <el-card shadow="hover" class="doc-card">
            <template #header>
              <div class="doc-card-header">
                <span>生成文档</span>
                <el-tag v-if="project?.generatedFileId" type="success" size="small">已生成</el-tag>
                <el-tag v-else type="info" size="small">未生成</el-tag>
              </div>
            </template>
            <div v-if="project?.generatedFileId" class="doc-info">
              <p>文件ID：{{ project.generatedFileId }}</p>
              <el-button type="primary" :icon="View" size="small" @click="showPreview = true">预览文档</el-button>
            </div>
            <el-empty v-else description="暂无生成文档，请先完成文档集成步骤" :image-size="80" />
          </el-card>
        </div>
      </el-tab-pane>

      <!-- Tab3: 项目进度 -->
      <el-tab-pane label="项目进度" name="progress">
        <ProjectTimeline
          :current-phase="project?.currentPhase ?? 0"
          @click="handleTimelineClick"
        />
      </el-tab-pane>

      <!-- Tab4: 版本管理 -->
      <el-tab-pane label="版本管理" name="version">
        <div class="version-header">
          <h4>版本列表</h4>
          <el-button type="primary" :icon="Sort" @click="showVersionCompare = true">版本对比</el-button>
        </div>
        <el-table :data="versions" stripe style="width: 100%" v-if="versions.length">
          <el-table-column prop="versionNo" label="版本号" width="100">
            <template #default="{ row }">v{{ row.versionNo }}</template>
          </el-table-column>
          <el-table-column prop="createName" label="操作人" width="120" />
          <el-table-column prop="createTime" label="操作时间" width="180" />
          <el-table-column prop="changeDescription" label="变更摘要" min-width="200">
            <template #default="{ row }">{{ row.changeDescription || '-' }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无版本记录" />
      </el-tab-pane>
    </el-tabs>

    <!-- 文档预览弹窗 -->
    <DocumentPreview
      v-model="showPreview"
      :project-id="projectId"
    />

    <!-- 版本对比弹窗 -->
    <VersionCompare
      v-model="showVersionCompare"
      :project-id="projectId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  View,
  Download,
  Promotion,
  FolderChecked,
  CircleClose,
  Sort,
} from '@element-plus/icons-vue'
import { projectApi } from '@/api/project'
import { formatBudgetWanYuan } from '@/utils/budget'
import { PROJECT_STATUS_MAP, PROJECT_CATEGORY_MAP, PROJECT_TYPE_MAP } from '@/constants/status-maps'
import StatusBadge from '@/components/common/StatusBadge.vue'
import ProjectTimeline from '@/components/project/ProjectTimeline.vue'
import DocumentPreview from '@/components/document/DocumentPreview.vue'
import VersionCompare from '@/components/project/VersionCompare.vue'
import type { ProjectInfo, ProjectVersionInfo } from '@/types/project'

const router = useRouter()
const route = useRoute()

const projectId = computed(() => Number(route.params.id))
const project = ref<ProjectInfo | null>(null)
const versions = ref<ProjectVersionInfo[]>([])
const activeTab = ref('basic')
const pageLoading = ref(false)
const exportLoading = ref(false)
const showPreview = ref(false)
const showVersionCompare = ref(false)

const projectCategoryLabel = computed(() => {
  const category = project.value?.projectCategory
  return category ? (PROJECT_CATEGORY_MAP[category]?.label || category) : '-'
})

const projectTypeLabel = computed(() => {
  const type = project.value?.projectType
  return type ? (PROJECT_TYPE_MAP[type]?.label || type) : '-'
})

const canPublishOrPublished = computed(() => {
  const status = project.value?.status
  return status === 'DETECTION_PASSED' || status === 'PUBLISHED'
})

const canCancel = computed(() => {
  const status = project.value?.status
  return status && !['PUBLISHED', 'ARCHIVED', 'CANCELLED'].includes(status)
})

async function loadProject() {
  pageLoading.value = true
  try {
    project.value = await projectApi.getById(projectId.value)
  } catch {
    ElMessage.error('获取项目信息失败')
  } finally {
    pageLoading.value = false
  }
}

async function loadVersions() {
  try {
    versions.value = await projectApi.getVersions(projectId.value)
  } catch {
    versions.value = []
  }
}

function handleTimelineClick(index: number) {
  router.push(`/project/${projectId.value}/wizard?step=${index}`)
}

async function handleExport() {
  exportLoading.value = true
  try {
    await projectApi.export(projectId.value)
    ElMessage.success('文档导出成功')
  } catch {
    ElMessage.error('文档导出失败')
  } finally {
    exportLoading.value = false
  }
}

const actionHandlers: Record<string, { api: () => Promise<any>, success: string, confirm: string }> = {
  publish: {
    api: () => projectApi.publish(projectId.value),
    success: '项目发布成功',
    confirm: '确认发布该项目？发布后项目将进入已发布状态。',
  },
  archive: {
    api: () => projectApi.archive(projectId.value),
    success: '项目归档成功',
    confirm: '确认归档该项目？归档后项目将不可再编辑。',
  },
  cancel: {
    api: () => projectApi.cancel(projectId.value),
    success: '项目已取消',
    confirm: '确认取消该项目？取消后项目将终止所有流程。',
  },
}

async function handleAction(action: 'publish' | 'archive' | 'cancel') {
  const handler = actionHandlers[action]
  if (!handler) return

  try {
    await ElMessageBox.confirm(handler.confirm, '操作确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await handler.api()
    ElMessage.success(handler.success)
    await loadProject()
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadProject()
  loadVersions()
})
</script>

<style scoped>
.project-detail {
  padding: 20px;
}

.detail-header {
  margin-bottom: 20px;
}

.header-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title h2 {
  margin: 0;
  font-size: 20px;
  color: var(--el-text-color-primary);
}

.header-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin-left: 80px;
}

.detail-tabs {
  margin-top: 4px;
}

.action-section {
  margin-top: 24px;
  padding: 16px;
  background: var(--el-bg-color-page);
  border-radius: 8px;
}

.action-section h4 {
  margin: 0 0 12px;
  font-size: 15px;
  color: var(--el-text-color-primary);
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.document-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.doc-card {
  max-width: 600px;
}

.doc-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.doc-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.doc-info p {
  margin: 0;
  color: var(--el-text-color-regular);
}

.version-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.version-header h4 {
  margin: 0;
  font-size: 15px;
  color: var(--el-text-color-primary);
}
</style>
