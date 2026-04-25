<template>
  <div class="phase-document">
    <!-- 生成状态卡片 -->
    <div class="generation-status">
      <!-- 已集成完成 -->
      <template v-if="preview?.integrated">
        <div class="status-icon completed">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </div>
        <div class="status-info">
          <div class="status-title">文档集成完成</div>
          <div class="status-desc">招标文件已成功生成，请预览确认</div>
          <div class="progress-bar-container">
            <div class="progress-bar">
              <div class="progress-fill" style="width: 100%" />
            </div>
            <div class="progress-text">100%</div>
          </div>
        </div>
      </template>
      <!-- 集成中 -->
      <template v-else-if="isIntegrating">
        <div class="status-icon spinning">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <path d="M12 6v6l4 2" />
          </svg>
        </div>
        <div class="status-info">
          <div class="status-title">正在集成中</div>
          <div class="status-desc">正在生成招标文件，请稍候...</div>
          <div class="progress-bar-container">
            <div class="progress-bar">
              <div class="progress-fill" style="width: 60%" />
            </div>
            <div class="progress-text">60%</div>
          </div>
        </div>
      </template>
      <!-- 待集成 -->
      <template v-else>
        <div class="status-icon idle">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="status-info">
          <div class="status-title">待执行文档集成</div>
          <div class="status-desc">请先选择政策文件，然后执行文档集成</div>
        </div>
        <button v-if="!readonly" class="btn btn-primary generate-btn" :disabled="isIntegrating || !canCreateNew" @click="handleIntegrate">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
          </svg>
          执行集成
        </button>
      </template>
    </div>

    <!-- 文档信息栏 -->
    <div v-if="preview?.integrated" class="document-info">
      <div class="info-item">
        <div class="info-label">文档名称</div>
        <div class="info-value">{{ project?.projectName || '招标文件' }}.docx</div>
      </div>
      <div class="info-item">
        <div class="info-label">文档大小</div>
        <div class="info-value">-</div>
      </div>
      <div class="info-item">
        <div class="info-label">页数</div>
        <div class="info-value">-</div>
      </div>
      <div class="info-item">
        <div class="info-label">生成时间</div>
        <div class="info-value">{{ formatTime(project?.createTime) }}</div>
      </div>
    </div>

    <!-- 章节标题 -->
    <h4 class="section-title">文档确认</h4>

    <!-- 预览容器 -->
    <div v-if="preview?.integrated" class="preview-container">
      <!-- 目录浮层面板 -->
      <transition name="toc-fade">
        <div v-if="showTocPanel" class="toc-panel">
          <div class="toc-title">目录导航</div>
          <template v-for="chapter in tocData" :key="chapter.id">
            <div class="toc-item" @click="handleTocClick(chapter)">{{ chapter.title }}</div>
            <div
              v-for="child in chapter.children"
              :key="child.id"
              class="toc-item level-2"
              @click="handleTocClick(child)"
            >
              {{ child.title }}
            </div>
          </template>
        </div>
      </transition>

      <!-- 预览工具栏 -->
      <div class="preview-toolbar">
        <div class="toolbar-left">
          <button class="toolbar-btn" :class="{ active: showTocPanel }" @click="showTocPanel = !showTocPanel">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </svg>
            目录
          </button>
          <button class="toolbar-btn" @click="handlePrint">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="6 9 6 2 18 2 18 9" />
              <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2" />
              <rect x="6" y="14" width="12" height="8" />
            </svg>
            打印
          </button>
          <button class="toolbar-btn" @click="handleExport">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" />
              <line x1="12" y1="15" x2="12" y2="3" />
            </svg>
            下载
          </button>
        </div>
        <div class="toolbar-right">
          <div class="zoom-controls">
            <button class="toolbar-btn" @click="handleZoomOut">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
                <line x1="8" y1="11" x2="14" y2="11" />
              </svg>
            </button>
            <span class="zoom-value">{{ zoomLevel }}%</span>
            <button class="toolbar-btn" @click="handleZoomIn">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
                <line x1="11" y1="8" x2="11" y2="14" />
                <line x1="8" y1="11" x2="14" y2="11" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- Word文档预览内容 -->
      <DocxPreview :file-id="preview?.generatedFileId ?? null" :zoom="zoomLevel" />
    </div>

    <!-- 未集成时显示空状态 -->
    <div v-else class="empty-state">
      <el-icon :size="48" color="var(--app-text-tertiary)"><Document /></el-icon>
      <p>请先执行文档集成</p>
      <button v-if="!readonly" class="btn btn-secondary" @click="policyModalVisible = true">选择政策文件</button>
    </div>

    <!-- 底部操作栏 -->
    <div class="form-actions">
      <div class="form-actions-left">
        <button class="btn btn-secondary" @click="$emit('prev')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          上一步
        </button>
      </div>
      <div v-if="!readonly" class="form-actions-right">
        <button class="btn btn-secondary" @click="handleSaveDraft">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
            <polyline points="17 21 17 13 7 13 7 21" />
            <polyline points="7 3 7 8 15 8" />
          </svg>
          保存草稿
        </button>
        <button class="btn btn-success" :disabled="!preview?.integrated" @click="handleSubmitReview">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 11l3 3L22 4" />
            <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
          </svg>
          提交检测
        </button>
      </div>
    </div>

    <!-- 政策文件选择弹窗 -->
    <el-dialog v-model="policyModalVisible" title="政策文件匹配" width="560px" destroy-on-close>
      <div class="policy-modal-content">
        <div class="policy-group">
          <div class="policy-group-label">项目类别：<span class="highlight">{{ PROJECT_CATEGORY_MAP[project?.projectCategory || '']?.label || project?.projectCategory || '-' }}</span></div>
          <p class="policy-group-hint">系统根据项目类别匹配到以下政策文件，请选择需要应用的文件。</p>
        </div>
        <div class="policy-group">
          <h4 class="policy-group-title">匹配的政策文件</h4>
          <div class="policy-file-list">
            <label
              v-for="file in knowledgePolicyDocs"
              :key="file.id"
              class="policy-file-item"
            >
              <input v-model="selectedPolicyMap[file.id]" type="checkbox" class="policy-checkbox" />
              <div class="policy-file-info">
                <div class="policy-file-name">{{ file.docName }}</div>
                <div class="policy-file-meta">政策文件 - {{ file.fileType || 'PDF' }}</div>
              </div>
            </label>
            <div v-if="!knowledgePolicyDocs.length" class="policy-empty">暂无政策文件</div>
          </div>
        </div>
        <div class="policy-group">
          <h4 class="policy-group-title">本单位政策文件</h4>
          <div class="policy-file-list">
            <label
              v-for="file in otherPolicyFiles"
              :key="file.id"
              class="policy-file-item"
            >
              <input v-model="selectedPolicyMap[file.id]" type="checkbox" class="policy-checkbox" />
              <div class="policy-file-info">
                <div class="policy-file-name">{{ file.fileName }}</div>
                <div class="policy-file-meta">内部文件 - {{ file.fileType || 'DOCX' }}</div>
              </div>
            </label>
            <div v-if="!otherPolicyFiles.length" class="policy-empty">暂无政策文件</div>
          </div>
        </div>
      </div>
      <template #footer>
        <button class="btn btn-secondary" @click="policyModalVisible = false">取消</button>
        <button class="btn btn-primary" @click="handleConfirmPolicyFiles">确认并检测</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import DocxPreview from '@/components/document/DocxPreview.vue'
