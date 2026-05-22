import { ref } from 'vue'

const visible = ref(false)
const merchantId = ref<string | null>(null)

/** 全站商户洞察抽屉（单例） */
export function useMerchantInsightOverlay() {
  function open(id: string) {
    const trimmed = id?.trim()
    if (!trimmed) return
    merchantId.value = trimmed
    visible.value = true
  }

  function close() {
    visible.value = false
    merchantId.value = null
  }

  return { visible, merchantId, open, close }
}
