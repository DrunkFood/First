<template>
  <div class="detection-report">
    <!-- 摘要卡片 -->
    <div v-if="report" class="report-summary">
      <div
        v-for="item in summaryItems"
        :key="item.type"
        class="summary-card"
        :class="{ success: item.issueCount === 0, warning: item.issueCount > 0 }"
      >
        <span class="summary-label">{{ item.name }}</span>
        <el-tag :type="item.issueCount === 0 ? 'success' : 'warning'" size="small">
          {{ item.issueCount === 0 ? '通过' : `${item.issueCount}个问题` }}
        </el-tag>
      </div>
    </div>

    <!-- 问题列表 -->
    <div v-if="report && report.issues.length" class="issue-list">
      <div class="issue-list-header">
        <h4>问题列表</h4>
        <el-tag type="danger" size="small">{{ unresolvedCount }}个问题</el-tag>
      </div>

      <!-- 按检测类型分组 -->
      <div
        v-for="group in groupedIssues"
        :key="group.type"
        class="issue-group"
      >
        <div class="group-header">
          <span>{{ group.name }}</span>
          <el-tag size="small">{{ group.issues.length }}个问题</el-tag>
        </div>

        <div
          v-for="issue in group.issues"
          :key="issue.recordId"
          class="issue-card"
          :class="{
            accepted: issue.handleStatus === 1,
            rejected: issue.handleStatus === 2,
          }"
        >
          <div class="issue-header">
            <span class="issue-title">{{ issue.description }}</span>
            <el-tag
              :type="issue.severity === 'HIGH' ? 'danger' : issue.severity === 'MEDIUM' ? 'warning' : 'info'"
              size="small"
            >
              {{ severityLabel(issue.severity) }}
            </el-tag>
          </div>
          <div class="issue-location">位置：{{ issue.location }}</div>
          <div v-if="issue.suggestion" class="issue-suggestion">
            <span class="suggestion-label">建议：</span>{{ issue.suggestion }}
          </div>
          <div v-if="issue.handleStatus !== 1 && issue.handleStatus !== 2" class="issue-actions">
            <el-button size="small" type="primary" @click="$emit('accept', issue.recordId)">接受建议</el-button>
            <el-button size="small" @click="$emit('reject', issue.recordId)">拒绝建议</el-button>
          </div>
          <div v-else class="issue-status">
            <el-tag :type="issue.handleStatus === 1 ? 'success' : 'info'" size="small">
              {{ issue.handleStatus === 1 ? '已接受' : '已拒绝' }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!report" description="暂无检测报告" />

    <!-- 一键接受 -->
    <div v-if="report && unresolvedCount > 0" class="report-actions">
      <el-button type="warning" @click="$emit('accept-all')">
        一键接受全部
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { detectionApi } from '@/api/detection'
import type { DetectionReportVO, DetectionIssueVO } from '@/types/detection'

const props = defineProps<{ projectId: number }>()
defineEmits<{ accept: [recordId: number]; reject: [recordId: number]; 'accept-all': [] }>()

const report = ref<DetectionReportVO | null>(null)

const summaryItems = computed(() => {
  if (!report.value) return []
  const typeMap: Record<string, string> = {
    FAIRNESS: '公平竞争检测',
    COMPLIANCE: '合规性检查',
    TYPO: '错别字检查',
    SENSITIVE_WORD: '敏感词检测',
    POLICY_REVIEW: '合规性检测',
    FORMAT_CHECK: '格式规范检测',
  }
  const grouped: Record<string, { name: string; issueCount: number }> = {}
  for (const issue of report.value.issues) {
    const type = issue.detectionType
    if (!grouped[type]) {
      grouped[type] = { name: typeMap[type] || type, issueCount: 0 }
    }
    if (issue.handleStatus !== 1) {
      grouped[type].issueCount++
    }
  }
  return Object.entries(grouped).map(([type, data]) => ({ type, ...data }))
})

const unresolvedCount = computed(() => {
  if (!report.value) return 0
  return report.value.issues.filter(i => i.handleStatus !== 1 && i.handleStatus !== 2).length
})

const groupedIssues = computed(() => {
  if (!report.value) return []
  const typeMap: Record<string, string> = {
    FAIRNESS: '公平竞争检测',
    COMPLIANCE: '合规性检查',
    TYPO: '错别字检查',
    SENSITIVE_WORD: '敏感词检测',
    POLICY_REVIEW: '合规性检测',
    FORMAT_CHECK: '格式规范检测',
  }
  const groups: Record<string, { name: string; issues: DetectionIssueVO[] }> = {}
  for (const issue of report.value.issues) {
    const type = issue.detectionType
    if (!groups[type]) {
      groups[type] = { name: typeMap[type] || type, issues: [] }
    }
    groups[type].issues.push(issue)
  }
  return Object.entries(groups).map(([type, data]) => ({ type, ...data }))
})

const severityLabel = (severity: string) => {
  const map: Record<string, string> = { HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[severity] || severity
}

onMounted(async () => {
  report.value = await detectionApi.getReport(props.projectId)
})
</script>

<style scoped lang="scss">
.detection-report {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

// 摘要卡片
.report-summary {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.summary-card {
  padding: 12px 16px;
  border-radius: 6px;
  background: var(--app-bg-secondary);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-left: 3px solid var(--app-border-light);

  &.success {
    border-left-color: var(--app-color-success);
  }

  &.warning {
    border-left-color: var(--app-color-warning);
  }
}

.summary-label {
  font-weight: 500;
  color: var(--app-text-primary);
}

// 问题列表
.issue-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  h4 { margin: 0; color: var(--app-text-primary); }
}

.issue-group {
  margin-top: 12px;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: var(--app-bg-secondary);
  border-radius: 6px 6px 0 0;
  font-weight: 500;
  color: var(--app-text-primary);
}

.issue-card {
  padding: 12px 16px;
  border: 1px solid var(--app-border-light);
  border-top: none;
  transition: var(--app-transition-base);

  &:last-child {
    border-radius: 0 0 6px 6px;
  }

  &.accepted {
    border-left: 3px solid var(--app-color-success);
    opacity: 0.7;
  }

  &.rejected {
    border-left: 3px solid var(--app-color-danger);
  }
}

.issue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.issue-title {
  font-weight: 500;
  color: var(--app-text-primary);
}

.issue-location {
  font-size: 12px;
  color: var(--app-text-tertiary);
  margin-bottom: 8px;
}

.issue-suggestion {
  padding: 8px 12px;
  background: var(--app-bg-secondary);
  border-left: 3px solid var(--app-brand-color);
  border-radius: 4px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.suggestion-label {
  color: var(--app-brand-color);
  font-weight: 500;
}

.issue-actions {
  display: flex;
  gap: 8px;
}

.issue-status {
  display: flex;
}

.report-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
}
</style>
