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
      <template v-if="isWeightMode">
        <!-- 权重模式：每评分类型显示权重%输入（绑定 level-1 根节点 weight）+ 类型内满分 -->
        <template v-for="typeConfig in enabledTypes" :key="typeConfig.reviewType">
          <div v-if="typeConfig.reviewType === 'COMPLIANCE'" class="score-item">
            <div class="score-label">符合性审查</div>
            <div class="score-value info">通过/不通过</div>
          </div>
          <div v-else class="score-item">
            <div class="score-label">{{ REVIEW_TYPE_LABELS[typeConfig.reviewType] }}</div>
            <div class="score-value weight-value">
              <el-input-number
                v-if="rootsByType[typeConfig.reviewType]"
                v-model="rootsByType[typeConfig.reviewType]!.weight"
                :min="0"
                :max="100"
                :precision="1"
                :step="5"
                size="small"
                class="weight-input"
                :disabled="isEditingDisabled"
              />
              <span v-else>—</span>
              <span class="weight-unit">%</span>
            </div>
            <div class="score-sub">类型满分 {{ typeInternalScore(typeConfig.reviewType) }}/100</div>
          </div>
        </template>
        <div v-if="scoringTypes.length > 0" class="score-item">
          <div class="score-label">权重合计</div>
          <div class="score-value" :class="weightTotal === 100 ? 'success' : weightTotal > 0 ? 'warning' : ''">
            {{ weightTotal }}%
          </div>
          <div class="score-sub">须等于100%</div>
        </div>
      </template>
      <template v-else>
        <!-- 分值模式：各类分值直接计入总分 -->
        <template v-for="typeConfig in enabledTypes" :key="typeConfig.reviewType">
          <div v-if="typeConfig.reviewType === 'COMPLIANCE'" class="score-item">
            <div class="score-label">符合性审查</div>
            <div class="score-value info">通过/不通过</div>
          </div>
          <div v-else-if="typeConfig.reviewType === 'CREDIT'" class="score-item">
            <div class="score-label">资信评审</div>
            <div class="score-value">{{ creditScore }}分</div>
          </div>
          <div v-else-if="typeConfig.reviewType === 'TECHNICAL'" class="score-item">
            <div class="score-label">技术评审</div>
            <div class="score-value">{{ technicalScore }}分</div>
          </div>
          <div v-else-if="typeConfig.reviewType === 'COMMERCIAL'" class="score-item">
            <div class="score-label">商务评审</div>
            <div class="score-value">{{ commercialScore }}分</div>
          </div>
        </template>
        <div v-if="scoringTypes.length > 0" class="score-item">
          <div class="score-label">合计总分</div>
          <div class="score-value" :class="scoreTotal === 100 ? 'success' : scoreTotal > 0 ? 'warning' : ''">
            {{ scoreTotal }}分
          </div>
        </div>
      </template>
    </div>

    <!-- 评分说明 -->
    <div v-if="scoringTypes.length > 0" class="score-notice info-notice">
      <strong>评分说明：</strong>
      <template v-if="isWeightMode">
        {{ scoringTypes.map(t => REVIEW_TYPE_LABELS[t.reviewType]).join('、') }}各类型权重%合计必须为100%，且每个类型内分值合计为100分。
      </template>
      <template v-else>
        {{ scoringTypes.map(t => REVIEW_TYPE_LABELS[t.reviewType]).join('、') }}合计总分必须为100分。允许其中一项或两项为0分。
      </template>
    </div>

    <!-- 评分建议（仅分值模式） -->
    <div v-if="scoringTypes.length > 0 && !isWeightMode" class="score-notice warning-notice">
      <strong>评分建议：</strong>建议货物类项目商务分30-60，资信10-25分；建议服务类项目商务分10-30，资信10-25分。
    </div>

    <!-- 评审类型Tabs -->
    <div class="review-tabs-wrap">
      <el-tabs v-model="activeReviewType" class="review-tabs">
        <el-tab-pane
          v-for="typeConfig in enabledTypes"
          :key="typeConfig.reviewType"
          :label="REVIEW_TYPE_LABELS[typeConfig.reviewType]"
          :name="typeConfig.reviewType"
        >
          <!-- 符合性审查：只有评审标准+操作 -->
            <template v-if="typeConfig.reviewType === 'COMPLIANCE'">
              <div class="table-container">
                <el-table
                  :data="complianceTree"
                  row-key="id"
                  :tree-props="{ children: 'children' }"
                  class="review-table"
                  :border="true"
                  default-expand-all
                >
                  <el-table-column label="评审项" min-width="100">
                    <template #default="{ row }">
                      <el-input
                        v-model="row.itemName"
                        type="textarea"
                        :rows="2"
                        placeholder="请输入评审项"
                        class="table-textarea"
                        :disabled="isEditingDisabled"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="评审标准" min-width="450">
                    <template #default="{ row }">
                      <el-input
                        v-model="row.itemContent"
                        type="textarea"
                        :rows="2"
                        placeholder="请输入评审标准"
                        class="table-textarea"
                        :disabled="isEditingDisabled"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column v-if="!isEditingDisabled" label="操作" width="130" align="center">
                    <template #default="{ row }">
                      <el-button
                        v-if="row.level < MAX_LEVEL"
                        link
                        type="primary"
                        class="add-child-btn"
                        @click="handleAddChild(row)"
                      >
                        <el-icon :size="14"><Plus /></el-icon> 子项
                      </el-button>
                      <el-button link type="danger" class="delete-btn" @click="handleDeleteItem(row)">
                        <el-icon :size="16"><Delete /></el-icon>
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
              <div v-if="!isEditingDisabled" class="add-item-btn" @click="handleAddItem('COMPLIANCE')">
                <el-icon><Plus /></el-icon> 添加评审项
              </div>
            </template>

            <!-- 资信评审：评审项+评审标准+主观/客观+分值 -->
            <template v-else-if="typeConfig.reviewType === 'CREDIT'">
              <div class="table-container">
                <el-table
                  :data="creditTree"
                  row-key="id"
                  :tree-props="{ children: 'children' }"
                  class="review-table"
                  :border="true"
                  default-expand-all
                >
                  <el-table-column label="评审项" min-width="100">
                    <template #default="{ row }">
                      <el-input v-model="row.itemName" type="textarea" :rows="2" placeholder="请输入评审项" class="table-textarea" :disabled="isEditingDisabled" />
                    </template>
                  </el-table-column>
                  <el-table-column label="评审标准" min-width="450">
                    <template #default="{ row }">
                      <el-input v-model="row.itemContent" type="textarea" :rows="2" placeholder="请输入评审标准" class="table-textarea" :disabled="isEditingDisabled" />
                    </template>
                  </el-table-column>
                  <el-table-column v-if="shouldShowSubjectivity(typeConfig)" label="主观/客观" width="120" align="center">
                    <template #default="{ row }">
                      <el-select
                        v-if="isLeaf(row)"
                        v-model="row.subjectivity"
                        size="small"
                        class="subjective-select"
                        :disabled="isEditingDisabled"
                      >
                        <el-option value="OBJECTIVE" label="客观" />
                        <el-option value="SUBJECTIVE" label="主观" />
                      </el-select>
                      <span v-else class="summary-text">&mdash;</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="分值" width="100" align="center">
                    <template #default="{ row }">
                      <el-input-number
                        v-if="isLeaf(row)"
                        v-model="row.score"
                        :min="0"
                        :max="100"
                        :precision="1"
                        size="small"
                        class="score-input"
                        controls-position="right"
                        :disabled="isEditingDisabled"
                      />
                      <span v-else class="summary-text">{{ calcNodeScore(row) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column v-if="!isEditingDisabled" label="操作" width="130" align="center">
                    <template #default="{ row }">
                      <el-button
                        v-if="row.level < MAX_LEVEL"
                        link
                        type="primary"
                        class="add-child-btn"
                        @click="handleAddChild(row)"
                      >
                        <el-icon :size="14"><Plus /></el-icon> 子项
                      </el-button>
                      <el-button link type="danger" class="delete-btn" @click="handleDeleteItem(row)">
                        <el-icon :size="16"><Delete /></el-icon>
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
              <div v-if="!isEditingDisabled" class="add-item-btn" @click="handleAddItem('CREDIT')">
                <el-icon><Plus /></el-icon> 添加评审项
              </div>
            </template>

            <!-- 技术评审：评审项+评审标准+主观/客观+分值 -->
            <template v-else-if="typeConfig.reviewType === 'TECHNICAL'">
              <div class="table-container">
                <el-table
                  :data="technicalTree"
                  row-key="id"
                  :tree-props="{ children: 'children' }"
                  class="review-table"
                  :border="true"
                  default-expand-all
                >
                  <el-table-column label="评审项" min-width="100">
                    <template #default="{ row }">
                      <el-input v-model="row.itemName" type="textarea" :rows="2" placeholder="请输入评审项" class="table-textarea" :disabled="isEditingDisabled" />
                    </template>
                  </el-table-column>
                  <el-table-column label="评审标准" min-width="450">
                    <template #default="{ row }">
                      <el-input v-model="row.itemContent" type="textarea" :rows="2" placeholder="请输入评审标准" class="table-textarea" :disabled="isEditingDisabled" />
                    </template>
                  </el-table-column>
                  <el-table-column v-if="shouldShowSubjectivity(typeConfig)" label="主观/客观" width="120" align="center">
                    <template #default="{ row }">
                      <el-select
                        v-if="isLeaf(row)"
                        v-model="row.subjectivity"
                        size="small"
                        class="subjective-select"
                        :disabled="isEditingDisabled"
                      >
                        <el-option value="OBJECTIVE" label="客观" />
                        <el-option value="SUBJECTIVE" label="主观" />
                      </el-select>
                      <span v-else class="summary-text">&mdash;</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="分值" width="100" align="center">
                    <template #default="{ row }">
                      <el-input-number
                        v-if="isLeaf(row)"
                        v-model="row.score"
                        :min="0"
                        :max="100"
                        :precision="1"
                        size="small"
                        class="score-input"
                        controls-position="right"
                        :disabled="isEditingDisabled"
                      />
                      <span v-else class="summary-text">{{ calcNodeScore(row) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column v-if="!isEditingDisabled" label="操作" width="130" align="center">
                    <template #default="{ row }">
                      <el-button
                        v-if="row.level < MAX_LEVEL"
                        link
                        type="primary"
                        class="add-child-btn"
                        @click="handleAddChild(row)"
                      >
                        <el-icon :size="14"><Plus /></el-icon> 子项
                      </el-button>
                      <el-button link type="danger" class="delete-btn" @click="handleDeleteItem(row)">
                        <el-icon :size="16"><Delete /></el-icon>
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
              <div v-if="!isEditingDisabled" class="add-item-btn" @click="handleAddItem('TECHNICAL')">
                <el-icon><Plus /></el-icon> 添加评审项
              </div>
            </template>

            <!-- 商务评审：评审项+评审标准+分值 -->
            <template v-else-if="typeConfig.reviewType === 'COMMERCIAL'">
              <div class="table-container">
                <el-table
                  :data="commercialTree"
                  row-key="id"
                  :tree-props="{ children: 'children' }"
                  class="review-table"
                  :border="true"
                  default-expand-all
                >
                  <el-table-column label="评审项" min-width="100">
                    <template #default="{ row }">
                      <el-input v-model="row.itemName" type="textarea" :rows="2" placeholder="请输入评审项" class="table-textarea" :disabled="isEditingDisabled" />
                    </template>
                  </el-table-column>
                  <el-table-column label="评审标准" min-width="450">
                    <template #default="{ row }">
                      <el-input v-model="row.itemContent" type="textarea" :rows="2" placeholder="请输入评审标准" class="table-textarea" :disabled="isEditingDisabled" />
                    </template>
                  </el-table-column>
                  <el-table-column label="分值" width="100" align="center">
                    <template #default="{ row }">
                      <el-input-number
                        v-if="isLeaf(row)"
                        v-model="row.score"
                        :min="0"
                        :max="100"
                        :precision="1"
                        size="small"
                        class="score-input"
                        controls-position="right"
                        :disabled="isEditingDisabled"
                      />
                      <span v-else class="summary-text">{{ calcNodeScore(row) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column v-if="!isEditingDisabled" label="操作" width="130" align="center">
                    <template #default="{ row }">
                      <el-button
                        v-if="row.level < MAX_LEVEL"
                        link
                        type="primary"
                        class="add-child-btn"
                        @click="handleAddChild(row)"
                      >
                        <el-icon :size="14"><Plus /></el-icon> 子项
                      </el-button>
                      <el-button link type="danger" class="delete-btn" @click="handleDeleteItem(row)">
                        <el-icon :size="16"><Delete /></el-icon>
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
              <div v-if="!isEditingDisabled" class="add-item-btn" @click="handleAddItem('COMMERCIAL')">
                <el-icon><Plus /></el-icon> 添加评审项
              </div>
            </template>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 底部操作栏 -->
    <div class="form-actions">
      <div class="actions-left">
        <el-button @click="$emit('prev')">
          <el-icon><ArrowLeft /></el-icon> 上一步
        </el-button>
        <el-button v-if="!isEditingDisabled" type="warning" :disabled="!canCreateNew" @click="handleGenerate">
          <el-icon><RefreshRight /></el-icon> 重新生成
        </el-button>
      </div>
      <div v-if="!isEditingDisabled" class="actions-right">
        <el-button type="primary" :disabled="!canCreateNew" @click="handleNext">
          确认评审项 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, ArrowLeft, ArrowRight, RefreshRight } from '@element-plus/icons-vue'
import { reviewApi } from '@/api/review'
import { projectApi } from '@/api/project'
import { projectTemplateApi } from '@/api/projectTemplate'
import { useLatestTask } from '@/composables/useLatestTask'
import { getTaskProgress, isTaskResultSyncing, isTaskSucceeded } from '@/types/ai-task'
import GenerationStatusCard from '@/components/GenerationStatusCard.vue'
import type { ReviewItemTree, ReviewConfig, ReviewTypeConfig } from '@/types/review'
import { REVIEW_TYPE_LABELS } from '@/types/review'

type ReviewCategory = 'COMPLIANCE' | 'TECHNICAL' | 'CREDIT' | 'COMMERCIAL'

const MAX_LEVEL = 3

const props = defineProps<{ projectId: number; readonly?: boolean }>()
const emit = defineEmits<{ next: []; prev: [] }>()

const allItems = ref<ReviewItemTree[]>([])
const activeReviewType = ref<string>('COMPLIANCE')
const isCreatingGenerationTask = ref(false)
let tempIdSeed = -1

// 评审项配置
const reviewConfig = ref<ReviewConfig | null>(null)

/** 获取启用的评审类型配置列表 */
const enabledTypes = computed<ReviewTypeConfig[]>(() => {
  if (!reviewConfig.value) {
    // 兜底：无配置时全部启用
    return Object.keys(REVIEW_TYPE_LABELS).map(type => ({
      reviewType: type,
      enabled: true,
      generateStandard: true,
      distinguishSubjectivity: type === 'TECHNICAL' || type === 'CREDIT',
    }))
  }
  return reviewConfig.value.reviewTypes.filter(t => t.enabled)
})

/**
 * 该评审类型是否展示"主观/客观"列
 * distinguishSubjectivity 非 undefined 时按其值；undefined（老数据）回退到 CREDIT/TECHNICAL 展示
 */
const shouldShowSubjectivity = (typeConfig: ReviewTypeConfig): boolean => {
  if (typeConfig.distinguishSubjectivity !== undefined) return typeConfig.distinguishSubjectivity
  return typeConfig.reviewType === 'CREDIT' || typeConfig.reviewType === 'TECHNICAL'
}

/** 当 enabledTypes 变化时，修正 activeReviewType 为首个启用的类型 */
watch(enabledTypes, (types) => {
  if (types?.length > 0 && !types.some(t => t.reviewType === activeReviewType.value)) {
    activeReviewType.value = types[0]!.reviewType
  }
}, { immediate: true })

// 使用 useLatestTask 查询最新任务状态
const projectIdRef = computed(() => props.projectId)
const { latestTask, canCreateNew, setActive, refresh } = useLatestTask(
  'REVIEW_ITEM_GENERATE',
  projectIdRef,
  'REVIEW_ITEM',
  () => {
    // resultSynced=1 时业务数据已同步，直接加载
    loadReviewItems()
  },
)


watch(latestTask, (task) => {
  if (task && isCreatingGenerationTask.value) {
    isCreatingGenerationTask.value = false
  }
})

/** 将扁平列表组装为树形结构 */
function buildTree(flatList: any[]): ReviewItemTree[] {
  const map = new Map<number, ReviewItemTree>()
  const roots: ReviewItemTree[] = []

  for (const item of flatList) {
    map.set(item.id, { ...item, children: [] })
  }

  for (const item of flatList) {
    const node = map.get(item.id)!
    const parentId = item.parentId
    if (parentId && map.has(parentId)) {
      map.get(parentId)!.children.push(node)
    } else {
      if (parentId && !map.has(parentId)) {
        console.warn(`[buildTree] 孤儿节点: id=${item.id}, parentId=${parentId} 不存在`)
      }
      roots.push(node)
    }
  }

  return roots
}

/** 判断是否叶子节点 */
function isLeaf(row: ReviewItemTree): boolean {
  return !row.children || row.children.length === 0
}

function nextTempId(): number {
  return tempIdSeed--
}

function isCategoryRoot(row: ReviewItemTree): boolean {
  const reviewType = row.reviewType || ''
  return row.level === 1 && row.itemName === REVIEW_TYPE_LABELS[reviewType]
}

function createLocalItem(type: ReviewCategory, parent: ReviewItemTree | null, sortOrder: number): ReviewItemTree {
  return {
    id: nextTempId(),
    projectId: props.projectId,
    parentId: parent?.id ?? null,
    level: parent ? parent.level + 1 : 1,
    itemName: parent ? '' : (REVIEW_TYPE_LABELS[type] || type),
    itemContent: '',
    sortOrder,
    reviewType: type,
    score: 0,
    maxScore: undefined,
    weight: undefined,
    subjectivity: 'OBJECTIVE',
    isRequired: parent ? 1 : 0,
    createTime: '',
    children: [],
  }
}

function ensureCategoryRoot(type: ReviewCategory): ReviewItemTree {
  let categoryRoot = allItems.value.find(i => i.reviewType === type && i.level === 1 && (isCategoryRoot(i) || i.children?.length))
  if (!categoryRoot) {
    categoryRoot = createLocalItem(type, null, allItems.value.filter(i => i.reviewType === type).length)
    allItems.value.push(categoryRoot)
  }
  return categoryRoot
}

function removeNodeById(nodes: ReviewItemTree[], id: number): boolean {
  const index = nodes.findIndex(node => node.id === id)
  if (index >= 0) {
    nodes.splice(index, 1)
    return true
  }
  for (const node of nodes) {
    if (node.children?.length && removeNodeById(node.children, id)) {
      return true
    }
  }
  return false
}

/** 计算节点的子项分值汇总 */
function calcNodeScore(row: ReviewItemTree): number {
  if (isLeaf(row)) return row.score || 0
  return sumLeafScores(row)
}

/** 递归汇总叶子节点分值 */
function sumLeafScores(node: ReviewItemTree): number {
  if (isLeaf(node)) return node.score || 0
  return node.children.reduce((sum, child) => sum + sumLeafScores(child), 0)
}

/** 收集某类型下所有叶子节点 */
function collectLeaves(nodes: ReviewItemTree[]): ReviewItemTree[] {
  const result: ReviewItemTree[] = []
  function walk(list: ReviewItemTree[]) {
    for (const node of list) {
      if (isLeaf(node)) {
        result.push(node)
      } else {
        walk(node.children)
      }
    }
  }
  walk(nodes)
  return result
}

/** 展开分类根节点：若level=1节点有子项，直接显示其子项（避免与Tab标签重复） */
function unwrapCategoryRoots(roots: ReviewItemTree[]): ReviewItemTree[] {
  const result: ReviewItemTree[] = []
  for (const node of roots) {
    if (node.level === 1 && (node.children?.length || isCategoryRoot(node))) {
      result.push(...node.children)
    } else {
      result.push(node)
    }
  }
  return result
}

// 按 reviewType 分组的树形数据（展开分类根节点，避免与Tab标签重复）
const complianceTree = computed(() => unwrapCategoryRoots(allItems.value.filter(i => i.reviewType === 'COMPLIANCE')))
const creditTree = computed(() => unwrapCategoryRoots(allItems.value.filter(i => i.reviewType === 'CREDIT')))
const technicalTree = computed(() => unwrapCategoryRoots(allItems.value.filter(i => i.reviewType === 'TECHNICAL')))
const commercialTree = computed(() => unwrapCategoryRoots(allItems.value.filter(i => i.reviewType === 'COMMERCIAL')))

/** 获取指定评审类型的树形数据 */
const getTreeByType = (reviewType: string): ReviewItemTree[] => {
  const map: Record<string, any> = {
    COMPLIANCE: complianceTree,
    CREDIT: creditTree,
    TECHNICAL: technicalTree,
    COMMERCIAL: commercialTree,
  }
  return map[reviewType]?.value || []
}

// 评分计算：只统计启用的非符合性类型叶子节点
const creditScore = computed(() => collectLeaves(creditTree.value).reduce((sum, i) => sum + (i.score || 0), 0))
const technicalScore = computed(() => collectLeaves(technicalTree.value).reduce((sum, i) => sum + (i.score || 0), 0))
const commercialScore = computed(() => collectLeaves(commercialTree.value).reduce((sum, i) => sum + (i.score || 0), 0))

/** 获取参与评分合计的类型列表（启用的非符合性类型） */
const scoringTypes = computed(() =>
  enabledTypes.value.filter(t => t.reviewType !== 'COMPLIANCE')
)

/** 权重模式：scoreMode === 'WEIGHT'（未配置视为分值模式） */
const isWeightMode = computed(() => reviewConfig.value?.scoreMode === 'WEIGHT')

/** 各评分类型的 level-1 根节点（权重模式下权重%存在根节点 weight 字段） */
const rootsByType = computed<Record<string, ReviewItemTree | undefined>>(() => {
  const map: Record<string, ReviewItemTree | undefined> = {}
  for (const t of ['CREDIT', 'TECHNICAL', 'COMMERCIAL']) {
    map[t] = allItems.value.find(i => i.reviewType === t && i.level === 1)
  }
  return map
})

/** 某评分类型的类型内分值合计（权重模式下每类型应为100） */
const typeInternalScore = (reviewType: string): number => {
  const root = rootsByType.value[reviewType]
  if (!root) return 0
  // 归一化到1位小数，避免浮点累加误差导致 !== 100 误判
  return Math.round(sumLeafScores(root) * 10) / 10
}

/** 权重合计（权重模式，应为100%） */
const weightTotal = computed(() => {
  const sum = scoringTypes.value.reduce((sum, t) => {
    const root = rootsByType.value[t.reviewType]
    return sum + (root?.weight || 0)
  }, 0)
  // 归一化到1位小数，避免 40.1+30.2+29.7=99.999... 误判
  return Math.round(sum * 10) / 10
})

const scoreTotal = computed(() => {
  return scoringTypes.value.reduce((sum, t) => {
    const tree = getTreeByType(t.reviewType)
    return sum + collectLeaves(tree).reduce((s, i) => s + (i.score || 0), 0)
  }, 0)
})

const progressPercent = computed(() => {
  return getTaskProgress(latestTask.value)
})

const isGenerating = computed(() => {
  if (isCreatingGenerationTask.value) return true
  const task = latestTask.value
  if (!task || isTaskSucceeded(task)) return false
  return task.status === 'PENDING' || task.status === 'PROCESSING' || isTaskResultSyncing(task)
})

const isEditingDisabled = computed(() => props.readonly || isGenerating.value)

const loadReviewItems = async () => {
  const data = await reviewApi.getTree(props.projectId)
  tempIdSeed = -1
  const flatList = (data || []).map((item: any) => ({
    id: item.id,
    projectId: item.projectId,
    parentId: item.parentId || null,
    level: item.level ?? 1,
    itemName: item.itemName || '',
    itemContent: item.itemContent || '',
    sortOrder: item.sortOrder ?? 0,
    reviewType: item.reviewType || 'COMPLIANCE',
    score: item.score ?? 0,
    maxScore: item.maxScore ?? null,
    weight: item.weight ?? null,
    subjectivity: item.subjectivity || 'OBJECTIVE',
    isRequired: item.isRequired ?? null,
    createTime: item.createTime || '',
  }))
  allItems.value = buildTree(flatList)
}

const handleGenerate = async () => {
  await refresh()
  if (!canCreateNew.value) {
    ElMessage.warning('AI生成任务正在处理中，请稍候')
    return
  }
  const originalItems = allItems.value
  isCreatingGenerationTask.value = true
  try {
    const res = await reviewApi.generate(props.projectId, {})
    allItems.value = []
    setActive(res.id)
  } catch (e: any) {
    allItems.value = originalItems
    isCreatingGenerationTask.value = false
    if (e?.code === 8084) {
      ElMessage.warning('AI生成任务正在处理中，请稍候')
      refresh()
      return
    }
    ElMessage.error('提交AI生成失败')
  }
}

/** 添加评审项（优先添加为分类根节点的子项，避免创建重复的分类层级） */
const handleAddItem = (type: ReviewCategory) => {
  const categoryRoot = ensureCategoryRoot(type)
  const newItem = createLocalItem(type, categoryRoot, categoryRoot.children?.length || 0)
  categoryRoot.children = [...(categoryRoot.children || []), newItem]
}

/** 添加子评审项 */
const handleAddChild = (parent: ReviewItemTree) => {
  if (parent.level >= MAX_LEVEL) {
    ElMessage.warning('评审项最多支持3级')
    return
  }
  const type = (parent.reviewType || activeReviewType.value) as ReviewCategory
  const newItem = createLocalItem(type, parent, parent.children?.length || 0)
  parent.children = [...(parent.children || []), newItem]
}

/** 删除评审项 */
const handleDeleteItem = async (row: ReviewItemTree) => {
  const hasChildren = !isLeaf(row)
  const msg = hasChildren
    ? `确定删除「${row.itemName || '该项'}」及其所有子项？`
    : `确定删除「${row.itemName || '该项'}」？`
  try {
    await ElMessageBox.confirm(msg, '确认')
  } catch {
    return
  }
  try {
    removeNodeById(allItems.value, row.id)
    ElMessage.success('删除成功')
  } catch {
    ElMessage.error('删除失败')
  }
}

/** 确认评审项并推进阶段 */
const handleNext = async () => {
  if (isWeightMode.value) {
    // 权重模式：校验权重合计100% + 每类型内分值合计100
    if (weightTotal.value !== 100) {
      ElMessage.warning(`各类型权重合计应为100%，当前为${weightTotal.value}%`)
      return
    }
    const badType = scoringTypes.value.find(t => typeInternalScore(t.reviewType) !== 100)
    if (badType) {
      ElMessage.warning(`${REVIEW_TYPE_LABELS[badType.reviewType]}类型内分值合计应为100分，当前为${typeInternalScore(badType.reviewType)}分`)
      return
    }
  } else {
    // 分值模式：校验合计100分（仅校验启用且生成评审标准的非符合性类型）
    const scoringTypeNames = scoringTypes.value
      .map(t => REVIEW_TYPE_LABELS[t.reviewType] || t.reviewType)
      .join('+')
    if (scoringTypes.value.length > 0 && Math.abs(scoreTotal.value - 100) > 0.001) {
      ElMessage.warning(`${scoringTypeNames}评审合计应为100分，当前为${scoreTotal.value}分`)
      return
    }
  }

  // 保存所有修改：将树形数据展平后一次性替换
  try {
    const flatItems: any[] = []
    function flatten(nodes: ReviewItemTree[], parentId: number | null = null, level = 1) {
      for (const node of nodes) {
        const currentLevel = parentId == null ? node.level || level : level
        const hasChildren = !isLeaf(node)
        if (!hasChildren && isCategoryRoot(node)) {
          continue
        }
        flatItems.push({
          id: node.id,
          parentId,
          level: currentLevel,
          itemName: node.itemName,
          itemContent: node.itemContent,
          sortOrder: node.sortOrder,
          reviewType: node.reviewType,
          subjectivity: node.subjectivity,
          score: hasChildren ? undefined : node.score,
          // 权重模式下权重%存在 level-1 根节点；其他节点 weight 为 undefined（NOT_NULL 策略不更新）
          weight: node.level === 1 ? node.weight : undefined,
          isRequired: node.isRequired,
        })
        if (hasChildren) flatten(node.children, node.id, currentLevel + 1)
      }
    }
    flatten(allItems.value)

    if (flatItems.length === 0) {
      ElMessage.warning('请至少保留一个评审项')
      return
    }

    await reviewApi.replaceAll(props.projectId, flatItems)
    await loadReviewItems()
  } catch {
    ElMessage.error('保存失败')
    return
  }

  // 推进阶段
  try {
    await projectApi.advancePhase(props.projectId, 4)
    emit('next')
  } catch (e: any) {
    ElMessage.warning(e?.message || '阶段推进失败，请稍后重试')
  }
}

/** 加载项目模板的评审项配置 */
const loadReviewConfig = async () => {
  try {
    const pt = await projectTemplateApi.getByProject(props.projectId)
    if (pt?.reviewConfig) {
      reviewConfig.value = typeof pt.reviewConfig === 'string'
        ? JSON.parse(pt.reviewConfig)
        : pt.reviewConfig
    }
  } catch {
    // 无模板配置，使用默认（全部启用）
  }
}

onMounted(() => {
  loadReviewConfig()
  loadReviewItems()
})
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

// 权重模式摘要
.weight-value {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.weight-input {
  width: 90px;

  :deep(.el-input__wrapper) {
    background: var(--app-input-bg);
    border: 1px solid var(--app-border-light);
    border-radius: 4px;
    box-shadow: none;

    .el-input__inner {
      text-align: center;
      font-size: 14px;
      font-weight: 600;
      color: var(--app-text-primary);
    }
  }
}

.weight-unit {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-secondary);
}

.score-sub {
  font-size: 12px;
  color: var(--app-text-secondary);
  margin-top: 4px;
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

  :deep(.el-table__expand-icon) {
    color: var(--app-text-secondary);
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

  // 汇总文本
  .summary-text {
    font-weight: 600;
    color: var(--app-text-secondary);
  }

  // 添加子项按钮
  .add-child-btn {
    padding: 4px;
    border-radius: 4px;

    &:hover {
      background: var(--app-brand-color-light, rgba(64, 158, 255, 0.1));
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
// 占位提示
// ============================================================
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
