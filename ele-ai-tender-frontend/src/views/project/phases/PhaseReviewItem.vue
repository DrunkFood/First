<template>
  <div class="phase-review-item">
    <AiUnavailableAlert
      :visible="isAiUnavailable"
      @retry="handleRetry"
      @skip="handleSkipAi"
    />

    <!-- 评分摘要栏 -->
    <div class="score-summary">
      <div class="score-item compliance">
        <span class="score-label">符合性审查</span>
        <span class="score-value">{{ complianceCount }}项</span>
        <el-tag :type="complianceCount > 0 ? 'success' : 'info'" size="small">
          {{ complianceCount > 0 ? '通过/不通过' : '未设置' }}
        </el-tag>
      </div>
      <div class="score-item credit">
        <span class="score-label">资信评审</span>
        <span class="score-value">{{ creditScore }}分</span>
      </div>
      <div class="score-item technical">
        <span class="score-label">技术评审</span>
        <span class="score-value">{{ technicalScore }}分</span>
      </div>
      <div class="score-item commercial">
        <span class="score-label">商务评审</span>
        <span class="score-value">{{ commercialScore }}分</span>
      </div>
      <div class="score-item total" :class="{ valid: scoreTotal === 100, invalid: scoreTotal !== 100 }">
        <span class="score-label">合计总分</span>
        <span class="score-value">{{ scoreTotal }}分</span>
        <el-tag v-if="scoreTotal === 100" type="success" size="small">符合</el-tag>
        <el-tag v-else-if="scoreTotal > 0" type="warning" size="small">需调整</el-tag>
      </div>
    </div>

    <!-- 评分说明 -->
    <el-alert
      v-if="scoreTotal > 0 && scoreTotal !== 100"
      title="资信评审、技术评审、商务评审三项合计必须为100分"
      type="warning"
      :closable="false"
      show-icon
      class="score-alert"
    />

    <!-- 工具栏 -->
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
    </div>

    <!-- 评审类型Tabs -->
    <el-tabs v-model="activeReviewType" class="review-tabs">
      <!-- 符合性审查 -->
      <el-tab-pane label="符合性审查" name="COMPLIANCE">
        <el-table :data="complianceItems" border class="review-table">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="评审标准" min-width="300">
            <template #default="{ row }">
              <el-input v-model="row.itemName" type="textarea" :rows="2" placeholder="请输入评审标准" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="handleDeleteItem('COMPLIANCE', $index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="add-item-btn" @click="handleAddItem('COMPLIANCE')">
          <el-icon><Plus /></el-icon> 添加评审项
        </div>
      </el-tab-pane>

      <!-- 资信评审 -->
      <el-tab-pane label="资信评审" name="CREDIT">
        <el-table :data="creditItems" border class="review-table">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="评审标准" min-width="240">
            <template #default="{ row }">
              <el-input v-model="row.itemName" type="textarea" :rows="2" placeholder="请输入评审标准" />
            </template>
          </el-table-column>
          <el-table-column label="主观/客观" width="120" align="center">
            <template #default="{ row }">
              <el-select v-model="row.subjective" size="small">
                <el-option :value="false" label="客观" />
                <el-option :value="true" label="主观" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="分值" width="100" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.score" :min="0" :max="100" :precision="1" size="small" style="width: 80px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="handleDeleteItem('CREDIT', $index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="add-item-btn" @click="handleAddItem('CREDIT')">
          <el-icon><Plus /></el-icon> 添加评审项
        </div>
      </el-tab-pane>

      <!-- 技术评审 -->
      <el-tab-pane label="技术评审" name="TECHNICAL">
        <el-table :data="technicalItems" border class="review-table">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="评审标准" min-width="240">
            <template #default="{ row }">
              <el-input v-model="row.itemName" type="textarea" :rows="2" placeholder="请输入评审标准" />
            </template>
          </el-table-column>
          <el-table-column label="主观/客观" width="120" align="center">
            <template #default="{ row }">
              <el-select v-model="row.subjective" size="small">
                <el-option :value="false" label="客观" />
                <el-option :value="true" label="主观" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="分值" width="100" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.score" :min="0" :max="100" :precision="1" size="small" style="width: 80px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="handleDeleteItem('TECHNICAL', $index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="add-item-btn" @click="handleAddItem('TECHNICAL')">
          <el-icon><Plus /></el-icon> 添加评审项
        </div>
      </el-tab-pane>

      <!-- 商务评审 -->
      <el-tab-pane label="商务评审" name="COMMERCIAL">
        <el-table :data="commercialItems" border class="review-table">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="评审标准" min-width="300">
            <template #default="{ row }">
              <el-input v-model="row.itemName" type="textarea" :rows="2" placeholder="请输入评审标准" />
            </template>
          </el-table-column>
          <el-table-column label="分值" width="100" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.score" :min="0" :max="100" :precision="1" size="small" style="width: 80px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="handleDeleteItem('COMMERCIAL', $index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="add-item-btn" @click="handleAddItem('COMMERCIAL')">
          <el-icon><Plus /></el-icon> 添加评审项
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 底部操作 -->
    <div class="phase-actions">
      <el-button @click="$emit('prev')">上一步</el-button>
      <el-button type="warning" plain @click="handleGenerate">重新生成</el-button>
      <div style="flex: 1" />
      <el-button type="primary" @click="handleNext">确认评审项</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { reviewApi } from '@/api/review'
import { useTaskPolling } from '@/composables/useTaskPolling'
import AiTaskStatus from '@/components/AiTaskStatus.vue'
import AiUnavailableAlert from '@/components/AiUnavailableAlert.vue'

