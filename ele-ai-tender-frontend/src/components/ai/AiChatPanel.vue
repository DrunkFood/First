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
            <div class="message-content markdown-body">
              <MdPreview :modelValue="msg.content" :theme="themeStore.mode" />
            </div>
            <span class="output-cursor">▌</span>
          </template>
          <template v-else-if="msg.role === 'assistant'">
            <div class="message-content markdown-body">
              <MdPreview :modelValue="msg.content" :theme="themeStore.mode" />
            </div>
          </template>
          <template v-else>
            <div class="message-content">
              <div v-if="msg.selectedText" class="user-quote">
                {{ msg.selectedText }}
              </div>
              <div>{{ msg.content }}</div>
            </div>
          </template>
          <!-- AI消息失败标记 -->
          <div v-if="msg.role === 'assistant' && msg.error" class="message-error">
            AI对话失败，请稍后重试
          </div>
          <!-- AI消息反馈按钮（欢迎语和发送中不显示） -->
          <div v-if="msg.role === 'assistant' && msg.content && !sending && !msg.isGreeting" class="message-actions">
            <template v-if="msg.uid && chatFeedbackMap[msg.uid]">
              <span class="feedback-indicator">
                {{ chatFeedbackMap[msg.uid!] === 'LIKE' ? '已赞' : '已反馈不满意' }}
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
          <!-- 替换操作栏 -->
          <div v-if="canShowReplace(index, msg)" class="replace-actions">
            <template v-if="getReplaceableContents(msg.content).length <= 1">
              <el-button size="small" type="primary" @click="handleReplace(index, msg)">
                应用替换
              </el-button>
            </template>
            <template v-else>
              <el-button
                v-for="(_replacement, replaceIndex) in getReplaceableContents(msg.content)"
                :key="replaceIndex"
                size="small"
                type="primary"
                @click="handleReplace(index, msg, replaceIndex)"
              >
                替换方案{{ replaceIndex + 1 }}
              </el-button>
            </template>
            <el-button size="small" @click="dismissReplace(index)">
              取消
            </el-button>
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
      <!-- 选中内容引用块 -->
      <div v-if="props.selectedText" class="selection-quote">
        <div class="quote-header">
          <span class="quote-label">选中的内容</span>
          <button class="quote-close" @click="clearSelection" title="清除">&times;</button>
        </div>
        <div class="quote-content">{{ props.selectedText }}</div>
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
import { ref, reactive, nextTick, onBeforeUnmount, watch } from 'vue'
import { Close, CircleCheck, CircleClose, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { useThemeStore } from '@/store/theme'
import { aiApi, createSSEConnection } from '@/api/ai'
import { generateUUID } from '@/utils/crypto'
import { extractReplaceableContents } from '@/utils/aiReplacement'
import type { AiChatMessage } from '@/types/ai'

const themeStore = useThemeStore()

const props = defineProps<{
  context?: string
  projectId?: number
  requirementId?: number
  showClose?: boolean
  greeting?: string
  selectedText?: string
}>()

const messages = defineModel<AiChatMessage[]>('messages', { default: () => [] })

/** 消息为空时，将 greeting 作为首条 assistant 消息插入 */
watch([() => messages.value.length, () => props.greeting], ([len, greetingText]) => {
  if (len === 0 && greetingText) {
    messages.value = [{
      role: 'assistant',
      content: greetingText,
      timestamp: Date.now(),
      uid: 'greeting',
      isGreeting: true,
    }]
  }
}, { immediate: true })

const emit = defineEmits<{
  close: []
  feedback: [type: 'like' | 'dislike', msg: AiChatMessage]
  message: [content: string, hadSelection: boolean]
  replace: [payload: { selectedText: string; replacement: string }]
  'update:selectedText': [value: string]
}>()

function clearSelection() {
  emit('update:selectedText', '')
}

const inputText = ref('')
const sending = ref(false)
const messageListRef = ref<HTMLDivElement>()
let closeSSE: (() => void) | null = null
const conversationId = generateUUID()

/** 每条聊天消息的反馈状态：uid → LIKE/DISLIKE */
const chatFeedbackMap = reactive<Record<string, 'LIKE' | 'DISLIKE'>>({})

/** 记录已取消替换的消息索引 */
const dismissedReplace = ref(new Set<number>())

/** 获取 AI 回复消息对应的用户选中原文 */
function getSelectedTextForAiMsg(aiMsgIndex: number): string | undefined {
  if (dismissedReplace.value.has(aiMsgIndex)) return undefined
  if (aiMsgIndex <= 0) return undefined
  const userMsg = messages.value[aiMsgIndex - 1]
  if (userMsg?.role !== 'user') return undefined
  return userMsg.selectedText
}

function canShowReplace(index: number, msg: AiChatMessage) {
  return msg.role === 'assistant'
    && !!msg.content
    && !msg.error
    && !msg.isGreeting
    && !sending.value
    && !!getSelectedTextForAiMsg(index)
}

function getReplaceableContents(content: string): string[] {
  return extractReplaceableContents(content)
}

/** 应用替换 */
function handleReplace(_index: number, msg: AiChatMessage, replacementIndex = 0) {
  const selectedText = getSelectedTextForAiMsg(_index)
  if (!selectedText) return
  const replacement = extractReplaceableContents(msg.content)[replacementIndex]
  if (!replacement) {
    ElMessage.warning(replacementIndex === 0 ? '未识别到可替换正文，请手动复制' : '未识别到对应替换方案，请手动复制')
    return
  }
  emit('replace', { selectedText, replacement })
  dismissedReplace.value.add(_index)
}

/** 取消替换 */
function dismissReplace(index: number) {
  dismissedReplace.value.add(index)
}

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
  if (!msg.uid || chatFeedbackMap[msg.uid]) return
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

/** 构建对话历史 */
function buildHistory() {
  return messages.value
    .filter(msg => msg.content && !msg.error && !msg.isGreeting)
    .map(msg => ({ role: msg.role, content: msg.content }))
    .slice(-10)
}

/** 统一发送逻辑 */
function doSend(text: string, context: string) {
  const currentSelection = props.selectedText || ''
  const hadSelection = !!currentSelection
  const userMsg: AiChatMessage = { role: 'user', content: text, timestamp: Date.now(), uid: generateUid('user', Date.now(), text), selectedText: currentSelection || undefined }
  messages.value.push(userMsg)
  inputText.value = ''
  sending.value = true
  emit('message', text, hadSelection)
  if (hadSelection) {
    emit('update:selectedText', '')
  }
  scrollToBottom()

  const aiMsgTimestamp = Date.now()
  const aiMsg: AiChatMessage = { role: 'assistant', content: '', timestamp: aiMsgTimestamp, uid: generateUid('assistant', aiMsgTimestamp) }
  messages.value.push(aiMsg)
  const aiIndex = messages.value.length - 1

  closeSSE = createSSEConnection(
    aiApi.chatUrl,
    {
      message: text,
      context,
      projectId: props.projectId,
      requirementId: props.requirementId,
      conversationId,
      replaceMode: hadSelection,
      history: buildHistory(),
    },
    (data: string) => {
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
      sending.value = false
      closeSSE = null
    },
  )

  scrollToBottom()
}

/** 发送消息（用户输入：context = 选中内容 || 空） */
function handleSend() {
  const text = inputText.value.trim()
  if (!text || sending.value) return
  doSend(text, props.selectedText || '')
}

/** 快捷操作：设置输入内容并发送 */
function sendQuickAction(text: string) {
  if (sending.value || !text.trim()) return
  inputText.value = text
  // 快捷操作：context = 选中内容 || 完整内容
  doSend(text, props.selectedText || props.context || '')
}

defineExpose({ sendQuickAction })

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
  line-height: 1.6;
}

