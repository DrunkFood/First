<template>
  <div class="page-shell external-management">
    <div class="page-header-row">
      <div>
        <div class="page-title">接入系统</div>
        <div class="page-subtitle">管理外部系统接入凭证与调用状态</div>
      </div>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="search-wrap">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="系统名称">
            <el-input v-model="queryParams.systemName" placeholder="请输入系统名称" clearable />
          </el-form-item>
          <el-form-item label="AppKey">
            <el-input v-model="queryParams.appKey" placeholder="请输入AppKey" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
              <el-option label="正常" :value="1" />
              <el-option label="禁用" :value="0" />
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
          新增接入系统
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="systemName" label="系统名称" min-width="150" />
        <el-table-column prop="systemUrl" label="系统URL" min-width="220" show-overflow-tooltip />
        <el-table-column prop="appKey" label="AppKey" min-width="280">
          <template #default="{ row }">
            <div class="key-cell">
              <span class="key-text">{{ row.appKey }}</span>
              <el-button type="primary" link @click="copyToClipboard(row.appKey)">
                <el-icon><CopyDocument /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="有效期截止时间" width="180" />
        <el-table-column prop="description" label="描述" min-width="170" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
              <el-button type="warning" link @click="handleViewSecret(row)">查看AppSecret</el-button>
              <el-button type="success" link @click="handleRegenerate(row)">重新生成</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </div>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="系统名称" prop="systemName">
          <el-input v-model="form.systemName" placeholder="请输入系统名称" />
        </el-form-item>
        <el-form-item label="系统URL" prop="systemUrl">
          <el-input v-model="form.systemUrl" placeholder="请输入系统URL，如：https://example.com" />
        </el-form-item>
        <el-form-item label="有效期截止" prop="expireTime">
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择有效期截止时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="招标文件后缀" prop="tenderDocumentSuffix">
          <el-input v-model="form.tenderDocumentSuffix" placeholder=".HzctZbs" />
        </el-form-item>
        <el-form-item label="投标文件后缀" prop="bidDocumentSuffix">
          <el-input v-model="form.bidDocumentSuffix" placeholder=".HzctTbs" />
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

    <el-dialog v-model="secretDialogVisible" title="密钥信息" width="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="系统名称">{{ currentSystem?.systemName }}</el-descriptions-item>
        <el-descriptions-item label="AppKey">
          <div class="key-cell">
            <span class="key-text">{{ currentSystem?.appKey }}</span>
            <el-button type="primary" link @click="copyToClipboard(currentSystem?.appKey || '')">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="AppSecret">
          <div class="key-cell">
            <span class="key-text">{{ currentSystem?.appSecret || '******' }}</span>
            <el-button type="primary" link @click="copyToClipboard(currentSystem?.appSecret || '')">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="secretDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, CopyDocument } from '@element-plus/icons-vue'
import { externalSystemApi, type CreateExternalSystemParams, type UpdateExternalSystemParams } from '@/api/external'
import type { ExternalSystem } from '@/types'

const loading = ref(false)
const tableData = ref<ExternalSystem[]>([])
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  systemName: '',
  appKey: '',
  status: undefined as number | undefined,
})

const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  systemName: '',
  systemUrl: '',
  expireTime: '',
  description: '',
  tenderDocumentSuffix: '',
  bidDocumentSuffix: '',
  status: 1,
})

const dialogTitle = computed(() => (form.id ? '编辑接入系统' : '新增接入系统'))

const secretDialogVisible = ref(false)
const currentSystem = ref<ExternalSystem | null>(null)

const rules: FormRules = {
  systemName: [{ required: true, message: '请输入系统名称', trigger: 'blur' }],
  systemUrl: [
    {
      type: 'url',
      message: '系统URL格式不正确',
      trigger: 'blur',
    },
  ],
}

const fallbackCopyToClipboard = (text: string): boolean => {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'true')
  textarea.style.position = 'fixed'
  textarea.style.top = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  const copied = document.execCommand('copy')
  document.body.removeChild(textarea)
  return copied
}

const copyToClipboard = async (text: string) => {
  if (!text) {
    ElMessage.warning('无可复制内容')
    return
  }
  try {
    if (navigator.clipboard?.writeText && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
    } else if (!fallbackCopyToClipboard(text)) {
      throw new Error('Fallback copy failed')
    }
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await externalSystemApi.getList(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('Fetch external systems failed:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handleReset = () => {
  queryParams.systemName = ''
  queryParams.appKey = ''
  queryParams.status = undefined
  handleSearch()
}

const handleAdd = () => {
  form.id = undefined
  form.systemName = ''
  form.systemUrl = ''
  form.expireTime = ''
  form.description = ''
  form.tenderDocumentSuffix = ''
  form.bidDocumentSuffix = ''
  form.status = 1
  dialogVisible.value = true
}

const handleEdit = (row: ExternalSystem) => {
  form.id = row.id
  form.systemName = row.systemName
  form.systemUrl = row.systemUrl || ''
  form.expireTime = row.expireTime || ''
  form.description = row.description || ''
  form.tenderDocumentSuffix = row.tenderDocumentSuffix || ''
  form.bidDocumentSuffix = row.bidDocumentSuffix || ''
  form.status = row.status
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      if (form.id) {
        const params: UpdateExternalSystemParams = {
          id: form.id,
          systemName: form.systemName,
          systemUrl: form.systemUrl || undefined,
          expireTime: form.expireTime || undefined,
          description: form.description,
          status: form.status,
          tenderDocumentSuffix: form.tenderDocumentSuffix || undefined,
          bidDocumentSuffix: form.bidDocumentSuffix || undefined,
        }
        await externalSystemApi.update(params)
        ElMessage.success('更新成功')
      } else {
        const params: CreateExternalSystemParams = {
          systemName: form.systemName,
          systemUrl: form.systemUrl || undefined,
          expireTime: form.expireTime || undefined,
          description: form.description,
          tenderDocumentSuffix: form.tenderDocumentSuffix || undefined,
          bidDocumentSuffix: form.bidDocumentSuffix || undefined,
        }
        await externalSystemApi.create(params)
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

const handleViewSecret = async (row: ExternalSystem) => {
  try {
    const res = await externalSystemApi.getById(row.id)
    currentSystem.value = res.data
    secretDialogVisible.value = true
  } catch (error) {
    console.error('Get system detail failed:', error)
  }
}

const handleRegenerate = (row: ExternalSystem) => {
  ElMessageBox.confirm(`确定要重新生成系统 "${row.systemName}" 的密钥吗？原密钥将失效！`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    const res = await externalSystemApi.regenerateSecret(row.id)
    ElMessage.success('密钥重新生成成功')
    currentSystem.value = { ...row, appSecret: res.data.appSecret }
    secretDialogVisible.value = true
  })
}

const handleDelete = (row: ExternalSystem) => {
  ElMessageBox.confirm(`确定要删除接入系统 "${row.systemName}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await externalSystemApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.external-management {
  .action-cell {
    white-space: nowrap;
  }

  .key-cell {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .key-text {
    word-break: break-all;
    color: #38574b;
  }
}
</style>
