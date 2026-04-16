<template>
  <div class="phase-basic-info">
    <!-- 项目基本信息 -->
    <div class="section-title">项目基本信息</div>
    <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="项目名称" prop="projectName">
            <el-input v-model="form.projectName" placeholder="请输入项目名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="项目类别" prop="projectCategory">
            <el-select v-model="form.projectCategory" placeholder="请选择" style="width: 100%" @change="handleCategoryChange">
              <el-option label="限额以下" value="LIMITED_BELOW" />
              <el-option label="产权交易" value="PROPERTY_TRADE" />
              <el-option label="政府采购" value="GOVERNMENT_PROCUREMENT" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="项目类型" prop="projectType">
            <el-select v-model="form.projectType" placeholder="请选择" style="width: 100%" @change="handleTypeChange">
              <el-option label="工程" value="ENGINEERING" />
              <el-option label="货物" value="GOODS" />
              <el-option label="服务" value="SERVICE" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item v-if="form.projectType === 'SERVICE'" label="服务子类型">
            <el-select v-model="form.serviceSubType" placeholder="请选择" style="width: 100%">
              <el-option label="物业服务" value="物业服务" />
              <el-option label="IT服务" value="IT服务" />
              <el-option label="咨询服务" value="咨询服务" />
              <el-option label="维保服务" value="维保服务" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="预算金额(万元)" prop="budget">
            <el-input-number v-model="form.budget" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="评审方式" prop="reviewType">
            <el-radio-group v-model="form.reviewType">
              <el-radio value="INTELLIGENT">智能评审</el-radio>
              <el-radio value="MANUAL">人工评审</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="招标单位">
            <el-input v-model="form.tenderUnit" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系人">
            <el-input v-model="form.contactPerson" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="联系电话">
        <el-input v-model="form.contactPhone" />
      </el-form-item>
      <el-form-item label="项目描述">
        <el-input v-model="form.projectDescription" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>

    <!-- 选择招标文件模板 -->
    <div class="section-title">选择招标文件模板</div>
    <div class="template-grid" v-loading="templateLoading">
      <div
        v-for="tpl in templateList"
        :key="tpl.id"
        class="template-card"
        :class="{ selected: form.templateId === tpl.id }"
        @click="form.templateId = tpl.id"
      >
        <div class="tpl-card-header">
          <span class="tpl-name">{{ tpl.templateName }}</span>
          <el-tag
            v-if="isDefaultTemplate(tpl)"
            type="success"
            size="small"
            effect="dark"
          >推荐</el-tag>
        </div>
        <p class="tpl-desc">{{ tpl.structureDefinition || '标准招标文件模板' }}</p>
        <div class="tpl-meta">
          <span>v{{ tpl.versionNo || '1' }}</span>
          <span>{{ formatTime(tpl.createTime) }}</span>
        </div>
        <el-button text size="small" @click.stop="handlePreviewTemplate(tpl)">预览</el-button>
      </div>
      <el-empty v-if="!templateLoading && !templateList.length" description="暂无可用模板" :image-size="60" />
    </div>

    <!-- 历史招标文件匹配 -->
    <div class="section-title">历史招标文件匹配</div>
    <div class="match-section">
      <el-radio-group v-model="matchMode" class="match-mode-group">
        <el-radio-button value="auto">系统自动匹配</el-radio-button>
        <el-radio-button value="manual">手动选择</el-radio-button>
        <el-radio-button value="upload">上传文件</el-radio-button>
      </el-radio-group>

      <!-- 自动匹配模式 -->
      <div v-if="matchMode === 'auto'" class="match-content">
        <p class="match-desc">系统将根据项目名称和描述自动匹配历史招标文件</p>
        <el-button type="primary" :loading="autoMatching" @click="handleAutoMatch">
          开始自动匹配
        </el-button>
        <el-button type="success" :loading="suggesting" @click="handleSuggest">
          AI推荐
        </el-button>
      </div>

      <!-- 手动选择模式 -->
      <div v-if="matchMode === 'manual'" class="match-content">
        <div class="manual-search-bar">
          <el-input
            v-model="manualKeyword"
            placeholder="输入关键词搜索"
            clearable
            @keyup.enter="handleManualMatch"
          >
            <template #append>
              <el-button :loading="manualMatching" @click="handleManualMatch">搜索</el-button>
            </template>
          </el-input>
        </div>
        <div v-if="matchResults.length" class="match-file-list">
          <div
            v-for="item in matchResults"
            :key="item.requirementId"
            class="match-file-card"
            :class="{ selected: selectedMatchId === item.requirementId }"
            @click="selectedMatchId = item.requirementId"
          >
            <div class="match-file-info">
              <span class="match-file-name">{{ item.requirementName }}</span>
              <el-progress
                :percentage="Math.round(item.similarity * 100)"
                :stroke-width="10"
                :format="() => Math.round(item.similarity * 100) + '%'"
                style="width: 120px"
              />
            </div>
            <div class="match-file-actions">
              <el-button text size="small" @click.stop="handlePreviewMatch(item)">预览</el-button>
              <el-button text size="small" type="primary" @click.stop="selectedMatchId = item.requirementId">选择</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else-if="!manualMatching" description="暂无匹配结果，请输入关键词搜索" :image-size="60" />
      </div>

      <!-- 上传模式 -->
      <div v-if="matchMode === 'upload'" class="match-content">
        <el-upload
          drag
          :auto-upload="false"
          accept=".doc,.docx"
          :limit="1"
          :on-change="handleFileChange"
          class="upload-area"
        >
          <el-icon size="48"><UploadFilled /></el-icon>
          <div>将文件拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="upload-tip">仅支持.doc/.docx格式，最大50MB</div>
          </template>
        </el-upload>
      </div>

      <!-- AI推荐结果 -->
      <div v-if="suggestResults.length" class="suggest-section">
        <h4>AI推荐</h4>
        <el-tag
          v-for="(s, idx) in suggestResults"
          :key="idx"
          class="suggest-tag"
          effect="plain"
        >{{ s }}</el-tag>
      </div>
    </div>

    <!-- 底部操作 -->
    <div class="phase-actions">
      <el-button type="primary" @click="handleSaveAndNext">保存并继续</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { projectApi } from '@/api/project'
