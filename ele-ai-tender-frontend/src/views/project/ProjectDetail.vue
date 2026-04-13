<template>
  <div class="project-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>项目详情</h3>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="项目编号">{{ project?.projectCode }}</el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ project?.projectName }}</el-descriptions-item>
        <el-descriptions-item label="项目类别">{{ project?.projectCategory }}</el-descriptions-item>
        <el-descriptions-item label="项目类型">{{ project?.projectType }}</el-descriptions-item>
        <el-descriptions-item label="预算金额">{{ project?.budget }} 万元</el-descriptions-item>
        <el-descriptions-item label="评审类型">{{ project?.reviewType }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ project?.status }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ project?.createName }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ project?.createTime }}</el-descriptions-item>
      </el-descriptions>

      <el-tabs class="detail-tabs">
        <el-tab-pane label="版本历史">
          <el-empty description="暂无版本记录" />
        </el-tab-pane>
        <el-tab-pane label="需求信息">
          <el-empty description="暂无需求信息" />
        </el-tab-pane>
        <el-tab-pane label="评审项">
          <el-empty description="暂无评审项" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { projectApi } from '@/api/project'

const router = useRouter()
const route = useRoute()
const project = ref<any>(null)

onMounted(async () => {
  const id = Number(route.params.id)
  project.value = await projectApi.getById(id)
})
</script>

<style scoped>
.project-detail {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.detail-tabs {
  margin-top: 20px;
}
</style>
