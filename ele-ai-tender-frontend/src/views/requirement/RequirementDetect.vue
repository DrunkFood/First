<template>
  <div class="requirement-detect">
    <!-- 顶部工具栏 -->
    <div class="detect-toolbar">
      <div class="toolbar-left">
        <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
        <el-divider direction="vertical" />
        <span class="toolbar-title">{{ requirementName || '需求智能检测' }}</span>
        <StatusBadge
          v-if="detectionStatus"
          :status="detectionStatus"
          :type-map="DETECTION_STATUS_MAP"
        />
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
        <!-- 检测类型卡片 -->
        <div class="detect-cards">
          <div
            v-for="card in detectCards"
            :key="card.type"
            class="detect-card"
            :class="{ 'is-completed': card.completed, 'is-failed': card.failed }"
          >
            <div class="card-header">
              <el-icon :size="20" :color="getCardIconColor(card)">
                <component :is="getCardIcon(card)" />
              </el-icon>
              <span class="card-title">{{ card.label }}</span>
              <el-tag
                v-if="card.status"
                :type="getTaskStatusType(card.status)"
                size="small"
              >
                {{ getTaskStatusLabel(card.status) }}
              </el-tag>
            </div>
            <el-progress
              :percentage="card.percentage"
              :status="getProgressStatus(card.status)"
              :stroke-width="8"
              class="card-progress"
            />
            <div class="card-footer">
              <span v-if="card.issueCount > 0" class="issue-count">
                发现 <strong>{{ card.issueCount }}</strong> 个问题
              </span>
              <span v-else-if="card.completed" class="no-issue">暂无问题</span>
              <span v-else class="waiting-text">检测中...</span>
            </div>
          </div>
        </div>

        <!-- 检测结果 -->
        <div class="detect-result">
          <div class="result-header">
            <h4>检测结果</h4>
            <el-tag v-if="issues.length > 0" type="danger" size="small">
              共 {{ issues.length }} 个问题
            </el-tag>
            <el-tag v-else-if="allCompleted && issues.length === 0" type="success" size="small">
              检测通过
            </el-tag>
          </div>

          <div v-if="issues.length > 0" class="result-list">
            <div
              v-for="(issue, idx) in issues"
              :key="idx"
              class="result-item"
            >
              <div class="item-header">
                <el-tag
                  :type="issue.detectionType === 'SENSITIVE_WORD' ? 'danger' : 'warning'"
                  size="small"
                >
                  {{ getTypeLabel(issue.detectionType) }}
                </el-tag>
                <el-tag
                  :type="issue.severity === 'HIGH' ? 'danger' : issue.severity === 'MEDIUM' ? 'warning' : 'info'"
                  size="small"
                  class="severity-tag"
                >
                  {{ getSeverityLabel(issue.severity) }}
                </el-tag>
              </div>
              <div class="item-body">
                <div class="item-location">
                  <span class="label">原文片段：</span>
                  <span class="location-text" v-html="highlightLocation(issue.location)"></span>
                </div>
                <div v-if="issue.description" class="item-desc">
                  <span class="label">问题描述：</span>
                  {{ issue.description }}
                </div>
                <div v-if="issue.suggestion" class="item-suggestion">
                  <span class="label">AI建议：</span>
                  <span class="suggestion-text">{{ issue.suggestion }}</span>
                </div>
              </div>
              <div class="item-actions">
                <el-tooltip content="功能开发中" placement="top">
                  <el-button size="small" type="primary" disabled>接受</el-button>
                </el-tooltip>
                <el-tooltip content="功能开发中" placement="top">
                  <el-button size="small" disabled>拒绝</el-button>
                </el-tooltip>
              </div>
            </div>
          </div>

          <el-empty
            v-else-if="allCompleted"
            description="检测通过，未发现问题"
            :image-size="80"
          />
          <div v-else class="result-waiting">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>正在检测中，请稍候...</span>
          </div>
        </div>

        <!-- 底部操作 -->
        <div class="detect-actions">
          <el-tooltip content="功能开发中" placement="top">
            <el-button type="primary" :disabled="issues.length === 0">
              批量接受
            </el-button>
          </el-tooltip>
          <el-button @click="router.back()">返回修改</el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Loading, Warning, DocumentChecked, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { requirementApi } from '@/api/requirement'
