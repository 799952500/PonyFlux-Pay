import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { AdminLoginResponse } from '@/types'

export const useAdminStore = defineStore('admin', () => {
  const stored = localStorage.getItem('adminUser')
  const user = ref<AdminLoginResponse | null>(stored ? JSON.parse(stored) : null)

  const token = ref(localStorage.getItem('adminToken') ?? '')

  function setAuth(loginData: AdminLoginResponse) {
    token.value = loginData.token
    user.value = loginData
    localStorage.setItem('adminToken', loginData.token)
    localStorage.setItem('adminUser', JSON.stringify(loginData))
  }

  function clearAuth() {
    token.value = ''
    user.value = null
    localStorage.removeItem('adminToken')
    localStorage.removeItem('adminUser')
  }

  function isLoggedIn() {
    return !!token.value
  }

  return { user, token, setAuth, clearAuth, isLoggedIn }
})
