<template>
  <div class="phase-requirement">
    <!-- 生成状态卡片 -->
    <div v-if="latestTask" class="generation-status-card">
      <div class="status-left">
        <el-icon v-if="!canCreateNew" size="32" class="spinning" color="var(--app-brand-color)"><Loading /></el-icon>
        <el-icon v-else-if="latestTask.status === 'COMPLETED'" size="32" color="var(--app-color-success)"><CircleCheck /></el-icon>
        <el-icon v-else size="32" color="var(--app-brand-color)"><Document /></el-icon>
      </div>
      <div class="status-right">
        <h4>{{ getStatusTitle }}</h4>
        <el-progress
          :percentage="progressPercent"
          :stroke-width="8"
          :status="progressStatus"
          style="width: 300px"
        />
        <span v-if="latestTask.errorMsg" class="status-hint error-hint">{{ latestTask.errorMsg }}</span>
        <span v-else-if="!canCreateNew" class="status-hint">AI正在生成招标需求内容，请稍候...</span>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="requirement-toolbar">
      <el-button type="primary" :disabled="!canCreateNew" :loading="!canCreateNew" @click="handleGenerate">
        AI 生成需求
      </el-button>
      <el-button type="warning" :loading="isOptimizing" @click="handleOptimize">
        文本优化
      </el-button>
      <div style="flex: 1" />
      <span v-if="isSaving" class="auto-save-hint">自动保存中...</span>
      <span v-else-if="lastSaveTime" class="auto-save-hint">上次自动保存: {{ lastSaveTime }}</span>
    </div>

    <!-- 编辑器 -->
    <div class="requirement-body">
      <div class="editor-area">
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
    </div>

    <!-- AI助手侧边栏 -->
    <div :class="['ai-sidebar', { collapsed: !chatVisible }]">
      <button class="ai-toggle-btn" @click="chatVisible = !chatVisible" :title="chatVisible ? '收起AI助手' : '展开AI助手'">
        <el-icon :size="18">
          <component :is="chatVisible ? Close : ChatDotRound" />
        </el-icon>
      </button>
      <div v-if="chatVisible" class="ai-sidebar-content">
        <AiChatPanel
          show-close
          greeting="您好！我是您的AI助手，可以帮助您修改招标需求内容。请选择快捷操作或输入您的修改需求。"
          :context="content"
          :project-id="projectId"
          :requirement-id="requirementId"
          v-model:messages="chatMessages"
          @close="chatVisible = false"
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
        </AiChatPanel>
      </div>
    </div>

    <!-- 底部操作 -->
    <div class="phase-actions">
      <el-button @click="$emit('prev')">上一步</el-button>
      <el-button type="warning" plain :disabled="!canCreateNew" @click="handleGenerate">重新生成章节</el-button>
      <div style="flex: 1" />
      <el-button @click="handleSave">保存编辑</el-button>
      <el-button type="primary" @click="handleSaveAndNext">确认需求</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, CircleCheck, Document, ChatDotRound, Close } from '@element-plus/icons-vue'
import { requirementApi } from '@/api/requirement'
import { projectApi } from '@/api/project'
import { aiApi, createSSEConnection } from '@/api/ai'
import { useLatestTask } from '@/composables/useLatestTask'
import { useAutoSave } from '@/composables/useAutoSave'
import { getTaskProgress, getProgressStatus } from '@/types/ai-task'
import AiChatPanel from '@/components/ai/AiChatPanel.vue'
import MarkdownEditor from '@/components/editor/MarkdownEditor.vue'
import type { AiChatMessage } from '@/types/ai'

const props = defineProps<{ projectId: number }>()
const emit = defineEmits<{ next: []; prev: [] }>()

const content = ref('')
const requirementId = ref(0)
const isOptimizing = ref(false)
const aiFeedbackType = ref<'like' | 'dislike' | null>(null)

