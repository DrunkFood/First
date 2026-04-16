<template>
  <div class="message-center">
    <el-page-header content="消息中心" />

    <!-- 消息分类 Tab -->
    <el-tabs v-model="activeTab" class="message-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="ALL" />
      <el-tab-pane label="系统通知" name="SYSTEM" />
      <el-tab-pane label="检测通知" name="DETECTION" />
      <el-tab-pane label="项目通知" name="PROJECT" />
    </el-tabs>

    <div class="toolbar">
      <el-button @click="handleMarkAllRead">全部已读</el-button>
      <el-tag>未读: {{ unreadCount }}</el-tag>
    </div>

    <el-table :data="messages" v-loading="loading" stripe>
      <el-table-column prop="title" label="标题">
        <template #default="{ row }">
          <el-link :underline="false" :class="{ unread: row.isRead === 0 }" @click="handleViewMessage(row)">
            {{ row.title }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="messageType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="getMessageTagType(row.messageType)">
            {{ getMessageTypeLabel(row.messageType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isRead === 1 ? 'info' : 'danger'" size="small">
            {{ row.isRead === 1 ? '已读' : '未读' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button v-if="row.isRead === 0" text size="small" @click="handleMarkRead(row.id)">
            标记已读
          </el-button>
          <el-button text type="danger" size="small" @click="handleDelete(row.id)">
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

    <!-- 消息详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="消息详情" width="500px">
      <template v-if="currentMessage">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">{{ currentMessage.title }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag size="small" :type="getMessageTagType(currentMessage.messageType)">
              {{ getMessageTypeLabel(currentMessage.messageType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatTime(currentMessage.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="内容">
            <div class="message-content">{{ currentMessage.content }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { messageApi } from '@/api/message'
import type { MessageVO } from '@/types/message'
import { MESSAGE_TYPES } from '@/types/message'

const messages = ref<MessageVO[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const unreadCount = ref(0)
const activeTab = ref('ALL')

// --- 消息详情 ---
const showDetailDialog = ref(false)
const currentMessage = ref<MessageVO | null>(null)

const loadData = async () => {
  loading.value = true
  try {
    const params: { pageNum: number; pageSize: number; messageType?: string } = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    }
    if (activeTab.value !== 'ALL') {
      params.messageType = activeTab.value
    }
    const res = await messageApi.getMyMessages(params)
    messages.value = res.records
    total.value = res.total
    unreadCount.value = await messageApi.getUnreadCount()
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  pageNum.value = 1
  loadData()
}

const handleMarkRead = async (id: number) => {
  await messageApi.markRead(id)
  await loadData()
}

const handleMarkAllRead = async () => {
  await messageApi.markAllRead()
  ElMessage.success('已全部标记为已读')
  await loadData()
}

const handleDelete = async (id: number) => {
  await messageApi.deleteById(id)
  ElMessage.success('删除成功')
  await loadData()
}

const handleViewMessage = async (row: MessageVO) => {
  currentMessage.value = row
  showDetailDialog.value = true
  // 自动标记已读
  if (row.isRead === 0) {
    await messageApi.markRead(row.id)
    row.isRead = 1
    unreadCount.value = await messageApi.getUnreadCount()
  }
}

const getMessageTypeLabel = (type: string): string => {
  return MESSAGE_TYPES.find(t => t.value === type)?.label ?? type
}

const getMessageTagType = (type: string): '' | 'success' | 'warning' | 'info' | 'danger' => {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    SYSTEM: '',
    DETECTION: 'warning',
    PROJECT: 'success',
  }
  return map[type] ?? 'info'
}

/** 格式化ISO时间为友好显示 */
function formatTime(value: string): string {
  if (!value) return '-'
  const d = new Date(value)
  if (isNaN(d.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onMounted(loadData)
</script>

<style scoped>
.message-tabs {
  margin-top: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px 0;
}

.unread {
  font-weight: bold;
  cursor: pointer;
}

.message-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
