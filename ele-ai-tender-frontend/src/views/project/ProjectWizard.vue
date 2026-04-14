<template>
  <div class="project-wizard">
    <el-page-header @back="$router.push('/project')" :content="project?.projectName || '项目编制'" />

    <el-steps :active="currentStep" finish-status="success" class="wizard-steps">
      <el-step title="基础信息" description="模板选择与项目信息" />
      <el-step title="招标需求" description="AI生成/手动编辑" />
      <el-step title="评审项设置" description="评审标准配置" />
      <el-step title="文档集成" description="预览与导出" />
      <el-step title="智能检测" description="合规检测" />
    </el-steps>

    <div class="wizard-content">
      <PhaseBasicInfo
        v-if="currentStep === 0"
        :project-id="projectId"
        @next="handleNext"
      />
      <PhaseRequirement
        v-else-if="currentStep === 1"
        :project-id="projectId"
        @next="handleNext"
        @prev="handlePrev"
      />
      <PhaseReviewItem
        v-else-if="currentStep === 2"
        :project-id="projectId"
        @next="handleNext"
        @prev="handlePrev"
      />
      <PhaseDocument
        v-else-if="currentStep === 3"
        :project-id="projectId"
        @next="handleNext"
        @prev="handlePrev"
      />
      <PhaseDetection
        v-else-if="currentStep === 4"
        :project-id="projectId"
        @prev="handlePrev"
        @finish="handleFinish"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { projectApi } from '@/api/project'
import type { ProjectInfo } from '@/types/project'
import PhaseBasicInfo from './phases/PhaseBasicInfo.vue'
import PhaseRequirement from './phases/PhaseRequirement.vue'
import PhaseReviewItem from './phases/PhaseReviewItem.vue'
import PhaseDocument from './phases/PhaseDocument.vue'
import PhaseDetection from './phases/PhaseDetection.vue'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const project = ref<ProjectInfo | null>(null)

const currentStep = computed(() => {
  if (!project.value?.currentPhase) return 0
  return Math.max(0, project.value.currentPhase - 1)
})

const loadProject = async () => {
  project.value = await projectApi.getById(projectId.value)
}

const handleNext = async () => {
  await loadProject()
}

const handlePrev = async () => {
  await loadProject()
}

const handleFinish = () => {
  router.push('/project')
}

onMounted(loadProject)
</script>

<style scoped>
.project-wizard {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
}

.wizard-steps {
  margin: 24px 0;
}

.wizard-content {
  min-height: 400px;
}
</style>
