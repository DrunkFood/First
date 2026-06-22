<template>
  <div class="project-create">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>{{ isEdit ? '编辑项目' : '新建项目' }}</h3>
          <el-button @click="router.back()">返回列表</el-button>
        </div>
      </template>

      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-width="130px"
        v-loading="pageLoading"
        class="create-form"
      >
        <!-- 新建模式：双模式Tab -->
        <el-tabs
          v-if="!isEdit"
          v-model="activeTab"
          class="mode-tabs"
          @tab-change="handleTabChange"
        >
          <el-tab-pane label="系统生成模式" name="DIRECT" />
          <el-tab-pane label="引用模式" name="REFERENCE" />
        </el-tabs>

        <!-- 引用模式：业务需求选择+预览 -->
        <template v-if="!isEdit && activeTab === 'REFERENCE'">
          <div class="section-title">项目描述</div>
          <el-form-item label="引用业务需求" prop="requirementId">
            <el-select
              v-model="form.requirementId"
              placeholder="请选择已完成的业务需求"
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
          <div v-if="requirementPreviewContent" class="requirement-preview">
            <div class="preview-label">需求内容预览</div>
            <div class="preview-content">{{ requirementPreviewContent }}</div>
          </div>
        </template>

        <!-- 项目基本信息 -->
        <div class="section-title">项目基本信息</div>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目编号" prop="projectCode">
              <el-input
                v-model="form.projectCode"
                placeholder="请输入项目编号，不可重复。"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目名称" prop="projectName">
              <el-input
                v-model="form.projectName"
                placeholder="长度1-100字符，不可重复"
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
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预算金额(万元)" prop="budget">
              <el-input-number
                v-model="form.budget"
                :min="0"
                :precision="6"
                :step="1"
                :disabled="isReferenceMode"
                style="width: 100%"
                placeholder="正数，保留6位小数"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评审方式" prop="reviewType">
              <div class="review-type-wrapper">
                <el-select
                  v-model="form.reviewType"
                  placeholder="请选择评审方式"
                  style="flex: 1"
                >
                  <el-option label="人工评审" value="MANUAL" />
                  <el-option label="智能评审" value="INTELLIGENT" />
                </el-select>
                <el-tag
                  v-if="autoRecommended"
                  type="success"
                  size="small"
                  class="auto-recommend-tag"
                >
                  自动推荐
                </el-tag>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="招标单位">
              <el-input v-model="form.tenderUnit" placeholder="请输入招标单位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
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

        <el-form-item label="项目描述" prop="projectDescription">
          <el-input
            v-model="form.projectDescription"
            type="textarea"
            :rows="4"
            placeholder="请详细描述项目概况、招标范围、技术标准、资格要求等核心信息，描述越清晰，生成的招标文件越精准。示例“本项目服务期2年，预算约480万元，服务面积3.2万㎡。招标范围包括环境卫生保洁、安全保卫、设施设备日常维护及会议服务。质量标准：保洁合格率≥98%，报修响应≤15分钟。投标人须具备近三年政府办公楼物业服务业绩，项目经理持物业管理师证书。”"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <!-- 编辑模式：资格要求 + 其他信息 -->
        <template v-if="isEdit">
          <div class="section-title">资格要求</div>
          <el-form-item label="资格要求">
            <el-input
              v-model="qualificationRequirements"
              type="textarea"
              :rows="8"
              placeholder="请输入资格要求，每行一项"
            />
          </el-form-item>

          <div class="section-title">其他信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="创建时间">
                <el-input :model-value="projectDetail?.createTime || '-'" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="创建人">
                <el-input :model-value="projectDetail?.createName || '-'" disabled />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目状态">
                <el-input disabled>
                  <template #prefix>
                    <StatusBadge
                      v-if="projectDetail?.status"
                      :status="projectDetail.status"
                      :type-map="PROJECT_STATUS_MAP"
                    />
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="使用模板">
                <el-input :model-value="usedTemplateName" disabled />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <!-- 底部按钮 -->
        <el-form-item class="form-actions">
          <template v-if="isEdit">
            <div class="edit-actions">
              <div class="actions-left">
                <el-button @click="router.push('/project')">返回列表</el-button>
                <el-button @click="handleViewDetail">查看详情</el-button>
              </div>
              <div class="actions-right">
                <el-button type="danger" plain @click="handleCancelProject">取消项目</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="submitting">保存修改</el-button>
              </div>
            </div>
          </template>
          <template v-else>
            <el-button @click="router.back()">取消</el-button>
            <el-button type="primary" @click="handleSubmit" :loading="submitting">保存并继续</el-button>
          </template>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { projectApi } from '@/api/project'
