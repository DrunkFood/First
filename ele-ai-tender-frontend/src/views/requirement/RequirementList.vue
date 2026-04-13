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
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="requirementName" label="需求名称" />
        <el-table-column prop="projectCategory" label="项目类别" />
        <el-table-column prop="projectType" label="项目类型" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row.id)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
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
        @current-change="fetchData"
        @size-change="fetchData"
        layout="total, prev, pager, next"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { requirementApi } from '@/api/requirement'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  requirementName: '',
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
  handleSearch()
}

function handleCreate() {
  router.push('/requirement/create')
}

function handleEdit(id: number) {
  router.push(`/requirement/edit/${id}`)
}

async function handleDelete(id: number) {
  try {
    await requirementApi.deleteById(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
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
