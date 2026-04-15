<template>
  <div class="project-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>项目管理</h3>
          <el-button type="primary" @click="handleCreate">新建项目</el-button>
        </div>
      </template>

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
            <StatusBadge :status="row.projectCategory" :type-map="PROJECT_CATEGORY_MAP" />
          </template>
        </el-table-column>
        <el-table-column label="项目类型" width="100" align="center">
          <template #default="{ row }">
            <StatusBadge :status="row.projectType" :type-map="PROJECT_TYPE_MAP" />
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
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
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
        @current-change="fetchData"
        @size-change="fetchData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { projectApi } from '@/api/project'
import { ElMessage } from 'element-plus'
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

onMounted(fetchData)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-form {
  margin-bottom: 20px;
}
.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
