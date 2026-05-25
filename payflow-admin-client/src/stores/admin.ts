import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { AdminLoginResponse } from '@/types'
import { applyUiPreferencesFromServer } from '@/composables/useAppearancePreferences'
import type { PermissionLogical } from '@/composables/usePermission'

export const useAdminStore = defineStore('admin', () => {
  const stored = localStorage.getItem('adminUser')
  const user = ref<AdminLoginResponse | null>(stored ? JSON.parse(stored) : null)

  const token = ref(localStorage.getItem('adminToken') ?? '')

  const permissionSet = computed(() => {
    const perms = user.value?.permissions ?? []
    return new Set(perms)
  })

  function hasPermission(code: string | string[], logical: PermissionLogical = 'AND'): boolean {
    if (user.value?.role === 'SUPER_ADMIN') {
      return true
    }
    const owned = permissionSet.value
    if (typeof code === 'string') {
      return owned.has(code)
    }
    if (code.length === 0) {
      return true
    }
    return logical === 'OR'
      ? code.some((c) => owned.has(c))
      : code.every((c) => owned.has(c))
  }

  function setPermissions(permissions: string[]) {
    if (!user.value) {
      return
    }
    user.value = { ...user.value, permissions: [...permissions] }
    localStorage.setItem('adminUser', JSON.stringify(user.value))
  }

  function setAuth(loginData: AdminLoginResponse) {
    const tok = loginData.token != null && String(loginData.token) ? String(loginData.token) : ''
    token.value = tok
    user.value = { ...loginData, token: tok }
    localStorage.setItem('adminToken', tok)
    localStorage.setItem('adminUser', JSON.stringify(user.value))
    applyUiPreferencesFromServer(loginData.uiPreferences)
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
      permissions: profile.permissions ?? prev?.permissions ?? [],
      nickname: profile.nickname ?? prev?.nickname,
      uiPreferences: profile.uiPreferences ?? prev?.uiPreferences,
      token: nextToken,
    }
    token.value = nextToken
    user.value = merged
    localStorage.setItem('adminToken', nextToken)
    localStorage.setItem('adminUser', JSON.stringify(merged))
    applyUiPreferencesFromServer(profile.uiPreferences ?? merged.uiPreferences)
  }

  function isLoggedIn() {
    return !!token.value
  }

  return {
    user,
    token,
    permissionSet,
    hasPermission,
    setPermissions,
    setAuth,
    clearAuth,
    applyProfile,
    isLoggedIn,
  }
})
