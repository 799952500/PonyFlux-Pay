import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/pages/login/index.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/admin',
      component: () => import('@/pages/admin/layout.vue'),
      meta: { requiresAuth: true, title: '管理后台' },
      children: [
        {
          path: '',
          redirect: '/admin/dashboard',
        },
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/pages/admin/dashboard.vue'),
          meta: { title: '数据概览', requiresAuth: true },
        },
        {
          path: 'orders',
          name: 'Orders',
          component: () => import('@/pages/admin/orders/index.vue'),
          meta: { title: '订单管理', requiresAuth: true },
        },
        {
          path: 'orders/:orderId',
          name: 'OrderDetail',
          component: () => import('@/pages/admin/orders/detail.vue'),
          meta: { title: '订单详情', requiresAuth: true },
        },
        {
          path: 'refunds',
          name: 'Refunds',
          component: () => import('@/pages/admin/refunds.vue'),
          meta: { title: '退款管理', requiresAuth: true },
        },
        {
          path: 'channels',
          name: 'Channels',
          component: () => import('@/pages/admin/channels.vue'),
          meta: { title: '渠道管理', requiresAuth: true },
        },
        {
          path: 'risk',
          name: 'Risk',
          component: () => import('@/pages/admin/risk.vue'),
          meta: { title: '风控配置', requiresAuth: true },
        },
        {
          path: 'merchants',
          name: 'Merchants',
          component: () => import('@/pages/admin/merchants.vue'),
          meta: { title: '商户管理', requiresAuth: true },
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/pages/admin/settings.vue'),
          meta: { title: '系统设置', requiresAuth: true },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/login',
    },
  ],
})

// -------------------------------------------------------------------
// 路由守卫
// -------------------------------------------------------------------
router.beforeEach((to, _from, next) => {
  // 设置页面标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} - PayFlow 管理平台`
  }

  const token = localStorage.getItem('adminToken')

  // 需要登录但没有 token → 跳转登录页
  if (to.meta?.requiresAuth && !token) {
    next('/login')
    return
  }

  // 已有 token 却访问登录页 → 跳转后台首页
  if (to.path === '/login' && token) {
    next('/admin/dashboard')
    return
  }

  next()
})

export default router
