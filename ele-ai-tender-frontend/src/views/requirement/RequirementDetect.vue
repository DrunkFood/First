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
        <!-- 初始状态：需求已完成（只读） -->
        <div v-if="notStarted && isRequirementCompleted" class="detect-card start-card">
          <div class="start-icon">
            <el-icon :size="48" color="var(--app-color-success)"><CircleCheck /></el-icon>
          </div>
          <h3 class="start-title">业务需求智能检测</h3>
          <p class="start-desc">该需求已完成，暂无检测记录</p>
          <div class="start-actions">
            <el-button @click="router.push(`/requirement/generate/${requirementId}`)">返回需求</el-button>
          </div>
        </div>

        <!-- 初始状态：跳过检测 / 开始智能检测 -->
        <div v-else-if="notStarted" class="detect-card start-card">
          <div class="start-icon">
            <el-icon :size="48" color="var(--app-brand-color)"><CircleCheck /></el-icon>
          </div>
          <h3 class="start-title">业务需求智能检测</h3>
          <p class="start-desc">对需求内容进行敏感词检测和错别字检查，帮助发现潜在问题</p>
          <div class="start-actions">
            <el-button @click="handleSkip">跳过检测</el-button>
            <el-button type="primary" @click="startDetection">开始智能检测</el-button>
          </div>
        </div>

        <!-- 卡片1：检测进度 -->
        <div v-if="!notStarted" class="detect-card progress-card">
          <div class="card-header">
            <span class="card-title">检测进度</span>
            <el-button
              v-if="allCompleted && canReDetect"
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
            :status="allCompleted && !hasIncompleteDetection ? 'success' : hasIncompleteDetection ? 'warning' : ''"
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
                <p v-if="issue.targeted" class="item-suggestion">
                  <span class="label">替换建议：</span>
                  <span class="suggestion-text">{{ issue.targeted }}</span>
                </p>
                <p v-else-if="issue.suggestion" class="item-suggestion">
                  <span class="label">AI建议：</span>
                  <span class="suggestion-text">{{ issue.suggestion }}</span>
                </p>
              </div>
              <div class="item-actions">
                <template v-if="issue.handleStatus === 0 && !isRequirementCompleted">
                  <el-button size="small" type="success" @click="handleAccept(issue, idx)">
                    接受建议
                  </el-button>
                  <el-button size="small" type="danger" @click="handleReject(issue, idx)">
                    拒绝建议
                  </el-button>
                </template>
                <template v-else-if="issue.handleStatus === 0 && isRequirementCompleted">
                  <el-tag type="info" size="small">未处理</el-tag>
                </template>
                <el-tag v-else-if="issue.handleStatus === 1" type="success" size="small">已接受</el-tag>
                <el-tag v-else-if="issue.handleStatus === 2" type="info" size="small">已拒绝</el-tag>
                <el-tag v-else-if="issue.handleStatus === 3" type="warning" size="small">未找到</el-tag>
                <el-button size="small" @click="handleViewOriginal(issue)">
                  查看原文
                </el-button>
              </div>
            </div>
          </div>

          <div v-else-if="allCompleted && hasIncompleteDetection" class="no-issues-tip incomplete-tip">
            <el-icon :size="32" color="var(--app-color-warning)"><WarningFilled /></el-icon>
            <p>部分检测项未完成，结果可能不完整</p>
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
              <template v-if="card.failed && card.status === 'FAILED'">检测未完成，结果不可用。</template>
              <template v-else-if="card.issueCount > 0">发现 {{ card.issueCount }} 个问题，建议进行修改。</template>
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
            <el-button v-if="!isRequirementCompleted" @click="handleSkip">跳过检测</el-button>
            <el-button v-if="!isRequirementCompleted" type="primary" @click="handleFinish">
              完成检测
            </el-button>
          </div>
        </div>
      </template>
    </div>

    <!-- 查看原文弹窗 -->
    <el-dialog v-model="originalVisible" title="查看原文" width="600px">
      <div v-if="currentOriginal" class="original-content">
        <div v-if="currentOriginal.location" class="original-position">{{ currentOriginal.location }}</div>
        <div class="original-context" v-html="highlightedContent"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Loading, RefreshRight, CircleCheck, WarningFilled } from '@element-plus/icons-vue'