import { documentApi } from '@/api/document'
import { fileApi } from '@/api/file'
import { policyFileApi } from '@/api/policy-file'
import { projectApi } from '@/api/project'
import { useLatestTask } from '@/composables/useLatestTask'
import { PROJECT_CATEGORY_MAP } from '@/constants/status-maps'
import type { DocumentPreviewVO } from '@/types/document'
import type { KnowledgeDocumentPolicyVO, PolicyFileVO } from '@/types/policy-file'
import type { ProjectInfo } from '@/types/project'

const props = defineProps<{ projectId: number; readonly?: boolean }>()
const emit = defineEmits<{ next: []; prev: [] }>()

const preview = ref<DocumentPreviewVO | null>(null)
const showTocPanel = ref(false)
const zoomLevel = ref(100)
const policyModalVisible = ref(false)

// 文档集成AI任务状态管理
const projectIdRef = computed(() => props.projectId)
const { latestTask, canCreateNew, setActive, refresh } = useLatestTask(
  'DOCUMENT_INTEGRATION',
  projectIdRef,
  'PROJECT',
  (task) => {
    // AI任务完成后，带重试刷新预览（后端同步结果可能有延迟）
    if (task.status === 'COMPLETED') {
      loadPreviewWithRetry()
    } else if (task.status === 'FAILED' || task.status === 'AI_UNAVAILABLE') {
      ElMessage.error('文档集成失败，请重试')
    }
  },
)

