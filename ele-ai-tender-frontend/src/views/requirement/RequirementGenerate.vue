<template>
  <div class="requirement-generate">
    <!-- 顶部工具栏 -->
    <div class="gen-toolbar">
      <div class="toolbar-left">
        <el-button :icon="ArrowLeft" @click="router.push('/requirement')">返回列表</el-button>
        <el-divider direction="vertical" />
        <span class="toolbar-title">业务需求生成</span>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="gen-body">
      <div class="gen-main">
        <!-- 生成进度 -->
        <div class="progress-container">
          <div class="progress-header">
            <span class="progress-title">生成进度</span>
            <el-tag v-if="sseGenerating" type="" size="small" class="is-pulse">生成中</el-tag>
            <el-tag v-else-if="latestTask && !canCreateNew" type="" size="small" class="is-pulse">
              {{ latestTask.status === 'PENDING' ? '任务排队中' : '处理中' }}
            </el-tag>
            <el-tag v-else-if="latestTask?.status === 'COMPLETED' || content" type="success" size="small">已完成</el-tag>
            <el-tag v-else-if="latestTask?.status === 'FAILED'" type="danger" size="small">失败</el-tag>
            <el-tag v-else-if="latestTask?.status === 'AI_UNAVAILABLE'" type="warning" size="small">服务不可用</el-tag>
            <el-tag v-else type="info" size="small">未开始</el-tag>
          </div>
          <el-progress
            :percentage="progressPercent"
            :stroke-width="8"
            :status="progressStatus"
          />
          <span class="progress-text">{{ progressPercent }}% 完成</span>
        </div>

        <!-- 内容容器 -->
        <div class="content-container">
          <!-- 内容头部 -->
          <div class="content-header">
            <h3>业务需求内容</h3>
            <div class="content-actions">
              <el-tooltip v-if="!isRequirementCompleted" content="保存" placement="top">
                <button class="primary-action-btn" :disabled="saving" @click="handleSave">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
                    <polyline points="17 21 17 13 7 13 7 21" />
                    <polyline points="7 3 7 8 15 8" />
                  </svg>
                </button>
              </el-tooltip>
              <el-tooltip content="导出" placement="top">
                <button class="primary-action-btn" :disabled="exporting" @click="handleExport">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                    <polyline points="7 10 12 15 17 10" />
                    <line x1="12" y1="15" x2="12" y2="3" />
                  </svg>
                </button>
              </el-tooltip>
            </div>
          </div>

          <!-- 内容主体 -->
          <div class="content-body">
            <!-- 项目基本信息 -->
            <div class="content-section">
              <h4 class="section-title">项目基本信息</h4>
              <div class="section-content">
                <p><strong>需求名称：</strong>{{ requirementName }}</p>
                <p><strong>项目类型：</strong>
                  <StatusBadge v-if="requirementData.projectType" :status="requirementData.projectType" :type-map="PROJECT_TYPE_MAP" />
                  <span v-else>-</span>
                </p>
                <p><strong>项目预算：</strong>{{ formatBudget(requirementData.budget) }}</p>
                <p><strong>需求类型：</strong>{{ REQUIREMENT_TYPE_MAP[requirementData.requirementType || 'NEW']?.label || '-' }}</p>
                <p><strong>需求描述：</strong>{{ requirementData.requirementDescription || '-' }}</p>
              </div>
            </div>

            <!-- 参考文件 -->
            <div v-if="referenceFiles.length > 0" class="content-section">
              <h4 class="section-title">参考文件</h4>
              <div class="reference-files">
                <div v-for="file in referenceFiles" :key="file.id" class="file-item">
                  <div class="file-info">
                    <div class="file-name">{{ file.fileName }}</div>
                    <div class="file-meta">项目类型：{{ file.fileType }} | 预算：{{ formatBudget(file.budget) }} | 完成时间：{{ file.uploadTime }}</div>
                  </div>
                  <el-button size="small" link type="primary" class="preview-btn">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                    预览
                  </el-button>
                </div>
              </div>
            </div>

            <!-- 业务需求详情 -->
            <div class="content-section">
              <div class="section-title-row">
                <h4 class="section-title">业务需求详情</h4>
                <div v-if="!isRequirementCompleted" class="section-actions">
                  <el-button
                    v-if="canCreateNew && !sseGenerating"
                    type="success"
                    size="small"
                    @click="handleGenerate"
                  >
                    AI生成
                  </el-button>
                  <el-button
                    v-if="!canCreateNew && !sseGenerating"
                    type="warning"
                    size="small"
                    loading
                    disabled
                  >
                    AI生成中...
                  </el-button>
                  <el-button
                    v-if="sseGenerating"
                    type="danger"
                    size="small"
                    @click="stopGenerate"
                  >
                    停止生成
                  </el-button>
                </div>
              </div>
              <div class="content-area">
                <WysiwygEditor
                  v-model="content"
                  :readonly="isRequirementCompleted"
                  class="content-editor"
                />
              </div>

              <!-- AI反馈 -->
              <div v-if="content && !sseGenerating && canCreateNew && !isRequirementCompleted" class="ai-feedback">
                <span v-if="genFeedback" class="feedback-label">
                  {{ genFeedback.feedbackType === 'LIKE' ? '已赞' : '已反馈不满意' }}
                </span>
                <span v-else class="feedback-label">帮助我们改进AI生成质量</span>
                <div class="feedback-buttons">
                  <el-button
                    :type="genFeedback?.feedbackType === 'LIKE' ? 'success' : 'default'"
                    size="small"
                    :disabled="hasGenFeedback"
                    @click="handleFeedback('like')"
                  >
                    赞
                  </el-button>
                  <el-button
                    :type="genFeedback?.feedbackType === 'DISLIKE' ? 'danger' : 'default'"
                    size="small"
                    :disabled="hasGenFeedback"
                    @click="handleFeedback('dislike')"
                  >
                    不行
                  </el-button>
                </div>
              </div>

              <!-- 任务失败提示 -->
              <div v-if="latestTask?.errorMsg" class="ai-feedback error-feedback">
                <span class="feedback-label" style="color: var(--app-color-danger)">{{ latestTask.errorMsg }}</span>
              </div>
            </div>

            <!-- 关键标签 -->
            <div v-if="tags.length > 0" class="content-section">
              <h4 class="section-title">关键标签</h4>
              <div class="tags-area">
                <span
                  v-for="tag in tags"
                  :key="tag.text"
                  class="custom-tag"
                  :class="'tag-' + getTagClass(tag.type)"
                >
                  {{ tag.text }}
                </span>
              </div>
            </div>
          </div>

          <!-- 底部操作栏 -->
          <div class="form-actions">
            <el-button v-if="isRequirementCompleted" @click="router.push('/requirement')">
              返回列表
            </el-button>
            <el-button v-if="isRequirementCompleted" type="primary" @click="handleNextStep">
              查看智能检测
            </el-button>
            <el-button v-if="!isRequirementCompleted" @click="router.push('/requirement')">
              返回列表
            </el-button>
            <el-button v-if="!isRequirementCompleted" type="primary" @click="handleNextStep">
              下一步：智能检测
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- AI助手侧边栏 -->
    <AiAssistantSidebar
      v-if="!isRequirementCompleted"
      v-model:visible="chatVisible"
      v-model:messages="chatMessages"
      greeting="您好！我是您的AI助手，可以帮助您修改业务需求内容。请选择或输入需要修改的内容，我会为您提供修改建议。"
      :context="content"
      :requirement-id="requirementId"
      @feedback="handleChatFeedback"
      @message="handleAiMessage"
    >
      <template #quick-actions>
        <div class="quick-actions">
          <button class="quick-action-btn" @click="sendQuickAction('修改项目概况')">修改项目概况</button>
          <button class="quick-action-btn" @click="sendQuickAction('修改项目目标')">修改项目目标</button>
          <button class="quick-action-btn" @click="sendQuickAction('修改技术要求')">修改技术要求</button>
        </div>
      </template>
    </AiAssistantSidebar>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { requirementApi } from '@/api/requirement'