import { requirementApi } from '@/api/requirement'
import { templateApi } from '@/api/template'
import { PROJECT_STATUS_MAP, PROJECT_CATEGORY_MAP, PROJECT_TYPE_MAP } from '@/constants/status-maps'
import { toWanYuan, toYuan } from '@/utils/budget'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import type { ProjectCreateParams, ProjectInfo } from '@/types/project'
import type { RequirementInfo } from '@/types/requirement'
import type { TemplateInfo } from '@/types/template'
import StatusBadge from '@/components/common/StatusBadge.vue'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const pageLoading = ref(false)
const activeTab = ref<'DIRECT' | 'REFERENCE'>('DIRECT')

const requirementList = ref<RequirementInfo[]>([])
const templateList = ref<TemplateInfo[]>([])
const requirementPreviewContent = ref('')
const autoRecommended = ref(false)
const isDataLoading = ref(false)
const qualificationRequirements = ref('')
const projectDetail = ref<ProjectInfo | null>(null)

const isEdit = computed(() => route.name === 'ProjectEdit' || !!route.params.id)
const isReferenceMode = computed(() => !isEdit.value && activeTab.value === 'REFERENCE')

const usedTemplateName = computed(() => {
  if (!projectDetail.value?.templateId) return '-'
  const tpl = templateList.value.find(t => t.id === projectDetail.value!.templateId)
  return tpl?.templateName || '-'
})

const form = reactive<ProjectCreateParams>({
  projectCode: '',
  projectName: '',
  projectCategory: '',
  projectType: '',
  serviceSubType: undefined,
  budget: undefined,
  reviewType: 'MANUAL',
  requirementContent: undefined,
  templateId: undefined,
  requirementId: undefined,
  requirementSource: 'SYSTEM_GENERATE',
  tenderUnit: undefined,
  contactPerson: undefined,
  contactPhone: undefined,
  projectDescription: undefined,
})

