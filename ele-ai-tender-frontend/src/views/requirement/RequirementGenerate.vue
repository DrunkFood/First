<template>
  <div class="requirement-generate">
    <!-- 顶部工具栏 -->
    <div class="gen-toolbar">
      <div class="toolbar-left">
        <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
        <el-divider direction="vertical" />
        <span class="toolbar-title">{{ requirementName || 'AI生成需求' }}</span>
        <StatusBadge
          v-if="requirementStatus"
          :status="requirementStatus"
          :type-map="REQUIREMENT_STATUS_MAP"
        />
      </div>
      <div class="toolbar-right">
        <el-button :loading="saving" @click="handleSave">保存</el-button>
        <el-button
          type="primary"
          :disabled="requirementStatus !== 'GENERATING'"
          @click="handleSubmitReview"
        >
          提交审核
        </el-button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="gen-body">
      <!-- 左侧内容区 -->
      <div class="gen-main">
        <!-- 内容工具栏 -->
        <div class="content-toolbar">
          <el-button
            type="success"
            :loading="generating"
            @click="handleGenerate"
          >
            {{ generating ? '生成中...' : 'AI生成' }}
          </el-button>
          <el-button
            :type="editMode ? 'default' : 'primary'"
            @click="editMode = !editMode"
          >
            {{ editMode ? '切换预览' : '切换编辑' }}
          </el-button>
        </div>

        <!-- 内容展示区 -->
        <div class="content-area">
          <MdPreview
            v-if="!editMode"
            :model-value="content"
            class="content-preview"
          />
          <MarkdownEditor
            v-else
            v-model="content"
            class="content-editor"
          />
        </div>
      </div>

      <!-- 右侧AI对话面板 -->
      <transition name="slide">
        <div v-show="chatVisible" class="gen-chat">
          <AiChatPanel
            :context="content"
            :requirement-id="requirementId"
            v-model:messages="chatMessages"
            @close="chatVisible = false"
            @feedback="handleChatFeedback"
            @message="handleAiMessage"
          />
        </div>
      </transition>
    </div>

    <!-- AI对话浮动按钮（面板折叠时显示） -->
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
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ChatDotRound } from '@element-plus/icons-vue'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { requirementApi } from '@/api/requirement'
import { createSSEConnection } from '@/api/ai'
import MarkdownEditor from '@/components/editor/MarkdownEditor.vue'
import AiChatPanel from '@/components/ai/AiChatPanel.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { REQUIREMENT_STATUS_MAP } from '@/constants/status-maps'
import type { AiChatMessage } from '@/types/ai'

const router = useRouter()
const route = useRoute()

// ---- 基础数据 ----
const requirementId = ref(0)
const requirementName = ref('')
const requirementStatus = ref('')
const content = ref('')

// ---- 状态 ----
const saving = ref(false)
const generating = ref(false)
const editMode = ref(false)
const chatVisible = ref(true)
const chatMessages = ref<AiChatMessage[]>([])

// ---- SSE关闭函数 ----
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
    requirementStatus.value = data.status || ''
    content.value = data.content || ''
  } catch {
    ElMessage.error('加载需求失败')
    router.back()
  }
})

onBeforeUnmount(() => {
  closeGenerateSSE?.()
})

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

// ---- AI生成 ----
function handleGenerate() {
  if (generating.value) {
    closeGenerateSSE?.()
    generating.value = false
    return
  }

  generating.value = true
  editMode.value = false

  closeGenerateSSE = createSSEConnection(
    `/core-api/v1/requirements/${requirementId.value}/generate`,
    {},
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

// ---- 提交审核 ----
async function handleSubmitReview() {
  try {
    await ElMessageBox.confirm(
      '确定要提交审核吗？提交后内容将无法修改。',
      '提交审核',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }

  try {
    await requirementApi.update(requirementId.value, { status: 'PENDING_REVIEW' })
    ElMessage.success('已提交审核')
    router.push('/requirement')
  } catch {
    ElMessage.error('提交审核失败')
  }
}

// ---- AI对话回调 ----
function handleChatFeedback(type: 'like' | 'dislike', _index: number) {
  ElMessage.success(type === 'like' ? '感谢您的反馈' : '我们会持续改进')
}

function handleAiMessage(msg: string) {
  if (!msg.trim()) return
  content.value += '\n\n' + msg
  ElMessage.info('AI建议已追加到内容末尾，请查看并编辑')
}
</script>

<style scoped>
.requirement-generate {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f7fa;
}

/* ---- 顶部工具栏 ---- */
.gen-toolbar {
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
.gen-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.gen-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 12px;
}

/* ---- 内容工具栏 ---- */
.content-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

/* ---- 内容展示区 ---- */
.content-area {
  flex: 1;
  overflow: auto;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.content-preview {
  padding: 20px 24px;
  min-height: 400px;
}

.content-editor {
  min-height: 400px;
}

/* ---- 右侧AI对话面板 ---- */
.gen-chat {
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

/* ---- AI对话浮动按钮 ---- */
.chat-fab {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 48px;
  height: 48px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 100;
}

/* 浮动按钮淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