import { requirementApi } from '@/api/requirement'
import { useTaskPolling } from '@/composables/useTaskPolling'
import { getTaskProgress } from '@/types/ai-task'
import type { AiTaskVO, AiTaskStatus } from '@/types/ai-task'
import type { DetectionIssueVO, RequirementDetectionRecord } from '@/types/detection'

/**
 * 需求级检测只有2项：敏感词 + 错别字
 * 后端AiTaskType: DETECTION_SENSITIVE_WORD, DETECTION_TYPO
 * 后端DetectionType code: SENSITIVE_WORD, TYPO
 */
const DETECT_TYPE_CONFIG: Record<string, { label: string }> = {
  TYPO: { label: '错别字检查' },
  SENSITIVE_WORD: { label: '敏感词检测' },
}

interface DetectCard {
  type: string
  label: string
  recordId: number | null
  taskId: number | null
  status: AiTaskStatus | ''
  percentage: number
  issueCount: number
  completed: boolean
  failed: boolean
}

const router = useRouter()
const route = useRoute()

// ---- 基础数据 ----
const requirementId = ref(0)
const requirementName = ref('')
const requirementStatus = ref('')

// ---- 检测卡片（只有2项） ----
const detectCards = ref<DetectCard[]>([
  { type: 'TYPO', label: '错别字检查', recordId: null, taskId: null, status: '', percentage: 0, issueCount: 0, completed: false, failed: false },
  { type: 'SENSITIVE_WORD', label: '敏感词检测', recordId: null, taskId: null, status: '', percentage: 0, issueCount: 0, completed: false, failed: false },
])

// ---- 为每个卡片创建独立的轮询实例 ----
const cardTaskIds = detectCards.value.map(() => ref<number | null>(null))
const cardPollings = cardTaskIds.map(idRef => useTaskPolling(idRef))

// 同步轮询结果到卡片
cardPollings.forEach((polling, index) => {
  watch(polling.task, (task) => {
    if (task) {
      updateCardFromTask(index, task)
    }
  })
})

// ---- 检测问题 ----
const issues = ref<DetectionIssueVO[]>([])

// ---- 状态 ----
const submitting = ref(false)
const reDetecting = ref(false)
const originalVisible = ref(false)
const currentOriginal = ref<DetectionIssueVO | null>(null)

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

/** 是否有检测项标记为failed（即未完成/超时），用于区分"通过"和"未完成" */
const hasIncompleteDetection = computed(() =>
  detectCards.value.some(c => c.failed && c.status === 'FAILED')
)

/** 是否从未提交过检测（初始状态） */
const notStarted = computed(() =>
  !submitting.value && detectCards.value.every(c => !c.taskId && !c.completed && !c.failed)
)

/** 需求是否已完成（COMPLETED状态下隐藏重新检测） */
const isRequirementCompleted = computed(() => requirementStatus.value === 'COMPLETED')

/** 是否可以重新检测（需求未完成 + 所有任务终态） */
const canReDetect = computed(() => {
  if (isRequirementCompleted.value) return false
  return detectCards.value.every(c => !c.taskId || c.completed || c.failed)
})

/** 查看原文：展示original内容并高亮 */
const highlightedContent = computed(() => {
  if (!currentOriginal.value) return ''
  const original = currentOriginal.value.original
  if (original) {
    return '<mark class="highlight-issue">' + escapeHtml(original) + '</mark>'
  }
  return escapeHtml(currentOriginal.value.description || '无原文信息')
})

