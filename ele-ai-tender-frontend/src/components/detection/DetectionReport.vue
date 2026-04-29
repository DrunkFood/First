<template>
  <div class="detection-report">
    <!-- 摘要卡片 -->
    <div v-if="report" class="review-summary">
      <div
        v-for="item in summaryItems"
        :key="item.type"
        class="summary-card"
        :class="{ success: item.issueCount === 0, warning: item.issueCount > 0 }"
      >
        <div class="summary-card-header">
          <div class="summary-card-title">{{ item.name }}</div>
          <div class="summary-card-icon">
            <el-icon v-if="item.issueCount === 0" :size="20"><CircleCheck /></el-icon>
            <el-icon v-else :size="20"><WarningFilled /></el-icon>
          </div>
        </div>
        <div class="summary-card-content">
          {{ item.issueCount === 0 ? '通过检测，未发现问题' : '发现需要修改的内容' }}
        </div>
        <div class="summary-card-count">{{ item.issueCount }}个问题</div>
      </div>
    </div>

    <!-- 检测文件 -->
    <template v-if="report && report.detectionFiles?.length">
      <div class="section-title">检测文件</div>
      <div class="detection-files">
        <div class="detection-files-label">本次检测的文件：</div>
        <ul class="detection-files-list">
          <li v-for="file in report.detectionFiles" :key="file.fileId" class="detection-file-item">
            <el-icon :size="16" class="file-icon"><Document /></el-icon>
            <div class="file-info">
              <span class="file-name">{{ file.fileName || '未知文件' }}</span>
              <span class="file-type">{{ file.fileType }}</span>
            </div>
          </li>
        </ul>
      </div>
    </template>

    <!-- 问题列表 -->
    <template v-if="report && groupedIssues.length">
      <div class="section-title">问题列表</div>

      <div
        v-for="group in groupedIssues"
        :key="group.type"
        class="issue-group"
      >
        <div class="issue-group-header">
          <span class="issue-type-badge" :class="getTypeBadgeClass(group.type)">{{ group.name }}</span>
          <span class="issue-count">{{ group.issues.length }}个问题</span>
        </div>

        <div class="issue-list">
          <div
            v-for="issue in group.issues"
            :key="issue.recordId + '-' + issue.issueIndex"
            class="issue-card"
            :class="{
              accepted: issue.handleStatus === 1,
              rejected: issue.handleStatus === 2,
            }"
          >
            <div class="issue-header">
              <div class="issue-meta">
                <div class="issue-location">{{ issue.location }}</div>
                <div class="issue-title">{{ issue.description }}</div>
              </div>
              <span class="severity-badge" :class="issue.severity.toLowerCase()">
                {{ severityLabel(issue.severity) }}
              </span>
            </div>
            <div class="issue-body">
              <div v-if="issue.suggestion" class="issue-suggestion">
                <div class="issue-suggestion-label">修改建议：</div>
                {{ issue.suggestion }}
              </div>
            </div>
            <div v-if="issue.handleStatus === 0 && !readonly" class="issue-actions">
              <el-button size="small" type="success" @click="$emit('accept', issue.recordId, issue.issueIndex)">
                <el-icon :size="12"><CircleCheck /></el-icon>
                接受建议
              </el-button>
              <el-button size="small" type="danger" plain @click="$emit('reject', issue.recordId, issue.issueIndex)">
                <el-icon :size="12"><Close /></el-icon>
                拒绝建议
              </el-button>
              <el-button size="small" @click="handleViewOriginal(issue)">
                <el-icon :size="12"><View /></el-icon>
                查看原文
              </el-button>
              <el-button
                v-if="issue.locationRef"
                type="warning"
                size="small"
                @click="$emit('locate', issue)"
              >
                定位到文档
              </el-button>
            </div>
            <div v-else class="issue-status">
              <el-tag
                :type="issue.handleStatus === 1 ? 'success' : issue.handleStatus === 2 ? 'info' : issue.handleStatus === 3 ? 'warning' : 'warning'"
                size="small"
              >
                {{ issue.handleStatus === 1 ? '已接受' : issue.handleStatus === 2 ? '已拒绝' : issue.handleStatus === 3 ? '未找到原文' : '待处理' }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </template>

    <el-empty v-if="!report" description="暂无检测报告" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { CircleCheck, WarningFilled, Close, View, Document } from '@element-plus/icons-vue'
import { detectionApi } from '@/api/detection'
import type { DetectionReportVO, DetectionIssueVO } from '@/types/detection'

const props = defineProps<{ projectId: number; readonly?: boolean }>()
const emit = defineEmits<{
  accept: [recordId: number, issueIndex: number]
  reject: [recordId: number, issueIndex: number]
  'accept-all': []
  loaded: [data: { totalIssueCount: number; unresolvedCount: number }]
  locate: [issue: DetectionIssueVO]
}>()

const report = ref<DetectionReportVO | null>(null)

const TYPE_MAP: Record<string, string> = {
  SENSITIVE_WORD: '敏感词检测',
  TYPO: '错别字检测',
  POLICY_REVIEW: '政策文件审查',
  FORMAT_CHECK: '格式规范检测',
}
const ALL_TYPES = Object.keys(TYPE_MAP)

const summaryItems = computed(() => {
  if (!report.value) return []
  const countMap: Record<string, number> = {}
  for (const type of ALL_TYPES) {
    countMap[type] = 0
  }
  for (const issue of report.value.issues) {
    if (issue.handleStatus !== 1) {
      countMap[issue.detectionType] = (countMap[issue.detectionType] || 0) + 1
    }
  }
  return ALL_TYPES.map(type => ({
    type,
    name: TYPE_MAP[type],
    issueCount: countMap[type] ?? 0,
  }))
})

const unresolvedCount = computed(() => {
  if (!report.value) return 0
  return report.value.issues.filter(i => i.handleStatus === 0).length
})

const groupedIssues = computed(() => {
  if (!report.value) return []
  const groups: Record<string, { name: string; issues: DetectionIssueVO[] }> = {}
  for (const issue of report.value.issues) {
    const type = issue.detectionType
    if (!groups[type]) {
      groups[type] = { name: TYPE_MAP[type] || type, issues: [] }
    }
    groups[type].issues.push(issue)
  }
  return Object.entries(groups).map(([type, data]) => ({ type, ...data }))
})

const severityLabel = (severity: string) => {
  const map: Record<string, string> = { HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[severity] || severity
}

const getTypeBadgeClass = (type: string) => {
  const map: Record<string, string> = {
    POLICY_REVIEW: 'compliance',
    FORMAT_CHECK: 'fair',
    TYPO: 'typo',
    SENSITIVE_WORD: 'sensitive',
  }
  return map[type] || 'fair'
}

const handleViewOriginal = (issue: DetectionIssueVO) => {
  const lines: string[] = []
  if (issue.location) lines.push(`<b>位置：</b>${issue.location}`)
  if (issue.original) lines.push(`<b>原文：</b>${issue.original}`)
  if (issue.targeted) lines.push(`<b>建议替换为：</b>${issue.targeted}`)
  ElMessageBox.alert(lines.join('<br/><br/>') || '暂无原文信息', '查看原文', {
    dangerouslyUseHTMLString: true,
    confirmButtonText: '关闭',
  })
}

const refresh = async () => {
  report.value = await detectionApi.getReport(props.projectId)
  emit('loaded', {
    totalIssueCount: report.value?.totalIssueCount ?? 0,
    unresolvedCount: unresolvedCount.value,
  })
}

defineExpose({ refresh, unresolvedCount })

onMounted(refresh)
</script>

<style scoped lang="scss">
.detection-report {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.review-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.summary-card {
  background: var(--app-bg-tertiary);
  border-radius: 8px;
  padding: 20px;
  border: 1px solid var(--app-border-light);
  transition: all 0.2s;

  &:hover { border-color: var(--app-brand-color); }

  &.success { border-left: 3px solid var(--app-color-success); }
  &.warning { border-left: 3px solid var(--app-color-warning); }
}

.summary-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.summary-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text-primary);
}

.summary-card-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;

  .success & {
    background: rgba(16, 185, 129, 0.15);
    color: var(--app-color-success);
  }

  .warning & {
    background: rgba(245, 158, 11, 0.15);
    color: var(--app-color-warning);
  }
}

