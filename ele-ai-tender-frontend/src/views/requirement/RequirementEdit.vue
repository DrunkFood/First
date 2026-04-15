<template>
  <div class="requirement-edit">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button @click="router.push('/requirement')">返回列表</el-button>
          <h3>编辑业务需求</h3>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="130px">
        <!-- 第一部分：业务需求基本信息 -->
        <el-divider content-position="left">业务需求基本信息</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目名称" prop="requirementName">
              <el-input v-model="form.requirementName" placeholder="请输入项目名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目类型" prop="projectType">
              <el-select v-model="form.projectType" placeholder="请选择项目类型">
                <el-option label="工程类" value="ENGINEERING" />
                <el-option label="货物类" value="GOODS" />
                <el-option label="服务类" value="SERVICE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目预算(万元)" prop="budget">
              <el-input-number
                v-model="form.budget"
                :min="0"
                :precision="2"
                :step="0.01"
                class="full-width"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="需求类型" prop="requirementType">
              <el-select v-model="form.requirementType" placeholder="请选择需求类型">
                <el-option label="新增需求" value="NEW" />
                <el-option label="修改需求" value="MODIFY" />
                <el-option label="延续需求" value="CONTINUE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="项目基本情况描述">
          <el-input
            v-model="form.requirementDescription"
            type="textarea"
            :rows="4"
            placeholder="请输入项目基本情况描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <!-- 第二部分：参考文件选择 -->
        <el-divider content-position="left">参考文件选择</el-divider>

        <el-form-item label="匹配模式">
          <MatchModePanel
            v-model="form.matchMode"
            :match-files="matchFiles"
            :selected-file-ids="form.matchedFileIds"
            :upload-accept="'.doc,.docx,.pdf'"
            :upload-limit="5"
            mode="edit"
            :requirement-id="requirementId"
            @update:selected-file-ids="form.matchedFileIds = $event"
            @file-preview="handlePreviewFile"
          />
        </el-form-item>

        <!-- 第三部分：资格要求 -->
        <el-divider content-position="left">资格要求</el-divider>

        <el-form-item label="资格条件">
          <QualificationList
            v-model="form.qualifications"
            :min-items="1"
          />
        </el-form-item>

        <!-- 底部按钮 -->
        <el-form-item class="form-actions">
          <el-button @click="router.push('/requirement')">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            保存修改
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 文件预览弹窗 -->
    <el-dialog v-model="previewVisible" title="文件预览" width="600px" destroy-on-close>
      <div v-if="previewFile" class="preview-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文件名称">{{ previewFile.fileName }}</el-descriptions-item>
          <el-descriptions-item label="项目类型">{{ previewFile.fileType }}</el-descriptions-item>
          <el-descriptions-item label="项目预算">{{ formatBudget(previewFile.budget) }}</el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ previewFile.uploadTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { requirementApi } from '@/api/requirement'
import { toYuan } from '@/utils/budget'
import { ElMessage, type FormInstance } from 'element-plus'
import MatchModePanel from '@/components/requirement/MatchModePanel.vue'
import QualificationList from '@/components/requirement/QualificationList.vue'
import type { MatchFile, RequirementType } from '@/types/requirement'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const requirementId = ref(0)
const matchFiles = ref<MatchFile[]>([])
const previewVisible = ref(false)
const previewFile = ref<MatchFile | null>(null)

const form = reactive({
  requirementName: '',
  projectType: '',
  budget: undefined as number | undefined,
  requirementType: 'NEW' as RequirementType,
  requirementDescription: '',
  matchMode: 'SYSTEM_SELECT',
  matchedFileIds: [] as number[],
  uploadedFileId: undefined as number | undefined,
  qualifications: [''] as string[],
})

const rules = {
  requirementName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  requirementType: [{ required: true, message: '请选择需求类型', trigger: 'change' }],
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id || isNaN(id)) {
    ElMessage.error('参数错误')
    router.back()
    return
  }
  requirementId.value = id

  try {
    const data = await requirementApi.getById(id)
    form.requirementName = data.requirementName || ''
    form.projectType = data.projectType || ''
    form.budget = data.budget ? data.budget / 10000 : undefined
    form.requirementType = data.requirementType || 'NEW'
    form.requirementDescription = data.requirementDescription || ''
    form.matchMode = data.matchMode || 'SYSTEM_SELECT'
    form.matchedFileIds = data.matchedFileIds || []
    form.uploadedFileId = data.uploadedFileId
    form.qualifications = data.qualifications?.length ? data.qualifications : ['']
  } catch {
    ElMessage.error('加载需求失败')
    router.back()
  }

  try {
    matchFiles.value = await requirementApi.getMatchFiles({ requirementId: id }) || []
  } catch {
    matchFiles.value = []
  }
})

function handlePreviewFile(file: MatchFile) {
  previewFile.value = file
  previewVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const emptyQuals = form.qualifications.filter(q => !q.trim())
  if (emptyQuals.length > 0) {
    ElMessage.warning('请填写所有资格条件')
    return
  }

  submitting.value = true
  try {
    await requirementApi.update(requirementId.value, {
      requirementName: form.requirementName,
      projectType: form.projectType,
      budget: toYuan(form.budget),
      requirementType: form.requirementType,
      requirementDescription: form.requirementDescription,
      matchMode: form.matchMode,
      matchedFileIds: form.matchedFileIds,
      uploadedFileId: form.uploadedFileId,
      qualifications: form.qualifications,
    })
    ElMessage.success('保存成功')
    router.push('/requirement')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    submitting.value = false
  }
}

function formatBudget(yuan?: number): string {
  if (yuan == null) return '-'
  return `¥${(yuan / 10000).toLocaleString('zh-CN', { maximumFractionDigits: 2 })} 万元`
}
</script>

<style scoped>
.requirement-edit {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  color: var(--app-text-primary);
}

.full-width {
  width: 100%;
}

.form-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--app-border-light);
}

.preview-content {
  padding: 8px 0;
}
</style>
