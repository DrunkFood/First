<template>
  <div class="phase-detection">
    <!-- 提交检测 -->
    <div v-if="!submitted && !readonly" class="detection-submit">
      <!-- 检测类型选择 -->
      <div class="section-title">选择检测类型</div>
      <el-checkbox-group v-model="selectedDetectionTypes" class="detection-type-group">
        <el-checkbox value="SENSITIVE_WORD">敏感词检测</el-checkbox>
        <el-checkbox value="TYPO">错别字检测</el-checkbox>
        <el-checkbox value="FAIRNESS">公平竞争检测</el-checkbox>
        <el-checkbox value="COMPLIANCE">合规性检测</el-checkbox>
      </el-checkbox-group>

      <!-- 政策文件选择 -->
      <div class="section-title">选择政策文件</div>
      <PolicyFileSelect
        v-model="selectedPolicyFileIds"
        :applicable-category="projectCategory"
      />

      <div class="submit-actions">
        <el-button
          type="primary"
          :loading="isSubmitting || hasActiveDetection"
          :disabled="!selectedDetectionTypes.length || hasActiveDetection"
          @click="handleSubmit"
        >
          提交检测
        </el-button>
      </div>
    </div>

    <!-- 检测进度 -->
    <DetectionProgress v-if="submitted" :project-id="projectId" ref="progressRef" @completed="handleDetectionCompleted" />

    <!-- 检测报告 -->
    <DetectionReport
      v-if="showReport"
      :project-id="projectId"
      @accept="handleAcceptIssue"
      @reject="handleRejectIssue"
      @accept-all="handleAcceptAll"
    />

    <div class="phase-actions">
      <el-button @click="$emit('prev')">上一步</el-button>
      <div style="flex: 1" />
      <el-button v-if="showReport" @click="$emit('prev')">返回修改</el-button>
      <el-button v-if="!readonly" type="success" :disabled="!canFinish" @click="$emit('finish')">
        完成编制
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { detectionApi } from '@/api/detection'
import { projectApi } from '@/api/project'
import PolicyFileSelect from '@/components/detection/PolicyFileSelect.vue'
import DetectionProgress from '@/components/detection/DetectionProgress.vue'
import DetectionReport from '@/components/detection/DetectionReport.vue'
import type { DetectionType } from '@/types/detection'

const props = defineProps<{ projectId: number; readonly?: boolean }>()
defineEmits<{ prev: []; finish: [] }>()

const submitted = ref(false)
const isSubmitting = ref(false)
const showReport = ref(false)
const hasActiveDetection = ref(false)
const selectedPolicyFileIds = ref<number[]>([])
const selectedDetectionTypes = ref<DetectionType[]>([
  'FAIRNESS',
  'COMPLIANCE',
  'TYPO',
  'SENSITIVE_WORD',
])
const projectCategory = ref('')

const canFinish = computed(() => submitted.value)

const loadProject = async () => {
  const project = await projectApi.getById(props.projectId)
  projectCategory.value = project.projectCategory || ''
  if (['DETECTING', 'DETECTION_PASSED', 'DETECTION_FAILED', 'DETECTION_SKIPPED'].includes(project.status)) {
    submitted.value = true
    if (['DETECTION_PASSED', 'DETECTION_FAILED'].includes(project.status)) {
      showReport.value = true
    }
  }
  // 项目状态 DETECTING = 有活跃检测任务
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
}

const handleRejectIssue = async (recordId: number, issueIndex: number) => {
  await detectionApi.reject(recordId, issueIndex)
  ElMessage.success('已拒绝建议')
}

const handleAcceptAll = async () => {
  await detectionApi.acceptAll(props.projectId)
  ElMessage.success('已接受所有建议')
}

const handleDetectionCompleted = async (_status: string) => {
  // 检测完成后，延迟等待后端同步项目状态，再刷新
  setTimeout(async () => {
    await loadProject()
  }, 1500)
}

onMounted(loadProject)
</script>

<style scoped lang="scss">
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 20px 0 12px;
  padding-left: 10px;
  border-left: 3px solid var(--app-brand-color);

  &:first-child {
    margin-top: 0;
  }
}

.detection-submit {
  margin-bottom: 20px;
}

.detection-type-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.submit-actions {
  margin-top: 16px;
}

.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