.summary-card-content {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.summary-card-count {
  font-size: 24px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin-top: 8px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--app-brand-color);
}

.issue-group {
  margin-top: -20px;
  margin-bottom: 24px;
}

.issue-group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 12px;
  background: var(--app-bg-tertiary);
  border-radius: 6px;
}

.issue-type-badge {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;

  &.fair {
    background: rgba(51, 108, 255, 0.15);
    color: var(--app-brand-color);
  }

  &.compliance {
    background: rgba(245, 158, 11, 0.15);
    color: var(--app-color-warning);
  }

  &.typo {
    background: rgba(16, 185, 129, 0.15);
    color: var(--app-color-success);
  }

  &.sensitive {
    background: rgba(239, 68, 68, 0.15);
    color: var(--app-color-danger);
  }
}

.issue-count {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.issue-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.issue-card {
  background: var(--app-bg-tertiary);
  border-radius: 8px;
  padding: 16px;
  border: 1px solid var(--app-border-light);
  transition: all 0.2s;

  &:hover { border-color: var(--app-brand-color); }

  &.accepted {
    border-left: 3px solid var(--app-color-success);
    opacity: 0.7;
  }

  &.rejected {
    border-left: 3px solid var(--app-color-danger);
    opacity: 0.7;
  }
}

.issue-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.issue-meta { flex: 1; }

.issue-location {
  font-size: 12px;
  color: var(--app-text-tertiary);
  margin-bottom: 4px;
}

.issue-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text-primary);
}

.severity-badge {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
  margin-left: 12px;

  &.high {
    background: rgba(239, 68, 68, 0.15);
    color: var(--app-color-danger);
  }

  &.medium {
    background: rgba(245, 158, 11, 0.15);
    color: var(--app-color-warning);
  }

  &.low {
    background: rgba(16, 185, 129, 0.15);
    color: var(--app-color-success);
  }
}

.issue-body { margin-bottom: 12px; }

.issue-suggestion {
  background: var(--app-bg-elevated, var(--app-bg-secondary));
  padding: 12px;
  border-radius: 6px;
  font-size: 13px;
  color: var(--app-text-primary);
  border-left: 3px solid var(--app-brand-color);
}

.issue-suggestion-label {
  font-size: 11px;
  color: var(--app-text-tertiary);
  margin-bottom: 4px;
  font-weight: 600;
}

.issue-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.issue-status {
  display: flex;
}

.detection-files {
  border: 1px solid var(--app-border-light);
  border-radius: 6px;
  padding: 16px;
  margin-top: -8px;
}

.detection-files-label {
  font-weight: 500;
  margin-bottom: 12px;
  color: var(--app-text-primary);
}

.detection-files-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.detection-file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--app-border-light);

  &:last-child { border-bottom: none; }
}

.file-icon {
  color: var(--app-brand-color);
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.file-name {
  font-size: 13px;
  color: var(--app-text-primary);
}

.file-type {
  font-size: 12px;
  color: var(--app-text-tertiary);
}
</style>
