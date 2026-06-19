<template>
  <div class="page-shell policy-file-page">
    <div class="page-header-row">
      <div>
        <div class="page-title">政策文件管理</div>
        <div class="page-subtitle">管理法律法规、规章制度和政策文件</div>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        上传文件
      </el-button>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="search-wrap">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="文件名称">
            <el-input v-model="queryParams.fileName" placeholder="请输入文件名称" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="文件分类">
            <el-select v-model="queryParams.fileCategory" placeholder="请选择" clearable style="width: 160px">
              <el-option label="法律法规" value="LAW" />
              <el-option label="规章制度" value="REGULATION" />
              <el-option label="政策文件" value="POLICY" />
            </el-select>
          </el-form-item>
          <el-form-item label="适用类别">
            <el-select v-model="queryParams.applicableCategory" placeholder="请选择" clearable style="width: 160px">
              <el-option label="小额交易" value="SMALL_TRADE" />
              <el-option label="政府采购" value="GOVERNMENT_PROCUREMENT" />
              <el-option label="综合交易" value="COMPREHENSIVE_TRADE" />
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

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="fileName" label="文件名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="fileCategory" label="文件分类" width="120">
          <template #default="{ row }">
            {{ categoryLabel(row.fileCategory) }}
          </template>
        </el-table-column>
        <el-table-column prop="applicableCategory" label="适用类别" width="120">
          <template #default="{ row }">
            {{ applicableLabel(row.applicableCategory) }}
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="格式" width="80" />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createName" label="上传人" width="100" />
        <el-table-column prop="createTime" label="上传时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog v-model="dialogVisible" title="上传政策文件" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="文件" required>
          <el-upload
            :http-request="handleUpload"
            :before-upload="beforeUpload"
            v-model:file-list="fileList"
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
        <el-form-item label="文件分类" required>
          <el-select v-model="form.fileCategory" placeholder="请选择" style="width: 100%">
            <el-option label="法律法规" value="LAW" />
            <el-option label="规章制度" value="REGULATION" />
            <el-option label="政策文件" value="POLICY" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用类别">
          <el-select v-model="form.applicableCategory" placeholder="请选择" clearable style="width: 100%">
            <el-option label="小额交易" value="SMALL_TRADE" />
            <el-option label="政府采购" value="GOVERNMENT_PROCUREMENT" />
            <el-option label="综合交易" value="COMPREHENSIVE_TRADE" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入文件描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions, UploadUserFile } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import type { PolicyFileInfo } from '@/types/policy-file'
import { policyFileApi } from '@/api/policy-file'
import { fileApi } from '@/api/file'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<PolicyFileInfo[]>([])
const total = ref(0)
const dialogVisible = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  fileName: '',
  fileCategory: '',
  applicableCategory: '',
})

const fileList = ref<UploadUserFile[]>([])

const form = reactive({
  fileName: '',
  fileCategory: '',
  applicableCategory: '',
  fileId: 0 as number | string,
  fileSize: 0,
  fileType: '',
  description: '',
})

const categoryMap: Record<string, string> = { LAW: '法律法规', REGULATION: '规章制度', POLICY: '政策文件' }
const applicableMap: Record<string, string> = { SMALL_TRADE: '小额交易', GOVERNMENT_PROCUREMENT: '政府采购', COMPREHENSIVE_TRADE: '综合交易' }

const categoryLabel = (code?: string) => (code ? categoryMap[code] || code : '-')
const applicableLabel = (code?: string) => (code ? applicableMap[code] || code : '-')

const formatSize = (bytes?: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}:${s}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await policyFileApi.getList(queryParams)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('Fetch policy files failed:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handleReset = () => {
  queryParams.fileName = ''
  queryParams.fileCategory = ''
  queryParams.applicableCategory = ''
  handleSearch()
}

const handleCreate = () => {
  form.fileName = ''
  form.fileCategory = ''
  form.applicableCategory = ''
  form.fileId = 0
  form.fileSize = 0
  form.fileType = ''
  form.description = ''
  fileList.value = []
  dialogVisible.value = true
}

const beforeUpload = (file: File) => {
  const maxSize = 50 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  return true
}

const handleUpload = async (options: UploadRequestOptions) => {
  try {
    const res = await fileApi.upload(options.file as File, 'policy_file')
    form.fileId = res.data.fileId
    form.fileName = res.data.fileName
    form.fileSize = res.data.fileSize
    form.fileType = extractFileExt(form.fileName)
    ElMessage.success('文件上传成功')
  } catch (error) {
    console.error('Upload failed:', error)
  }
}

function extractFileExt(fileName: string): string {
  if (!fileName || !fileName.includes('.')) return ''
  return fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase()
}

const handleSubmit = async () => {
  if (!form.fileId) {
    ElMessage.warning('请先上传文件')
    return
  }
  if (!form.fileCategory) {
    ElMessage.warning('请选择文件分类')
    return
  }
  submitting.value = true
  try {
    await policyFileApi.create(form)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('Create policy file failed:', error)
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row: PolicyFileInfo) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await policyFileApi.setStatus(row.id, newStatus)
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    fetchData()
  } catch (error) {
    console.error('Toggle status failed:', error)
  }
}

const handleDelete = (row: PolicyFileInfo) => {
  ElMessageBox.confirm(`确定删除文件「${row.fileName}」？`, '提示', { type: 'warning' }).then(async () => {
    try {
      await policyFileApi.deleteById(row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (error) {
      console.error('Delete policy file failed:', error)
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchData()
})
</script>
