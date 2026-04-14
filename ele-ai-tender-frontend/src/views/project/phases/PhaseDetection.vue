<template>
  <div class="phase-detection">
    <AiUnavailableAlert
      :visible="hasUnavailable"
      @retry="handleRetry"
      @skip="handleSkip"
    />

    <!-- 提交检测 -->
    <div v-if="!submitted" class="detection-submit">
      <PolicyFileSelect
        v-model="selectedPolicyFileIds"
        :applicable-category="projectCategory"
      />
      <el-button type="primary" :loading="isSubmitting" @click="handleSubmit">
        提交检测
      </el-button>
    </div>

    <!-- 检测进度 -->
    <DetectionProgress v-if="submitted" :project-id="projectId" ref="progressRef" />

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
      <el-button type="success" :disabled="!canFinish" @click="$emit('finish')">
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
import AiUnavailableAlert from '@/components/AiUnavailableAlert.vue'
import PolicyFileSelect from '@/components/detection/PolicyFileSelect.vue'
import DetectionProgress from '@/components/detection/DetectionProgress.vue'
import DetectionReport from '@/components/detection/DetectionReport.vue'

const props = defineProps<{ projectId: number }>()
defineEmits<{ prev: []; finish: [] }>()

const submitted = ref(false)
const isSubmitting = ref(false)
const showReport = ref(false)
const hasUnavailable = ref(false)
const selectedPolicyFileIds = ref<number[]>([])
const projectCategory = ref('')

const canFinish = computed(() => submitted.value)

const loadProject = async () => {
  const project = await projectApi.getById(props.projectId)
  projectCategory.value = project.projectCategory || ''
  // 检查是否已提交过检测
  if (['DETECTING', 'DETECTION_PASSED', 'DETECTION_FAILED', 'DETECTION_SKIPPED'].includes(project.status)) {
    submitted.value = true
    if (['DETECTION_PASSED', 'DETECTION_FAILED'].includes(project.status)) {
      showReport.value = true
    }
  }
}

const handleSubmit = async () => {
  isSubmitting.value = true
  try {
    await detectionApi.submit(props.projectId, { policyFileIds: selectedPolicyFileIds.value })
    submitted.value = true
    ElMessage.success('检测已提交')
  } catch {
    ElMessage.error('提交检测失败')
  } finally {
    isSubmitting.value = false
  }
}

const handleRetry = async () => {
  await detectionApi.retry(props.projectId)
  ElMessage.success('已重新提交检测')
}

const handleSkip = async () => {
  await detectionApi.skip(props.projectId)
  ElMessage.success('已跳过检测')
  submitted.value = true
}

const handleAcceptIssue = async (recordId: number) => {
  await detectionApi.accept(recordId)
  ElMessage.success('已接受建议')
}

const handleRejectIssue = async (recordId: number) => {
  await detectionApi.reject(recordId)
  ElMessage.success('已拒绝建议')
}

const handleAcceptAll = async () => {
  await detectionApi.acceptAll(props.projectId)
  ElMessage.success('已接受所有建议')
}

onMounted(loadProject)
</script>

<style scoped>
.detection-submit {
  margin-bottom: 20px;
}

.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
