<template>
  <div class="phase-requirement">
    <AiUnavailableAlert
      :visible="isAiUnavailable"
      @retry="handleRetry"
      @skip="handleSkipAi"
    />

    <!-- 生成状态卡片 -->
    <div v-if="isGenerating || task" class="generation-status-card">
      <div class="status-left">
        <el-icon v-if="isGenerating" size="32" class="spinning" color="var(--app-brand-color)"><Loading /></el-icon>
        <el-icon v-else-if="task?.status === 'COMPLETED'" size="32" color="var(--app-color-success)"><CircleCheck /></el-icon>
        <el-icon v-else size="32" color="var(--app-brand-color)"><Document /></el-icon>
      </div>
      <div class="status-right">
        <h4>{{ isGenerating ? '正在生成中...' : task?.status === 'COMPLETED' ? '生成完成' : 'AI生成需求' }}</h4>
        <el-progress
          :percentage="generationProgress"
          :stroke-width="8"
          :status="isGenerating ? '' : 'success'"
          style="width: 300px"
        />
        <span v-if="isGenerating" class="status-hint">AI正在生成招标需求内容，请稍候...</span>
      </div>
    </div>

    <!-- 工具栏 -->
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

      <el-button
        :type="chatVisible ? 'primary' : 'default'"
        @click="chatVisible = !chatVisible"
        class="chat-toggle-btn"
      >
        AI助手
      </el-button>
    </div>

    <!-- 编辑器+AI助手 -->
    <div class="requirement-body">
      <div class="editor-area" :class="{ 'editor-shrink': chatVisible }">
        <MarkdownEditor v-model="content" />

        <!-- AI内容反馈 -->
        <div v-if="content" class="ai-feedback">
          <span class="feedback-label">对AI生成内容的评价：</span>
          <el-button
            :type="aiFeedbackType === 'like' ? 'success' : 'default'"
            :icon="aiFeedbackType === 'like' ? '✓' : undefined"
            size="small"
            @click="handleFeedback('like')"
          >
            有帮助
          </el-button>
          <el-button
            :type="aiFeedbackType === 'dislike' ? 'danger' : 'default'"
            :icon="aiFeedbackType === 'dislike' ? '✗' : undefined"
            size="small"
            @click="handleFeedback('dislike')"
          >
            需改进
          </el-button>
        </div>
      </div>

      <!-- AI对话面板 -->
      <transition name="slide">
        <div v-if="chatVisible" class="chat-panel-wrapper">
          <AiChatPanel
            :context="content"
            :project-id="projectId"
            :requirement-id="requirementId"
            v-model:messages="chatMessages"
            @close="chatVisible = false"
          >
            <template #quick-actions>
              <div class="quick-actions">
                <el-button size="small" round @click="sendQuickAction('修改工程范围')">修改工程范围</el-button>
                <el-button size="small" round @click="sendQuickAction('修改技术要求')">修改技术要求</el-button>
                <el-button size="small" round @click="sendQuickAction('修改质量标准')">修改质量标准</el-button>
              </div>
            </template>
          </AiChatPanel>
        </div>
      </transition>
    </div>

    <!-- 浮动切换按钮 -->
    <div v-if="!chatVisible" class="chat-fab" @click="chatVisible = true">
      AI助手
    </div>

    <!-- 底部操作 -->
    <div class="phase-actions">
      <el-button @click="$emit('prev')">上一步</el-button>
      <el-button type="warning" plain @click="handleGenerate">重新生成章节</el-button>
      <div style="flex: 1" />
      <el-button @click="handleSave">保存编辑</el-button>
      <el-button type="primary" @click="handleSaveAndNext">确认需求</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, CircleCheck, Document } from '@element-plus/icons-vue'
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
const aiFeedbackType = ref<'like' | 'dislike' | null>(null)

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

const generationProgress = computed(() => {
  if (task.value?.status === 'COMPLETED') return 100
  if (isGenerating.value) return 65
  if (task.value?.status === 'PROCESSING') return 65
  return 0
})

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

const handleRetry = () => retryTask()
const handleSkipAi = () => skipTask()

const handleFeedback = (type: 'like' | 'dislike') => {
  aiFeedbackType.value = aiFeedbackType.value === type ? null : type
  ElMessage.success(type === 'like' ? '感谢反馈！' : '我们会持续改进AI生成质量')
}

const sendQuickAction = (action: string) => {
  chatMessages.value.push({
    role: 'user',
    content: action,
    timestamp: Date.now(),
  })
}

const handleSave = async () => {
  if (requirementId.value && content.value) {
    await requirementApi.update(requirementId.value, { content: content.value })
  }
  ElMessage.success('需求内容保存成功')
}

const handleSaveAndNext = async () => {
  await handleSave()
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

<style scoped lang="scss">
// 生成状态卡片
.generation-status-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--app-bg-secondary);
  border-radius: var(--app-radius-sm);
  margin-bottom: 16px;
  border: 1px solid var(--app-border-light);
}

.status-left {
  flex-shrink: 0;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.status-right {
  h4 {
    margin: 0 0 8px;
    color: var(--app-text-primary);
  }
}

.status-hint {
  font-size: 12px;
  color: var(--app-text-tertiary);
}

// 工具栏
.requirement-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.auto-save-hint {
  color: var(--app-text-tertiary);
  font-size: 12px;
}

.chat-toggle-btn {
  margin-left: auto;
}

// 编辑器
.requirement-body {
  display: flex;
  gap: 0;
  min-height: 400px;
}

.editor-area {
  flex: 1;
  min-width: 0;
  transition: flex 0.3s ease;
  border: 1px solid var(--app-border-light);
  border-radius: var(--app-radius-sm);
}

.chat-panel-wrapper {
  width: 380px;
  flex-shrink: 0;
  border: 1px solid var(--app-border-light);
  border-left: none;
  border-radius: 0 var(--app-radius-sm) var(--app-radius-sm) 0;
  height: 500px;
  overflow: hidden;
}

// AI反馈
.ai-feedback {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--app-bg-secondary);
  border-top: 1px solid var(--app-border-light);
}

.feedback-label {
  font-size: 13px;
  color: var(--app-text-tertiary);
  margin-right: 4px;
}

// 快速操作
.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 0;
}

// 侧滑动画
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  width: 0;
  opacity: 0;
}

// 浮动切换按钮
.chat-fab {
  position: fixed;
  right: 24px;
  bottom: 80px;
  width: 48px;
  padding: 12px 0;
  text-align: center;
  background: var(--app-brand-color);
  color: #fff;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  writing-mode: vertical-rl;
  box-shadow: var(--app-shadow-md);
  transition: all 0.2s;
  z-index: 100;
  user-select: none;

  &:hover {
    background: var(--app-brand-color-dark-2);
    transform: scale(1.05);
  }
}

// 底部操作
.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
