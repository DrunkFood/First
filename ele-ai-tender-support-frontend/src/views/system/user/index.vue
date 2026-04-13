<template>
  <div class="page-shell user-management">
    <div class="page-header-row">
      <div>
        <div class="page-title">用户管理</div>
        <div class="page-subtitle">维护系统用户信息、状态与角色分配</div>
      </div>
    </div>

    <el-card shadow="never" class="panel-card">
      <div class="search-wrap">
        <el-form :inline="true" :model="queryParams" class="search-form">
          <el-form-item label="用户名">
            <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="queryParams.realName" placeholder="请输入姓名" clearable />
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
          新增用户
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="角色" min-width="190">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role.id" size="small" class="tag-gap">
              {{ role.roleName }}
            </el-tag>
          </template>
        </el-table-column>
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
            <el-button type="warning" link @click="handleResetPassword(row)">重置密码</el-button>
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
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%;">
            <el-option
              v-for="role in roleList"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { userApi, type CreateUserParams, type UpdateUserParams } from '@/api/user'
import { roleApi } from '@/api/role'
import type { UserInfo, RoleInfo } from '@/types'

const loading = ref(false)
const tableData = ref<UserInfo[]>([])
const total = ref(0)
const roleList = ref<RoleInfo[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  realName: '',
  status: undefined as number | undefined,
})

const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  roleIds: [] as number[],
  status: 1,
})

const dialogTitle = computed(() => (form.id ? '编辑用户' : '新增用户'))

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleIds: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await userApi.getList(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('Fetch users failed:', error)
  } finally {
    loading.value = false
  }
}

const fetchRoles = async () => {
  try {
    const res = await roleApi.getAll()
    roleList.value = res.data
  } catch (error) {
    console.error('Fetch roles failed:', error)
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handleReset = () => {
  queryParams.username = ''
  queryParams.realName = ''
  queryParams.status = undefined
  handleSearch()
}

const handleAdd = () => {
  form.id = undefined
  form.username = ''
  form.password = ''
  form.realName = ''
  form.email = ''
  form.phone = ''
  form.roleIds = []
  form.status = 1
  dialogVisible.value = true
}

const handleEdit = (row: UserInfo) => {
  form.id = row.id
  form.username = row.username
  form.password = ''
  form.realName = row.realName
  form.email = row.email || ''
  form.phone = row.phone || ''
  form.roleIds = row.roleIds || []
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
        const params: UpdateUserParams = {
          id: form.id,
          realName: form.realName,
          email: form.email,
          phone: form.phone,
          roleIds: form.roleIds,
          status: form.status,
        }
        await userApi.update(params)
        ElMessage.success('更新成功')
      } else {
        const params: CreateUserParams = {
          username: form.username,
          password: form.password,
          realName: form.realName,
          email: form.email,
          phone: form.phone,
          roleIds: form.roleIds,
        }
        await userApi.create(params)
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

const handleResetPassword = (row: UserInfo) => {
  ElMessageBox.prompt('请输入新密码', '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputType: 'password',
    inputPattern: /^.{6,20}$/,
    inputErrorMessage: '密码长度在 6 到 20 个字符',
  }).then(async (result) => {
    const { value } = result as { value: string }
    await userApi.resetPassword(row.id, value)
    ElMessage.success('密码重置成功')
  })
}

const handleDelete = (row: UserInfo) => {
  ElMessageBox.confirm(`确定要删除用户 "${row.username}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await userApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

onMounted(() => {
  fetchData()
  fetchRoles()
})
</script>

<style scoped lang="scss">
.user-management {
  .search-form {
    row-gap: 8px;
  }

  .tag-gap {
    margin-right: 4px;
    margin-bottom: 4px;
  }
}
</style>
