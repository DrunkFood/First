<template>
  <div class="policy-file-select">
    <h4>选择政策文件</h4>
    <p class="hint">选择用于检测的政策文件（平台文件默认勾选）</p>
    <el-checkbox-group v-model="selected">
      <div v-for="file in files" :key="file.id" class="file-item">
        <el-checkbox :value="file.id">
          <span>{{ file.fileName }}</span>
          <el-tag :type="file.source === 'SYSTEM' ? '' : 'success'" size="small" class="source-tag">
            {{ file.source === 'SYSTEM' ? '平台' : '用户' }}
          </el-tag>
        </el-checkbox>
      </div>
    </el-checkbox-group>
    <el-empty v-if="!files.length" description="暂无可用政策文件" :image-size="60" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { policyFileApi } from '@/api/policy-file'
import type { PolicyFileVO } from '@/types/policy-file'

const props = defineProps<{
  applicableCategory?: string
}>()

const selected = defineModel<number[]>({ default: () => [] })
const files = ref<PolicyFileVO[]>([])

const loadFiles = async () => {
  files.value = await policyFileApi.getAllAvailable(props.applicableCategory)
  // 默认勾选平台文件
  selected.value = files.value
    .filter(f => f.source === 'SYSTEM')
    .map(f => f.id)
}

watch(() => props.applicableCategory, loadFiles)
onMounted(loadFiles)
</script>

<style scoped>
.policy-file-select h4 {
  margin-bottom: 8px;
}

.hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-bottom: 12px;
}

.file-item {
  margin-bottom: 8px;
}

.source-tag {
  margin-left: 8px;
}
</style>
