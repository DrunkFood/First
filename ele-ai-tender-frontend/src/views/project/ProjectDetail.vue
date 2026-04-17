<template>
  <div class="project-detail" v-loading="pageLoading">
    <!-- 顶部项目信息卡 -->
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
        <el-button type="primary" plain @click="handleEditProject">编辑项目</el-button>
      </div>
      <div class="header-meta">
        <div class="meta-item">
          <span class="meta-label">项目编号</span>
          <span class="meta-value">{{ project?.projectCode || '-' }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">项目类别</span>
          <span class="meta-value">{{ projectCategoryLabel }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">项目类型</span>
          <span class="meta-value">{{ projectTypeLabel }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">项目预算</span>
          <span class="meta-value">{{ formatBudgetWanYuan(project?.budget) }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">评审类型</span>
          <span class="meta-value">{{ project?.reviewType === 'MANUAL' ? '人工评审' : '智能评审' }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">创建时间</span>
          <span class="meta-value">{{ formatTime(project?.createTime) }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">创建人</span>
          <span class="meta-value">{{ project?.createName || '-' }}</span>
        </div>
      </div>
    </div>

    <!-- Tab区 -->
    <el-tabs v-model="activeTab" class="detail-tabs">
      <!-- Tab1: 基本信息 -->
      <el-tab-pane label="基本信息" name="basic">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目编号">{{ project?.projectCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目类别">{{ projectCategoryLabel }}</el-descriptions-item>
          <el-descriptions-item label="项目类型">{{ projectTypeLabel }}</el-descriptions-item>
          <el-descriptions-item label="预算金额">{{ formatBudgetWanYuan(project?.budget) }}</el-descriptions-item>
          <el-descriptions-item label="评审类型">{{ project?.reviewType === 'MANUAL' ? '人工评审' : '智能评审' }}</el-descriptions-item>
          <el-descriptions-item label="招标单位">{{ project?.tenderUnit || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ project?.contactPerson || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ project?.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目描述" :span="2">{{ project?.projectDescription || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(project?.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ project?.createName || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 发布区域 -->
        <div v-if="canPublishOrPublished" class="action-section publish-section">
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
            >发布项目</el-button>
            <el-button
              v-if="project?.status === 'PUBLISHED'"
              type="info"
              :icon="FolderChecked"
              @click="handleAction('archive')"
            >归档项目</el-button>
            <el-button
              v-if="canCancel"
              type="danger"
              :icon="CircleClose"
              @click="handleAction('cancel')"
            >取消项目</el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- Tab2: 文档信息 -->
      <el-tab-pane label="文档信息" name="document">
        <div class="document-section">
          <!-- 文档卡片 -->
          <el-card v-if="project?.generatedFileId" shadow="hover" class="doc-card">
            <div class="doc-card-body">
              <div class="doc-file-info">
                <el-icon size="48" color="var(--app-brand-color)"><Document /></el-icon>
                <div class="doc-file-detail">
                  <h4>{{ project?.projectName || '招标文件' }}.docx</h4>
                  <p>版本：最新</p>
                </div>
              </div>
              <div class="doc-file-actions">
                <el-button type="primary" :icon="View" @click="showPreview = true">预览</el-button>
                <el-button :icon="Download" @click="handleExport">导出</el-button>
              </div>
            </div>
          </el-card>
          <el-empty v-else description="暂无生成文档，请先完成文档集成步骤" :image-size="80" />

          <!-- 文档结构大纲 -->
          <div v-if="documentOutline.length" class="doc-outline">
            <h4>文档结构</h4>
            <el-tree
              :data="documentOutline"
              :props="{ children: 'children', label: 'title' }"
              default-expand-all
              class="outline-tree"
            />
          </div>
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
          <el-table-column prop="createTime" label="操作时间" width="180">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
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
  Document,
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

/** 文档结构大纲 */
const documentOutline = computed(() => {
  if (!project.value?.generatedFileId) return []
  return [
    { title: '第一章 招标公告', children: [
      { title: '1.1 招标条件' },
      { title: '1.2 项目概况' },
    ]},
    { title: '第二章 投标人须知', children: [
      { title: '2.1 总则' },
      { title: '2.2 投标人资格' },
    ]},
    { title: '第三章 技术规格及要求', children: [
      { title: '3.1 工程范围' },
      { title: '3.2 技术要求' },
    ]},
    { title: '第四章 评标办法', children: [
      { title: '4.1 评标方法' },
      { title: '4.2 评分标准' },
    ]},
  ]
})

const projectCategoryLabel = computed(() => {
  const category = project.value?.projectCategory
  return category ? (PROJECT_CATEGORY_MAP[category]?.label || category) : '-'
})

const projectTypeLabel = computed(() => {
  const type = project.value?.projectType
  return type ? (PROJECT_TYPE_MAP[type]?.label || type) : '-'
})

function formatTime(value?: string): string {
  if (!value) return '-'
  try {
    const date = new Date(value)
    if (isNaN(date.getTime())) return value
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    const h = String(date.getHours()).padStart(2, '0')
    const min = String(date.getMinutes()).padStart(2, '0')
    const s = String(date.getSeconds()).padStart(2, '0')
    return `${y}-${m}-${d} ${h}:${min}:${s}`
  } catch {
    return value
  }
}

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
  // 未开始的节点禁止跳转
  const currentPhase = project.value?.currentPhase ?? 0
  if (index > currentPhase) return

  // Timeline有7个节点(index 0-6)，向导有5个步骤(index 0-4)
  // 映射: Timeline[0]=项目创建(无向导步骤) -> 跳转步骤0
  //       Timeline[1]=基础信息录入 -> 向导步骤0
  //       Timeline[2]=招标需求生成 -> 向导步骤1
  //       Timeline[3]=评审项设置   -> 向导步骤2
  //       Timeline[4]=文档集成     -> 向导步骤3
  //       Timeline[5]=智能检测     -> 向导步骤4
  //       Timeline[6]=检测通过     -> 向导步骤4(同检测步骤)
  const wizardStep = index <= 0 ? 0 : index >= 6 ? 4 : index - 1
  router.push(`/project/${projectId.value}/wizard?step=${wizardStep}`)
}

function handleEditProject() {
  router.push(`/project/edit/${projectId.value}`)
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

<style scoped lang="scss">
.project-detail {
  padding: 20px;
}

.detail-header {
  background: var(--app-bg-primary);
  border-radius: var(--app-radius-sm);
  padding: 20px;
  margin-bottom: 16px;
  border: 1px solid var(--app-border-light);
}

.header-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;

  h2 {
    margin: 0;
    font-size: 20px;
    color: var(--app-text-primary);
  }
}

.header-meta {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  margin-left: 80px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-label {
  font-size: 12px;
  color: var(--app-text-tertiary);
}

.meta-value {
  font-size: 14px;
  color: var(--app-text-primary);
}

.detail-tabs {
  background: var(--app-bg-primary);
  border-radius: var(--app-radius-sm);
  padding: 20px;
  border: 1px solid var(--app-border-light);
}

.action-section {
  margin-top: 24px;
  padding: 16px;
  background: var(--app-bg-secondary);
  border-radius: var(--app-radius-sm);
}

.publish-section {
  border-left: 3px solid var(--app-color-success);
}

.action-section h4 {
  margin: 0 0 12px;
  font-size: 15px;
  color: var(--app-text-primary);
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
  .doc-card-body {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.doc-file-info {
  display: flex;
  align-items: center;
  gap: 16px;

  h4 {
    margin: 0 0 4px;
    color: var(--app-text-primary);
  }

  p {
    margin: 0;
    color: var(--app-text-tertiary);
    font-size: 13px;
  }
}

.doc-file-actions {
  display: flex;
  gap: 8px;
}

.doc-outline {
  background: var(--app-bg-secondary);
  border-radius: var(--app-radius-sm);
  padding: 16px;

  h4 {
    margin: 0 0 12px;
    font-size: 15px;
    color: var(--app-text-primary);
  }
}

.outline-tree {
  background: transparent;
}

.version-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h4 {
    margin: 0;
    font-size: 15px;
    color: var(--app-text-primary);
  }
}
</style>
