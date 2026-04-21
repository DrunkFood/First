<template>
  <div class="phase-requirement">
    <!-- 卡片容器 -->
    <div class="form-container">
      <!-- 卡片头部 -->
      <div class="form-header">
        <h3 class="form-title">详细需求生成</h3>
      </div>

      <!-- 卡片内容区 -->
      <div class="form-section">
        <!-- AI生成状态卡片（始终可见） -->
        <GenerationStatusCard
          :task="latestTask"
          :can-create-new="canCreateNew"
          :progress-percent="progressPercent"
          generating-title="正在生成中"
          generating-desc="AI正在生成招标需求内容，请稍候..."
          completed-desc="所有章节已生成完成，您可以在编辑器中查看和修改内容"
          idle-title="AI生成需求"
          idle-desc="点击下方按钮开始AI生成招标需求内容"
        >
          <template #idle-action>
            <button v-if="!readonly" class="btn btn-primary generate-btn" @click="handleGenerate">
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
            <MdPreview v-if="readonly" :model-value="content" :theme="themeStore.mode" />
            <MarkdownEditor v-else v-model="content" :toolbars-exclude="excludeToolbars" />
          </div>
        </div>

        <!-- AI内容反馈 -->
        <div v-if="content && !readonly" class="ai-feedback">
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
          <button v-if="!readonly" class="btn btn-warning" :disabled="!canCreateNew" @click="handleGenerate">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 4 23 10 17 10" />
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
            </svg>
            重新生成章节
          </button>
        </div>
        <div v-if="!readonly" class="form-actions-right">
          <button class="btn btn-secondary" @click="handleSave">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
              <polyline points="17 21 17 13 7 13 7 21" />
              <polyline points="7 3 7 8 15 8" />
            </svg>
            保存编辑
          </button>
          <button class="btn btn-primary" :disabled="!canCreateNew" @click="handleSaveAndNext">
            确认需求
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- AI助手侧边栏 -->
    <AiAssistantSidebar
      v-if="!readonly"
      v-model:visible="chatVisible"
      v-model:messages="chatMessages"
      greeting="您好！我是您的AI助手，可以帮助您修改详细需求内容。请选择或输入需要修改的内容，我会为您提供修改建议。"
      :context="content"
      :project-id="projectId"
      @feedback="handleChatFeedback"
      @message="handleChatMessage"
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

import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { projectApi } from '@/api/project'
import { aiApi, createSSEConnection } from '@/api/ai'
import { useLatestTask } from '@/composables/useLatestTask'
import { useFeedback } from '@/composables/useFeedback'
import { getTaskProgress } from '@/types/ai-task'
import { useThemeStore } from '@/store/theme'
import GenerationStatusCard from '@/components/GenerationStatusCard.vue'
import AiAssistantSidebar from '@/components/ai/AiAssistantSidebar.vue'
import MarkdownEditor from '@/components/editor/MarkdownEditor.vue'
import type { AiChatMessage } from '@/types/ai'
import type { ToolbarNames } from 'md-editor-v3'

const props = defineProps<{ projectId: number; readonly?: boolean }>()
const emit = defineEmits<{ next: []; prev: [] }>()
const themeStore = useThemeStore()

// 排除不需要的工具栏项，只保留原型中的：加粗/斜体/下划线/列表/插入图片
const excludeToolbars: ToolbarNames[] = [
  'strikeThrough', 'title', 'sub', 'sup', 'quote', 'task', 'codeRow', 'code',
  'link', 'table', 'mermaid', 'katex', 'save',
  'prettier', 'pageFullscreen', 'fullscreen', 'preview', 'htmlPreview', 'catalog',
  'github',
]

const content = ref('')
const isOptimizing = ref(false)

const chatVisible = ref(false)
const chatMessages = ref<AiChatMessage[]>([])
let closeOptimizeSSE: (() => void) | null = null

// AI任务：项目需求生成，bizId=projectId
const projectIdRef = toRef(props, 'projectId')
const { latestTask, canCreateNew, refresh } = useLatestTask(
  'PROJECT_REQUIREMENT_GENERATE',
  projectIdRef,
  'REQUIREMENT',
  async (task) => {
    if (task.status !== 'COMPLETED') return

    // 优先从任务结果中直接提取内容（实时可用，避免后端同步延迟）
    let contentLoaded = false
    if (task.result) {
      try {
        const resultObj = JSON.parse(task.result)
        if (resultObj.content) {
          content.value = resultObj.content
          contentLoaded = true
          // 同步到项目
          await projectApi.update(props.projectId, { requirementContent: resultObj.content })
        }
      } catch { /* JSON解析失败，走fallback */ }
    }

    // Fallback: 从项目接口读取
    if (!contentLoaded) {
      setTimeout(async () => {
        try {
          const proj = await projectApi.getById(props.projectId)
          if (proj.requirementContent) {
            content.value = proj.requirementContent
          }
        } catch {
          // 忽略刷新失败，用户可手动刷新
        }
      }, 1500)
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

// 自动保存：直接通过 projectApi.update 保存到项目
let autoSaveTimer: ReturnType<typeof setInterval> | null = null
const isAutoSaving = ref(false)

function startAutoSave() {
  if (autoSaveTimer) return
  autoSaveTimer = setInterval(async () => {
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
  if (!latestTask.value) return 0
  return getTaskProgress(latestTask.value.status)
})

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
  // 推进阶段触发后端 RequirementTrigger.onEnter() 自动创建AI任务
  // 如果已在需求阶段，则通过 advancePhase 重新进入（后端会创建新任务）
  try {
    await projectApi.advancePhase(props.projectId, 2)
  } catch (e: any) {
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

const sendQuickAction = (action: string) => {
  chatMessages.value.push({
    role: 'user',
    content: action,
    timestamp: Date.now(),
  })
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

const handleSave = async () => {
  if (content.value) {
    await projectApi.update(props.projectId, { requirementContent: content.value })
  }
  ElMessage.success('需求内容保存成功')
}

const handleSaveAndNext = async () => {
  await handleSave()

  // 推进阶段到"评审项设置"，后端会自动触发AI评审项生成任务
  try {
    await projectApi.advancePhase(props.projectId, 3)
  } catch (e: any) {
    ElMessage.warning(e?.message || '阶段推进失败，可手动进入下一步')
  }

  emit('next')
}

onBeforeUnmount(() => {
  if (closeOptimizeSSE) {
    closeOptimizeSSE()
    closeOptimizeSSE = null
  }
  stopAutoSave()
  // 清空内容，防止md-editor-v3在DOM销毁后报querySelectorAll/MutationObserver错误
  content.value = ''
})

onMounted(loadData)
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
// 编辑器
// ========================================
.editor-container {
  border: 1px solid var(--app-border-light);
  border-radius: var(--app-radius-sm);
  background: var(--app-input-bg);
  overflow: hidden;
}

.editor-area {
  :deep(.markdown-editor) {
    min-height: 500px;
  }

  :deep(.md-editor-preview-wrapper) {
    padding: 24px;
  }
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