import { useLatestTask } from '@/composables/useLatestTask'
import { useFeedback } from '@/composables/useFeedback'
import { getTaskProgress, getProgressStatus } from '@/types/ai-task'
import WysiwygEditor from '@/components/editor/WysiwygEditor.vue'
import AiAssistantSidebar from '@/components/ai/AiAssistantSidebar.vue'
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
const chatVisible = ref(false)
const chatMessages = ref<AiChatMessage[]>([])

// ---- 最新任务 ----
const { latestTask, canCreateNew, refresh, setActive } = useLatestTask(
  'REQUIREMENT_GENERATE',
  requirementId,
  'REQUIREMENT',
  (task) => {
    // AI任务完成后，重试读取需求内容（后端定时同步可能有延迟）
    if (task.status === 'COMPLETED' && requirementId.value) {
      // 清除进度模拟计时器
      if (generateProgressTimer) {
        clearInterval(generateProgressTimer)
        generateProgressTimer = null
      }
      sseProgress.value = 100
      ElMessage.success('AI生成完成')
      sseGenerating.value = false

      // 重试读取内容，最多5次，间隔3秒
      const retryLoadContent = async (retries = 0) => {
        try {
          const req = await requirementApi.getById(requirementId.value!)
          if (req.content) {
            content.value = req.content
            return
          }
        } catch {
          // 忽略单次失败
        }
        if (retries < 5) {
          setTimeout(() => retryLoadContent(retries + 1), 3000)
        } else {
          ElMessage.warning('内容加载超时，请刷新页面重试')
        }
      }
      setTimeout(retryLoadContent, 2000)
    } else if (task.status === 'FAILED' || task.status === 'AI_UNAVAILABLE') {
      // 任务失败
      if (generateProgressTimer) {
        clearInterval(generateProgressTimer)
        generateProgressTimer = null
      }
      ElMessage.error('AI生成失败，请稍后重试')
      sseGenerating.value = false
    }
  },
)

