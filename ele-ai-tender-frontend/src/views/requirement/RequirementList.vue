<template>
  <div class="requirement-list">
    <!-- 页面标题 -->
    <h1 class="page-title">编制业务需求</h1>

    <!-- 筛选区域 -->
    <div class="filter-section">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="项目名称">
          <el-input v-model="queryParams.requirementName" placeholder="请输入项目名称" clearable />
        </el-form-item>
        <el-form-item label="需求状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="(item, key) in REQUIREMENT_STATUS_MAP"
              :key="key"
              :label="item.label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="项目类型">
          <el-select v-model="queryParams.projectType" placeholder="全部" clearable style="width: 140px">
            <el-option label="工程类" value="ENGINEERING" />
            <el-option label="货物类" value="GOODS" />
            <el-option label="服务类" value="SERVICE" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格区域 -->
    <div class="table-container">
      <div class="table-header">
        <h3 class="table-title">业务需求列表</h3>
        <el-button type="primary" @click="handleCreate">新建业务需求</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="requirementName" label="项目名称" min-width="160">
          <template #default="{ row }">
            <el-link type="primary" @click="handleView(row.id)">{{ row.requirementName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="需求状态" width="100">
          <template #default="{ row }">
            <StatusBadge :status="row.status" :type-map="REQUIREMENT_STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column prop="projectType" label="项目类型" width="120">
          <template #default="{ row }">
            <StatusBadge :status="row.projectType" :type-map="PROJECT_TYPE_MAP" />
          </template>
        </el-table-column>
        <el-table-column prop="budget" label="项目预算(万元)" width="150" align="right">
          <template #default="{ row }">
            {{ formatBudget(row.budget) }}
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="完成进度" width="180">
          <template #default="{ row }">
            <ProgressCell :percentage="row.progress ?? 0" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="查看" placement="top">
                <el-button :icon="View" link type="primary" @click="handleView(row.id)" />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button :icon="Delete" link type="danger" @click="handleDelete(row.id)" />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <span class="pagination-info">
          共 <strong>{{ total }}</strong> 条记录，当前显示第 <strong>{{ paginationStart }}-{{ paginationEnd }}</strong> 条
        </span>
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          @current-change="fetchData"
          @size-change="fetchData"
          layout="sizes, prev, pager, next"
          :page-sizes="[5, 10, 20, 50]"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { requirementApi } from '@/api/requirement'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Delete } from '@element-plus/icons-vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import ProgressCell from '@/components/common/ProgressCell.vue'
import { PROJECT_TYPE_MAP, REQUIREMENT_STATUS_MAP } from '@/constants/status-maps'
import { formatBudgetWanYuan } from '@/utils/budget'
import type { RequirementQueryParams } from '@/types/requirement'

const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)

const queryParams = reactive<RequirementQueryParams>({
  pageNum: 1,
  pageSize: 5,
  requirementName: '',
  status: '',
  projectType: '',
  createTimeStart: '',
  createTimeEnd: '',
})

const paginationStart = computed(() => {
  if (total.value === 0) return 0
  return (queryParams.pageNum - 1) * queryParams.pageSize + 1
})

const paginationEnd = computed(() => {
  return Math.min(queryParams.pageNum * queryParams.pageSize, total.value)
})

async function fetchData() {
  loading.value = true
  try {
    const result = await requirementApi.getList(queryParams)
    tableData.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryParams.pageNum = 1
  fetchData()
}

function handleReset() {
  queryParams.requirementName = ''
  queryParams.status = ''
  queryParams.projectType = ''
  queryParams.createTimeStart = ''
  queryParams.createTimeEnd = ''
  dateRange.value = null
  handleSearch()
}

function handleDateChange(val: [string, string] | null) {
  if (val) {
    queryParams.createTimeStart = val[0]
    queryParams.createTimeEnd = val[1]
  } else {
    queryParams.createTimeStart = ''
    queryParams.createTimeEnd = ''
  }
}

function handleCreate() {
  router.push('/requirement/create')
}

function handleView(id: number) {
  router.push(`/requirement/generate/${id}`)
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该需求？删除后不可恢复。', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await requirementApi.deleteById(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 用户取消或删除失败
  }
}

function formatBudget(value?: number): string {
  return formatBudgetWanYuan(value)
}

function formatTime(value?: string): string {
  if (!value) return '-'
  try {
    const date = new Date(value)
    if (isNaN(date.getTime())) return value
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    const h = String(date.getHours()).padStart(2, '0')
    const min = String(date.getMinutes()).padStart(2, '0')
    const s = String(date.getSeconds()).padStart(2, '0')
    return `${y}-${m}-${d} ${h}:${min}:${s}`
  } catch {
    return value
  }
}

onMounted(fetchData)
</script>

<style scoped>
.requirement-list {
  padding: 24px 40px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin: 0 0 24px 0;
}

.filter-section {
  background: var(--app-bg-elevated);
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid var(--app-border-light);
}

.table-container {
  background: var(--app-bg-elevated);
  border-radius: 8px;
  border: 1px solid var(--app-border-light);
  overflow: hidden;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--app-border-light);
}

.table-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-primary);
}

.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-top: 1px solid var(--app-border-light);
}

.pagination-info {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.pagination-info strong {
  color: var(--app-text-primary);
}

.action-buttons {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
