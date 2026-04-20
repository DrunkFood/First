<template>
  <div class="match-mode-panel">
    <label class="match-mode-label">
      匹配模式<span class="required">*</span>
    </label>
    <el-radio-group :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" class="mode-group">
      <el-radio label="AUTO_MATCH">系统自动匹配</el-radio>
      <el-radio :label="mode === 'create' ? 'MANUAL_SELECT' : 'SYSTEM_SELECT'">
        {{ mode === 'create' ? '手动选择' : '系统匹配用户选择' }}
      </el-radio>
      <el-radio label="UPLOAD">{{ mode === 'create' ? '上传' : '本地上传' }}</el-radio>
    </el-radio-group>

    <!-- 自动匹配 -->
    <div v-if="modelValue === 'AUTO_MATCH'" class="mode-content auto-match-content">
      <span class="auto-match-text">系统将根据历史信息匹配，生成业务需求</span>
      <el-button
        v-if="mode === 'edit'"
        type="primary"
        :loading="matching"
        @click="handleStartMatch"
      >
        开始匹配
      </el-button>
    </div>

    <!-- 手动选择 / 系统匹配用户选择 -->
    <div v-if="showManualSelect" class="mode-content">
      <div v-if="loadingFiles" class="loading-tip">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在加载匹配文件...</span>
      </div>
      <template v-else-if="(matchFiles ?? []).length > 0">
        <div class="file-list">
          <FileCard
            v-for="file in (matchFiles ?? [])"
            :key="file.id"
            :file="file"
            :match-percent="file.matchPercent"
            :selectable="true"
            :selected="isFileSelected(file.id)"
            :selection-mode="mode === 'create' ? 'single' : 'multi'"
            @select="handleFileSelect"
            @preview="$emit('file-preview', $event)"
          />
        </div>
        <p class="file-hint">
          {{ mode === 'create' ? '展示历史文件库中匹配度高的文件，仅可单选' : '勾选需要参考的历史文件，可多选' }}
        </p>
      </template>
      <el-empty v-else description="暂无匹配文件" :image-size="60" />
    </div>

    <!-- 上传 -->
    <div v-if="modelValue === 'UPLOAD'" class="mode-content">
      <el-upload
        :action="uploadAction"
        :headers="uploadHeaders"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload"
        :file-list="fileList"
        :limit="uploadLimit"
        :on-exceed="handleExceed"
        drag
      >
        <el-icon :size="40" class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="upload-tip">
            支持 {{ uploadAccept === '.doc,.docx' ? 'doc/docx' : 'doc/docx/pdf' }} 格式，{{
              uploadLimit === 1 ? '单个文件' : '单个文件'
            }}不超过 50MB
          </div>
        </template>
      </el-upload>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, type UploadFile, type UploadFiles, type UploadRawFile } from 'element-plus'
import { Loading, UploadFilled } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'
import { requirementApi } from '@/api/requirement'
import FileCard from './FileCard.vue'
import type { MatchFile } from '@/types/requirement'

const props = withDefaults(defineProps<{
  modelValue: string
  matchFiles?: MatchFile[]
  selectedFileId?: number
  selectedFileIds?: number[]
  uploadAccept?: string
  uploadLimit?: number
  mode: 'create' | 'edit'
  requirementId?: number
}>(), {
  uploadAccept: '.doc,.docx',
  uploadLimit: 1,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:selectedFileId': [id: number | undefined]
  'update:selectedFileIds': [ids: number[]]
  'file-preview': [file: MatchFile]
}>()

const loadingFiles = ref(false)
const matching = ref(false)
const internalFiles = ref<MatchFile[]>([])
const fileList = ref<UploadFile[]>([])

const uploadAction = '/file-api/api/v1/file/upload'
const uploadHeaders = computed(() => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const showManualSelect = computed(() => {
  return props.modelValue === 'MANUAL_SELECT' || props.modelValue === 'SYSTEM_SELECT'
})

// 加载匹配文件
watch(() => props.modelValue, async (mode) => {
  if ((mode === 'MANUAL_SELECT' || mode === 'SYSTEM_SELECT') && !props.matchFiles?.length) {
    await fetchMatchFiles()
  }
})

async function fetchMatchFiles() {
  loadingFiles.value = true
  try {
    const files = await requirementApi.getMatchFiles({
      requirementId: props.requirementId,
    })
    internalFiles.value = files || []
  } catch {
    internalFiles.value = []
  } finally {
    loadingFiles.value = false
  }
}

async function handleStartMatch() {
  matching.value = true
  try {
    await fetchMatchFiles()
    ElMessage.success('匹配完成')
  } catch {
    ElMessage.error('匹配失败')
  } finally {
    matching.value = false
  }
}

function isFileSelected(fileId: number): boolean {
  if (props.mode === 'create') {
    return props.selectedFileId === fileId
  }
  return props.selectedFileIds?.includes(fileId) ?? false
}

function handleFileSelect(file: MatchFile) {
  if (props.mode === 'create') {
    emit('update:selectedFileId', props.selectedFileId === file.id ? undefined : file.id)
  } else {
    const ids = [...(props.selectedFileIds || [])]
    const idx = ids.indexOf(file.id)
    if (idx >= 0) {
      ids.splice(idx, 1)
    } else {
      ids.push(file.id)
    }
    emit('update:selectedFileIds', ids)
  }
}

function beforeUpload(file: UploadRawFile) {
  const allowedExts = props.uploadAccept.split(',').map(e => e.trim())
  const ext = '.' + file.name.split('.').pop()?.toLowerCase()
  const isAllowedType = allowedExts.includes(ext) || allowedExts.some(e => file.type && file.type.includes(e.replace('.', '')))
  const isLt50M = file.size / 1024 / 1024 < 50

  if (!isAllowedType) {
    ElMessage.error(`仅支持 ${props.uploadAccept} 格式文件`)
    return false
  }
  if (!isLt50M) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  return true
}

function handleUploadSuccess(response: any, _file: UploadFile, _files: UploadFiles) {
  const fileId = response?.data?.id || response?.data?.fileId
  if (fileId) {
    ElMessage.success('文件上传成功')
  } else {
    ElMessage.error('上传返回数据异常，未获取到文件ID')
  }
}

function handleUploadError() {
  ElMessage.error('文件上传失败，请重试')
}

function handleExceed() {
  ElMessage.warning(`仅允许上传${props.uploadLimit}个文件，请先删除已上传文件`)
}
</script>

<style scoped>
.match-mode-panel {
  margin-top: 0;
}

.match-mode-label {
  display: block;
  font-size: 13px;
  color: var(--app-text-secondary);
  margin-bottom: 12px;
}

.match-mode-label .required {
  color: var(--el-color-danger);
  margin-left: 2px;
}

.mode-group {
  display: flex;
  flex-direction: row;
  gap: 24px;
}

.mode-content {
  margin-top: 16px;
  padding: 16px;
  background: var(--app-bg-tertiary);
  border-radius: 6px;
}

.auto-match-content {
  background: var(--app-bg-tertiary);
}

.auto-match-text {
  font-size: 13px;
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.loading-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: var(--app-text-tertiary);
}

.file-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}

.file-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--app-text-tertiary);
}

.upload-icon {
  color: var(--app-text-tertiary);
}

.upload-text {
  font-size: 14px;
  color: var(--app-text-secondary);
}

.upload-text em {
  color: var(--app-brand-color);
  font-style: normal;
}

.upload-tip {
  font-size: 12px;
  color: var(--app-text-tertiary);
  margin-top: 4px;
}
</style>
