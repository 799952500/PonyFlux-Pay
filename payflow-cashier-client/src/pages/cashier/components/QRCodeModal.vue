<template>
  <el-dialog
    :model-value="modelValue"
    title="扫码支付"
    width="340px"
    :show-close="false"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <!-- 金额 -->
    <div class="text-center mb-4">
      <p class="text-gray-500 text-sm mb-1">待支付金额</p>
      <p class="text-2xl font-bold text-gray-900">
        ¥{{ amount ? (amount / 100).toFixed(2) : '—' }}
      </p>
    </div>

    <!-- 二维码 -->
    <div class="flex justify-center mb-4">
      <div v-if="qrUrl" class="p-3 bg-white rounded-xl border border-gray-200">
        <div class="w-[200px] h-[200px] bg-gray-100 rounded-lg flex items-center justify-center text-gray-400 text-xs select-all break-all text-center px-2">
          {{ qrUrl.length > 80 ? qrUrl.slice(0, 80) + '...' : qrUrl }}
        </div>
      </div>
      <el-skeleton v-else animated style="width: 200px; height: 200px" />
    </div>

    <!-- 提示 -->
    <p class="text-center text-sm text-gray-500 mb-2">
      请使用{{ scanTarget }}扫码支付
    </p>
    <p class="text-center text-xs text-gray-400">
      二维码有效期 15 分钟，请尽快支付
    </p>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="confirming" @click="$emit('confirm')">
        我已支付
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  modelValue: boolean
  qrUrl: string
  amount?: number
  confirming?: boolean
}>()

defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'confirm'): void
}>()

const scanTarget = computed(() => {
  const url = props.qrUrl ?? ''
  if (url.includes('weixin') || url.includes('wxpki')) return '微信'
  if (url.includes('alipay') || url.includes('render')) return '支付宝'
  return '对应 App'
})
</script>
