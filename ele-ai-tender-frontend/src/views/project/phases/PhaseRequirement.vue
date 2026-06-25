<template>
  <div class="phase-requirement">
    <!-- AI生成状态卡片（始终可见） -->
    <GenerationStatusCard
      :task="latestTask"
      :can-create-new="canCreateNew"
      :progress-percent="progressPercent"
      :generating-title="generationStageText"
      :generating-desc="generationOverlayDetail"
      completed-desc="所有章节已生成完成，您可以在编辑器中查看和修改内容"
      idle-title="AI生成需求"
      idle-desc="点击下方按钮开始AI生成招标需求内容"
    >
      <template #idle-action>
        <button v-if="!readonly" class="btn btn-primary generate-btn" :disabled="generationLocked || !canCreateNew" @click="handleGenerate">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
          </svg>
          AI生成需求
        </button>
      </template>
    </GenerationStatusCard>

    <!-- 章节标题 -->
    <h4 class="section-title">第三章 招标/采购需求</h4>

    <!-- 编辑器区域 -->
    <div class="editor-container">
      <div class="editor-area">
        <WysiwygEditor
          v-model="content"
          :readonly="editorReadonly"
          @selection-change="handleSelectionChange"
        />
        <div v-if="generationLocked" class="generation-overlay" role="status" aria-live="polite">
          <div class="generation-status-panel">
            <div class="generation-spinner" aria-hidden="true"></div>
            <div class="generation-status-content">
              <div class="generation-status-title">{{ generationStageText }}</div>
              <div class="generation-status-detail">{{ generationOverlayDetail }}</div>
              <div class="generation-status-progress">
                <span>{{ generationOverlayProgressText }}</span>
                <div class="generation-progress-bar">
                  <div class="generation-progress-fill" :style="{ width: progressPercent + '%' }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- AI内容反馈 -->
    <div v-if="latestTask && content && !editorReadonly" class="ai-feedback">
      <h4 class="feedback-title">对AI生成内容的反馈</h4>
      <div class="feedback-actions">
        <button
          :class="['feedback-btn', 'btn-like', { active: genFeedback?.feedbackType === 'LIKE' }]"
          :disabled="hasGenFeedback"
          @click="handleFeedback('like')"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
            <polyline points="22 4 12 14.01 9 11.01" />
          </svg>
          <span>赞</span>
        </button>
        <button
          :class="['feedback-btn', 'btn-dislike', { active: genFeedback?.feedbackType === 'DISLIKE' }]"
          :disabled="hasGenFeedback"
          @click="handleFeedback('dislike')"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
          <span>不行</span>
        </button>
        <span v-if="genFeedback" class="feedback-status">
          {{ genFeedback.feedbackType === 'LIKE' ? '已赞' : '已反馈不满意' }}
        </span>
        <span v-else class="feedback-hint">帮助我们改进AI生成质量</span>
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
        <button v-if="!readonly" class="btn btn-warning" :disabled="generationLocked || !canCreateNew" @click="handleGenerate">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10" />
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
          </svg>
          重新生成章节
        </button>
      </div>
      <div v-if="!readonly" class="form-actions-right">
        <button class="btn btn-secondary" :disabled="generationLocked" @click="handleSave">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
            <polyline points="17 21 17 13 7 13 7 21" />
            <polyline points="7 3 7 8 15 8" />
          </svg>
          保存编辑
        </button>
        <button class="btn btn-primary" :disabled="generationLocked || !canCreateNew" @click="handleSaveAndNext">
          确认需求
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="5" y1="12" x2="19" y2="12" />
            <polyline points="12 5 19 12 12 19" />
          </svg>
        </button>
      </div>
    </div>

    <!-- AI助手侧边栏 -->
    <AiAssistantSidebar
      ref="aiSidebarRef"
      v-if="!editorReadonly"
      v-model:visible="chatVisible"
      v-model:messages="chatMessages"
      greeting="您好！我是您的AI助手，可以帮助您修改详细需求内容。请选择或输入需要修改的内容，我会为您提供修改建议。"
      :context="content"
      :project-id="projectId"
      :selected-text="selectedText"
      @feedback="handleChatFeedback"
      @message="handleChatMessage"
      @replace="handleReplace"
      @update:selected-text="selectedText = $event"
    >
      <template #quick-actions>
        <div class="quick-actions">
          <button class="quick-action-btn" @click="sendQuickAction('修改工程范围')">修改工程范围</button>
          <button class="quick-action-btn" @click="sendQuickAction('修改技术要求')">修改技术要求</button>
          <button class="quick-action-btn" @click="sendQuickAction('修改质量标准')">修改质量标准</button>
        </div>
      </template>
    </AiAssistantSidebar>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, toRef } from 'vue'
import { ElMessage } from 'element-plus'

