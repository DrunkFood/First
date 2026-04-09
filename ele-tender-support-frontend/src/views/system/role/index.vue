<template>
  <div class="page-shell role-management">
    <div class="page-header-row">
      <div>
        <div class="page-title">角色管理</div>
        <div class="page-subtitle">配置角色身份、权限集合与启停状态</div>
      </div>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="search-wrap">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="角色名称">
            <el-input v-model="queryParams.roleName" placeholder="请输入角色名称" clearable />
          </el-form-item>
          <el-form-item label="角色编码">
            <el-input v-model="queryParams.roleCode" placeholder="请输入角色编码" clearable />
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
          新增角色
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleName" label="角色名称" min-width="160" />
        <el-table-column prop="roleCode" label="角色编码" min-width="170" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link @click="handleAssignMenus(row)">分配权限</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="!!form.id" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item v-if="form.id" label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="menuDialogVisible" title="分配权限" width="500px">
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        :props="{ label: 'menuName', children: 'children' }"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedMenuIds"
        :check-strictly="false"
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="menuSubmitLoading" @click="handleSubmitMenus">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { roleApi, type CreateRoleParams, type UpdateRoleParams } from '@/api/role'
import { menuApi } from '@/api/menu'
import type { RoleInfo, MenuInfo } from '@/types'

const loading = ref(false)
const tableData = ref<RoleInfo[]>([])
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  roleName: '',
  roleCode: '',
  status: undefined as number | undefined,
})

const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  roleName: '',
  roleCode: '',
  description: '',
  status: 1,
})

const dialogTitle = computed(() => (form.id ? '编辑角色' : '新增角色'))

const rules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z_]+$/, message: '角色编码只能包含大写字母和下划线', trigger: 'blur' },
  ],
}

const menuDialogVisible = ref(false)
const menuSubmitLoading = ref(false)
const menuTreeRef = ref()
const menuTree = ref<MenuInfo[]>([])
const checkedMenuIds = ref<number[]>([])
const currentRoleId = ref<number>()

const fetchData = async () => {
  loading.value = true
  try {
    const res = await roleApi.getList(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('Fetch roles failed:', error)
  } finally {
    loading.value = false
  }
}

const fetchMenuTree = async () => {
  try {
    const res = await menuApi.getTree()
    menuTree.value = res.data
  } catch (error) {
    console.error('Fetch menu tree failed:', error)
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handleReset = () => {
  queryParams.roleName = ''
  queryParams.roleCode = ''
  queryParams.status = undefined
  handleSearch()
}

const handleAdd = () => {
  form.id = undefined
  form.roleName = ''
  form.roleCode = ''
  form.description = ''
  form.status = 1
  dialogVisible.value = true
}

const handleEdit = (row: RoleInfo) => {
  form.id = row.id
  form.roleName = row.roleName
  form.roleCode = row.roleCode
  form.description = row.description || ''
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
        const params: UpdateRoleParams = {
          id: form.id,
          roleName: form.roleName,
          description: form.description,
          status: form.status,
        }
        await roleApi.update(params)
        ElMessage.success('更新成功')
      } else {
        const params: CreateRoleParams = {
          roleName: form.roleName,
          roleCode: form.roleCode,
          description: form.description,
          menuIds: [],
        }
        await roleApi.create(params)
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

const handleAssignMenus = async (row: RoleInfo) => {
  currentRoleId.value = row.id
  try {
    const res = await roleApi.getRoleMenus(row.id)
    checkedMenuIds.value = res.data
    menuDialogVisible.value = true
  } catch (error) {
    console.error('Fetch role menus failed:', error)
  }
}

const handleSubmitMenus = async () => {
  if (!currentRoleId.value) return

  menuSubmitLoading.value = true
  try {
    const checkedKeys = menuTreeRef.value?.getCheckedKeys() || []
    const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() || []
    const menuIds = [...checkedKeys, ...halfCheckedKeys]

    await roleApi.assignMenus(currentRoleId.value, menuIds)
    ElMessage.success('权限分配成功')
    menuDialogVisible.value = false
  } catch (error) {
    console.error('Assign menus failed:', error)
  } finally {
    menuSubmitLoading.value = false
  }
}

const handleDelete = (row: RoleInfo) => {
  ElMessageBox.confirm(`确定要删除角色 "${row.roleName}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await roleApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

onMounted(() => {
  fetchData()
  fetchMenuTree()
})
</script>

<style scoped lang="scss">
.role-management {
  .search-form {
    row-gap: 8px;
  }
}
</style>
