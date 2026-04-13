<template>
  <div class="requirement-create">
    <el-card>
      <template #header>
        <h3>新建需求</h3>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="需求名称" prop="requirementName">
          <el-input v-model="form.requirementName" placeholder="请输入需求名称" />
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
        <el-form-item label="预算价(万元)">
          <el-input-number v-model="form.budget" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="需求描述">
          <el-input v-model="form.requirementDescription" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="匹配模式">
          <el-radio-group v-model="form.matchMode">
            <el-radio label="AUTO_MATCH">自动匹配</el-radio>
            <el-radio label="MANUAL_SELECT">手动选择</el-radio>
            <el-radio label="UPLOAD">上传</el-radio>
          </el-radio-group>
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
import { requirementApi } from '@/api/requirement'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  requirementName: '',
  projectCategory: '',
  projectType: '',
  budget: undefined as number | undefined,
  requirementDescription: '',
  matchMode: 'AUTO_MATCH',
})

const rules = {
  requirementName: [{ required: true, message: '请输入需求名称', trigger: 'blur' }],
  projectCategory: [{ required: true, message: '请选择项目类别', trigger: 'change' }],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await requirementApi.create(form)
    ElMessage.success('创建成功')
    router.push('/requirement')
  } catch (e) {
    ElMessage.error('创建失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.requirement-create {
  padding: 20px;
}
</style>