import { projectApi } from '@/api/project'
import { aiApi, createSSEConnection } from '@/api/ai'
import { useLatestTask } from '@/composables/useLatestTask'
import { useFeedback } from '@/composables/useFeedback'
import {
  buildRequirementGenerationProgressMarkdown,
  getTaskProgress,
  isTaskResultSyncing,
  isTaskSucceeded,
  parseRequirementGenerationProgress,
} from '@/types/ai-task'
import GenerationStatusCard from '@/components/GenerationStatusCard.vue'
import AiAssistantSidebar from '@/components/ai/AiAssistantSidebar.vue'
import WysiwygEditor from '@/components/editor/WysiwygEditor.vue'
import { applyAiReplacement } from '@/utils/aiReplacement'
import type { AiChatMessage } from '@/types/ai'

const props = defineProps<{ projectId: number; readonly?: boolean }>()
const emit = defineEmits<{ next: []; prev: [] }>()

const content = ref('')
const isOptimizing = ref(false)
const isCreatingGenerationTask = ref(false)

const chatVisible = ref(false)
const chatMessages = ref<AiChatMessage[]>([])
const selectedText = ref('')
let closeOptimizeSSE: (() => void) | null = null

// AI任务：项目需求生成，bizId=projectId
const projectIdRef = toRef(props, 'projectId')
const { latestTask, canCreateNew, refresh, setActive } = useLatestTask(
  'PROJECT_REQUIREMENT_GENERATE',
  projectIdRef,
  'REQUIREMENT',
  async (_task) => {
    // resultSynced=1 时业务数据已同步，直接从项目接口读取
    try {
      const proj = await projectApi.getById(props.projectId)
      if (proj.requirementContent) {
        content.value = proj.requirementContent
      }
    } catch {
      // 忽略刷新失败，用户可手动刷新
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
  if (task && isCreatingGenerationTask.value) {
    isCreatingGenerationTask.value = false
  }
  if (task && isTaskSucceeded(task)) {
    loadGenFeedback()
  }
})

// ---- AI生成进度渲染 ----
const generationProgress = computed(() => parseRequirementGenerationProgress(latestTask.value?.result))

const generationLocked = computed(() => {
  if (isCreatingGenerationTask.value) return true
  const task = latestTask.value
  if (!task) return false
  if (isTaskSucceeded(task)) return false
  return task.status === 'PENDING'
    || task.status === 'PROCESSING'
    || isTaskResultSyncing(task)
})

const editorReadonly = computed(() => props.readonly || generationLocked.value)

const generationStageText = computed(() => {
  const task = latestTask.value
  const progress = generationProgress.value

  if (isCreatingGenerationTask.value) return '任务创建中'
  if (!progress) {
    if (task?.status === 'PENDING') return '任务排队中'
    if (task?.status === 'PROCESSING') return 'AI处理中'
    if (isTaskResultSyncing(task)) return '结果同步中'
    return '正在生成中'
  }

  switch (progress.contentStage) {
    case 'OUTLINE_GENERATED':
      return '大纲已生成'
    case 'CHAPTER_GENERATING':
      return `正文生成中 ${progress.completedChapterCount ?? 0}/${progress.totalChapterCount ?? 0}`
    case 'DRAFT_COMPLETED':
      return '草稿已生成'
    case 'REVIEWING':
      return '全文审查中'
    case 'COMPLETED':
      return isTaskResultSyncing(task) ? '结果同步中' : '已完成'
    default:
      return 'AI处理中'
  }
})

const generationOverlayDetail = computed(() => {
  const task = latestTask.value
  const progress = generationProgress.value

  if (isCreatingGenerationTask.value) {
    return '正在提交AI生成任务'
  }
  if (!progress) {
    if (task?.status === 'PENDING') return '任务排队中，等待AI服务消费'
    if (task?.status === 'PROCESSING') return 'AI正在处理任务，生成内容会分批填充到编辑器'
    if (isTaskResultSyncing(task)) return 'AI已完成，正在同步到项目需求内容'
    return '正在获取任务状态'
  }

  const totalChapterCount = progress.totalChapterCount
    ?? progress.outline?.chapters?.length
    ?? 0
  const completedChapterCount = progress.completedChapterCount ?? 0

  switch (progress.contentStage) {
    case 'OUTLINE_GENERATED':
      return totalChapterCount > 0
        ? `已生成 ${totalChapterCount} 个章节大纲，等待正文生成`
        : '大纲已生成，等待正文生成'
    case 'CHAPTER_GENERATING':
      return totalChapterCount > 0
        ? `已完成 ${completedChapterCount} / ${totalChapterCount} 章，正文会实时填充到对应章节`
        : '正文生成中，生成内容会实时填充'
    case 'DRAFT_COMPLETED':
      return '草稿已生成，准备进行全文审查'
    case 'REVIEWING':
      return '全文审查修订中，审查结束前暂不可编辑'
    case 'COMPLETED':
      return isTaskResultSyncing(task)
        ? 'AI已完成，正在同步到项目需求内容'
        : '生成已完成'
    default:
      return 'AI正在处理任务'
  }
})

watch(generationProgress, (progress) => {
  if (!progress || isTaskSucceeded(latestTask.value)) return
  const progressContent = buildRequirementGenerationProgressMarkdown(progress)
  if (progressContent && progressContent !== content.value) {
    content.value = progressContent
  }
}, { deep: true })

// 自动保存：直接通过 projectApi.update 保存到项目
let autoSaveTimer: ReturnType<typeof setInterval> | null = null
const isAutoSaving = ref(false)

function startAutoSave() {
  if (autoSaveTimer) return
  autoSaveTimer = setInterval(async () => {
    if (generationLocked.value) return
    if (!content.value) return
    isAutoSaving.value = true
    try {
      await projectApi.update(props.projectId, { requirementContent: content.value })
    } catch {
      // 自动保存失败不阻塞
    } finally {
      isAutoSaving.value = false
    }
  }, 120000)
}

function stopAutoSave() {
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
    autoSaveTimer = null
  }
}