type ReviewCategory = 'COMPLIANCE' | 'TECHNICAL' | 'CREDIT' | 'COMMERCIAL'

interface ReviewItemData {
  id?: number
  parentId?: number
  level: number
  itemName: string
  itemContent?: string
  reviewType: ReviewCategory
  subjective: boolean
  score: number
  sortOrder: number
}

const props = defineProps<{ projectId: number }>()
const emit = defineEmits<{ next: []; prev: [] }>()

const reviewItems = ref<ReviewItemData[]>([])
const taskId = ref<number | null>(null)
const isGenerating = ref(false)
const activeReviewType = ref<ReviewCategory>('COMPLIANCE')

const { task, retry: retryTask, skip: skipTask } = useTaskPolling(taskId)
const isAiUnavailable = computed(() => task.value?.status === 'AI_UNAVAILABLE')

// 按类型分组
const complianceItems = computed(() => reviewItems.value.filter(i => i.reviewType === 'COMPLIANCE'))
const creditItems = computed(() => reviewItems.value.filter(i => i.reviewType === 'CREDIT'))
const technicalItems = computed(() => reviewItems.value.filter(i => i.reviewType === 'TECHNICAL'))
const commercialItems = computed(() => reviewItems.value.filter(i => i.reviewType === 'COMMERCIAL'))

// 评分计算
const complianceCount = computed(() => complianceItems.value.length)
const creditScore = computed(() => creditItems.value.reduce((sum, i) => sum + (i.score || 0), 0))
const technicalScore = computed(() => technicalItems.value.reduce((sum, i) => sum + (i.score || 0), 0))
const commercialScore = computed(() => commercialItems.value.reduce((sum, i) => sum + (i.score || 0), 0))
const scoreTotal = computed(() => creditScore.value + technicalScore.value + commercialScore.value)

const loadReviewItems = async () => {
  const data = await reviewApi.getTree(props.projectId)
  reviewItems.value = (data || []).map((item: any) => ({
    id: item.id,
    parentId: item.parentId,
    level: item.level ?? 1,
    itemName: item.itemName || '',
    itemContent: item.itemContent || '',
    reviewType: item.reviewType || 'COMPLIANCE',
    subjective: item.subjective ?? false,
    score: item.score ?? 0,
    sortOrder: item.sortOrder ?? 0,
  }))
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

const handleAddItem = async (type: ReviewCategory) => {
  const newItem: ReviewItemData = {
    level: 1,
    itemName: '',
    reviewType: type,
    subjective: false,
    score: 0,
    sortOrder: reviewItems.value.filter(i => i.reviewType === type).length,
  }

  try {
    await reviewApi.create({
      projectId: props.projectId,
      itemName: newItem.itemName || '新评审项',
      level: newItem.level,
      sortOrder: newItem.sortOrder,
      reviewType: newItem.reviewType,
      subjective: newItem.subjective,
      score: newItem.score,
    })
    await loadReviewItems()
  } catch {
    ElMessage.error('添加失败')
  }
}

const handleDeleteItem = async (type: ReviewCategory, index: number) => {
  const items = reviewItems.value.filter(i => i.reviewType === type)
  const item = items[index]
  if (!item?.id) return

  await ElMessageBox.confirm(`确定删除「${item.itemName}」？`, '确认')
  await reviewApi.deleteById(item.id)
  ElMessage.success('删除成功')
  await loadReviewItems()
}

const handleRetry = () => retryTask()
const handleSkipAi = () => skipTask()

const handleNext = async () => {
  // 校验100分
  if (scoreTotal.value > 0 && scoreTotal.value !== 100) {
    ElMessage.warning(`资信+技术+商务评审合计应为100分，当前为${scoreTotal.value}分`)
    return
  }

  // 保存所有修改
  try {
    const itemsToUpdate = reviewItems.value.filter(i => i.id)
    for (const item of itemsToUpdate) {
      await reviewApi.update(item.id!, {
        itemName: item.itemName,
        itemContent: item.itemContent,
        reviewType: item.reviewType,
        subjective: item.subjective,
        score: item.score,
      })
    }
  } catch {
    ElMessage.error('保存失败')
    return
  }

  emit('next')
}

onMounted(loadReviewItems)
</script>

<style scoped lang="scss">
// 评分摘要栏
.score-summary {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.score-item {
  flex: 1;
  padding: 12px;
  border-radius: 6px;
  text-align: center;
  background: var(--app-bg-secondary);
  border-left: 3px solid var(--app-border-light);
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.score-label {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.score-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--app-text-primary);
}

.score-item.compliance {
  border-left-color: var(--app-brand-color);
}

.score-item.credit {
  border-left-color: var(--app-color-success);
}

.score-item.technical {
  border-left-color: var(--app-auxiliary-color);
}

.score-item.commercial {
  border-left-color: var(--app-embellishment-color);
}

.score-item.total {
  border-left-width: 4px;

  &.valid {
    border-left-color: var(--app-color-success);
    .score-value { color: var(--app-color-success); }
  }

  &.invalid {
    border-left-color: var(--app-color-warning);
    .score-value { color: var(--app-color-warning); }
  }
}

.score-alert {
  margin-bottom: 16px;
}

// 工具栏
.review-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.review-tabs {
  margin-bottom: 8px;
}

.review-table {
  margin-bottom: 8px;
}

// 添加评审项虚线按钮
.add-item-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 10px;
  border: 2px dashed var(--app-border-medium);
  border-radius: 6px;
  cursor: pointer;
  color: var(--app-text-tertiary);
  font-size: 14px;
  transition: var(--app-transition-base);

  &:hover {
    border-color: var(--app-brand-color);
    color: var(--app-brand-color);
  }
}

// 底部操作
.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
