<template>
  <div class="phase-review-item">
    <AiUnavailableAlert
      :visible="isAiUnavailable"
      @retry="handleRetry"
      @skip="handleSkipAi"
    />

    <div class="review-toolbar">
      <el-button type="primary" :loading="isGenerating" @click="handleGenerate">
        AI 生成评审项
      </el-button>
      <AiTaskStatus
        v-if="task"
        :task="task"
        :show-actions="true"
        @retry="handleRetry"
        @skip="handleSkipAi"
      />
      <el-button @click="handleAddRoot">手动添加根项</el-button>
    </div>

    <!-- 评审类型Tabs -->
    <el-tabs v-model="activeReviewType" class="review-tabs">
      <el-tab-pane label="符合性审查" name="COMPLIANCE" />
      <el-tab-pane label="技术标" name="TECHNICAL" />
      <el-tab-pane label="资信标" name="CREDIT" />
      <el-tab-pane label="商务标" name="COMMERCIAL" />
    </el-tabs>

    <!-- 当前类型的评审项统计 -->
    <div class="review-type-summary">
      <el-tag type="info" size="small">共 {{ currentTypeItems.length }} 项</el-tag>
      <el-tag type="primary" size="small">客观 {{ currentTypeObjectiveCount }}</el-tag>
      <el-tag type="warning" size="small">主观 {{ currentTypeSubjectiveCount }}</el-tag>
    </div>

    <!-- 评审项标记与权重编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑评审项" width="480px" destroy-on-close>
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="评审项名称">
          <el-input v-model="editForm.itemName" />
        </el-form-item>
        <el-form-item label="评审类型">
          <el-select v-model="editForm.reviewType">
            <el-option label="符合性审查" value="COMPLIANCE" />
            <el-option label="技术标" value="TECHNICAL" />
            <el-option label="资信标" value="CREDIT" />
            <el-option label="商务标" name="COMMERCIAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="客观/主观">
          <el-radio-group v-model="editForm.subjective">
            <el-radio :value="false">
              <el-tag type="primary" size="small">客观</el-tag>
            </el-radio>
            <el-radio :value="true">
              <el-tag type="warning" size="small">主观</el-tag>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分值权重">
          <el-input-number v-model="editForm.score" :min="0" :max="100" :precision="1" />
        </el-form-item>
        <el-form-item label="评审内容">
          <el-input v-model="editForm.itemContent" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 当前类型的评审项树（内联渲染，支持客观/主观标记和分值） -->
    <el-tree
      v-if="treeData.length"
      :data="treeData"
      :props="treeProps"
      node-key="id"
      default-expand-all
      class="review-tree"
    >
      <template #default="{ data }">
        <div class="tree-node">
          <span class="node-name">{{ data.itemName }}</span>
          <el-tag
            :type="data.subjective ? 'warning' : 'primary'"
            size="small"
          >
            {{ data.subjective ? '主观' : '客观' }}
          </el-tag>
          <span v-if="data.score" class="node-score">{{ data.score }}分</span>
          <span class="node-actions">
            <el-button text size="small" @click.stop="openEditDialog(data)">编辑</el-button>
            <el-button text size="small" type="danger" @click.stop="handleDelete(data)">删除</el-button>
          </span>
        </div>
      </template>
    </el-tree>

    <el-empty v-if="!currentTypeItems.length" description="当前类型暂无评审项" />

    <div class="phase-actions">
      <el-button @click="$emit('prev')">上一步</el-button>
      <el-button type="primary" @click="handleNext">保存并继续</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reviewApi } from '@/api/review'
import { useTaskPolling } from '@/composables/useTaskPolling'
import AiTaskStatus from '@/components/AiTaskStatus.vue'
import AiUnavailableAlert from '@/components/AiUnavailableAlert.vue'

/** 评审类型枚举 */
type ReviewCategory = 'COMPLIANCE' | 'TECHNICAL' | 'CREDIT' | 'COMMERCIAL'