const rules = {
  projectCode: [
    { required: true, message: '请输入项目编号', trigger: 'blur' },
    { max: 50, message: '项目编号不超过50个字符', trigger: 'blur' },
  ],
  projectName: [
    { required: true, message: '请输入项目名称', trigger: 'blur' },
    {
      asyncValidator: async (_rule: any, value: string, callback: any) => {
        if (!value || !value.trim()) return callback()
        try {
          const editId = isEdit.value ? Number(route.params.id) : undefined
          const isUnique = await projectApi.checkName(value.trim(), editId)
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
  projectCategory: [{ required: true, message: '请选择项目类别', trigger: 'change' }],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  reviewType: [{ required: true, message: '请选择评审方式', trigger: 'change' }],
  budget: [
    { required: true, message: '请输入预算金额', trigger: 'blur' },
    {
      type: 'number',
      validator: (_rule: any, value: number | undefined, callback: any) => {
        if (value === undefined || value === null) {
          callback(new Error('请输入预算金额'))
          return
        }
        if (value <= 0) {
          callback(new Error('预算金额必须大于0'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  projectDescription: [
    { required: true, message: '请输入项目描述', trigger: 'blur' },
  ],
  requirementId: [{ required: true, message: '请选择业务需求', trigger: 'change' }],
}

/** 自动推荐评审类型 */
function computeAutoReviewType() {
  if (!form.budget || !form.projectType) {
    autoRecommended.value = false
    return
  }
  // 预算>=500万 或 工程/货物类 -> 人工评审
  if (form.budget >= 500 || form.projectType === 'ENGINEERING' || form.projectType === 'GOODS') {
    form.reviewType = 'MANUAL'
  } else {
    form.reviewType = 'INTELLIGENT'
  }
  autoRecommended.value = true
}

watch(() => [form.budget, form.projectType], () => {
  if (isDataLoading.value) return
  computeAutoReviewType()
})

async function loadRequirements() {
  try {
    const res = await requirementApi.getList({
      pageNum: 1,
      pageSize: 200,
      status: 'COMPLETED',
    })
    requirementList.value = res.records || []
  } catch {
    ElMessage.error('加载业务需求列表失败')
  }
}

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

async function loadProjectDetail(id: number) {
  pageLoading.value = true
  isDataLoading.value = true
  try {
    const data = await projectApi.getById(id)
    projectDetail.value = data
    Object.assign(form, {
      projectCode: data.projectCode || '',
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
    // 根据当前值判断是否为自动推荐
    autoRecommended.value = false
  } catch {
    ElMessage.error('加载项目详情失败')
  } finally {
    pageLoading.value = false
    nextTick(() => { isDataLoading.value = false })
  }
}

function handleTabChange(tab: string | number) {
  form.requirementSource = tab === 'REFERENCE' ? 'REFERENCE' : 'SYSTEM_GENERATE'
  if (tab === 'REFERENCE' && requirementList.value.length === 0) {
    loadRequirements()
  }
  if (tab === 'DIRECT') {
    form.requirementId = undefined
    requirementPreviewContent.value = ''
  }
}

function handleRequirementChange(reqId: number | undefined) {
  if (!reqId) {
    requirementPreviewContent.value = ''
    return
  }
  const req = requirementList.value.find((r) => r.id === reqId)
  if (!req) return
  form.projectCategory = req.projectCategory || ''
  form.projectType = req.projectType || ''
  form.budget = toWanYuan(req.budget)
  form.requirementContent = req.content || req.requirementDescription || ''
  requirementPreviewContent.value = req.content || req.requirementDescription || ''
  if (req.requirementName && !form.projectName) {
    form.projectName = req.requirementName
  }
}

function applyQueryParams() {
  const query = route.query
  const fieldMap: Record<string, keyof ProjectCreateParams> = {
    projectCode: 'projectCode',
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
  if (query.budget) form.budget = Number(query.budget)
  if (query.templateId) form.templateId = Number(query.templateId)
  if (query.requirementId) form.requirementId = Number(query.requirementId)
  if (query.requirementSource === 'REFERENCE') {
    activeTab.value = 'REFERENCE'
    loadRequirements()
  }
}

function handleViewDetail() {
  if (route.params.id) {
    router.push(`/project/${route.params.id}`)
  }
}

async function handleCancelProject() {
  if (!route.params.id) return
  try {
    await ElMessageBox.confirm('确认取消该项目？取消后项目将终止所有流程。', '操作确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await projectApi.cancel(Number(route.params.id))
    ElMessage.success('项目已取消')
    router.push('/project')
  } catch {
    // 用户取消
  }
}

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
      router.push(`/project/${id}`)
    } else {
      const project = await projectApi.create(submitData)
      ElMessage.success('创建成功')
      router.push(`/project/${project.id}/wizard?step=0`)
    }
  } catch {
    ElMessage.error(isEdit.value ? '修改失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadTemplates()
  if (isEdit.value) {
    await loadProjectDetail(Number(route.params.id))
  } else {
    applyQueryParams()
  }
})
</script>

<style scoped lang="scss">
.project-create {
  padding: 20px;
  max-width: 1600px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  h3 { margin: 0; }
}

.mode-tabs {
  margin-bottom: 20px;
}

.mode-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 20px 0 12px;
  padding-left: 10px;
  border-left: 3px solid var(--app-brand-color);
}

.requirement-preview {
  margin: 0 0 20px 130px;
  background: var(--app-bg-secondary);
  border: 1px solid var(--app-border-light);
  border-radius: var(--app-radius-sm);
  padding: 16px;
}

.preview-label {
  font-size: 13px;
  color: var(--app-text-tertiary);
  margin-bottom: 8px;
}

.preview-content {
  font-size: 14px;
  color: var(--app-text-secondary);
  line-height: 1.6;
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
}

.review-type-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.auto-recommend-tag {
  white-space: nowrap;
}

.edit-actions {
  display: flex;
  justify-content: space-between;
  width: 100%;

  .actions-left,
  .actions-right {
    display: flex;
    gap: 8px;
  }
}

.form-actions {
  margin-top: 24px;
}
</style>
