import { ref } from 'vue'

const visible = ref(false)
const orderId = ref<string | null>(null)

/** 全站订单详情抽屉（单例） */
export function useOrderDetailOverlay() {
  function open(id: string) {
    const trimmed = id?.trim()
    if (!trimmed) return
    orderId.value = trimmed
    visible.value = true
  }

  function close() {
    visible.value = false
    orderId.value = null
  }

  return { visible, orderId, open, close }
}
