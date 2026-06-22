<template>
  <div class="requirement-editor">
    <!-- 顶部工具栏 -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
        <el-divider direction="vertical" />
        <span class="toolbar-title">编辑业务需求</span>
      </div>
      <div class="toolbar-right">
        <el-button :loading="saving" type="primary" @click="handleSave">
          保存
        </el-button>
        <el-button :loading="generating" type="success" @click="handleGenerate">
          AI生成
        </el-button>
        <el-button :loading="optimizing" type="warning" @click="handleOptimize">
          AI优化
        </el-button>
        <el-button type="danger" @click="handleDetect">
          提交检测
        </el-button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="editor-body">
      <!-- 编辑区 -->
      <div class="editor-main">
        <div class="editor-name-row">
          <el-input
            v-model="requirementName"
            placeholder="请输入需求名称"
            class="name-input"
          />
        </div>
        <WysiwygEditor
          v-model="content"
          class="editor-content"
          @selection-change="handleSelectionChange"
        />
      </div>
    </div>

    <!-- AI助手侧边栏 -->
    <AiAssistantSidebar
      ref="aiSidebarRef"
      v-model:visible="chatVisible"
      v-model:messages="chatMessages"
      greeting="您好！我是您的AI助手，可以帮助您优化和修改业务需求内容。请选择快捷操作或输入您的需求。"
      :context="content"
      :requirement-id="requirementId"
      :selected-text="selectedText"
      @feedback="handleChatFeedback"
      @message="handleChatMessage"
      @replace="handleReplace"
      @update:selected-text="selectedText = $event"
    >
      <template #quick-actions>
        <div class="quick-actions">
          <button class="quick-action-btn" @click="sendQuickAction('优化需求描述')">优化需求描述</button>
          <button class="quick-action-btn" @click="sendQuickAction('补充技术要求')">补充技术要求</button>
          <button class="quick-action-btn" @click="sendQuickAction('修改资格条件')">修改资格条件</button>
        </div>
      </template>
    </AiAssistantSidebar>

    <!-- 底部状态栏 -->
    <div class="editor-statusbar">
      <div class="statusbar-left">
        <span v-if="autoSaveStatus === 'saved'" class="status-saved">
          自动保存: 已保存 {{ lastSaveTime }}
        </span>
        <span v-else-if="autoSaveStatus === 'saving'" class="status-saving">
          自动保存: 保存中...
        </span>
        <span v-else-if="autoSaveStatus === 'error'" class="status-error">
          自动保存: 保存失败
        </span>
        <span v-else class="status-idle">
          自动保存: 未启用
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { requirementApi } from '@/api/requirement'
import { aiApi, createSSEConnection } from '@/api/ai'
import { aiTaskApi } from '@/api/ai-task'
import { useFeedback } from '@/composables/useFeedback'
import WysiwygEditor from '@/components/editor/WysiwygEditor.vue'
import AiAssistantSidebar from '@/components/ai/AiAssistantSidebar.vue'
import { applyAiReplacement } from '@/utils/aiReplacement'
import type { AiChatMessage } from '@/types/ai'

const router = useRouter()
const route = useRoute()

// ---- 基础数据 ----
const requirementId = ref(0)
const requirementName = ref('')
const content = ref('')
const originalContent = ref('')
const originalName = ref('')

// ---- 状态 ----
const saving = ref(false)
const generating = ref(false)
const optimizing = ref(false)
const chatVisible = ref(true)
const chatMessages = ref<AiChatMessage[]>([])
const selectedText = ref('')
const latestTaskId = ref<number | undefined>(undefined)

// ---- 反馈（仅聊天场景） ----
const {
  submitFeedback: submitChatFeedback,
} = useFeedback(
  'CHAT_MESSAGE',
  () => latestTaskId.value,
)

// ---- 自动保存 ----
type AutoSaveStatus = 'idle' | 'saved' | 'saving' | 'error'
const autoSaveStatus = ref<AutoSaveStatus>('idle')
const lastSaveTime = ref('')
let autoSaveTimer: ReturnType<typeof setInterval> | null = null
const AUTO_SAVE_INTERVAL = 2 * 60 * 1000 // 2分钟

// ---- SSE关闭函数 ----
let closeGenerateSSE: (() => void) | null = null
let closeOptimizeSSE: (() => void) | null = null

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
    content.value = data.content || ''
    originalContent.value = content.value
    originalName.value = requirementName.value

    // 获取最新AI任务ID，用于聊天反馈关联
    try {
      const task = await aiTaskApi.getLatestTask('REQUIREMENT_GENERATE', id, 'REQUIREMENT')
      if (task && task.id) {
        latestTaskId.value = task.id
      }
    } catch {
      // 忽略，反馈功能可选
    }
  } catch {
    ElMessage.error('加载需求失败')
    router.back()
    return
  }

  // 检查草稿恢复
  await checkAutoSaveDraft(id)

  // 启动自动保存
  startAutoSave()
})

