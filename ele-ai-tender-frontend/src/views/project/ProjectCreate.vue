<template>
  <div class="project-create">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>{{ isEdit ? '编辑项目' : '新建项目' }}</h3>
          <el-button v-if="!isEdit" @click="router.back()">返回</el-button>
        </div>
      </template>

      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-width="130px"
        v-loading="pageLoading"
      >
        <!-- 新建模式下显示双模式Tab -->
        <el-tabs
          v-if="!isEdit"
          v-model="activeTab"
          class="mode-tabs"
          @tab-change="handleTabChange"
        >
          <el-tab-pane label="系统生成模式" name="DIRECT" />
          <el-tab-pane label="引用模式" name="REFERENCE" />
        </el-tabs>

        <!-- 引用模式：业务需求选择 -->
        <template v-if="!isEdit && activeTab === 'REFERENCE'">
          <el-form-item label="引用业务需求" prop="requirementId">
            <el-select
              v-model="form.requirementId"
              placeholder="请选择已审核通过的业务需求"
              filterable
              clearable
              style="width: 100%"
              @change="handleRequirementChange"
            >
              <el-option
                v-for="req in requirementList"
                :key="req.id"
                :label="req.requirementName"
                :value="req.id"
              />
            </el-select>
          </el-form-item>
        </template>

        <!-- 基本信息 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目名称" prop="projectName">
              <el-input
                v-model="form.projectName"
                placeholder="请输入项目名称"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目类别" prop="projectCategory">
              <el-select
                v-model="form.projectCategory"
                placeholder="请选择项目类别"
                style="width: 100%"
                :disabled="isReferenceMode"
              >
                <el-option
                  v-for="(item, key) in PROJECT_CATEGORY_MAP"
                  :key="key"
                  :label="item.label"
                  :value="key"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目类型" prop="projectType">
              <el-select
                v-model="form.projectType"
                placeholder="请选择项目类型"
                style="width: 100%"
                :disabled="isReferenceMode"
              >
                <el-option
                  v-for="(item, key) in PROJECT_TYPE_MAP"
                  :key="key"
                  :label="item.label"
                  :value="key"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务子类型">
              <el-input
                v-model="form.serviceSubType"
                placeholder="请输入服务子类型"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预算金额(万元)">
              <el-input-number
                v-model="form.budget"
                :min="0"
                :precision="2"
                :disabled="isReferenceMode"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评审类型">
              <el-select
                v-model="form.reviewType"
                placeholder="请选择评审类型"
                clearable
                style="width: 100%"
              >
                <el-option label="人工评审" value="MANUAL" />
                <el-option label="智能评审" value="INTELLIGENT" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="招标单位">
              <el-input
                v-model="form.tenderUnit"
                placeholder="请输入招标单位"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input
                v-model="form.contactPerson"
                placeholder="请输入联系人"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input
                v-model="form.contactPhone"
                placeholder="请输入联系电话"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板选择">
              <el-select
                v-model="form.templateId"
                placeholder="请选择模板"
                clearable
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="tpl in templateList"
                  :key="tpl.id"
                  :label="tpl.templateName"
                  :value="tpl.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="项目描述">
          <el-input
            v-model="form.projectDescription"
            type="textarea"
            :rows="4"
            placeholder="请输入项目描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存修改' : '提交' }}
          </el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { projectApi } from '@/api/project'
import { requirementApi } from '@/api/requirement'
import { templateApi } from '@/api/template'
import { PROJECT_CATEGORY_MAP, PROJECT_TYPE_MAP } from '@/constants/status-maps'
import { toWanYuan, toYuan } from '@/utils/budget'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import type { ProjectCreateParams } from '@/types/project'
import type { RequirementInfo } from '@/types/requirement'
import type { TemplateInfo } from '@/types/template'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const pageLoading = ref(false)
const activeTab = ref<'DIRECT' | 'REFERENCE'>('DIRECT')

const requirementList = ref<RequirementInfo[]>([])
const templateList = ref<TemplateInfo[]>([])

const isEdit = computed(() => route.name === 'ProjectEdit' || !!route.params.id)
const isReferenceMode = computed(() => !isEdit.value && activeTab.value === 'REFERENCE')

const form = reactive<ProjectCreateParams>({
  projectName: '',
  projectCategory: '',
  projectType: '',
  serviceSubType: undefined,
  budget: undefined,
  reviewType: undefined,
  requirementContent: undefined,
  templateId: undefined,
  requirementId: undefined,
  requirementSource: 'DIRECT',
  tenderUnit: undefined,
  contactPerson: undefined,
  contactPhone: undefined,
  projectDescription: undefined,
})

