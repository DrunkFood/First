<template>
  <div class="requirement-create">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">新建业务需求</h1>
      <el-button @click="router.push('/requirement')">
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </el-button>
    </div>

    <div class="form-container">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="130px">
        <!-- 第一部分：业务需求基本信息 -->
        <div class="form-section">
          <h3 class="section-title">业务需求基本信息</h3>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目名称" prop="requirementName">
                <el-input
                  v-model="form.requirementName"
                  placeholder="请输入项目名称，1-100字符，必须唯一"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目类型" prop="projectType">
                <el-select v-model="form.projectType" placeholder="请选择项目类型" @change="handleTypeChange">
                  <el-option label="工程类" value="ENGINEERING" />
                  <el-option label="货物类" value="GOODS" />
                  <el-option-group label="服务类">
                    <el-option label="服务类" value="SERVICE" />
                    <el-option label="物业" value="PROPERTY" />
                    <el-option label="IT服务" value="IT_SERVICE" />
                    <el-option label="咨询服务" value="CONSULTING" />
                    <el-option label="维保服务" value="MAINTENANCE" />
                  </el-option-group>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="预算价(万元)" prop="budget">
                <el-input-number
                  v-model="form.budget"
                  :min="0"
                  :precision="6"
                  :step="0.01"
                  placeholder="请输入正数，保留6位小数"
                  class="full-width"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="项目基本情况描述">
            <el-input
              v-model="form.requirementDescription"
              type="textarea"
              :rows="4"
              placeholder="请输入项目基本情况描述，最多500字"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </div>

        <!-- 第二部分：历史业务需求匹配 -->
        <div class="form-section">
          <h3 class="section-title">历史业务需求匹配</h3>

          <MatchModePanel
            v-model="form.matchMode"
            :match-files="matchFiles"
            :selected-file-id="form.matchedFileId"
            :upload-accept="'.doc,.docx'"
            :upload-limit="1"
            mode="create"
            @update:selected-file-id="form.matchedFileId = $event"
            @file-preview="handlePreviewFile"
          />
        </div>

        <!-- 底部按钮 -->
        <div class="form-actions">
          <el-button @click="router.push('/requirement')">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            保存并继续
          </el-button>
        </div>
      </el-form>
    </div>

    <!-- 文件预览弹窗 -->
    <el-dialog v-model="previewVisible" title="文件预览" width="600px" destroy-on-close>
      <div v-if="previewFile" class="preview-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目名称">{{ previewFile.fileName }}</el-descriptions-item>
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
import { useRouter } from 'vue-router'
import { requirementApi } from '@/api/requirement'
import { toYuan } from '@/utils/budget'
import { ElMessage, type FormInstance } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import MatchModePanel from '@/components/requirement/MatchModePanel.vue'
import type { MatchFile } from '@/types/requirement'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const matchFiles = ref<MatchFile[]>([])
const previewVisible = ref(false)
const previewFile = ref<MatchFile | null>(null)

const form = reactive({
  requirementName: '',
  projectType: '',
  budget: undefined as number | undefined,
  requirementDescription: '',
  matchMode: 'AUTO_MATCH',
  matchedFileId: undefined as number | undefined,
  uploadedFileId: undefined as number | undefined,
})

const rules = {
  requirementName: [
    { required: true, message: '请输入项目名称', trigger: 'blur' },
    { max: 100, message: '项目名称不能超过100字符', trigger: 'blur' },
    {
      asyncValidator: async (_rule: any, value: string, callback: any) => {
        if (!value || !value.trim()) return callback()
        try {
          const isUnique = await requirementApi.checkName(value.trim())
          if (isUnique) {
            callback()
          } else {
            callback(new Error('该项目名称已存在'))
          }
        } catch {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  budget: [
    { required: true, message: '请输入预算价', trigger: 'blur' },
    { type: 'number', validator: (_rule: any, value: any, callback: any) => {
      if (value !== undefined && value !== null && value <= 0) {
        callback(new Error('预算价必须大于0'))
      } else {
        callback()
      }
    }, trigger: 'blur' },
  ],
}

function handleTypeChange() {
  // 类型变更时清除匹配数据
  form.matchedFileId = undefined
}

function handlePreviewFile(file: MatchFile) {
  previewFile.value = file
  previewVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (form.matchMode === 'MANUAL_SELECT' && !form.matchedFileId) {
    ElMessage.warning('请选择一个匹配文件')
    return
  }

  submitting.value = true
  try {
    const res = await requirementApi.create({
      ...form,
      budget: toYuan(form.budget),
    })
    ElMessage.success('创建成功')
    if (res?.id) {
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

function formatBudget(yuan?: number): string {
  if (yuan == null) return '-'
  return `¥${(yuan / 10000).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 6 })} 万元`
}

onMounted(async () => {
  try {
    matchFiles.value = await requirementApi.getMatchFiles({}) || []
  } catch {
    matchFiles.value = []
  }
})
</script>

<style scoped>
.requirement-create {
  padding: 24px 40px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 0;
}

.form-container {
  background: var(--app-bg-elevated);
  border-radius: 8px;
  border: 1px solid var(--app-border-light);
  overflow: hidden;
}

.form-section {
  padding: 24px;
  border-bottom: 1px solid var(--app-border-light);
}

.form-section:last-of-type {
  border-bottom: none;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--app-brand-color);
}

.full-width {
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 24px;
  background: var(--app-bg-tertiary);
  border-top: 1px solid var(--app-border-light);
}

.preview-content {
  padding: 8px 0;
}
</style>