import { templateApi } from '@/api/template'
import { aiApi } from '@/api/ai'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { toWanYuan, toYuan } from '@/utils/budget'
import type { TemplateInfo } from '@/types/template'
import type { AiMatchResult } from '@/types/ai'

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
  templateId: null as number | null,
})

const rules: FormRules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectCategory: [{ required: true, message: '请选择项目类别', trigger: 'change' }],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  budget: [{ required: true, message: '请输入预算金额', trigger: 'blur' }],
  reviewType: [{ required: true, message: '请选择评审方式', trigger: 'change' }],
}

// --- 模板 ---
const templateList = ref<TemplateInfo[]>([])
const templateLoading = ref(false)
const defaultTemplateId = ref<number | null>(null)

const isDefaultTemplate = (tpl: TemplateInfo) => tpl.id === defaultTemplateId.value

const loadTemplates = async () => {
  templateLoading.value = true
  try {
    const res = await templateApi.getList({ pageNum: 1, pageSize: 100 })
    templateList.value = res.records || []
  } catch {
    ElMessage.error('获取模板列表失败')
  } finally {
    templateLoading.value = false
  }
}

function formatTime(value?: string): string {
  if (!value) return '-'
  try {
    const date = new Date(value)
    if (isNaN(date.getTime())) return value
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  } catch {
    return value
  }
}

const handleCategoryChange = () => handleAutoSelectDefaultTemplate()
const handleTypeChange = () => handleAutoSelectDefaultTemplate()

const handleAutoSelectDefaultTemplate = async () => {
  if (!form.value.projectCategory || !form.value.projectType) return
  try {
    const tpl = await templateApi.getDefault(form.value.projectCategory, form.value.projectType)
    if (tpl?.id) {
      form.value.templateId = tpl.id
      defaultTemplateId.value = tpl.id
      ElMessage.success(`已自动选择默认模板：${tpl.templateName}`)
    }
  } catch {
    // 没有默认模板时不报错
  }
}

const handlePreviewTemplate = (tpl: TemplateInfo) => {
  ElMessage.info(`预览模板：${tpl.templateName}`)
}

// --- 历史匹配 ---
const matchMode = ref<'auto' | 'manual' | 'upload'>('auto')
const matchResults = ref<AiMatchResult[]>([])
const autoMatching = ref(false)
const manualMatching = ref(false)
const manualKeyword = ref('')
const selectedMatchId = ref<number | null>(null)

const handleAutoMatch = async () => {
  if (!form.value.projectName && !form.value.projectDescription) {
    ElMessage.warning('请先填写项目名称或项目描述')
    return
  }
  autoMatching.value = true
  try {
    matchResults.value = await aiApi.matchAuto({
      content: form.value.projectDescription || form.value.projectName,
      projectCategory: form.value.projectCategory,
      projectType: form.value.projectType,
    })
    if (matchResults.value.length) {
      ElMessage.success(`匹配到 ${matchResults.value.length} 条历史需求`)
    } else {
      ElMessage.info('未匹配到相关历史需求')
    }
  } catch {
    ElMessage.error('自动匹配失败')
  } finally {
    autoMatching.value = false
  }
}