function escapeHtml(text: string) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

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
    requirementStatus.value = data.status || ''
  } catch {
    ElMessage.error('加载需求失败')
    router.back()
    return
  }

  await restoreDetectionState()
})

// ---- 恢复检测任务状态 ----
async function restoreDetectionState() {
  try {
    const records = await requirementApi.getDetectionRecords(requirementId.value)

    if (!records || records.length === 0) {
      // 从未提交过检测，显示初始按钮（跳过检测 / 开始智能检测）
      return
    }

    // 每种检测类型只取最新记录（按id倒序取第一条），防止重新检测后出现重复记录
    const latestByType = new Map<string, RequirementDetectionRecord>()
    for (const record of records) {
      const existing = latestByType.get(record.detectionType)
      if (!existing || record.id > existing.id) {
        latestByType.set(record.detectionType, record)
      }
    }

    const cards = detectCards.value
    for (const record of latestByType.values()) {
      const cardIndex = cards.findIndex(c => c.type === record.detectionType)
      if (cardIndex < 0) continue
      const card = cards[cardIndex]!

      card.recordId = record.id
      card.taskId = record.taskId

      const isTerminal = ['COMPLETED', 'FAILED', 'AI_UNAVAILABLE', 'SKIPPED'].includes(record.status)
      if (isTerminal) {
        card.completed = record.status === 'COMPLETED'
        card.failed = ['FAILED', 'AI_UNAVAILABLE'].includes(record.status)
        card.percentage = 100
        card.status = record.status as AiTaskStatus
        if (record.result) {
          parseTaskResult(card, record.result)
        }
      } else if (isRequirementCompleted.value) {
        // 需求已完成后，非终态记录视为检测未完成/超时，不再轮询
        card.failed = true
        card.percentage = 100
        card.status = 'FAILED'
      } else {
        // 还在进行中，设置轮询
        cardTaskIds[cardIndex]!.value = record.taskId
      }
    }
  } catch {
    // 查询失败，不做任何操作，显示初始按钮
  }
}

// ---- 更新卡片状态 ----
function updateCardFromTask(index: number, task: AiTaskVO) {
  const card = detectCards.value[index]
  if (!card) return
  card.status = task.status

  if (task.status === 'COMPLETED') {
    if (task.resultSynced === 1) {
      if (card.completed) return
      card.completed = true
      card.percentage = 100
      parseTaskResult(card, task.result)
    } else if (task.resultSynced === 2) {
      card.failed = true
      card.percentage = 0
    } else {
      card.percentage = 90
    }
  } else if (task.status === 'FAILED' || task.status === 'AI_UNAVAILABLE') {
    card.failed = true
    card.percentage = 100
  } else if (task.status === 'SKIPPED') {
    card.completed = true
    card.percentage = 100
  } else {
    card.percentage = getTaskProgress(task)
  }
}

// ---- 提交检测 ----
async function startDetection() {
  if (!canReDetect.value) {
    ElMessage.warning('检测任务正在处理中，请稍候')
    return
  }
  submitting.value = true
  try {
    await requirementApi.detect(requirementId.value)
    submitting.value = false

    // 重新加载检测记录以获取 recordId（accept/reject 接口依赖 recordId）
    await restoreDetectionState()
  } catch (e: any) {
    submitting.value = false
    if (e?.code === 8084) {
      ElMessage.warning('检测任务正在处理中，正在恢复进度...')
      await restoreDetectionState()
    } else {
      ElMessage.error('提交检测失败')
    }
  }
}

// ---- 重新检测 ----
async function handleReDetect() {
  if (!canReDetect.value) {
    ElMessage.warning('检测任务正在处理中，请稍候')
    return
  }
  reDetecting.value = true
  try {
    for (const card of detectCards.value) {
      card.recordId = null
      card.taskId = null
      card.status = ''
      card.percentage = 0
      card.issueCount = 0
      card.completed = false
      card.failed = false
    }
    for (const idRef of cardTaskIds) {
      idRef.value = null
    }
    issues.value = []

    await startDetection()
  } catch (e: any) {
    if (e?.code === 8084) {
      ElMessage.warning('检测任务正在处理中，请稍候')
    }
  } finally {
    reDetecting.value = false
  }
}

