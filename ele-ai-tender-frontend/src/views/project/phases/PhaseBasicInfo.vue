<template>
  <div class="phase-basic-info">
    <!-- 项目基本信息 -->
    <div class="section-title">项目基本信息</div>
    <el-form :model="form" label-width="120px" :rules="rules" ref="formRef" :disabled="readonly">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="项目名称" prop="projectName">
            <el-input v-model="form.projectName" placeholder="请输入项目名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="项目类别" prop="projectCategory">
            <el-select v-model="form.projectCategory" placeholder="请选择" style="width: 100%" @change="handleCategoryChange">
              <el-option v-for="(item, key) in PROJECT_CATEGORY_MAP" :key="key" :label="item.label" :value="key" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="项目类型" prop="projectType">
            <el-select v-model="form.projectType" placeholder="请选择" style="width: 100%" @change="handleTypeChange">
              <el-option v-for="(item, key) in PROJECT_TYPE_MAP" :key="key" :label="item.label" :value="key" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="预算金额(万元)" prop="budget">
            <el-input-number v-model="form.budget" :min="0" :precision="6" :step="1" placeholder="请输入正数，保留6位小数" style="width: 100%" />
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
    <div class="tpl-hint">新建项目选择模板，引用项目显示当前项目的招标文件</div>
    <div class="template-grid" v-loading="templateLoading">
      <div
        v-for="tpl in displayTemplateList"
        :key="tpl.id"
        class="template-card"
        :class="{ selected: form.templateId === tpl.id, readonly: readonly }"
        @click="!readonly && (form.templateId = tpl.id)"
      >
        <div class="tpl-card-header">
          <span class="tpl-name">{{ tpl.templateName }}</span>
          <span v-if="isDefaultTemplate(tpl)" class="tpl-badge">推荐</span>
        </div>
        <div class="tpl-card-body">
          <!-- 用途说明 -->
          <div v-if="tpl.content" class="tpl-desc">{{ tpl.content }}</div>
