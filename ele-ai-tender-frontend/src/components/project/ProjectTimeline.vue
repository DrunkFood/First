<template>
  <div class="project-timeline">
    <el-timeline>
      <el-timeline-item
        v-for="(node, index) in nodes"
        :key="index"
        :type="getNodeStatus(index).type"
        :hollow="getNodeStatus(index).hollow"
        :timestamp="node.label"
        placement="top"
      >
        <el-card
          shadow="hover"
          :class="{ clickable: isNodeAccessible(index), disabled: !isNodeAccessible(index) }"
          @click="handleNodeClick(index)"
        >
          <div class="node-content">
            <el-icon v-if="getNodeStatus(index).completed" :style="{ color: 'var(--app-color-success)' }"><CircleCheck /></el-icon>
            <span>{{ node.title }}</span>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup lang="ts">
import { CircleCheck } from '@element-plus/icons-vue'

/** 检测已完成的终态 */
const DETECTION_DONE_STATUSES = ['DETECTION_PASSED', 'DETECTION_SKIPPED', 'PUBLISHED', 'ARCHIVED']
/** 检测失败的终态 */
const DETECTION_FAILED_STATUS = 'DETECTION_FAILED'

interface NodeStatus {
  type: 'primary' | 'success' | 'info' | 'warning'
  hollow: boolean
  completed: boolean
}

const props = defineProps<{
  currentPhase: number
  status?: string
}>()

const emit = defineEmits<{
  click: [index: number]
}>()

const nodes = [
  { title: '项目创建', label: '第1步' },
  { title: '基础信息录入', label: '第2步' },
  { title: '招标需求生成', label: '第3步' },
  { title: '评审项设置', label: '第4步' },
  { title: '文档集成', label: '第5步' },
  { title: '智能检测', label: '第6步' },
  { title: '检测通过', label: '第7步' },
]

function getNodeStatus(index: number): NodeStatus {
  const { currentPhase, status } = props

  // 检测阶段(index=5)和检测通过(index=6)需要结合项目状态判断
  if (currentPhase === 5 && index >= 5) {
    if (DETECTION_DONE_STATUSES.includes(status ?? '')) {
      return { type: 'success', hollow: false, completed: true }
    }
    if (status === DETECTION_FAILED_STATUS) {
      if (index === 5) return { type: 'warning', hollow: false, completed: false }
      return { type: 'info', hollow: true, completed: false }
    }
    // DETECTING / PENDING_DETECTION 等检测进行中状态
    if (index === 5) return { type: 'primary', hollow: false, completed: false }
    return { type: 'info', hollow: true, completed: false }
  }

  // 常规节点：按 currentPhase 判断
  if (index < currentPhase) {
    return { type: 'success', hollow: false, completed: true }
  }
  if (index === currentPhase) {
    return { type: 'primary', hollow: false, completed: false }
  }
  return { type: 'info', hollow: true, completed: false }
}

/** 节点是否可点击（已完成或当前进行中） */
function isNodeAccessible(index: number): boolean {
  const { currentPhase, status } = props
  // 检测通过后，检测相关节点都可点击
  if (currentPhase === 5 && index >= 5 && DETECTION_DONE_STATUSES.includes(status ?? '')) {
    return true
  }
  return index <= currentPhase
}

function handleNodeClick(index: number) {
  if (isNodeAccessible(index)) {
    emit('click', index)
  }
}
</script>

<style scoped>
.project-timeline {
  padding: 16px 0;
}
.clickable {
  cursor: pointer;
  transition: transform 0.2s;
}
.clickable:hover {
  transform: translateX(4px);
}
.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.disabled:hover {
  transform: none;
}
.node-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
</style>
