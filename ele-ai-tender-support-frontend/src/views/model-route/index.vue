<template>
  <div class="page-shell model-route-page">
    <div class="page-header-row">
      <div>
        <div class="page-title">模型路由管理</div>
        <div class="page-subtitle">配置AI使用场景对应的模型路由规则</div>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        创建规则
      </el-button>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="search-wrap">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="使用场景">
            <el-select v-model="queryParams.usageScenario" placeholder="请选择" clearable style="width: 180px">
              <el-option label="内容生成" value="GENERATION" />
              <el-option label="内容优化" value="OPTIMIZATION" />
              <el-option label="智能检测" value="DETECTION" />
              <el-option label="AI对话" value="CHAT" />
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
        <el-table-column prop="usageScenario" label="使用场景" width="140">
          <template #default="{ row }">
            {{ scenarioLabel(row.usageScenario) }}
          </template>
        </el-table-column>
        <el-table-column prop="primaryModelName" label="优先模型" min-width="180" show-overflow-tooltip />
        <el-table-column prop="fallbackModelName" label="降级模型" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.fallbackModelName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80" />
        <el-table-column prop="isActive" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isActive === 1 ? 'success' : 'info'">
              {{ row.isActive === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleToggleActive(row)">
              {{ row.isActive === 1 ? '停用' : '启用' }}
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

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑路由规则' : '创建路由规则'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="使用场景" required>
          <el-select v-model="form.usageScenario" placeholder="请选择" style="width: 100%">
            <el-option label="内容生成" value="GENERATION" />
            <el-option label="内容优化" value="OPTIMIZATION" />
            <el-option label="智能检测" value="DETECTION" />
            <el-option label="AI对话" value="CHAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先模型" required>
          <el-select v-model="form.primaryModelId" placeholder="请选择优先模型" filterable style="width: 100%">
            <el-option
              v-for="m in activeModelList"
              :key="m.id"
              :label="`${m.modelName}${m.provider === 'ZHIPU' ? ' [智谱]' : ''}`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="降级模型">
          <el-select v-model="form.fallbackModelId" placeholder="请选择降级模型（可选）" filterable clearable style="width: 100%">
            <el-option
              v-for="m in activeModelList"
              :key="m.id"
              :label="`${m.modelName}${m.provider === 'ZHIPU' ? ' [智谱]' : ''}`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入规则描述" />
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
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import type { ModelRouteRuleInfo } from '@/types/model-route'
import type { ModelConfigInfo } from '@/types/model-config'
import { modelRouteApi } from '@/api/model-route'
import { modelConfigApi } from '@/api/model-config'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<ModelRouteRuleInfo[]>([])
const activeModelList = ref<ModelConfigInfo[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  usageScenario: '',
})

const form = reactive({
  usageScenario: '',
  primaryModelId: undefined as number | undefined,
  fallbackModelId: undefined as number | undefined,
  priority: 0,
  description: '',
})

const scenarioMap: Record<string, string> = { GENERATION: '内容生成', OPTIMIZATION: '内容优化', DETECTION: '智能检测', CHAT: 'AI对话' }
const scenarioLabel = (code?: string) => (code ? scenarioMap[code] || code : '-')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await modelRouteApi.getList(queryParams)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('Fetch model routes failed:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handleReset = () => {
  queryParams.usageScenario = ''
  handleSearch()
}

const handleCreate = () => {
  isEdit.value = false
  editId.value = null
  form.usageScenario = ''
  form.primaryModelId = undefined
  form.fallbackModelId = undefined
  form.priority = 0
  form.description = ''
  dialogVisible.value = true
}

const handleEdit = (row: ModelRouteRuleInfo) => {
  isEdit.value = true
  editId.value = row.id
  form.usageScenario = row.usageScenario
  form.primaryModelId = row.primaryModelId
  form.fallbackModelId = row.fallbackModelId
  form.priority = row.priority
  form.description = row.description || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.usageScenario || !form.primaryModelId) {
    ElMessage.warning('请填写必填项')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value && editId.value) {
      await modelRouteApi.update(editId.value, { ...form, primaryModelId: form.primaryModelId! })
      ElMessage.success('更新成功')
    } else {
      await modelRouteApi.create({ ...form, primaryModelId: form.primaryModelId! })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('Submit failed:', error)
  } finally {
    submitting.value = false
  }
}

const handleToggleActive = async (row: ModelRouteRuleInfo) => {
  const newActive = row.isActive === 1 ? 0 : 1
  try {
    await modelRouteApi.setActive(row.id, newActive)
    ElMessage.success(newActive === 1 ? '已启用' : '已停用')
    fetchData()
  } catch (error) {
    console.error('Toggle active failed:', error)
  }
}

const handleDelete = (row: ModelRouteRuleInfo) => {
  ElMessageBox.confirm('确定删除该路由规则？', '提示', { type: 'warning' }).then(async () => {
    try {
      await modelRouteApi.deleteById(row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (error) {
      console.error('Delete failed:', error)
    }
  }).catch(() => {})
}

const fetchActiveModels = async () => {
  try {
    const res = await modelConfigApi.getActiveList()
    activeModelList.value = res.data || []
  } catch (error) {
    console.error('Fetch active models failed:', error)
  }
}

onMounted(() => {
  fetchData()
  fetchActiveModels()
})
</script>
