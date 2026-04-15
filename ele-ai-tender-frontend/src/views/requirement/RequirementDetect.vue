<template>
  <div class="requirement-detect">
    <!-- 顶部工具栏 -->
    <div class="detect-toolbar">
      <div class="toolbar-left">
        <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
        <el-divider direction="vertical" />
        <span class="toolbar-title">{{ requirementName || '业务需求智能检测' }}</span>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="detect-body">
      <!-- 加载中 -->
      <div v-if="submitting" class="detect-loading">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>正在提交检测任务...</p>
      </div>

      <template v-else>
        <!-- 卡片1：检测进度 -->
        <div class="detect-card progress-card">
          <div class="card-header">
            <span class="card-title">检测进度</span>
            <el-button
              v-if="allCompleted"
              :icon="RefreshRight"
              size="small"
              :loading="reDetecting"
              @click="handleReDetect"
            >
              重新检测
            </el-button>
          </div>
          <el-progress
            :percentage="overallProgress"
            :stroke-width="8"
            :status="allCompleted ? 'success' : ''"
          />
          <span class="progress-text">{{ overallProgress }}% 完成</span>
        </div>

        <!-- 卡片2：检测结果汇总 -->
        <div v-if="allCompleted" class="detect-card summary-card">
          <div class="card-header">
            <span class="card-title">检测结果汇总</span>
          </div>
          <div class="summary-items">
            <div
              v-for="card in detectCards"
              :key="card.type"
              class="summary-item"
              :class="{ 'has-issues': card.issueCount > 0, 'no-issues': card.issueCount === 0 }"
            >
              <div class="summary-number">{{ card.issueCount }}</div>
              <div class="summary-label">{{ card.label }}</div>
            </div>
          </div>
        </div>

        <!-- 卡片3：检测详情 -->
        <div class="detect-card detail-card">
          <div class="card-header">
            <span class="card-title">检测详情</span>
          </div>

          <div v-if="issues.length > 0" class="detail-list">
            <div
              v-for="(issue, idx) in issues"
              :key="idx"
              class="detail-item"
              :class="getIssueClass(issue)"
            >
              <div class="item-header">
                <span class="item-title">{{ getTypeLabel(issue.detectionType) }}</span>
                <el-tag
                  :type="issue.severity === 'HIGH' ? 'danger' : issue.severity === 'MEDIUM' ? 'warning' : 'info'"
                  size="small"
                >
                  {{ issue.severity === 'HIGH' ? '严重' : issue.severity === 'MEDIUM' ? '警告' : '提示' }}
                </el-tag>
              </div>
              <div class="item-body">
                <p class="item-content">{{ issue.description }}</p>
                <p v-if="issue.suggestion" class="item-suggestion">
                  <span class="label">AI建议：</span>
                  <span class="suggestion-text">{{ issue.suggestion }}</span>
                </p>
              </div>
              <div class="item-actions">
                <template v-if="issue.handleStatus === 0">
                  <el-button size="small" type="success" @click="handleAccept(issue, idx)">
                    接受建议
                  </el-button>
                  <el-button size="small" type="danger" @click="handleReject(issue, idx)">
                    拒绝建议
                  </el-button>
                </template>
                <el-tag v-else :type="issue.handleStatus === 1 ? 'success' : 'info'" size="small">
                  {{ issue.handleStatus === 1 ? '已处理' : '已拒绝' }}
                </el-tag>
                <el-button size="small" @click="handleViewOriginal(issue)">
                  查看原文
                </el-button>
              </div>
            </div>
          </div>

          <div v-else-if="allCompleted" class="no-issues-tip">
            <el-icon :size="32" color="var(--app-color-success)"><CircleCheck /></el-icon>
            <p>通过检测，未发现问题</p>
          </div>

          <div v-else class="detecting-tip">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>正在检测中，请稍候...</span>
          </div>
        </div>

        <!-- 卡片4：检测结论 -->
        <div v-if="allCompleted" class="detect-card conclusion-card">
          <div class="card-header">
            <span class="card-title">检测结论</span>
          </div>
          <div class="conclusion-body">
            <p v-for="card in detectCards" :key="card.type" class="conclusion-line">
              <strong>{{ card.label }}：</strong>
              <template v-if="card.issueCount > 0">发现 {{ card.issueCount }} 个问题，建议进行修改。</template>
              <template v-else>未发现问题，通过检测。</template>
            </p>
            <p v-if="totalUnhandledIssues > 0" class="conclusion-warning">
              还有 {{ totalUnhandledIssues }} 个未处理的问题，建议处理后再提交。
            </p>
            <p v-else class="conclusion-success">
              所有问题已处理完毕，可以提交。
            </p>
          </div>
          <div class="conclusion-actions">
            <el-button @click="router.push(`/requirement/generate/${requirementId}`)">
              上一步
            </el-button>
            <el-button type="primary" :disabled="totalUnhandledIssues > 0" @click="handleFinish">
              完成检测
            </el-button>
          </div>
        </div>
      </template>
    </div>

    <!-- 查看原文弹窗 -->
    <el-dialog v-model="originalVisible" title="查看原文" width="500px">
      <div v-if="currentOriginal" class="original-content">
        <p class="original-text">{{ currentOriginal.location || currentOriginal.description }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Loading, RefreshRight, CircleCheck } from '@element-plus/icons-vue'
