/**
 * 登录状态 Store
 * 管理商户认证状态（token、merchantInfo）
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LoginResponse } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  // -------------------------------------------------------------------
  // State
  // -------------------------------------------------------------------
  const token = ref<string>(localStorage.getItem('token') ?? '')
  const merchantInfo = ref<LoginResponse['merchantInfo'] | null>(
    JSON.parse(localStorage.getItem('merchantInfo') ?? 'null')
  )

  // -------------------------------------------------------------------
  // Getters
  // -------------------------------------------------------------------
  const isLoggedIn = (): boolean => !!token.value

  // -------------------------------------------------------------------
  // Actions
  // -------------------------------------------------------------------
  function setLogin(data: LoginResponse) {
    token.value = data.token
    merchantInfo.value = data.merchantInfo

    localStorage.setItem('token', data.token)
    localStorage.setItem('merchantInfo', JSON.stringify(data.merchantInfo))
    // 同步兼容旧字段
    localStorage.setItem('merchantId', data.merchantInfo.merchantId)
  }

  function logout() {
    token.value = ''
    merchantInfo.value = null

    localStorage.removeItem('token')
    localStorage.removeItem('merchantInfo')
    localStorage.removeItem('merchantId')
    localStorage.removeItem('secret')
  }

  return {
    token,
    merchantInfo,
    isLoggedIn,
    setLogin,
    logout,
  }
})
