<template>
  <div class="phase-review-item">
    <!-- 评审项生成进度 -->
    <GenerationStatusCard
      :task="latestTask"
      :can-create-new="canCreateNew"
      :progress-percent="progressPercent"
      generating-title="正在生成评审项"
      generating-desc="AI正在生成评审项内容，请稍候..."
      completed-desc="评审项已生成完成，您可以在下方查看和修改内容"
      idle-title="AI生成评审项"
      idle-desc="点击下方重新生成按钮开始AI生成评审项内容"
    />

    <!-- 评分摘要栏 -->
    <div class="score-summary">
      <div class="score-item">
        <div class="score-label">符合性审查</div>
        <div class="score-value info">通过/不通过</div>
      </div>
      <div class="score-item">
        <div class="score-label">资信评审</div>
        <div class="score-value">{{ creditScore }}分</div>
      </div>
      <div class="score-item">
        <div class="score-label">技术评审</div>
        <div class="score-value">{{ technicalScore }}分</div>
      </div>
      <div class="score-item">
        <div class="score-label">商务评审</div>
        <div class="score-value">{{ commercialScore }}分</div>
      </div>
      <div class="score-item">
        <div class="score-label">三项合计总分</div>
        <div class="score-value" :class="scoreTotal === 100 ? 'success' : scoreTotal > 0 ? 'warning' : ''">
          {{ scoreTotal }}分
        </div>
      </div>
    </div>

    <!-- 评分说明 -->
    <div class="score-notice info-notice">
      <strong>评分说明：</strong>资信评审、技术评审、商务评审三项合计总分必须为100分。允许其中一项或两项为0分。
    </div>

    <!-- 评分建议 -->
    <div class="score-notice warning-notice">
      <strong>评分建议：</strong>建议货物类项目商务分30-60，资信10-25分；建议服务类项目商务分10-30，资信10-25分。
    </div>

    <!-- 评审类型Tabs -->
    <div class="review-tabs-wrap">
      <el-tabs v-model="activeReviewType" class="review-tabs">
        <!-- 符合性审查 -->
        <el-tab-pane label="符合性审查" name="COMPLIANCE">
          <div class="table-container">
            <el-table :data="complianceItems" class="review-table" :border="true">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column label="评审标准" min-width="500">
                <template #default="{ row }">
                  <el-input
                    v-model="row.itemName"
                    type="textarea"
                    :rows="2"
                    placeholder="请输入评审标准"
                    class="table-textarea"
                    :disabled="readonly"
                  />
                </template>
              </el-table-column>
              <el-table-column v-if="!readonly" label="操作" width="70" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" class="delete-btn" @click="handleDeleteItem('COMPLIANCE', $index)">
                    <el-icon :size="16"><Delete /></el-icon>
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div v-if="!readonly" class="add-item-btn" @click="handleAddItem('COMPLIANCE')">
            <el-icon><Plus /></el-icon> 添加评审项
          </div>
        </el-tab-pane>

        <!-- 资信评审 -->
        <el-tab-pane label="资信评审" name="CREDIT">
          <div class="table-container">
            <el-table :data="creditItems" class="review-table" :border="true">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column label="评审标准" min-width="300">
                <template #default="{ row }">
                  <el-input v-model="row.itemName" type="textarea" :rows="2" placeholder="请输入评审标准" class="table-textarea" :disabled="readonly" />
                </template>
              </el-table-column>
              <el-table-column label="主观/客观" width="120" align="center">
                <template #default="{ row }">
                  <el-select v-model="row.subjective" size="small" class="subjective-select" :disabled="readonly">
                    <el-option :value="false" label="客观" />
                    <el-option :value="true" label="主观" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="分值" width="100" align="center">
                <template #default="{ row }">
                  <el-input-number v-model="row.score" :min="0" :max="100" :precision="1" size="small" class="score-input" controls-position="right" :disabled="readonly" />
                </template>
              </el-table-column>
              <el-table-column v-if="!readonly" label="操作" width="70" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" class="delete-btn" @click="handleDeleteItem('CREDIT', $index)">
                    <el-icon :size="16"><Delete /></el-icon>
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div v-if="!readonly" class="add-item-btn" @click="handleAddItem('CREDIT')">
            <el-icon><Plus /></el-icon> 添加评审项
          </div>
        </el-tab-pane>

        <!-- 技术评审 -->
        <el-tab-pane label="技术评审" name="TECHNICAL">
          <div class="table-container">
            <el-table :data="technicalItems" class="review-table" :border="true">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column label="评审标准" min-width="300">
                <template #default="{ row }">
                  <el-input v-model="row.itemName" type="textarea" :rows="2" placeholder="请输入评审标准" class="table-textarea" :disabled="readonly" />
                </template>
              </el-table-column>
              <el-table-column label="主观/客观" width="120" align="center">
                <template #default="{ row }">
                  <el-select v-model="row.subjective" size="small" class="subjective-select" :disabled="readonly">
                    <el-option :value="false" label="客观" />
                    <el-option :value="true" label="主观" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="分值" width="100" align="center">
                <template #default="{ row }">
                  <el-input-number v-model="row.score" :min="0" :max="100" :precision="1" size="small" class="score-input" controls-position="right" :disabled="readonly" />
                </template>
              </el-table-column>
              <el-table-column v-if="!readonly" label="操作" width="70" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" class="delete-btn" @click="handleDeleteItem('TECHNICAL', $index)">
                    <el-icon :size="16"><Delete /></el-icon>
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div v-if="!readonly" class="add-item-btn" @click="handleAddItem('TECHNICAL')">
            <el-icon><Plus /></el-icon> 添加评审项
          </div>
        </el-tab-pane>

        <!-- 商务评审 -->
        <el-tab-pane label="商务评审" name="COMMERCIAL">
          <div class="table-container">
            <el-table :data="commercialItems" class="review-table" :border="true">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column label="评审标准" min-width="400">
                <template #default="{ row }">
                  <el-input v-model="row.itemName" type="textarea" :rows="2" placeholder="请输入评审标准" class="table-textarea" :disabled="readonly" />
                </template>
              </el-table-column>
              <el-table-column label="分值" width="100" align="center">
                <template #default="{ row }">
                  <el-input-number v-model="row.score" :min="0" :max="100" :precision="1" size="small" class="score-input" controls-position="right" :disabled="readonly" />
                </template>
              </el-table-column>
              <el-table-column v-if="!readonly" label="操作" width="70" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" class="delete-btn" @click="handleDeleteItem('COMMERCIAL', $index)">
                    <el-icon :size="16"><Delete /></el-icon>
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div v-if="!readonly" class="add-item-btn" @click="handleAddItem('COMMERCIAL')">
            <el-icon><Plus /></el-icon> 添加评审项
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 底部操作栏 -->
    <div class="form-actions">
      <div class="actions-left">
        <el-button @click="$emit('prev')">
          <el-icon><ArrowLeft /></el-icon> 上一步
        </el-button>
        <el-button v-if="!readonly" type="warning" :disabled="!canCreateNew" @click="handleGenerate">
          <el-icon><RefreshRight /></el-icon> 重新生成
        </el-button>
      </div>
      <div v-if="!readonly" class="actions-right">
        <el-button type="primary" :disabled="!canCreateNew" @click="handleNext">
          确认评审项 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, ArrowLeft, ArrowRight, RefreshRight } from '@element-plus/icons-vue'
