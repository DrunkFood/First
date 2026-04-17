<template>
  <div class="ai-chat-panel">
    <!-- 顶部标题栏（含安全提示） -->
    <div class="chat-header">
      <h3 class="chat-title">AI助手</h3>
      <el-button v-if="showClose" :icon="Close" text @click="emit('close')" />
      <div class="chat-warning">AI助手接入互联网，若有涉密信息请勿发送</div>
    </div>

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
            <template v-if="chatFeedbackMap[msg.uid]">
              <span class="feedback-indicator">
                {{ chatFeedbackMap[msg.uid] === 'LIKE' ? '已赞' : '已反馈不满意' }}
              </span>
            </template>
            <template v-else>
              <el-button
                :icon="CircleCheck"
                text
                size="small"
                @click="handleFeedback('like', msg)"
              />
              <el-button
                :icon="CircleClose"
                text
                size="small"
                @click="handleFeedback('dislike', msg)"
              />
            </template>
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
import { ref, reactive, nextTick, onBeforeUnmount } from 'vue'
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
  feedback: [type: 'like' | 'dislike', msg: AiChatMessage]
  message: [content: string]
}>()

const inputText = ref('')
const sending = ref(false)
const messageListRef = ref<HTMLDivElement>()
let closeSSE: (() => void) | null = null

/** 每条聊天消息的反馈状态：uid → LIKE/DISLIKE */
const chatFeedbackMap = reactive<Record<string, 'LIKE' | 'DISLIKE'>>({})

/** 生成消息唯一标识 */
function generateUid(role: string, timestamp: number, content?: string): string {
  const raw = `${role}-${timestamp}-${(content || '').substring(0, 50)}`
  let hash = 0
  for (let i = 0; i < raw.length; i++) {
    hash = ((hash << 5) - hash) + raw.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash).toString(36)
}

/** 处理聊天消息反馈（记录状态后冒泡给父组件） */
function handleFeedback(type: 'like' | 'dislike', msg: AiChatMessage) {
  if (chatFeedbackMap[msg.uid]) return
  chatFeedbackMap[msg.uid] = type === 'like' ? 'LIKE' : 'DISLIKE'
  emit('feedback', type, msg)
}

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
  const userMsg: AiChatMessage = { role: 'user', content: text, timestamp: Date.now(), uid: generateUid('user', Date.now(), text) }
  messages.value.push(userMsg)
  inputText.value = ''
  scrollToBottom()

  // 准备AI占位消息
  const aiMsgTimestamp = Date.now()
  const aiMsg: AiChatMessage = { role: 'assistant', content: '', timestamp: aiMsgTimestamp, uid: generateUid('assistant', aiMsgTimestamp) }
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
      const newContent = current.content + data
      messages.value.splice(aiIndex, 1, {
        role: current.role,
        content: newContent,
        timestamp: current.timestamp,
        uid: generateUid('assistant', current.timestamp, newContent),
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
          uid: generateUid('assistant', current.timestamp, current.content),
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

<style scoped lang="scss">
.ai-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--app-bg-secondary);
}

// ========================================
// 头部（标题 + 安全提示）
// ========================================
.chat-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--app-border-light);
  background: var(--app-bg-tertiary);
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.chat-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 0;
  flex: 1;
}

.chat-warning {
  width: 100%;
  font-size: 11px;
  color: var(--app-color-warning, #F59E0B);
  background: rgba(245, 158, 11, 0.1);
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid rgba(245, 158, 11, 0.2);
}

// ========================================
// 消息区
// ========================================
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
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
  max-width: 85%;
  border-radius: 8px;
  padding: 12px 16px;
  word-break: break-word;
}

.message-user .message-bubble {
  background: var(--app-brand-color);
  color: #fff;
}

.message-assistant .message-bubble {
  background: var(--app-bg-elevated);
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
  align-items: center;
}

.feedback-indicator {
  font-size: 12px;
  color: var(--app-brand-color);
  font-weight: 500;
}

.message-assistant .message-actions :deep(.el-button) {
  color: var(--app-text-tertiary);
  padding: 2px 4px;
}

.message-assistant .message-actions :deep(.el-button:hover) {
  color: var(--app-brand-color);
}

// ========================================
// 输入区
// ========================================
.chat-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 20px;
  border-top: 1px solid var(--app-border-light);
  background: var(--app-bg-tertiary);
  flex-shrink: 0;
}

.chat-input-row {
  display: flex;
  gap: 8px;
}

// ========================================
// 打字动画指示器
// ========================================
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

// ========================================
// 输出光标
// ========================================
.output-cursor {
  color: var(--app-brand-color);
  animation: cursor-blink 1s step-end infinite;
}

@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

// ========================================
// 失败消息
// ========================================
.message-error {
  font-size: 12px;
  color: var(--app-color-danger, #EF4444);
  margin-top: 4px;
}

// ========================================
// 禁用快捷操作
// ========================================
.is-disabled {
  pointer-events: none;
  opacity: 0.5;
}
</style>
