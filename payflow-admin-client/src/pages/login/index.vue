<template>
  <div class="login-page">
    <!-- 顶部导航 -->
    <header class="login-header">
      <div class="login-header__brand">
        <img src="/ponyflux-logo.svg" width="32" height="32" alt="" class="login-header__logo" />
        <span class="login-header__name">PonyFlux Pay</span>
      </div>
      <a href="#" class="login-header__link" @click.prevent>返回首页</a>
    </header>

    <!-- 主内容 -->
    <main class="login-main">
      <div class="login-card">
        <div class="login-card__accent" />
        <h2 class="login-card__title">PonyFlux Pay 登录</h2>

        <el-form class="login-form" @submit.prevent="handleLogin">
          <el-form-item>
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
              autocomplete="username"
              :disabled="loading"
            />
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              autocomplete="current-password"
              :disabled="loading"
            >
              <template #suffix>
                <el-icon class="login-pwd-toggle" @click="showPassword = !showPassword">
                  <View v-if="showPassword" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
            <div class="login-forgot">
              <a href="#" class="login-forgot__link" @click.prevent>忘记密码？</a>
            </div>
          </el-form-item>

          <el-form-item v-if="captchaRequired">
            <div class="login-captcha-row">
              <span class="login-captcha__expr">{{ captchaQuestion || '加载中…' }}</span>
              <el-button link type="primary" size="small" @click.prevent="refreshCaptcha">换一题</el-button>
            </div>
            <el-input
              v-model="form.captchaAnswer"
              placeholder="请输入计算结果"
              size="large"
              inputmode="numeric"
              autocomplete="off"
              :disabled="loading"
            />
            <p class="login-captcha-hint">密码错误后需完成验证</p>
          </el-form-item>

          <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" class="login-alert" />

          <el-button
            type="primary"
            size="large"
            class="login-submit"
            native-type="submit"
            :loading="loading"
          >
            登 录
          </el-button>
        </el-form>

        <p class="login-register">
          没有账号？
          <a href="#" class="login-register__link" @click.prevent>立即注册</a>
        </p>

        <div class="login-social" aria-label="第三方登录">
          <el-tooltip content="手机登录" placement="top">
            <button type="button" class="login-social__btn" aria-label="手机登录">
              <el-icon><Iphone /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="微信登录" placement="top">
            <button type="button" class="login-social__btn" aria-label="微信登录">
              <el-icon><ChatDotRound /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="QQ登录" placement="top">
            <button type="button" class="login-social__btn" aria-label="QQ登录">
              <el-icon><Promotion /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="支付宝登录" placement="top">
            <button type="button" class="login-social__btn" aria-label="支付宝登录">
              <el-icon><Wallet /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </div>
    </main>

    <!-- 底部波浪 -->
    <div class="login-waves" aria-hidden="true">
      <svg class="login-waves__svg" viewBox="0 0 1440 320" preserveAspectRatio="none">
        <path
          class="login-waves__layer login-waves__layer--1"
          d="M0,192L48,197.3C96,203,192,213,288,229.3C384,245,480,267,576,250.7C672,235,768,181,864,181.3C960,181,1056,235,1152,234.7C1248,235,1344,181,1392,154.7L1440,128L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"
        />
        <path
          class="login-waves__layer login-waves__layer--2"
          d="M0,224L48,213.3C96,203,192,181,288,181.3C384,181,480,203,576,213.3C672,224,768,224,864,208C960,192,1056,160,1152,154.7C1248,149,1344,171,1392,181.3L1440,192L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"
        />
        <path
          class="login-waves__layer login-waves__layer--3"
          d="M0,256L48,261.3C96,267,192,277,288,261.3C384,245,480,203,576,197.3C672,192,768,224,864,229.3C960,235,1056,213,1152,197.3C1248,181,1344,171,1392,165.3L1440,160L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"
        />
      </svg>
      <footer class="login-footer">
        <span>PonyFlux (上海) 网络科技</span>
        <span class="login-footer__sep">·</span>
        <span>沪ICP备00000000号</span>
        <span class="login-footer__sep">·</span>
        <a href="#" @click.prevent>联系我们</a>
        <span class="login-footer__sep">·</span>
        <a href="#" @click.prevent>隐私声明</a>
        <span class="login-footer__sep">·</span>
        <a href="#" @click.prevent>法律条款</a>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import {
  User,
  Lock,
  View,
  Hide,
  Iphone,
  ChatDotRound,
  Promotion,
  Wallet,
} from '@element-plus/icons-vue'
import request from '@/api/request'
import { adminLogin, getCaptchaRequired } from '@/api/auth'
import { useAdminStore } from '@/stores/admin'
import type { AdminLoginResponse } from '@/types'