import { reviewApi } from '@/api/review'
import { projectApi } from '@/api/project'
import { useLatestTask } from '@/composables/useLatestTask'
import { getTaskProgress } from '@/types/ai-task'
import GenerationStatusCard from '@/components/GenerationStatusCard.vue'

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

const props = defineProps<{ projectId: number; readonly?: boolean }>()
const emit = defineEmits<{ next: []; prev: [] }>()

const reviewItems = ref<ReviewItemData[]>([])
const activeReviewType = ref<ReviewCategory>('COMPLIANCE')

// 使用 useLatestTask 查询最新任务状态
const projectIdRef = computed(() => props.projectId)
const { latestTask, canCreateNew, setActive, refresh } = useLatestTask(
  'REVIEW_ITEM_GENERATE',
  projectIdRef,
  'PROJECT',
  (task) => {
    // AI任务完成后，延迟等待后端同步结果，再重新加载评审项
    // 后端AiTaskResultSyncScheduler间隔10秒，需带重试确保数据已同步
    if (task.status === 'COMPLETED') {
      loadReviewItemsWithRetry()
    }
  },
)

/** 带重试的评审项加载：AI任务完成后后端同步可能有延迟，最多重试3次 */
const loadReviewItemsWithRetry = async (retries = 3, delayMs = 2000) => {
  for (let i = 0; i < retries; i++) {
    await new Promise(r => setTimeout(r, delayMs))
    await loadReviewItems()
    if (reviewItems.value.length > 0) return
  }
}

