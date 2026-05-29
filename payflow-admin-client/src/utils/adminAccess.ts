import { useAdminStore } from '@/stores/admin'

/** 平台管理员（可维护全局配置、进件、费率、支付方式公共定义等） */
export function isPlatformAdmin(): boolean {
  const user = useAdminStore().user
  if (user?.platformAdmin === true) return true
  return user?.role === 'SUPER_ADMIN'
}

/** 商户管理员禁止直接访问的路由前缀 */
export const PLATFORM_ONLY_ROUTE_PREFIXES = [
  '/admin/settings',
  '/admin/users',
  '/admin/roles',
  '/admin/menus',
  '/admin/dicts',
  '/admin/data-isolation',
  '/admin/audit-logs',
  '/admin/security-audit',
  '/admin/channels',
  '/admin/payment-methods',
  '/admin/onboarding',
  '/admin/fee-rate',
  '/admin/routing/logs',
  '/admin/channel-routing',
  '/admin/insights',
  '/admin/search',
  '/admin/reconcile/sla-rules',
] as const

export function isPlatformOnlyRoute(path: string): boolean {
  return PLATFORM_ONLY_ROUTE_PREFIXES.some((prefix) => path.startsWith(prefix))
}