const handleManualMatch = async () => {
  if (!manualKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  manualMatching.value = true
  try {
    matchResults.value = await aiApi.matchManual({
      projectCategory: form.value.projectCategory,
      projectType: form.value.projectType,
      keyword: manualKeyword.value.trim(),
    })
    if (matchResults.value.length) {
      ElMessage.success(`搜索到 ${matchResults.value.length} 条结果`)
    } else {
      ElMessage.info('未搜索到相关结果')
    }
  } catch {
    ElMessage.error('手动搜索失败')
  } finally {
    manualMatching.value = false
  }
}

const handlePreviewMatch = (item: AiMatchResult) => {
  ElMessage.info(`预览：${item.requirementName}`)
}

const handleFileChange = () => {
  ElMessage.info('文件已选择')
}

// --- AI推荐 ---
const suggestResults = ref<string[]>([])
const suggesting = ref(false)

const handleSuggest = async () => {
  if (!form.value.projectCategory && !form.value.projectType) {
    ElMessage.warning('请先选择项目类别和类型')
    return
  }
  suggesting.value = true
  try {
    const res = await aiApi.suggest({
      content: form.value.projectDescription || form.value.projectName,
      type: 'template',
      projectId: props.projectId,
    })
    suggestResults.value = res.suggestions || []
  } catch {
    ElMessage.error('AI推荐失败')
  } finally {
    suggesting.value = false
  }
}

// --- 保存 ---
const loadProject = async () => {
  const project = await projectApi.getById(props.projectId)
  Object.assign(form.value, {
    projectName: project.projectName || '',
    projectCategory: project.projectCategory || '',
    projectType: project.projectType || '',
    serviceSubType: project.serviceSubType || '',
    budget: toWanYuan(project.budget) ?? 0,
    reviewType: project.reviewType || 'INTELLIGENT',
    projectDescription: project.projectDescription || '',
    tenderUnit: project.tenderUnit || '',
    contactPerson: project.contactPerson || '',
    contactPhone: project.contactPhone || '',
    templateId: project.templateId || null,
  })
}

const handleSaveAndNext = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  await projectApi.update(props.projectId, {
    ...form.value,
    budget: toYuan(form.value.budget),
    templateId: form.value.templateId ?? undefined,
  })
  ElMessage.success('基础信息保存成功')

  // 推进阶段到"需求生成"，后端会自动触发AI需求生成任务
  try {
    await projectApi.advancePhase(props.projectId, 2)
  } catch (e: any) {
    ElMessage.warning(e?.message || '阶段推进失败，可手动进入下一步')
  }

  emit('next')
}

onMounted(() => {
  loadProject()
  loadTemplates()
})
</script>

<style scoped lang="scss">
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 24px 0 12px;
  padding-left: 10px;
  border-left: 3px solid var(--app-brand-color);

  &:first-child {
    margin-top: 0;
  }
}

// 模板卡片网格
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.template-card {
  background: var(--app-bg-secondary);
  border: 2px solid var(--app-border-light);
  border-radius: var(--app-radius-sm);
  padding: 16px;
  cursor: pointer;
  transition: var(--app-transition-base);

  &:hover {
    border-color: var(--app-brand-color-light-5);
  }

  &.selected {
    border-color: var(--app-brand-color);
    background: var(--app-hover-state);
  }
}

.tpl-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.tpl-name {
  font-weight: 600;
  color: var(--app-text-primary);
  font-size: 14px;
}

.tpl-desc {
  font-size: 13px;
  color: var(--app-text-tertiary);
  margin: 0 0 8px;
  line-height: 1.4;
}

.tpl-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--app-text-tertiary);
  margin-bottom: 4px;
}

// 匹配区
.match-section {
  padding: 0;
}

.match-mode-group {
  margin-bottom: 16px;
}

.match-content {
  margin-top: 12px;
}

.match-desc {
  color: var(--app-text-secondary);
  font-size: 14px;
  margin-bottom: 12px;
}

.manual-search-bar {
  max-width: 400px;
  margin-bottom: 12px;
}

.match-file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.match-file-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--app-bg-secondary);
  border: 2px solid var(--app-border-light);
  border-radius: 6px;
  cursor: pointer;
  transition: var(--app-transition-base);

  &:hover {
    border-color: var(--app-brand-color-light-5);
  }

  &.selected {
    border-color: var(--app-brand-color);
    background: var(--app-hover-state);
  }
}

.match-file-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.match-file-name {
  font-weight: 500;
  color: var(--app-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-file-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.upload-area {
  width: 100%;

  :deep(.el-upload-dragger) {
    background: var(--app-bg-secondary);
    border-color: var(--app-border-medium);
  }
}

.upload-tip {
  color: var(--app-text-tertiary);
  font-size: 12px;
  margin-top: 4px;
}

.suggest-section {
  margin-top: 16px;

  h4 {
    margin-bottom: 8px;
    color: var(--app-text-secondary);
  }
}

.suggest-tag {
  margin: 0 8px 8px 0;
}

.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
