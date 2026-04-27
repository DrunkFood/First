<template>
  <div class="phase-detection">
    <div class="form-container">
      <!-- 表单头部 -->
      <div class="form-header">
        <h3 class="form-title">
          {{ showReport ? '检测报告' : '智能检测' }}
          <span v-if="showReport && issueCount > 0" class="issue-badge" :class="{ resolved: issueCount === 0 }">
            {{ issueCount }}个问题
          </span>
        </h3>
      </div>

      <!-- 表单内容 -->
      <div class="form-section">
        <!-- 状态1：提交检测 -->
        <template v-if="!submitted && !readonly">
          <div class="section-title">选择检测类型</div>
          <el-checkbox-group v-model="selectedDetectionTypes" class="detection-type-group">
            <el-checkbox value="SENSITIVE_WORD">敏感词检测</el-checkbox>
            <el-checkbox value="TYPO">错别字检测</el-checkbox>
            <el-checkbox value="POLICY_REVIEW">政策文件审查</el-checkbox>
            <el-checkbox value="FORMAT_CHECK">格式规范检测</el-checkbox>
          </el-checkbox-group>

          <div class="section-title">选择政策文件</div>
          <PolicyFileSelect
            v-model="selectedPolicyFileIds"
            :applicable-category="projectCategory"
          />
        </template>

        <!-- 状态2：检测进度 -->
        <DetectionProgress
          v-if="submitted && !showReport"
          :project-id="projectId"
          @completed="handleDetectionCompleted"
        />

        <!-- 状态3：检测报告 -->
        <DetectionReport
          v-if="showReport"
          ref="reportRef"
          :project-id="projectId"
          :readonly="readonly"
          @accept="handleAcceptIssue"
          @reject="handleRejectIssue"
          @accept-all="handleAcceptAll"
          @loaded="handleReportLoaded"
        />
      </div>

      <!-- 底部操作栏 -->
      <div class="form-actions">
        <div class="form-actions-left">
          <el-button @click="$emit('prev')">
            <el-icon><ArrowLeft /></el-icon>
            上一步
          </el-button>
        </div>
        <div class="form-actions-right">
          <!-- 未提交状态 -->
          <el-button
            v-if="!submitted && !readonly"
            type="primary"
            :loading="isSubmitting || hasActiveDetection"
            :disabled="!selectedDetectionTypes.length || hasActiveDetection"
            @click="handleSubmit"
          >
            提交检测
          </el-button>

          <!-- 报告状态（非只读） -->
          <template v-if="showReport && !readonly">
            <el-button type="warning" :disabled="issueCount === 0" @click="handleAcceptAll">
              <el-icon><CircleCheck /></el-icon>
              一键接受全部
            </el-button>
            <el-button :loading="isRetrying" @click="handleRetry">
              <el-icon><RefreshRight /></el-icon>
              重新检测
            </el-button>
            <el-button type="success" :disabled="!canFinish" :loading="finishing" @click="$emit('finish')">
              完成编制
            </el-button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, CircleCheck, RefreshRight } from '@element-plus/icons-vue'
import { detectionApi } from '@/api/detection'
import { projectApi } from '@/api/project'
import PolicyFileSelect from '@/components/detection/PolicyFileSelect.vue'
import DetectionProgress from '@/components/detection/DetectionProgress.vue'
import DetectionReport from '@/components/detection/DetectionReport.vue'
import type { DetectionType } from '@/types/detection'

const props = defineProps<{ projectId: number; readonly?: boolean; finishing?: boolean }>()
defineEmits<{ prev: []; finish: [] }>()

const submitted = ref(false)
const isSubmitting = ref(false)
const isRetrying = ref(false)
const showReport = ref(false)
const hasActiveDetection = ref(false)
const projectStatus = ref('')
const issueCount = ref(0)
const selectedPolicyFileIds = ref<number[]>([])
const selectedDetectionTypes = ref<DetectionType[]>([
  'POLICY_REVIEW',
  'FORMAT_CHECK',
  'TYPO',
  'SENSITIVE_WORD',
])
const projectCategory = ref('')
const reportRef = ref<InstanceType<typeof DetectionReport> | null>(null)