const chatVisible = ref(false)
const chatMessages = ref<AiChatMessage[]>([])
let closeOptimizeSSE: (() => void) | null = null

const { latestTask, canCreateNew, setActive, refresh } = useLatestTask(
  'REQUIREMENT_GENERATE',
  requirementId,
  'REQUIREMENT',
)

const { isSaving, lastSaveTime, startAutoSave, recoverDraft } = useAutoSave(
  requirementId,
  content,
  (id, data) => requirementApi.autoSave(id, data.content),
  (id) => requirementApi.getAutoSave(id),
  (id) => requirementApi.clearAutoSave(id),
)

const getStatusTitle = computed(() => {
  if (!latestTask.value) return 'AI生成需求'
  const status = latestTask.value.status
  if (status === 'PENDING') return '任务排队中...'
  if (status === 'PROCESSING') return '正在生成中...'
  if (status === 'COMPLETED') return '生成完成'
  if (status === 'FAILED') return '生成失败'
  if (status === 'AI_UNAVAILABLE') return 'AI服务不可用'
  if (status === 'SKIPPED') return '已跳过'
  return 'AI生成需求'
})

const progressPercent = computed(() => {
  if (!latestTask.value) return 0
  return getTaskProgress(latestTask.value.status)
})

const progressStatus = computed(() => {
  if (!latestTask.value) return ''
  return getProgressStatus(latestTask.value.status)
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
  // 提交前刷新最新任务状态，确保校验是最新的
  await refresh()
  if (!canCreateNew.value) {
    ElMessage.warning('AI生成任务正在处理中，请稍候')
    return
  }
  try {
    const res = await requirementApi.generate(requirementId.value, {})
    setActive(res.id)
  } catch (e: any) {
    if (e?.code === 8084) {
      ElMessage.warning('AI生成任务正在处理中，请稍候')
      refresh()
      return
    }
    ElMessage.error('提交AI生成失败')
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

const handleChatFeedback = (type: 'like' | 'dislike', _index: number) => {
  ElMessage.success(type === 'like' ? '感谢反馈！' : '我们会持续改进AI生成质量')
}

const handleChatMessage = (_msg: string) => {
  // 消息已通过 v-model 同步到 chatMessages
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

.error-hint {
  color: var(--app-color-danger);
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

// 编辑器
.requirement-body {
  flex: 1;
  min-height: 400px;
}

.editor-area {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--app-border-light);
  border-radius: var(--app-radius-sm);
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
  flex-direction: column;
  gap: 6px;
}

.quick-action-btn {
  width: 100%;
  padding: 8px 12px;
  background: var(--app-bg-elevated);
  border: 1px solid var(--app-border-light);
  border-radius: 6px;
  color: var(--app-text-primary);
  font-size: 13px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--app-hover-state, rgba(51, 108, 255, 0.12));
    border-color: var(--app-brand-color);
    color: var(--app-brand-color);
  }
}

// AI助手侧边栏
.ai-sidebar {
  position: fixed;
  right: 0;
  top: 80px;
  width: 340px;
  height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
  z-index: 100;
  transition: transform 0.3s ease;

  &.collapsed {
    transform: translateX(100%);
  }
}

.ai-toggle-btn {
  position: absolute;
  left: -44px;
  top: 20px;
  width: 44px;
  height: 44px;
  background: var(--app-brand-color);
  color: white;
  border: none;
  border-radius: 8px 0 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: -2px 2px 8px rgba(0, 0, 0, 0.15);

  &:hover {
    background: #2855d9;
  }
}

.ai-sidebar-content {
  flex: 1;
  overflow: hidden;
  background: var(--app-bg-secondary);
  border-left: 1px solid var(--app-border-light);
  border-top: 1px solid var(--app-border-light);
  border-bottom: 1px solid var(--app-border-light);
  border-radius: 8px 0 0 8px;
  box-shadow: -4px 0 16px rgba(0, 0, 0, 0.1);
}

// 底部操作
.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