const progressPercent = computed(() => {
  if (isCreatingGenerationTask.value) return 5

  const task = latestTask.value
  const progress = generationProgress.value
  if (!progress) return getTaskProgress(task)

  const totalChapterCount = progress.totalChapterCount
    ?? progress.outline?.chapters?.length
    ?? 0
  const completedChapterCount = progress.completedChapterCount ?? 0

  switch (progress.contentStage) {
    case 'OUTLINE_GENERATED':
      return 20
    case 'CHAPTER_GENERATING': {
      const chapterRatio = totalChapterCount > 0
        ? completedChapterCount / totalChapterCount
        : 0
      return Math.min(80, Math.round(25 + chapterRatio * 55))
    }
    case 'DRAFT_COMPLETED':
      return 85
    case 'REVIEWING':
      return 92
    case 'COMPLETED':
      return task?.resultSynced === 1 ? 100 : 95
    default:
      return getTaskProgress(task)
  }
})

const generationOverlayProgressText = computed(() => `当前进度 ${progressPercent.value}%`)

const loadData = async () => {
  const project = await projectApi.getById(props.projectId)
  content.value = project.requirementContent || ''

  // 非只读模式启动自动保存
  if (!props.readonly) {
    startAutoSave()
  }
}

const handleGenerate = async () => {
  await refresh()
  if (!canCreateNew.value) {
    ElMessage.warning('AI生成任务正在处理中，请稍候')
    return
  }
  const originalContent = content.value
  isCreatingGenerationTask.value = true
  try {
    const res = await projectApi.generateRequirement(props.projectId)
    content.value = ''
    setActive(res.id)
  } catch (e: any) {
    content.value = originalContent
    isCreatingGenerationTask.value = false
    if (e?.code === 8084) {
      ElMessage.warning('AI生成任务正在处理中，请稍候')
      refresh()
      return
    }
    ElMessage.error('提交AI生成失败')
  }
}

/** 文本优化（通过AI助手或直接调用） */
const handleOptimize = async () => {
  if (editorReadonly.value) {
    ElMessage.warning('当前内容暂不可编辑')
    return
  }
  if (!content.value.trim()) {
    ElMessage.warning('请先输入需求内容')
    return
  }
  isOptimizing.value = true
  const originalContent = content.value
  let optimizedContent = ''

  closeOptimizeSSE = createSSEConnection(
    aiApi.optimizeUrl,
    {
      content: content.value,
      type: 'requirement',
      projectId: props.projectId,
    },
    (data: string) => {
      optimizedContent += data
      content.value = optimizedContent
    },
    () => {
      if (!optimizedContent) {
        content.value = originalContent
        ElMessage.error('文本优化失败')
      }
      isOptimizing.value = false
      closeOptimizeSSE = null
    },
    () => {
      isOptimizing.value = false
      closeOptimizeSSE = null
      ElMessage.success('文本优化完成')
    },
  )
}

// 暴露给AI助手调用
defineExpose({ handleOptimize })

const handleFeedback = async (type: 'like' | 'dislike') => {
  await submitGenFeedback(type)
}

const aiSidebarRef = ref<InstanceType<typeof AiAssistantSidebar>>()

const sendQuickAction = (action: string) => {
  aiSidebarRef.value?.sendQuickAction(action)
}

const handleChatFeedback = async (type: 'like' | 'dislike', msg: AiChatMessage) => {
  await submitChatFeedback(type, {
    chatMessageId: msg.uid,
    chatContent: msg.content?.substring(0, 200),
  })
}

