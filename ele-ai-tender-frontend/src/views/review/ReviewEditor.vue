<template>
  <div class="review-editor">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>评审项管理</h3>
          <el-button type="primary" @click="handleAdd">新增评审项</el-button>
        </div>
      </template>

      <el-table :data="treeData" row-key="id" :tree-props="{ children: 'children' }" v-loading="loading">
        <el-table-column prop="itemName" label="评审项名称" />
        <el-table-column prop="level" label="层级" width="80" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { reviewApi } from '@/api/review'
import { ElMessage } from 'element-plus'
import type { ReviewItemTree } from '@/types/review'

const route = useRoute()
const loading = ref(false)
const treeData = ref<ReviewItemTree[]>([])

async function fetchData() {
  loading.value = true
  try {
    treeData.value = await reviewApi.getTree(Number(route.params.projectId))
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  ElMessage.info('评审项新增功能待实现')
}

function handleEdit(_row: ReviewItemTree) {
  ElMessage.info('评审项编辑功能待实现')
}

async function handleDelete(row: ReviewItemTree) {
  try {
    await reviewApi.deleteById(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(fetchData)
</script>

<style scoped>
.review-editor {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
