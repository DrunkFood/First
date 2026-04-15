<template>
  <div class="phase-basic-info">
    <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
      <el-form-item label="项目名称" prop="projectName">
        <el-input v-model="form.projectName" placeholder="请输入项目名称" />
      </el-form-item>
      <el-form-item label="项目类别" prop="projectCategory">
        <el-select v-model="form.projectCategory" placeholder="请选择" @change="handleCategoryChange">
          <el-option label="限额以下" value="LIMITED_BELOW" />
          <el-option label="产权交易" value="PROPERTY_TRADE" />
          <el-option label="政府采购" value="GOVERNMENT_PROCUREMENT" />
        </el-select>
      </el-form-item>
      <el-form-item label="项目类型" prop="projectType">
        <el-select v-model="form.projectType" placeholder="请选择" @change="handleTypeChange">
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

      <!-- 模板选择 -->
      <el-divider content-position="left">模板选择</el-divider>
      <el-form-item label="选择模板">
        <el-select
          v-model="form.templateId"
          placeholder="请选择模板"
          clearable
          filterable
          :loading="templateLoading"
          style="width: 100%"
        >
          <el-option
            v-for="tpl in templateList"
            :key="tpl.id"
            :label="tpl.templateName"
            :value="tpl.id"
          >
            <span>{{ tpl.templateName }}</span>
            <el-tag size="small" style="margin-left: 8px">{{ tpl.projectCategory }}</el-tag>
          </el-option>
        </el-select>
      </el-form-item>
    </el-form>

    <!-- 历史招标文件匹配 -->
    <el-divider content-position="left">历史招标文件匹配</el-divider>
    <div class="match-section">
      <div class="match-toolbar">
        <el-button type="primary" :loading="autoMatching" @click="handleAutoMatch">
          自动匹配
        </el-button>
        <el-button type="success" :loading="suggesting" @click="handleSuggest">
          AI推荐
        </el-button>
        <div class="manual-search">
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
      </div>

      <el-table
        v-if="matchResults.length"
        :data="matchResults"
        stripe
        highlight-current-row
        @current-change="handleSelectMatch"
        class="match-table"
      >
        <el-table-column prop="requirementName" label="需求名称" />
        <el-table-column prop="similarity" label="相似度" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round(row.similarity * 100)"
              :stroke-width="12"
              :format="() => Math.round(row.similarity * 100) + '%'"
            />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容摘要" show-overflow-tooltip />
      </el-table>
      <el-empty v-else description="暂无匹配结果，请点击自动匹配或手动搜索" :image-size="60" />

      <!-- AI推荐结果 -->
      <div v-if="suggestResults.length" class="suggest-section">
        <h4>AI推荐</h4>
        <el-tag
          v-for="(s, idx) in suggestResults"
          :key="idx"
          class="suggest-tag"
          effect="plain"
        >
          {{ s }}
        </el-tag>
      </div>
    </div>

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

/** 类别/类型变更时自动选择默认模板 */
const handleCategoryChange = () => handleAutoSelectDefaultTemplate()
const handleTypeChange = () => handleAutoSelectDefaultTemplate()

const handleAutoSelectDefaultTemplate = async () => {
  if (!form.value.projectCategory || !form.value.projectType) return
  try {
    const tpl = await templateApi.getDefault(form.value.projectCategory, form.value.projectType)
    if (tpl?.id) {
      form.value.templateId = tpl.id
      ElMessage.success(`已自动选择默认模板：${tpl.templateName}`)
    }
  } catch {
    // 没有默认模板时不报错
  }
}

// --- 历史匹配 ---
const matchResults = ref<AiMatchResult[]>([])
const autoMatching = ref(false)
const manualMatching = ref(false)
const manualKeyword = ref('')
const selectedMatch = ref<AiMatchResult | null>(null)

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

const handleSelectMatch = (row: AiMatchResult | null) => {
  selectedMatch.value = row
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
    if (suggestResults.value.length) {
      ElMessage.success('AI推荐已生成')
    } else {
      ElMessage.info('暂无推荐')
    }
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
  emit('next')
}

onMounted(() => {
  loadProject()
  loadTemplates()
})
</script>

<style scoped>
.phase-basic-info {
  max-width: 800px;
}

.match-section {
  padding: 0 0 0 120px;
}

.match-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.manual-search {
  flex: 1;
  max-width: 360px;
}

.match-table {
  margin-bottom: 16px;
}

.suggest-section h4 {
  margin-bottom: 8px;
  color: var(--el-text-color-regular);
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
