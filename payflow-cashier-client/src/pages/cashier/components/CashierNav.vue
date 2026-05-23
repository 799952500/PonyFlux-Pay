<template>
  <header class="cashier-nav">
    <div class="cashier-nav__inner">
      <div class="cashier-nav__brand">
        <img src="/ponyflux-logo.svg" width="32" height="32" alt="" class="cashier-nav__logo" />
        <span class="cashier-nav__name">PonyFlux Pay</span>
      </div>

      <div class="cashier-nav__actions">
        <span
          v-if="expireCountdown"
          class="nav-countdown"
          :class="{ 'nav-countdown--warn': isExpiringSoon }"
        >
          <svg class="nav-countdown__icon" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.2" />
            <path d="M8 4.5V8l2.5 1.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" />
          </svg>
          {{ expireCountdown }}
        </span>
        <a href="tel:4008888888" class="nav-link">帮助</a>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useCashierStore } from '@/stores/cashier'

const cashierStore = useCashierStore()

const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null

const expireCountdown = computed(() => {
  const info = cashierStore.orderInfo
  if (!info?.expireTime) return ''
  const diff = new Date(info.expireTime).getTime() - now.value
  if (diff <= 0) return '已过期'
  const min = Math.floor(diff / 60000)
  const sec = Math.floor((diff % 60000) / 1000)
  return `${min}:${sec.toString().padStart(2, '0')}`
})

const isExpiringSoon = computed(() => {
  const info = cashierStore.orderInfo
  if (!info?.expireTime) return false
  const diff = new Date(info.expireTime).getTime() - now.value
  return diff > 0 && diff < 3 * 60 * 1000
})

onMounted(() => {
  timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.cashier-nav__inner {
  max-width: 440px;
  margin: 0 auto;
  padding: 0 20px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cashier-nav__brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cashier-nav__logo {
  border-radius: 8px;
}

.cashier-nav__name {
  font-size: 15px;
  font-weight: 700;
  color: var(--pf-primary);
  letter-spacing: 0.02em;
}

.cashier-nav__actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.nav-countdown {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  padding: 4px 10px;
  border-radius: 6px;
  background: var(--pf-primary-soft);
  color: var(--pf-primary-hover);
}

.nav-countdown__icon {
  width: 14px;
  height: 14px;
}

.nav-countdown--warn {
  background: #fef3c7;
  color: #b45309;
}
</style>