function parseTaskResult(card: DetectCard, resultJson?: string) {
  if (!resultJson) return
  try {
    const result = JSON.parse(resultJson)
    if (Array.isArray(result.issues)) {
      for (let i = 0; i < result.issues.length; i++) {
        const issue = result.issues[i]
        issues.value.push({
          recordId: issue.recordId || result.recordId || card.recordId || 0,
          detectionType: card.type as any,
          typeName: card.label,
          description: issue.reason || issue.description || '',
          location: issue.position || issue.location || '',
          original: issue.original || '',
          targeted: issue.targeted || '',
          suggestion: issue.suggestion || '',
          severity: issue.severity || 'MEDIUM',
          handleStatus: issue.handleStatus || 0,
          issueIndex: i,
        })
      }
      card.issueCount = result.issues.length
    }
  } catch {
    // JSON解析失败，忽略
  }
}

// ---- 接受建议（调用后端API + 自动修正内容） ----
async function handleAccept(issue: DetectionIssueVO, idx: number) {
  try {
    await requirementApi.acceptDetection(requirementId.value, issue.recordId, issue.issueIndex)
    const item = issues.value[idx]
    if (item) item.handleStatus = 1
    ElMessage.success('已接受建议，内容已自动修正')
  } catch {
    ElMessage.error('操作失败')
  }
}

// ---- 拒绝建议（调用后端API） ----
async function handleReject(issue: DetectionIssueVO, idx: number) {
  try {
    await ElMessageBox.confirm('确定拒绝该建议吗？', '拒绝确认', {
      confirmButtonText: '确定拒绝',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await requirementApi.rejectDetection(requirementId.value, issue.recordId, issue.issueIndex)
    const item = issues.value[idx]
    if (item) item.handleStatus = 2
    ElMessage.info('已拒绝建议')
  } catch {
    // 用户取消
  }
}

function handleViewOriginal(issue: DetectionIssueVO) {
  currentOriginal.value = issue
  originalVisible.value = true
}

async function handleFinish() {
  try {
    await requirementApi.finishDetection(requirementId.value)
    requirementStatus.value = 'COMPLETED'
    ElMessage.success('检测完成')
    router.push('/requirement')
  } catch {
    ElMessage.error('操作失败')
  }
}

/** 跳过检测 = 完成检测 */
function handleSkip() {
  ElMessageBox.confirm('确定跳过智能检测吗？跳过后将直接完成需求。', '跳过确认', {
    confirmButtonText: '确定跳过',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    handleFinish()
  }).catch(() => {})
}

// ---- 辅助函数 ----
function getTypeLabel(type: string): string {
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

/* ---- 初始状态卡片 ---- */
.start-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 20px;
  text-align: center;
}

.start-icon {
  margin-bottom: 16px;
}

.start-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 0 0 8px 0;
}

.start-desc {
  font-size: 14px;
  color: var(--app-text-secondary);
  margin: 0 0 24px 0;
  max-width: 400px;
}

.start-actions {
  display: flex;
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

.no-issues-tip.incomplete-tip {
  color: var(--app-color-warning);
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

.original-position {
  font-size: 13px;
  color: var(--app-text-secondary);
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--app-border-light);
}

.original-context {
  font-size: 14px;
  line-height: 1.8;
  color: var(--app-text-primary);
  background: var(--app-bg-secondary);
  padding: 12px 16px;
  border-radius: var(--app-radius-sm);
}

:deep(.highlight-issue) {
  background-color: #fef08a;
  color: #854d0e;
  padding: 2px 4px;
  border-radius: 2px;
  font-weight: 600;
}
</style>
