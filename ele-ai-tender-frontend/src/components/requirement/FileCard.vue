<template>
  <div
    class="file-card"
    :class="{ 'is-selected': selected }"
    @click="handleSelect"
  >
    <div class="file-card-header">
      <el-icon :size="16"><Document /></el-icon>
      <span class="file-name" :title="file.fileName">{{ file.fileName }}</span>
    </div>
    <div class="file-card-meta">
      <el-tag size="small" :type="getFileTypeTagType(file.fileType)">{{ file.fileType }}</el-tag>
      <span v-if="file.budget" class="file-budget">{{ formatBudget(file.budget) }}</span>
    </div>
    <div v-if="matchPercent != null" class="file-card-match">
      <el-progress
        :percentage="matchPercent"
        :stroke-width="6"
        :color="getMatchColor(matchPercent)"
        class="match-progress"
      />
      <span class="match-text" :style="{ color: getMatchColor(matchPercent) }">
        {{ matchPercent }}% 匹配
      </span>
    </div>
    <p v-if="file.matchDesc" class="file-card-desc">{{ file.matchDesc }}</p>
    <div class="file-card-actions">
      <el-button size="small" @click.stop="$emit('preview', file)">预览</el-button>
      <el-button
        v-if="selectable"
        size="small"
        :type="selected ? 'primary' : 'default'"
        @click.stop="handleSelect"
      >
        {{ selected ? '已选择' : '选择' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Document } from '@element-plus/icons-vue'
import { formatBudgetWanYuan } from '@/utils/budget'
import type { MatchFile } from '@/types/requirement'

const props = withDefaults(defineProps<{
  file: MatchFile
  matchPercent?: number
  selectable?: boolean
  selected?: boolean
  selectionMode?: 'single' | 'multi'
}>(), {
  selectable: true,
  selected: false,
  selectionMode: 'single',
})

const emit = defineEmits<{
  select: [file: MatchFile]
  preview: [file: MatchFile]
}>()

function handleSelect() {
  if (props.selectable) {
    emit('select', props.file)
  }
}

function getFileTypeTagType(fileType: string): '' | 'success' | 'warning' | 'info' {
  const map: Record<string, '' | 'success' | 'warning' | 'info'> = {
    '工程类': '',
    '货物类': 'warning',
    '服务类': 'success',
  }
  return map[fileType] || 'info'
}

function getMatchColor(percent: number): string {
  if (percent >= 80) return 'var(--app-color-success)'
  if (percent >= 50) return 'var(--app-color-warning)'
  return 'var(--app-color-danger)'
}

function formatBudget(yuan: number): string {
  return formatBudgetWanYuan(yuan)
}
</script>

<style scoped>
.file-card {
  background: var(--app-bg-elevated);
  border: 1px solid var(--app-border-medium);
  border-radius: var(--app-radius-sm);
  padding: 16px;
  cursor: pointer;
  transition: var(--app-transition-base);
}

.file-card:hover {
  box-shadow: var(--app-shadow-sm);
  border-color: var(--app-brand-color);
}

.file-card.is-selected {
  border-color: var(--app-brand-color);
  background: var(--app-brand-color-light, rgba(51, 108, 255, 0.05));
}

.file-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--app-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.file-card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.file-budget {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.file-card-match {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.match-progress {
  flex: 1;
}

.match-text {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.file-card-desc {
  font-size: 13px;
  color: var(--app-text-tertiary);
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.file-card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
