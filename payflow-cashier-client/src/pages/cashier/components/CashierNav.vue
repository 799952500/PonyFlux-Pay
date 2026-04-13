<template>
  <header class="cashier-nav">
    <div class="max-w-[540px] mx-auto px-6 h-[60px] flex items-center justify-between">
      <!-- Logo & 商户名 -->
      <div class="flex items-center gap-3">
        <!-- 品牌徽标区 -->
        <div class="flex flex-col leading-none">
          <span class="nav-brand-name">小马支付</span>
          <span class="nav-brand-en">PonyFlux Pay</span>
        </div>

        <!-- 分隔符 + 商户名 -->
        <template v-if="merchantName">
          <span class="text-slate-600 text-lg font-light mx-1">|</span>
          <span class="text-slate-300 text-sm font-medium">{{ merchantName }}</span>
        </template>
      </div>

      <!-- 右侧操作 -->
      <div class="flex items-center gap-4">
        <!-- 倒计时 -->
        <span
          v-if="expireCountdown"
          class="text-xs px-2.5 py-1 rounded-full"
          :class="isExpiringSoon
            ? 'bg-amber-500/20 text-amber-300 font-semibold'
            : 'bg-white/10 text-slate-400'"
        >
          {{ expireCountdown }}
        </span>

        <!-- 客服电话 -->
        <a
          href="tel:4008888888"
          class="nav-link flex items-center gap-1"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"
            />
          </svg>
          客服
        </a>
      </div>
    </div>

    <!-- 底部紫色光晕线 -->
    <div class="h-px bg-gradient-to-r from-transparent via-indigo-500/40 to-transparent" />
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useCashierStore } from '@/stores/cashier'

const cashierStore = useCashierStore()

const merchantName = computed(() => cashierStore.orderInfo?.merchantName ?? '')

// 超时倒计时
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null

const expireCountdown = computed(() => {
  const info = cashierStore.orderInfo
  if (!info?.expireTime) return ''
  const diff = new Date(info.expireTime).getTime() - now.value
  if (diff <= 0) return '已过期'
  const min = Math.floor(diff / 60000)
  const sec = Math.floor((diff % 60000) / 1000)
  return `剩余 ${min}:${sec.toString().padStart(2, '0')}`
})

// 剩余 3 分钟内高亮警示
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
