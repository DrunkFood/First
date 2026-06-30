<template>
  <el-dialog
    v-model="visible"
    title="重置密码"
    width="420px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <!-- 步骤1：验证身份 -->
    <el-form
      v-if="step === 1"
      ref="verifyFormRef"
      :model="verifyForm"
      :rules="verifyRules"
      label-position="top"
    >
      <el-form-item label="手机号" prop="phone">
        <el-input
          v-model="verifyForm.phone"
          placeholder="请输入手机号"
          :prefix-icon="Iphone"
          size="large"
        />
      </el-form-item>
      <el-form-item label="验证码" prop="code">
        <div class="code-input">
          <el-input
            v-model="verifyForm.code"
            placeholder="请输入验证码"
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
          @click="handleNextStep"
        >
          下一步
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 步骤2：设置新密码 -->
    <el-form
      v-else
      ref="passwordFormRef"
      :model="passwordForm"
      :rules="passwordRules"
      label-position="top"
    >
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="passwordForm.newPassword"
          type="password"
          placeholder="请输入新密码（6-20位）"
          :prefix-icon="Lock"
          show-password
          size="large"
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="passwordForm.confirmPassword"
          type="password"
          placeholder="请再次输入新密码"
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
          @click="handleResetPassword"
        >
          确认重置
        </el-button>
      </el-form-item>
    </el-form>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Iphone, Message, Lock } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'

const visible = defineModel<boolean>({ default: false })

const step = ref(1)
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)

// 步骤1：验证身份
const verifyFormRef = ref<FormInstance>()
const verifyForm = reactive({
  phone: '',
  code: '',
})

const verifyRules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{4,6}$/, message: '验证码格式不正确', trigger: 'blur' },
  ],
}

// 步骤2：设置新密码
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const handleSendCode = async () => {
  if (!verifyFormRef.value) return
  await verifyFormRef.value.validateField('phone', async (valid) => {
    if (!valid) return

    sendingCode.value = true
    try {
      await authApi.sendSmsCode({
        phone: verifyForm.phone,
        scene: 'RESET_PWD',
      })
      ElMessage.success('验证码已发送，请注意查收')

      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) clearInterval(timer)
      }, 1000)
    } catch {
      // 错误已由 request 拦截器处理
    } finally {
      sendingCode.value = false
    }
  })
}

const handleNextStep = async () => {
  if (!verifyFormRef.value) return
  await verifyFormRef.value.validate(async (valid) => {
    if (!valid) return
    step.value = 2
  })
}

const handleResetPassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await authApi.resetPassword(
        verifyForm.phone,
        verifyForm.code,
        passwordForm.newPassword,
      )
      ElMessage.success('密码重置成功，请使用新密码登录')
      visible.value = false
    } catch {
      // 错误已由 request 拦截器处理
    } finally {
      loading.value = false
    }
  })
}

const handleClosed = () => {
  step.value = 1
  verifyForm.phone = ''
  verifyForm.code = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  countdown.value = 0
  verifyFormRef.value?.resetFields()
  passwordFormRef.value?.resetFields()
}
</script>

<style scoped lang="scss">
.code-input {
  display: flex;
  gap: 12px;
  width: 100%;

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
</style>
