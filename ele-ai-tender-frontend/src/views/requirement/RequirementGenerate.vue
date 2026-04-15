<template>
  <div class="requirement-generate">
    <!-- 顶部工具栏 -->
    <div class="gen-toolbar">
      <div class="toolbar-left">
        <el-button :icon="ArrowLeft" @click="router.back()">返回列表</el-button>
        <el-divider direction="vertical" />
        <span class="toolbar-title">业务需求生成</span>
      </div>
      <div class="toolbar-right">
        <el-button :loading="saving" @click="handleSave">保存</el-button>
        <el-button :type="editMode ? 'default' : 'primary'" @click="editMode = !editMode">
          {{ editMode ? '切换预览' : '编辑' }}
        </el-button>
        <el-button :loading="exporting" @click="handleExport">导出</el-button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="gen-body">
      <div class="gen-main">
        <!-- 生成进度 -->
        <div class="progress-section">
          <div class="progress-header">
            <span class="progress-title">生成进度</span>
            <el-tag v-if="generating" type="" size="small" class="is-pulse">
              {{ isActive ? '任务排队中' : '生成中' }}
            </el-tag>
            <el-tag v-else-if="content" type="success" size="small">已完成</el-tag>
            <el-tag v-else type="info" size="small">未开始</el-tag>
          </div>
          <el-progress
            :percentage="progressPercent"
            :stroke-width="8"
            :status="generating ? '' : content ? 'success' : ''"
          />
          <span class="progress-text">{{ progressPercent }}% 完成</span>
        </div>

        <!-- 项目基本信息 -->
        <el-card class="content-card" shadow="never">
          <template #header>
            <span class="card-title">项目基本信息</span>
          </template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="需求名称">{{ requirementName }}</el-descriptions-item>
            <el-descriptions-item label="项目类型">
              <StatusBadge v-if="requirementData.projectType" :status="requirementData.projectType" :type-map="PROJECT_TYPE_MAP" />
            </el-descriptions-item>
            <el-descriptions-item label="项目预算">{{ formatBudget(requirementData.budget) }}</el-descriptions-item>
            <el-descriptions-item label="需求类型">
              {{ REQUIREMENT_TYPE_MAP[requirementData.requirementType || 'NEW']?.label || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="需求描述" :span="2">
              {{ requirementData.requirementDescription || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 参考文件 -->
        <el-card v-if="referenceFiles.length > 0" class="content-card" shadow="never">
          <template #header>
            <span class="card-title">参考文件</span>
          </template>
          <div class="ref-file-list">
            <div v-for="file in referenceFiles" :key="file.id" class="ref-file-item">
              <el-icon><Document /></el-icon>
              <span class="ref-file-name">{{ file.fileName }}</span>
              <el-tag size="small">{{ file.fileType }}</el-tag>
              <span class="ref-file-budget">{{ formatBudget(file.budget) }}</span>
              <span class="ref-file-time">{{ file.uploadTime }}</span>
              <el-button size="small" link type="primary">预览</el-button>
            </div>
          </div>
        </el-card>

        <!-- 业务需求详情 -->
        <el-card class="content-card" shadow="never">
          <template #header>
            <div class="card-title-row">
              <span class="card-title">业务需求详情</span>
              <el-button
                v-if="!generating && !content"
                type="success"
                @click="handleGenerate"
              >
                AI生成
              </el-button>
              <el-button
                v-if="isActive && !content"
                type="warning"
                loading
                disabled
              >
                AI生成中...
              </el-button>
              <el-button
                v-if="sseGenerating"
                type="danger"
                @click="stopGenerate"
              >
                停止生成
              </el-button>
            </div>
          </template>

          <div class="content-area">
            <MarkdownEditor v-if="editMode" v-model="content" class="content-editor" />
            <MdPreview v-else :model-value="content" class="content-preview" />
          </div>

          <!-- AI反馈 -->
          <div v-if="content && !generating" class="ai-feedback">
            <span class="feedback-label">帮助我们改进AI生成质量</span>
            <div class="feedback-buttons">
              <el-button
                :type="feedbackType === 'like' ? 'success' : 'default'"
                size="small"
                @click="handleFeedback('like')"
              >
                赞
              </el-button>
              <el-button
                :type="feedbackType === 'dislike' ? 'danger' : 'default'"
                size="small"
                @click="handleFeedback('dislike')"
              >
                不行
              </el-button>
            </div>
          </div>
        </el-card>

        <!-- 关键标签 -->
        <el-card v-if="tags.length > 0" class="content-card" shadow="never">
          <template #header>
            <span class="card-title">关键标签</span>
          </template>
          <div class="tags-area">
            <el-tag
              v-for="tag in tags"
              :key="tag.text"
              :type="tag.type"
              size="default"
              class="tag-item"
            >
              {{ tag.text }}
            </el-tag>
          </div>
        </el-card>

        <!-- 底部导航 -->
        <div class="bottom-nav">
          <el-button @click="router.back()">返回修改</el-button>
          <div style="flex: 1" />
          <el-button type="primary" @click="handleNextStep">
            下一步：智能检测
          </el-button>
        </div>
      </div>
    </div>

    <!-- AI助手浮动面板 -->
    <transition name="slide-float">
      <div v-if="chatVisible" class="ai-assistant-panel">
        <div class="panel-header">
          <span class="panel-title">AI助手</span>
          <el-button :icon="Close" size="small" link @click="chatVisible = false" />
        </div>
        <div class="panel-warning">
          AI助手接入互联网，若有涉密信息请勿发送
        </div>
        <div class="panel-body">
          <AiChatPanel
            :context="content"
            :requirement-id="requirementId"
            v-model:messages="chatMessages"
            @close="chatVisible = false"
            @feedback="handleChatFeedback"
            @message="handleAiMessage"
          />
        </div>
      </div>
    </transition>

    <!-- AI助手浮动按钮 -->
    <transition name="fade">
      <el-button
        v-if="!chatVisible"
        class="chat-fab"
        type="primary"
        :icon="ChatDotRound"
        circle
        @click="chatVisible = true"
      />
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ChatDotRound, Close, Document } from '@element-plus/icons-vue'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { requirementApi } from '@/api/requirement'
import { createSSEConnection } from '@/api/ai'
import { useActiveTask } from '@/composables/useActiveTask'
import MarkdownEditor from '@/components/editor/MarkdownEditor.vue'
import AiChatPanel from '@/components/ai/AiChatPanel.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { PROJECT_TYPE_MAP } from '@/constants/status-maps'
import { REQUIREMENT_TYPE_MAP } from '@/types/requirement'
import { formatBudgetWanYuan } from '@/utils/budget'
import type { RequirementInfo, MatchFile } from '@/types/requirement'
import type { AiChatMessage } from '@/types/ai'

const router = useRouter()
const route = useRoute()

// ---- 基础数据 ----
const requirementId = ref(0)
const requirementName = ref('')
const requirementData = ref<Partial<RequirementInfo>>({})
const content = ref('')
const referenceFiles = ref<MatchFile[]>([])
const tags = ref<Array<{ text: string; type: '' | 'success' | 'warning' | 'info' | 'danger' }>>([])

// ---- 状态 ----
const saving = ref(false)
const exporting = ref(false)
const editMode = ref(false)
const chatVisible = ref(false)
const chatMessages = ref<AiChatMessage[]>([])
const feedbackType = ref<'like' | 'dislike' | null>(null)
const progressPercent = ref(0)

// ---- 活跃任务检测 ----
const { isActive, activeTask, checkActiveTask } = useActiveTask(
  'REQUIREMENT_GENERATE',
  requirementId,
  'REQUIREMENT',
)

// generating = SSE生成中 或 异步任务活跃中
const generating = computed(() => isActive.value || sseGenerating.value)

const sseGenerating = ref(false)

// ---- SSE ----
let closeGenerateSSE: (() => void) | null = null

// ---- 初始化 ----
onMounted(async () => {
  const id = Number(route.params.id)
  if (!id || isNaN(id)) {
    ElMessage.error('参数错误')
    router.back()
    return
  }
  requirementId.value = id

  try {
    const data = await requirementApi.getById(id)
    requirementName.value = data.requirementName || ''
    requirementData.value = data
    content.value = data.content || ''

    if (content.value) {
      progressPercent.value = 100
    }

    // 检查是否有活跃的AI生成任务
    await checkActiveTask()
    if (isActive.value && !content.value) {
      progressPercent.value = activeTask.value?.status === 'PROCESSING' ? 65 : 10
    }

    // 加载参考文件
    try {
      referenceFiles.value = await requirementApi.getMatchFiles({ requirementId: id }) || []
    } catch {
      referenceFiles.value = []
    }

    // 生成标签（基于项目信息）
    generateTags(data)
  } catch {
    ElMessage.error('加载需求失败')
    router.back()
  }
})

onBeforeUnmount(() => {
  closeGenerateSSE?.()
})

function generateTags(data: RequirementInfo) {
  const result: Array<{ text: string; type: '' | 'success' | 'warning' | 'info' | 'danger' }> = []
  const typeMap: Record<string, string> = { ENGINEERING: '工程', GOODS: '货物', SERVICE: '服务' }
  if (data.projectType && typeMap[data.projectType]) {
    result.push({ text: typeMap[data.projectType] + '类', type: 'success' })
  }
  if (data.budget) {
    result.push({ text: formatBudgetWanYuan(data.budget), type: 'warning' })
  }
  if (data.requirementName) {
    const keywords = data.requirementName.replace(/业务需求$/, '').trim()
    if (keywords) {
      result.push({ text: keywords, type: '' })
    }
  }
  tags.value = result
}

// ---- AI生成 ----
async function handleGenerate() {
  if (generating.value) return

  // 先检查是否有活跃任务
  await checkActiveTask()
  if (isActive.value) {
    ElMessage.warning('AI生成任务正在处理中，请稍候')
    return
  }

  sseGenerating.value = true
  editMode.value = false
  content.value = ''
  progressPercent.value = 0

  // 模拟进度
  const progressTimer = setInterval(() => {
    if (progressPercent.value < 90) {
      progressPercent.value += Math.floor(Math.random() * 5) + 1
    }
  }, 300)

  closeGenerateSSE = createSSEConnection(
    `/core-api/v1/requirements/${requirementId.value}/generate`,
    {},
    (data: string) => {
      content.value += data
    },
    () => {
      clearInterval(progressTimer)
      ElMessage.error('AI生成失败，请稍后重试')
      sseGenerating.value = false
      closeGenerateSSE = null
    },
    () => {
      clearInterval(progressTimer)
      progressPercent.value = 100
      ElMessage.success('AI生成完成')
      sseGenerating.value = false
      closeGenerateSSE = null
    },
  )
}

function stopGenerate() {
  closeGenerateSSE?.()
  sseGenerating.value = false
  closeGenerateSSE = null
}

// ---- 保存 ----
async function handleSave() {
  saving.value = true
  try {
    await requirementApi.update(requirementId.value, { content: content.value })
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// ---- 导出 ----
async function handleExport() {
  exporting.value = true
  try {
    const blob = await requirementApi.exportDocument(requirementId.value)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${requirementName.value}.docx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

// ---- 下一步 ----
function handleNextStep() {
  if (!content.value) {
    ElMessage.warning('请先生成需求内容')
    return
  }
  router.push(`/requirement/detect/${requirementId.value}`)
}

// ---- AI反馈 ----
function handleFeedback(type: 'like' | 'dislike') {
  if (type === 'dislike') {
    const reason = prompt('请说明不满意的原因，帮助我们改进：')
    if (!reason) return
  }
  feedbackType.value = type
  ElMessage.success(type === 'like' ? '感谢您的反馈' : '我们会持续改进')
}

// ---- AI对话回调 ----
function handleChatFeedback(type: 'like' | 'dislike', _index: number) {
  ElMessage.success(type === 'like' ? '感谢您的反馈' : '我们会持续改进')
}

function handleAiMessage(msg: string) {
  if (!msg.trim()) return
  content.value += '\n\n' + msg
  ElMessage.info('AI建议已追加到内容末尾')
}

function formatBudget(yuan?: number): string {
  return formatBudgetWanYuan(yuan)
}
</script>

<style scoped>
.requirement-generate {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--app-bg-secondary);
  transition: var(--app-transition-base);
}

/* ---- 顶部工具栏 ---- */
.gen-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: var(--app-bg-elevated);
  border-bottom: 1px solid var(--app-border-medium);
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-primary);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ---- 主内容区 ---- */
.gen-body {
  flex: 1;
  overflow: auto;
  padding: 20px;
}

.gen-main {
  max-width: 960px;
  margin: 0 auto;
}

/* ---- 生成进度 ---- */
.progress-section {
  background: var(--app-bg-elevated);
  border: 1px solid var(--app-border-medium);
  border-radius: var(--app-radius-sm);
  padding: 16px 20px;
  margin-bottom: 16px;
}

.progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.progress-title {
  font-weight: 600;
  color: var(--app-text-primary);
}

.progress-text {
  font-size: 12px;
  color: var(--app-text-secondary);
  margin-top: 4px;
}

.is-pulse {
  animation: pulse-anim 2s ease-in-out infinite;
}

@keyframes pulse-anim {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ---- 内容卡片 ---- */
.content-card {
  margin-bottom: 16px;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--app-text-primary);
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* ---- 参考文件 ---- */
.ref-file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ref-file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--app-bg-secondary);
  border-radius: var(--app-radius-sm);
}

.ref-file-name {
  flex: 1;
  font-size: 14px;
  color: var(--app-text-primary);
}

.ref-file-budget,
.ref-file-time {
  font-size: 13px;
  color: var(--app-text-secondary);
}

/* ---- 内容区域 ---- */
.content-area {
  min-height: 300px;
}

.content-preview {
  padding: 16px;
}

.content-editor {
  min-height: 300px;
}

/* ---- AI反馈 ---- */
.ai-feedback {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--app-border-light);
}

.feedback-label {
  font-size: 13px;
  color: var(--app-text-tertiary);
}

.feedback-buttons {
  display: flex;
  gap: 8px;
}

/* ---- 标签区域 ---- */
.tags-area {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  font-size: 13px;
}

/* ---- 底部导航 ---- */
.bottom-nav {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  background: var(--app-bg-tertiary);
  border-top: 1px solid var(--app-border-light);
  border-radius: 0 0 var(--app-radius-sm, 8px) var(--app-radius-sm, 8px);
  margin: 16px -20px -20px;
}

/* ---- AI助手浮动面板 ---- */
.ai-assistant-panel {
  position: fixed;
  right: 24px;
  top: 80px;
  width: 340px;
  height: 600px;
  background: var(--app-bg-elevated);
  border: 1px solid var(--app-border-medium);
  border-radius: var(--app-radius-sm);
  box-shadow: var(--app-shadow-lg);
  display: flex;
  flex-direction: column;
  z-index: 100;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--app-border-light);
}

.panel-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--app-text-primary);
}

.panel-warning {
  padding: 8px 16px;
  font-size: 12px;
  color: var(--app-color-warning);
  background: var(--app-color-warning-light, rgba(230, 162, 60, 0.1));
}

.panel-body {
  flex: 1;
  overflow: hidden;
}

/* ---- 浮动面板动画 ---- */
.slide-float-enter-active,
.slide-float-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.slide-float-enter-from,
.slide-float-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

/* ---- AI助手浮动按钮 ---- */
.chat-fab {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 48px;
  height: 48px;
  box-shadow: var(--app-shadow-md);
  z-index: 100;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
