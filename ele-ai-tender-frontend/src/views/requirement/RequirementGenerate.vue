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
              {{ generationStageText }}
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
                <button class="primary-action-btn" :disabled="saving || editorReadonly" @click="handleSave">
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
                <p><strong>项目类别：</strong>
                  <StatusBadge v-if="requirementData.projectCategory" :status="requirementData.projectCategory" :type-map="PROJECT_CATEGORY_MAP" />
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
                  :readonly="editorReadonly"
                  :highlights="detectionIssues"
                  class="content-editor"
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
                        <el-progress
                          :percentage="progressPercent"
                          :stroke-width="6"
                          :show-text="false"
                        />
                      </div>
                    </div>
                  </div>
                </div>
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
      ref="aiSidebarRef"
      v-if="!isRequirementCompleted"
      v-model:visible="chatVisible"
      v-model:messages="chatMessages"
      greeting="您好！我是您的AI助手，可以帮助您修改业务需求内容。请选择或输入需要修改的内容，我会为您提供修改建议。"
      :context="content"
      :requirement-id="requirementId"
      :selected-text="selectedText"
      @feedback="handleChatFeedback"
      @message="handleAiMessage"
      @replace="handleReplace"
      @update:selected-text="selectedText = $event"
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
import {
  getTaskProgress,
  getProgressStatus,
  getProcessingProgressByTime,
  isTaskSucceeded,
  isTaskTerminal,
  parseRequirementGenerationProgress,
  buildRequirementGenerationProgressMarkdown
} from '@/types/ai-task'
import WysiwygEditor from '@/components/editor/WysiwygEditor.vue'
import AiAssistantSidebar from '@/components/ai/AiAssistantSidebar.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import {PROJECT_CATEGORY_MAP, PROJECT_TYPE_MAP} from '@/constants/status-maps'
import { REQUIREMENT_TYPE_MAP } from '@/types/requirement'
import { formatBudgetWanYuan } from '@/utils/budget'
import type { RequirementInfo, MatchFile } from '@/types/requirement'
import type { AiChatMessage } from '@/types/ai'
import type { DetectionIssueVO, RequirementDetectionRecord } from '@/types/detection'

const router = useRouter()
const route = useRoute()

// ---- 基础数据 ----
const requirementId = ref(0)
const requirementName = ref('')
const requirementData = ref<Partial<RequirementInfo>>({})
const content = ref('')
const referenceFiles = ref<MatchFile[]>([])
const tags = ref<Array<{ text: string; type: '' | 'success' | 'warning' | 'info' | 'danger' }>>([])
const detectionIssues = ref<DetectionIssueVO[]>([])

// ---- 状态 ----
const saving = ref(false)
const exporting = ref(false)
const chatVisible = ref(false)
const chatMessages = ref<AiChatMessage[]>([])
const selectedText = ref('')

