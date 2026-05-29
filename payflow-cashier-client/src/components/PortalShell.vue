<template>
  <div class="portal-page">
    <header class="portal-header">
      <div class="portal-header__brand">
        <img src="/ponyflux-logo.svg" width="32" height="32" :alt="t('app.brand')" class="portal-header__logo" />
        <span class="portal-header__name">PonyFlux Pay</span>
      </div>
      <slot name="header-extra" />
    </header>

    <main class="portal-main">
      <div class="portal-card" :class="{ 'portal-card--wide': wide }">
        <div class="portal-card__accent" />
        <h1 v-if="title" class="portal-card__title">{{ title }}</h1>
        <p v-if="subtitle" class="portal-card__subtitle">{{ subtitle }}</p>
        <slot />
      </div>
    </main>

    <div class="portal-waves" aria-hidden="true">
      <svg class="portal-waves__svg" viewBox="0 0 1440 320" preserveAspectRatio="none">
        <path
          class="portal-waves__layer portal-waves__layer--1"
          d="M0,192L48,197.3C96,203,192,213,288,229.3C384,245,480,267,576,250.7C672,235,768,181,864,181.3C960,181,1056,235,1152,234.7C1248,235,1344,181,1392,154.7L1440,128L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"
        />
        <path
          class="portal-waves__layer portal-waves__layer--2"
          d="M0,224L48,213.3C96,203,192,181,288,181.3C384,181,480,203,576,213.3C672,224,768,224,864,208C960,192,1056,160,1152,154.7C1248,149,1344,171,1392,181.3L1440,192L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"
        />
        <path
          class="portal-waves__layer portal-waves__layer--3"
          d="M0,96L48,112C96,128,192,160,288,186.7C384,213,480,235,576,213.3C672,192,768,128,864,128C960,128,1056,192,1152,197.3C1248,203,1344,181,1392,170.7L1440,160L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"
        />
      </svg>
    </div>

    <footer class="portal-footer">
      <span>© {{ year }} PonyFlux Pay</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineProps<{
  title?: string
  subtitle?: string
  wide?: boolean
}>()

const year = new Date().getFullYear()
</script>

<style scoped>
.portal-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
}

.portal-header {
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

.portal-header__brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.portal-header__logo {
  border-radius: 8px;
}

.portal-header__name {
  font-size: 16px;
  font-weight: 700;
  color: var(--pf-primary);
  letter-spacing: 0.02em;
}

.portal-main {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 32px 24px 200px;
  z-index: 1;
}

.portal-card {
  width: 380px;
  max-width: 100%;
  padding: 32px 32px 28px;
  background: var(--pf-card-bg);
  border-radius: 16px;
  box-shadow: 0 12px 40px var(--pf-card-shadow);
  border: 1px solid var(--pf-card-border);
  animation: portalCardIn 0.45s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

.portal-card--wide {
  width: min(520px, 100%);
}

@keyframes portalCardIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.portal-card__accent {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  background: var(--pf-primary);
  margin: 0 auto 14px;
}

.portal-card__title {
  margin: 0 0 8px;
  text-align: center;
  font-size: 20px;
  font-weight: 700;
  color: var(--pf-primary);
}

.portal-card__subtitle {
  margin: 0 0 24px;
  text-align: center;
  font-size: 13px;
  color: var(--pf-text-secondary);
  line-height: 1.5;
}

.portal-waves {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 220px;
  z-index: 0;
  pointer-events: none;
}

.portal-waves__svg {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.portal-waves__layer--1 {
  fill: var(--pf-wave-1);
  opacity: 0.7;
}

.portal-waves__layer--2 {
  fill: var(--pf-wave-2);
  opacity: 0.85;
}

.portal-waves__layer--3 {
  fill: var(--pf-wave-3);
}

.portal-footer {
  position: fixed;
  bottom: 12px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.88);
  z-index: 1;
  pointer-events: none;
}

:deep(.portal-form .el-form-item__label) {
  color: var(--pf-text-primary);
  font-weight: 500;
}

:deep(.portal-form .el-input__wrapper),
:deep(.portal-form .el-textarea__inner) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px var(--pf-card-border) inset;
}

:deep(.portal-form .el-input__wrapper.is-focus),
:deep(.portal-form .el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--pf-primary) inset;
}

:deep(.portal-form .el-steps) {
  --el-color-primary: var(--pf-primary-hover);
}

.portal-header :deep(.portal-link),
:deep(.portal-link) {
  color: var(--pf-primary);
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
}

.portal-header :deep(.portal-link:hover),
:deep(.portal-link:hover) {
  color: var(--pf-primary-hover);
}

:deep(.portal-submit) {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.06em;
  border-radius: 8px;
}

:deep(.portal-footnote) {
  margin-top: 20px;
  text-align: center;
  font-size: 12px;
  color: var(--pf-text-muted);
  line-height: 1.6;
}

@media (max-width: 480px) {
  .portal-header {
    padding: 0 20px;
  }

  .portal-card {
    padding: 24px 20px 20px;
  }
}
</style>
