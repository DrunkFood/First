<template>
  <div class="login-page">
    <div class="bg-orb orb-a" />
    <div class="bg-orb orb-b" />

    <div class="login-grid">
      <section class="brand-panel panel-card">
        <p class="kicker">ELE TENDER PLATFORM</p>
        <h1>运维支撑中心</h1>
        <p class="desc">
          统一管理用户权限、接入系统、版本分发与插件生命周期。
          登录后可直接完成审计可追踪的发布闭环。
        </p>

        <div class="features">
          <div class="item">
            <span class="dot" />
            多系统接入与密钥轮换
          </div>
          <div class="item">
            <span class="dot" />
            版本/插件双轨发布
          </div>
          <div class="item">
            <span class="dot" />
            权限与操作日志联动
          </div>
        </div>
      </section>

      <section class="form-panel panel-card">
        <div class="form-head">
          <h2>欢迎登录</h2>
          <p>请输入账号密码进入控制台</p>
        </div>

        <el-form ref="formRef" :model="loginForm" :rules="rules" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" size="large" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              show-password
              size="large"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleLogin">
              进入系统
            </el-button>
          </el-form-item>
        </el-form>

      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度 3-20 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' },
  ],
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await userStore.login(loginForm.username, loginForm.password)
      await userStore.getUserMenus()
      ElMessage.success('登录成功')
      router.push((route.query.redirect as string) || '/')
    } catch (error) {
      console.error('Login failed:', error)
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
