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
      <span class="ml-3 flex-shrink-0 text-xs px-2 py-0.5 rounded-full bg-emerald-500/25 text-emerald-100 font-medium border border-emerald-300/35">
        待支付
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
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CashierInfo } from '@/types'

const props = defineProps<{
  info: CashierInfo
}>()

const amountStr = computed(() => (props.info.amount / 100).toFixed(2))
const integerPart = computed(() => {
  const [int] = amountStr.value.split('.')
  return int.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
})
const decimalPart = computed(() => amountStr.value.split('.')[1])
</script>