interface ReviewItemData {
  id: number
  parentId?: number
  level: number
  itemName: string
  itemContent?: string
  reviewType?: string
  subjective?: boolean
  score?: number
  sortOrder: number
  children?: ReviewItemData[]
}

const props = defineProps<{ projectId: number }>()
const emit = defineEmits<{ next: []; prev: [] }>()

const reviewItems = ref<ReviewItemData[]>([])
const taskId = ref<number | null>(null)
const isGenerating = ref(false)
const activeReviewType = ref<ReviewCategory>('COMPLIANCE')

// 编辑弹窗
const editDialogVisible = ref(false)
const editSaving = ref(false)
const editForm = ref<ReviewItemData>({
  id: 0,
  level: 1,
  itemName: '',
  itemContent: '',
  reviewType: 'COMPLIANCE',
  subjective: false,
  score: 0,
  sortOrder: 0,
})

const { task, retry: retryTask, skip: skipTask } = useTaskPolling(taskId)
const isAiUnavailable = computed(() => task.value?.status === 'AI_UNAVAILABLE')

/** 按当前Tab筛选评审项 */
const currentTypeItems = computed(() => {
  return reviewItems.value.filter(item => {
    const type = item.reviewType || 'COMPLIANCE'
    return type === activeReviewType.value
  })
})

/** 构建树形结构 */
const treeProps = { children: 'children', label: 'itemName' }

const treeData = computed(() => {
  const items = currentTypeItems.value
  const map = new Map<number, ReviewItemData & { children: ReviewItemData[] }>()
  const roots: (ReviewItemData & { children: ReviewItemData[] })[] = []

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

const currentTypeObjectiveCount = computed(() =>
  currentTypeItems.value.filter(i => !i.subjective).length,
)

const currentTypeSubjectiveCount = computed(() =>
  currentTypeItems.value.filter(i => i.subjective).length,
)

const loadReviewItems = async () => {
  reviewItems.value = await reviewApi.getTree(props.projectId)
}

const handleGenerate = async () => {
  isGenerating.value = true
  try {
    const res = await reviewApi.generate(props.projectId, {})
    taskId.value = res.id
  } catch {
    ElMessage.error('提交AI生成失败')
  } finally {
    isGenerating.value = false
  }
}

const handleAddRoot = async () => {
  await reviewApi.create({
    projectId: props.projectId,
    itemName: '新评审项',
    level: 1,
    sortOrder: reviewItems.value.length,
    reviewType: activeReviewType.value,
    subjective: false,
    score: 0,
  })
  await loadReviewItems()
}

/** 打开编辑弹窗 */
const openEditDialog = (data: ReviewItemData) => {
  editForm.value = { ...data }
  editDialogVisible.value = true
}

/** 保存编辑 */
const handleSaveEdit = async () => {
  editSaving.value = true
  try {
    await reviewApi.update(editForm.value.id, {
      itemName: editForm.value.itemName,
      itemContent: editForm.value.itemContent,
      reviewType: editForm.value.reviewType,
      subjective: editForm.value.subjective,
      score: editForm.value.score,
    })
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    await loadReviewItems()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    editSaving.value = false
  }
}

/** 删除评审项 */
const handleDelete = async (data: ReviewItemData) => {
  await ElMessageBox.confirm(`确定删除「${data.itemName}」及其子项？`, '确认')
  await reviewApi.deleteById(data.id)
  ElMessage.success('删除成功')
  await loadReviewItems()
}

const handleRetry = () => retryTask()
const handleSkipAi = () => skipTask()

const handleNext = () => {
  emit('next')
}

onMounted(loadReviewItems)
</script>

<style scoped>
.review-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.review-tabs {
  margin-bottom: 8px;
}

.review-type-summary {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.review-tree {
  margin-bottom: 12px;
}

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

.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
