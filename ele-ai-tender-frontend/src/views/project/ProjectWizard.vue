<template>
  <div class="project-wizard">
    <!-- 顶部标题栏 -->
    <div class="wizard-header">
      <el-page-header @back="$router.push('/project')" :content="project?.projectName || '项目编制'" />
    </div>

    <!-- 5步进度指示器 -->
    <el-steps :active="currentStep" finish-status="success" class="wizard-steps">
      <el-step title="基础信息" description="模板选择与项目信息" />
      <el-step title="详细需求" description="AI生成/手动编辑" />
      <el-step title="评审项设置" description="评审标准配置" />
      <el-step title="文档集成" description="预览与导出" />
      <el-step title="智能检测" description="合规检测" />
    </el-steps>

    <!-- 两栏布局：左侧步骤导航 + 右侧内容 -->
    <div class="wizard-body">
      <!-- 左侧步骤导航 -->
      <div class="step-nav">
        <div
          v-for="(step, index) in steps"
          :key="index"
          class="step-item"
          :class="{
            active: index === currentStep,
            completed: index < currentStep,
            disabled: index > currentStep,
          }"
          @click="handleStepClick(index)"
        >
          <div class="step-icon">
            <el-icon v-if="index < currentStep" color="var(--app-color-success)"><CircleCheck /></el-icon>
            <span v-else class="step-number">{{ index + 1 }}</span>
          </div>
          <div class="step-text">
            <span class="step-title">{{ step.title }}</span>
            <span class="step-desc">{{ step.desc }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧内容区 -->
      <div class="step-content">
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheck } from '@element-plus/icons-vue'
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

const currentStep = ref(0)

const steps = [
  { title: '基础信息录入', desc: '模板选择与项目信息' },
  { title: '详细需求生成', desc: 'AI生成/手动编辑' },
  { title: '评审项设置', desc: '评审标准配置' },
  { title: '文档集成', desc: '预览与导出' },
  { title: '智能检测', desc: '合规检测' },
]

const getStepFromPhase = (phase?: number) => {
  if (!phase) return 0
  return Math.max(0, phase - 1)
}

const initStepFromRoute = () => {
  const stepParam = route.query.step
  if (stepParam !== undefined) {
    const step = Number(stepParam)
    if (!isNaN(step) && step >= 0 && step <= 4) {
      currentStep.value = step
      return true
    }
  }
  return false
}

const loadProject = async () => {
  project.value = await projectApi.getById(projectId.value)
  if (!initStepFromRoute()) {
    currentStep.value = getStepFromPhase(project.value?.currentPhase)
  }
}

watch(() => route.query.step, (newStep) => {
  if (newStep !== undefined) {
    const step = Number(newStep)
    if (!isNaN(step) && step >= 0 && step <= 4) {
      currentStep.value = step
    }
  }
})

function handleStepClick(index: number) {
  // 只允许跳到已完成步骤或当前步骤
  if (index <= currentStep.value) {
    currentStep.value = index
  }
}

const handleNext = async () => {
  await loadProject()
  if (currentStep.value < 4) {
    currentStep.value++
  }
}

const handlePrev = async () => {
  await loadProject()
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const handleFinish = () => {
  router.push('/project')
}

onMounted(loadProject)
</script>

<style scoped lang="scss">
.project-wizard {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
}

.wizard-header {
  background: var(--app-bg-primary);
  border-radius: var(--app-radius-sm);
  padding: 12px 20px;
  border: 1px solid var(--app-border-light);
}

.wizard-steps {
  background: var(--app-bg-primary);
  border-radius: var(--app-radius-sm);
  padding: 20px 40px;
  border: 1px solid var(--app-border-light);
}

.wizard-body {
  display: flex;
  gap: 16px;
  min-height: 600px;
}

.step-nav {
  width: 240px;
  flex-shrink: 0;
  background: var(--app-bg-primary);
  border-radius: var(--app-radius-sm);
  padding: 16px;
  border: 1px solid var(--app-border-light);
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 6px;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: var(--app-transition-base);
  margin-bottom: 4px;

  &:hover:not(.disabled) {
    background: var(--app-hover-state);
  }

  &.active {
    border-left-color: var(--app-brand-color);
    background: var(--app-hover-state);

    .step-number {
      background: var(--app-brand-color);
      color: #fff;
    }

    .step-title {
      color: var(--app-brand-color);
      font-weight: 600;
    }
  }

  &.completed {
    .step-title {
      color: var(--app-color-success);
    }

    .step-desc {
      color: var(--app-text-tertiary);
    }
  }

  &.disabled {
    cursor: not-allowed;
    opacity: 0.5;
  }
}

.step-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--app-bg-tertiary);
  color: var(--app-text-secondary);
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.step-title {
  font-size: 14px;
  color: var(--app-text-primary);
  font-weight: 500;
}

.step-desc {
  font-size: 12px;
  color: var(--app-text-tertiary);
}

.step-content {
  flex: 1;
  min-width: 0;
  background: var(--app-bg-primary);
  border-radius: var(--app-radius-sm);
  padding: 24px;
  border: 1px solid var(--app-border-light);
}
</style>