import { aiTaskApi } from '@/api/ai-task'
import StatusBadge from '@/components/common/StatusBadge.vue'
import type { AiTaskStatus } from '@/types/ai-task'
import type { DetectionType, DetectionIssueVO } from '@/types/detection'

const router = useRouter()
const route = useRoute()

// ---- 检测状态映射（复用项目检测状态） ----
const DETECTION_STATUS_MAP: Record<string, { label: string; type?: '' | 'success' | 'warning' | 'info' | 'danger'; pulse?: boolean }> = {
  DETECTING: { label: '检测中', type: '', pulse: true },
  DETECTION_PASSED: { label: '检测通过', type: 'success' },
  DETECTION_FAILED: { label: '检测未通过', type: 'danger' },
}

// ---- 检测类型配置 ----
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

const DETECT_TYPE_CONFIG: Record<string, { label: string }> = {
  SENSITIVE_WORD: { label: '敏感词检测' },
  TYPO: { label: '错别字检测' },
}

// ---- 基础数据 ----
const requirementId = ref(0)
const requirementName = ref('')
const detectionStatus = ref('')

// ---- 检测卡片 ----
const detectCards = ref<DetectCard[]>([
  {
    type: 'SENSITIVE_WORD',
    label: '敏感词检测',
    taskId: null,
    status: '',
    percentage: 0,
    issueCount: 0,
    completed: false,
    failed: false,
  },
  {
    type: 'TYPO',
    label: '错别字检测',
    taskId: null,
    status: '',
    percentage: 0,
    issueCount: 0,
    completed: false,
    failed: false,
  },
])

// ---- 检测问题 ----
const issues = ref<DetectionIssueVO[]>([])

// ---- 状态 ----
const submitting = ref(false)
let pollingTimer: ReturnType<typeof setInterval> | null = null

// ---- 计算属性 ----
const allCompleted = computed(() => detectCards.value.every(c => c.completed || c.failed))

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

    // taskMap: { SENSITIVE_WORD: taskId, TYPO: taskId }
    for (const card of detectCards.value) {
      const taskId = taskMap[card.type]
      if (taskId) {
        card.taskId = taskId
      } else {
        // 没有返回该类型的任务ID，标记为完成（无问题）
        card.completed = true
        card.percentage = 100
      }
    }

    detectionStatus.value = 'DETECTING'
    startPolling()
  } catch {
    submitting.value = false
    ElMessage.error('提交检测失败')
  }
}

// ---- 轮询逻辑 ----
function startPolling() {
  pollingTimer = setInterval(pollTaskStatus, 3000)
  // 立即执行一次
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
    onAllTasksFinished()
    return
  }

  for (const card of pendingCards) {
    try {
      const task = await aiTaskApi.getStatus(card.taskId!)
      card.status = task.status

      if (task.status === 'COMPLETED') {
        card.completed = true
        card.percentage = 100
        // 解析任务结果中的问题列表
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
    onAllTasksFinished()
  }
}

function parseTaskResult(card: DetectCard, resultJson?: string) {
  if (!resultJson) return
  try {
    const result = JSON.parse(resultJson)
    // 任务结果可能包含 issueCount 和/或 issues 数组
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
        })
      }
      card.issueCount = result.issues.length
    }
  } catch {
    // JSON解析失败，忽略
  }
}

function onAllTasksFinished() {
  stopPolling()
  const hasFailed = detectCards.value.some(c => c.failed)
  const hasIssues = issues.value.length > 0

  if (hasFailed) {
    detectionStatus.value = 'DETECTION_FAILED'
  } else if (hasIssues) {
    detectionStatus.value = 'DETECTION_FAILED'
  } else {
    detectionStatus.value = 'DETECTION_PASSED'
  }
}

