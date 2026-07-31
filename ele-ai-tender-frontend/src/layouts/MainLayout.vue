<template>
  <el-container class="main-layout">

    <el-header class="main-header">
      <Header />
    </el-header>

    <el-container>
      <el-aside width="240px" class="main-aside">
        <Sidebar />
      </el-aside>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <!-- 新增：安全提示弹窗 -->
  <el-dialog
      v-model="dialogVisible"
      class="security-notice-dialog"
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
  >
    <template #header>
      <div class="dialog-header">重要安全与保密提示</div>
    </template>

    <div class="security-dialog-content">
      <p>本系统对接的AI服务通过公共互联网进行数据传输与处理，
        存在数据被拦截、泄露或非授权访问的风险。
        <strong>请您在输入任何信息前，务必自行完成去敏化处理，
          切勿在系统中输入、上传或处理任何涉及国家秘密、
          商业秘密、未公开敏感项目信息及个人信息的内容。</strong>
        若您的项目具备上述敏感性， 请立即退出系统，
        改用内部合规的安全环境进行编制。 点击“已知悉并同意”即表示您已充分理解上述风险，
        并确认所处理信息不涉及秘密及敏感内容。
        您将对因违规输入上述信息导致的一切后果自行承担全部责任。</p>
    </div>
    <template #footer>
      <div class="security-dialog-footer">
        <div class="security-dialog-buttons">
          <el-button type="primary" class="dialog-btn" @click="handleConfirm">已知悉并同意</el-button>
          <el-button class="dialog-btn" @click="handleExit">退出</el-button>
        </div>
        <div class="security-dialog-checkbox">
          <el-checkbox v-model="dontShowAgain">不再提示</el-checkbox>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import Header from './components/Header.vue'
import Sidebar from './components/Sidebar.vue'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const STORAGE_KEY_PREFIX = 'security_notice_dismissed'
const dialogVisible = ref(false)
const dontShowAgain = ref(false)

onMounted(() => {
  const storageKey = getSecurityNoticeStorageKey()
  const dismissed = storageKey ? localStorage.getItem(storageKey) : null
  if (dismissed !== 'true') {
    setTimeout(() => {
      dialogVisible.value = true
    }, 500)
  }
})

const handleConfirm = () => {
  const storageKey = getSecurityNoticeStorageKey()
  if (dontShowAgain.value && storageKey) {
    localStorage.setItem(storageKey, 'true')
  }
  dialogVisible.value = false
}

const handleExit = () => {
  dialogVisible.value = false
  userStore.logout()
  router.push('/login')
}

const getSecurityNoticeStorageKey = () => {
  const phone = userStore.userInfo?.phone || getStoredUserInfoPhone()
  return phone ? `${STORAGE_KEY_PREFIX}_${phone}` : null
}

const getStoredUserInfoPhone = () => {
  const storedUserInfo = localStorage.getItem('userInfo')
  if (!storedUserInfo) return ''

  try {
    return JSON.parse(storedUserInfo)?.phone || ''
  } catch {
    return ''
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.main-header {
  background: var(--app-header-bg);
  box-shadow: var(--app-header-shadow);
  padding: 0;
  transition: var(--app-transition-base);
}

.main-aside {
  background: var(--app-sidebar-bg);
  backdrop-filter: var(--app-backdrop-blur);
  -webkit-backdrop-filter: var(--app-backdrop-blur);
  border-right: 1px solid var(--app-border-light);
  transition: var(--app-transition-base);
}
.main-content {
  background: var(--app-bg-secondary);
  padding: 20px;
  transition: var(--app-transition-base);
}

:global(.security-notice-dialog) {
  max-width: calc(100vw - 32px);
  border: 1px solid #3f83f8;
  border-radius: 0;
  box-shadow: none;
  padding: 0;
  background: #fff;
}

:global(.security-notice-dialog .el-dialog__header) {
  margin: 0;
  padding: 0;
}

:global(.security-notice-dialog .el-dialog__body) {
  padding: 40px 46px 22px;
  background: #fff;
}

:global(.security-notice-dialog .el-dialog__footer) {
  padding: 0 46px 18px;
  background: #fff;
}

.dialog-header {
  height: 40px;
  line-height: 40px;
  text-align: center;
  font-weight: 700;
  font-size: 16px;
  color: #fff;
  background: #4385f4;
}

.security-dialog-content {
  font-size: 14px;
  line-height: 1.6;
  color: #3f3f3f;
  font-weight: 500;
}

.security-dialog-content p {
  margin: 0;
}

.security-dialog-footer {
  display: flex;
  align-items: center;
  position: relative;
  min-height: 54px;
}

.security-dialog-buttons {
  flex: 1;
  display: flex;
  justify-content: center;
  gap: 42px;
}

.dialog-btn {
  width: 140px;
  height: 40px;
  font-size: 14px;
  border-radius: 6px;
}

:global(.security-notice-dialog .el-button:not(.el-button--primary)) {
  background: #fff;
  border-color: #b8b8b8;
  color: #333;
}

:global(.security-notice-dialog .el-button--primary) {
  background: #3f83f8;
  border-color: #3f83f8;
  color: #fff;
}

.security-dialog-checkbox {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
}

:global(.security-notice-dialog .security-dialog-checkbox .el-checkbox__label) {
  font-size: 14px;
  color: #555;
}

:global(.security-notice-dialog .security-dialog-checkbox .el-checkbox__inner) {
  width: 14px;
  height: 14px;
  background: #fff;
  border-color: #999;
}

:global(.security-notice-dialog .security-dialog-checkbox .el-checkbox.is-checked .el-checkbox__inner) {
  background: #3f83f8;
  border-color: #3f83f8;
}

</style>
