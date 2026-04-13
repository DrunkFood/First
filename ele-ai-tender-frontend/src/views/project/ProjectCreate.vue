<template>
  <div class="project-create">
    <el-card>
      <template #header>
        <h3>{{ isEdit ? '编辑项目' : '新建项目' }}</h3>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
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
        <el-form-item label="预算金额(万元)">
          <el-input-number v-model="form.budget" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="评审类型">
          <el-select v-model="form.reviewType" placeholder="请选择" clearable>
            <el-option label="人工评审" value="MANUAL" />
            <el-option label="智能评审" value="INTELLIGENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="需求内容">
          <el-input v-model="form.requirementContent" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">提交</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { projectApi } from '@/api/project'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const isEdit = ref(false)

const form = reactive({
  projectName: '',
  projectCategory: '',
  projectType: '',
  budget: undefined as number | undefined,
  reviewType: '',
  requirementContent: '',
})

const rules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectCategory: [{ required: true, message: '请选择项目类别', trigger: 'change' }],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await projectApi.create(form)
    ElMessage.success('创建成功')
    router.push('/project')
  } catch (e) {
    ElMessage.error('创建失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.project-create {
  padding: 20px;
}
</style>
