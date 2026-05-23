<template>
  <div class="receipt-shell">
    <CashierNav />

    <main class="flex-1 flex flex-col items-center px-5 pb-10 pt-4">
      <div class="w-full max-w-[480px]">
        <button type="button" class="receipt-back mb-4" @click="goBack">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
          返回
        </button>

        <div v-if="loading" class="py-16 flex justify-center">
          <el-skeleton animated :rows="8" class="w-full" />
        </div>

        <div v-else-if="receipt" class="receipt-card">
          <h1 class="text-xl font-bold text-center pf-text-title mb-1">电子收据</h1>
          <p class="text-center pf-text-muted text-xs mb-6">由 PonyFlux Pay 开具</p>

          <dl class="space-y-3 text-sm">
            <div class="receipt-row">
              <dt class="pf-text-muted shrink-0">收据编号</dt>
              <dd class="text-right font-mono pf-text-title break-all">{{ receipt.receiptNo ?? '—' }}</dd>
            </div>
            <div class="receipt-row">
              <dt class="pf-text-muted shrink-0">生成时间</dt>
              <dd class="text-right pf-text-body">{{ receipt.generatedAt ?? '—' }}</dd>
            </div>
            <div class="receipt-row">
              <dt class="pf-text-muted shrink-0">商户</dt>
              <dd class="text-right pf-text-body">{{ receipt.merchantName }}</dd>
            </div>
            <div class="receipt-row">
              <dt class="pf-text-muted shrink-0">商品</dt>
              <dd class="text-right pf-text-body">{{ receipt.subject }}</dd>
            </div>
            <div class="receipt-row">
              <dt class="pf-text-muted shrink-0">订单号</dt>
              <dd class="text-right font-mono text-xs pf-text-body break-all">{{ receipt.orderId }}</dd>
            </div>
            <div class="receipt-row">
              <dt class="pf-text-muted shrink-0">金额</dt>
              <dd class="text-right">
                <span class="text-lg font-semibold" style="color: var(--pf-amount)">¥{{ amountYuan }}</span>
                <span class="pf-text-muted text-xs ml-1">{{ receipt.currency }}</span>
              </dd>
            </div>
            <div class="receipt-row">
              <dt class="pf-text-muted shrink-0">大写</dt>
              <dd class="text-right pf-text-body text-xs leading-snug max-w-[240px]">{{ receipt.amountCn }}</dd>
            </div>
            <div class="receipt-row">
              <dt class="pf-text-muted shrink-0">支付渠道</dt>
              <dd class="text-right pf-text-body">{{ receipt.payChannel }}</dd>
            </div>
            <div class="receipt-row">
              <dt class="pf-text-muted shrink-0">支付时间</dt>
              <dd class="text-right pf-text-body">{{ receipt.payTime || '—' }}</dd>
            </div>
            <div class="receipt-row receipt-row--last">
              <dt class="pf-text-muted shrink-0">流水号</dt>
              <dd class="text-right font-mono text-xs pf-text-body break-all">{{ receipt.transactionNo || '—' }}</dd>
            </div>
          </dl>

          <button type="button" class="pay-btn mt-6" @click="downloadPdf">下载 PDF 收据</button>
        </div>

        <p v-else class="text-center pf-text-muted py-16 text-sm">暂无收据数据</p>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { buildCashierEntryPath } from '@/utils/cashierDevice'
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
  router.push(buildCashierEntryPath(oid, route.query as Record<string, string>))
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
.receipt-back {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--pf-text-secondary);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  transition: color 0.2s;
}

.receipt-back:hover {
  color: var(--pf-primary-hover);
}

.receipt-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--pf-divider);
}

.receipt-row--last {
  border-bottom: none;
  padding-bottom: 0;
}
</style>