// ---- 反馈 ----
const {
  currentFeedback: genFeedback,
  hasFeedback: hasGenFeedback,
  loadFeedback: loadGenFeedback,
  submitFeedback: submitGenFeedback,
} = useFeedback(
  'GENERATION_CONTENT',
  () => latestTask.value?.id,
)

const {
  submitFeedback: submitChatFeedback,
} = useFeedback(
  'CHAT_MESSAGE',
  () => latestTask.value?.id,
)

// 任务终态时加载反馈状态
watch(latestTask, (task) => {
  if (task && ['COMPLETED', 'FAILED', 'AI_UNAVAILABLE', 'SKIPPED'].includes(task.status)) {
    loadGenFeedback()
  }
})

// ---- 生成状态 ----
const sseGenerating = ref(false)
const isRequirementCompleted = computed(() => requirementData.value.status === 'COMPLETED')
let sseProgress = ref(0)

// ---- 进度计算 ----
const progressPercent = computed(() => {
  // SSE 正在流式输出
  if (sseGenerating.value) return sseProgress.value
  // 有任务状态
  if (latestTask.value) return getTaskProgress(latestTask.value.status)
  // 有内容但无任务 = 历史已完成
  if (content.value) return 100
  return 0
})

const progressStatus = computed(() => {
  if (sseGenerating.value) return ''
  if (latestTask.value) return getProgressStatus(latestTask.value.status)
  return ''
})

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

    // 加载参考文件
    try {
      referenceFiles.value = await requirementApi.getMatchFiles({ requirementId: id }) || []
    } catch {
      referenceFiles.value = []
    }

    // 生成标签
    generateTags(data)
  } catch {
    ElMessage.error('加载需求失败')
    router.back()
  }
})

