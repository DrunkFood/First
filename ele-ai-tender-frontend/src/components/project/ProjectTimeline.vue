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
        <el-card shadow="hover" :class="{ clickable: true }" @click="$emit('click', index)">
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

interface NodeStatus {
  type: 'primary' | 'success' | 'info'
  hollow: boolean
  completed: boolean
}

const props = defineProps<{
  currentPhase: number
}>()

defineEmits<{
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
  if (index < props.currentPhase) {
    return { type: 'success', hollow: false, completed: true }
  }
  if (index === props.currentPhase) {
    return { type: 'primary', hollow: false, completed: false }
  }
  return { type: 'info', hollow: true, completed: false }
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
.node-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
</style>
