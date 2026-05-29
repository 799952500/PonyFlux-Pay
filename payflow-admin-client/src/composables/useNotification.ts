import { defineStore } from 'pinia'
import { ref, onUnmounted } from 'vue'
import { getUnreadCount } from '@/api/admin'

const POLL_INTERVAL = 60_000

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  let timer: ReturnType<typeof setInterval> | null = null

  async function fetchUnreadCount() {
    try {
      unreadCount.value = await getUnreadCount()
    } catch {
      /* 静默失败，下次轮询重试 */
    }
  }

  function startPolling() {
    if (timer) return
    fetchUnreadCount()
    timer = setInterval(fetchUnreadCount, POLL_INTERVAL)
  }

  function stopPolling() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function decrementUnread(n = 1) {
    unreadCount.value = Math.max(0, unreadCount.value - n)
  }

  function resetUnread() {
    unreadCount.value = 0
  }

  return { unreadCount, fetchUnreadCount, startPolling, stopPolling, decrementUnread, resetUnread }
})

/**
 * 在组件中使用：自动启动轮询，组件销毁时停止。
 */
export function useNotificationPolling() {
  const store = useNotificationStore()
  store.startPolling()
  onUnmounted(() => store.stopPolling())
  return store
}
