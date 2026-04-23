<template>
  <div class="login-page">
    <div class="bg-orb orb-a" />
    <div class="bg-orb orb-b" />

    <div class="login-grid">
      <section class="brand-panel panel-card">
        <p class="kicker">AI TENDER PLATFORM</p>
        <h1>AI招标文件编制</h1>
        <p class="desc">
          智能化招标文件生成工具，支持AI辅助需求分析、评审项管理和文档生成。
          登录后即可开始高效的招标文件编制工作。
        </p>

        <div class="features">
          <div class="item">
            <span class="dot" />
            AI智能生成招标文件
          </div>
          <div class="item">
            <span class="dot" />
            模板匹配与知识库检索
          </div>
          <div class="item">
            <span class="dot" />
            公平性与合规性智能检测
          </div>
        </div>
      </section>

      <section class="form-panel panel-card">
        <div class="form-head">
          <h2>欢迎登录</h2>
          <p>请选择登录方式进入系统</p>
        </div>

        <!-- 登录方式切换 -->
        <div class="login-tabs">
          <button
            :class="['tab-btn', { active: loginType === 'password' }]"
            @click="loginType = 'password'"
          >
            账号密码登录
          </button>
          <button
            :class="['tab-btn', { active: loginType === 'phone' }]"
            @click="loginType = 'phone'"
          >
            手机验证码登录
          </button>
        </div>

        <!-- 账号密码登录表单 -->
        <el-form
          v-if="loginType === 'password'"
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          @keyup.enter="handlePasswordLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="passwordForm.username"
              placeholder="用户名"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="passwordForm.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              show-password
              size="large"
            />
          </el-form-item>
          <el-form-item>
            <div class="remember-row">
              <el-checkbox v-model="rememberUsername">记住密码</el-checkbox>
              <el-link type="primary" :underline="false" @click="showResetDialog = true">忘记密码?</el-link>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              :loading="loading"
              @click="handlePasswordLogin"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 手机验证码登录表单 -->
        <el-form
          v-else
          ref="phoneFormRef"
          :model="phoneForm"
          :rules="phoneRules"
          @keyup.enter="handlePhoneLogin"
        >
          <el-form-item prop="phone">
            <el-input
              v-model="phoneForm.phone"
              placeholder="手机号"
              :prefix-icon="Iphone"
              size="large"
            />
          </el-form-item>
          <el-form-item prop="code">
            <div class="code-input">
              <el-input
                v-model="phoneForm.code"
                placeholder="验证码"
                :prefix-icon="Message"
                size="large"
              />
              <el-button
                :disabled="countdown > 0"
                :loading="sendingCode"
                size="large"
                @click="handleSendCode"
              >
                {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              :loading="loading"
              @click="handlePhoneLogin"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>
      </section>
    </div>

    <ResetPasswordDialog v-model="showResetDialog" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User, Iphone, Message } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { authApi } from '@/api/auth'
import ResetPasswordDialog from '@/components/common/ResetPasswordDialog.vue'

const REMEMBERED_USERNAME_KEY = 'remembered_username'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginType = ref<'password' | 'phone'>('password')
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const rememberUsername = ref(false)
const showResetDialog = ref(false)

// 账号密码登录
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  username: '',
  password: '',
})

const passwordRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度 3-20 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' },
  ],
}

const handlePasswordLogin = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await userStore.login(passwordForm.username, passwordForm.password)

      // 记住用户名逻辑：勾选则存储，未勾选则清除
      if (rememberUsername.value) {
        localStorage.setItem(REMEMBERED_USERNAME_KEY, passwordForm.username)
      } else {
        localStorage.removeItem(REMEMBERED_USERNAME_KEY)
      }

      ElMessage.success('登录成功')
      router.push((route.query.redirect as string) || '/')
    } catch (error) {
      console.error('登录失败:', error)
    } finally {
      loading.value = false
    }
  })
}

// 手机验证码登录
const phoneFormRef = ref<FormInstance>()
const phoneForm = reactive({
  phone: '',
  code: '',
})

const phoneRules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '手机号格式不正确',
      trigger: 'blur',
    },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    {
      pattern: /^\d{4,6}$/,
      message: '验证码格式不正确',
      trigger: 'blur',
    },
  ],
}