<!-- 占位符标签 -->
          <div v-if="parseStructure(tpl.structureDefinition)?.placeholders?.length" class="tpl-placeholders">
            <span class="placeholder-tag" v-for="p in parseStructure(tpl.structureDefinition)!.placeholders.slice(0, 5)" :key="p">
              {{ p }}
            </span>
            <span v-if="parseStructure(tpl.structureDefinition)!.placeholders.length > 5" class="placeholder-more">
              +{{ parseStructure(tpl.structureDefinition)!.placeholders.length - 5 }}
            </span>
          </div>
        </div>
        <div class="tpl-meta">
          <span class="tpl-meta-item">
            <el-icon :size="12"><Calendar /></el-icon>
            {{ formatTime(tpl.modifyTime || tpl.createTime) }} 更新
          </span>
        </div>
        <div class="tpl-card-footer">
          <el-button size="small" @click.stop="handlePreviewTemplate(tpl)">
            <el-icon><View /></el-icon>预览
          </el-button>
        </div>
      </div>
      <el-empty v-if="!templateLoading && !displayTemplateList.length" description="暂无可用模板" :image-size="60" />
    </div>

    <!-- 模板详情预览弹窗 -->
    <el-dialog
      v-model="previewDialogVisible"
      :title="previewTemplate?.templateName || '模板预览'"
      width="80%"
      top="5vh"
      destroy-on-close
      class="template-preview-dialog"
    >
      <div class="template-preview-content">
        <!-- 用途说明 -->
        <div v-if="previewTemplate?.content" class="preview-desc">{{ previewTemplate.content }}</div>
        <!-- Word文档预览 -->
        <DocxPreview v-if="previewTemplate?.fileId" :file-id="previewTemplate.fileId" />
        <el-empty v-else-if="!previewTemplate?.content" description="该模板暂无预览内容" />
        <!-- 占位符标签 -->
        <div v-if="parseStructure(previewTemplate?.structureDefinition)?.placeholders?.length" class="preview-placeholders">
          <div class="structure-title">填充字段</div>
          <div class="placeholder-tags">
            <span class="placeholder-tag" v-for="p in parseStructure(previewTemplate!.structureDefinition)!.placeholders" :key="p">
              {{ p }}
            </span>
          </div>
        </div>
        <!-- 书签 -->
        <div v-if="parseStructure(previewTemplate?.structureDefinition)?.bookmarks?.length" class="preview-bookmarks">
          <div class="structure-title">书签</div>
          <div class="bookmark-tags">
            <span class="bookmark-tag" v-for="b in parseStructure(previewTemplate!.structureDefinition)!.bookmarks" :key="b">
              {{ b }}
            </span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 历史招标文件匹配 -->
    <div class="section-title">历史招标文件匹配</div>
    <div class="match-section" :class="{ 'match-readonly': readonly }">
      <div class="match-mode-label">匹配模式<span class="required-star">*</span></div>
      <el-radio-group v-model="matchMode" class="match-radio-group" :disabled="readonly">
        <el-radio value="auto">1. 系统自动匹配</el-radio>
        <el-radio value="manual">2. 手动选择</el-radio>
        <el-radio value="upload">3. 上传</el-radio>
      </el-radio-group>

      <!-- 自动匹配模式 -->
      <div v-if="matchMode === 'auto'" class="match-content">
        <div class="auto-match-box">
          <div class="auto-match-title">系统自动匹配</div>
          <div class="auto-match-desc">系统将根据项目信息自动匹配历史招标文件，生成招标需求。</div>
        </div>
      </div>

      <!-- 手动选择模式 -->
      <div v-if="matchMode === 'manual'" class="match-content">
        <div class="manual-title">选择历史招标文件</div>
        <div v-if="matchResults.length" class="match-file-list">
          <div
            v-for="item in matchResults"
            :key="item.requirementId"
            class="match-file-card"
            :class="{ selected: selectedMatchId === item.requirementId }"
            @click="!readonly && (selectedMatchId = item.requirementId)"
          >
            <div class="match-file-header">
              <span class="match-file-name">{{ item.requirementName }}</span>
              <span class="match-file-type">工程类</span>
            </div>
            <div class="match-file-similarity">
              匹配度：{{ Math.round(item.similarity * 100) }}% - 与当前需求相似度较高
            </div>
            <div class="match-file-actions">
              <el-button size="small" @click.stop="handlePreviewMatch(item)">预览</el-button>
              <el-button
                size="small"
                :type="selectedMatchId === item.requirementId ? 'primary' : ''"
                @click.stop="selectedMatchId = item.requirementId"
              >选择</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无匹配结果" :image-size="60" />
        <div class="manual-hint">展示历史文件库中匹配度高的文件，仅可单选</div>
      </div>

      <!-- 上传模式 -->
      <div v-if="matchMode === 'upload'" class="match-content">
        <div class="upload-title">上传招标文件</div>
        <el-upload
          drag
          :auto-upload="false"
          accept=".doc,.docx"
          :limit="1"
          :on-change="handleFileChange"
          class="upload-area"
        >
          <el-icon size="48"><UploadFilled /></el-icon>
          <div>点击或拖拽文件到此处上传</div>
          <template #tip>
            <div class="upload-tip">支持word格式，文件大小不超过50M，上传1份文件</div>
          </template>
        </el-upload>
      </div>
    </div>

    <!-- 底部操作 -->
    <div class="phase-actions">
      <el-button v-if="!readonly" type="primary" @click="handleSaveAndNext">保存并继续</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { projectApi } from '@/api/project'
import { templateApi } from '@/api/template'
import { projectTemplateApi } from '@/api/projectTemplate'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { UploadFilled, Calendar, View } from '@element-plus/icons-vue'
import { toWanYuan, toYuan } from '@/utils/budget'
import { PROJECT_CATEGORY_MAP, PROJECT_TYPE_MAP } from '@/constants/status-maps'
import DocxPreview from '@/components/document/DocxPreview.vue'
import type { TemplateInfo, WordStructure } from '@/types/template'
import type { AiMatchResult } from '@/types/ai'

