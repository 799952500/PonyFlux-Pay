<template>
  <QRCodeModal
    :model-value="showQR"
    :qr-url="cashierStore.qrCodeUrl"
    :amount="cashierStore.orderInfo?.amount"
    :confirming="confirming"
    :fullscreen="fullscreenQr ?? false"
    @update:model-value="onQrVisible"
    @confirm="$emit('confirm-pay')"
  />

  <PaymentResult
    v-if="payResult"
    :status="payResult"
    :order-id="cashierStore.orderInfo?.orderId"
    :success-url="cashierStore.orderInfo?.successUrl"
    :fail-url="cashierStore.orderInfo?.failUrl"
    @retry="$emit('retry')"
  />
</template>

<script setup lang="ts">
import QRCodeModal from './QRCodeModal.vue'
import PaymentResult from './PaymentResult.vue'
import { useCashierStore } from '@/stores/cashier'

defineProps<{
  showQR: boolean
  confirming: boolean
  payResult: 'success' | 'failed' | null
  cashierStore: ReturnType<typeof useCashierStore>
  fullscreenQr?: boolean
}>()

const emit = defineEmits<{
  (e: 'confirm-pay'): void
  (e: 'retry'): void
  (e: 'update:showQR', value: boolean): void
}>()

function onQrVisible(v: boolean) {
  emit('update:showQR', v)
}
</script>