const isIntegrating = computed(() =>
  latestTask.value?.status === 'PENDING' || latestTask.value?.status === 'PROCESSING'
)

// --- 文档目录 ---
const tocData = [
  { title: '第一章 招标公告', id: 'chapter1', children: [
    { title: '1.1 招标条件', id: 'section1-1' },
    { title: '1.2 项目概况', id: 'section1-2' },
  ]},
  { title: '第二章 投标人须知', id: 'chapter2', children: [
    { title: '2.1 总则', id: 'section2-1' },
    { title: '2.2 投标人资格', id: 'section2-2' },
  ]},
  { title: '第三章 技术规格及要求', id: 'chapter3', children: [
    { title: '3.1 工程范围', id: 'section3-1' },
    { title: '3.2 技术要求', id: 'section3-2' },
  ]},
  { title: '第四章 评标办法', id: 'chapter4', children: [
    { title: '4.1 评标方法', id: 'section4-1' },
    { title: '4.2 评分标准', id: 'section4-2' },
  ]},
]

const handleTocClick = (data: any) => {
  const el = document.getElementById(data.id)
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

const handleZoomIn = () => { zoomLevel.value = Math.min(200, zoomLevel.value + 10) }
const handleZoomOut = () => { zoomLevel.value = Math.max(50, zoomLevel.value - 10) }
const handlePrint = () => window.print()

const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).replace(/\//g, '-')
}

// --- 政策文件匹配 ---
const knowledgePolicyDocs = ref<KnowledgeDocumentPolicyVO[]>([])
const policyFiles = ref<PolicyFileVO[]>([])
const selectedPolicyMap = reactive<Record<number, boolean>>({})
const project = ref<ProjectInfo | null>(null)

const otherPolicyFiles = computed(() => policyFiles.value)

const loadKnowledgePolicyDocs = async () => {
  try {
    knowledgePolicyDocs.value = await policyFileApi.getKnowledgePolicyDocuments()
  } catch {
    ElMessage.error('获取知识库政策文档列表失败')
  }
}

const loadPolicyFiles = async () => {
  try {
    policyFiles.value = await policyFileApi.getAllAvailable(project.value?.projectCategory)
  } catch {
    ElMessage.error('获取政策文件列表失败')
  }
}

const handleConfirmPolicyFiles = async () => {
  // 提取选中的文件ID
  const selectedDocIds = Object.entries(selectedPolicyMap)
    .filter(([, checked]) => checked)
    .map(([id]) => Number(id))

  // 知识库文档的 fileId
  const knowledgeFileIds = knowledgePolicyDocs.value
    .filter(doc => selectedDocIds.includes(doc.id))
    .map(doc => doc.fileId)
  // 本单位政策文件的 fileId（直接就是 fileId）
  const policyFileIds = policyFiles.value
    .filter(f => selectedDocIds.includes(f.id))
    .map(f => f.fileId)
  const allFileIds = [...knowledgeFileIds, ...policyFileIds]
  // 允许不选政策文件直接提交检测（政策文件列表可能为空）
  policyModalVisible.value = false

  // 推进阶段到"智能检测"，后端会自动提交检测并携带政策文件ID
  try {
    await projectApi.advancePhase(props.projectId, 5, { policyFileIds: allFileIds })
    ElMessage.success('已提交检测，正在进入智能检测阶段')
    emit('next')
  } catch (e: any) {
    ElMessage.error(e?.message || '提交检测失败')
  }
}