const router = useRouter()
const adminStore = useAdminStore()
const form = reactive({ username: '', password: '', captchaAnswer: '' })
const loading = ref(false)
const errorMsg = ref('')
const showPassword = ref(false)
const captchaRequired = ref(false)
const captchaQuestion = ref('')
const captchaId = ref('')

async function syncCaptchaRequired() {
  const username = form.username.trim()
  if (!username) {
    captchaRequired.value = false
    captchaId.value = ''
    captchaQuestion.value = ''
    form.captchaAnswer = ''
    return
  }
  try {
    const data = await getCaptchaRequired(username)
    captchaRequired.value = data.required
    if (data.required) {
      if (!captchaId.value) {
        await refreshCaptcha()
      }
    } else {
      captchaId.value = ''
      captchaQuestion.value = ''
      form.captchaAnswer = ''
    }
  } catch {
    captchaRequired.value = false
  }
}

async function refreshCaptcha() {
  try {
    const data = await request.get('/admin/auth/captcha') as { captchaId?: string; question?: string }
    if (data?.captchaId) {
      captchaId.value = data.captchaId
      captchaQuestion.value = data.question ?? ''
      errorMsg.value = ''
    } else {
      captchaId.value = ''
      captchaQuestion.value = ''
      errorMsg.value = '验证码加载失败'
    }
  } catch {
    captchaId.value = ''
    captchaQuestion.value = ''
    errorMsg.value = '验证码加载失败，请检查网络'
  }
}

let captchaCheckTimer: ReturnType<typeof setTimeout> | undefined
watch(
  () => form.username,
  () => {
    if (captchaCheckTimer) {
      clearTimeout(captchaCheckTimer)
    }
    captchaCheckTimer = setTimeout(() => {
      void syncCaptchaRequired()
    }, 300)
  },
)

const handleLogin = async () => {
  if (!form.username.trim()) {
    errorMsg.value = '请输入用户名'
    return
  }
  if (!form.password) {
    errorMsg.value = '请输入密码'
    return
  }
  if (captchaRequired.value) {
    if (!form.captchaAnswer.trim()) {
      errorMsg.value = '请输入验证码'
      return
    }
    if (!captchaId.value) {
      errorMsg.value = '验证码未就绪，请点击换一题重试'
      await refreshCaptcha()
      return
    }
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const payload: { username: string; password: string; captchaId?: string; captchaAnswer?: string } = {
      username: form.username.trim(),
      password: form.password,
    }
    if (captchaRequired.value) {
      payload.captchaId = captchaId.value
      payload.captchaAnswer = form.captchaAnswer.trim()
    }
    const res = await adminLogin(payload)
    if (!res.token) {
      errorMsg.value = '登录响应缺少令牌'
      return
    }
    adminStore.setAuth(res as AdminLoginResponse)
    router.push('/admin/dashboard')
  } catch (e: unknown) {
    let msg = '用户名或密码错误'
    if (axios.isAxiosError(e)) {
      const body = e.response?.data as { message?: string; data?: Record<string, string> } | undefined
      const fieldMsg = body?.data && typeof body.data === 'object'
        ? Object.values(body.data).find((v) => typeof v === 'string' && v.length > 0)
        : undefined
      msg = fieldMsg ?? body?.message ?? e.message ?? msg
      if (e.response) {
        adminStore.clearAuth()
      }
    } else if (e !== null && typeof e === 'object' && 'message' in e) {
      const o = e as { message?: string; code?: number }
      msg = typeof o.message === 'string' ? o.message : msg
      if (o.code !== undefined && o.code !== 0) {
        adminStore.clearAuth()
      }
    }
    errorMsg.value = msg
    await syncCaptchaRequired()
    if (captchaRequired.value) {
      await refreshCaptcha()
      form.captchaAnswer = ''
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--pf-bg-page);
  color: var(--pf-text-primary);
  overflow-x: hidden;
}

/* 顶部 header */
.login-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 40px;
  background: var(--pf-login-header-bg);
  border-bottom: 1px solid var(--pf-card-border);
  flex-shrink: 0;
  z-index: 2;
}

