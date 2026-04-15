<template>
  <div class="requirement-create">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>新建需求</h3>
          <div class="ai-switch">
            <span class="ai-switch-label">AI辅助生成</span>
            <el-switch v-model="aiAssisted" />
          </div>
        </div>
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
        <el-form-item v-if="form.matchMode === 'UPLOAD'" label="上传文件" prop="uploadedFileId">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            :file-list="fileList"
            :limit="1"
            :on-exceed="handleExceed"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 doc/docx/pdf 格式，单个文件不超过 50MB</div>
            </template>
          </el-upload>
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
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { requirementApi } from '@/api/requirement'
import { toYuan } from '@/utils/budget'
import { ElMessage, type FormInstance, type UploadFile, type UploadFiles, type UploadRawFile } from 'element-plus'
import { getToken } from '@/utils/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const aiAssisted = ref(false)
const fileList = ref<UploadFile[]>([])

const form = reactive({
  requirementName: '',
  projectCategory: '',
  projectType: '',
  budget: undefined as number | undefined,
  requirementDescription: '',
  matchMode: 'AUTO_MATCH',
  uploadedFileId: undefined as number | undefined,
})

const rules = {
  requirementName: [{ required: true, message: '请输入需求名称', trigger: 'blur' }],
  projectCategory: [{ required: true, message: '请选择项目类别', trigger: 'change' }],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
}

const uploadAction = '/file-api/api/v1/file/upload'

const uploadHeaders = computed(() => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
})

function beforeUpload(file: UploadRawFile) {
  const allowedTypes = [
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/pdf',
  ]
  const isAllowedType = allowedTypes.includes(file.type)
  const isLt50M = file.size / 1024 / 1024 < 50

  if (!isAllowedType) {
    ElMessage.error('仅支持 doc/docx/pdf 格式文件')
    return false
  }
  if (!isLt50M) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  return true
}

function handleUploadSuccess(response: any, _file: UploadFile, _files: UploadFiles) {
  const fileId = response?.data?.id || response?.data?.fileId
  if (fileId) {
    form.uploadedFileId = fileId
    ElMessage.success('文件上传成功')
  } else {
    ElMessage.error('上传返回数据异常，未获取到文件ID')
  }
}

function handleUploadError() {
  ElMessage.error('文件上传失败，请重试')
}

function handleExceed() {
  ElMessage.warning('仅允许上传一个文件，请先删除已上传文件')
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (form.matchMode === 'UPLOAD' && !form.uploadedFileId) {
    ElMessage.error('请上传文件')
    return
  }

  submitting.value = true
  try {
    const res = await requirementApi.create({ ...form, budget: toYuan(form.budget) })
    ElMessage.success('创建成功')
    if (aiAssisted.value && res?.id) {
      router.push(`/requirement/generate/${res.id}`)
    } else {
      router.push('/requirement')
    }
  } catch {
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

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header h3 {
  margin: 0;
}

.ai-switch {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-switch-label {
  font-size: 14px;
  color: #606266;
}
</style>
