<template>
  <div class="message-center">
    <el-page-header content="消息中心" />

    <div class="toolbar">
      <el-button @click="handleMarkAllRead">全部已读</el-button>
      <el-tag>未读: {{ unreadCount }}</el-tag>
    </div>

    <el-table :data="messages" v-loading="loading" stripe>
      <el-table-column prop="title" label="标题">
        <template #default="{ row }">
          <span :class="{ unread: row.isRead === 0 }">{{ row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="messageType" label="类型" width="100" />
      <el-table-column prop="createTime" label="时间" width="170" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { messageApi } from '@/api/message'
import type { MessageVO } from '@/types/message'

const messages = ref<MessageVO[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const unreadCount = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const res = await messageApi.getMyMessages({ pageNum: pageNum.value, pageSize: pageSize.value })
    messages.value = res.records
    total.value = res.total
    unreadCount.value = await messageApi.getUnreadCount()
  } finally {
    loading.value = false
  }
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

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px 0;
}

.unread {
  font-weight: bold;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