// 按类型分组
const complianceItems = computed(() => reviewItems.value.filter(i => i.reviewType === 'COMPLIANCE'))
const creditItems = computed(() => reviewItems.value.filter(i => i.reviewType === 'CREDIT'))
const technicalItems = computed(() => reviewItems.value.filter(i => i.reviewType === 'TECHNICAL'))
const commercialItems = computed(() => reviewItems.value.filter(i => i.reviewType === 'COMMERCIAL'))

// 评分计算
const creditScore = computed(() => creditItems.value.reduce((sum, i) => sum + (i.score || 0), 0))
const technicalScore = computed(() => technicalItems.value.reduce((sum, i) => sum + (i.score || 0), 0))
const commercialScore = computed(() => commercialItems.value.reduce((sum, i) => sum + (i.score || 0), 0))
const scoreTotal = computed(() => creditScore.value + technicalScore.value + commercialScore.value)

const progressPercent = computed(() => {
  if (!latestTask.value) return 0
  return getTaskProgress(latestTask.value.status)
})

const loadReviewItems = async () => {
  const data = await reviewApi.getTree(props.projectId)
  reviewItems.value = (data || [])
    .filter((item: any) => (item.level ?? 1) > 1)
    .map((item: any) => ({
      id: item.id,
      parentId: item.parentId,
      level: item.level ?? 1,
      itemName: item.itemName || '',
      itemContent: item.itemContent || '',
      reviewType: item.reviewType || 'COMPLIANCE',
      subjective: item.subjectivity === 'SUBJECTIVE',
      score: item.score ?? 0,
      sortOrder: item.sortOrder ?? 0,
    }))
}

const handleGenerate = async () => {
  // 提交前刷新最新任务状态，确保校验是最新的
  await refresh()
  if (!canCreateNew.value) {
    ElMessage.warning('AI生成任务正在处理中，请稍候')
    return
  }
  try {
    const res = await reviewApi.generate(props.projectId, {})
    setActive(res.id)
  } catch (e: any) {
    if (e?.code === 8084) {
      ElMessage.warning('AI生成任务正在处理中，请稍候')
      refresh()
      return
    }
    ElMessage.error('提交AI生成失败')
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
      subjectivity: newItem.subjective ? 'SUBJECTIVE' : 'OBJECTIVE',
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
        subjectivity: item.subjective ? 'SUBJECTIVE' : 'OBJECTIVE',
        score: item.score,
      })
    }
  } catch {
    ElMessage.error('保存失败')
    return
  }

  // 推进阶段到"文档集成"，后端会自动执行文档集成
  try {
    await projectApi.advancePhase(props.projectId, 4)
  } catch (e: any) {
    ElMessage.warning(e?.message || '阶段推进失败，可手动进入下一步')
  }

  emit('next')
}

onMounted(loadReviewItems)
</script>

<style scoped lang="scss">
// ============================================================
// 评分摘要栏
// ============================================================
.score-summary {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
  padding: 16px;
  background: var(--app-bg-tertiary);
  border-radius: 8px;
}

.score-item {
  flex: 1;
  text-align: center;
}

.score-label {
  font-size: 13px;
  color: var(--app-text-secondary);
  margin-bottom: 4px;
}

.score-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--app-text-primary);

  &.info {
    color: var(--app-brand-color);
  }

  &.success {
    color: var(--app-color-success);
  }

  &.warning {
    color: var(--app-color-warning);
  }
}

// ============================================================
// 评分说明 / 建议
// ============================================================
.score-notice {
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 6px;
  font-size: 13px;
  color: var(--app-text-secondary);
  line-height: 1.6;

  strong {
    font-weight: 600;
  }
}