// ---- 辅助函数 ----
function getTypeLabel(type: DetectionType): string {
  return DETECT_TYPE_CONFIG[type]?.label || type
}

function getSeverityLabel(severity: string): string {
  const map: Record<string, string> = { HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[severity] || severity
}

function getTaskStatusType(status: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    PENDING: 'info',
    PROCESSING: '',
    COMPLETED: 'success',
    FAILED: 'danger',
    AI_UNAVAILABLE: 'warning',
    SKIPPED: 'info',
  }
  return map[status] || 'info'
}

function getTaskStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '等待中',
    PROCESSING: '检测中',
    COMPLETED: '已完成',
    FAILED: '失败',
    AI_UNAVAILABLE: 'AI不可用',
    SKIPPED: '已跳过',
  }
  return map[status] || status
}

function getProgressStatus(status: string): '' | 'success' | 'warning' | 'exception' {
  if (status === 'COMPLETED' || status === 'SKIPPED') return 'success'
  if (status === 'FAILED') return 'exception'
  if (status === 'AI_UNAVAILABLE') return 'warning'
  return ''
}

function getCardIcon(card: DetectCard) {
  if (card.failed) return CircleClose
  if (card.completed) return CircleCheck
  return card.type === 'SENSITIVE_WORD' ? Warning : DocumentChecked
}

function getCardIconColor(card: DetectCard): string {
  if (card.failed) return '#F56C6C'
  if (card.completed) return '#67C23A'
  return '#409EFF'
}

/** 高亮原文片段中的问题词（简单的 **...** 标记转 <mark>） */
function highlightLocation(location: string): string {
  if (!location) return ''
  // 将 **xxx** 格式转为 <mark>xxx</mark>
  return location
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<mark>$1</mark>')
}
</script>

<style scoped>
.requirement-detect {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f7fa;
}

/* ---- 顶部工具栏 ---- */
.detect-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
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
  color: #303133;
}

/* ---- 主内容区 ---- */
.detect-body {
  flex: 1;
  overflow: auto;
  padding: 20px;
}

/* ---- 加载中 ---- */
.detect-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #909399;
  gap: 12px;
}

/* ---- 检测类型卡片 ---- */
.detect-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.detect-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  transition: border-color 0.3s;
}

.detect-card.is-completed {
  border-color: #67c23a;
}

.detect-card.is-failed {
  border-color: #f56c6c;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.card-progress {
  margin-bottom: 8px;
}

.card-footer {
  font-size: 13px;
  min-height: 20px;
}

.issue-count {
  color: #f56c6c;
}

.no-issue {
  color: #67c23a;
}

.waiting-text {
  color: #909399;
}

/* ---- 检测结果 ---- */
.detect-result {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 24px;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.result-header h4 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

/* ---- 问题列表 ---- */
.result-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 16px;
  transition: box-shadow 0.2s;
}

.result-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.severity-tag {
  margin-left: auto;
}

.item-body {
  margin-bottom: 12px;
  line-height: 1.6;
  font-size: 14px;
  color: #606266;
}

.item-body .label {
  color: #909399;
  font-size: 13px;
}

.item-location {
  margin-bottom: 6px;
}

.location-text {
  color: #303133;
}

.location-text :deep(mark) {
  background: #fde2e2;
  color: #f56c6c;
  padding: 1px 4px;
  border-radius: 2px;
  font-weight: 600;
}

.item-desc {
  margin-bottom: 6px;
}

.item-suggestion {
  background: #f0f9eb;
  border-radius: 4px;
  padding: 8px 12px;
}

.suggestion-text {
  color: #67c23a;
  font-weight: 500;
}

.item-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* ---- 等待检测结果 ---- */
.result-waiting {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 0;
  color: #909399;
}

/* ---- 底部操作 ---- */
.detect-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}
</style>
