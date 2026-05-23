<template>
  <div class="result-overlay fixed inset-0 flex items-center justify-center z-50">
    <div class="result-card w-[min(360px,calc(100vw-32px))]">
      <div class="mb-4">
        <div
          v-if="status === 'success'"
          class="w-16 h-16 mx-auto rounded-full flex items-center justify-center text-3xl"
          style="background: var(--pf-primary-soft)"
        >
          ✅
        </div>
        <div
          v-else
          class="w-16 h-16 mx-auto rounded-full flex items-center justify-center text-3xl bg-red-50"
        >
          ❌
        </div>
      </div>

      <h3 class="text-xl font-bold pf-text-title mb-2">
        {{ status === 'success' ? '支付成功' : '支付失败' }}
      </h3>
      <p class="pf-text-body text-sm mb-6">
        {{ status === 'success' ? '您的订单已完成支付' : '支付未完成，请重试' }}
      </p>

      <el-button
        v-if="status === 'success' && orderId"
        type="primary"
        class="w-full !h-[44px] !rounded-lg mb-3"
        @click="goReceipt"
      >
        查看收据
      </el-button>

      <el-button v-if="destUrl" type="primary" class="w-full !h-[44px] !rounded-lg" @click="goReturn">
        返回商户
      </el-button>

      <div class="mt-3">
        <el-button v-if="status === 'failed'" class="w-full !h-[44px] !rounded-lg" @click="$emit('retry')">
          重新支付
        </el-button>
        <button v-else type="button" class="pf-text-muted text-sm hover:text-[var(--pf-primary)] mt-1" @click="close">
          关闭
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps<{
  status: 'success' | 'failed'
  orderId?: string
  successUrl?: string
  failUrl?: string
}>()

defineEmits<{
  (e: 'retry'): void
}>()

const router = useRouter()

const destUrl = computed(() => (props.status === 'success' ? props.successUrl : props.failUrl))

function goReceipt() {
  if (props.orderId) {
    router.push(`/receipt/${props.orderId}`)
  }
}

function goReturn() {
  if (destUrl.value) {
    window.location.href = destUrl.value
  }
}

function close() {
  window.close()
}
</script>
