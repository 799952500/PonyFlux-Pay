<template>
  <div class="order-card p-5 mb-4">
    <p class="text-white font-semibold text-sm mb-4 flex items-center gap-2">
      <svg class="w-4 h-4 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
      </svg>
      选择支付方式
    </p>

    <div class="space-y-2.5">
      <div
        v-for="method in methods"
        :key="method.methodCode"
        class="channel-card flex items-center justify-between"
        :class="{ 'selected': selected === method.methodCode }"
        @click="$emit('update:selected', method.methodCode)"
      >
        <!-- 左侧：图标 + 名称 -->
        <div class="flex items-center gap-3">
          <!-- 渠道图标容器 -->
          <div
            class="w-11 h-11 rounded-xl flex items-center justify-center text-xl transition-colors"
            :class="selected === method.methodCode
              ? 'bg-primary/10'
              : 'bg-white/10'"
          >
            <span v-html="getChannelIcon(method.channel)" />
          </div>

          <div>
            <p class="text-white font-medium text-sm">{{ method.methodName }}</p>
            <p v-if="method.discount" class="text-emerald-600 text-xs mt-0.5 font-medium">
              🎉 {{ method.discount.name }}
            </p>
            <p v-else class="text-emerald-100/65 text-xs mt-0.5">安全、快捷</p>
          </div>
        </div>

        <!-- 右侧：优惠金额 + 选中圆圈 -->
        <div class="flex items-center gap-3 flex-shrink-0">
          <span v-if="method.discount" class="text-xs text-emerald-600 font-semibold">
            -¥{{ (method.discount.amount / 100).toFixed(2) }}
          </span>

          <!-- 自定义单选 -->
          <div
            class="w-5 h-5 rounded-full border-2 flex items-center justify-center transition-all"
            :class="selected === method.methodCode
              ? 'border-primary bg-primary shadow-sm shadow-primary/30'
              : 'border-white/35'"
          >
            <span
              v-if="selected === method.methodCode"
              class="w-2 h-2 rounded-full bg-white block"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PaymentMethod } from '@/types'

defineProps<{
  methods: PaymentMethod[]
  selected: string
}>()

defineEmits<{
  (e: 'update:selected', value: string): void
}>()

function getChannelIcon(channel: string): string {
  const icons: Record<string, string> = {
    ALIPAY: '💙',
    WECHAT_PAY: '💚',
    UNION_PAY: '💳',
    CARD: '💳',
    CASH: '💵',
  }
  return icons[channel] ?? '💳'
}
</script>
