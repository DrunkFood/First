<template>
  <div class="page-shell sys-param-page">
    <div class="page-header-row">
      <div>
        <div class="page-title">系统参数</div>
        <div class="page-subtitle">管理系统运行参数、功能开关和AI检测规则</div>
      </div>
    </div>

    <el-card shadow="never" class="panel-card">
      <el-tabs v-model="activeGroup" @tab-change="onGroupChange">
        <el-tab-pane label="系统参数" name="SYSTEM" />
        <el-tab-pane label="功能开关" name="SWITCH" />
        <el-tab-pane label="AI检测规则" name="AI_RULE" />
      </el-tabs>

      <div v-loading="loading" class="param-list">
        <el-form label-width="180px" style="max-width: 700px; margin-top: 16px">
          <el-form-item
            v-for="param in paramList"
            :key="param.paramKey"
            :label="param.paramName"
          >
            <!-- 布尔类型用 Switch -->
            <el-switch
              v-if="param.paramType === 'BOOLEAN'"
              v-model="editValues[param.paramKey]"
              active-value="true"
              inactive-value="false"
            />
            <!-- 数字类型用 InputNumber -->
            <el-input-number
              v-else-if="param.paramType === 'NUMBER'"
              v-model.number="editValues[param.paramKey]"
              :min="0"
              controls-position="right"
            />
            <!-- JSON 类型用 Textarea -->
            <el-input
              v-else-if="param.paramType === 'JSON'"
              v-model="editValues[param.paramKey]"
              type="textarea"
              :rows="4"
            />
            <!-- 默认字符串类型 -->
            <el-input v-else v-model="editValues[param.paramKey]" />
            <div v-if="param.description" class="param-desc">{{ param.description }}</div>
          </el-form-item>
        </el-form>

        <div v-if="paramList.length > 0" style="margin-top: 20px; padding-left: 180px">
          <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { SysParamInfo } from '@/types/sys-param'
import { sysParamApi } from '@/api/sys-param'

const activeGroup = ref('SYSTEM')
const loading = ref(false)
const saving = ref(false)
const paramList = ref<SysParamInfo[]>([])
const editValues = reactive<Record<string, string>>({})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await sysParamApi.getList(activeGroup.value)
    paramList.value = res.data || []
    // 初始化编辑值
    for (const param of paramList.value) {
      editValues[param.paramKey] = param.paramValue || ''
    }
  } catch (error) {
    console.error('Fetch sys params failed:', error)
  } finally {
    loading.value = false
  }
}

const onGroupChange = () => {
  fetchData()
}

const handleSave = async () => {
  saving.value = true
  try {
    // 收集变更的参数
    const changed: Record<string, string> = {}
    for (const param of paramList.value) {
      const newVal = String(editValues[param.paramKey] ?? '')
      if (newVal !== (param.paramValue || '')) {
        changed[param.paramKey] = newVal
      }
    }
    if (Object.keys(changed).length === 0) {
      ElMessage.info('没有参数被修改')
      return
    }
    await sysParamApi.batchUpdate(changed)
    ElMessage.success('保存成功')
    fetchData()
  } catch (error) {
    console.error('Save sys params failed:', error)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.param-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style>
