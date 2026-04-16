<template>
  <div class="phase-document">
    <!-- 卡片容器 -->
    <div class="form-container">
      <!-- 卡片头部 -->
      <div class="form-header">
        <h3 class="form-title">文档集成</h3>
      </div>

      <!-- 卡片内容区 -->
      <div class="form-section">
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
            <button class="btn btn-primary generate-btn" :loading="isIntegrating" @click="handleIntegrate">
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
            <div class="info-value">{{ formatTime(preview.generateTime) }}</div>
          </div>
        </div>

        <!-- 章节标题 -->
        <h4 class="section-title">文档确认</h4>

        <!-- 预览容器 -->
        <div v-if="preview?.integrated" class="preview-container">
          <!-- 预览工具栏 -->
          <div class="preview-toolbar">
            <div class="toolbar-left">
              <button class="toolbar-btn" @click="showTocPanel = !showTocPanel">
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

          <!-- 预览内容区（含目录面板） -->
          <div class="preview-body">
            <!-- 目录面板 -->
            <transition name="toc-slide">
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

            <!-- 预览/编辑区 -->
            <div class="preview-content" :style="{ fontSize: zoomLevel / 100 * 14 + 'px' }">
              <el-tabs v-model="activeTab" class="preview-tabs">
                <el-tab-pane label="预览" name="html">
                  <div class="html-preview" v-html="preview.htmlContent" />
                </el-tab-pane>
                <el-tab-pane label="Markdown编辑" name="markdown">
                  <MarkdownEditor v-model="markdownContent" :preview="false" />
                  <div class="save-edit-bar">
                    <button class="btn btn-primary" @click="handleSaveEdit">保存修改</button>
                  </div>
                </el-tab-pane>
                <el-tab-pane label="变量替换" name="variables">
                  <div class="variable-section">
                    <p class="variable-hint">以下变量将从项目信息中自动填充，您也可以手动修改</p>
                    <div class="variable-grid">
                      <div v-for="v in variables" :key="v.key" class="variable-item">
                        <label class="variable-label">{{ v.label }}</label>
                        <input v-model="v.value" class="variable-input" :placeholder="`请输入${v.label}`" />
                      </div>
                    </div>
                    <button class="btn btn-primary" style="margin-top: 16px" @click="handleApplyVariables">
                      应用变量替换
                    </button>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </div>
          </div>
        </div>

        <!-- 未集成时显示空状态 -->
        <div v-else class="empty-state">
          <el-icon :size="48" color="var(--app-text-tertiary)"><Document /></el-icon>
          <p>请先执行文档集成</p>
          <button class="btn btn-secondary" @click="policyModalVisible = true">选择政策文件</button>
        </div>
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
        <div class="form-actions-right">
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
    </div>

    <!-- 政策文件选择弹窗 -->
    <el-dialog v-model="policyModalVisible" title="政策文件匹配" width="560px" destroy-on-close>
      <div class="policy-modal-content">
        <div class="policy-group">
          <div class="policy-group-label">项目类别：<span class="highlight">{{ project?.projectCategory || '-' }}</span></div>
          <p class="policy-group-hint">系统根据项目类别匹配到以下政策文件，请选择需要应用的文件。</p>
        </div>
        <div class="policy-group">
          <h4 class="policy-group-title">匹配的政策文件</h4>
          <div class="policy-file-list">
            <label
              v-for="file in aiRecommendedFiles"
              :key="file.id"
              class="policy-file-item"
            >
              <input v-model="selectedPolicyMap[file.id]" type="checkbox" class="policy-checkbox" />
              <div class="policy-file-info">
                <div class="policy-file-name">{{ file.fileName }}</div>
                <div class="policy-file-meta">政策文件 - {{ file.fileType || 'PDF' }}</div>
              </div>
            </label>
            <div v-if="!aiRecommendedFiles.length" class="policy-empty">暂无推荐</div>
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
import { documentApi } from '@/api/document'
import { policyFileApi } from '@/api/policy-file'
import { aiApi } from '@/api/ai'
import { projectApi } from '@/api/project'
import { toWanYuan } from '@/utils/budget'
import type { DocumentPreviewVO } from '@/types/document'
import type { PolicyFileVO } from '@/types/policy-file'
import type { ProjectInfo } from '@/types/project'
import MarkdownEditor from '@/components/editor/MarkdownEditor.vue'

