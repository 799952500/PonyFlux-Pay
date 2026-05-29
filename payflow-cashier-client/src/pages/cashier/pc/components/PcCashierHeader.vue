<template>
  <header class="pc-header">
    <div class="pc-header__bar">
      <div class="pc-header__brand">
        <img src="/ponyflux-logo.svg" width="32" height="32" alt="" class="pc-header__logo" />
        <div class="pc-header__titles">
          <span class="pc-header__name">PonyFlux Pay</span>
          <span class="pc-header__sub">{{ t('nav.enterpriseCashier') }}</span>
        </div>
      </div>

      <div class="pc-header__center">
        <span class="pc-header__page-title">{{ t('nav.orderPayment') }}</span>
      </div>

      <div class="pc-header__actions">
        <span
          v-if="expireCountdown"
          class="pc-header__timer"
          :class="{ 'pc-header__timer--warn': isExpiringSoon }"
        >
          {{ t('nav.timeRemaining', { time: expireCountdown }) }}
        </span>
        <a href="tel:4008888888" class="pc-header__link">{{ t('nav.contactSupport') }}</a>
      </div>
    </div>
    <div class="pc-header__stamp-edge" aria-hidden="true" />
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCashierStore } from '@/stores/cashier'

const { t } = useI18n()
const cashierStore = useCashierStore()
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null

const expireCountdown = computed(() => {
  const info = cashierStore.orderInfo
  if (!info?.expireTime) return ''
  const diff = new Date(info.expireTime).getTime() - now.value
  if (diff <= 0) return t('order.expired')
  const min = Math.floor(diff / 60000)
  const sec = Math.floor((diff % 60000) / 1000)
  return t('nav.minutesSeconds', { min, sec: sec.toString().padStart(2, '0') })
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
