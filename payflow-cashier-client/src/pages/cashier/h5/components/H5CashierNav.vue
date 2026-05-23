<template>
  <header class="h5-nav">
    <div class="h5-nav__brand">
      <img src="/ponyflux-logo.svg" width="28" height="28" alt="" class="h5-nav__logo" />
      <span class="h5-nav__title">小马支付</span>
    </div>
    <span
      v-if="expireCountdown"
      class="h5-nav__timer"
      :class="{ 'h5-nav__timer--warn': isExpiringSoon }"
    >
      {{ expireCountdown }}
    </span>
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
