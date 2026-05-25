import { computed } from 'vue'
import { useAdminStore } from '@/stores/admin'

export type PermissionLogical = 'AND' | 'OR'

/**
 * 按钮权限组合式函数。
 */
export function usePermission() {
  const adminStore = useAdminStore()

  const permissionSet = computed(() => adminStore.permissionSet)

  function hasPermission(code: string | string[], logical: PermissionLogical = 'AND'): boolean {
    return adminStore.hasPermission(code, logical)
  }

  return {
    permissionSet,
    hasPermission,
  }
}