import { requirementApi } from '@/api/requirement'
import { aiTaskApi } from '@/api/ai-task'
import type { AiTaskStatus } from '@/types/ai-task'
import type { DetectionType, DetectionIssueVO } from '@/types/detection'

interface DetectCard {
  type: DetectionType
  label: string
  taskId: number | null
  status: AiTaskStatus | ''
  percentage: number
  issueCount: number
  completed: boolean
  failed: boolean
}

const DETECT_TYPE_CONFIG: Record<string, { label: string; icon: string }> = {
  FAIRNESS: { label: '公平竞争检测', icon: 'ScaleToOriginal' },
  COMPLIANCE: { label: '合规性检查', icon: 'DocumentChecked' },
  TYPO: { label: '错别字检查', icon: 'EditPen' },
  SENSITIVE_WORD: { label: '敏感词检测', icon: 'Warning' },
}

const router = useRouter()
const route = useRoute()

// ---- 基础数据 ----
const requirementId = ref(0)
const requirementName = ref('')

// ---- 检测卡片 ----
const detectCards = ref<DetectCard[]>([
  { type: 'FAIRNESS', label: '公平竞争检测', taskId: null, status: '', percentage: 0, issueCount: 0, completed: false, failed: false },
  { type: 'COMPLIANCE', label: '合规性检查', taskId: null, status: '', percentage: 0, issueCount: 0, completed: false, failed: false },
  { type: 'TYPO', label: '错别字检查', taskId: null, status: '', percentage: 0, issueCount: 0, completed: false, failed: false },
  { type: 'SENSITIVE_WORD', label: '敏感词检测', taskId: null, status: '', percentage: 0, issueCount: 0, completed: false, failed: false },
])

// ---- 检测问题 ----
const issues = ref<DetectionIssueVO[]>([])

// ---- 状态 ----
const submitting = ref(false)
const reDetecting = ref(false)
const originalVisible = ref(false)
const currentOriginal = ref<DetectionIssueVO | null>(null)
let pollingTimer: ReturnType<typeof setInterval> | null = null

// ---- 计算属性 ----
const allCompleted = computed(() => detectCards.value.every(c => c.completed || c.failed))

const overallProgress = computed(() => {
  if (detectCards.value.length === 0) return 0
  const total = detectCards.value.reduce((sum, c) => sum + c.percentage, 0)
  return Math.round(total / detectCards.value.length)
})

const totalUnhandledIssues = computed(() =>
  issues.value.filter(i => i.handleStatus === 0).length
)

// ---- 初始化 ----
onMounted(async () => {
  const id = Number(route.params.id)
  if (!id || isNaN(id)) {
    ElMessage.error('参数错误')
    router.back()
    return
  }
  requirementId.value = id

  try {
    const data = await requirementApi.getById(id)
    requirementName.value = data.requirementName || ''
  } catch {
    ElMessage.error('加载需求失败')
    router.back()
    return
  }

  await startDetection()
})

onBeforeUnmount(() => {
  stopPolling()
})

// ---- 提交检测 ----
async function startDetection() {
  submitting.value = true
  try {
    const taskMap = await requirementApi.detect(requirementId.value)
    submitting.value = false

    for (const card of detectCards.value) {
      const taskId = taskMap[card.type]
      if (taskId) {
        card.taskId = taskId
      } else {
        card.completed = true
        card.percentage = 100
      }
    }

    startPolling()
  } catch {
    submitting.value = false
    ElMessage.error('提交检测失败')
  }
}

