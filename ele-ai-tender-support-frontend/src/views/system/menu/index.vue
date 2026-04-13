<template>
  <div class="page-shell menu-management">
    <div class="page-header-row">
      <div>
        <div class="page-title">菜单权限</div>
        <div class="page-subtitle">维护导航树与按钮级权限标识</div>
      </div>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="toolbar-wrap">
        <el-button type="primary" @click="handleAdd()">
          <el-icon><Plus /></el-icon>
          新增菜单
        </el-button>
        <el-button @click="toggleExpandAll">
          {{ isExpandAll ? '全部收起' : '全部展开' }}
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        row-key="id"
        border
        :default-expand-all="isExpandAll"
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="220" />
        <el-table-column prop="icon" label="图标" width="80">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="menuType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getMenuTypeTag(row.menuType)">
              {{ getMenuTypeLabel(row.menuType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="菜单URL" min-width="180" />
        <el-table-column prop="permission" label="权限标识" min-width="180" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.menuType !== 2" type="primary" link @click="handleAdd(row)">新增</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="menuTreeWithRoot"
            :props="{ label: 'menuName', value: 'id' }"
            check-strictly
            placeholder="请选择上级菜单"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="菜单URL" prop="path">
          <el-input v-model="form.path" placeholder="请输入菜单URL" />
        </el-form-item>
        <el-form-item label="权限标识" prop="permission">
          <el-input v-model="form.permission" placeholder="请输入权限标识" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="请输入图标名（可选）" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item v-if="form.id" label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { menuApi, type CreateMenuParams, type UpdateMenuParams } from '@/api/menu'
import type { MenuInfo } from '@/types'

const loading = ref(false)
const tableData = ref<MenuInfo[]>([])
const isExpandAll = ref(true)

const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  parentId: 0,
  menuName: '',
  menuType: 1,
  path: '',
  permission: '',
  icon: '',
  sortOrder: 0,
  status: 1,
})

const dialogTitle = computed(() => (form.id ? '编辑菜单' : '新增菜单'))

const menuTreeWithRoot = computed(() => [{ id: 0, menuName: '根目录', children: tableData.value }])

const rules: FormRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path: [{ required: true, message: '请输入菜单URL', trigger: 'blur' }],
}

const getMenuTypeLabel = (type: number) => {
  const labels: Record<number, string> = { 1: '菜单', 2: '按钮' }
  return labels[type] || `未知(${type})`
}

const getMenuTypeTag = (type: number) => {
  const tags: Record<number, string> = { 1: 'success', 2: 'warning' }
  return tags[type] || 'info'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await menuApi.getTree()
    tableData.value = res.data
  } catch (error) {
    console.error('Fetch menus failed:', error)
  } finally {
    loading.value = false
  }
}

const toggleExpandAll = () => {
  isExpandAll.value = !isExpandAll.value
  fetchData()
}

const handleAdd = (parent?: MenuInfo) => {
  form.id = undefined
  form.parentId = parent?.id || 0
  form.menuName = ''
  form.menuType = parent?.menuType === 1 ? 2 : 1
  form.path = ''
  form.permission = ''
  form.icon = ''
  form.sortOrder = 0
  form.status = 1
  dialogVisible.value = true
}

const handleEdit = (row: MenuInfo) => {
  form.id = row.id
  form.parentId = row.parentId
  form.menuName = row.menuName
  form.menuType = row.menuType
  form.path = row.path || ''
  form.permission = row.permission || ''
  form.icon = row.icon || ''
  form.sortOrder = row.sortOrder
  form.status = row.status
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      if (form.id) {
        const params: UpdateMenuParams = {
          id: form.id,
          parentId: form.parentId,
          menuName: form.menuName,
          menuType: form.menuType,
          path: form.path,
          permission: form.permission,
          icon: form.icon,
          sortOrder: form.sortOrder,
          status: form.status,
        }
        await menuApi.update(params)
        ElMessage.success('更新成功')
      } else {
        const params: CreateMenuParams = {
          parentId: form.parentId,
          menuName: form.menuName,
          menuType: form.menuType,
          path: form.path,
          permission: form.permission,
          icon: form.icon,
          sortOrder: form.sortOrder,
        }
        await menuApi.create(params)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchData()
    } catch (error) {
      console.error('Submit failed:', error)
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDelete = (row: MenuInfo) => {
  ElMessageBox.confirm(`确定要删除菜单 "${row.menuName}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await menuApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

onMounted(() => {
  fetchData()
})
</script>
