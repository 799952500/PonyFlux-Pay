<template>
  <div class="receipt-page min-h-screen flex flex-col bg-gradient-to-b from-emerald-950 via-emerald-900 to-slate-950 text-white">
    <CashierNav />

    <main class="flex-1 flex flex-col items-center px-5 pb-10 pt-4">
      <div class="w-full max-w-[480px]">
        <button
          type="button"
          class="mb-4 text-sm text-emerald-200/70 hover:text-white flex items-center gap-1 transition-colors"
          @click="goBack"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
          返回
        </button>

        <div v-if="loading" class="py-16 flex justify-center">
          <el-skeleton animated :rows="8" class="w-full" />
        </div>

        <div
          v-else-if="receipt"
          class="rounded-2xl border border-white/15 bg-white/10 backdrop-blur-xl p-6 shadow-xl"
        >
          <h1 class="text-xl font-bold text-center mb-1">电子收据</h1>
          <p class="text-center text-emerald-100/55 text-xs mb-6">由 PonyFlux Pay 开具</p>

          <dl class="space-y-3 text-sm">
            <div class="flex justify-between gap-4 border-b border-white/10 pb-2">
              <dt class="text-emerald-100/60 shrink-0">收据编号</dt>
              <dd class="text-right font-mono text-emerald-50 break-all">{{ receipt.receiptNo ?? '—' }}</dd>
            </div>
            <div class="flex justify-between gap-4 border-b border-white/10 pb-2">
              <dt class="text-emerald-100/60 shrink-0">生成时间</dt>
              <dd class="text-right text-emerald-50">{{ receipt.generatedAt ?? '—' }}</dd>
            </div>
            <div class="flex justify-between gap-4 border-b border-white/10 pb-2">
              <dt class="text-emerald-100/60 shrink-0">商户</dt>
              <dd class="text-right text-emerald-50">{{ receipt.merchantName }}</dd>
            </div>
            <div class="flex justify-between gap-4 border-b border-white/10 pb-2">
              <dt class="text-emerald-100/60 shrink-0">商品</dt>
              <dd class="text-right text-emerald-50">{{ receipt.subject }}</dd>
            </div>
            <div class="flex justify-between gap-4 border-b border-white/10 pb-2">
              <dt class="text-emerald-100/60 shrink-0">订单号</dt>
              <dd class="text-right font-mono text-xs text-emerald-100/90 break-all">{{ receipt.orderId }}</dd>
            </div>
            <div class="flex justify-between gap-4 border-b border-white/10 pb-2">
              <dt class="text-emerald-100/60 shrink-0">金额</dt>
              <dd class="text-right">
                <span class="text-lg font-semibold text-teal-100">¥{{ amountYuan }}</span>
                <span class="text-emerald-100/50 text-xs ml-1">{{ receipt.currency }}</span>
              </dd>
            </div>
            <div class="flex justify-between gap-4 border-b border-white/10 pb-2">
              <dt class="text-emerald-100/60 shrink-0">大写</dt>
              <dd class="text-right text-emerald-100/90 text-xs leading-snug max-w-[240px]">{{ receipt.amountCn }}</dd>
            </div>
            <div class="flex justify-between gap-4 border-b border-white/10 pb-2">
              <dt class="text-emerald-100/60 shrink-0">支付渠道</dt>
              <dd class="text-right text-emerald-50">{{ receipt.payChannel }}</dd>
            </div>
            <div class="flex justify-between gap-4 border-b border-white/10 pb-2">
              <dt class="text-emerald-100/60 shrink-0">支付时间</dt>
              <dd class="text-right text-emerald-50">{{ receipt.payTime || '—' }}</dd>
            </div>
            <div class="flex justify-between gap-4 pb-1">
              <dt class="text-emerald-100/60 shrink-0">流水号</dt>
              <dd class="text-right font-mono text-xs text-emerald-100/90 break-all">{{ receipt.transactionNo || '—' }}</dd>
            </div>
          </dl>

          <button
            type="button"
            class="mt-6 w-full h-11 rounded-full text-sm font-medium text-white receipt-download-btn"
            @click="downloadPdf"
          >
            下载 PDF 收据
          </button>
        </div>

        <p v-else class="text-center text-emerald-100/60 py-16 text-sm">暂无收据数据</p>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CashierNav from '@/pages/cashier/components/CashierNav.vue'
import { getReceipt, receiptPdfUrl } from '@/api/cashier'
import type { ReceiptInfo } from '@/types'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const receipt = ref<ReceiptInfo | null>(null)

const amountYuan = computed(() =>
  receipt.value ? (receipt.value.amount / 100).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',') : '—'
)

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  const oid = route.params.orderId as string
  router.push(`/cashier/${oid}`)
}

function downloadPdf() {
  const oid = route.params.orderId as string
  window.location.href = receiptPdfUrl(oid)
}

onMounted(async () => {
  const orderId = route.params.orderId as string
  if (!orderId) {
    loading.value = false
    return
  }
  try {
    receipt.value = await getReceipt(orderId)
  } catch {
    receipt.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.receipt-download-btn {
  background: linear-gradient(180deg, #0d9488 0%, #047857 50%, #065f46 100%);
  border: none;
  cursor: pointer;
  transition: opacity 0.2s ease, transform 0.15s ease;
}

.receipt-download-btn:hover {
  opacity: 0.92;
  transform: translateY(-1px);
}
</style>
