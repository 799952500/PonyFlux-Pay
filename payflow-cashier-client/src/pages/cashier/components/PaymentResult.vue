<template>
  <div class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 backdrop-blur-sm">
    <div class="bg-white rounded-2xl w-[360px] p-8 text-center card-shadow">
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
      <h3 class="text-xl font-bold text-gray-900 mb-2">
        {{ status === 'success' ? '支付成功' : '支付失败' }}
      </h3>
      <p class="text-gray-500 text-sm mb-6">
        {{ status === 'success' ? '您的订单已完成支付' : '支付未完成，请重试' }}
      </p>

      <!-- 返回商户 -->
      <el-button
        v-if="returnUrl"
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
          class="text-gray-400 text-sm hover:text-gray-600 transition-colors mt-1"
          @click="close"
        >
          关闭
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  status: 'success' | 'failed'
  returnUrl?: string
}>()

const emit = defineEmits<{
  (e: 'retry'): void
}>()

function goReturn() {
  const url = new URLSearchParams(window.location.search).get('returnUrl')
  if (url) window.location.href = url
}

function close() {
  window.close()
}
</script>
