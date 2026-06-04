/**
 * Vue Router 配置
 * 收银台为公开页面，无需任何登录认证
 */
import { createRouter, createWebHistory, type RouteLocationGeneric } from 'vue-router'
import { installPfSurface } from '@/composables/usePfSurface'
import { buildCashierPath, resolveCashierTerminal } from '@/utils/cashierDevice'
import { i18n } from '@/i18n'
import { applyDisplayLocale, detectBrowserLocale, isPortalRoute } from '@/composables/useDisplayLocale'

function redirectCashierByDevice(to: RouteLocationGeneric) {
  const orderId = (to.params.orderId as string) || 'demo'
  const query = to.query as Record<string, string | string[] | undefined>
  const search = new URLSearchParams()
  for (const [key, val] of Object.entries(query)) {
    if (val === undefined) continue
    if (Array.isArray(val)) val.forEach((v) => search.append(key, v))
    else search.set(key, val)
  }
  const terminal = resolveCashierTerminal(search)
  return buildCashierPath(orderId, terminal, query)
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/cashier/:orderId',
      name: 'cashier',
      redirect: redirectCashierByDevice,
      meta: { titleKey: 'router.cashier' },
    },
    {
      path: '/cashier/pc/:orderId',
      name: 'cashier-pc',
      component: () => import('@/pages/cashier/pc/index.vue'),
      meta: { titleKey: 'router.cashier', terminal: 'PC', portal: false },
    },
    {
      path: '/cashier/h5/:orderId',
      name: 'cashier-h5',
      component: () => import('@/pages/cashier/h5/index.vue'),
      meta: { titleKey: 'router.cashier', terminal: 'H5', portal: false },
    },

    {
      path: '/receipt/:orderId',
      name: 'receipt',
      component: () => import('@/pages/receipt/index.vue'),
      meta: { titleKey: 'router.receipt', portal: false },
    },

    {
      path: '/',
      redirect: '/cashier/demo',
    },

    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/login/index.vue'),
      meta: { titleKey: 'portal.loginTitle', portal: true },
    },

    {
      path: '/register',
      name: 'register',
      component: () => import('@/pages/register/index.vue'),
      meta: { titleKey: 'router.register', portal: true },
    },

    {
      path: '/onboarding/result',
      name: 'onboarding-result',
      component: () => import('@/pages/onboarding/result.vue'),
      meta: { titleKey: 'router.onboardingResult', portal: true },
    },

    {
      path: '/:pathMatch(.*)*',
      redirect: '/cashier/demo',
    },
  ],
})

installPfSurface()

router.beforeEach(async (to) => {
  if (isPortalRoute(to.path)) {
    await applyDisplayLocale(detectBrowserLocale(), { persist: true })
  }
})

router.afterEach((to) => {
  const { t } = i18n.global
  const titleKey = (to.meta?.titleKey as string | undefined) ?? 'router.default'
  const title = t(titleKey)
  const terminal = to.meta?.terminal as string | undefined
  const suffix = terminal ? ` (${terminal})` : ''
  document.title = `${title}${suffix} - ${t('app.titleSuffix')}`
})

export default router
