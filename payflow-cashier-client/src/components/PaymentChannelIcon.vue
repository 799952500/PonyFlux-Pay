<template>
  <div class="channel-icon" :aria-label="label">
    <svg v-if="variant === 'alipay'" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect width="24" height="24" rx="6" fill="#1677FF" />
      <path
        d="M7.2 8.4h9.6M7.2 12h6.8M7.2 15.6h4.4"
        stroke="#fff"
        stroke-width="1.6"
        stroke-linecap="round"
      />
    </svg>
    <svg v-else-if="variant === 'wechat'" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect width="24" height="24" rx="6" fill="#07C160" />
      <path
        d="M8.5 9.5c2.8 0 5 1.6 5 3.6 0 .8-.3 1.5-.8 2.1l.9 2.4-2.5-1.3c-.5.1-1 .2-1.6.2-2.8 0-5-1.6-5-3.6s2.2-3.4 5-3.4z"
        fill="#fff"
      />
      <path
        d="M14.8 11.2c2.2 0 4 1.3 4 2.9 0 .7-.2 1.3-.6 1.8l.7 1.9-2-1.1c-.4.1-.8.1-1.1.1-2.2 0-4-1.3-4-2.9s1.8-2.7 4-2.7z"
        fill="#fff"
        opacity="0.92"
      />
    </svg>
    <svg v-else-if="variant === 'union'" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect width="24" height="24" rx="6" fill="#E60012" />
      <path d="M6 8h12v8H6z" fill="#fff" opacity="0.15" />
      <path d="M8 10.5h8M8 13.5h5.5" stroke="#fff" stroke-width="1.4" stroke-linecap="round" />
    </svg>
    <svg v-else viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect width="24" height="24" rx="6" fill="#64748b" />
      <path d="M7 9h10M7 12h10M7 15h6" stroke="#fff" stroke-width="1.6" stroke-linecap="round" />
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  channel: string
}>()

const variant = computed(() => {
  const c = props.channel.toUpperCase()
  if (c.includes('ALIPAY')) return 'alipay'
  if (c.includes('WECHAT')) return 'wechat'
  if (c.includes('UNION')) return 'union'
  return 'default'
})

const label = computed(() => {
  const map: Record<string, string> = {
    alipay: '支付宝',
    wechat: '微信支付',
    union: '云闪付',
    default: '支付方式',
  }
  return map[variant.value]
})
</script>

<style scoped>
.channel-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
}

.channel-icon svg {
  width: 100%;
  height: 100%;
  display: block;
}
</style>
