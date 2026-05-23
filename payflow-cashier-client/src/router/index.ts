/**
 * Vue Router 配置
 * 收银台为公开页面，无需任何登录认证
 */
import { createRouter, createWebHistory, type RouteLocationGeneric } from 'vue-router'
import { installPfSurface } from '@/composables/usePfSurface'
import { buildCashierPath, resolveCashierTerminal } from '@/utils/cashierDevice'

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
      meta: { title: '收银台' },
    },
    {
      path: '/cashier/pc/:orderId',
      name: 'cashier-pc',
      component: () => import('@/pages/cashier/pc/index.vue'),
      meta: { title: '收银台', terminal: 'PC' },
    },
    {
      path: '/cashier/h5/:orderId',
      name: 'cashier-h5',
      component: () => import('@/pages/cashier/h5/index.vue'),
      meta: { title: '收银台', terminal: 'H5' },
    },

    {
      path: '/receipt/:orderId',
      name: 'receipt',
      component: () => import('@/pages/receipt/index.vue'),
      meta: { title: '电子收据' },
    },

    {
      path: '/',
      redirect: '/cashier/demo',
    },

    {
      path: '/register',
      name: 'register',
      component: () => import('@/pages/register/index.vue'),
      meta: { title: '商户入驻申请' },
    },

    {
      path: '/onboarding/result',
      name: 'onboarding-result',
      component: () => import('@/pages/onboarding/result.vue'),
      meta: { title: '入驻结果查询' },
    },

    {
      path: '/:pathMatch(.*)*',
      redirect: '/cashier/demo',
    },
  ],
})

installPfSurface()

router.afterEach((to) => {
  const title = (to.meta?.title as string | undefined) ?? '小马支付'
  const terminal = to.meta?.terminal as string | undefined
  const suffix = terminal ? ` (${terminal})` : ''
  document.title = `${title}${suffix} - 小马支付 PonyFlux Pay`
})

export default router
