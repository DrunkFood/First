<template>
  <div class="page-shell knowledge-management">
    <div class="page-header-row">
      <div>
        <div class="page-title">知识库管理</div>
        <div class="page-subtitle">管理知识库文档，支持向量化检索</div>
      </div>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="search-wrap">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="文档名称">
            <el-input v-model="queryParams.documentName" placeholder="请输入文档名称" clearable />
          </el-form-item>
          <el-form-item label="类别">
            <el-input v-model="queryParams.documentCategory" placeholder="请输入文档类别" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
              <el-option label="正常" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="toolbar-wrap">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增文档
        </el-button>
        <el-button
          type="danger"
          :disabled="selectedIds.length === 0"
          @click="handleBatchDelete"
        >
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="documentName" label="文档名称" min-width="180" />
        <el-table-column prop="documentCategory" label="类别" width="120" />
        <el-table-column prop="fileType" label="文件类型" width="100" />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="vectorCollection" label="向量集合" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.vectorCollection" type="success" size="small">已向量化</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small">正常</el-tag>
            <el-tag v-else type="danger" size="small">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createName" label="创建人" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="文档名称" prop="documentName">
          <el-input v-model="formData.documentName" placeholder="请输入文档名称" />
        </el-form-item>
        <el-form-item label="文档类别" prop="documentCategory">
          <el-input v-model="formData.documentCategory" placeholder="请输入文档类别" />
        </el-form-item>
        <el-form-item label="文件类型" prop="fileType">
          <el-select v-model="formData.fileType" placeholder="请选择文件类型" style="width: 100%">
            <el-option label="PDF" value="PDF" />
            <el-option label="Word" value="WORD" />
            <el-option label="TXT" value="TXT" />
            <el-option label="Markdown" value="MARKDOWN" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件URL">
          <el-input v-model="formData.fileUrl" placeholder="请输入文件URL（可选）" />
        </el-form-item>
        <el-form-item label="文档内容">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="8"
            placeholder="请输入文档内容（可选）"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import { knowledgeApi } from '@/api/knowledge'
import type {
  KnowledgeDocumentInfo,
  KnowledgeDocumentQueryParams,
  KnowledgeDocumentCreateParams,
  KnowledgeDocumentUpdateParams,
} from '@/types/knowledge'

const loading = ref(false)
const tableData = ref<KnowledgeDocumentInfo[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])

const queryParams = reactive<KnowledgeDocumentQueryParams>({
  pageNum: 1,
  pageSize: 10,
  documentName: undefined,
  documentCategory: undefined,
  status: undefined,
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive<KnowledgeDocumentCreateParams & { id?: number; fileUrl?: string }>({
  documentName: '',
  documentCategory: '',
  fileType: 'PDF',
  fileUrl: '',
  content: '',
  remark: '',
})

const formRules = reactive<FormRules>({
  documentName: [{ required: true, message: '请输入文档名称', trigger: 'blur' }],
  documentCategory: [{ required: true, message: '请输入文档类别', trigger: 'blur' }],
  fileType: [{ required: true, message: '请选择文件类型', trigger: 'change' }],
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await knowledgeApi.getList(queryParams)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    console.error('获取知识库列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchList()
}

const handleReset = () => {
  queryParams.documentName = undefined
  queryParams.documentCategory = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  fetchList()
}

const handleAdd = () => {
  dialogTitle.value = '新增文档'
  dialogVisible.value = true
}

const handleEdit = (row: KnowledgeDocumentInfo) => {
  dialogTitle.value = '编辑文档'
  Object.assign(formData, {
    id: row.id,
    documentName: row.documentName,
    documentCategory: row.documentCategory,
    fileType: row.fileType,
    fileUrl: row.fileUrl,
    content: row.content,
    remark: row.remark,
  })
  dialogVisible.value = true
}

const handleDelete = async (row: KnowledgeDocumentInfo) => {
  try {
    await ElMessageBox.confirm(`确定删除文档"${row.documentName}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await knowledgeApi.deleteById(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除文档失败:', error)
    }
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个文档吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await knowledgeApi.deleteByIds(selectedIds.value)
    ElMessage.success('批量删除成功')
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除文档失败:', error)
    }
  }
}

const handleSelectionChange = (selection: KnowledgeDocumentInfo[]) => {
  selectedIds.value = selection.map((item) => item.id)
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      if (formData.id) {
        await knowledgeApi.update(formData as KnowledgeDocumentUpdateParams)
        ElMessage.success('更新成功')
      } else {
        await knowledgeApi.create(formData)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchList()
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDialogClosed = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: undefined,
    documentName: '',
    documentCategory: '',
    fileType: 'PDF',
    fileUrl: '',
    content: '',
    remark: '',
  })
}

const formatFileSize = (bytes: number): string => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(1) + ' ' + sizes[i]
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
@import '@/assets/styles/index.scss';
</style>
