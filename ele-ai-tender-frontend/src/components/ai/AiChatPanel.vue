<template>
  <div class="ai-chat-panel">
    <!-- 顶部标题栏 -->
    <div class="chat-header">
      <span class="chat-title">AI助手</span>
      <el-button v-if="showClose" :icon="Close" text @click="emit('close')" />
    </div>

    <!-- 安全提示 -->
    <el-alert
      title="AI助手接入互联网，若有涉密信息请勿发送"
      type="warning"
      :closable="false"
      show-icon
      class="chat-alert"
    />

    <!-- 消息列表 -->
    <div ref="messageListRef" class="chat-messages">
      <!-- 欢迎语（消息为空时显示） -->
      <div v-if="messages.length === 0 && greeting" class="chat-message message-assistant">
        <div class="message-bubble">
          <div class="message-content">{{ greeting }}</div>
        </div>
      </div>
      <div
        v-for="(msg, index) in messages"
        :key="index"
        :class="['chat-message', msg.role === 'user' ? 'message-user' : 'message-assistant']"
      >
        <div class="message-bubble">
          <!-- AI正在输出时的打字动画 -->
          <template v-if="msg.role === 'assistant' && !msg.content && sending">
            <div class="typing-indicator">
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
            </div>
          </template>
          <template v-else-if="msg.role === 'assistant' && msg.content && sending && index === messages.length - 1">
            <div class="message-content">{{ msg.content }}</div>
            <span class="output-cursor">▌</span>
          </template>
          <template v-else>
            <div class="message-content">{{ msg.content }}</div>
          </template>
          <!-- AI消息失败标记 -->
          <div v-if="msg.role === 'assistant' && msg.error" class="message-error">
            AI对话失败，请稍后重试
          </div>
          <!-- AI消息反馈按钮（非发送中且有内容时才显示） -->
          <div v-if="msg.role === 'assistant' && msg.content && !sending" class="message-actions">
            <el-button
              :icon="CircleCheck"
              text
              size="small"
              @click="emit('feedback', 'like', index)"
            />
            <el-button
              :icon="CircleClose"
              text
              size="small"
              @click="emit('feedback', 'dislike', index)"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 底部输入区 -->
    <div class="chat-input">
      <!-- 快捷操作插槽（发送中时禁用） -->
      <div :class="{ 'is-disabled': sending }">
        <slot name="quick-actions" />
      </div>
      <div class="chat-input-row">
        <el-input
          v-model="inputText"
          placeholder="请输入您的修改需求..."
          :disabled="sending"
          @keyup.enter="handleSend"
        />
        <el-button
          type="primary"
          :icon="Promotion"
          :disabled="!inputText.trim() || sending"
          @click="handleSend"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onBeforeUnmount } from 'vue'
import { Close, CircleCheck, CircleClose, Promotion } from '@element-plus/icons-vue'
import { aiApi, createSSEConnection } from '@/api/ai'
import type { AiChatMessage } from '@/types/ai'

const props = defineProps<{
  context?: string
  projectId?: number
  requirementId?: number
  showClose?: boolean
  greeting?: string
}>()

const messages = defineModel<AiChatMessage[]>('messages', { default: () => [] })

const emit = defineEmits<{
  close: []
  feedback: [type: 'like' | 'dislike', index: number]
  message: [content: string]
}>()

const inputText = ref('')
const sending = ref(false)
const messageListRef = ref<HTMLDivElement>()
let closeSSE: (() => void) | null = null

/** 滚动到底部 */
function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

/** 发送消息 */
function handleSend() {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  // 添加用户消息
  const userMsg: AiChatMessage = { role: 'user', content: text, timestamp: Date.now() }
  messages.value.push(userMsg)
  inputText.value = ''
  scrollToBottom()

  // 准备AI占位消息
  const aiMsg: AiChatMessage = { role: 'assistant', content: '', timestamp: Date.now() }
  messages.value.push(aiMsg)
  const aiIndex = messages.value.length - 1

  sending.value = true
  emit('message', text)

  // 发起SSE连接
  closeSSE = createSSEConnection(
    aiApi.chatUrl,
    {
      message: text,
      context: props.context,
      projectId: props.projectId,
      requirementId: props.requirementId,
    },
    (data: string) => {
      // 流式追加内容
      const current = messages.value[aiIndex]!
      messages.value.splice(aiIndex, 1, {
        role: current.role,
        content: current.content + data,
        timestamp: current.timestamp,
      })
      scrollToBottom()
    },
    () => {
      // 错误处理
      const current = messages.value[aiIndex]
      if (current) {
        messages.value.splice(aiIndex, 1, {
          role: current.role,
          content: current.content || '',
          timestamp: current.timestamp,
          error: true,
        })
      }
      sending.value = false
      closeSSE = null
    },
    () => {
      // 完成回调
      sending.value = false
      closeSSE = null
    },
  )

  scrollToBottom()
}

/** 组件卸载时关闭SSE连接 */
onBeforeUnmount(() => {
  if (closeSSE) {
    closeSSE()
    closeSSE = null
  }
})
</script>

<style scoped>
.ai-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--app-bg-elevated);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--app-border-medium);
  flex-shrink: 0;
}

.chat-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-primary);
}

.chat-alert {
  flex-shrink: 0;
  margin: 8px 12px 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.chat-message {
  margin-bottom: 12px;
  display: flex;
}

.message-user {
  justify-content: flex-end;
}

.message-assistant {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 75%;
  border-radius: var(--app-radius-sm);
  padding: 10px 14px;
  word-break: break-word;
}

.message-user .message-bubble {
  background: var(--app-brand-color);
  color: #fff;
}

.message-assistant .message-bubble {
  background: var(--app-bg-tertiary);
  color: var(--app-text-primary);
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.6;
}

.message-actions {
  display: flex;
  gap: 4px;
  margin-top: 6px;
  justify-content: flex-end;
}

.message-assistant .message-actions :deep(.el-button) {
  color: var(--app-text-tertiary);
  padding: 2px 4px;
}

.message-assistant .message-actions :deep(.el-button:hover) {
  color: var(--app-brand-color);
}

.chat-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--app-border-medium);
  flex-shrink: 0;
}

.chat-input-row {
  display: flex;
  gap: 8px;
}

/* ---- 打字动画指示器 ---- */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
}

.typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--app-text-tertiary);
  animation: typing-bounce 1.4s ease-in-out infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.16s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes typing-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

/* ---- 输出光标 ---- */
.output-cursor {
  color: var(--app-brand-color);
  animation: cursor-blink 1s step-end infinite;
}

@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* ---- 失败消息 ---- */
.message-error {
  font-size: 12px;
  color: var(--app-color-danger, #EF4444);
  margin-top: 4px;
}

/* ---- 禁用快捷操作 ---- */
.is-disabled {
  pointer-events: none;
  opacity: 0.5;
}
</style>
