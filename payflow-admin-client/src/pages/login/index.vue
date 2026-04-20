<template>
  <div class="login-page">
    <div class="login-page__bg" aria-hidden="true" />

    <div class="login-page__inner">
      <header class="login-header">
        <img src="/ponyflux-logo.svg" width="56" height="56" class="login-logo-img" alt="小马支付" />
        <div class="login-header__text">
          <h1 class="login-brand">小马支付</h1>
          <p class="login-tagline">轻盈支付，一马当先</p>
        </div>
      </header>

      <p class="login-lead">让支付更简单 · 安全稳定高效的新一代支付管理平台</p>

      <div class="login-panel">
        <div class="login-panel__head">
          <div class="login-panel__accent" />
          <h2>欢迎回来</h2>
          <p>请使用管理员账号登录系统</p>
        </div>

        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-item">
            <label>用户名</label>
            <div class="input-wrap" :class="{ focused: usernameFocused }">
              <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              <input
                v-model="form.username"
                type="text"
                placeholder="请输入用户名"
                autocomplete="username"
                @focus="usernameFocused = true"
                @blur="usernameFocused = false"
                :disabled="loading"
              />
            </div>
          </div>

          <div class="form-item">
            <label>密码</label>
            <div class="input-wrap" :class="{ focused: passwordFocused }">
              <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                autocomplete="current-password"
                @focus="passwordFocused = true"
                @blur="passwordFocused = false"
                :disabled="loading"
              />
              <button type="button" class="eye-btn" @click="showPassword = !showPassword">
                <svg v-if="!showPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                  <line x1="1" y1="1" x2="23" y2="23" />
                </svg>
              </button>
            </div>
          </div>

          <div v-if="errorMsg" class="error-msg">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            {{ errorMsg }}
          </div>

          <button type="submit" class="login-btn" :disabled="loading">
            <span v-if="loading" class="spinner" />
            <span v-else>登 录</span>
          </button>
        </form>

        <div class="form-footer">
          <a href="#" class="link">忘记密码？</a>
        </div>
      </div>

      <ul class="login-trust" aria-label="产品特点">
        <li>
          <span class="login-trust__icon" aria-hidden="true">◇</span>
          <span>资金安全</span>
        </li>
        <li>
          <span class="login-trust__icon" aria-hidden="true">◇</span>
          <span>实时监控</span>
        </li>
        <li>
          <span class="login-trust__icon" aria-hidden="true">◇</span>
          <span>数据洞察</span>
        </li>
      </ul>

      <footer class="login-foot">
        <span>v1.0.0</span>
        <span>© 2026 小马支付 PonyFlux Pay</span>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { adminLogin } from '@/api/auth'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')
const showPassword = ref(false)
const usernameFocused = ref(false)
const passwordFocused = ref(false)

