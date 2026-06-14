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
      <!-- 只读模式：展示已选匹配信息 -->
      <template v-if="readonly">
        <div class="match-readonly-info">
          <span class="match-readonly-label">匹配模式：</span>
          <span class="match-readonly-value">{{ matchModeLabel }}</span>
        </div>
      </template>
      <!-- 编辑模式：MatchModePanel 交互 -->
      <MatchModePanel
        v-else
        v-model="matchMode"
        v-model:selected-file-id="selectedMatchId"
        v-model:uploaded-file-id="uploadedFileId"
        :match-files="matchFiles"
        mode="create"
        @file-preview="handlePreviewMatch"
      />
    </div>

    <!-- 底部操作 -->
    <div class="phase-actions">
      <el-button v-if="!readonly" type="primary" @click="handleSaveAndNext">保存并继续</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { projectApi } from '@/api/project'
import { templateApi } from '@/api/template'
import { projectTemplateApi } from '@/api/projectTemplate'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Calendar, View } from '@element-plus/icons-vue'
import { toWanYuan, toYuan } from '@/utils/budget'
import { PROJECT_CATEGORY_MAP, PROJECT_TYPE_MAP } from '@/constants/status-maps'
import DocxPreview from '@/components/document/DocxPreview.vue'
import { aiApi } from '@/api/ai'
import MatchModePanel from '@/components/requirement/MatchModePanel.vue'
import type { TemplateInfo, WordStructure } from '@/types/template'
import type { MatchFile } from '@/types/requirement'
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
const matchMode = ref<string>('AUTO_MATCH')
const selectedMatchId = ref<number | undefined>(undefined)
const uploadedFileId = ref<number | undefined>(undefined)
const matchFiles = ref<MatchFile[]>([])
const matchLoading = ref(false)

/** 将 AiMatchResult 转为 MatchFile 格式，供 MatchModePanel 使用 */
const toMatchFile = (r: AiMatchResult): MatchFile => ({
  id: r.requirementId,
  fileName: r.requirementName,
  fileType: form.value.projectType || '工程类',
  matchPercent: Math.round(r.similarity * 100),
  matchDesc: r.content?.substring(0, 200),
})

/** 根据匹配模式调用 AI 接口获取候选列表 */
const fetchMatchResults = async () => {
  if (!form.value.projectCategory || !form.value.projectType) {
    ElMessage.warning('请先选择项目类别和项目类型')
    return
  }
  matchLoading.value = true
  try {
    let results: AiMatchResult[] = []
    if (matchMode.value === 'AUTO_MATCH') {
      results = await aiApi.matchAuto({
        projectCategory: form.value.projectCategory,
        projectType: form.value.projectType,
        content: form.value.projectDescription || form.value.projectName || '',
      })
      // 自动匹配时，默认选中第一个
      if (results.length > 0) {
        selectedMatchId.value = results[0].requirementId
      }
    } else if (matchMode.value === 'MANUAL_SELECT') {
      results = await aiApi.matchManual({
        projectCategory: form.value.projectCategory,
        projectType: form.value.projectType,
      })
    }
    matchFiles.value = results.map(toMatchFile)
  } catch {
    matchFiles.value = []
    ElMessage.error('匹配失败，请稍后重试')
  } finally {
    matchLoading.value = false
  }
}

/** 监听匹配模式变化，切换时重新获取候选 */
watch(matchMode, () => {
  selectedMatchId.value = undefined
  matchFiles.value = []
  if (matchMode.value === 'AUTO_MATCH' || matchMode.value === 'MANUAL_SELECT') {
    fetchMatchResults()
  }
})

/** 预览匹配文件 */
const handlePreviewMatch = (file: MatchFile) => {
  ElMessage.info(`预览：${file.fileName}`)
}

/** 只读模式下匹配模式显示文本 */
const MATCH_MODE_LABELS: Record<string, string> = {
  AUTO_MATCH: '系统自动匹配',
  MANUAL_SELECT: '手动选择',
  UPLOAD: '上传',
}
const matchModeLabel = computed(() => MATCH_MODE_LABELS[matchMode.value] || '未选择')

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

  // 回填匹配字段
  matchMode.value = project.matchMode || 'AUTO_MATCH'
  selectedMatchId.value = project.matchedFileId || undefined
  uploadedFileId.value = project.uploadedFileId || undefined

  // 非只读模式下，加载后触发一次匹配获取候选列表
  if (!props.readonly && (project.matchMode === 'AUTO_MATCH' || project.matchMode === 'MANUAL_SELECT')) {
    fetchMatchResults()
  }
}

const handleSaveAndNext = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const { templateId, ...updateData } = form.value
  // 计算匹配相似度（从候选列表中查找选中文件的 matchPercent）
  const matchedFile = matchFiles.value.find(f => f.id === selectedMatchId.value)
  await projectApi.update(props.projectId, {
    ...updateData,
    budget: toYuan(form.value.budget),
    matchMode: matchMode.value,
    matchedFileId: selectedMatchId.value,
    matchedSimilarity: matchedFile?.matchPercent ?? undefined,
    uploadedFileId: uploadedFileId.value,
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

.match-readonly-info {
  padding: 16px;
  background: var(--app-bg-tertiary, var(--app-bg-secondary));
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.6;
}

.match-readonly-label {
  color: var(--app-text-tertiary);
}

.match-readonly-value {
  color: var(--app-text-primary);
  font-weight: 500;
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
