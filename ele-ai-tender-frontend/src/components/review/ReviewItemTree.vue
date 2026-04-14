<template>
  <div class="review-item-tree">
    <el-tree
      :data="treeData"
      :props="treeProps"
      node-key="id"
      default-expand-all
      draggable
    >
      <template #default="{ data }">
        <div class="tree-node">
          <span class="node-name">{{ data.itemName }}</span>
          <span v-if="data.reviewType" class="node-tag">
            <el-tag size="small">{{ data.reviewType }}</el-tag>
          </span>
          <span v-if="data.score" class="node-score">{{ data.score }}分</span>
          <span class="node-actions">
            <el-button text size="small" @click.stop="handleEdit(data)">编辑</el-button>
            <el-button text size="small" type="danger" @click.stop="handleDelete(data)">删除</el-button>
          </span>
        </div>
      </template>
    </el-tree>

    <el-empty v-if="!items.length" description="暂无评审项" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reviewApi } from '@/api/review'

interface ReviewItem {
  id: number
  parentId?: number
  level: number
  itemName: string
  itemContent?: string
  reviewType?: string
  score?: number
  sortOrder: number
  children?: ReviewItem[]
}

const props = defineProps<{
  items: ReviewItem[]
  projectId: number
}>()

const emit = defineEmits<{ refresh: [] }>()

const treeProps = { children: 'children', label: 'itemName' }

// 根据parentId构建树形结构
const treeData = computed(() => {
  const items = props.items
  const map = new Map<number, ReviewItem & { children: ReviewItem[] }>()
  const roots: (ReviewItem & { children: ReviewItem[] })[] = []

  for (const item of items) {
    map.set(item.id, { ...item, children: [] })
  }

  for (const item of items) {
    const node = map.get(item.id)!
    if (item.parentId && map.has(item.parentId)) {
      map.get(item.parentId)!.children.push(node)
    } else {
      roots.push(node)
    }
  }

  return roots
})

const handleEdit = (data: ReviewItem) => {
  ElMessage.info('编辑功能：' + data.itemName)
}

const handleDelete = async (data: ReviewItem) => {
  await ElMessageBox.confirm(`确定删除「${data.itemName}」及其子项？`, '确认')
  await reviewApi.deleteById(data.id)
  ElMessage.success('删除成功')
  emit('refresh')
}
</script>

<style scoped>
.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.node-name {
  font-weight: 500;
}

.node-score {
  color: var(--el-color-primary);
  font-size: 12px;
}

.node-actions {
  margin-left: auto;
}
</style>
