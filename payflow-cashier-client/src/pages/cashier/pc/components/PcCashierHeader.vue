<template>
  <header class="pc-header">
    <div class="pc-header__bar">
      <div class="pc-header__brand">
        <img src="/ponyflux-logo.svg" width="32" height="32" alt="" class="pc-header__logo" />
        <div class="pc-header__titles">
          <span class="pc-header__name">PonyFlux Pay</span>
          <span class="pc-header__sub">企业收银台</span>
        </div>
      </div>

      <div class="pc-header__center">
        <span class="pc-header__page-title">订单支付</span>
      </div>

      <div class="pc-header__actions">
        <span
          v-if="expireCountdown"
          class="pc-header__timer"
          :class="{ 'pc-header__timer--warn': isExpiringSoon }"
        >
          支付剩余 {{ expireCountdown }}
        </span>
        <a href="tel:4008888888" class="pc-header__link">联系客服</a>
      </div>
    </div>
    <div class="pc-header__stamp-edge" aria-hidden="true" />
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
  return `${min} 分 ${sec.toString().padStart(2, '0')} 秒`
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