const canFinish = computed(() =>
  projectStatus.value === 'DETECTION_PASSED' || projectStatus.value === 'DETECTION_SKIPPED'
)

const loadProject = async () => {
  const project = await projectApi.getById(props.projectId)
  projectCategory.value = project.projectCategory || ''
  projectStatus.value = project.status
  if (['DETECTING', 'DETECTION_PASSED', 'DETECTION_FAILED', 'DETECTION_SKIPPED', 'PUBLISHED', 'ARCHIVED'].includes(project.status)) {
    submitted.value = true
    if (['DETECTION_PASSED', 'DETECTION_FAILED', 'PUBLISHED', 'ARCHIVED'].includes(project.status)) {
      showReport.value = true
    }
  }
  hasActiveDetection.value = project.status === 'DETECTING'
}

const handleSubmit = async () => {
  if (!selectedDetectionTypes.value.length) {
    ElMessage.warning('请至少选择一种检测类型')
    return
  }
  isSubmitting.value = true
  try {
    await detectionApi.submit(props.projectId, {
      policyFileIds: selectedPolicyFileIds.value,
    })
    submitted.value = true
    hasActiveDetection.value = true
    ElMessage.success('检测已提交')
  } catch (e: any) {
    if (e?.code === 8084) {
      ElMessage.warning('检测任务正在处理中，请稍候')
      hasActiveDetection.value = true
    } else {
      ElMessage.error('提交检测失败')
    }
  } finally {
    isSubmitting.value = false
  }
}

const handleAcceptIssue = async (recordId: number, issueIndex: number) => {
  await detectionApi.accept(recordId, issueIndex)
  ElMessage.success('已接受建议')
  await Promise.all([loadProject(), reportRef.value?.refresh()])
}

const handleRejectIssue = async (recordId: number, issueIndex: number) => {
  await detectionApi.reject(recordId, issueIndex)
  ElMessage.success('已拒绝建议')
  await Promise.all([loadProject(), reportRef.value?.refresh()])
}

const handleAcceptAll = async () => {
  try {
    await ElMessageBox.confirm('将接受所有修改建议，是否继续？', '一键接受', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await detectionApi.acceptAll(props.projectId)
    ElMessage.success('已接受所有建议')
    await Promise.all([loadProject(), reportRef.value?.refresh()])
  } catch { /* cancelled */ }
}

const handleRetry = async () => {
  try {
    await ElMessageBox.confirm('重新检测将覆盖当前结果，是否继续？', '重新检测', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
    isRetrying.value = true
    await detectionApi.retry(props.projectId)
    showReport.value = false
    submitted.value = true
    hasActiveDetection.value = true
    ElMessage.success('已重新提交检测')
    await loadProject()
  } catch { /* cancelled or error */ }
  finally {
    isRetrying.value = false
  }
}

const handleDetectionCompleted = async (_status: string) => {
  setTimeout(async () => {
    await loadProject()
  }, 1500)
}

const handleReportLoaded = (data: { totalIssueCount: number; unresolvedCount: number }) => {
  issueCount.value = data.unresolvedCount
}

onMounted(loadProject)
</script>

<style scoped lang="scss">
.phase-detection {
  display: flex;
  flex-direction: column;
}

.form-container {
  background: var(--app-bg-secondary);
  border-radius: 8px;
  border: 1px solid var(--app-border-light);
  overflow: hidden;
}

.form-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--app-border-light);
  background: var(--app-bg-tertiary);
}

.form-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.issue-badge {
  display: inline-block;
  background: var(--app-color-warning);
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;

  &.resolved { background: var(--app-color-success); }
}

.form-section {
  padding: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 20px 0 12px;
  padding-left: 10px;
  border-left: 3px solid var(--app-brand-color);

  &:first-child { margin-top: 0; }
}

.detection-type-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.form-actions {
  padding: 20px 24px;
  background: var(--app-bg-tertiary);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.form-actions-left,
.form-actions-right {
  display: flex;
  gap: 12px;
}
</style>