// Markdown预览组件样式覆盖
.message-content.markdown-body {
  :deep(.md-editor-preview-wrapper) {
    padding: 0;
  }

  :deep(.md-editor-preview) {
    font-size: 14px;
    line-height: 1.6;
  }

  :deep(.md-editor-preview p) {
    margin: 0 0 8px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(.md-editor-preview pre) {
    margin: 8px 0;
    border-radius: 4px;
  }

  :deep(.md-editor-preview code) {
    font-size: 13px;
  }

  :deep(.md-editor-preview ul),
  :deep(.md-editor-preview ol) {
    padding-left: 20px;
    margin: 4px 0;
  }

  :deep(.md-editor-preview blockquote) {
    margin: 8px 0;
    padding: 4px 12px;
  }

  :deep(.md-editor-preview table) {
    margin: 8px 0;
  }

  :deep(.md-editor-preview h1),
  :deep(.md-editor-preview h2),
  :deep(.md-editor-preview h3),
  :deep(.md-editor-preview h4) {
    margin: 12px 0 6px;
  }
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

// ========================================
// 选中内容引用块
// ========================================
.selection-quote {
  background: var(--app-bg-elevated);
  border: 1px solid var(--app-border-light);
  border-left: 3px solid var(--app-brand-color);
  border-radius: 4px;
  padding: 8px 10px;
  margin-bottom: 4px;
}

.quote-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.quote-label {
  font-size: 11px;
  color: var(--app-text-tertiary);
  font-weight: 500;
}

.quote-close {
  background: none;
  border: none;
  color: var(--app-text-tertiary);
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  padding: 0 2px;
}

.quote-close:hover {
  color: var(--app-text-secondary);
}

.quote-content {
  font-size: 12px;
  color: var(--app-text-secondary);
  line-height: 1.5;
  max-height: 4.5em;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  word-break: break-all;
}

// ========================================
// 用户消息中的引用
// ========================================
.user-quote {
  border-left: 3px solid rgba(255, 255, 255, 0.4);
  padding-left: 8px;
  margin-bottom: 6px;
  font-size: 13px;
  opacity: 0.8;
  white-space: pre-wrap;
  word-break: break-all;
}

// ========================================
// 替换操作栏
// ========================================
.replace-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  justify-content: flex-end;
}
</style>
