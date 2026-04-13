<template>
  <div class="requirement-editor">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>编辑需求</h3>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <el-form :model="form" label-width="100px">
        <el-form-item label="需求名称">
          <el-input v-model="form.requirementName" />
        </el-form-item>
        <el-form-item label="需求内容">
          <el-input v-model="form.content" type="textarea" :rows="15" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { requirementApi } from '@/api/requirement'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const saving = ref(false)
const form = reactive({
  id: 0,
  requirementName: '',
  content: '',
})

onMounted(async () => {
  const id = Number(route.params.id)
  const data = await requirementApi.getById(id)
  form.id = data.id
  form.requirementName = data.requirementName
  form.content = data.content || ''
})

async function handleSave() {
  saving.value = true
  try {
    await requirementApi.update(form.id, { content: form.content, requirementName: form.requirementName })
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.requirement-editor {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
