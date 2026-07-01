<template>
  <div class="page-shell template-management">
    <div class="page-header-row">
      <div>
        <div class="page-title">模板管理</div>
        <div class="page-subtitle">管理招标文件模板，上传Word模板文件</div>
      </div>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="search-wrap">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="模板名称">
            <el-input v-model="queryParams.templateName" placeholder="请输入模板名称" clearable />
          </el-form-item>
          <el-form-item label="类别">
            <el-select v-model="queryParams.projectCategory" placeholder="请选择类别" clearable>
              <el-option label="小额交易" value="SMALL_TRADE" />
              <el-option label="政府采购" value="GOVERNMENT_PROCUREMENT" />
              <el-option label="综合交易" value="COMPREHENSIVE_TRADE" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
              <el-option label="启用" value="ENABLED" />
              <el-option label="禁用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="toolbar-wrap">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增模板
        </el-button>
        <el-button
          type="danger"
          :disabled="selectedIds.length === 0"
          @click="handleBatchDelete"
        >
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="40" />
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="templateName" label="模板名称" min-width="180" />
        <el-table-column prop="projectCategory" label="类别" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.projectCategory === 'SMALL_TRADE'" type="info">小额交易</el-tag>
            <el-tag v-else-if="row.projectCategory === 'COMPREHENSIVE_TRADE'" type="warning">综合交易</el-tag>
            <el-tag v-else-if="row.projectCategory === 'GOVERNMENT_PROCUREMENT'" type="success">政府采购</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模板文件" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.fileId" type="success" size="small">已上传</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="isDefault" label="默认" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" type="success" size="small">默认</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'ENABLED'"
              @change="(val: boolean) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createName" label="创建人" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.isDefault !== 1"
              link
              type="warning"
              @click="handleSetDefault(row)"
            >
              设为默认
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="formData.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板类别" prop="projectCategory">
          <el-select v-model="formData.projectCategory" placeholder="请选择模板类别" style="width: 100%">
            <el-option label="小额交易" value="SMALL_TRADE" />
            <el-option label="政府采购" value="GOVERNMENT_PROCUREMENT" />
            <el-option label="综合交易" value="COMPREHENSIVE_TRADE" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目类型" prop="projectType">
          <el-select v-model="formData.projectType" placeholder="请选择项目类型" style="width: 100%">
            <el-option label="工程" value="ENGINEERING" />
            <el-option label="货物" value="GOODS" />
            <el-option label="服务" value="SERVICE" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板文件" prop="fileId">
          <el-upload
            :action="''"
            :auto-upload="false"
            :limit="1"
            accept=".docx"
            :on-change="handleFileChange"
            :file-list="fileList"
            :on-remove="handleFileRemove"
          >
            <el-button type="primary">选择Word文件</el-button>
            <template #tip>
              <div class="el-upload__tip">仅支持 .docx 格式</div>
            </template>
            <template #file="{ file }">
              <div class="template-file-row">
                <div class="template-file-info">
                  <el-icon><Document /></el-icon>
                  <span class="template-file-name">{{ file.name }}</span>
                  <el-icon v-if="formData.fileId || file.status === 'success'" class="template-file-success">
                    <CircleCheck />
                  </el-icon>
                </div>
                <div class="template-file-row-actions">
                  <el-button v-if="formData.fileId" type="primary" link @click.stop="handlePreviewTemplate">
                    <el-icon><View /></el-icon>
                    预览Word
                  </el-button>
                  <el-button
                    v-if="formData.fileId"
                    type="primary"
                    link
                    :loading="templateDownloadLoading"
                    @click.stop="handleDownloadTemplate"
                  >
                    <el-icon><Download /></el-icon>
                    下载模板
                  </el-button>
                  <el-button link class="template-file-remove" @click.stop="handleFileRemove">
                    <el-icon><Close /></el-icon>
                  </el-button>
                </div>
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="用途说明">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="3"
            placeholder="请输入模板用途说明"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
        <!-- 评审项配置 -->
        <el-divider content-position="left">评审项配置</el-divider>
        <div class="review-score-mode">
          <span class="review-score-mode__label">评分模式：</span>
          <el-radio-group v-model="formData.reviewConfig.scoreMode" size="small">
            <el-radio value="SCORE">分值模式（各类分值直接计入总分）</el-radio>
            <el-radio value="WEIGHT">权重模式（每类满分100，类型间按权重%分配，合计100%）</el-radio>
          </el-radio-group>
        </div>
        <el-table :data="formData.reviewConfig.reviewTypes" border size="small">
          <el-table-column label="评审类型" prop="reviewType" width="150">
            <template #default="{ row }">
              {{ REVIEW_TYPE_LABELS[row.reviewType] || row.reviewType }}
            </template>
          </el-table-column>
          <el-table-column label="启用" width="80" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" size="small" @change="(val: boolean) => { if (!val) { row.generateStandard = true; row.distinguishSubjectivity = true } }" />
            </template>
          </el-table-column>
          <el-table-column label="生成评审标准" width="120" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.generateStandard" size="small" :disabled="!row.enabled" />
            </template>
          </el-table-column>
          <el-table-column label="手动评审项" width="130" align="center">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                :disabled="!row.enabled || row.generateStandard"
                @click="openManualReviewDialog(row)"
              >
                配置{{ countManualItems(row.manualItems || []) ? `(${countManualItems(row.manualItems || [])})` : '' }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="区分客观主观" width="120" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.distinguishSubjectivity" size="small" :disabled="!row.enabled" />
            </template>
          </el-table-column>
          <el-table-column label="说明">
            <template #default="{ row }">
              <span v-if="!row.enabled" style="color: #909399">该类型不参与评审</span>
              <span v-else-if="!row.generateStandard && countManualItems(row.manualItems || [])" style="color: #67C23A">使用手动配置的评审项</span>
              <span v-else-if="!row.generateStandard" style="color: #E6A23C">item_name 将填充"详见评审文件"</span>
              <span v-else-if="!row.distinguishSubjectivity" style="color: #909399">AI 生成详细评审项内容（不区分主客观）</span>
              <span v-else style="color: #909399">AI 生成详细评审项内容（区分主客观）</span>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="manualDialogVisible"
      title="手动评审项配置"
      width="1100px"
      append-to-body
      destroy-on-close
    >
      <div class="manual-review-dialog">
        <div class="manual-review-toolbar">
          <el-button type="primary" @click="handleAddManualItem">
            <el-icon><Plus /></el-icon>
            添加评审项
          </el-button>
        </div>
        <el-table
          :data="activeManualItems"
          row-key="id"
          :tree-props="{ children: 'children' }"
          border
          default-expand-all
          class="manual-review-table"
        >
          <el-table-column label="评审项" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.itemName" type="textarea" :rows="2" placeholder="请输入评审项" />
            </template>
          </el-table-column>
          <el-table-column label="评审标准" min-width="360">
            <template #default="{ row }">
              <el-input v-model="row.itemContent" type="textarea" :rows="2" placeholder="请输入评审标准" />
            </template>
          </el-table-column>
          <el-table-column v-if="activeManualConfig?.distinguishSubjectivity" label="主观/客观" width="120" align="center">
            <template #default="{ row }">
              <el-select v-model="row.subjectivity" size="small">
                <el-option value="OBJECTIVE" label="客观" />
                <el-option value="SUBJECTIVE" label="主观" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column v-if="activeManualConfig?.reviewType !== 'COMPLIANCE'" label="分值" width="120" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.score"
                :min="0"
                :max="100"
                :precision="1"
                size="small"
                controls-position="right"
                :disabled="hasManualChildren(row)"
                class="manual-score-input"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <el-button v-if="(row.level || 2) < 3" link type="primary" @click="handleAddManualChild(row)">
                子项
              </el-button>
              <el-button link type="danger" @click="handleDeleteManualItem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button type="primary" @click="handleManualDialogConfirm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="previewVisible"
      title="Word模板预览"
      width="88%"
      top="4vh"
      class="word-preview-dialog"
      destroy-on-close
      append-to-body
    >
      <DocxPreview :file-id="previewFileId" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Delete, View, Document, CircleCheck, Close, Download } from '@element-plus/icons-vue'
