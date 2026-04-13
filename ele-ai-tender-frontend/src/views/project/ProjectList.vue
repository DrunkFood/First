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
        <el-form-item label="项目名称">
          <el-input v-model="queryParams.projectName" placeholder="请输入项目名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="草稿" value="DRAFT" />
            <el-option label="编制中" value="IN_PROGRESS" />
            <el-option label="已发布" value="PUBLISHED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="projectCode" label="项目编号" />
        <el-table-column prop="projectName" label="项目名称" />
        <el-table-column prop="projectCategory" label="项目类别" />
        <el-table-column prop="projectType" label="项目类型" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row.id)">查看</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
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

const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  projectName: '',
  status: '',
})

async function fetchData() {
  loading.value = true
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
  handleSearch()
}

function handleCreate() {
  router.push('/project/create')
}

function handleView(id: number) {
  router.push(`/project/${id}`)
}

function handleEdit(row: any) {
  router.push(`/project/create?id=${row.id}`)
}

async function handleDelete(id: number) {
  try {
    await projectApi.deleteByIds([id])
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
