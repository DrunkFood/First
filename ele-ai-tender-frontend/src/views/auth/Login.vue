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
          <p>请输入手机号和验证码进入系统</p>
        </div>

        <!-- 手机验证码登录表单 -->
        <el-form
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
        <p class="agreement-tip">
          登录即表示同意
          <button type="button" class="agreement-link" @click="openAgreement('user', 'view')">
            《用户服务协议》
          </button>
          和
          <button type="button" class="agreement-link" @click="openAgreement('privacy', 'view')">
            《隐私政策》
          </button>
        </p>
      </section>
    </div>

    <el-dialog
      v-model="agreementDialogVisible"
      :title="activeAgreementTitle"
      width="1000px"
      :show-close="agreementDialogMode === 'view'"
      :close-on-click-modal="agreementDialogMode === 'view'"
      :close-on-press-escape="agreementDialogMode === 'view'"
      class="agreement-dialog"
    >
      <div class="agreement-content">
        <p
          v-for="paragraph in activeAgreementParagraphs"
          :key="paragraph.id"
          :class="['agreement-paragraph', `agreement-paragraph--${paragraph.type}`]"
        >
          <template v-for="(part, index) in paragraph.parts" :key="index">
            <strong v-if="part.bold" class="agreement-strong">{{ part.text }}</strong>
            <span v-else>{{ part.text }}</span>
          </template>
        </p>
      </div>
      <template v-if="agreementDialogMode === 'required'" #footer>
        <el-button type="primary" class="agreement-confirm-btn" :loading="loading" @click="handleAgreementConfirm">
          同意并继续
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Iphone, Message } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { authApi } from '@/api/auth'
import userAgreementText from '@/assets/agreement/user-service-agreement.txt?raw'
import privacyPolicyText from '@/assets/agreement/privacy-policy.txt?raw'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const agreementDialogVisible = ref(false)
const agreementDialogMode = ref<'view' | 'required'>('view')
const activeAgreement = ref<'user' | 'privacy'>('user')
const agreementVersion = '2026-07-31'

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

const activeAgreementTitle = computed(() =>
  activeAgreement.value === 'user' ? '用户服务协议' : '隐私政策'
)

const activeAgreementText = computed(() =>
  activeAgreement.value === 'user' ? userAgreementText : privacyPolicyText
)

type AgreementParagraphType = 'title' | 'section' | 'paragraph'

interface AgreementTextPart {
  text: string
  bold: boolean
}

interface AgreementParagraph {
  id: number
  type: AgreementParagraphType
  parts: AgreementTextPart[]
}

const sectionTitlePattern = /^[一二三四五六七八九十]+、/
const boldMarkerPattern = /\*\*(.+?)\*\*/g

const parseBoldParts = (line: string): AgreementTextPart[] => {
  const parts: AgreementTextPart[] = []
  let lastIndex = 0
  let match: RegExpExecArray | null

  boldMarkerPattern.lastIndex = 0
  while ((match = boldMarkerPattern.exec(line)) !== null) {
    if (match.index > lastIndex) {
      parts.push({ text: line.slice(lastIndex, match.index), bold: false })
    }
    parts.push({ text: match[1] ?? '', bold: true })
    lastIndex = match.index + match[0].length
  }

  if (lastIndex < line.length) {
    parts.push({ text: line.slice(lastIndex), bold: false })
  }

  return parts.length ? parts : [{ text: line, bold: false }]
}

const parseAgreementText = (text: string): AgreementParagraph[] => {
  const lines = text.split(/\r?\n/).map(line => line.trim()).filter(Boolean)
  return lines.map((line, index) => {
    const type: AgreementParagraphType = index === 0
      ? 'title'
      : sectionTitlePattern.test(line)
        ? 'section'
        : 'paragraph'
    return {
      id: index,
      type,
      parts: parseBoldParts(line),
    }
  })
}

const activeAgreementParagraphs = computed(() => parseAgreementText(activeAgreementText.value))

const openAgreement = (agreement: 'user' | 'privacy', mode: 'view' | 'required') => {
  activeAgreement.value = agreement
  agreementDialogMode.value = mode
  agreementDialogVisible.value = true
}

const startRequiredAgreementFlow = () => {
  openAgreement('user', 'required')
}

const finishPhoneLogin = async () => {
  await userStore.phoneLogin(phoneForm.phone, phoneForm.code, {
    agreementAccepted: true,
    acceptedAgreementTypes: ['USER_SERVICE_AGREEMENT', 'PRIVACY_POLICY'],
    agreementVersion,
  })
  agreementDialogVisible.value = false
  ElMessage.success('登录成功')
  router.push((route.query.redirect as string) || '/')
}

const handleAgreementConfirm = async () => {
  if (loading.value) return

  if (activeAgreement.value === 'user') {
    openAgreement('privacy', 'required')
    return
  }

  loading.value = true
  try {
    await finishPhoneLogin()
  } catch (error) {
    console.error('协议同意后登录失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSendCode = async () => {
  if (!phoneFormRef.value) return

  // 仅验证手机号
  await phoneFormRef.value.validateField('phone', async (valid) => {
    if (!valid) return

    sendingCode.value = true
    try {
      const code = await authApi.sendSmsCode({ phone: phoneForm.phone })
      if (code) {
        phoneForm.code = code
        ElMessage.success(`验证码已发送，测试验证码：${code}`)
      } else {
        ElMessage.success('验证码已发送，请注意查收')
      }

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
      if ((error as { code?: number })?.code === 1014) {
        startRequiredAgreementFlow()
        return
      }
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
  background: var(--app-gradient-brand) !important;
  border: none !important;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(51, 108, 255, 0.4);
  }
}

.agreement-tip {
  margin: 4px 0 0;
  text-align: center;
  font-size: 13px;
  color: var(--app-text-tertiary);
}

.agreement-link {
  padding: 0;
  border: none;
  background: transparent;
  color: var(--app-brand-color);
  cursor: pointer;
  font-size: inherit;
}

.agreement-link:hover {
  text-decoration: underline;
}

.agreement-content {
  max-height: 380px;
  overflow-y: auto;
  padding: 24px 34px 8px;
  color: #1f1f1f;
  font-size: 16px;
  line-height: 1.55;
  font-weight: 400;
}

.agreement-paragraph {
  margin: 0 0 14px;
  white-space: pre-wrap;
}

.agreement-paragraph--title,
.agreement-paragraph--section {
  color: #000;
  font-weight: 700;
}

.agreement-paragraph--title {
  text-align: center;
  font-size: 18px;
}

.agreement-strong {
  color: #000;
  font-weight: 700;
}

:deep(.agreement-dialog) {
  border: 2px solid #2f83ff;
  border-radius: 0;
  padding: 0;
  background: #fff;
  box-shadow: none;
  overflow: hidden;
}

:deep(.agreement-dialog .el-dialog__header) {
  margin-right: 0;
  padding: 8px 48px;
  background: #2f83ff;
  text-align: center;
}

:deep(.agreement-dialog .el-dialog__title) {
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}

:deep(.agreement-dialog .el-dialog__headerbtn) {
  top: 0;
  right: 16px;
  width: 48px;
  height: 40px;
}

:deep(.agreement-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: #fff;
  font-size: 24px;
  font-weight: 700;
}

:deep(.agreement-dialog .el-dialog__body) {
  padding: 0;
  background: #fff;
}

:deep(.agreement-dialog .el-dialog__footer) {
  padding: 10px 20px 20px;
  background: #fff;
  text-align: center;
}

.agreement-confirm-btn {
  width: 160px;
  height: 46px;
  font-size: 16px;
  font-weight: 700;
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
