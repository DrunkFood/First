<template>
  <div class="header-shell panel-card">
    <div class="header-left">
      <el-button circle class="ghost-btn" @click="emit('toggleCollapse')">
        <el-icon><Fold /></el-icon>
      </el-button>
      <el-button circle class="ghost-btn mobile-only" @click="emit('toggleMobile')">
        <el-icon><Menu /></el-icon>
      </el-button>

      <div class="title-block">
        <div class="title-main">{{ currentTitle }}</div>
        <div class="title-sub">{{ fullPathLabel }}</div>
      </div>
    </div>

    <div class="header-right">
      <el-tag type="success" effect="dark" round>在线</el-tag>
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="34" :icon="UserFilled" />
          <div class="user-text">
            <div class="name">{{ userStore.realName || userStore.username }}</div>
            <div class="role">系统管理员</div>
          </div>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="password">
              <el-icon><Lock /></el-icon>
              修改密码
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>

  <el-dialog v-model="passwordDialogVisible" title="修改密码" width="420px">
    <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
      <el-form-item label="旧密码" prop="oldPassword">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="passwordForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Fold, Menu, UserFilled, ArrowDown, Lock, SwitchButton } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/store/user'

defineProps<{
  isCollapse: boolean
  isMobileOpen: boolean
}>()

const emit = defineEmits<{
  toggleCollapse: []
  toggleMobile: []
}>()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const breadcrumbs = computed(() => {
  return route.matched
    .filter((item) => item.meta?.title && item.path !== '/')
    .map((item) => ({ path: item.path, title: String(item.meta.title) }))
})

const currentTitle = computed(() => {
  const items = breadcrumbs.value
  return items[items.length - 1]?.title || '控制台'
})

const fullPathLabel = computed(() => breadcrumbs.value.map((b) => b.title).join(' / ') || '系统首页')

const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirmPassword = (_: unknown, value: string, callback: (err?: Error) => void) => {
  if (value !== passwordForm.newPassword) callback(new Error('两次密码输入不一致'))
  else callback()
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const handleCommand = (command: string) => {
  if (command === 'password') {
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordDialogVisible.value = true
    return
  }
  if (command === 'logout') handleLogout()
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return
    passwordLoading.value = true
    try {
      await authApi.changePassword(passwordForm.oldPassword, passwordForm.newPassword)
      ElMessage.success('密码已修改，请重新登录')
      passwordDialogVisible.value = false
      userStore.logout()
    } finally {
      passwordLoading.value = false
    }
  })
}

const handleLogout = () => {
  ElMessageBox.confirm('确认退出当前登录状态？', '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    userStore.logout()
    router.push('/login')
  })
}
</script>

<style scoped lang="scss">
.header-shell {
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background: linear-gradient(90deg, #ffffff 0, #f7fbf9 100%);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ghost-btn {
  border: 1px solid #d7e2dc;
  background: #f5faf7;
}

.mobile-only {
  display: none;
}

.title-main {
  font-size: 16px;
  font-weight: 700;
  color: #112a1f;
}

.title-sub {
  font-size: 12px;
  color: #678377;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 10px;
}

.user-info:hover {
  background: #eef6f2;
}

.user-text .name {
  line-height: 1.1;
  font-weight: 600;
  color: #1f372c;
}

.user-text .role {
  font-size: 11px;
  color: #6d887d;
}

@media (max-width: 960px) {
  .mobile-only {
    display: inline-flex;
  }

  .title-sub,
  .user-text .role {
    display: none;
  }

  .user-text .name {
    font-size: 13px;
  }
}
</style>
