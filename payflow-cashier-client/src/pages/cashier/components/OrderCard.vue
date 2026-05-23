<template>
  <section class="checkout-order">
    <div class="checkout-merchant">
      <div class="checkout-merchant__avatar">{{ merchantInitial }}</div>
      <div class="checkout-merchant__meta">
        <span class="checkout-merchant__name">{{ info.merchantName }}</span>
        <span class="checkout-merchant__label">收款方</span>
      </div>
      <span class="checkout-status" :class="isExpired ? 'checkout-status--danger' : 'checkout-status--pending'">
        {{ isExpired ? '已过期' : '待支付' }}
      </span>
    </div>

    <div class="checkout-amount-block">
      <p class="checkout-amount-label">支付金额</p>
      <div class="checkout-amount">
        <span class="checkout-amount__currency">¥</span>
        <span class="checkout-amount__int">{{ integerPart }}</span>
        <span class="checkout-amount__dec">.{{ decimalPart }}</span>
      </div>
      <h2 class="checkout-subject">{{ info.subject }}</h2>
      <p v-if="info.body" class="checkout-body">{{ info.body }}</p>
    </div>

    <div class="checkout-meta">
      <span class="checkout-meta__item">
        <span class="checkout-meta__label">订单号</span>
        <span class="checkout-meta__value">{{ info.orderId }}</span>
      </span>
    </div>

    <div v-if="!isExpired && remainingMs > 0" class="checkout-timer">
      <div class="checkout-timer__track">
        <div
          class="checkout-timer__bar"
          :style="{ width: progressPercent + '%', backgroundColor: progressColor }"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import type { CashierInfo } from '@/types'

const props = defineProps<{
  info: CashierInfo
  merchantInitial?: string
}>()

const emit = defineEmits<{
  (e: 'expired'): void
}>()

const merchantInitial = computed(
  () => props.merchantInitial ?? props.info.merchantName?.charAt(0).toUpperCase() ?? '?'
)

const amountStr = computed(() => (props.info.amount / 100).toFixed(2))
const integerPart = computed(() => {
  const [int] = amountStr.value.split('.')
  return int.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
})
const decimalPart = computed(() => amountStr.value.split('.')[1])

const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null

const expireTs = computed(() => {
  if (!props.info.expireTime) return 0
  return new Date(props.info.expireTime).getTime()
})

const remainingMs = computed(() => Math.max(0, expireTs.value - now.value))
const isExpired = computed(() => remainingMs.value <= 0)

const totalMs = computed(() => {
  if (!props.info.expireTime || !props.info.createdAt) return 15 * 60 * 1000
  const start = new Date(props.info.createdAt).getTime()
  return Math.max(expireTs.value - start, 60_000)
})

const progressPercent = computed(() => {
  if (isExpired.value) return 0
  return Math.min(100, (remainingMs.value / totalMs.value) * 100)
})

const progressColor = computed(() => {
  const pct = progressPercent.value
  if (pct > 50) return 'var(--pf-primary-hover)'
  if (pct > 20) return '#d97706'
  return '#dc2626'
})

onMounted(() => {
  timer = setInterval(() => {
    now.value = Date.now()
    if (isExpired.value) emit('expired')
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>