const rules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectCategory: [{ required: true, message: '请选择项目类别', trigger: 'change' }],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  requirementId: [{ required: true, message: '请选择业务需求', trigger: 'change' }],
}

/** 加载业务需求列表（已审核通过） */
async function loadRequirements() {
  try {
    const res = await requirementApi.getList({
      pageNum: 1,
      pageSize: 200,
      status: 'APPROVED',
    })
    requirementList.value = res.records || []
  } catch {
    ElMessage.error('加载业务需求列表失败')
  }
}

/** 加载模板列表 */
async function loadTemplates() {
  try {
    const res = await templateApi.getList({
      pageNum: 1,
      pageSize: 200,
      status: 'PUBLISHED',
    })
    templateList.value = res.records || []
  } catch {
    ElMessage.error('加载模板列表失败')
  }
}

/** 加载项目详情（编辑模式） */
async function loadProjectDetail(id: number) {
  pageLoading.value = true
  try {
    const data = await projectApi.getById(id)
    Object.assign(form, {
      projectName: data.projectName || '',
      projectCategory: data.projectCategory || '',
      projectType: data.projectType || '',
      serviceSubType: data.serviceSubType,
      budget: toWanYuan(data.budget),
      reviewType: data.reviewType,
      requirementContent: data.requirementContent,
      templateId: data.templateId,
      requirementId: data.requirementId,
      requirementSource: data.requirementSource,
      tenderUnit: data.tenderUnit,
      contactPerson: data.contactPerson,
      contactPhone: data.contactPhone,
      projectDescription: data.projectDescription,
    })
  } catch {
    ElMessage.error('加载项目详情失败')
  } finally {
    pageLoading.value = false
  }
}

/** Tab切换处理 */
function handleTabChange(tab: string | number) {
  form.requirementSource = tab as 'DIRECT' | 'REFERENCE'
  if (tab === 'REFERENCE' && requirementList.value.length === 0) {
    loadRequirements()
  }
  // 切换模式时清空引用相关字段
  if (tab === 'DIRECT') {
    form.requirementId = undefined
  }
}

/** 选择业务需求后自动填充 */
function handleRequirementChange(reqId: number | undefined) {
  if (!reqId) return
  const req = requirementList.value.find((r) => r.id === reqId)
  if (!req) return
  form.projectCategory = req.projectCategory || ''
  form.projectType = req.projectType || ''
  form.budget = toWanYuan(req.budget)
  form.requirementContent = req.content || req.requirementDescription || ''
  if (req.requirementName && !form.projectName) {
    form.projectName = req.requirementName
  }
}

/** URL参数自动填充 */
function applyQueryParams() {
  const query = route.query
  const fieldMap: Record<string, keyof ProjectCreateParams> = {
    projectName: 'projectName',
    projectCategory: 'projectCategory',
    projectType: 'projectType',
    serviceSubType: 'serviceSubType',
    reviewType: 'reviewType',
    tenderUnit: 'tenderUnit',
    contactPerson: 'contactPerson',
    contactPhone: 'contactPhone',
    projectDescription: 'projectDescription',
    requirementSource: 'requirementSource',
  }
  for (const [queryKey, formKey] of Object.entries(fieldMap)) {
    const val = query[queryKey]
    if (val && typeof val === 'string') {
      ;(form as any)[formKey] = val
    }
  }
  // 数值类型字段
  if (query.budget) {
    form.budget = Number(query.budget)
  }
  if (query.templateId) {
    form.templateId = Number(query.templateId)
  }
  if (query.requirementId) {
    form.requirementId = Number(query.requirementId)
  }
  // 如果URL指定了需求来源为REFERENCE，切换Tab
  if (query.requirementSource === 'REFERENCE') {
    activeTab.value = 'REFERENCE'
    loadRequirements()
  }
}

/** 提交表单 */
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const submitData = { ...form, budget: toYuan(form.budget) }
    if (isEdit.value) {
      const id = Number(route.params.id)
      await projectApi.update(id, submitData)
      ElMessage.success('修改成功')
    } else {
      await projectApi.create(submitData)
      ElMessage.success('创建成功')
    }
    router.push('/project')
  } catch {
    ElMessage.error(isEdit.value ? '修改失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  // 加载模板列表（两种模式都需要）
  await loadTemplates()

  if (isEdit.value) {
    // 编辑模式：加载项目详情
    await loadProjectDetail(Number(route.params.id))
  } else {
    // 新建模式：应用URL参数自动填充
    applyQueryParams()
  }
})
</script>

<style scoped>
.project-create {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
}

.mode-tabs {
  margin-bottom: 20px;
}

.mode-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
</style>