const props = defineProps<{ projectId: number; readonly?: boolean }>()
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
  projectName: [
    { required: true, message: '请输入项目名称', trigger: 'blur' },
    {
      asyncValidator: async (_rule: any, value: string, callback: any) => {
        if (!value || !value.trim()) return callback()
        try {
          const isUnique = await projectApi.checkName(value.trim(), props.projectId || undefined)
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
  budget: [
    { required: true, message: '请输入预算金额', trigger: 'blur' },
    { type: 'number', validator: (_rule: any, value: any, callback: any) => {
      if (value !== undefined && value !== null && value <= 0) {
        callback(new Error('预算金额必须大于0'))
      } else {
        callback()
      }
    }, trigger: 'blur' },
  ],
  reviewType: [{ required: true, message: '请选择评审方式', trigger: 'change' }],
}

// --- 模板 ---
const templateList = ref<TemplateInfo[]>([])
const templateLoading = ref(false)
const defaultTemplateId = ref<number | null>(null)

const isDefaultTemplate = (tpl: TemplateInfo) => tpl.id === defaultTemplateId.value

/** 项目模板快照（readonly时从tb_project_template查询） */
const projectTemplateSnapshot = ref<TemplateInfo | null>(null)

/** 展示用模板列表：编辑模式=全量，只读模式=项目绑定模板 */
const displayTemplateList = computed<TemplateInfo[]>(() => {
  if (!props.readonly) return templateList.value
  return projectTemplateSnapshot.value ? [projectTemplateSnapshot.value] : []
})

/** 只读模式下加载项目模板快照 */
const loadProjectTemplate = async () => {
  try {
    const pt = await projectTemplateApi.getByProject(props.projectId)
    if (pt) {
      projectTemplateSnapshot.value = {
        id: pt.id,
        templateName: pt.templateName,
        projectCategory: pt.projectCategory,
        projectType: pt.projectType,
        fileId: pt.fileId,
        content: pt.content,
        structureDefinition: pt.structureDefinition as any,
        versionNo: pt.versionNo,
        isDefault: 0,
        status: 'PUBLISHED',
        createTime: pt.createTime,
        modifyTime: pt.modifyTime,
      }
    }
  } catch {
    // 查询失败不报错，展示为空
  }
}

/** 解析 structureDefinition（可能是JSON字符串或已解析对象） */
const parseStructure = (sd: WordStructure | string | undefined): WordStructure | null => {
  if (!sd) return null
  if (typeof sd === 'string') {
    try {
      return JSON.parse(sd)
    } catch {
      return null
    }
  }
  return sd
}

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

const previewDialogVisible = ref(false)
const previewTemplate = ref<TemplateInfo | null>(null)

const handlePreviewTemplate = (tpl: TemplateInfo) => {
  previewTemplate.value = tpl
  previewDialogVisible.value = true
}

// --- 历史匹配 ---
const matchMode = ref<'auto' | 'manual' | 'upload'>('auto')
const matchResults = ref<AiMatchResult[]>([])
const selectedMatchId = ref<number | null>(null)

const handlePreviewMatch = (item: AiMatchResult) => {
  ElMessage.info(`预览：${item.requirementName}`)
}

const handleFileChange = () => {
  ElMessage.info('文件已选择')
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
    reviewType: project.reviewType ?? 'INTELLIGENT',
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

  const { templateId, ...updateData } = form.value
  await projectApi.update(props.projectId, {
    ...updateData,
    budget: toYuan(form.value.budget),
  })

  // 绑定模板到项目模板表
  if (form.value.templateId) {
    await projectTemplateApi.bind(props.projectId, form.value.templateId)
  }

  ElMessage.success('基础信息保存成功')

  // 推进阶段到"需求生成"，后端会自动触发AI需求生成任务
  try {
    await projectApi.advancePhase(props.projectId, 2)
    emit('next')
  } catch (e: any) {
    ElMessage.warning(e?.message || '阶段推进失败，请稍后重试')
  }
}

onMounted(() => {
  loadProject()
  loadTemplates()
  loadProjectTemplate()
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

// 模板提示文字
.tpl-hint {
  font-size: 13px;
  color: var(--app-text-tertiary);
  margin-bottom: 16px;
}

// 模板卡片网格
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.template-card {
  background: var(--app-bg-secondary);
  border: 2px solid var(--app-border-light);
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: var(--app-transition-base);

  &:hover {
    border-color: var(--app-brand-color);
    background: var(--app-hover-state);
  }

  &.selected {
    border-color: var(--app-brand-color);
    background: var(--app-hover-state);
  }

  &.readonly {
    cursor: default;

    &:hover {
      border-color: var(--app-border-light);
    }
  }
}

.tpl-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.tpl-name {
  font-weight: 600;
  color: var(--app-text-primary);
  font-size: 14px;
}

.tpl-badge {
  padding: 2px 8px;
  background: rgba(51, 108, 255, 0.15);
  color: var(--app-brand-color);
  border-radius: 4px;
  font-size: 11px;
  flex-shrink: 0;
}

.tpl-desc {
  font-size: 12px;
  color: var(--app-text-secondary);
  margin: 0 0 8px;
  line-height: 1.5;
}

// 模板卡片主体
.tpl-card-body {
  margin-bottom: 8px;
}

// 占位符标签
.tpl-placeholders {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}

.placeholder-tag {
  display: inline-block;
  padding: 1px 6px;
  background: rgba(51, 108, 255, 0.08);
  color: var(--app-brand-color);
  border-radius: 3px;
  font-size: 11px;
  line-height: 1.6;
}

.placeholder-more {
  display: inline-block;
  padding: 1px 6px;
  background: var(--app-bg-tertiary, var(--app-bg-secondary));
  color: var(--app-text-tertiary);
  border-radius: 3px;
  font-size: 11px;
  line-height: 1.6;
}

.tpl-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--app-text-tertiary);
}

.tpl-meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.tpl-card-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

// 匹配区
.match-section {
  padding: 0;
}

.match-mode-label {
  font-size: 13px;
  color: var(--app-text-secondary);
  margin-bottom: 12px;
}

.required-star {
  color: var(--el-color-danger);
  margin-left: 2px;
}

.match-radio-group {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
}

.match-content {
  margin-top: 0;
}

// 自动匹配
.auto-match-box {
  padding: 16px;
  background: var(--app-bg-tertiary, var(--app-bg-secondary));
  border-radius: 6px;
}

.auto-match-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin-bottom: 8px;
}

.auto-match-desc {
  font-size: 13px;
  color: var(--app-text-secondary);
  line-height: 1.5;
}

// 手动选择
.manual-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin-bottom: 16px;
}

.manual-hint {
  font-size: 12px;
  color: var(--app-text-tertiary);
  margin-top: 12px;
}

.match-file-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.match-file-card {
  padding: 16px;
  background: var(--app-bg-tertiary, var(--app-bg-secondary));
  border: 1px solid var(--app-border-light);
  border-radius: 8px;
  cursor: pointer;
  transition: var(--app-transition-base);

  &:hover {
    border-color: var(--app-brand-color-light-5);
  }

  &.selected {
    border-color: var(--app-brand-color);
    background: rgba(51, 108, 255, 0.05);
  }
}

.match-file-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.match-file-name {
  font-weight: 600;
  font-size: 13px;
  color: var(--app-text-primary);
}

.match-file-type {
  background: rgba(51, 108, 255, 0.15);
  color: var(--app-brand-color);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  flex-shrink: 0;
}

.match-file-similarity {
  font-size: 12px;
  color: var(--app-text-secondary);
  margin: 8px 0;
}

.match-file-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

// 上传
.upload-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin-bottom: 16px;
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

.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

// 模板预览弹窗
.template-preview-content {
  max-height: 70vh;
  overflow-y: auto;

  .preview-desc {
    font-size: 14px;
    color: var(--app-text-secondary);
    line-height: 1.6;
    margin-bottom: 20px;
  }

  .preview-placeholders,
  .preview-bookmarks {
    margin-bottom: 20px;

    .structure-title {
      font-size: 14px;
      font-weight: 600;
      color: var(--app-text-primary);
      margin-bottom: 8px;
    }
  }

  .placeholder-tags,
  .bookmark-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .placeholder-tag {
    display: inline-block;
    padding: 2px 8px;
    background: rgba(51, 108, 255, 0.08);
    color: var(--app-brand-color);
    border-radius: 4px;
    font-size: 12px;
    line-height: 1.6;
  }

  .bookmark-tag {
    display: inline-block;
    padding: 2px 8px;
    background: rgba(103, 194, 58, 0.08);
    color: var(--el-color-success);
    border-radius: 4px;
    font-size: 12px;
    line-height: 1.6;
  }
}
</style>
