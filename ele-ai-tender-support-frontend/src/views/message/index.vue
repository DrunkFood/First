<template>
  <div class="page-shell message-page">
    <div class="page-header-row">
      <div>
        <div class="page-title">消息中心</div>
        <div class="page-subtitle">查看系统通知、审核通知和检测预警消息</div>
      </div>
      <el-button type="primary" @click="handleMarkAllRead">全部已读</el-button>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="search-wrap">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="消息类型">
            <el-select v-model="queryParams.messageType" placeholder="请选择" clearable style="width: 160px">
              <el-option label="系统通知" value="SYSTEM" />
              <el-option label="审核通知" value="AUDIT" />
              <el-option label="检测通知" value="DETECTION" />
              <el-option label="预警通知" value="WARNING" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.isRead" placeholder="请选择" clearable style="width: 120px">
              <el-option label="未读" :value="0" />
              <el-option label="已读" :value="1" />
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
        <el-table-column prop="title" label="消息标题" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span :style="{ fontWeight: row.isRead === 0 ? '600' : 'normal' }">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="messageType" label="消息类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTagMap[row.messageType] || 'info'" size="small">
              {{ typeLabel(row.messageType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isRead" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isRead === 1 ? 'info' : 'danger'" size="small">
              {{ row.isRead === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
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

    <el-drawer v-model="detailVisible" title="消息详情" size="500px">
      <template v-if="currentMsg">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">{{ currentMsg.title }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ typeLabel(currentMsg.messageType) }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatDateTime(currentMsg.createTime) }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 16px; padding: 14px; background: #f5f7fa; border-radius: 8px; line-height: 1.8">
          {{ currentMsg.content || '无内容' }}
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import type { MessageInfo } from '@/types/message'
import { messageApi } from '@/api/message'

const loading = ref(false)
const tableData = ref<MessageInfo[]>([])
const total = ref(0)
const detailVisible = ref(false)
const currentMsg = ref<MessageInfo | null>(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  messageType: '',
  isRead: undefined as number | undefined,
})

const typeMap: Record<string, string> = { SYSTEM: '系统通知', AUDIT: '审核通知', DETECTION: '检测通知', WARNING: '预警通知' }
const typeTagMap: Record<string, string> = { SYSTEM: '', AUDIT: 'success', DETECTION: 'warning', WARNING: 'danger' }
const typeLabel = (code?: string) => (code ? typeMap[code] || code : '-')

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
    const res = await messageApi.getList(queryParams)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('Fetch messages failed:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handleReset = () => {
  queryParams.messageType = ''
  queryParams.isRead = undefined
  handleSearch()
}

const handleView = async (row: MessageInfo) => {
  currentMsg.value = row
  detailVisible.value = true
  if (row.isRead === 0) {
    try {
      await messageApi.markRead(row.id)
      row.isRead = 1
    } catch (e) {
      console.error('Mark read failed:', e)
    }
  }
}

const handleMarkAllRead = async () => {
  try {
    await messageApi.markAllRead()
    ElMessage.success('已全部标记为已读')
    fetchData()
  } catch (error) {
    console.error('Mark all read failed:', error)
  }
}

const handleDelete = (row: MessageInfo) => {
  ElMessageBox.confirm('确定删除该消息？', '提示', { type: 'warning' }).then(async () => {
    try {
      await messageApi.deleteById(row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (error) {
      console.error('Delete message failed:', error)
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchData()
})
</script>
