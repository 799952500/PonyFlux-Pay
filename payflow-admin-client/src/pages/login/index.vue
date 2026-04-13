<template>
  <div class="login-layout">

    <!-- 左侧品牌区（42%） -->
    <div class="brand-side">
      <div class="brand-bg"></div>
      <div class="brand-grid"></div>

      <div class="brand-content">
        <!-- Logo -->
        <div class="brand-logo">
          <img src="/ponyflux-logo-dark.svg" width="48" height="48" alt="PonyFlux Pay Logo" />
          <span class="brand-name">小马支付</span>
        </div>

        <!-- Slogan -->
        <h1 class="brand-slogan">轻盈支付，一马当先</h1>
        <p class="brand-sub">让支付更简单 · 安全稳定高效的新一代支付管理平台</p>

        <!-- 功能特性卡片 -->
        <div class="feature-cards">
          <div class="feature-card">
            <div class="feature-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#6366F1" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
            </div>
            <div>
              <div class="feature-title">资金安全</div>
              <div class="feature-desc">多重加密保障</div>
            </div>
          </div>

          <div class="feature-card">
            <div class="feature-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#6366F1" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
              </svg>
            </div>
            <div>
              <div class="feature-title">实时监控</div>
              <div class="feature-desc">交易全链路追踪</div>
            </div>
          </div>

          <div class="feature-card">
            <div class="feature-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#6366F1" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="7" height="7"/>
                <rect x="14" y="3" width="7" height="7"/>
                <rect x="14" y="14" width="7" height="7"/>
                <rect x="3" y="14" width="7" height="7"/>
              </svg>
            </div>
            <div>
              <div class="feature-title">数据洞察</div>
              <div class="feature-desc">多维度统计分析</div>
            </div>
          </div>
        </div>

        <!-- 底部 -->
        <div class="brand-footer">
          <span>v1.0.0</span>
          <span>© 2026 小马支付 PonyFlux Pay. All rights reserved.</span>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单区 -->
    <div class="form-side">
      <div class="form-container">

        <div class="form-header">
          <div class="header-accent"></div>
          <h2>欢迎回来</h2>
          <p>请使用管理员账号登录系统</p>
        </div>

        <form @submit.prevent="handleLogin" class="login-form">
          <!-- 用户名 -->
          <div class="form-item">
            <label>用户名</label>
            <div class="input-wrap" :class="{ 'focused': usernameFocused }">
              <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
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

          <!-- 密码 -->
          <div class="form-item">
            <label>密码</label>
            <div class="input-wrap" :class="{ 'focused': passwordFocused }">
              <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
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
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- 错误提示 -->
          <div v-if="errorMsg" class="error-msg">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            {{ errorMsg }}
          </div>

          <!-- 登录按钮 -->
          <button type="submit" class="login-btn" :disabled="loading">
            <span v-if="loading" class="spinner"></span>
            <span v-else>登 录</span>
          </button>
        </form>

        <div class="form-footer">
          <a href="#" class="link">忘记密码？</a>
        </div>

      </div>
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
/* === 全局布局 === */
.login-layout {
  display: flex;
  min-height: 100vh;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', -apple-system, sans-serif;

  /* 全页连续渐变：从深色平滑过渡到浅色，中间无硬边界 */
  background: linear-gradient(
    105deg,
    #0F172A 0%,        /* 深色起点 */
    #1E293B 28%,       /* 品牌区主体 */
    #312E81 42%,       /* 过渡带 - 渐变色渗透到表单区边缘 */
    #4338CA 50%,       /* 过渡中心点（正好在分割线） */
    #6366F1 52%,       /* 品牌色渗透到表单区 */
    #818CF8 58%,       /* 浅色区开始，保留品牌色余韵 */
    #C7D2FE 68%,       /* 越来越浅 */
    #E0E7FF 78%,       /* 表单区主体浅色 */
    #F8FAFC 100%       /* 最终浅白 */
  );
}

/* 过渡光带效果：在 42% 分割线处添加柔和光带 */
.login-layout::after {
  content: '';
  position: fixed;
  left: 42%;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(
    to bottom,
    transparent 0%,
    rgba(99, 102, 241, 0.3) 20%,
    rgba(99, 102, 241, 0.5) 50%,
    rgba(99, 102, 241, 0.3) 80%,
    transparent 100%
  );
  filter: blur(2px);
  pointer-events: none;
  z-index: 10;
}

