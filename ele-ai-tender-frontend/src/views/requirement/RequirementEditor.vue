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
      <!-- 左侧编辑区 -->
      <div class="editor-main">
        <div class="editor-name-row">
          <el-input
            v-model="requirementName"
            placeholder="请输入需求名称"
            class="name-input"
          />
        </div>
        <MarkdownEditor v-model="content" class="editor-content" />
      </div>

      <!-- 右侧AI对话面板 -->
      <transition name="slide">
        <div v-show="chatVisible" class="editor-chat">
          <AiChatPanel
            :context="content"
            :project-id="projectId"
            :requirement-id="requirementId"
            v-model:messages="chatMessages"
            @close="chatVisible = false"
            @feedback="handleChatFeedback"
            @message="handleChatMessage"
          />
        </div>
      </transition>
    </div>

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
      <div class="statusbar-right">
        <el-button
          text
          :icon="chatVisible ? Close : ChatDotRound"
          @click="chatVisible = !chatVisible"
        >
          {{ chatVisible ? '关闭AI对话' : 'AI对话' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Close, ChatDotRound } from '@element-plus/icons-vue'
import { requirementApi } from '@/api/requirement'
import { aiApi, createSSEConnection } from '@/api/ai'
import MarkdownEditor from '@/components/editor/MarkdownEditor.vue'
import AiChatPanel from '@/components/ai/AiChatPanel.vue'
import type { AiChatMessage } from '@/types/ai'

const router = useRouter()
const route = useRoute()

// ---- 基础数据 ----
const requirementId = ref(0)
const projectId = ref<number | undefined>(undefined)
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
    projectId.value = data.projectId
    originalContent.value = content.value
    originalName.value = requirementName.value
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
    { projectId: projectId.value },
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
      projectId: projectId.value,
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
function handleChatFeedback(type: 'like' | 'dislike', _index: number) {
  ElMessage.success(type === 'like' ? '感谢您的反馈' : '我们会持续改进')
}

function handleChatMessage(_msg: string) {
  // 消息已通过 v-model 同步到 chatMessages
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
  background: #f5f7fa;
}

/* ---- 顶部工具栏 ---- */
.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
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
  color: #303133;
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

/* ---- 右侧AI对话面板 ---- */
.editor-chat {
  width: 400px;
  flex-shrink: 0;
  border-left: 1px solid #e4e7ed;
  background: #fff;
  overflow: hidden;
}

/* 折叠动画 */
.slide-enter-active,
.slide-leave-active {
  transition: width 0.3s ease, opacity 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  width: 0;
  opacity: 0;
}

/* ---- 底部状态栏 ---- */
.editor-statusbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 16px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.status-saved {
  color: #67c23a;
}

.status-saving {
  color: #e6a23c;
}

.status-error {
  color: #f56c6c;
}

.status-idle {
  color: #c0c4cc;
}
</style>
