<template>
  <div class="policy-file-list">
    <el-page-header content="政策文件管理" />

    <!-- 搜索筛选区 -->
    <el-form :inline="true" class="search-form" @submit.prevent="handleSearch">
      <el-form-item label="文件名称">
        <el-input v-model="searchForm.fileName" placeholder="请输入文件名称" clearable />
      </el-form-item>
      <el-form-item label="文件分类">
        <el-select v-model="searchForm.fileCategory" placeholder="请选择分类" clearable>
          <el-option label="法律法规" value="法律法规" />
          <el-option label="规章制度" value="规章制度" />
          <el-option label="政策文件" value="政策文件" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源">
        <el-select v-model="searchForm.source" placeholder="请选择来源" clearable>
          <el-option label="平台" value="SYSTEM" />
          <el-option label="用户上传" value="USER" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="showCreateDialog = true">上传政策文件</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="fileName" label="文件名" />
      <el-table-column prop="fileCategory" label="文件分类" width="120" />
      <el-table-column prop="applicableCategory" label="适用类别" width="120" />
      <el-table-column prop="source" label="来源" width="80">
        <template #default="{ row }">
          <el-tag :type="row.source === 'SYSTEM' ? '' : 'success'" size="small">
            {{ row.source === 'SYSTEM' ? '平台' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="handleView(row)">查看</el-button>
          <el-button
            v-if="row.source === 'USER'"
            text
            type="danger"
            size="small"
            @click="handleDelete(row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      class="pagination"
      @current-change="loadData"
    />

    <!-- 上传对话框 -->
    <el-dialog v-model="showCreateDialog" title="上传政策文件" width="500px" @close="resetUploadForm">
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="80px">
        <el-form-item label="文件分类" prop="fileCategory">
          <el-select v-model="uploadForm.fileCategory" placeholder="请选择分类" style="width: 100%">
            <el-option label="法律法规" value="法律法规" />
            <el-option label="规章制度" value="规章制度" />
            <el-option label="政策文件" value="政策文件" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用类别" prop="applicableCategory">
          <el-select v-model="uploadForm.applicableCategory" placeholder="请选择适用类别" style="width: 100%">
            <el-option label="限额以下" value="LIMITED_BELOW" />
            <el-option label="产权交易" value="PROPERTY_TRADE" />
            <el-option label="政府采购" value="GOVERNMENT_PROCUREMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件" prop="fileId">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            :file-list="fileList"
            :limit="1"
            :on-exceed="() => ElMessage.warning('只能上传一个文件')"
            accept=".pdf,.doc,.docx"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、Word 文档，单文件不超过 50MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="uploadForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmitUpload">确定</el-button>
      </template>
    </el-dialog>

    <!-- 文件详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="文件详情" width="500px">
      <el-descriptions :column="1" border v-if="currentDetail">
        <el-descriptions-item label="文件名">{{ currentDetail.fileName }}</el-descriptions-item>
        <el-descriptions-item label="文件分类">{{ currentDetail.fileCategory }}</el-descriptions-item>
        <el-descriptions-item label="适用类别">{{ currentDetail.applicableCategory }}</el-descriptions-item>
        <el-descriptions-item label="来源">
          <el-tag :type="currentDetail.source === 'SYSTEM' ? '' : 'success'" size="small">
            {{ currentDetail.source === 'SYSTEM' ? '平台' : '用户上传' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatFileSize(currentDetail.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="文件类型">{{ currentDetail.fileType }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentDetail.status === 1 ? 'success' : 'info'" size="small">
            {{ currentDetail.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ currentDetail.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ formatTime(currentDetail.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadUserFile } from 'element-plus'
import { policyFileApi } from '@/api/policy-file'
import type { PolicyFileVO } from '@/types/policy-file'

// --- 列表相关 ---
const tableData = ref<PolicyFileVO[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

const searchForm = ref({
  fileName: '',
  fileCategory: '',
  source: '',
})

const loadData = async () => {
  loading.value = true
  try {
    const params: { pageNum: number; pageSize: number; fileName?: string; fileCategory?: string; source?: string } = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    }
    if (searchForm.value.fileName) params.fileName = searchForm.value.fileName
    if (searchForm.value.fileCategory) params.fileCategory = searchForm.value.fileCategory
    if (searchForm.value.source) params.source = searchForm.value.source
    const res = await policyFileApi.getList(params)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

const handleReset = () => {
  searchForm.value = { fileName: '', fileCategory: '', source: '' }
  pageNum.value = 1
  loadData()
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定删除该政策文件？', '确认')
  await policyFileApi.deleteById(id)
  ElMessage.success('删除成功')
  await loadData()
}

// --- 上传对话框 ---
const showCreateDialog = ref(false)
const submitLoading = ref(false)
const uploadFormRef = ref<FormInstance>()
const fileList = ref<UploadUserFile[]>([])

const uploadForm = ref({
  fileCategory: '',
  applicableCategory: '',
  fileId: 0,
  fileSize: 0,
  fileType: '',
  description: '',
})

const uploadRules: FormRules = {
  fileCategory: [{ required: true, message: '请选择文件分类', trigger: 'change' }],
  applicableCategory: [{ required: true, message: '请选择适用类别', trigger: 'change' }],
}

const uploadAction = '/file-api/file/upload'
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const beforeUpload = (file: File) => {
  const maxSize = 50 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  return true
}

const handleUploadSuccess = (response: any) => {
  if (response?.code === 200 && response.data) {
    const fileData = response.data
    uploadForm.value.fileId = fileData.id || fileData.fileId
    uploadForm.value.fileSize = fileData.fileSize || fileData.size || 0
    uploadForm.value.fileType = fileData.fileType || fileData.contentType || ''
    ElMessage.success('文件上传成功')
  } else {
    ElMessage.error(response?.message || '上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('文件上传失败，请重试')
}

const resetUploadForm = () => {
  uploadForm.value = {
    fileCategory: '',
    applicableCategory: '',
    fileId: 0,
    fileSize: 0,
    fileType: '',
    description: '',
  }
  fileList.value = []
  uploadFormRef.value?.resetFields()
}

const handleSubmitUpload = async () => {
  const valid = await uploadFormRef.value?.validate().catch(() => false)
  if (!valid) return

  if (!uploadForm.value.fileId) {
    ElMessage.warning('请先上传文件')
    return
  }

  submitLoading.value = true
  try {
    await policyFileApi.create({
      fileName: fileList.value[0]?.name || '',
      fileCategory: uploadForm.value.fileCategory,
      applicableCategory: uploadForm.value.applicableCategory,
      fileId: uploadForm.value.fileId,
      fileSize: uploadForm.value.fileSize,
      fileType: uploadForm.value.fileType,
      description: uploadForm.value.description || undefined,
    })
    ElMessage.success('上传成功')
    showCreateDialog.value = false
    await loadData()
  } finally {
    submitLoading.value = false
  }
}

// --- 文件详情 ---
const showDetailDialog = ref(false)
const currentDetail = ref<PolicyFileVO | null>(null)

const handleView = (row: PolicyFileVO) => {
  currentDetail.value = row
  showDetailDialog.value = true
}

const formatFileSize = (bytes: number): string => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

/** 格式化ISO时间为友好显示 */
function formatTime(value: string): string {
  if (!value) return '-'
  const d = new Date(value)
  if (isNaN(d.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onMounted(loadData)
</script>

<style scoped>
.search-form {
  margin-top: 16px;
}

.toolbar {
  margin: 16px 0;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
