<template>
  <div class="detection-report">
    <h4>检测报告</h4>
    <div v-if="report" class="report-summary">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="总问题数">{{ report.totalIssueCount }}</el-descriptions-item>
        <el-descriptions-item label="总体状态">
          <el-tag :type="report.overallStatus === 'PASSED' ? 'success' : 'danger'" size="small">
            {{ report.overallStatus === 'PASSED' ? '通过' : '未通过' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-button type="primary" class="accept-all-btn" @click="$emit('accept-all')">
        一键接受所有建议
      </el-button>

      <el-table :data="report.issues" stripe class="issue-table">
        <el-table-column prop="typeName" label="检测类型" width="120" />
        <el-table-column prop="description" label="问题描述" />
        <el-table-column prop="location" label="位置" width="150" />
        <el-table-column prop="suggestion" label="建议" />
        <el-table-column prop="severity" label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag :type="row.severity === 'HIGH' ? 'danger' : row.severity === 'MEDIUM' ? 'warning' : 'info'" size="small">
              {{ row.severity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="$emit('accept', row.recordId)">接受</el-button>
            <el-button text size="small" @click="$emit('reject', row.recordId)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-empty v-else description="暂无检测报告" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { detectionApi } from '@/api/detection'
import type { DetectionReportVO } from '@/types/detection'

const props = defineProps<{ projectId: number }>()
defineEmits<{ accept: [recordId: number]; reject: [recordId: number]; 'accept-all': [] }>()

const report = ref<DetectionReportVO | null>(null)

onMounted(async () => {
  report.value = await detectionApi.getReport(props.projectId)
})
</script>

<style scoped>
.detection-report h4 {
  margin-bottom: 16px;
}

.accept-all-btn {
  margin: 12px 0;
}

.issue-table {
  margin-top: 12px;
}
</style>
