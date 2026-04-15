<template>
  <el-dialog
    :model-value="modelValue"
    title="版本对比"
    fullscreen
    @update:model-value="$emit('update:modelValue', $event)"
    @open="handleOpen"
  >
    <div class="version-compare">
      <div class="compare-header">
        <div class="version-select">
          <span class="select-label">版本 A：</span>
          <el-select v-model="versionAId" placeholder="选择版本" style="width: 220px" @change="handleSelectChange">
            <el-option
              v-for="v in versions"
              :key="v.id"
              :label="`v${v.versionNo} - ${v.createName} (${v.createTime})`"
              :value="v.id"
            />
          </el-select>
        </div>
        <div class="version-select">
          <span class="select-label">版本 B：</span>
          <el-select v-model="versionBId" placeholder="选择版本" style="width: 220px" @change="handleSelectChange">
            <el-option
              v-for="v in versions"
              :key="v.id"
              :label="`v${v.versionNo} - ${v.createName} (${v.createTime})`"
              :value="v.id"
            />
          </el-select>
        </div>
      </div>

      <div v-if="versionAId && versionBId" class="compare-content">
        <div class="compare-pane">
          <h4>{{ versionALabel }}</h4>
          <div class="diff-content" v-html="diffHtmlA"></div>
        </div>
        <div class="compare-divider" />
        <div class="compare-pane">
          <h4>{{ versionBLabel }}</h4>
          <div class="diff-content" v-html="diffHtmlB"></div>
        </div>
      </div>

      <el-empty v-else description="请选择两个版本进行对比" />

      <div class="compare-legend">
        <span class="legend-item legend-add">新增内容</span>
        <span class="legend-item legend-modify">修改内容</span>
        <span class="legend-item legend-delete">删除内容</span>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { projectApi } from '@/api/project'
import type { ProjectVersionInfo } from '@/types/project'

const props = defineProps<{
  modelValue: boolean
  projectId: number
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const versions = ref<ProjectVersionInfo[]>([])
const versionAId = ref<number>()
const versionBId = ref<number>()

const versionAMap = computed(() => {
  const map = new Map<number, ProjectVersionInfo>()
  versions.value.forEach(v => map.set(v.id, v))
  return map
})

const versionALabel = computed(() => {
  const v = versionAMap.value.get(versionAId.value!)
  return v ? `v${v.versionNo}` : ''
})

const versionBLabel = computed(() => {
  const v = versionAMap.value.get(versionBId.value!)
  return v ? `v${v.versionNo}` : ''
})

const diffHtmlA = computed(() => {
  if (!versionAId.value) return ''
  const v = versionAMap.value.get(versionAId.value)
  return v ? escapeHtml(v.changeDescription || '无变更描述') : ''
})

const diffHtmlB = computed(() => {
  if (!versionBId.value) return ''
  const v = versionAMap.value.get(versionBId.value)
  return v ? escapeHtml(v.changeDescription || '无变更描述') : ''
})

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br/>')
}

async function handleOpen() {
  if (!props.projectId) return
  try {
    versions.value = await projectApi.getVersions(props.projectId)
  } catch {
    versions.value = []
  }
}

function handleSelectChange() {
  // 触发 computed 重新计算
}
</script>

<style scoped>
.version-compare {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.compare-header {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
  align-items: center;
}
.version-select {
  display: flex;
  align-items: center;
  gap: 8px;
}
.select-label {
  font-weight: 500;
  white-space: nowrap;
}
.compare-content {
  display: flex;
  flex: 1;
  gap: 0;
  min-height: 400px;
}
.compare-pane {
  flex: 1;
  padding: 16px;
  background: var(--el-bg-color-page);
  border-radius: 8px;
  overflow: auto;
}
.compare-pane h4 {
  margin: 0 0 12px;
  color: var(--el-text-color-primary);
}
.compare-divider {
  width: 2px;
  background: var(--el-border-color);
  margin: 0 12px;
}
.diff-content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
}
.compare-legend {
  display: flex;
  gap: 20px;
  margin-top: 16px;
  padding: 12px 16px;
  background: var(--el-bg-color-page);
  border-radius: 4px;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.legend-item::before {
  content: '';
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 3px;
}
.legend-add::before {
  background: #e1f3d8;
  border: 1px solid #67c23a;
}
.legend-modify::before {
  background: #faecd8;
  border: 1px solid #e6a23c;
}
.legend-delete::before {
  background: #fde2e2;
  border: 1px solid #f56c6c;
}
</style>