onBeforeUnmount(() => {
  stopAutoSave()
  closeGenerateSSE?.()
  closeOptimizeSSE?.()
})

// ---- 保存 ----
async function handleSave() {
  saving.value = true
  try {
    await requirementApi.update(requirementId.value, {
      content: content.value,
      requirementName: requirementName.value,
    })
    originalContent.value = content.value
    originalName.value = requirementName.value
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// ---- AI生成 ----
function handleGenerate() {
  if (generating.value) {
    closeGenerateSSE?.()
    generating.value = false
    return
  }

  generating.value = true

  closeGenerateSSE = createSSEConnection(
    `/core-api/v1/requirements/${requirementId.value}/generate`,
    { },
    (data: string) => {
      content.value += data
    },
    () => {
      ElMessage.error('AI生成失败，请稍后重试')
      generating.value = false
      closeGenerateSSE = null
    },
    () => {
      ElMessage.success('AI生成完成')
      generating.value = false
      closeGenerateSSE = null
    },
  )
}

// ---- AI优化 ----
function handleOptimize() {
  if (!content.value.trim()) {
    ElMessage.warning('请先输入需要优化的内容')
    return
  }

  if (optimizing.value) {
    closeOptimizeSSE?.()
    optimizing.value = false
    return
  }

  optimizing.value = true
  const originalText = content.value
  content.value = ''

  closeOptimizeSSE = createSSEConnection(
    aiApi.optimizeUrl,
    {
      content: originalText,
      type: 'requirement',
      projectId: undefined,
    },
    (data: string) => {
      content.value += data
    },
    () => {
      ElMessage.error('AI优化失败，已恢复原始内容')
      content.value = originalText
      optimizing.value = false
      closeOptimizeSSE = null
    },
    () => {
      ElMessage.success('AI优化完成')
      optimizing.value = false
      closeOptimizeSSE = null
    },
  )
}

// ---- 提交检测 ----
function handleDetect() {
  router.push(`/requirement/detect/${requirementId.value}`)
}

// ---- AI对话回调 ----
async function handleChatFeedback(type: 'like' | 'dislike', msg: AiChatMessage) {
  await submitChatFeedback(type, {
    chatMessageId: msg.uid,
    chatContent: msg.content?.substring(0, 200),
  })
}

function handleChatMessage(_msg: string) {
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

const aiSidebarRef = ref<InstanceType<typeof AiAssistantSidebar>>()

function sendQuickAction(action: string) {
  aiSidebarRef.value?.sendQuickAction(action)
}

// ---- 自动保存 ----
function startAutoSave() {
  stopAutoSave()
  autoSaveTimer = setInterval(doAutoSave, AUTO_SAVE_INTERVAL)
}

function stopAutoSave() {
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
    autoSaveTimer = null
  }
}

async function doAutoSave() {
  if (!content.value && !requirementName.value) return

  autoSaveStatus.value = 'saving'
  try {
    await requirementApi.autoSave(requirementId.value, content.value)
    autoSaveStatus.value = 'saved'
    lastSaveTime.value = formatTime(new Date())
  } catch {
    autoSaveStatus.value = 'error'
  }
}

function formatTime(date: Date): string {
  const h = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

// ---- 草稿恢复 ----
async function checkAutoSaveDraft(id: number) {
  try {
    const draft = await requirementApi.getAutoSave(id)
    if (draft && draft.trim()) {
      await ElMessageBox.confirm(
        '检测到未保存的草稿内容，是否恢复？',
        '草稿恢复',
        { confirmButtonText: '恢复', cancelButtonText: '忽略', type: 'info' },
      )
      content.value = draft
      ElMessage.success('草稿已恢复')
    }
  } catch {
    // 用户选择忽略或接口异常，不做处理
  }
}
</script>

<style scoped>
.requirement-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--app-bg-secondary);
  transition: var(--app-transition-base);
}

/* ---- 顶部工具栏 ---- */
.editor-toolbar {
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
.editor-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 12px;
}

.editor-name-row {
  margin-bottom: 8px;
  flex-shrink: 0;
}

.name-input :deep(.el-input__inner) {
  font-size: 18px;
  font-weight: 600;
}

.editor-content {
  flex: 1;
  overflow: hidden;
}

/* ---- 底部状态栏 ---- */
.editor-statusbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 16px;
  background: var(--app-bg-elevated);
  border-top: 1px solid var(--app-border-medium);
  font-size: 12px;
  color: var(--app-text-tertiary);
  flex-shrink: 0;
}

.status-saved {
  color: var(--app-color-success);
}

.status-saving {
  color: var(--app-color-warning);
}

.status-error {
  color: var(--app-color-danger);
}

.status-idle {
  color: var(--app-text-tertiary);
}
</style>
