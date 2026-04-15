<template>
  <div class="phase-document">
    <!-- 生成状态卡片 -->
    <div v-if="preview?.integrated" class="status-card success">
      <el-icon size="28" color="var(--app-color-success)"><CircleCheck /></el-icon>
      <div class="status-info">
        <h4>文档集成完成</h4>
        <p>招标文件已成功集成，可预览和导出</p>
      </div>
    </div>
    <div v-else class="status-card pending">
      <el-icon size="28" color="var(--app-text-tertiary)"><Document /></el-icon>
      <div class="status-info">
        <h4>待执行文档集成</h4>
        <p>请先选择政策文件，然后执行文档集成</p>
      </div>
    </div>

    <!-- 文档信息栏 -->
    <div v-if="preview?.integrated" class="doc-meta-bar">
      <div class="meta-item">
        <span class="meta-label">文档名称</span>
        <span class="meta-value">{{ project?.projectName || '招标文件' }}.docx</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">项目</span>
        <span class="meta-value">{{ preview.projectName || '-' }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">集成状态</span>
        <span class="meta-value">
          <el-tag type="success" size="small">已完成</el-tag>
        </span>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="doc-toolbar">
      <el-button type="primary" :loading="isIntegrating" @click="handleIntegrate">
        执行文档集成
      </el-button>
      <el-button @click="policyModalVisible = true">选择政策文件</el-button>
      <el-button v-if="preview?.integrated" :icon="Download" @click="handleExport">
        导出Word
      </el-button>
      <el-button v-if="preview?.integrated" :icon="Printer" @click="handlePrint">
        打印
      </el-button>
    </div>

    <!-- 文档预览区 -->
    <div v-if="preview?.integrated" class="doc-preview-area">
      <!-- 目录面板 -->
      <transition name="toc-slide">
        <div v-if="showTocPanel" class="toc-panel">
          <div class="toc-header">
            <h4>目录</h4>
            <el-button text size="small" @click="showTocPanel = false">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <el-tree
            :data="tocData"
            :props="{ children: 'children', label: 'title' }"
            default-expand-all
            :highlight-current="true"
            @node-click="handleTocClick"
            class="toc-tree"
          />
        </div>
      </transition>

      <!-- 预览主区 -->
      <div class="preview-main">
        <div class="preview-toolbar">
          <el-button text @click="showTocPanel = !showTocPanel">
            <el-icon><List /></el-icon> 目录
          </el-button>
          <div class="zoom-controls">
            <el-button text @click="zoomOut"><el-icon><ZoomOut /></el-icon></el-button>
            <span class="zoom-level">{{ zoomLevel }}%</span>
            <el-button text @click="zoomIn"><el-icon><ZoomIn /></el-icon></el-button>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="preview-tabs">
          <el-tab-pane label="预览" name="html">
            <div
              class="html-preview"
              :style="{ fontSize: zoomLevel / 100 * 14 + 'px' }"
              v-html="preview.htmlContent"
            />
          </el-tab-pane>
          <el-tab-pane label="Markdown编辑" name="markdown">
            <MarkdownEditor v-model="markdownContent" />
            <el-button type="primary" class="save-btn" @click="handleSaveEdit">保存修改</el-button>
          </el-tab-pane>
          <el-tab-pane label="变量替换" name="variables">
            <div class="variable-section">
              <p class="variable-hint">以下变量将从项目信息中自动填充，您也可以手动修改</p>
              <el-form label-width="140px" class="variable-form">
                <el-form-item v-for="v in variables" :key="v.key" :label="v.label">
                  <el-input v-model="v.value" :placeholder="`请输入${v.label}`" />
                </el-form-item>
              </el-form>
              <el-button type="primary" @click="handleApplyVariables">应用变量替换</el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <el-empty v-else description="请先执行文档集成" />

    <!-- 政策文件选择模态框 -->
    <el-dialog v-model="policyModalVisible" title="选择政策文件" width="640px" destroy-on-close>
      <div class="policy-modal-content">
        <!-- AI推荐的政策文件 -->
        <div class="policy-group">
          <h4>匹配的政策文件</h4>
          <div v-if="policyFiles.length" class="policy-checkbox-list">
            <el-checkbox
              v-for="file in policyFiles.filter(f => isAiRecommended(f.id))"
              :key="file.id"
              v-model="selectedPolicyMap[file.id]"
              :label="file.fileName"
              border
              class="policy-checkbox-item"
            />
          </div>
          <el-empty v-else description="暂无推荐" :image-size="40" />
        </div>

        <!-- 本单位政策文件 -->
        <div class="policy-group">
          <h4>本单位政策文件</h4>
          <div v-if="policyFiles.filter(f => !isAiRecommended(f.id)).length" class="policy-checkbox-list">
            <el-checkbox
              v-for="file in policyFiles.filter(f => !isAiRecommended(f.id))"
              :key="file.id"
              v-model="selectedPolicyMap[file.id]"
              :label="file.fileName"
              border
              class="policy-checkbox-item"
            />
          </div>
          <el-empty v-else description="暂无政策文件" :image-size="40" />
        </div>
      </div>
      <template #footer>
        <el-button @click="policyModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmPolicyFiles">确认并检测</el-button>
      </template>
    </el-dialog>

    <!-- 底部操作 -->
    <div class="phase-actions">
      <el-button @click="$emit('prev')">上一步</el-button>
      <div style="flex: 1" />
      <el-button type="primary" :disabled="!preview?.integrated" @click="$emit('next')">
        提交检测
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  CircleCheck,
  Document,
  Download,
  Printer,
  Close,
  List,
  ZoomOut,
  ZoomIn,
} from '@element-plus/icons-vue'
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
defineEmits<{ next: []; prev: [] }>()

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
  // 滚动到对应章节（简单实现）
  const el = document.getElementById(data.id)
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

