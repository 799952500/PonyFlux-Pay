import { computed } from 'vue'
import { useAdminStore } from '@/stores/admin'

/**
 * 登录响应中的商户数据授权范围（与后台 JWT dataMerchantIds 一致）。
 */
export function useMerchantScope() {
  const adminStore = useAdminStore()

  const isPlatformAdmin = computed(() => {
    const user = adminStore.user
    if (user?.platformAdmin === true) return true
    return user?.role === 'SUPER_ADMIN'
  })

  const authorizedMerchantIds = computed(
    () => adminStore.user?.authorizedMerchantIds?.filter((id) => !!id) ?? []
  )

  const defaultMerchantId = computed(() => {
    if (isPlatformAdmin.value) return undefined
    const ids = authorizedMerchantIds.value
    return ids.length === 1 ? ids[0] : undefined
  })

  /** 商户管理员且已绑定授权商户时，列表应默认限定在授权范围内 */
  const merchantFilterLocked = computed(
    () => !isPlatformAdmin.value && authorizedMerchantIds.value.length > 0
  )

  function applyDefaultMerchantFilter(target: { merchantId?: string }) {
    const mid = defaultMerchantId.value
    if (mid) {
      target.merchantId = mid
    }
  }

  /**
   * 新增/保存时解析商户号：商户管理员强制使用登录授权商户；平台管理员可使用表单传入值。
   */
  function resolveMerchantIdForCreate(explicit?: string): string | undefined {
    if (isPlatformAdmin.value) {
      const v = explicit?.trim()
      return v || undefined
    }
    const mid = defaultMerchantId.value
    if (mid) {
      if (explicit && explicit !== mid && !authorizedMerchantIds.value.includes(explicit)) {
        return mid
      }
      return mid
    }
    if (explicit && isMerchantAllowed(explicit)) {
      return explicit
    }
    return undefined
  }

  /** 向请求体写入 merchantId（字段名默认 merchantId） */
  function applyMerchantIdForCreate<T extends Record<string, unknown>>(
    target: T,
    field: keyof T & string = 'merchantId' as keyof T & string
  ): string | undefined {
    const current = target[field] as string | undefined
    const mid = resolveMerchantIdForCreate(current)
    if (mid) {
      ;(target as Record<string, unknown>)[field] = mid
    }
    return mid
  }

  function isMerchantAllowed(merchantId?: string) {
    if (!merchantId || isPlatformAdmin.value) return true
    return authorizedMerchantIds.value.includes(merchantId)
  }

  function filterMerchantOptions<T extends { merchantId: string }>(list: T[]): T[] {
    if (isPlatformAdmin.value) return list
    const allowed = new Set(authorizedMerchantIds.value)
    return list.filter((item) => allowed.has(item.merchantId))
  }

  return {
    isPlatformAdmin,
    authorizedMerchantIds,
    defaultMerchantId,
    merchantFilterLocked,
    applyDefaultMerchantFilter,
    resolveMerchantIdForCreate,
    applyMerchantIdForCreate,
    isMerchantAllowed,
    filterMerchantOptions,
  }
}