onBeforeUnmount(() => {
  if (generateProgressTimer) {
    clearInterval(generateProgressTimer)
    generateProgressTimer = null
  }
  // 清空内容，防止编辑器在DOM销毁后报错
  content.value = ''
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

function getTagClass(type: string): string {
  if (!type) return 'primary'
  return type
}

// ---- AI生成 ----
async function handleGenerate() {
  if (!canCreateNew.value) return

  // 先刷新任务状态，确保没有活跃任务
  await refresh()
  if (!canCreateNew.value) {
    ElMessage.warning('AI生成任务正在处理中，请稍候')
    return
  }

  sseGenerating.value = true
  content.value = ''
  sseProgress.value = 0

  try {
    // 调用后端创建AI生成任务（普通POST，非SSE）
    const task = await requirementApi.generate(requirementId.value!, {})
    // 启动任务轮询
    if (task?.id) {
      setActive(task.id)
    }

    // 模拟进度（等待后端异步任务执行期间）
    const progressTimer = setInterval(() => {
      if (sseProgress.value < 90) {
        sseProgress.value += Math.floor(Math.random() * 3) + 1
      }
    }, 1000)

    // 保存timer以便停止时清除
    generateProgressTimer = progressTimer
  } catch (e: any) {
    ElMessage.error(e?.message || 'AI生成任务创建失败')
    sseGenerating.value = false
  }
}

// 模拟进度计时器引用
let generateProgressTimer: ReturnType<typeof setInterval> | null = null

function stopGenerate() {
  if (generateProgressTimer) {
    clearInterval(generateProgressTimer)
    generateProgressTimer = null
  }
  sseGenerating.value = false
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
async function handleFeedback(type: 'like' | 'dislike') {
  await submitGenFeedback(type)
}

// ---- AI对话回调 ----
async function handleChatFeedback(type: 'like' | 'dislike', msg: AiChatMessage) {
  await submitChatFeedback(type, {
    chatMessageId: msg.uid,
    chatContent: msg.content?.substring(0, 200),
  })
}

// ---- 快捷操作 ----
function sendQuickAction(action: string) {
  const userMsg: AiChatMessage = { role: 'user', content: action, timestamp: Date.now() }
  chatMessages.value.push(userMsg)
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
.progress-container {
  background: var(--app-bg-elevated, var(--app-card-bg));
  border-radius: 8px;
  border: 1px solid var(--app-border-light);
  padding: 24px;
  margin-bottom: 24px;
}

.progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.progress-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-primary);
}

.progress-text {
  font-size: 13px;
  color: var(--app-text-secondary);
  text-align: right;
  margin-top: 4px;
}

.is-pulse {
  animation: pulse-anim 2s ease-in-out infinite;
}

@keyframes pulse-anim {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ---- 内容容器 ---- */
.content-container {
  background: var(--app-bg-elevated, var(--app-card-bg));
  border-radius: 8px;
  border: 1px solid var(--app-border-light);
  overflow: hidden;
  margin-bottom: 24px;
}

/* ---- 内容头部 ---- */
.content-header {
  padding: 16px 24px;
  background: var(--app-bg-tertiary);
  border-bottom: 1px solid var(--app-border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 0;
}

.content-actions {
  display: flex;
  gap: 8px;
}

.primary-action-btn {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--app-brand-color);
  border: 2px solid rgba(51, 108, 255, 0.3);
  box-shadow: 0 4px 12px rgba(51, 108, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  color: white;
  padding: 0;
}

.primary-action-btn:hover {
  background: #2855d9;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(51, 108, 255, 0.45);
  border-color: rgba(51, 108, 255, 0.5);
}

.primary-action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.primary-action-btn svg {
  width: 18px;
  height: 18px;
}

/* ---- 内容主体 ---- */
.content-body {
  padding: 24px;
}

/* ---- 内容区块 ---- */
.content-section {
  margin-bottom: 24px;
}

.content-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--app-border-light);
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--app-border-light);
}

.section-title-row .section-title {
  margin: 0;
  padding: 0;
  border: none;
}

.section-content {
  font-size: 13px;
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.section-content p {
  margin: 0 0 6px 0;
}

.section-content p:last-child {
  margin-bottom: 0;
}

.section-content strong {
  color: var(--app-text-primary);
}

/* ---- 参考文件 ---- */
.reference-files {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--app-input-bg, var(--app-bg-secondary));
  border-radius: 6px;
}

.file-info {
  flex: 1;
}

.file-name {
  font-size: 13px;
  color: var(--app-text-primary);
  margin-bottom: 4px;
}

.file-meta {
  font-size: 12px;
  color: var(--app-text-tertiary);
}

.preview-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* ---- 内容区域 ---- */
.content-area {
  min-height: 400px;
  border: 1px solid var(--app-border-light);
  border-radius: 6px;
  background: var(--app-input-bg, var(--app-bg-secondary));
  overflow: hidden;
}

.content-preview {
  padding: 16px;
}

.content-editor {
  min-height: 400px;
}

/* ---- AI反馈 ---- */
.ai-feedback {
  margin-top: 20px;
  padding: 16px;
  background: var(--app-bg-tertiary);
  border-radius: 8px;
  border: 1px solid var(--app-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.error-feedback {
  background: rgba(239, 68, 68, 0.05);
  border-color: rgba(239, 68, 68, 0.2);
}

.feedback-label {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.feedback-buttons {
  display: flex;
  gap: 8px;
}

/* ---- 标签区域 ---- */
.tags-area {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
}

.custom-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  margin-right: 8px;
  margin-bottom: 8px;
}

.tag-primary {
  background: rgba(51, 108, 255, 0.15);
  color: var(--app-brand-color);
}

.tag-success {
  background: rgba(16, 185, 129, 0.15);
  color: var(--app-color-success, #10B981);
}

.tag-warning {
  background: rgba(245, 158, 11, 0.15);
  color: var(--app-color-warning, #F59E0B);
}

.tag-info {
  background: rgba(107, 114, 128, 0.15);
  color: var(--app-text-tertiary);
}

.tag-danger {
  background: rgba(239, 68, 68, 0.15);
  color: var(--app-color-danger, #EF4444);
}

/* ---- 底部操作栏 ---- */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 24px;
  background: var(--app-bg-tertiary);
  border-top: 1px solid var(--app-border-light);
}

</style>