// ---- 重新检测 ----
async function handleReDetect() {
  reDetecting.value = true
  try {
    // 重置状态
    for (const card of detectCards.value) {
      card.taskId = null
      card.status = ''
      card.percentage = 0
      card.issueCount = 0
      card.completed = false
      card.failed = false
    }
    issues.value = []

    await startDetection()
  } finally {
    reDetecting.value = false
  }
}

// ---- 轮询逻辑 ----
function startPolling() {
  pollingTimer = setInterval(pollTaskStatus, 3000)
  pollTaskStatus()
}

function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

async function pollTaskStatus() {
  const pendingCards = detectCards.value.filter(c => c.taskId && !c.completed && !c.failed)
  if (pendingCards.length === 0) {
    if (allCompleted.value) stopPolling()
    return
  }

  for (const card of pendingCards) {
    try {
      const task = await aiTaskApi.getStatus(card.taskId!)
      card.status = task.status

      if (task.status === 'COMPLETED') {
        card.completed = true
        card.percentage = 100
        parseTaskResult(card, task.result)
      } else if (task.status === 'FAILED' || task.status === 'AI_UNAVAILABLE') {
        card.failed = true
        card.percentage = 100
      } else if (task.status === 'PROCESSING') {
        card.percentage = 60
      } else if (task.status === 'PENDING') {
        card.percentage = 10
      } else if (task.status === 'SKIPPED') {
        card.completed = true
        card.percentage = 100
      }
    } catch {
      // 单个任务轮询失败不影响其他
    }
  }

  if (allCompleted.value) {
    stopPolling()
  }
}

function parseTaskResult(card: DetectCard, resultJson?: string) {
  if (!resultJson) return
  try {
    const result = JSON.parse(resultJson)
    if (typeof result.issueCount === 'number') {
      card.issueCount = result.issueCount
    }
    if (Array.isArray(result.issues)) {
      for (const issue of result.issues) {
        issues.value.push({
          recordId: issue.recordId || 0,
          detectionType: card.type,
          typeName: card.label,
          description: issue.description || '',
          location: issue.location || '',
          suggestion: issue.suggestion || '',
          severity: issue.severity || 'MEDIUM',
          handleStatus: issue.handleStatus || 0,
          issueIndex: issues.value.length,
        })
      }
      card.issueCount = result.issues.length
    }
  } catch {
    // JSON解析失败，忽略
  }
}

// ---- 接受/拒绝建议 ----
async function handleAccept(_issue: DetectionIssueVO, idx: number) {
  try {
    await requirementApi.update(requirementId.value, {} as any)
    if (issues.value[idx]) {
      issues.value[idx].handleStatus = 1
    }
    ElMessage.success('已接受建议')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleReject(_issue: DetectionIssueVO, idx: number) {
  try {
    await ElMessageBox.confirm('确定拒绝该建议吗？', '拒绝确认', {
      confirmButtonText: '确定拒绝',
      cancelButtonText: '取消',
      type: 'warning',
    })
    if (issues.value[idx]) {
      issues.value[idx].handleStatus = 2
    }
    ElMessage.info('已拒绝建议')
  } catch {
    // 用户取消
  }
}

function handleViewOriginal(issue: DetectionIssueVO) {
  currentOriginal.value = issue
  originalVisible.value = true
}

function handleFinish() {
  ElMessage.success('检测完成')
  router.push('/requirement')
}

// ---- 辅助函数 ----
function getTypeLabel(type: DetectionType): string {
  return DETECT_TYPE_CONFIG[type]?.label || type
}

function getIssueClass(issue: DetectionIssueVO): string {
  if (issue.handleStatus !== 0) return 'is-handled'
  if (issue.severity === 'HIGH') return 'is-danger'
  if (issue.severity === 'MEDIUM') return 'is-warning'
  return 'is-info'
}
</script>

<style scoped>
.requirement-detect {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--app-bg-secondary);
  transition: var(--app-transition-base);
}

/* ---- 顶部工具栏 ---- */
.detect-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: var(--app-bg-elevated);
  border-bottom: 1px solid var(--app-border-medium);
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-primary);
}