const zoomIn = () => { zoomLevel.value = Math.min(200, zoomLevel.value + 10) }
const zoomOut = () => { zoomLevel.value = Math.max(50, zoomLevel.value - 10) }
const handlePrint = () => window.print()

// --- 政策文件匹配 ---
const policyFiles = ref<PolicyFileVO[]>([])
const selectedPolicyMap = reactive<Record<number, boolean>>({})
const aiRecommendedIds = ref<number[]>([])
const suggestingPolicy = ref(false)
const project = ref<ProjectInfo | null>(null)

const loadPolicyFiles = async () => {
  try {
    policyFiles.value = await policyFileApi.getAllAvailable(project.value?.projectCategory)
    // 初始化选择状态：AI推荐的默认选中
    for (const id of aiRecommendedIds.value) {
      selectedPolicyMap[id] = true
    }
  } catch {
    ElMessage.error('获取政策文件列表失败')
  }
}

const isAiRecommended = (id: number) => aiRecommendedIds.value.includes(id)

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
      // 预选中AI推荐
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

onMounted(async () => {
  project.value = await projectApi.getById(props.projectId)
  initVariables()
  await Promise.all([loadPreview(), loadPolicyFiles(), handleSuggestPolicy()])
})
</script>

<style scoped lang="scss">
// 状态卡片
.status-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-radius: var(--app-radius-sm);
  margin-bottom: 16px;

  &.success {
    background: var(--app-color-success-light);
    border: 1px solid var(--app-color-success);
  }

  &.pending {
    background: var(--app-bg-secondary);
    border: 1px solid var(--app-border-light);
  }
}

.status-info {
  h4 { margin: 0 0 4px; color: var(--app-text-primary); }
  p { margin: 0; font-size: 13px; color: var(--app-text-secondary); }
}

// 文档信息栏
.doc-meta-bar {
  display: flex;
  gap: 24px;
  padding: 12px 16px;
  background: var(--app-bg-secondary);
  border-radius: 6px;
  margin-bottom: 16px;
}

.meta-item {
  .meta-label { font-size: 12px; color: var(--app-text-tertiary); margin-right: 8px; }
  .meta-value { font-size: 14px; color: var(--app-text-primary); font-weight: 500; }
}

// 操作工具栏
.doc-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

// 文档预览区
.doc-preview-area {
  display: flex;
  gap: 0;
  border: 1px solid var(--app-border-light);
  border-radius: var(--app-radius-sm);
  overflow: hidden;
  min-height: 500px;
}

.toc-panel {
  width: 240px;
  flex-shrink: 0;
  background: var(--app-bg-secondary);
  border-right: 1px solid var(--app-border-light);
  overflow-y: auto;
}

.toc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--app-border-light);

  h4 { margin: 0; font-size: 14px; color: var(--app-text-primary); }
}

.toc-tree {
  background: transparent;
  padding: 8px;
}

.toc-slide-enter-active,
.toc-slide-leave-active {
  transition: all 0.3s ease;
}

.toc-slide-enter-from,
.toc-slide-leave-to {
  width: 0;
  opacity: 0;
}

.preview-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.preview-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid var(--app-border-light);
  background: var(--app-bg-secondary);
}

.zoom-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.zoom-level {
  font-size: 13px;
  color: var(--app-text-secondary);
  min-width: 40px;
  text-align: center;
}

.preview-tabs {
  flex: 1;
  padding: 0 16px;
}

.html-preview {
  padding: 20px;
  min-height: 400px;
  background: var(--app-bg-elevated);
}

.save-btn {
  margin-top: 12px;
}

.variable-section {
  padding: 16px;
  border: 1px solid var(--app-border-light);
  border-radius: 6px;
}

.variable-hint {
  color: var(--app-text-tertiary);
  font-size: 13px;
  margin-bottom: 16px;
}

.variable-form {
  max-width: 600px;
}

// 政策文件模态框
.policy-modal-content {
  max-height: 400px;
  overflow-y: auto;
}

.policy-group {
  margin-bottom: 20px;

  h4 {
    margin: 0 0 8px;
    font-size: 14px;
    color: var(--app-text-primary);
  }
}

.policy-checkbox-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.policy-checkbox-item {
  margin: 0;
  width: 100%;
}

// 底部操作
.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