const handleSendCode = async () => {
  if (!phoneFormRef.value) return

  // 仅验证手机号
  await phoneFormRef.value.validateField('phone', async (valid) => {
    if (!valid) return

    sendingCode.value = true
    try {
      const code = await authApi.sendSmsCode({ phone: phoneForm.phone })
      ElMessage.success(`验证码已发送：${code}（仅测试用）`)

      // 开始倒计时
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    } catch (error) {
      console.error('发送验证码失败:', error)
    } finally {
      sendingCode.value = false
    }
  })
}

const handlePhoneLogin = async () => {
  if (!phoneFormRef.value) return
  await phoneFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await userStore.phoneLogin(phoneForm.phone, phoneForm.code)
      ElMessage.success('登录成功')
      router.push((route.query.redirect as string) || '/')
    } catch (error) {
      console.error('登录失败:', error)
    } finally {
      loading.value = false
    }
  })
}

// 页面加载时检查是否有记住的用户名
onMounted(() => {
  const saved = localStorage.getItem(REMEMBERED_USERNAME_KEY)
  if (saved) {
    passwordForm.username = saved
    rememberUsername.value = true
  }
})
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(
    150deg,
    var(--app-bg-secondary) 0,
    var(--app-bg-primary) 55%,
    var(--app-bg-tertiary) 100%
  );
  transition: var(--app-transition-base);
}

.bg-orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(2px);
}

.orb-a {
  width: 460px;
  height: 460px;
  top: -140px;
  right: -80px;
  background: radial-gradient(circle, rgba(51, 108, 255, 0.2), rgba(51, 108, 255, 0));
}

.orb-b {
  width: 360px;
  height: 360px;
  left: -120px;
  bottom: -90px;
  background: radial-gradient(circle, rgba(49, 227, 253, 0.15), rgba(49, 227, 253, 0));
}

.login-grid {
  position: relative;
  z-index: 2;
  width: min(980px, calc(100vw - 32px));
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 16px;
}

.panel-card {
  padding: 30px;
  background: var(--app-card-bg);
  backdrop-filter: var(--app-backdrop-blur);
  -webkit-backdrop-filter: var(--app-backdrop-blur);
  border-radius: var(--app-radius-lg);
  border: 1px solid var(--app-border-light);
  transition: var(--app-transition-base);
}

.kicker {
  display: inline-block;
  padding: 5px 10px;
  border-radius: 999px;
  background: var(--app-hover-state);
  color: var(--app-brand-color);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.6px;
}

.brand-panel h1 {
  margin: 16px 0 10px;
  font-size: 38px;
  line-height: 1.1;
  color: var(--app-text-primary);
}

.desc {
  color: var(--app-text-secondary);
  line-height: 1.7;
}

.features {
  margin-top: 22px;
  display: grid;
  gap: 10px;
}

.item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--app-text-secondary);
  font-weight: 500;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 8px;
  background: var(--app-brand-color);
}

.form-head h2 {
  margin: 0;
  font-size: 28px;
  color: var(--app-text-primary);
}

.form-head p {
  color: var(--app-text-tertiary);
  margin: 8px 0 22px;
}

// 登录方式切换
.login-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  background: var(--app-bg-tertiary);
  padding: 4px;
  border-radius: var(--app-radius-sm);
}

.tab-btn {
  flex: 1;
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: var(--app-text-tertiary);
  transition: all 0.2s;

  &:hover {
    color: var(--app-brand-color);
  }

  &.active {
    background: var(--app-bg-elevated);
    color: var(--app-brand-color);
    font-weight: 600;
    box-shadow: 0 2px 4px var(--app-shadow-color);
  }
}

// 验证码输入
.code-input {
  display: flex;
  gap: 12px;

  :deep(.el-input) {
    flex: 1;
  }

  :deep(.el-button) {
    min-width: 120px;
  }
}

.remember-row {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.submit-btn {
  width: 100%;
  background: var(--app-gradient-brand) !important;
  border: none !important;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(51, 108, 255, 0.4);
  }
}

@media (max-width: 960px) {
  .login-grid {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    display: none;
  }

  .form-panel {
    padding: 22px;
  }
}
</style>
