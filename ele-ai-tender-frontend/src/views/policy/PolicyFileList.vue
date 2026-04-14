<template>
  <div class="policy-file-list">
    <el-page-header content="政策文件管理" />

    <div class="toolbar">
      <el-button type="primary" @click="showCreateDialog = true">上传政策文件</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="fileName" label="文件名" />
      <el-table-column prop="fileCategory" label="文件分类" width="120" />
      <el-table-column prop="applicableCategory" label="适用类别" width="120" />
      <el-table-column prop="source" label="来源" width="80">
        <template #default="{ row }">
          <el-tag :type="row.source === 'SYSTEM' ? '' : 'success'" size="small">
            {{ row.source === 'SYSTEM' ? '平台' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button
            v-if="row.source === 'USER'"
            text
            type="danger"
            size="small"
            @click="handleDelete(row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      class="pagination"
      @current-change="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { policyFileApi } from '@/api/policy-file'
import type { PolicyFileVO } from '@/types/policy-file'

const tableData = ref<PolicyFileVO[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const showCreateDialog = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await policyFileApi.getList({ pageNum: pageNum.value, pageSize: pageSize.value })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定删除该政策文件？', '确认')
  await policyFileApi.deleteById(id)
  ElMessage.success('删除成功')
  await loadData()
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  margin: 16px 0;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