.info-notice {
  background: var(--app-bg-tertiary);

  strong {
    color: var(--app-brand-color);
  }
}

.warning-notice {
  background: var(--app-color-warning-light);
  border: 1px solid rgba(230, 162, 60, 0.2);

  strong {
    color: var(--app-color-warning);
  }
}

// ============================================================
// Tabs 样式
// ============================================================
.review-tabs-wrap {
  margin-bottom: 8px;
}

.review-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 2px;
    background: var(--app-border-light);
  }

  :deep(.el-tabs__active-bar) {
    height: 2px;
    background-color: var(--app-brand-color);
  }

  :deep(.el-tabs__item) {
    font-size: 14px;
    font-weight: 500;
    color: var(--app-text-secondary);
    padding: 0 24px;
    height: 46px;
    line-height: 46px;

    &.is-active {
      color: var(--app-brand-color);
    }

    &:hover {
      color: var(--app-brand-color);
    }
  }

  :deep(.el-tabs__content) {
    padding: 0;
  }
}

// ============================================================
// 表格容器
// ============================================================
.table-container {
  border: 1px solid var(--app-border-light);
  border-radius: 8px;
  overflow: hidden;
}

// ============================================================
// 表格样式
// ============================================================
.review-table {
  --el-table-border-color: var(--app-border-light);
  --el-table-header-bg-color: var(--app-bg-tertiary);
  --el-table-row-hover-bg-color: var(--app-hover-state);
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-text-color: var(--app-text-primary);
  --el-table-text-color: var(--app-text-primary);
  --el-table-current-row-bg-color: var(--app-hover-state);

  :deep(th.el-table__cell) {
    font-size: 13px;
    font-weight: 600;
    background: var(--app-bg-tertiary) !important;
  }

  :deep(td.el-table__cell) {
    font-size: 13px;
    border-bottom: 1px solid var(--app-border-light);
  }

  :deep(.el-table__body tr:last-child td.el-table__cell) {
    border-bottom: none;
  }

  :deep(.el-table__inner-wrapper::before) {
    display: none;
  }

  // textarea 输入框
  .table-textarea {
    :deep(.el-textarea__inner) {
      background: var(--app-input-bg);
      border: 1px solid var(--app-border-light);
      border-radius: 4px;
      color: var(--app-text-primary);
      font-size: 13px;
      font-family: inherit;
      resize: vertical;
      min-height: 60px;
      padding: 6px 10px;

      &:focus {
        outline: none;
        border-color: var(--app-brand-color);
        background: var(--app-bg-elevated);
      }
    }
  }

  // 主观/客观选择框
  .subjective-select {
    width: 100%;

    :deep(.el-input__wrapper) {
      background: var(--app-input-bg);
      border: 1px solid var(--app-border-light);
      border-radius: 4px;
      box-shadow: none;
      color: var(--app-text-primary);
      font-size: 13px;

      &:hover {
        border-color: var(--app-border-medium);
      }
    }
  }

  // 分值输入
  .score-input {
    width: 90px;

    :deep(.el-input__wrapper) {
      background: var(--app-input-bg);
      border: 1px solid var(--app-border-light);
      border-radius: 4px;
      box-shadow: none;
      text-align: center;

      .el-input__inner {
        text-align: center;
        font-size: 13px;
        color: var(--app-text-primary);
      }
    }
  }

  // 删除按钮
  .delete-btn {
    padding: 6px;
    border-radius: 4px;
    color: var(--app-color-danger);

    &:hover {
      background: var(--app-color-danger-light);
    }
  }
}

// ============================================================
// 添加评审项按钮
// ============================================================
.add-item-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 12px;
  margin-top: 12px;
  background: transparent;
  border: 1px dashed var(--app-border-light);
  border-radius: 6px;
  color: var(--app-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: var(--app-transition-base);

  &:hover {
    background: var(--app-hover-state);
    border-color: var(--app-brand-color);
    color: var(--app-brand-color);
  }
}

// ============================================================
// 底部操作栏
// ============================================================
.form-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 24px;
  margin-top: 16px;
  background: var(--app-bg-tertiary);
  border-top: 1px solid var(--app-border-light);
  border-radius: 8px;
}

.actions-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.actions-right {
  display: flex;
  gap: 12px;
}
</style>
