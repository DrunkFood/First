<template>
  <div class="phase-review-item">
    <AiUnavailableAlert
      :visible="isAiUnavailable"
      @retry="handleRetry"
      @skip="handleSkipAi"
    />

    <div class="review-toolbar">
      <el-button type="primary" :loading="isGenerating" @click="handleGenerate">
        AI 生成评审项
      </el-button>
      <AiTaskStatus
        v-if="task"
        :task="task"
        :show-actions="true"
        @retry="handleRetry"
        @skip="handleSkipAi"
      />
      <el-button @click="handleAddRoot">手动添加根项</el-button>
    </div>

    <ReviewItemTree
      :items="reviewItems"
      :project-id="projectId"
      @refresh="loadReviewItems"
    />

    <div class="phase-actions">
      <el-button @click="$emit('prev')">上一步</el-button>
      <el-button type="primary" @click="handleNext">保存并继续</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { reviewApi } from '@/api/review'
import { useTaskPolling } from '@/composables/useTaskPolling'
import AiTaskStatus from '@/components/AiTaskStatus.vue'
import AiUnavailableAlert from '@/components/AiUnavailableAlert.vue'
import ReviewItemTree from '@/components/review/ReviewItemTree.vue'

const props = defineProps<{ projectId: number }>()
const emit = defineEmits<{ next: []; prev: [] }>()

const reviewItems = ref<any[]>([])
const taskId = ref<number | null>(null)
const isGenerating = ref(false)

const { task, retry: retryTask, skip: skipTask } = useTaskPolling(taskId)
const isAiUnavailable = computed(() => task.value?.status === 'AI_UNAVAILABLE')

const loadReviewItems = async () => {
  reviewItems.value = await reviewApi.getTree(props.projectId)
}

const handleGenerate = async () => {
  isGenerating.value = true
  try {
    const res = await reviewApi.generate(props.projectId, {})
    taskId.value = res.id
  } catch {
    ElMessage.error('提交AI生成失败')
  } finally {
    isGenerating.value = false
  }
}

const handleAddRoot = async () => {
  await reviewApi.create({
    projectId: props.projectId,
    itemName: '新评审项',
    level: 1,
    sortOrder: reviewItems.value.length,
  })
  await loadReviewItems()
}

const handleRetry = () => retryTask()
const handleSkipAi = () => skipTask()

const handleNext = () => {
  emit('next')
}

onMounted(loadReviewItems)
</script>

<style scoped>
.review-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
