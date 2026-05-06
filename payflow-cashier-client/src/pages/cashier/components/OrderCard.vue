<template>
  <div class="order-card p-4">
    <!-- 商品信息 -->
    <div class="flex items-start justify-between mb-2">
      <div class="flex-1 min-w-0">
        <h2 class="text-[15px] font-semibold text-white leading-snug">
          {{ info.subject }}
        </h2>
        <p v-if="info.body" class="text-emerald-100/75 text-xs mt-0.5 line-clamp-2">
          {{ info.body }}
        </p>
      </div>
      <!-- 状态标签 -->
      <span
        class="ml-3 flex-shrink-0 text-xs px-2 py-0.5 rounded-full font-medium"
        :class="isExpired
          ? 'bg-red-500/25 text-red-300 border border-red-400/40'
          : 'bg-emerald-500/25 text-emerald-100 border border-emerald-300/35'"
      >
        {{ isExpired ? '已过期' : '待支付' }}
      </span>
    </div>

    <!-- 分隔线 -->
    <div class="h-px bg-gradient-to-r from-transparent via-white/25 to-transparent my-3" />

    <!-- 金额 + 订单号 -->
    <div class="flex items-end justify-between">
      <div>
        <p class="text-emerald-100/70 text-xs mb-0.5">订单金额</p>
        <div class="flex items-baseline gap-0.5">
          <span class="text-emerald-100/75 text-base font-light">¥</span>
          <span class="amount-display">{{ integerPart }}</span>
          <span class="text-lg font-bold text-emerald-50">.{{ decimalPart }}</span>
        </div>
      </div>
      <div class="text-right">
        <p class="text-emerald-100/70 text-xs">订单号</p>
        <p class="text-emerald-100/55 text-xs font-mono mt-0.5">{{ info.orderId }}</p>
      </div>
    </div>

    <!-- 倒计时进度条 -->
    <div v-if="!isExpired && remainingMs > 0" class="countdown-bar-wrap">
      <div
        class="countdown-bar"
        :style="{
          width: progressPercent + '%',
          backgroundColor: progressColor,
        }"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import type { CashierInfo } from '@/types'

const props = defineProps<{
  info: CashierInfo
}>()

const emit = defineEmits<{
  (e: 'expired'): void
}>()

const amountStr = computed(() => (props.info.amount / 100).toFixed(2))
const integerPart = computed(() => {
  const [int] = amountStr.value.split('.')
  return int.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
})
const decimalPart = computed(() => amountStr.value.split('.')[1])

// ── 倒计时逻辑 ──
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null

/** 过期时间戳（ms） */
const expireTs = computed(() => {
  if (!props.info.expireTime) return 0
  return new Date(props.info.expireTime).getTime()
})

/** 进度条起点：优先订单创建时间，否则按过期前 30 分钟估算（与后端默认有效期对齐） */
const startTs = computed(() => {
  if (props.info.createdAt) {
    const t = new Date(props.info.createdAt).getTime()
    if (!Number.isNaN(t) && t > 0 && t < expireTs.value) {
      return t
    }
  }
  const fallbackMs = 30 * 60 * 1000
  return Math.max(0, expireTs.value - fallbackMs)
})

/** 剩余毫秒 */
const remainingMs = computed(() => {
  const diff = expireTs.value - now.value
  return diff > 0 ? diff : 0
})

/** 是否已过期 */
const isExpired = computed(() => {
  return expireTs.value > 0 && now.value >= expireTs.value
})

/** 进度百分比（0~100） */
const progressPercent = computed(() => {
  const total = expireTs.value - startTs.value
  if (total <= 0) return 0
  const pct = (remainingMs.value / total) * 100
  return Math.min(100, Math.max(0, pct))
})

/** 进度条颜色：绿→黄→红渐变 */
const progressColor = computed(() => {
  const pct = progressPercent.value
  if (pct > 60) return '#10b981'           // 翡翠绿
  if (pct > 30) return '#f59e0b'           // 琥珀黄
  return '#ef4444'                          // 红色
})

// 监听过期事件
let hasEmittedExpired = false

onMounted(() => {
  timer = setInterval(() => {
    now.value = Date.now()
    // 过期时向上发射事件（只触发一次）
    if (isExpired.value && !hasEmittedExpired) {
      hasEmittedExpired = true
      emit('expired')
    }
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
/* ── 倒计时进度条容器 ── */
.countdown-bar-wrap {
  margin-top: 12px;
  height: 3px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 2px;
  overflow: hidden;
}

/* ── 进度条主体 ── */
.countdown-bar {
  height: 100%;
  border-radius: 2px;
  transition: width 1s linear, background-color 3s ease;
  will-change: width;
}
</style>