// --- 文档操作 ---
const loadPreview = async () => {
  preview.value = await documentApi.getPreview(props.projectId)
}

/** 带重试的预览加载：AI任务完成后后端同步可能有延迟，最多重试3次 */
const loadPreviewWithRetry = async (retries = 3, delayMs = 2000) => {
  for (let i = 0; i < retries; i++) {
    await new Promise(r => setTimeout(r, delayMs))
    await loadPreview()
    if (preview.value?.integrated) return
  }
}

const handleIntegrate = async () => {
  // 刷新最新任务状态，确保校验是最新的
  await refresh()
  if (!canCreateNew.value) {
    ElMessage.warning('文档集成任务正在处理中，请稍候')
    return
  }
  try {
    const task = await documentApi.integrate(props.projectId)
    setActive(task.id)
    ElMessage.info('文档集成任务已提交，请稍候')
  } catch (e: any) {
    if (e?.code === 9044) {
      ElMessage.warning('文档集成任务正在处理中，请稍候')
      refresh()
      return
    }
    ElMessage.error('提交文档集成失败')
  }
}

const handleExport = async () => {
  if (!preview.value?.generatedFileId) {
    ElMessage.warning('文档尚未生成，无法下载')
    return
  }
  try {
    const blob = await fileApi.download(preview.value.generatedFileId) as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${project.value?.projectName || '招标文件'}.docx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

const handleSaveDraft = () => {
  ElMessage.success('草稿保存成功')
}

const handleSubmitReview = async () => {
  policyModalVisible.value = true
}

onMounted(async () => {
  project.value = await projectApi.getById(props.projectId)
  await Promise.all([loadPreview(), loadKnowledgePolicyDocs(), loadPolicyFiles()])
})
</script>

<style scoped lang="scss">
// ========================================
// 生成状态卡片
// ========================================
.generation-status {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--app-bg-tertiary);
  border-radius: var(--app-radius-sm);
  margin-bottom: 20px;
}

.status-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;

  &.completed {
    background: var(--app-color-success);
  }

  &.spinning {
    background: var(--app-brand-color);
    animation: spin 1s linear infinite;
  }

  &.idle {
    background: var(--app-brand-color);
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.status-info {
  flex: 1;
  min-width: 0;
}

.status-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin-bottom: 4px;
}

.status-desc {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.progress-bar-container {
  margin-top: 12px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: var(--app-bg-elevated);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--app-brand-color), var(--app-auxiliary-color));
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: var(--app-text-secondary);
  margin-top: 4px;
  text-align: right;
}

.generate-btn {
  flex-shrink: 0;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
}

// ========================================
// 文档信息栏
// ========================================
.document-info {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
  padding: 16px;
  background: var(--app-bg-tertiary);
  border-radius: var(--app-radius-sm);
}

.info-item {
  flex: 1;
}

.info-label {
  font-size: 12px;
  color: var(--app-text-tertiary);
  margin-bottom: 4px;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--app-text-primary);
}

// ========================================
// 章节标题
// ========================================
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 20px 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--app-brand-color);
}

// ========================================
// 预览容器
// ========================================
.preview-container {
  border: 1px solid var(--app-border-light);
  border-radius: var(--app-radius-sm);
  background: var(--app-input-bg);
  min-height: 600px;
  position: relative;
  overflow: visible;

  // 工具栏以下区域裁剪
  > .preview-content {
    overflow-y: auto;
  }
}