const props = defineProps<{ projectId: number }>()
const emit = defineEmits<{ next: []; prev: [] }>()

const preview = ref<DocumentPreviewVO | null>(null)
const markdownContent = ref('')
const activeTab = ref('html')
const isIntegrating = ref(false)
const showTocPanel = ref(true)
const zoomLevel = ref(100)
const policyModalVisible = ref(false)

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
const policyFiles = ref<PolicyFileVO[]>([])
const selectedPolicyMap = reactive<Record<number, boolean>>({})
const aiRecommendedIds = ref<number[]>([])
const suggestingPolicy = ref(false)
const project = ref<ProjectInfo | null>(null)

const aiRecommendedFiles = computed(() => policyFiles.value.filter(f => aiRecommendedIds.value.includes(f.id)))
const otherPolicyFiles = computed(() => policyFiles.value.filter(f => !aiRecommendedIds.value.includes(f.id)))

const loadPolicyFiles = async () => {
  try {
    policyFiles.value = await policyFileApi.getAllAvailable(project.value?.projectCategory)
    for (const id of aiRecommendedIds.value) {
      selectedPolicyMap[id] = true
    }
  } catch {
    ElMessage.error('获取政策文件列表失败')
  }
}

const handleSuggestPolicy = async () => {
  if (!project.value) {
    ElMessage.warning('项目信息加载中，请稍候')
    return
  }
  suggestingPolicy.value = true
  try {
    const res = await aiApi.suggest({
      content: project.value.projectDescription || project.value.projectName,
      type: 'policy',
      projectId: props.projectId,
    })
    if (res.suggestions?.length) {
      const suggestedIds = res.suggestions
        .map(s => { const num = Number(s); return isNaN(num) ? null : num })
        .filter((id): id is number => id !== null)
      aiRecommendedIds.value = suggestedIds
      for (const id of suggestedIds) {
        selectedPolicyMap[id] = true
      }
      ElMessage.success(`AI推荐了 ${suggestedIds.length} 个政策文件`)
    } else {
      ElMessage.info('暂无推荐政策文件')
    }
  } catch {
    ElMessage.error('AI推荐失败')
  } finally {
    suggestingPolicy.value = false
  }
}

const handleConfirmPolicyFiles = () => {
  const selectedIds = Object.entries(selectedPolicyMap)
    .filter(([, checked]) => checked)
    .map(([id]) => Number(id))
  if (!selectedIds.length) {
    ElMessage.warning('请至少选择一个政策文件')
    return
  }
  policyModalVisible.value = false
  ElMessage.success(`已选择 ${selectedIds.length} 个政策文件`)
}

// --- 变量替换 ---
interface VariableItem { key: string; label: string; value: string }
const variables = ref<VariableItem[]>([])

const initVariables = () => {
  if (!project.value) return
  variables.value = [
    { key: 'projectName', label: '项目名称', value: project.value.projectName || '' },
    { key: 'projectCategory', label: '项目类别', value: project.value.projectCategory || '' },
    { key: 'projectType', label: '项目类型', value: project.value.projectType || '' },
    { key: 'budget', label: '预算金额(万元)', value: project.value.budget ? String(toWanYuan(project.value.budget)) : '' },
    { key: 'tenderUnit', label: '招标单位', value: project.value.tenderUnit || '' },
    { key: 'contactPerson', label: '联系人', value: project.value.contactPerson || '' },
    { key: 'contactPhone', label: '联系电话', value: project.value.contactPhone || '' },
    { key: 'projectDescription', label: '项目描述', value: project.value.projectDescription || '' },
  ]
}

