<template>
  <section class="pc-order-panel pc-order-panel--ticket">
    <div class="pc-order-panel__banner">
      <div class="pc-order-panel__banner-text">
        <span class="pc-order-panel__banner-title">电子订单</span>
        <span class="pc-order-panel__banner-sub">Electronic Order</span>
      </div>
      <img src="/ponyflux-logo.svg" alt="" class="pc-order-panel__banner-logo" width="28" height="28" />
    </div>

    <div class="pc-order-panel__body">
      <div class="pc-order-panel__meta">
        <h2 class="pc-order-panel__title">订单信息</h2>
        <span class="pc-order-panel__status" :class="isExpired ? 'is-danger' : 'is-pending'">
          {{ isExpired ? '已过期' : '待支付' }}
        </span>
      </div>

      <div class="pc-order-panel__merchant">
        <div class="pc-order-panel__avatar">{{ merchantInitial }}</div>
        <div>
          <p class="pc-order-panel__merchant-name">{{ info.merchantName }}</p>
          <p class="pc-order-panel__merchant-label">收款商户</p>
        </div>
      </div>

      <dl class="pc-order-dl">
        <div class="pc-order-dl__row">
          <dt>商品名称</dt>
          <dd>{{ info.subject }}</dd>
        </div>
        <div v-if="info.body" class="pc-order-dl__row">
          <dt>商品说明</dt>
          <dd>{{ info.body }}</dd>
        </div>
        <div class="pc-order-dl__row">
          <dt>订单编号</dt>
          <dd class="pc-order-dl__mono">{{ info.orderId }}</dd>
        </div>
        <div class="pc-order-dl__row">
          <dt>创建时间</dt>
          <dd>{{ formatTime(info.createdAt) }}</dd>
        </div>
        <div class="pc-order-dl__row">
          <dt>过期时间</dt>
          <dd>{{ formatTime(info.expireTime) }}</dd>
        </div>
        <div class="pc-order-dl__row pc-order-dl__row--amount">
          <dt>应付金额</dt>
          <dd>
            <span class="pc-order-amount">
              <span class="pc-order-amount__currency">¥</span>
              <span class="pc-order-amount__int">{{ integerPart }}</span>
              <span class="pc-order-amount__dec">.{{ decimalPart }}</span>
            </span>
          </dd>
        </div>
      </dl>

      <div v-if="!isExpired && remainingMs > 0" class="pc-order-timer">
        <div class="pc-order-timer__label">
          <span>支付时限</span>
          <span class="pc-order-timer__text">{{ countdownText }}</span>
        </div>
        <div class="pc-order-timer__track">
          <div
            class="pc-order-timer__bar"
            :style="{ width: progressPercent + '%', backgroundColor: progressColor }"
          />
        </div>
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

const emit = defineEmits<{ (e: 'expired'): void }>()

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

const expireTs = computed(() =>
  props.info.expireTime ? new Date(props.info.expireTime).getTime() : 0
)
const remainingMs = computed(() => Math.max(0, expireTs.value - now.value))
const isExpired = computed(() => remainingMs.value <= 0)

const countdownText = computed(() => {
  if (isExpired.value) return '已超时'
  const min = Math.floor(remainingMs.value / 60000)
  const sec = Math.floor((remainingMs.value % 60000) / 1000)
  return `${min}:${sec.toString().padStart(2, '0')}`
})

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

function formatTime(iso?: string): string {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return iso
  }
}

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