import { templateApi, templateFileApi } from '@/api/template'
import { fileApi } from '@/api/file'
import type { TemplateInfo, TemplateQueryParams, TemplateCreateParams, TemplateUpdateParams, ReviewConfig, ReviewTypeConfig, ManualReviewItem } from '@/types/template'
import { REVIEW_TYPE_LABELS, buildDefaultReviewConfig, normalizeReviewConfig, normalizeManualReviewItems } from '@/types/template'
import DocxPreview from '@/components/document/DocxPreview.vue'

const loading = ref(false)
const tableData = ref<TemplateInfo[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])

const queryParams = reactive<TemplateQueryParams>({
  pageNum: 1,
  pageSize: 10,
  templateName: undefined,
  projectCategory: undefined,
  status: undefined,
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const previewVisible = ref(false)
const previewFileId = ref<number | string | null>(null)
const templateDownloadLoading = ref(false)
const formRef = ref<FormInstance>()
const fileList = ref<any[]>([])
const uploadingFile = ref<File | null>(null)
const formData = reactive<Omit<TemplateCreateParams, 'reviewConfig'> & { id?: number; reviewConfig: ReviewConfig }>({
  templateName: '',
  projectCategory: '',
  projectType: '',
  fileId: undefined,
  content: '',
  description: '',
  reviewConfig: buildDefaultReviewConfig(),
})
const manualDialogVisible = ref(false)
const activeManualConfig = ref<ReviewTypeConfig | null>(null)
const activeManualItems = ref<ManualReviewItem[]>([])
let manualItemIdSeed = -1

const formRules = reactive<FormRules>({
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  projectCategory: [{ required: true, message: '请选择模板类别', trigger: 'change' }],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
})

const createManualItem = (level = 2, sortOrder = 0): ManualReviewItem => ({
  id: manualItemIdSeed--,
  level,
  itemName: '',
  itemContent: '',
  sortOrder,
  score: 0,
  subjectivity: 'OBJECTIVE',
  isRequired: 1,
  children: [],
})

const countManualItems = (items: ManualReviewItem[]): number =>
  items.reduce((count, item) => count + 1 + countManualItems(item.children || []), 0)

const SCORING_REVIEW_TYPES = new Set(['TECHNICAL', 'CREDIT', 'COMMERCIAL'])

const isScoringReviewType = (reviewType: string): boolean => SCORING_REVIEW_TYPES.has(reviewType)

const hasManualChildren = (item: ManualReviewItem): boolean => !!item.children?.length

const sumManualLeafScore = (items: ManualReviewItem[] = []): number =>
  items.reduce((sum, item) => {
    if (hasManualChildren(item)) {
      return sum + sumManualLeafScore(item.children || [])
    }
    return sum + Number(item.score || 0)
  }, 0)

const isFullScore = (score: number): boolean => Math.abs(score - 100) < 0.001

const openManualReviewDialog = (row: ReviewTypeConfig) => {
  row.manualItems = normalizeManualReviewItems(row.manualItems || [], 2)
  activeManualConfig.value = row
  activeManualItems.value = row.manualItems
  manualDialogVisible.value = true
}

const handleAddManualItem = () => {
  activeManualItems.value.push(createManualItem(2, activeManualItems.value.length))
}

const handleAddManualChild = (row: ManualReviewItem) => {
  const level = row.level || 2
  if (level >= 3) {
    ElMessage.warning('评审项最多支持3级')
    return
  }
  if (!row.children) row.children = []
  row.score = undefined
  row.children.push(createManualItem(level + 1, row.children.length))
}

const removeManualItemById = (items: ManualReviewItem[], id?: number): boolean => {
  const index = items.findIndex(item => item.id === id)
  if (index >= 0) {
    items.splice(index, 1)
    return true
  }
  return items.some(item => removeManualItemById(item.children || [], id))
}

const handleDeleteManualItem = (row: ManualReviewItem) => {
  removeManualItemById(activeManualItems.value, row.id)
}

const serializeManualItems = (items: ManualReviewItem[] = []): any[] =>
  items.map((item, index) => {
    const children = serializeManualItems(item.children || [])
    return {
      itemName: item.itemName,
      itemContent: item.itemContent,
      sortOrder: item.sortOrder ?? index,
      score: children.length ? undefined : item.score,
      weight: item.weight,
      subjectivity: item.subjectivity,
      isRequired: item.isRequired ?? 1,
      children,
    }
  })

const serializeReviewConfig = (config: ReviewConfig): ReviewConfig => ({
  scoreMode: config.scoreMode,
  reviewTypes: config.reviewTypes.map(typeConfig => ({
    reviewType: typeConfig.reviewType,
    enabled: typeConfig.enabled,
    generateStandard: typeConfig.generateStandard,
    distinguishSubjectivity: typeConfig.distinguishSubjectivity,
    manualItems: serializeManualItems(typeConfig.manualItems || []),
  })),
})

const hasBlankManualItemName = (items: ManualReviewItem[] = []): boolean =>
  items.some(item => !(item.itemName || '').trim() || hasBlankManualItemName(item.children || []))

const validateManualReviewItems = (): boolean => {
  const invalidType = formData.reviewConfig.reviewTypes.find(typeConfig =>
    typeConfig.enabled
    && !typeConfig.generateStandard
    && hasBlankManualItemName(typeConfig.manualItems || []),
  )
  if (!invalidType) {
    return true
  }
  ElMessage.warning(`${REVIEW_TYPE_LABELS[invalidType.reviewType] || invalidType.reviewType}存在未填写的手动评审项名称`)
  return false
}

const validateManualScoreRules = (): boolean => {
  const manualScoringTypes = formData.reviewConfig.reviewTypes.filter(typeConfig =>
    typeConfig.enabled
    && !typeConfig.generateStandard
    && isScoringReviewType(typeConfig.reviewType)
    && countManualItems(typeConfig.manualItems || []) > 0,
  )
  if (!manualScoringTypes.length) {
    return true
  }

  if ((formData.reviewConfig.scoreMode || 'SCORE') === 'WEIGHT') {
    const invalidType = manualScoringTypes.find(typeConfig => !isFullScore(sumManualLeafScore(typeConfig.manualItems || [])))
    if (invalidType) {
      ElMessage.warning(`${REVIEW_TYPE_LABELS[invalidType.reviewType] || invalidType.reviewType}手动评审项叶子分合计必须为100分`)
      return false
    }
    return true
  }

  const manualScore = manualScoringTypes
    .map(typeConfig => sumManualLeafScore(typeConfig.manualItems || []))
    .reduce((sum, score) => sum + score, 0)
  if (manualScore > 100) {
    ElMessage.warning(`手动评审项叶子分合计不能超过100分，当前为${manualScore}分`)
    return false
  }

  const hasAiScoringTypes = formData.reviewConfig.reviewTypes.some(typeConfig =>
    typeConfig.enabled
    && typeConfig.generateStandard
    && isScoringReviewType(typeConfig.reviewType),
  )
  if (!hasAiScoringTypes && !isFullScore(manualScore)) {
    ElMessage.warning(`未启用AI生成评分类型时，手动评审项叶子分合计必须为100分，当前为${manualScore}分`)
    return false
  }
  return true
}

const validateActiveManualScoreRules = (): boolean => {
  const typeConfig = activeManualConfig.value
  if (!typeConfig
    || !typeConfig.enabled
    || typeConfig.generateStandard
    || !isScoringReviewType(typeConfig.reviewType)
    || countManualItems(typeConfig.manualItems || []) === 0) {
    return true
  }

  if ((formData.reviewConfig.scoreMode || 'SCORE') === 'WEIGHT') {
    const leafScore = sumManualLeafScore(typeConfig.manualItems || [])
    if (!isFullScore(leafScore)) {
      ElMessage.warning(`${REVIEW_TYPE_LABELS[typeConfig.reviewType] || typeConfig.reviewType}手动评审项叶子分合计必须为100分`)
      return false
    }
    return true
  }

  return validateManualScoreRules()
}

const validateManualReviewConfig = (): boolean => {
  if (!validateManualReviewItems()) {
    return false
  }
  return validateManualScoreRules()
}

const handleManualDialogConfirm = () => {
  if (!validateActiveManualScoreRules()) {
    return
  }
  manualDialogVisible.value = false
}

const handleFileChange = (uploadFile: any) => {
  uploadingFile.value = uploadFile.raw
  fileList.value = [uploadFile]
  formData.fileId = undefined
}

const handleFileRemove = () => {
  uploadingFile.value = null
  fileList.value = []
  formData.fileId = undefined
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await templateApi.getList(queryParams)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    console.error('获取模板列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchList()
}

const handleReset = () => {
  queryParams.templateName = undefined
  queryParams.projectCategory = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  fetchList()
}

const handleAdd = () => {
  dialogTitle.value = '新增模板'
  formData.reviewConfig = buildDefaultReviewConfig()
  dialogVisible.value = true
}

const handleEdit = (row: TemplateInfo) => {
  dialogTitle.value = '编辑模板'
  Object.assign(formData, {
    id: row.id,
    templateName: row.templateName,
    projectCategory: row.projectCategory,
    projectType: row.projectType,
    fileId: row.fileId,
    content: row.content,
    description: row.description,
  })
  if (row.fileId) {
    fileList.value = [{ name: '已上传模板文件', url: '' }]
  }
  // 解析评审项配置
  if (row.reviewConfig) {
    const parsed = typeof row.reviewConfig === 'string'
      ? JSON.parse(row.reviewConfig)
      : row.reviewConfig
    formData.reviewConfig = normalizeReviewConfig(parsed)
  } else {
    formData.reviewConfig = buildDefaultReviewConfig()
  }
  dialogVisible.value = true
}

const handleDelete = async (row: TemplateInfo) => {
  try {
    await ElMessageBox.confirm(`确定删除模板"${row.templateName}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await templateApi.deleteById(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除模板失败:', error)
    }
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个模板吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await templateApi.deleteByIds(selectedIds.value)
    ElMessage.success('批量删除成功')
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除模板失败:', error)
    }
  }
}

const handleSetDefault = async (row: TemplateInfo) => {
  try {
    await templateApi.setDefault(row.id)
    ElMessage.success('设为默认模板成功')
    fetchList()
  } catch (error) {
    console.error('设为默认模板失败:', error)
  }
}

const handleStatusChange = async (row: TemplateInfo, enabled: boolean) => {
  try {
    await templateApi.setStatus(row.id, enabled ? 'ENABLED' : 'DISABLED')
    ElMessage.success(enabled ? '已启用' : '已禁用')
    fetchList()
  } catch (error) {
    console.error('修改状态失败:', error)
  }
}

const handleSelectionChange = (selection: TemplateInfo[]) => {
  selectedIds.value = selection.map((item) => item.id)
}

const handlePreviewTemplate = () => {
  if (!formData.fileId) {
    ElMessage.warning('请先上传Word模板文件')
    return
  }
  previewFileId.value = formData.fileId
  previewVisible.value = true
}

const getTemplateDownloadFileName = () => {
  const defaultUploadedName = '已上传模板文件'
  const currentFileName = fileList.value[0]?.name
  const rawName = currentFileName && currentFileName !== defaultUploadedName
    ? currentFileName
    : (formData.templateName || defaultUploadedName)
  const safeName = rawName.trim().replace(/[\\/:*?"<>|]/g, '_')
  return /\.docx$/i.test(safeName) ? safeName : `${safeName}.docx`
}

const handleDownloadTemplate = async () => {
  if (!formData.fileId) {
    ElMessage.warning('请先上传Word模板文件')
    return
  }

  templateDownloadLoading.value = true
  try {
    const blob = await fileApi.download(formData.fileId)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = getTemplateDownloadFileName()
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    setTimeout(() => URL.revokeObjectURL(url), 0)
  } catch (error) {
    console.error('下载模板文件失败:', error)
    ElMessage.error('下载模板文件失败')
  } finally {
    templateDownloadLoading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!validateManualReviewConfig()) return

    submitLoading.value = true
    try {
      // 先上传文件
      if (uploadingFile.value) {
        const uploadRes = await templateFileApi.upload(uploadingFile.value, 'template')
        formData.fileId = uploadRes.data.fileId
      }

      if (formData.id) {
        await templateApi.update({
          ...formData,
          reviewConfig: JSON.stringify(serializeReviewConfig(formData.reviewConfig)),
        } as TemplateUpdateParams)
        ElMessage.success('更新成功')
      } else {
        await templateApi.create({
          ...formData,
          reviewConfig: JSON.stringify(serializeReviewConfig(formData.reviewConfig)),
        })
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchList()
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDialogClosed = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: undefined,
    templateName: '',
    projectCategory: '',
    projectType: '',
    fileId: undefined,
    content: '',
    description: '',
    reviewConfig: buildDefaultReviewConfig(),
  })
  fileList.value = []
  uploadingFile.value = null
  manualDialogVisible.value = false
  activeManualConfig.value = null
  activeManualItems.value = []
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/index.scss';

.template-file-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 24px;
}

.template-file-info {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  color: #606266;
}

.template-file-name {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-file-success {
  color: #67c23a;
}

.template-file-row-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.template-file-remove {
  color: #909399;
  padding: 0;
}

.word-preview-dialog :deep(.el-dialog__body) {
  max-height: 82vh;
  overflow: hidden;
  padding: 12px;
  background: #f5f7fa;
}

.review-score-mode {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;

  &__label {
    color: #606266;
    font-size: 14px;
  }
}

.manual-review-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.manual-review-toolbar {
  display: flex;
  justify-content: flex-start;
}

.manual-review-table {
  .manual-score-input {
    width: 96px;
  }
}
</style>
