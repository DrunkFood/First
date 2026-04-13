<template>
  <div class="page-shell model-config-management">
    <div class="page-header-row">
      <div>
        <div class="page-title">AI模型配置</div>
        <div class="page-subtitle">管理AI模型配置，支持多种模型类型切换</div>
      </div>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="search-wrap">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="模型名称">
            <el-input v-model="queryParams.modelName" placeholder="请输入模型名称" clearable />
          </el-form-item>
          <el-form-item label="模型类型">
            <el-select v-model="queryParams.modelType" placeholder="请选择模型类型" clearable>
              <el-option label="本地微调" value="LOCAL" />
              <el-option label="云端大模型" value="CLOUD" />
              <el-option label="私有化部署" value="PRIVATE" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
              <el-option label="启用" :value="1" />
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
          新增配置
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
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="modelName" label="模型名称" min-width="150" />
        <el-table-column prop="modelType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.modelType === 'LOCAL'" type="info">本地微调</el-tag>
            <el-tag v-else-if="row.modelType === 'CLOUD'" type="primary">云端大模型</el-tag>
            <el-tag v-else-if="row.modelType === 'PRIVATE'" type="warning">私有化部署</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="modelCode" label="模型代码" width="130" />
        <el-table-column prop="isActive" label="激活" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.isActive"
              :active-value="1"
              :inactive-value="0"
              @change="handleToggleActive(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="tokenUsage" label="Token用量" width="100">
          <template #default="{ row }">
            {{ row.tokenUsage }} / {{ row.tokenLimit || '不限' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small">启用</el-tag>
            <el-tag v-else type="danger" size="small">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.isActive !== 1"
              link
              type="success"
              @click="handleSetActive(row)"
            >
              设为激活
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
      width="700px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="formData.modelName" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="模型类型" prop="modelType">
          <el-select v-model="formData.modelType" placeholder="请选择模型类型" style="width: 100%">
            <el-option label="本地微调" value="LOCAL" />
            <el-option label="云端大模型" value="CLOUD" />
            <el-option label="私有化部署" value="PRIVATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型代码" prop="modelCode">
          <el-input v-model="formData.modelCode" placeholder="请输入模型代码，如deepseek-chat" />
        </el-form-item>
        <el-form-item label="API端点" prop="endpoint">
          <el-input v-model="formData.endpoint" placeholder="请输入API端点URL" />
        </el-form-item>
        <el-form-item label="API密钥">
          <el-input
            v-model="formData.apiKey"
            type="password"
            show-password
            placeholder="请输入API密钥"
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最大Token数">
              <el-input-number
                v-model="formData.maxTokens"
                :min="1"
                :max="100000"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Temperature">
              <el-input-number
                v-model="formData.temperature"
                :min="0"
                :max="2"
                :step="0.1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Top P">
              <el-input-number
                v-model="formData.topP"
                :min="0"
                :max="1"
                :step="0.1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="超时时间(ms)">
              <el-input-number
                v-model="formData.timeout"
                :min="1000"
                :step="1000"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Token限制">
              <el-input-number
                v-model="formData.tokenLimit"
                :min="0"
                :step="1000"
                style="width: 100%"
                placeholder="0表示不限"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="高级参数">
          <el-input
            v-model="formData.parameters"
            type="textarea"
            :rows="3"
            placeholder="JSON格式的高级参数（可选）"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
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
import { modelConfigApi } from '@/api/model-config'
import type {
  ModelConfigInfo,
  ModelConfigQueryParams,
  ModelConfigCreateParams,
  ModelConfigUpdateParams,
} from '@/types/model-config'

const loading = ref(false)
const tableData = ref<ModelConfigInfo[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])

const queryParams = reactive<ModelConfigQueryParams>({
  pageNum: 1,
  pageSize: 10,
  modelName: undefined,
  modelType: undefined,
  status: undefined,
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive<ModelConfigCreateParams & { id?: number }>({
  modelName: '',
  modelType: 'CLOUD',
  endpoint: '',
  apiKey: '',
  modelCode: '',
  maxTokens: 4000,
  temperature: 0.7,
  topP: 0.9,
  timeout: 30000,
  parameters: '',
  tokenLimit: 0,
  remark: '',
})

const formRules = reactive<FormRules>({
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
  modelCode: [{ required: true, message: '请输入模型代码', trigger: 'blur' }],
  endpoint: [{ required: true, message: '请输入API端点', trigger: 'blur' }],
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await modelConfigApi.getList(queryParams)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    console.error('获取模型配置列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchList()
}

const handleReset = () => {
  queryParams.modelName = undefined
  queryParams.modelType = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  fetchList()
}

const handleAdd = () => {
  dialogTitle.value = '新增模型配置'
  dialogVisible.value = true
}

const handleEdit = (row: ModelConfigInfo) => {
  dialogTitle.value = '编辑模型配置'
  Object.assign(formData, {
    id: row.id,
    modelName: row.modelName,
    modelType: row.modelType,
    modelCode: row.modelCode,
    endpoint: row.endpoint,
    apiKey: row.apiKey,
    maxTokens: row.maxTokens,
    temperature: row.temperature,
    topP: row.topP,
    timeout: row.timeout,
    parameters: row.parameters,
    tokenLimit: row.tokenLimit,
    remark: row.remark,
  })
  dialogVisible.value = true
}

const handleDelete = async (row: ModelConfigInfo) => {
  try {
    await ElMessageBox.confirm(`确定删除模型配置"${row.modelName}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await modelConfigApi.deleteById(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除模型配置失败:', error)
    }
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个模型配置吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await modelConfigApi.deleteByIds(selectedIds.value)
    ElMessage.success('批量删除成功')
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除模型配置失败:', error)
    }
  }
}

const handleToggleActive = async (row: ModelConfigInfo) => {
  try {
    if (row.isActive === 1) {
      await modelConfigApi.changeStatus(row.id, 0)
      ElMessage.success('已停用模型')
    } else {
      await modelConfigApi.setActive(row.id)
      ElMessage.success('已激活模型')
    }
    fetchList()
  } catch (error) {
    console.error('切换激活状态失败:', error)
    fetchList()
  }
}

const handleSetActive = async (row: ModelConfigInfo) => {
  try {
    await modelConfigApi.setActive(row.id)
    ElMessage.success('已设置为激活模型')
    fetchList()
  } catch (error) {
    console.error('设置激活模型失败:', error)
  }
}

const handleSelectionChange = (selection: ModelConfigInfo[]) => {
  selectedIds.value = selection.map((item) => item.id)
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      if (formData.id) {
        await modelConfigApi.update(formData as ModelConfigUpdateParams)
        ElMessage.success('更新成功')
      } else {
        await modelConfigApi.create(formData)
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
    modelName: '',
    modelType: 'CLOUD',
    endpoint: '',
    apiKey: '',
    modelCode: '',
    maxTokens: 4000,
    temperature: 0.7,
    topP: 0.9,
    timeout: 30000,
    parameters: '',
    tokenLimit: 0,
    remark: '',
  })
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/index.scss';
</style>
