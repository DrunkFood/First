<template>
  <div class="phase-document">
    <!-- 政策文件匹配推荐 -->
    <el-divider content-position="left">政策文件匹配推荐</el-divider>
    <div class="policy-match-section">
      <div class="policy-match-toolbar">
        <el-button type="primary" :loading="suggestingPolicy" @click="handleSuggestPolicy">
          AI推荐政策文件
        </el-button>
        <el-button @click="loadPolicyFiles">刷新政策文件列表</el-button>
      </div>

      <el-table
        v-if="policyFiles.length"
        :data="policyFiles"
        stripe
        max-height="240"
        class="policy-table"
        @selection-change="handlePolicySelect"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="fileName" label="文件名称" show-overflow-tooltip />
        <el-table-column prop="fileCategory" label="文件类别" width="120" />
        <el-table-column prop="applicableCategory" label="适用类别" width="120" />
        <el-table-column prop="source" label="来源" width="80">
          <template #default="{ row }">
            <el-tag :type="row.source === 'SYSTEM' ? '' : 'success'" size="small">
              {{ row.source === 'SYSTEM' ? '平台' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="AI推荐" width="80">
          <template #default="{ row }">
            <el-tag v-if="isAiRecommended(row.id)" type="success" size="small">推荐</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无可用政策文件" :image-size="60" />
    </div>

    <!-- 文档集成操作 -->
    <el-divider content-position="left">文档集成</el-divider>
    <div class="doc-toolbar">
      <el-button type="primary" :loading="isIntegrating" @click="handleIntegrate">
        执行文档集成
      </el-button>
      <el-button :disabled="!preview?.integrated" @click="handleExport">
        导出Word
      </el-button>
    </div>

    <div v-if="preview?.integrated" class="doc-preview">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="HTML预览" name="html">
          <div class="html-preview" v-html="preview.htmlContent" />
        </el-tab-pane>
        <el-tab-pane label="Markdown编辑" name="markdown">
          <MarkdownEditor v-model="markdownContent" />
          <el-button type="primary" class="save-btn" @click="handleSaveEdit">
            保存修改
          </el-button>
        </el-tab-pane>
        <el-tab-pane label="变量替换" name="variables">
          <div class="variable-section">
            <p class="variable-hint">以下变量将从项目信息中自动填充，您也可以手动修改</p>
            <el-form label-width="140px" class="variable-form">
              <el-form-item v-for="v in variables" :key="v.key" :label="v.label">
                <el-input v-model="v.value" :placeholder="`请输入${v.label}`" />
              </el-form-item>
            </el-form>
            <el-button type="primary" @click="handleApplyVariables">应用变量替换</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-empty v-else description="请先执行文档集成" />

    <div class="phase-actions">
      <el-button @click="$emit('prev')">上一步</el-button>
      <el-button type="primary" :disabled="!preview?.integrated" @click="$emit('next')">
        继续到检测
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { documentApi } from '@/api/document'
import { policyFileApi } from '@/api/policy-file'
import { aiApi } from '@/api/ai'
import { projectApi } from '@/api/project'
import type { DocumentPreviewVO } from '@/types/document'
import type { PolicyFileVO } from '@/types/policy-file'
import type { ProjectInfo } from '@/types/project'
import MarkdownEditor from '@/components/editor/MarkdownEditor.vue'

const props = defineProps<{ projectId: number }>()
defineEmits<{ next: []; prev: [] }>()

const preview = ref<DocumentPreviewVO | null>(null)
const markdownContent = ref('')
const activeTab = ref('html')
const isIntegrating = ref(false)

// --- 政策文件匹配 ---
const policyFiles = ref<PolicyFileVO[]>([])
const selectedPolicyIds = ref<number[]>([])
const aiRecommendedIds = ref<number[]>([])
const suggestingPolicy = ref(false)
const project = ref<ProjectInfo | null>(null)

const loadPolicyFiles = async () => {
  try {
    policyFiles.value = await policyFileApi.getAllAvailable(project.value?.projectCategory)
  } catch {
    ElMessage.error('获取政策文件列表失败')
  }
}

const handlePolicySelect = (selection: PolicyFileVO[]) => {
  selectedPolicyIds.value = selection.map(s => s.id)
}

const isAiRecommended = (id: number) => aiRecommendedIds.value.includes(id)

const handleSuggestPolicy = async () => {
  if (!project.value) {
    ElMessage.warning('项目信息加载中，请稍候')
    return
  }
  suggestingPolicy.value = true
  try {
    const res = await aiApi.suggest({
      content: project.value.projectDescription || project.value.projectName,
      type: 'policy',
      projectId: props.projectId,
    })
    // suggestions 返回的是政策文件ID列表或名称列表
    if (res.suggestions?.length) {
      // 尝试将建议匹配到政策文件ID
      const suggestedIds = res.suggestions
        .map(s => {
          const num = Number(s)
          return isNaN(num) ? null : num
        })
        .filter((id): id is number => id !== null)
      aiRecommendedIds.value = suggestedIds
      ElMessage.success(`AI推荐了 ${suggestedIds.length} 个政策文件`)
    } else {
      ElMessage.info('暂无推荐政策文件')
    }
  } catch {
    ElMessage.error('AI推荐失败')
  } finally {
    suggestingPolicy.value = false
  }
}

// --- 变量替换 ---
interface VariableItem {
  key: string
  label: string
  value: string
}

const variables = ref<VariableItem[]>([])

const initVariables = () => {
  if (!project.value) return
  variables.value = [
    { key: 'projectName', label: '项目名称', value: project.value.projectName || '' },
    { key: 'projectCategory', label: '项目类别', value: project.value.projectCategory || '' },
    { key: 'projectType', label: '项目类型', value: project.value.projectType || '' },
    { key: 'budget', label: '预算金额(万元)', value: project.value.budget ? String(project.value.budget) : '' },
    { key: 'tenderUnit', label: '招标单位', value: project.value.tenderUnit || '' },
    { key: 'contactPerson', label: '联系人', value: project.value.contactPerson || '' },
    { key: 'contactPhone', label: '联系电话', value: project.value.contactPhone || '' },
    { key: 'projectDescription', label: '项目描述', value: project.value.projectDescription || '' },
  ]
}

const handleApplyVariables = () => {
  // 将变量值替换到markdown内容中的{{变量名}}占位符
  let result = markdownContent.value
  for (const v of variables.value) {
    const regex = new RegExp(`\\{\\{${v.key}\\}\\}`, 'g')
    result = result.replace(regex, v.value)
  }
  markdownContent.value = result
  ElMessage.success('变量替换完成，请检查内容后保存')
  activeTab.value = 'markdown'
}

// --- 文档操作 ---
const loadPreview = async () => {
  preview.value = await documentApi.getPreview(props.projectId)
  if (preview.value?.markdownContent) {
    markdownContent.value = preview.value.markdownContent
  }
}

const handleIntegrate = async () => {
  isIntegrating.value = true
  try {
    preview.value = await documentApi.integrate(props.projectId)
    if (preview.value?.markdownContent) {
      markdownContent.value = preview.value.markdownContent
    }
    ElMessage.success('文档集成完成')
  } catch {
    ElMessage.error('文档集成失败')
  } finally {
    isIntegrating.value = false
  }
}

const handleExport = async () => {
  try {
    const blob = await documentApi.exportWord(props.projectId) as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '招标文件.docx'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

const handleSaveEdit = async () => {
  await documentApi.editContent(props.projectId, markdownContent.value)
  ElMessage.success('保存成功')
  await loadPreview()
  activeTab.value = 'html'
}

onMounted(async () => {
  project.value = await projectApi.getById(props.projectId)
  initVariables()
  await Promise.all([loadPreview(), loadPolicyFiles()])
})
</script>

<style scoped>
.policy-match-section {
  margin-bottom: 16px;
}

.policy-match-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.policy-table {
  margin-bottom: 12px;
}

.doc-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.html-preview {
  padding: 20px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  min-height: 400px;
  background: #fff;
}

.save-btn {
  margin-top: 12px;
}

.variable-section {
  padding: 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
}

.variable-hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-bottom: 16px;
}

.variable-form {
  max-width: 600px;
}

.phase-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
