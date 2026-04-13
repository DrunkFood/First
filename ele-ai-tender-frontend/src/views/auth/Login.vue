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

        <div class="tips">
          测试账号：`testuser / user123` 或 `admin / admin123`
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User, Iphone, Message } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { authApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginType = ref<'password' | 'phone'>('password')
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)

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
      ElMessage.success(`验证码已发送：${code.data}（仅测试用）`)

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
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(150deg, #f3faf6 0, #edf4ff 55%, #fef8ed 100%);
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
  background: radial-gradient(circle, rgba(42, 175, 128, 0.35), rgba(42, 175, 128, 0));
}

.orb-b {
  width: 360px;
  height: 360px;
  left: -120px;
  bottom: -90px;
  background: radial-gradient(circle, rgba(228, 178, 86, 0.3), rgba(228, 178, 86, 0));
}

.login-grid {
  position: relative;
  z-index: 2;
  width: min(980px, calc(100vw - 32px));
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 16px;
}

.brand-panel,
.form-panel {
  padding: 30px;
}

.kicker {
  display: inline-block;
  padding: 5px 10px;
  border-radius: 999px;
  background: #e8f7f0;
  color: #12734f;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.6px;
}

.brand-panel h1 {
  margin: 16px 0 10px;
  font-size: 38px;
  line-height: 1.1;
  color: #142e24;
}

.desc {
  color: #55766a;
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
  color: #30574b;
  font-weight: 500;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 8px;
  background: #21a06f;
}

.form-head h2 {
  margin: 0;
  font-size: 28px;
  color: #153428;
}

.form-head p {
  color: #708b80;
  margin: 8px 0 22px;
}

// 登录方式切换
.login-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  background: #f0f4f2;
  padding: 4px;
  border-radius: 8px;
}

.tab-btn {
  flex: 1;
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #5a7a6c;
  transition: all 0.2s;

  &:hover {
    color: #2a7a5a;
  }

  &.active {
    background: white;
    color: #1a5a42;
    font-weight: 600;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
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

.submit-btn {
  width: 100%;
}

.tips {
  margin-top: 16px;
  color: #6c8479;
  font-size: 12px;
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