.preview-toolbar {
  padding: 12px 16px;
  border-bottom: 1px solid var(--app-border-light);
  background: var(--app-bg-tertiary);
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 2;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.toolbar-btn {
  padding: 6px 12px;
  background: var(--app-bg-elevated);
  border: 1px solid var(--app-border-light);
  border-radius: 4px;
  color: var(--app-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
  font-family: inherit;

  &:hover {
    background: var(--app-hover-state);
    color: var(--app-brand-color);
    border-color: var(--app-brand-color);
  }

  &.active {
    background: var(--app-hover-state);
    color: var(--app-brand-color);
    border-color: var(--app-brand-color);
  }

  svg {
    width: 14px;
    height: 14px;
  }
}

.zoom-controls {
  display: flex;
  gap: 4px;
  align-items: center;
}

.zoom-value {
  font-size: 13px;
  color: var(--app-text-secondary);
  min-width: 50px;
  text-align: center;
}

// ========================================
// 目录浮层面板
// ========================================
.toc-panel {
  position: absolute;
  left: 0;
  top: 49px; // toolbar 高度偏移
  width: 260px;
  background: var(--app-card-bg);
  border: 1px solid var(--app-border-light);
  border-radius: 0 8px 8px 0;
  padding: 16px;
  max-height: 500px;
  overflow-y: auto;
  z-index: 10;
  box-shadow: 4px 4px 12px rgba(0, 0, 0, 0.15);
}

.toc-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--app-border-light);
}

.toc-item {
  padding: 8px 12px;
  font-size: 13px;
  color: var(--app-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 4px;

  &:hover {
    background: var(--app-hover-state);
    color: var(--app-brand-color);
  }

  &.level-2 {
    padding-left: 24px;
  }

  &.level-3 {
    padding-left: 36px;
  }
}

.toc-fade-enter-active,
.toc-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.toc-fade-enter-from,
.toc-fade-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

// ========================================
// 空状态
// ========================================
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
  color: var(--app-text-tertiary);

  p {
    font-size: 14px;
    margin: 0;
  }
}

// ========================================
// 底部操作栏
// ========================================
.form-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 0 0 0;
  margin-top: 20px;
  border-top: 1px solid var(--app-border-light);
}

.form-actions-left,
.form-actions-right {
  display: flex;
  gap: 12px;
}

// ========================================
// 按钮样式
// ========================================
.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  text-decoration: none;
  font-family: inherit;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.btn-primary {
  background: var(--app-brand-color);
  color: white;

  &:hover:not(:disabled) {
    background: #2855d9;
  }
}

.btn-secondary {
  background: var(--app-bg-elevated);
  color: var(--app-text-primary);
  border: 1px solid var(--app-border-light);

  &:hover:not(:disabled) {
    background: var(--app-bg-tertiary);
    border-color: var(--app-border-medium);
  }
}

.btn-success {
  background: var(--app-color-success);
  color: white;

  &:hover:not(:disabled) {
    background: #0d9668;
  }
}

// ========================================
// 政策文件弹窗
// ========================================
.policy-modal-content {
  max-height: 500px;
  overflow-y: auto;
}

.policy-group {
  margin-bottom: 20px;
}

.policy-group-label {
  font-size: 14px;
  color: var(--app-text-primary);
  font-weight: 500;

  .highlight {
    color: var(--app-brand-color);
  }
}

.policy-group-hint {
  font-size: 13px;
  color: var(--app-text-secondary);
  margin-top: 8px;
}

.policy-group-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 0 0 12px 0;
}

.policy-file-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.policy-file-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--app-border-light);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--app-hover-state);
    border-color: var(--app-brand-color);
  }
}

.policy-checkbox {
  margin-right: 12px;
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.policy-file-info {
  flex: 1;
}

.policy-file-name {
  font-weight: 500;
  font-size: 14px;
  color: var(--app-text-primary);
  margin-bottom: 4px;
}

.policy-file-meta {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.policy-empty {
  font-size: 13px;
  color: var(--app-text-tertiary);
  text-align: center;
  padding: 12px;
}
</style>
