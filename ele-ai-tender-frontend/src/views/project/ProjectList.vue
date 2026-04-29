<template>
  <div class="project-list">
    <!-- 筛选条件 -->
    <div class="filter-section">
      <div class="filter-header">
        <h3>筛选条件</h3>
      </div>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="搜索项目">
          <el-input
            v-model="queryParams.projectName"
            placeholder="名称/编号"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="项目状态">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="(cfg, key) in PROJECT_STATUS_MAP"
              :key="key"
              :label="cfg.label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="项目类别">
          <el-select v-model="queryParams.projectCategory" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="(cfg, key) in PROJECT_CATEGORY_MAP"
              :key="key"
              :label="cfg.label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="项目类型">
          <el-select v-model="queryParams.projectType" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="(cfg, key) in PROJECT_TYPE_MAP"
              :key="key"
              :label="cfg.label"
              :value="key"
            />
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
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 项目列表 -->
    <div class="table-section">
      <div class="table-header">
        <h3>项目列表</h3>
        <div class="table-actions">
          <el-button
            type="danger"
            plain
            :disabled="!selectedIds.length"
            @click="handleBatchDelete"
          >
            批量删除{{ selectedIds.length ? `(${selectedIds.length})` : '' }}
          </el-button>
          <el-button type="primary" @click="handleCreate">新建项目</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="projectCode" label="项目编号" width="150" />
        <el-table-column label="项目名称" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" @click="handleView(row.id)">{{ row.projectName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="项目类别" width="120" align="center">
          <template #default="{ row }">
            <el-tag
              :type="getCategoryTagType(row.projectCategory)"
              effect="dark"
              size="small"
              round
            >
              {{ getCategoryLabel(row.projectCategory) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="项目类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :color="PROJECT_TYPE_MAP[row.projectType]?.color"
              effect="dark"
              size="small"
              round
              :style="PROJECT_TYPE_MAP[row.projectType]?.color ? { color: '#fff', borderColor: PROJECT_TYPE_MAP[row.projectType]?.color } : {}"
            >
              {{ getTypeLabel(row.projectType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="项目预算" width="130" align="right">
          <template #default="{ row }">
            {{ formatBudget(row.budget) }}
          </template>
        </el-table-column>
        <el-table-column label="完成进度" width="160">
          <template #default="{ row }">
            <ProgressCell :percentage="row.progress ?? 0" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <StatusBadge :status="row.status" :type-map="PROJECT_STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row.id)">查看</el-button>
            <el-popconfirm title="确定删除该项目？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        :prev-text="'上一页'"
        :next-text="'下一页'"
        @current-change="fetchData"
        @size-change="fetchData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { projectApi } from '@/api/project'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusBadge from '@/components/common/StatusBadge.vue'
import ProgressCell from '@/components/common/ProgressCell.vue'
import { PROJECT_STATUS_MAP, PROJECT_CATEGORY_MAP, PROJECT_TYPE_MAP } from '@/constants/status-maps'
import { formatBudgetWanYuan } from '@/utils/budget'
import type { ProjectInfo } from '@/types/project'

const router = useRouter()
const loading = ref(false)
const tableData = ref<ProjectInfo[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])
const dateRange = ref<[string, string] | null>(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  projectName: '',
  status: '',
  projectCategory: '',
  projectType: '',
  createTimeStart: '',
  createTimeEnd: '',
})

function formatBudget(value?: number): string {
  return formatBudgetWanYuan(value)
}

function formatTime(value?: string): string {
  if (!value) return '-'
  // 处理ISO格式：2026-04-15T03:18:36.000+00:00 -> 2026-04-15 11:18:36
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

function getCategoryLabel(key: string): string {
  return PROJECT_CATEGORY_MAP[key]?.label || key
}

function getCategoryTagType(key: string): '' | 'success' | 'warning' | 'info' {
  const map: Record<string, '' | 'success' | 'warning' | 'info'> = {
    LIMITED_BELOW: '',
    PROPERTY_TRADE: 'warning',
    GOVERNMENT_PROCUREMENT: 'success',
  }
  return map[key] || 'info'
}

function getTypeLabel(key: string): string {
  return PROJECT_TYPE_MAP[key]?.label || key
}

function buildQueryParams() {
  if (dateRange.value) {
    queryParams.createTimeStart = dateRange.value[0]
    queryParams.createTimeEnd = dateRange.value[1]
  } else {
    queryParams.createTimeStart = ''
    queryParams.createTimeEnd = ''
  }
}

async function fetchData() {
  loading.value = true
  buildQueryParams()
  try {
    const result = await projectApi.getList(queryParams)
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
  queryParams.projectName = ''
  queryParams.status = ''
  queryParams.projectCategory = ''
  queryParams.projectType = ''
  queryParams.createTimeStart = ''
  queryParams.createTimeEnd = ''
  dateRange.value = null
  handleSearch()
}

function handleCreate() {
  router.push('/project/create')
}

function handleView(id: number) {
  router.push(`/project/${id}`)
}


function handleSelectionChange(rows: ProjectInfo[]) {
  selectedIds.value = rows.map(r => r.id)
}

async function handleDelete(id: number) {
  try {
    await projectApi.deleteByIds([id])
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    ElMessage.error('删除失败')
  }
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.length} 个项目？`,
      '批量删除确认',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' },
    )
    await projectApi.deleteByIds(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    fetchData()
  } catch {
    // 用户取消
  }
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.project-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-section,
.table-section {
  background: var(--app-bg-primary);
  border-radius: var(--app-radius-sm);
  padding: 20px;
  border: 1px solid var(--app-border-light);
}

.filter-header,
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h3 {
    margin: 0;
    font-size: 16px;
    color: var(--app-text-primary);
  }
}

.table-actions {
  display: flex;
  gap: 8px;
}

.search-form {
  margin-bottom: 0;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
