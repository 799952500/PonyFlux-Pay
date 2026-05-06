<template>
  <div class="fixed inset-0 bg-emerald-950/55 flex items-center justify-center z-50 backdrop-blur-md">
    <div
      class="rounded-2xl w-[min(360px,calc(100vw-32px))] p-8 text-center card-shadow border border-white/20 bg-white/12 backdrop-blur-xl"
    >
      <!-- 状态图标 -->
      <div class="mb-4">
        <div
          v-if="status === 'success'"
          class="w-16 h-16 mx-auto rounded-full bg-accent-soft flex items-center justify-center"
        >
          <span class="text-3xl">✅</span>
        </div>
        <div
          v-else
          class="w-16 h-16 mx-auto rounded-full bg-red-50 flex items-center justify-center"
        >
          <span class="text-3xl">❌</span>
        </div>
      </div>

      <!-- 标题 -->
      <h3 class="text-xl font-bold text-white mb-2">
        {{ status === 'success' ? '支付成功' : '支付失败' }}
      </h3>
      <p class="text-emerald-100/75 text-sm mb-6">
        {{ status === 'success' ? '您的订单已完成支付' : '支付未完成，请重试' }}
      </p>

      <!-- 查看收据按钮（仅支付成功时显示） -->
      <button
        v-if="status === 'success' && orderId"
        class="w-full h-[44px] rounded-full text-sm font-medium text-white mb-3 flex items-center justify-center gap-2 receipt-btn"
        @click="goReceipt"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        查看收据
      </button>

      <!-- 返回商户 -->
      <el-button
        v-if="destUrl"
        type="primary"
        class="w-full !h-[44px] !rounded-full"
        @click="goReturn"
      >
        返回商户
      </el-button>

      <!-- 重试 / 关闭 -->
      <div class="mt-3">
        <el-button
          v-if="status === 'failed'"
          class="w-full !h-[44px] !rounded-full"
          @click="$emit('retry')"
        >
          重新支付
        </el-button>
        <button
          v-else
          class="text-emerald-100/55 text-sm hover:text-white transition-colors mt-1"
          @click="close"
        >
          关闭
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps<{
  status: 'success' | 'failed'
  successUrl?: string
  failUrl?: string
  orderId?: string
}>()

const emit = defineEmits<{
  (e: 'retry'): void
}>()

const router = useRouter()

const destUrl = computed(() => {
  return props.status === 'success' ? props.successUrl : props.failUrl
})

let timer: number | undefined

function goReturn() {
  if (destUrl.value) window.location.href = destUrl.value
}

/** 跳转收据页面 */
function goReceipt() {
  if (props.orderId) {
    router.push(`/receipt/${props.orderId}`)
  }
}

onMounted(() => {
  // 支付成功时不再自动跳转，让用户有机会点击"查看收据"
  // 仅失败状态自动跳转
  if (props.status === 'failed' && destUrl.value) {
    timer = window.setTimeout(() => {
      if (destUrl.value) window.location.href = destUrl.value
    }, 3000)
  }
})

onUnmounted(() => {
  if (timer != null) clearTimeout(timer)
})

function close() {
  window.close()
}
</script>

<style scoped>
/* ── 收据按钮样式 ── */
.receipt-btn {
  background: linear-gradient(180deg, #0d9488 0%, #047857 50%, #065f46 100%);
  border: none;
  cursor: pointer;
  transition: opacity 0.2s ease, transform 0.15s ease;
}

.receipt-btn:hover {
  opacity: 0.92;
  transform: translateY(-1px);
}

.receipt-btn:active {
  transform: translateY(0);
}
</style>