const handleChatMessage = (_msg: string) => {
  // 消息已通过 v-model 同步到 chatMessages
}

function handleSelectionChange(text: string) {
  selectedText.value = text
}

function handleReplace(payload: { selectedText: string; replacement: string }) {
  const { selectedText: original, replacement } = payload

  const result = applyAiReplacement(content.value, original, replacement)
  if (!result.found) {
    ElMessage.warning('原文已被修改，请手动替换')
    return
  }

  if (result.duplicated) {
    ElMessage.warning('存在多处相同内容，已替换第一处')
  }

  content.value = result.content
  ElMessage.success('替换成功')
}

const handleSave = async () => {
  if (generationLocked.value) {
    ElMessage.warning('AI正在生成或审查，请稍后再保存')
    return
  }
  if (content.value) {
    await projectApi.update(props.projectId, { requirementContent: content.value })
  }
  ElMessage.success('需求内容保存成功')
}

const handleSaveAndNext = async () => {
  if (generationLocked.value) {
    ElMessage.warning('AI正在生成或审查，请稍后')
    return
  }
  await handleSave()

  // 推进阶段到"评审项设置"，后端会自动触发AI评审项生成任务
  try {
    await projectApi.advancePhase(props.projectId, 3)
    emit('next')
  } catch (e: any) {
    ElMessage.warning(e?.message || '阶段推进失败，请稍后重试')
  }
}

onBeforeUnmount(() => {
  if (closeOptimizeSSE) {
    closeOptimizeSSE()
    closeOptimizeSSE = null
  }
  stopAutoSave()
  // 清空内容，防止编辑器在DOM销毁后报错
  content.value = ''
})

onMounted(loadData)
</script>

<style scoped lang="scss">
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
// 编辑器
// ========================================
.editor-container {
  border: 1px solid var(--app-border-light);
  border-radius: var(--app-radius-sm);
  background: var(--app-input-bg);
  overflow: hidden;
  position: relative;
}

.editor-area {
  min-height: 500px;
  position: relative;
}

.generation-overlay {
  position: absolute;
  inset: 0;
  z-index: 5;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 72px 24px 24px;
  background: rgba(248, 250, 252, 0.62);
  backdrop-filter: blur(1px);
  pointer-events: auto;
}

.generation-status-panel {
  width: min(460px, 100%);
  display: flex;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--app-border-medium);
  border-radius: var(--app-radius-sm);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.14);
}

.generation-spinner {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 3px solid rgba(51, 108, 255, 0.18);
  border-top-color: var(--app-brand-color);
  flex-shrink: 0;
  animation: generation-spin 0.9s linear infinite;
}

.generation-status-content {
  flex: 1;
  min-width: 0;
}

.generation-status-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin-bottom: 6px;
}

.generation-status-detail {
  font-size: 13px;
  color: var(--app-text-secondary);
  line-height: 1.5;
  margin-bottom: 12px;
}

.generation-status-progress {
  display: grid;
  gap: 6px;
  font-size: 12px;
  color: var(--app-text-tertiary);
}

.generation-progress-bar {
  width: 100%;
  height: 6px;
  background: var(--app-bg-elevated);
  border-radius: 999px;
  overflow: hidden;
}

.generation-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--app-brand-color), var(--app-auxiliary-color));
  border-radius: 999px;
  transition: width 0.3s ease;
}

@keyframes generation-spin {
  to { transform: rotate(360deg); }
}

// ========================================
// AI内容反馈
// ========================================
.ai-feedback {
  margin-top: 20px;
  padding: 16px;
  background: var(--app-bg-tertiary);
  border-radius: var(--app-radius-sm);
  border: 1px solid var(--app-border-light);
}

.feedback-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 0 0 12px 0;
}

.feedback-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.feedback-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  color: white;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &.btn-like {
    background: var(--app-color-success);

    &:hover:not(:disabled),
    &.active {
      background: #4db87a;
      box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
    }
  }

  &.btn-dislike {
    background: var(--app-color-danger);

    &:hover:not(:disabled),
    &.active {
      background: #f78989;
      box-shadow: 0 2px 8px rgba(245, 108, 108, 0.3);
    }
  }
}

.feedback-status {
  font-size: 13px;
  color: var(--app-brand-color);
  margin-left: 12px;
  font-weight: 500;
}

.feedback-hint {
  font-size: 13px;
  color: var(--app-text-secondary);
  margin-left: 12px;
}

.generate-btn {
  flex-shrink: 0;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
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
// 按钮样式（匹配原型）
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

.btn-warning {
  background: var(--app-color-warning);
  color: white;

  &:hover:not(:disabled) {
    background: #cf8a2e;
  }
}
</style>
