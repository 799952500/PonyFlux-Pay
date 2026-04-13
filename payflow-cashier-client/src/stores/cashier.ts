/**
 * 收银台 Pinia Store
 * 管理当前收银台会话的状态（订单信息、支付方式、支付结果等）
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { CashierInfo, PaymentMethod, PaymentResult } from '@/types'

export const useCashierStore = defineStore('cashier', () => {
  // -------------------------------------------------------------------
  // State
  // -------------------------------------------------------------------
  const orderInfo = ref<CashierInfo | null>(null)
  const selectedMethod = ref<PaymentMethod | null>(null)
  const paymentResult = ref<PaymentResult | null>(null)
  const isLoading = ref(false)
  const isPaying = ref(false)
  const showQR = ref(false)
  const qrCodeUrl = ref('')

  // -------------------------------------------------------------------
  // Actions
  // -------------------------------------------------------------------
  function setOrderInfo(info: CashierInfo) {
    orderInfo.value = info
  }

  function selectMethod(method: PaymentMethod) {
    selectedMethod.value = method
  }

  function setLoading(loading: boolean) {
    isLoading.value = loading
  }

  function setPaying(paying: boolean) {
    isPaying.value = paying
  }

  function setPaymentResult(result: PaymentResult | null) {
    paymentResult.value = result
  }

  function openQR(url: string) {
    qrCodeUrl.value = url
    showQR.value = true
  }

  function closeQR() {
    showQR.value = false
    qrCodeUrl.value = ''
  }

  function reset() {
    orderInfo.value = null
    selectedMethod.value = null
    paymentResult.value = null
    isLoading.value = false
    isPaying.value = false
    showQR.value = false
    qrCodeUrl.value = ''
  }

  return {
    // State
    orderInfo,
    selectedMethod,
    paymentResult,
    isLoading,
    isPaying,
    showQR,
    qrCodeUrl,
    // Actions
    setOrderInfo,
    selectMethod,
    setLoading,
    setPaying,
    setPaymentResult,
    openQR,
    closeQR,
    reset,
  }
})
