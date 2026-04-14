<template>
  <div class="page-shell operation-log-page">
    <div class="page-header-row">
      <div>
        <div class="page-title">操作日志</div>
        <div class="page-subtitle">记录系统中用户的关键操作行为</div>
      </div>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="search-wrap">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="操作用户">
            <el-input v-model="queryParams.userName" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="操作类型">
            <el-input v-model="queryParams.operation" placeholder="如：创建用户" clearable />
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
        <el-table-column prop="userName" label="操作用户" min-width="120" />
        <el-table-column prop="operation" label="操作类型" min-width="150" />
        <el-table-column prop="method" label="请求方法" min-width="280" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" min-width="140" />
        <el-table-column prop="executeTime" label="耗时(ms)" width="100" />
        <el-table-column prop="createTime" label="操作时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDetail(row)">详情</el-button>
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

    <el-drawer v-model="detailVisible" title="操作详情" size="600px">
      <template v-if="currentLog">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="操作用户">{{ currentLog.userName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">{{ currentLog.operation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求方法">{{ currentLog.method || '-' }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ currentLog.ip || '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ currentLog.executeTime ?? '-' }} ms</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ formatDateTime(currentLog.createTime) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="currentLog.params" style="margin-top: 16px">
          <div style="font-weight: 600; margin-bottom: 8px">请求参数</div>
          <pre class="detail-content">{{ currentLog.params }}</pre>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import type { OperationLogInfo } from '@/types/operation-log'
import { operationLogApi } from '@/api/operation-log'

const loading = ref(false)
const tableData = ref<OperationLogInfo[]>([])
const total = ref(0)
const detailVisible = ref(false)
const currentLog = ref<OperationLogInfo | null>(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  userName: '',
  operation: '',
})

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}:${s}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await operationLogApi.getList(queryParams)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('Fetch operation logs failed:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handleReset = () => {
  queryParams.userName = ''
  queryParams.operation = ''
  handleSearch()
}

const handleViewDetail = (row: OperationLogInfo) => {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.detail-content {
  margin: 0;
  padding: 14px;
  border-radius: 8px;
  background: #f5f7fa;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 300px;
  overflow: auto;
}
</style>