const handleApplyVariables = () => {
  let result = markdownContent.value
  for (const v of variables.value) {
    const regex = new RegExp(`\\{\\{${v.key}\\}\\}`, 'g')
    result = result.replace(regex, v.value)
  }
  markdownContent.value = result
  ElMessage.success('变量替换完成，请检查内容后保存')
  activeTab.value = 'markdown'
}

// --- 文档操作 ---
const loadPreview = async () => {
  preview.value = await documentApi.getPreview(props.projectId)
  if (preview.value?.markdownContent) {
    markdownContent.value = preview.value.markdownContent
  }
}

const handleIntegrate = async () => {
  isIntegrating.value = true
  try {
    preview.value = await documentApi.integrate(props.projectId)
    if (preview.value?.markdownContent) {
      markdownContent.value = preview.value.markdownContent
    }
    ElMessage.success('文档集成完成')
  } catch {
    ElMessage.error('文档集成失败')
  } finally {
    isIntegrating.value = false
  }
}

const handleExport = async () => {
  try {
    const blob = await documentApi.exportWord(props.projectId) as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '招标文件.docx'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

const handleSaveEdit = async () => {
  await documentApi.editContent(props.projectId, markdownContent.value)
  ElMessage.success('保存成功')
  await loadPreview()
  activeTab.value = 'html'
}

const handleSaveDraft = () => {
  ElMessage.success('草稿保存成功')
}

const handleSubmitReview = () => {
  policyModalVisible.value = true
}

onMounted(async () => {
  project.value = await projectApi.getById(props.projectId)
  initVariables()
  await Promise.all([loadPreview(), loadPolicyFiles(), handleSuggestPolicy()])
})
</script>

<style scoped lang="scss">
// ========================================
// 卡片容器
// ========================================
.form-container {
  background: var(--app-card-bg);
  border-radius: var(--app-radius-sm);
  border: 1px solid var(--app-border-light);
  overflow: hidden;
}

.form-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--app-border-light);
  background: var(--app-bg-tertiary);
}

.form-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 0;
}

.form-section {
  padding: 24px;
}

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
  margin: 0 0 20px 0;
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
  overflow: hidden;
}

.preview-toolbar {
  padding: 12px 16px;
  border-bottom: 1px solid var(--app-border-light);
  background: var(--app-bg-tertiary);
  display: flex;
  justify-content: space-between;
  align-items: center;
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
// 预览主体
// ========================================
.preview-body {
  display: flex;
  min-height: 500px;
}

.toc-panel {
  width: 240px;
  flex-shrink: 0;
  background: var(--app-bg-secondary);
  border-right: 1px solid var(--app-border-light);
  padding: 16px;
  overflow-y: auto;
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
}

.toc-slide-enter-active,
.toc-slide-leave-active {
  transition: all 0.3s ease;
}

.toc-slide-enter-from,
.toc-slide-leave-to {
  width: 0;
  opacity: 0;
  padding: 0;
}

.preview-content {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
}

.preview-tabs {
  padding: 0 16px;
}

.html-preview {
  padding: 40px;
  background: white;
  color: #1a1a1a;
  min-height: 400px;
  line-height: 1.8;
}

.save-edit-bar {
  padding: 16px 0;
  display: flex;
  justify-content: flex-end;
}

// ========================================
// 变量替换
// ========================================
.variable-section {
  padding: 16px;
}

.variable-hint {
  color: var(--app-text-tertiary);
  font-size: 13px;
  margin-bottom: 16px;
}

.variable-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.variable-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.variable-label {
  font-size: 13px;
  color: var(--app-text-secondary);
  font-weight: 500;
}

.variable-input {
  padding: 8px 12px;
  background: var(--app-input-bg);
  border: 1px solid var(--app-border-light);
  border-radius: 6px;
  color: var(--app-text-primary);
  font-size: 14px;
  font-family: inherit;
  outline: none;

  &:focus {
    border-color: var(--app-brand-color);
    box-shadow: 0 0 0 2px rgba(51, 108, 255, 0.1);
  }
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
  padding: 24px;
  background: var(--app-bg-tertiary);
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
