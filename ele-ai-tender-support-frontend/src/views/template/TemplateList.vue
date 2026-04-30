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
              <el-option label="限额以下" value="LIMITED_BELOW" />
              <el-option label="产权交易" value="PROPERTY_TRADE" />
              <el-option label="政府采购" value="GOVERNMENT_PROCUREMENT" />
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
            <el-tag v-if="row.projectCategory === 'LIMITED_BELOW'" type="info">限额以下</el-tag>
            <el-tag v-else-if="row.projectCategory === 'PROPERTY_TRADE'" type="warning">产权交易</el-tag>
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
            <el-option label="限额以下" value="LIMITED_BELOW" />
            <el-option label="产权交易" value="PROPERTY_TRADE" />
            <el-option label="政府采购" value="GOVERNMENT_PROCUREMENT" />
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
        <el-table :data="formData.reviewConfig.reviewTypes" border size="small">
          <el-table-column label="评审类型" prop="reviewType" width="150">
            <template #default="{ row }">
              {{ REVIEW_TYPE_LABELS[row.reviewType] || row.reviewType }}
            </template>
          </el-table-column>
          <el-table-column label="启用" width="80" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" size="small" @change="(val: boolean) => { if (!val) row.generateStandard = true }" />
            </template>
          </el-table-column>
          <el-table-column label="生成评审标准" width="120" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.generateStandard" size="small" :disabled="!row.enabled" />
            </template>
          </el-table-column>
          <el-table-column label="说明">
            <template #default="{ row }">
              <span v-if="!row.enabled" style="color: #909399">该类型不参与评审</span>
              <span v-else-if="!row.generateStandard" style="color: #E6A23C">item_name 将填充"详见评审文件"</span>
              <span v-else style="color: #909399">AI 生成详细评审项内容</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import { templateApi, templateFileApi } from '@/api/template'
import type { TemplateInfo, TemplateQueryParams, TemplateCreateParams, TemplateUpdateParams, ReviewConfig } from '@/types/template'
import { REVIEW_TYPE_LABELS, buildDefaultReviewConfig } from '@/types/template'

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

const formRules = reactive<FormRules>({
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  projectCategory: [{ required: true, message: '请选择模板类别', trigger: 'change' }],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
})

const handleFileChange = (uploadFile: any) => {
  uploadingFile.value = uploadFile.raw
  fileList.value = [uploadFile]
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
    formData.reviewConfig = typeof row.reviewConfig === 'string'
      ? JSON.parse(row.reviewConfig)
      : row.reviewConfig
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

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

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
          reviewConfig: JSON.stringify(formData.reviewConfig),
        } as TemplateUpdateParams)
        ElMessage.success('更新成功')
      } else {
        await templateApi.create({
          ...formData,
          reviewConfig: JSON.stringify(formData.reviewConfig),
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
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/index.scss';
</style>