/* ---- 主内容区 ---- */
.detect-body {
  flex: 1;
  overflow: auto;
  padding: 20px;
  max-width: 960px;
  margin: 0 auto;
  width: 100%;
}

/* ---- 加载中 ---- */
.detect-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: var(--app-text-tertiary);
  gap: 12px;
}

/* ---- 检测卡片 ---- */
.detect-card {
  background: var(--app-bg-elevated);
  border: 1px solid var(--app-border-medium);
  border-radius: var(--app-radius-sm);
  padding: 20px;
  margin-bottom: 16px;
  transition: var(--app-transition-base);
}

.detect-card .card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.detect-card .card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-primary);
}

.progress-text {
  font-size: 12px;
  color: var(--app-text-secondary);
  margin-top: 4px;
}

/* ---- 汇总卡片 ---- */
.summary-items {
  display: flex;
  gap: 24px;
}

.summary-item {
  flex: 1;
  text-align: center;
  padding: 16px;
  border-radius: var(--app-radius-sm);
}

.summary-item.has-issues {
  background: var(--app-color-warning-light, rgba(230, 162, 60, 0.1));
  border: 1px solid rgba(230, 162, 60, 0.2);
}

.summary-item.no-issues {
  background: var(--app-color-success-light, rgba(103, 194, 58, 0.1));
  border: 1px solid rgba(103, 194, 58, 0.2);
}

.summary-number {
  font-size: 24px;
  font-weight: 700;
}

.has-issues .summary-number {
  color: var(--app-color-warning);
}

.no-issues .summary-number {
  color: var(--app-color-success);
}

.summary-label {
  font-size: 13px;
  color: var(--app-text-secondary);
  margin-top: 4px;
}

/* ---- 详情列表 ---- */
.detail-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-item {
  border: 1px solid var(--app-border-light);
  border-radius: 6px;
  padding: 16px;
  transition: var(--app-transition-base);
}

.detail-item:hover {
  box-shadow: var(--app-shadow-sm);
}

.detail-item.is-danger {
  border-left: 4px solid var(--app-color-danger);
  background: rgba(239, 68, 68, 0.05);
}

.detail-item.is-warning {
  border-left: 4px solid var(--app-color-warning);
  background: rgba(245, 158, 11, 0.05);
}

.detail-item.is-info {
  border-left: 4px solid var(--app-brand-color);
}

.detail-item.is-handled {
  opacity: 0.6;
}

.detail-item .item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.detail-item .item-title {
  font-weight: 600;
  color: var(--app-text-primary);
}

.detail-item .item-body {
  margin-bottom: 12px;
  line-height: 1.6;
  font-size: 14px;
  color: var(--app-text-secondary);
}

.detail-item .item-content {
  margin: 0 0 8px 0;
}

.detail-item .item-suggestion {
  background: var(--app-bg-secondary);
  border-left: 3px solid var(--app-brand-color);
  border-radius: 4px;
  padding: 8px 12px;
  margin: 0;
}

.detail-item .item-suggestion .label {
  color: var(--app-text-tertiary);
  font-size: 13px;
}

.suggestion-text {
  color: var(--app-color-success);
  font-weight: 500;
}

.detail-item .item-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* ---- 无问题/检测中 ---- */
.no-issues-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px;
  color: var(--app-color-success);
}

.detecting-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 0;
  color: var(--app-text-tertiary);
}

/* ---- 结论卡片 ---- */
.conclusion-body {
  line-height: 1.8;
  font-size: 14px;
  color: var(--app-text-secondary);
}

.conclusion-body p {
  margin: 0 0 8px 0;
}

.conclusion-line strong {
  color: var(--app-text-primary);
}

.conclusion-warning {
  color: var(--app-color-warning);
  font-weight: 500;
}

.conclusion-success {
  color: var(--app-color-success);
  font-weight: 500;
}

.conclusion-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
  padding: 16px 20px;
  background: var(--app-bg-tertiary, var(--app-bg-secondary));
  border-radius: 0 0 var(--app-radius-sm, 8px) var(--app-radius-sm, 8px);
  margin: 16px -20px -20px;
}

/* ---- 查看原文弹窗 ---- */
.original-content {
  padding: 8px 0;
}

.original-text {
  font-size: 14px;
  line-height: 1.8;
  color: var(--app-text-primary);
  background: var(--app-bg-secondary);
  padding: 12px 16px;
  border-radius: var(--app-radius-sm);
}
</style>
