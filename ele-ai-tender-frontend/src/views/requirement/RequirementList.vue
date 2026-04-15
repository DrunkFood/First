<template>
  <div class="requirement-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>需求编制</h3>
          <el-button type="primary" @click="handleCreate">新建需求</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="需求名称">
          <el-input v-model="queryParams.requirementName" placeholder="请输入需求名称" clearable />
        </el-form-item>
        <el-form-item label="需求状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option
              v-for="(item, key) in REQUIREMENT_STATUS_MAP"
              :key="key"
              :label="item.label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="项目类型">
          <el-select v-model="queryParams.projectType" placeholder="请选择项目类型" clearable>
            <el-option
              v-for="(item, key) in PROJECT_TYPE_MAP"
              :key="key"
              :label="item.label"
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
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="requirementName" label="需求名称" min-width="160">
          <template #default="{ row }">
            <el-link type="primary" @click="handleView(row.id)">{{ row.requirementName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="projectType" label="项目类型" width="120">
          <template #default="{ row }">
            <StatusBadge :status="row.projectType" :type-map="PROJECT_TYPE_MAP" />
          </template>
        </el-table-column>
        <el-table-column prop="budget" label="项目预算" width="130" align="right">
          <template #default="{ row }">
            {{ formatBudget(row.budget) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <StatusBadge :status="row.status" :type-map="REQUIREMENT_STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="完成进度" width="180">
          <template #default="{ row }">
            <ProgressCell :percentage="row.progress ?? 0" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row.id)">查看</el-button>
            <el-button link type="primary" @click="handleGenerate(row.id)">生成</el-button>
            <el-button link type="primary" @click="handleDetect(row.id)">检测</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        @current-change="fetchData"
        @size-change="fetchData"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { requirementApi } from '@/api/requirement'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusBadge from '@/components/common/StatusBadge.vue'
import ProgressCell from '@/components/common/ProgressCell.vue'
import { REQUIREMENT_STATUS_MAP, PROJECT_TYPE_MAP } from '@/constants/status-maps'
import type { RequirementQueryParams } from '@/types/requirement'

const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)

const queryParams = reactive<RequirementQueryParams>({
  pageNum: 1,
  pageSize: 10,
  requirementName: '',
  status: '',
  projectType: '',
  createTimeStart: '',
  createTimeEnd: '',
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
  router.push(`/requirement/edit/${id}`)
}

function handleGenerate(id: number) {
  router.push(`/requirement/generate/${id}`)
}

function handleDetect(id: number) {
  router.push(`/requirement/detect/${id}`)
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
    // 用户取消或删除失败，不处理
  }
}

function formatBudget(value?: number): string {
  if (value == null) return '-'
  return `¥${value.toLocaleString('zh-CN')}`
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
