<template>
  <div class="page-shell access-log-management">
    <div class="page-header-row">
      <div>
        <div class="page-title">访问日志</div>
        <div class="page-subtitle">追踪系统请求、交互链路和业务上下文</div>
      </div>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="search-wrap">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="TraceId">
            <el-input v-model="queryParams.traceId" placeholder="请输入 TraceId" clearable />
          </el-form-item>
          <el-form-item label="服务名称">
            <el-select v-model="queryParams.serviceName" placeholder="请选择服务" clearable style="width: 180px">
              <el-option label="支撑中心" value="ele-tender-support" />
              <el-option label="文件服务" value="ele-tender-file" />
              <el-option label="招标文件编制" value="ele-tender-tender-document" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态码">
            <el-select v-model="queryParams.statusCode" placeholder="请选择状态码" clearable style="width: 140px">
              <el-option label="200" :value="200" />
              <el-option label="400" :value="400" />
              <el-option label="401" :value="401" />
              <el-option label="403" :value="403" />
              <el-option label="404" :value="404" />
              <el-option label="500" :value="500" />
            </el-select>
          </el-form-item>
          <el-form-item label="业务类型">
            <el-input v-model="queryParams.bizType" placeholder="如 1 / 2" clearable />
          </el-form-item>
          <el-form-item label="业务ID">
            <el-input v-model="queryParams.bizId" placeholder="请输入业务ID" clearable />
          </el-form-item>
          <el-form-item label="项目ID">
            <el-input v-model="queryParams.projectId" placeholder="请输入项目ID" clearable />
          </el-form-item>
          <el-form-item label="标段ID">
            <el-input v-model="queryParams.tenderId" placeholder="请输入标段ID" clearable />
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="timeRange"
              type="datetimerange"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              range-separator="至"
            />
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
        <el-table-column prop="traceId" label="TraceId" min-width="220" show-overflow-tooltip />
        <el-table-column prop="serviceName" label="服务名称" min-width="170" />
        <el-table-column prop="httpMethod" label="方法" width="90" />
        <el-table-column prop="requestUri" label="请求地址" min-width="260" show-overflow-tooltip />
        <el-table-column prop="statusCode" label="状态码" width="90" />
        <el-table-column prop="successFlag" label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.successFlag === 1 ? 'success' : 'danger'">
              {{ row.successFlag === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="elapsedMs" label="耗时(ms)" width="100" />
        <el-table-column prop="userName" label="用户" min-width="120" show-overflow-tooltip />
        <el-table-column prop="bizId" label="业务ID" min-width="140" show-overflow-tooltip />
        <el-table-column prop="projectId" label="项目ID" min-width="140" show-overflow-tooltip />
        <el-table-column prop="tenderId" label="标段ID" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createTime" label="记录时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
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

    <el-drawer v-model="detailVisible" title="日志详情" size="720px">
      <template v-if="currentLog">
        <el-descriptions :column="2" border class="detail-block">
          <el-descriptions-item label="TraceId">{{ currentLog.traceId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="服务名称">{{ currentLog.serviceName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求方法">{{ currentLog.httpMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态码">{{ currentLog.statusCode ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务类型">{{ currentLog.bizType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务ID">{{ currentLog.bizId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目ID">{{ currentLog.projectId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="标段ID">{{ currentLog.tenderId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="文件ID">{{ currentLog.fileId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="文件名称">{{ currentLog.fileName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ currentLog.userName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="企业">{{ currentLog.enterpriseName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时(ms)">{{ currentLog.elapsedMs ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="记录时间">{{ formatDateTime(currentLog.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="请求地址" :span="2">{{ currentLog.requestUri || '-' }}</el-descriptions-item>
          <el-descriptions-item label="异常类型">{{ currentLog.errorType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="异常信息">{{ currentLog.errorMessage || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-block">
          <div class="block-title">请求头摘要</div>
          <pre class="detail-content">{{ currentLog.requestHeaders || '-' }}</pre>
        </div>
        <div class="detail-block">
          <div class="block-title">请求体摘要</div>
          <pre class="detail-content">{{ currentLog.requestBody || '-' }}</pre>
        </div>
        <div class="detail-block">
          <div class="block-title">响应体摘要</div>
          <pre class="detail-content">{{ currentLog.responseBody || '-' }}</pre>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import type { AccessLogInfo } from '@/types'
import { accessLogApi } from '@/api/access-log'

const loading = ref(false)
const tableData = ref<AccessLogInfo[]>([])
const total = ref(0)
const timeRange = ref<[string, string] | []>([])

const detailVisible = ref(false)
const currentLog = ref<AccessLogInfo | null>(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  traceId: '',
  serviceName: '',
  statusCode: undefined as number | undefined,
  bizType: '',
  bizId: '',
  projectId: '',
  tenderId: '',
})

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await accessLogApi.getList({
      ...queryParams,
      startTime: timeRange.value[0],
      endTime: timeRange.value[1],
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('Fetch access logs failed:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handleReset = () => {
  queryParams.traceId = ''
  queryParams.serviceName = ''
  queryParams.statusCode = undefined
  queryParams.bizType = ''
  queryParams.bizId = ''
  queryParams.projectId = ''
  queryParams.tenderId = ''
  timeRange.value = []
  handleSearch()
}

const handleViewDetail = (row: AccessLogInfo) => {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.detail-block + .detail-block {
  margin-top: 18px;
}

.block-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2a37;
  margin-bottom: 10px;
}

.detail-content {
  margin: 0;
  padding: 14px;
  border-radius: 14px;
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
  color: #dbeafe;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 220px;
  overflow: auto;
}
</style>