const handleLogin = async () => {
  if (!form.username.trim()) {
    errorMsg.value = '请输入用户名'
    return
  }
  if (!form.password) {
    errorMsg.value = '请输入密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await adminLogin({ username: form.username, password: form.password })
    localStorage.setItem('adminToken', res.token)
    localStorage.setItem('adminUser', JSON.stringify(res.user || res))
    router.push('/admin/dashboard')
  } catch (e: any) {
    errorMsg.value = e.message || '用户名或密码错误'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', -apple-system, sans-serif;
}

.login-page__bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-color: #021a18;
  background-image:
    radial-gradient(ellipse 92% 72% at 0% 0%, rgba(3, 42, 36, 0.78) 0%, transparent 56%),
    radial-gradient(ellipse 90% 52% at 50% 0%, rgba(2, 44, 36, 0.62) 0%, transparent 52%),
    radial-gradient(ellipse 88% 52% at 100% 14%, rgba(2, 32, 28, 0.62) 0%, transparent 50%),
    radial-gradient(ellipse 120% 58% at 50% 100%, rgba(1, 16, 14, 0.78) 0%, transparent 55%),
    linear-gradient(180deg, rgba(2, 36, 32, 0.88) 0%, rgba(4, 58, 48, 0.68) 48%, rgba(1, 22, 20, 0.9) 100%),
    url('/forest-hero.png');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

.login-page__inner {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px 32px;
  box-sizing: border-box;
}

.login-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.login-logo-img {
  flex-shrink: 0;
  border-radius: 14px;
  filter: drop-shadow(0 6px 20px rgba(0, 0, 0, 0.35));
}

.login-brand {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #ecfdf5;
  letter-spacing: 0.04em;
}

.login-tagline {
  margin: 4px 0 0;
  font-size: 13px;
  color: rgba(167, 243, 208, 0.75);
  letter-spacing: 0.12em;
}

.login-lead {
  margin: 0 0 28px;
  max-width: 420px;
  text-align: center;
  font-size: 14px;
  line-height: 1.65;
  color: rgba(203, 213, 225, 0.78);
}

.login-panel {
  width: 100%;
  max-width: 420px;
  padding: 36px 36px 28px;
  background: rgba(4, 28, 26, 0.78);
  border: 1px solid rgba(120, 150, 140, 0.22);
  border-radius: 20px;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow:
    0 28px 90px rgba(0, 0, 0, 0.48),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.login-panel__head {
  margin-bottom: 28px;
}

.login-panel__accent {
  width: 44px;
  height: 3px;
  border-radius: 2px;
  margin-bottom: 16px;
  background: linear-gradient(90deg, #065f46, #0d9488);
}

.login-panel__head h2 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: #f8fafc;
}

.login-panel__head p {
  margin: 0;
  font-size: 13px;
  color: rgba(148, 163, 184, 0.9);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 13px;
  font-weight: 500;
  color: rgba(209, 250, 229, 0.85);
}

.input-wrap {
  display: flex;
  align-items: center;
  border: 1px solid rgba(148, 163, 148, 0.28);
  border-radius: 10px;
  background: rgba(2, 20, 18, 0.55);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  overflow: hidden;
}

.input-wrap.focused {
  border-color: rgba(13, 148, 136, 0.65);
  box-shadow: 0 0 0 2px rgba(4, 120, 87, 0.35);
}

.input-icon {
  margin: 0 12px;
  color: rgba(148, 163, 184, 0.85);
  flex-shrink: 0;
}

.input-wrap input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 13px 0;
  font-size: 14px;
  color: #f1f5f9;
  outline: none;
}

.input-wrap input::placeholder {
  color: rgba(148, 163, 184, 0.55);
}

.input-wrap input:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.eye-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0 14px;
  color: rgba(148, 163, 184, 0.85);
  display: flex;
  align-items: center;
  transition: color 0.2s ease;
  height: 100%;
}

.eye-btn:hover {
  color: #5eead4;
}

.error-msg {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(69, 10, 10, 0.55);
  border: 1px solid rgba(248, 113, 113, 0.35);
  border-radius: 8px;
  color: #fecaca;
  font-size: 13px;
}

.login-btn {
  width: 100%;
  padding: 13px;
  margin-top: 4px;
  background: linear-gradient(180deg, #0d9488 0%, #047857 55%, #065f46 100%);
  border: 1px solid rgba(6, 95, 70, 0.85);
  border-radius: 10px;
  color: #ecfdf5;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  letter-spacing: 0.14em;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 28px rgba(2, 44, 34, 0.55);
}

.login-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.25);
  border-top-color: #ecfdf5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.form-footer {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid rgba(120, 150, 140, 0.18);
  text-align: center;
}

.link {
  font-size: 13px;
  color: rgba(148, 163, 184, 0.9);
  text-decoration: none;
  transition: color 0.2s ease;
}

.link:hover {
  color: #99f6e4;
}

.login-trust {
  list-style: none;
  margin: 28px 0 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 20px 28px;
  max-width: 480px;
}

.login-trust li {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(167, 243, 208, 0.65);
  letter-spacing: 0.06em;
}

.login-trust__icon {
  color: rgba(13, 148, 136, 0.75);
  font-size: 10px;
}

.login-foot {
  margin-top: auto;
  padding-top: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: rgba(100, 116, 139, 0.75);
}
</style>
