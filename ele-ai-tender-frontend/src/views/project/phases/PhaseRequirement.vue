<template>
  <div class="phase-requirement">
    <AiUnavailableAlert
      :visible="isAiUnavailable"
      @retry="handleRetry"
      @skip="handleSkipAi"
    />

    <div class="requirement-toolbar">
      <el-button type="primary" :loading="isGenerating" @click="handleGenerate">
        AI 生成需求
      </el-button>
      <el-button type="warning" :loading="isOptimizing" @click="handleOptimize">
        文本优化
      </el-button>
      <AiTaskStatus
        v-if="task"
        :task="task"
        :show-actions="true"
        @retry="handleRetry"
        @skip="handleSkipAi"
      />
      <span v-if="isSaving" class="auto-save-hint">自动保存中...</span>
      <span v-else-if="lastSaveTime" class="auto-save-hint">上次自动保存: {{ lastSaveTime }}</span>

      <!-- AI对话面板切换按钮 -->
      <el-button
        :type="chatVisible ? 'primary' : 'default'"
        @click="chatVisible = !chatVisible"
        class="chat-toggle-btn"
      >
        AI助手
      </el-button>
    </div>

    <div class="requirement-body">
      <div class="editor-area" :class="{ 'editor-shrink': chatVisible }">
        <MarkdownEditor v-model="content" />
      </div>

      <!-- AI对话面板 - 右侧可折叠 -->
      <transition name="slide">
        <div v-if="chatVisible" class="chat-panel-wrapper">
          <AiChatPanel
            :context="content"
            :project-id="projectId"
            :requirement-id="requirementId"
            v-model:messages="chatMessages"
            @close="chatVisible = false"
          />
        </div>
      </transition>
    </div>

    <!-- 浮动切换按钮 - 面板折叠时显示 -->
    <div v-if="!chatVisible" class="chat-fab" @click="chatVisible = true">
      AI助手
    </div>

    <div class="phase-actions">
      <el-button @click="$emit('prev')">上一步</el-button>
      <el-button type="primary" @click="handleSaveAndNext">保存并继续</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { requirementApi } from '@/api/requirement'
import { projectApi } from '@/api/project'
import { aiApi, createSSEConnection } from '@/api/ai'
import { useTaskPolling } from '@/composables/useTaskPolling'
import { useAutoSave } from '@/composables/useAutoSave'
import AiTaskStatus from '@/components/AiTaskStatus.vue'
import AiUnavailableAlert from '@/components/AiUnavailableAlert.vue'
import AiChatPanel from '@/components/ai/AiChatPanel.vue'
import MarkdownEditor from '@/components/editor/MarkdownEditor.vue'
import type { AiChatMessage } from '@/types/ai'

const props = defineProps<{ projectId: number }>()
const emit = defineEmits<{ next: []; prev: [] }>()

const content = ref('')
const requirementId = ref(0)
const taskId = ref<number | null>(null)
const isGenerating = ref(false)
const isOptimizing = ref(false)

// AI对话面板状态
const chatVisible = ref(false)
const chatMessages = ref<AiChatMessage[]>([])
let closeOptimizeSSE: (() => void) | null = null

const { task, retry: retryTask, skip: skipTask } = useTaskPolling(taskId)

const { isSaving, lastSaveTime, startAutoSave, recoverDraft } = useAutoSave(
  requirementId,
  content,
  (id, data) => requirementApi.autoSave(id, data.content),
  (id) => requirementApi.getAutoSave(id),
  (id) => requirementApi.clearAutoSave(id),
)

const isAiUnavailable = computed(() => task.value?.status === 'AI_UNAVAILABLE')

const loadData = async () => {
  const project = await projectApi.getById(props.projectId)
  if (project.requirementId) {
    requirementId.value = project.requirementId
    const req = await requirementApi.getById(project.requirementId)
    content.value = req.content || ''
    await recoverDraft()
    startAutoSave()
  }
}

const handleGenerate = async () => {
  if (!requirementId.value) return
  isGenerating.value = true
  try {
    const res = await requirementApi.generate(requirementId.value, {})
    taskId.value = res.id
  } catch {
    ElMessage.error('提交AI生成失败')
  } finally {
    isGenerating.value = false
  }
}

/** 文本优化 - SSE流式 */
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
      // 错误处理
      if (!optimizedContent) {
        content.value = originalContent
        ElMessage.error('文本优化失败')
      }
      isOptimizing.value = false
      closeOptimizeSSE = null
    },
    () => {
      // 完成回调
      isOptimizing.value = false
      closeOptimizeSSE = null
      ElMessage.success('文本优化完成')
    },
  )
}

const handleRetry = () => retryTask()
const handleSkipAi = () => skipTask()

const handleSaveAndNext = async () => {
  if (requirementId.value && content.value) {
    await requirementApi.update(requirementId.value, { content: content.value })
  }
  ElMessage.success('需求内容保存成功')
  emit('next')
}

onBeforeUnmount(() => {
  if (closeOptimizeSSE) {
    closeOptimizeSSE()
    closeOptimizeSSE = null
  }
})

onMounted(loadData)
</script>

<style scoped>
.requirement-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.auto-save-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.chat-toggle-btn {
  margin-left: auto;
}

.requirement-body {
  display: flex;
  gap: 0;
  min-height: 400px;
}

.editor-area {
  flex: 1;
  min-width: 0;
  transition: flex 0.3s ease;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
}

.editor-area.editor-shrink {
  flex: 1;
}

.chat-panel-wrapper {
  width: 380px;
  flex-shrink: 0;
  border: 1px solid var(--el-border-color);
  border-left: none;
  border-radius: 0 4px 4px 0;
  height: 500px;
  overflow: hidden;
}

/* 侧滑动画 */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  width: 0;
  opacity: 0;
}

/* 浮动切换按钮 */
.chat-fab {
  position: fixed;
  right: 24px;
  bottom: 80px;
  width: 48px;
  padding: 12px 0;
  text-align: center;
  background: var(--el-color-primary);
  color: #fff;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  writing-mode: vertical-rl;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.2s;
  z-index: 100;
  user-select: none;
}

.chat-fab:hover {
  background: var(--el-color-primary-dark-2);
  transform: scale(1.05);
}

.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
