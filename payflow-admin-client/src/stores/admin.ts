import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { AdminLoginResponse } from '@/types'

export const useAdminStore = defineStore('admin', () => {
  const stored = localStorage.getItem('adminUser')
  const user = ref<AdminLoginResponse | null>(stored ? JSON.parse(stored) : null)

  const token = ref(localStorage.getItem('adminToken') ?? '')

  function setAuth(loginData: AdminLoginResponse) {
    const tok = loginData.token != null && String(loginData.token) ? String(loginData.token) : ''
    token.value = tok
    user.value = { ...loginData, token: tok }
    localStorage.setItem('adminToken', tok)
    localStorage.setItem('adminUser', JSON.stringify(user.value))
  }

  function clearAuth() {
    token.value = ''
    user.value = null
    localStorage.removeItem('adminToken')
    localStorage.removeItem('adminUser')
  }

  /** 用 profile 接口结果刷新用户信息，不覆盖当前有效 Token */
  function applyProfile(profile: AdminLoginResponse) {
    const prev = user.value
    const nextToken =
      profile.token != null && String(profile.token) ? String(profile.token) : token.value
    const merged: AdminLoginResponse = {
      adminId: profile.adminId ?? prev?.adminId,
      username: profile.username ?? prev?.username ?? '',
      role: profile.role ?? prev?.role ?? '',
      platformAdmin: profile.platformAdmin ?? prev?.platformAdmin,
      scopeMode: profile.scopeMode ?? prev?.scopeMode,
      authorizedMerchantIds: profile.authorizedMerchantIds ?? prev?.authorizedMerchantIds,
      menus: profile.menus ?? prev?.menus,
      token: nextToken,
    }
    token.value = nextToken
    user.value = merged
    localStorage.setItem('adminToken', nextToken)
    localStorage.setItem('adminUser', JSON.stringify(merged))
  }

  function isLoggedIn() {
    return !!token.value
  }

  return { user, token, setAuth, clearAuth, applyProfile, isLoggedIn }
})
