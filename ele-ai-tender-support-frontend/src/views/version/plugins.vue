<template>
  <div class="page-shell plugin-management">
    <div class="page-header-row">
      <div>
        <div class="page-title">插件管理</div>
        <div class="page-subtitle">为指定主版本维护插件制品</div>
      </div>
      <el-button @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="version-info" v-if="versionInfo">
        <el-descriptions :column="4" border>
          <el-descriptions-item label="版本号">{{ versionInfo.versionNumber }}</el-descriptions-item>
          <el-descriptions-item label="版本名称">{{ versionInfo.versionName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(versionInfo.status)">
              {{ getStatusLabel(versionInfo.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ versionInfo.createTime }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="toolbar-wrap">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增插件
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="pluginName" label="插件名称" min-width="150" />
        <el-table-column prop="pluginCode" label="插件编码" min-width="150" />
        <el-table-column prop="description" label="描述" min-width="180" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="插件名称" prop="pluginName">
          <el-input v-model="form.pluginName" placeholder="请输入插件名称" />
        </el-form-item>
        <el-form-item label="插件编码" prop="pluginCode">
          <el-input v-model="form.pluginCode" :disabled="!!form.id" placeholder="请输入插件编码" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="插件文件" prop="fileId">
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
              <div class="el-upload__tip">支持上传 zip、jar 格式文件</div>
            </template>
          </el-upload>
          <div v-if="form.fileId" class="file-info">
            <el-tag closable @close="form.fileId = ''">
              已上传文件: {{ form.fileName || form.fileId }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item v-if="form.id" label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { ArrowLeft, Plus, Upload } from '@element-plus/icons-vue'
import { versionApi, type CreatePluginParams, type UpdatePluginParams } from '@/api/version'
import { fileApi } from '@/api/file'
import type { VersionInfo, PluginInfo } from '@/types'

const route = useRoute()
const router = useRouter()
const versionId = computed(() => Number(route.params.id))

const loading = ref(false)
const tableData = ref<PluginInfo[]>([])
const versionInfo = ref<VersionInfo | null>(null)

const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  pluginName: '',
  pluginCode: '',
  description: '',
  fileId: '',
  fileName: '',
  sortOrder: 0,
  status: 1,
})

const dialogTitle = computed(() => (form.id ? '编辑插件' : '新增插件'))

const rules: FormRules = {
  pluginName: [{ required: true, message: '请输入插件名称', trigger: 'blur' }],
  pluginCode: [
    { required: true, message: '请输入插件编码', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/,
      message: '插件编码只能包含字母、数字、下划线和连字符，且以字母开头',
      trigger: 'blur',
    },
  ],
}

const getStatusLabel = (status: number) => {
  const labels: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '已下线' }
  return labels[status] || ''
}

const getStatusType = (status: number) => {
  const types: Record<number, string> = { 0: 'info', 1: 'success', 2: 'danger' }
  return types[status] || ''
}

const fetchVersionInfo = async () => {
  try {
    const res = await versionApi.getById(versionId.value)
    versionInfo.value = res.data
  } catch (error) {
    console.error('Fetch version info failed:', error)
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await versionApi.getPlugins(versionId.value)
    tableData.value = res.data
  } catch (error) {
    console.error('Fetch plugins failed:', error)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  form.id = undefined
  form.pluginName = ''
  form.pluginCode = ''
  form.description = ''
  form.fileId = ''
  form.fileName = ''
  form.sortOrder = 0
  form.status = 1
  dialogVisible.value = true
}

const handleEdit = (row: PluginInfo) => {
  form.id = row.id
  form.pluginName = row.pluginName
  form.pluginCode = row.pluginCode
  form.description = row.description || ''
  form.fileId = row.fileId || ''
  form.fileName = ''
  form.sortOrder = row.sortOrder
  form.status = row.status
  dialogVisible.value = true
}

const beforeUpload = (file: File) => {
  const allowedTypes = ['application/zip', 'application/x-zip-compressed', 'application/java-archive']
  const allowedExtensions = ['.zip', '.jar']
  const extension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()

  if (!allowedTypes.includes(file.type) && !allowedExtensions.includes(extension)) {
    ElMessage.error('只能上传 zip、jar 格式文件')
    return false
  }

  if (file.size > 100 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 100MB')
    return false
  }

  return true
}

const handleUpload = async (options: UploadRequestOptions) => {
  try {
    const res = await fileApi.upload(options.file as File, 'plugin')
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
        const params: UpdatePluginParams = {
          id: form.id,
          pluginName: form.pluginName,
          description: form.description,
          fileId: form.fileId,
          sortOrder: form.sortOrder,
          status: form.status,
        }
        await versionApi.updatePlugin(params)
        ElMessage.success('更新成功')
      } else {
        const params: CreatePluginParams = {
          versionId: versionId.value,
          pluginName: form.pluginName,
          pluginCode: form.pluginCode,
          description: form.description,
          fileId: form.fileId,
          sortOrder: form.sortOrder,
        }
        await versionApi.createPlugin(params)
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

const handleDelete = (row: PluginInfo) => {
  ElMessageBox.confirm(`确定要删除插件 "${row.pluginName}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await versionApi.deletePlugin(row.id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

onMounted(() => {
  fetchVersionInfo()
  fetchData()
})
</script>

<style scoped lang="scss">
.plugin-management {
  .version-info {
    margin-bottom: 14px;
  }

  .file-info {
    margin-top: 10px;
  }
}
</style>
