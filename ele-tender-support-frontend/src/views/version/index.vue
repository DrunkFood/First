<template>
  <div class="page-shell version-management">
    <div class="page-header-row">
      <div>
        <div class="page-title">版本管理</div>
        <div class="page-subtitle">维护主版本发布状态与制品文件</div>
      </div>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="search-wrap">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="版本号">
            <el-input v-model="queryParams.versionNumber" placeholder="请输入版本号" clearable />
          </el-form-item>
          <el-form-item label="版本名称">
            <el-input v-model="queryParams.versionName" placeholder="请输入版本名称" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
              <el-option label="草稿" :value="0" />
              <el-option label="已发布" :value="1" />
              <el-option label="已下线" :value="2" />
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
          新增版本
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="versionNumber" label="版本号" min-width="120" />
        <el-table-column prop="versionName" label="版本名称" min-width="150" />
        <el-table-column prop="description" label="描述" min-width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="releaseDate" label="发布日期" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link @click="handlePlugins(row)">插件管理</el-button>
            <el-button v-if="row.status === 0" type="warning" link @click="handlePublish(row)">发布</el-button>
            <el-button v-if="row.status === 1" type="info" link @click="handleDeprecate(row)">下线</el-button>
            <el-button v-if="row.status === 0" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="版本号" prop="versionNumber">
          <el-input v-model="form.versionNumber" :disabled="!!form.id" placeholder="请输入版本号，如 1.0.0" />
        </el-form-item>
        <el-form-item label="版本名称" prop="versionName">
          <el-input v-model="form.versionName" placeholder="请输入版本名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入版本描述" />
        </el-form-item>
        <el-form-item label="发布说明" prop="releaseNotes">
          <el-input v-model="form.releaseNotes" type="textarea" :rows="4" placeholder="请输入发布说明" />
        </el-form-item>
        <el-form-item label="版本文件" prop="fileId">
          <el-upload
            class="upload-demo"
            :show-file-list="false"
            :http-request="handleUpload"
            :before-upload="beforeUpload"
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              上传文件
            </el-button>
            <template #tip>
              <div class="el-upload__tip">支持上传 zip、jar、war 格式文件</div>
            </template>
          </el-upload>
          <div v-if="form.fileId" class="file-info">
            <el-tag closable @close="form.fileId = ''">
              已上传文件: {{ form.fileName || form.fileId }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item v-if="form.id" label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下线" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { Search, Refresh, Plus, Upload } from '@element-plus/icons-vue'
import { versionApi, type CreateVersionParams, type UpdateVersionParams } from '@/api/version'
import { fileApi } from '@/api/file'
import type { VersionInfo } from '@/types'

const router = useRouter()
const loading = ref(false)
const tableData = ref<VersionInfo[]>([])
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  versionNumber: '',
  versionName: '',
  status: undefined as number | undefined,
})

const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  versionNumber: '',
  versionName: '',
  description: '',
  releaseNotes: '',
  fileId: '',
  fileName: '',
  status: 0,
})

const dialogTitle = computed(() => (form.id ? '编辑版本' : '新增版本'))

const rules: FormRules = {
  versionNumber: [
    { required: true, message: '请输入版本号', trigger: 'blur' },
    { pattern: /^\d+\.\d+\.\d+$/, message: '版本号格式不正确，如 1.0.0', trigger: 'blur' },
  ],
  versionName: [{ required: true, message: '请输入版本名称', trigger: 'blur' }],
}

const getStatusLabel = (status: number) => {
  const labels: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '已下线' }
  return labels[status] || ''
}

const getStatusType = (status: number) => {
  const types: Record<number, string> = { 0: 'info', 1: 'success', 2: 'danger' }
  return types[status] || ''
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await versionApi.getList(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('Fetch versions failed:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handleReset = () => {
  queryParams.versionNumber = ''
  queryParams.versionName = ''
  queryParams.status = undefined
  handleSearch()
}

const handleAdd = () => {
  form.id = undefined
  form.versionNumber = ''
  form.versionName = ''
  form.description = ''
  form.releaseNotes = ''
  form.fileId = ''
  form.fileName = ''
  form.status = 0
  dialogVisible.value = true
}

const handleEdit = (row: VersionInfo) => {
  form.id = row.id
  form.versionNumber = row.versionNumber
  form.versionName = row.versionName
  form.description = row.description || ''
  form.releaseNotes = row.releaseNotes || ''
  form.fileId = row.fileId || ''
  form.fileName = ''
  form.status = row.status
  dialogVisible.value = true
}

const beforeUpload = (file: File) => {
  const allowedTypes = ['application/zip', 'application/x-zip-compressed', 'application/java-archive']
  const allowedExtensions = ['.zip', '.jar', '.war']
  const extension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()

  if (!allowedTypes.includes(file.type) && !allowedExtensions.includes(extension)) {
    ElMessage.error('只能上传 zip、jar、war 格式文件')
    return false
  }

  if (file.size > 500 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 500MB')
    return false
  }

  return true
}

const handleUpload = async (options: UploadRequestOptions) => {
  try {
    const res = await fileApi.upload(options.file as File, 'main-version')
    form.fileId = String(res.data.fileId)
    form.fileName = res.data.fileName
    ElMessage.success('文件上传成功')
  } catch (error) {
    console.error('Upload failed:', error)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      if (form.id) {
        const params: UpdateVersionParams = {
          id: form.id,
          versionName: form.versionName,
          description: form.description,
          releaseNotes: form.releaseNotes,
          fileId: form.fileId,
          status: form.status,
        }
        await versionApi.update(params)
        ElMessage.success('更新成功')
      } else {
        const params: CreateVersionParams = {
          versionNumber: form.versionNumber,
          versionName: form.versionName,
          description: form.description,
          releaseNotes: form.releaseNotes,
          fileId: form.fileId,
        }
        await versionApi.create(params)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchData()
    } catch (error) {
      console.error('Submit failed:', error)
    } finally {
      submitLoading.value = false
    }
  })
}

const handlePlugins = (row: VersionInfo) => {
  router.push(`/version/${row.id}/plugins`)
}

const handlePublish = (row: VersionInfo) => {
  ElMessageBox.confirm(`确定要发布版本 "${row.versionNumber}" 吗？发布后将无法删除！`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await versionApi.publish(row.id)
    ElMessage.success('发布成功')
    fetchData()
  })
}

const handleDeprecate = (row: VersionInfo) => {
  ElMessageBox.confirm(`确定要废弃版本 "${row.versionNumber}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await versionApi.deprecate(row.id)
    ElMessage.success('操作成功')
    fetchData()
  })
}

const handleDelete = (row: VersionInfo) => {
  ElMessageBox.confirm(`确定要删除版本 "${row.versionNumber}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await versionApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.version-management {
  .file-info {
    margin-top: 10px;
  }
}
</style>
