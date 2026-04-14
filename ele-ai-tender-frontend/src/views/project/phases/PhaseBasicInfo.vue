<template>
  <div class="phase-basic-info">
    <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
      <el-form-item label="项目名称" prop="projectName">
        <el-input v-model="form.projectName" placeholder="请输入项目名称" />
      </el-form-item>
      <el-form-item label="项目类别" prop="projectCategory">
        <el-select v-model="form.projectCategory" placeholder="请选择">
          <el-option label="限额以下" value="LIMITED_BELOW" />
          <el-option label="产权交易" value="PROPERTY_TRADE" />
          <el-option label="政府采购" value="GOVERNMENT_PROCUREMENT" />
        </el-select>
      </el-form-item>
      <el-form-item label="项目类型" prop="projectType">
        <el-select v-model="form.projectType" placeholder="请选择">
          <el-option label="工程" value="ENGINEERING" />
          <el-option label="货物" value="GOODS" />
          <el-option label="服务" value="SERVICE" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.projectType === 'SERVICE'" label="服务子类型">
        <el-select v-model="form.serviceSubType" placeholder="请选择">
          <el-option label="物业服务" value="物业服务" />
          <el-option label="IT服务" value="IT服务" />
          <el-option label="咨询服务" value="咨询服务" />
          <el-option label="维保服务" value="维保服务" />
        </el-select>
      </el-form-item>
      <el-form-item label="预算金额(万元)" prop="budget">
        <el-input-number v-model="form.budget" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="评审方式" prop="reviewType">
        <el-radio-group v-model="form.reviewType">
          <el-radio value="INTELLIGENT">智能评审</el-radio>
          <el-radio value="MANUAL">人工评审</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="项目描述">
        <el-input v-model="form.projectDescription" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="招标单位">
        <el-input v-model="form.tenderUnit" />
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="form.contactPerson" />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="form.contactPhone" />
      </el-form-item>
    </el-form>

    <div class="phase-actions">
      <el-button type="primary" @click="handleSaveAndNext">保存并继续</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { projectApi } from '@/api/project'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const props = defineProps<{ projectId: number }>()
const emit = defineEmits<{ next: [] }>()

const formRef = ref<FormInstance>()
const form = ref({
  projectName: '',
  projectCategory: '',
  projectType: '',
  serviceSubType: '',
  budget: 0,
  reviewType: 'INTELLIGENT',
  projectDescription: '',
  tenderUnit: '',
  contactPerson: '',
  contactPhone: '',
})

const rules: FormRules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectCategory: [{ required: true, message: '请选择项目类别', trigger: 'change' }],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  budget: [{ required: true, message: '请输入预算金额', trigger: 'blur' }],
  reviewType: [{ required: true, message: '请选择评审方式', trigger: 'change' }],
}

const loadProject = async () => {
  const project = await projectApi.getById(props.projectId)
  Object.assign(form.value, {
    projectName: project.projectName || '',
    projectCategory: project.projectCategory || '',
    projectType: project.projectType || '',
    serviceSubType: project.serviceSubType || '',
    budget: project.budget || 0,
    reviewType: project.reviewType || 'INTELLIGENT',
  })
}

const handleSaveAndNext = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  await projectApi.update(props.projectId, form.value)
  ElMessage.success('基础信息保存成功')
  emit('next')
}

onMounted(loadProject)
</script>

<style scoped>
.phase-basic-info {
  max-width: 600px;
}

.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
