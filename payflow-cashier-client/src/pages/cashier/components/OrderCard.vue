<template>
  <div class="order-card p-4">
    <!-- 商品信息 -->
    <div class="flex items-start justify-between mb-2">
      <div class="flex-1 min-w-0">
        <h2 class="text-[15px] font-semibold text-[#0F172A] leading-snug">
          {{ info.subject }}
        </h2>
        <p v-if="info.body" class="text-[#64748B] text-xs mt-0.5 line-clamp-2">
          {{ info.body }}
        </p>
      </div>
      <span class="ml-3 flex-shrink-0 text-xs px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600 font-medium border border-emerald-100">
        待支付
      </span>
    </div>

    <!-- 分隔线 -->
    <div class="h-px bg-gradient-to-r from-transparent via-indigo-100 to-transparent my-3" />

    <!-- 金额 + 订单号 -->
    <div class="flex items-end justify-between">
      <div>
        <p class="text-[#64748B] text-xs mb-0.5">订单金额</p>
        <div class="flex items-baseline gap-0.5">
          <span class="text-[#64748B] text-base font-light">¥</span>
          <span class="amount-display">{{ integerPart }}</span>
          <span class="text-lg font-bold text-[#0F172A]">.{{ decimalPart }}</span>
        </div>
      </div>
      <div class="text-right">
        <p class="text-[#64748B] text-xs">订单号</p>
        <p class="text-[#94A3B8] text-xs font-mono mt-0.5">{{ info.orderId }}</p>
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