// ---- 最新任务 ----
const { latestTask, canCreateNew, refresh, setActive } = useLatestTask(
  'REQUIREMENT_GENERATE',
  requirementId,
  'REQUIREMENT',
  async (_task) => {
    // resultSynced=1 时业务数据已同步，直接读取
    if (requirementId.value) {
      stopProgressTimer()
      displayProgress.value = 100
      ElMessage.success('AI生成完成')
      sseGenerating.value = false

      try {
        const req = await requirementApi.getById(requirementId.value!)
        if (req.content) {
          content.value = req.content
        }
      } catch {
        ElMessage.warning('内容加载失败，请刷新页面重试')
      }
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

// 任务成功后加载反馈状态
watch(latestTask, (task) => {
  if (task && isTaskSucceeded(task)) {
    loadGenFeedback()
  }
})

// ---- 生成状态 ----
const sseGenerating = ref(false)
const isRequirementCompleted = computed(() => requirementData.value.status === 'COMPLETED')
const generationProgress = computed(() => parseRequirementGenerationProgress(latestTask.value?.result))
const generationLocked = computed(() => {
  const task = latestTask.value
  if (sseGenerating.value) return true
  if (!task) return false
  if (isTaskSucceeded(task)) return false
  return task.status === 'PENDING'
    || task.status === 'PROCESSING'
    || (task.status === 'COMPLETED' && task.resultSynced === 0)
})
const editorReadonly = computed(() => isRequirementCompleted.value || generationLocked.value)
const generationStageText = computed(() => {
  const progress = generationProgress.value
  if (!progress) {
    return latestTask.value?.status === 'PENDING' ? '任务排队中' : '处理中'
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
      return latestTask.value?.resultSynced === 0 ? '结果同步中' : '已完成'
    default:
      return '处理中'
  }
})
const generationOverlayDetail = computed(() => {
  const task = latestTask.value
  const progress = generationProgress.value

  if (!progress) {
    if (sseGenerating.value && !task) {
      return '任务已提交，等待后端接收'
    }
    if (task?.status === 'PENDING') {
      return '任务排队中，等待 AI 服务消费'
    }
    if (task?.status === 'COMPLETED' && task.resultSynced === 0) {
      return 'AI 已完成，正在同步到业务需求'
    }
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
      return task?.resultSynced === 0
        ? 'AI 已完成，正在同步到业务需求'
        : '生成已完成'
    default:
      return 'AI 正在处理任务'
  }
})

// ---- 统一进度模型 ----
const displayProgress = ref(0)       // 显示进度（只增不减，除非新任务重置）
const generateStartTime = ref(0)     // 本地记录的生成开始时间戳
watch(generationProgress, (progress) => {
  if (!progress || isTaskSucceeded(latestTask.value)) return
  const progressContent = buildRequirementGenerationProgressMarkdown(progress)
  if (progressContent && progressContent !== content.value) {
    content.value = progressContent
  }
}, { deep: true })

let progressTimer: ReturnType<typeof setInterval> | null = null

/** 更新进度（由定时器每秒调用） */
function updateProgress() {
  const task = latestTask.value
  if (!task) {
    displayProgress.value = content.value ? 100 : 0
    return
  }

  if (task.status === 'PROCESSING' || sseGenerating.value) {
    // PROCESSING / 刚触发生成：基于已运行时间计算进度
    displayProgress.value = getProcessingProgressByTime(task, generateStartTime.value)
  } else {
    // 其他状态（PENDING/COMPLETED/FAILED等）：使用固定映射
    const taskProgress = getTaskProgress(task)
    // 进度只增不减（避免从90%跳到10%等视觉回退）
    displayProgress.value = Math.max(displayProgress.value, taskProgress)
  }
}

/** 启动进度定时器 */
function startProgressTimer() {
  stopProgressTimer()
  updateProgress()
  progressTimer = setInterval(updateProgress, 1000)
}

/** 停止进度定时器 */
function stopProgressTimer() {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

// 监听任务状态变化，自动启动/停止进度定时器
watch(latestTask, (task) => {
  if (!task) {
    // 无任务时：sseGenerating 保持定时器（updateProgress 用 generateStartTime 兜底）
    if (!sseGenerating.value) {
      stopProgressTimer()
      displayProgress.value = content.value ? 100 : 0
    }
    return
  }

  if (task.status === 'PROCESSING' || sseGenerating.value) {
    // 生成中（含用户刚触发、任务尚在 PENDING 的场景）：确保定时器运行
    if (!progressTimer) {
      startProgressTimer()
    }
    return
  }

  // 非 PROCESSING 且非用户主动生成 → 停止定时器
  stopProgressTimer()

  if (task.status === 'COMPLETED') {
    if (task.resultSynced === 1) {
      displayProgress.value = 100
    } else if (task.resultSynced === 0) {
      // 已完成但结果同步中，进度不低于90%
      displayProgress.value = Math.max(displayProgress.value, 90)
    } else {
      // resultSynced=2 同步失败
      displayProgress.value = 0
    }
  } else if (isTaskTerminal(task)) {
    // FAILED / AI_UNAVAILABLE / SKIPPED
    displayProgress.value = getTaskProgress(task)
  } else {
    // PENDING（页面刷新恢复场景）
    const taskProgress = getTaskProgress(task)
    displayProgress.value = Math.max(displayProgress.value, taskProgress)
  }
}, { immediate: true })

// ---- 进度计算 ----
const progressPercent = computed(() => displayProgress.value)
const generationOverlayProgressText = computed(() => `当前进度 ${progressPercent.value}%`)

const progressStatus = computed(() => {
  if (latestTask.value) return getProgressStatus(latestTask.value)
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

    // 加载检测记录（用于编辑器高亮）
    loadDetectionHighlights(id)
  } catch {
    ElMessage.error('加载需求失败')
    router.back()
  }
})

onBeforeUnmount(() => {
  stopProgressTimer()
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
  displayProgress.value = 0
  generateStartTime.value = Date.now() // 记录本地开始时间

  try {
    // 调用后端创建AI生成任务（普通POST，非SSE）
    const task = await requirementApi.generate(requirementId.value!, {})
    // 启动任务轮询
    if (task?.id) {
      setActive(task.id)
    }

    // 启动基于时间的统一进度定时器
    startProgressTimer()
  } catch (e: any) {
    ElMessage.error(e?.message || 'AI生成任务创建失败')
    sseGenerating.value = false
    stopProgressTimer()
  }
}

function stopGenerate() {
  stopProgressTimer()
  sseGenerating.value = false
}

// ---- 保存 ----
async function handleSave() {
  if (editorReadonly.value) return
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
  if (generationLocked.value) {
    ElMessage.warning('AI\u6b63\u5728\u751f\u6210\u6216\u5ba1\u67e5\uff0c\u8bf7\u7a0d\u540e')
    return
  }
  const hasContent = content.value || requirementData.value.content
  if (!hasContent && requirementData.value.status !== 'COMPLETED') {
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
const aiSidebarRef = ref<InstanceType<typeof AiAssistantSidebar>>()

function sendQuickAction(action: string) {
  aiSidebarRef.value?.sendQuickAction(action)
}

function handleAiMessage(msg: string, hadSelection: boolean) {
  if (!msg.trim()) return
  // 带选中内容的消息走替换流程，不追加到末尾
  if (!hadSelection) {
    content.value += '\n\n' + msg
  }
}

function handleSelectionChange(text: string) {
  selectedText.value = text
}

function handleReplace(payload: { selectedText: string; replacement: string }) {
  const { selectedText: original, replacement } = payload
  const index = content.value.indexOf(original)

  if (index === -1) {
    ElMessage.warning('原文已被修改，请手动替换')
    return
  }

  const secondIndex = content.value.indexOf(original, index + 1)
  if (secondIndex !== -1) {
    ElMessage.warning('存在多处相同内容，已替换第一处')
  }

  content.value = content.value.substring(0, index) + replacement + content.value.substring(index + original.length)
  ElMessage.success('替换成功')
}

function formatBudget(yuan?: number): string {
  return formatBudgetWanYuan(yuan)
}

// ---- 检测高亮 ----
async function loadDetectionHighlights(reqId: number) {
  try {
    const records = await requirementApi.getDetectionRecords(reqId)
    if (!records || records.length === 0) return

    // 每种检测类型只取最新记录（与 RequirementDetect 逻辑一致）
    const latestByType = new Map<string, RequirementDetectionRecord>()
    for (const record of records) {
      const existing = latestByType.get(record.detectionType)
      if (!existing || record.id > existing.id) {
        latestByType.set(record.detectionType, record)
      }
    }

    const allIssues: DetectionIssueVO[] = []
    for (const record of latestByType.values()) {
      if (record.status !== 'COMPLETED' || !record.result) continue
      const typeLabel = record.detectionType === 'TYPO' ? '错别字检查'
        : record.detectionType === 'SENSITIVE_WORD' ? '敏感词检测' : record.detectionType
      const parsed = parseDetectionIssues(record, typeLabel)
      allIssues.push(...parsed)
    }
    detectionIssues.value = allIssues
  } catch {
    // 无检测记录，不处理
  }
}

function parseDetectionIssues(record: RequirementDetectionRecord, typeName: string): DetectionIssueVO[] {
  try {
    const result = JSON.parse(record.result!)
    if (!Array.isArray(result.issues)) return []
    return result.issues.map((issue: any, i: number) => ({
      recordId: issue.recordId || result.recordId || record.id,
      detectionType: record.detectionType,
      typeName,
      description: issue.reason || issue.description || '',
      location: issue.position || issue.location || '',
      original: issue.original || '',
      targeted: issue.targeted || '',
      suggestion: issue.suggestion || '',
      severity: issue.severity || 'MEDIUM',
      handleStatus: issue.handleStatus ?? 0,
      issueIndex: i,
    }))
  } catch {
    return []
  }
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
  position: relative;
}

.content-preview {
  padding: 16px;
}

.content-editor {
  min-height: 400px;
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
  border-radius: 8px;
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

@keyframes generation-spin {
  to { transform: rotate(360deg); }
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
