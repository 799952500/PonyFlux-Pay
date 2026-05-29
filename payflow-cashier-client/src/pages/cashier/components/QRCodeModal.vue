<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('qr.title')"
    :width="fullscreen ? 'min(360px, 92vw)' : '340px'"
    :class="{ 'qr-dialog--fullscreen': fullscreen }"
    :show-close="false"
    :close-on-click-modal="false"
    align-center
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="text-center mb-4">
      <p class="text-gray-500 text-sm mb-1">{{ t('qr.amountLabel') }}</p>
      <p class="text-2xl font-bold text-gray-900">
        ¥{{ amount ? (amount / 100).toFixed(2) : '—' }}
      </p>
    </div>

    <div class="flex justify-center mb-4">
      <div v-if="isRefreshing" class="qr-refresh-loading">
        <div class="qr-spinner" />
        <p class="text-xs text-gray-400 mt-2">{{ t('qr.refreshing') }}</p>
      </div>
      <div v-else-if="qrUrl" class="p-3 bg-white rounded-xl border border-gray-200">
        <div class="w-[200px] h-[200px] bg-gray-100 rounded-lg flex items-center justify-center text-gray-400 text-xs select-all break-all text-center px-2">
          {{ qrUrl.length > 80 ? qrUrl.slice(0, 80) + '...' : qrUrl }}
        </div>
      </div>
      <el-skeleton v-else animated style="width: 200px; height: 200px" />
    </div>

    <p class="text-center text-sm text-gray-500 mb-2">
      {{ t('qr.scanTip', { app: scanTarget }) }}
    </p>

    <p class="text-center text-xs text-gray-400">
      {{ t('qr.refreshIn', { time: refreshCountdownText }) }}
    </p>

    <div class="flex justify-center mt-3">
      <button
        class="refresh-btn"
        :disabled="isRefreshing"
        @click="handleManualRefresh"
      >
        <svg
          class="w-3.5 h-3.5"
          :class="{ 'animate-spin': isRefreshing }"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
          />
        </svg>
        {{ t('qr.refresh') }}
      </button>
    </div>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">{{ t('qr.cancel') }}</el-button>
      <el-button type="primary" :loading="confirming" @click="$emit('confirm')">
        {{ t('qr.confirmPaid') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    qrUrl: string
    amount?: number
    confirming?: boolean
    fullscreen?: boolean
  }>(),
  { fullscreen: false }
)

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'confirm'): void
  (e: 'refresh'): void
}>()

const scanTarget = computed(() => {
  const url = props.qrUrl ?? ''
  if (url.includes('weixin') || url.includes('wxpki')) return t('qr.wechat')
  if (url.includes('alipay') || url.includes('render')) return t('qr.alipay')
  return t('qr.defaultApp')
})

const REFRESH_INTERVAL = 5 * 60 * 1000
const remainingMs = ref(REFRESH_INTERVAL)
const isRefreshing = ref(false)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const refreshCountdownText = computed(() => {
  const totalSec = Math.max(0, Math.ceil(remainingMs.value / 1000))
  const min = Math.floor(totalSec / 60)
  const sec = totalSec % 60
  return `${min}:${sec.toString().padStart(2, '0')}`
})

function startCountdown() {
  stopCountdown()
  remainingMs.value = REFRESH_INTERVAL
  const tick = 1000
  countdownTimer = setInterval(() => {
    remainingMs.value -= tick
    if (remainingMs.value <= 0) {
      triggerRefresh()
    }
  }, tick)
}

function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

async function triggerRefresh() {
  if (isRefreshing.value) return
  isRefreshing.value = true
  stopCountdown()
  try {
    emit('refresh')
    await new Promise(resolve => setTimeout(resolve, 1500))
  } finally {
    isRefreshing.value = false
    startCountdown()
  }
}

function handleManualRefresh() {
  triggerRefresh()
}

watch(() => props.modelValue, (open) => {
  if (open) {
    startCountdown()
  } else {
    stopCountdown()
  }
}, { immediate: true })

onUnmounted(() => {
  stopCountdown()
})
</script>

<style scoped>
.qr-refresh-loading {
  width: 200px;
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.qr-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e5e7eb;
  border-top-color: #047857;
  border-radius: 50%;
  animation: qr-spin 0.8s linear infinite;
}

@keyframes qr-spin {
  to { transform: rotate(360deg); }
}

.refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  font-size: 12px;
  color: #047857;
  background: rgba(4, 120, 87, 0.08);
  border: 1px solid rgba(4, 120, 87, 0.2);
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: rgba(4, 120, 87, 0.15);
  border-color: rgba(4, 120, 87, 0.35);
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