/* === 左侧品牌区 === */
.brand-side {
  width: 42%;
  /* 移除旧纯色渐变，让父级全页渐变透上来 */
  background: transparent;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 网格背景 */
.brand-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(99, 102, 241, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.06) 1px, transparent 1px);
  background-size: 40px 40px;
}

/* 右上角光斑 */
.brand-bg {
  position: absolute;
  top: -100px;
  right: -100px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.25) 0%, transparent 70%);
}

/* 左下角光斑 */
.brand-bg::after {
  content: '';
  position: absolute;
  bottom: -80px;
  left: -80px;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.2) 0%, transparent 70%);
}

.brand-content {
  position: relative;
  z-index: 1;
  padding: 60px;
  width: 100%;
  box-sizing: border-box;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 48px;
}

.brand-name {
  font-size: 28px;
  font-weight: 700;
  color: #FFFFFF;
  letter-spacing: 0.5px;
}

.brand-slogan {
  font-size: 36px;
  font-weight: 700;
  color: #FFFFFF;
  margin: 0 0 16px;
  letter-spacing: 2px;
}

.brand-sub {
  font-size: 16px;
  color: #94A3B8;
  margin: 0 0 48px;
  line-height: 1.6;
}

.feature-cards {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 48px;
}

.feature-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  transition: all 0.25s ease;
  cursor: default;
}

.feature-card:hover {
  background: rgba(255, 255, 255, 0.07);
  border-color: rgba(99, 102, 241, 0.35);
  transform: translateX(6px);
}

.feature-icon {
  width: 44px;
  height: 44px;
  background: rgba(99, 102, 241, 0.15);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-title {
  font-size: 15px;
  font-weight: 600;
  color: #F8FAFC;
  margin-bottom: 2px;
}

.feature-desc {
  font-size: 13px;
  color: #64748B;
}

.brand-footer {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: #475569;
}

/* === 右侧登录表单区 === */
.form-side {
  flex: 1;
  /* 移除旧纯色渐变，让父级全页渐变透上来 */
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
}

.form-container {
  width: 100%;
  max-width: 400px;
  padding-left: 20px;
}

.form-header {
  margin-bottom: 40px;
  position: relative;
}

/* 标题装饰横线 */
.header-accent {
  width: 48px;
  height: 4px;
  background: linear-gradient(90deg, #4F46E5, #6366F1);
  border-radius: 2px;
  margin-bottom: 20px;
}

.form-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #0F172A;
  margin: 0 0 8px;
}

.form-header p {
  font-size: 14px;
  color: #64748B;
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 48px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 24px;
  box-shadow:
    0 25px 80px rgba(0, 0, 0, 0.12),
    0 8px 32px rgba(99, 102, 241, 0.15);
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.input-wrap {
  display: flex;
  align-items: center;
  border: 1.5px solid rgba(99, 102, 241, 0.15);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
  overflow: hidden;
}

.input-wrap.focused {
  border-color: #4F46E5;
  background: #FFFFFF;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12), inset 0 1px 3px rgba(0, 0, 0, 0.04);
}

.input-icon {
  margin: 0 12px;
  color: #94A3B8;
  flex-shrink: 0;
}

.input-wrap input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 14px 0;
  font-size: 14px;
  color: #0F172A;
  outline: none;
}

.input-wrap input::placeholder {
  color: #94A3B8;
}

.input-wrap input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.eye-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0 14px;
  color: #94A3B8;
  display: flex;
  align-items: center;
  transition: color 0.2s ease;
  height: 100%;
}

.eye-btn:hover {
  color: #4F46E5;
}

.error-msg {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #FEF2F2;
  border: 1px solid #FECACA;
  border-radius: 8px;
  color: #DC2626;
  font-size: 13px;
}

.login-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #4F46E5 0%, #6366F1 100%);
  border: none;
  border-radius: 10px;
  color: #FFFFFF;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  position: relative;
  overflow: hidden;
  letter-spacing: 2px;
}

.login-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.15), transparent);
  transition: left 0.5s ease;
}

.login-btn:hover:not(:disabled)::before {
  left: 100%;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.35);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #FFFFFF;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.form-footer {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid rgba(99, 102, 241, 0.1);
  text-align: center;
}

.link {
  font-size: 13px;
  color: #64748B;
  text-decoration: none;
  transition: color 0.2s ease;
}

.link:hover {
  color: #4F46E5;
}
</style>
