<template>
  <div :class="['ai-assistant-sidebar', { collapsed: !visible }]">
    <button :class="['ai-toggle-btn', { collapsed: !visible }]" @click="toggleVisible" :title="visible ? '收起AI助手' : '展开AI助手'">
      <el-icon :size="18">
        <component :is="visible ? Close : ChatDotRound" />
      </el-icon>
    </button>
    <div v-if="visible" class="ai-sidebar-body">
      <AiChatPanel
        ref="chatPanelRef"
        show-close
        :greeting="greeting"
        :context="context"
        :project-id="projectId"
        :requirement-id="requirementId"
        v-model:messages="messages"
        @close="emit('update:visible', false)"
        @feedback="(type, msg) => emit('feedback', type, msg)"
        @message="(content) => emit('message', content)"
      >
        <template #quick-actions>
          <slot name="quick-actions" />
        </template>
      </AiChatPanel>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Close, ChatDotRound } from '@element-plus/icons-vue'
import AiChatPanel from './AiChatPanel.vue'
import type { AiChatMessage } from '@/types/ai'

const props = defineProps<{
  visible: boolean
  messages: AiChatMessage[]
  greeting?: string
  context?: string
  projectId?: number
  requirementId?: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'update:messages': [value: AiChatMessage[]]
  feedback: [type: 'like' | 'dislike', msg: AiChatMessage]
  message: [content: string]
}>()

const messages = defineModel<AiChatMessage[]>('messages', { default: () => [] })
const chatPanelRef = ref<InstanceType<typeof AiChatPanel>>()

/** 透传快捷操作发送方法 */
function sendQuickAction(text: string) {
  chatPanelRef.value?.sendQuickAction(text)
}

defineExpose({ sendQuickAction })

function toggleVisible() {
  emit('update:visible', !props.visible)
}
</script>

<style scoped lang="scss">
// ========================================
// 侧边栏容器（匹配原型设计）
// ========================================
.ai-assistant-sidebar {
  position: fixed;
  right: 20px;
  top: 100px;
  width: 320px;
  height: 600px;
  border: 1px solid var(--app-border-light);
  background: var(--app-bg-secondary);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 16px var(--app-shadow-color);
  z-index: 1000;
  transition: transform 0.3s ease;

  &.collapsed {
    transform: translateX(calc(100% - 40px));
  }
}

// ========================================
// 切换按钮
// ========================================
.ai-toggle-btn {
  position: absolute;
  left: -40px;
  top: 20px;
  width: 40px;
  height: 40px;
  background: var(--app-brand-color);
  color: white;
  border: none;
  border-radius: 8px 0 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: #2855d9;
  }

  // 收起时全圆角
  &.collapsed {
    border-radius: 8px;
  }
}

// ========================================
// 内容区（全高填充，无边框/背景，由外层侧边栏提供）
// ========================================
.ai-sidebar-body {
  flex: 1;
  overflow: hidden;
  border-radius: 8px;
}

// ========================================
// 快捷操作（通过 :deep 穿透 slot 内容）
// ========================================
:deep(.quick-actions) {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

:deep(.quick-action-btn) {
  padding: 6px 12px;
  background: var(--app-bg-elevated);
  border: 1px solid var(--app-border-light);
  border-radius: 16px;
  color: var(--app-text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--app-hover-state);
    color: var(--app-brand-color);
    border-color: var(--app-brand-color);
  }
}
</style>