.login-header__brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.login-header__logo {
  border-radius: 8px;
}

.login-header__name {
  font-size: 16px;
  font-weight: 700;
  color: var(--pf-primary);
  letter-spacing: 0.02em;
}

.login-header__link {
  font-size: 13px;
  color: var(--pf-primary);
  text-decoration: none;
  transition: color 0.2s;
}

.login-header__link:hover {
  color: var(--pf-primary-hover);
}

/* 主区域 */
.login-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px 200px;
  z-index: 1;
}

.login-card {
  width: 380px;
  padding: 36px 36px 28px;
  background: var(--pf-card-bg);
  border-radius: 16px;
  box-shadow: 0 12px 40px var(--pf-card-shadow);
  border: 1px solid var(--pf-card-border);
  animation: cardIn 0.5s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.login-card__accent {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  background: var(--pf-primary);
  margin: 0 auto 16px;
}

.login-card__title {
  margin: 0 0 28px;
  text-align: center;
  font-size: 20px;
  font-weight: 700;
  color: var(--pf-primary);
  letter-spacing: 0.02em;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px var(--pf-card-border) inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--pf-primary) inset;
}

.login-pwd-toggle {
  cursor: pointer;
  color: var(--pf-sidebar-text-muted);
}

.login-pwd-toggle:hover {
  color: var(--pf-primary);
}

.login-forgot {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
}

.login-forgot__link {
  font-size: 12px;
  color: var(--pf-primary);
  text-decoration: none;
}

.login-forgot__link:hover {
  color: var(--pf-primary-hover);
}

.login-captcha-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--pf-primary-soft);
  border: 1px solid var(--pf-card-border);
}

.login-captcha__expr {
  font-size: 16px;
  font-weight: 700;
  color: var(--pf-primary-hover);
  letter-spacing: 0.06em;
}

.login-captcha-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--pf-text-secondary);
}

.login-alert {
  margin-bottom: 12px;
}

.login-submit {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.12em;
  border-radius: 8px;
}

.login-register {
  margin: 20px 0 0;
  text-align: center;
  font-size: 13px;
  color: var(--pf-text-secondary);
}

.login-register__link {
  color: var(--pf-primary);
  text-decoration: none;
  font-weight: 500;
}

.login-register__link:hover {
  color: var(--pf-primary-hover);
}

.login-social {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--pf-card-border);
}

.login-social__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: var(--pf-primary);
  color: #fff;
  cursor: pointer;
  font-size: 18px;
  transition: transform 0.2s, background 0.2s;
}

.login-social__btn:hover {
  background: var(--pf-primary-hover);
  transform: translateY(-2px);
}

/* 底部波浪 */
.login-waves {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 220px;
  z-index: 0;
  pointer-events: none;
}

.login-waves__svg {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.login-waves__layer--1 {
  fill: var(--pf-wave-1);
  opacity: 0.7;
  animation: waveDrift 18s ease-in-out infinite alternate;
}

.login-waves__layer--2 {
  fill: var(--pf-wave-2);
  opacity: 0.85;
  animation: waveDrift 14s ease-in-out infinite alternate-reverse;
}

.login-waves__layer--3 {
  fill: var(--pf-wave-3);
  animation: waveDrift 22s ease-in-out infinite alternate;
}

@keyframes waveDrift {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-2%);
  }
}

.login-footer {
  position: absolute;
  bottom: 16px;
  left: 0;
  right: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 0 16px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.85);
  pointer-events: auto;
  z-index: 1;
}

.login-footer a {
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
}

.login-footer a:hover {
  color: #fff;
  text-decoration: underline;
}

.login-footer__sep {
  opacity: 0.6;
}

@media (max-width: 480px) {
  .login-header {
    padding: 0 20px;
  }

  .login-card {
    width: 100%;
    max-width: 380px;
    padding: 28px 24px 24px;
  }

  .login-footer {
    font-size: 10px;
  }
}
</style>
