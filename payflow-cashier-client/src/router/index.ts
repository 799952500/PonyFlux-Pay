/**
 * Vue Router 配置
 * 收银台为公开页面，无需任何登录认证
 */
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ============================================================
    // 收银台（公开，无需登录）
    // ============================================================
    {
      path: '/cashier/:orderId',
      name: 'cashier',
      component: () => import('@/pages/cashier/index.vue'),
      meta: { title: '收银台' },
    },

    // ============================================================
    // 首页重定向到 demo 收银台
    // ============================================================
    {
      path: '/',
      redirect: '/cashier/demo',
    },

    // ============================================================
    // 兜底：所有未匹配路径都重定向到 demo 收银台
    // ============================================================
    {
      path: '/:pathMatch(.*)*',
      redirect: '/cashier/demo',
    },
  ],
})

// 路由切换后更新页面标题
router.afterEach((to) => {
  const title = (to.meta?.title as string | undefined) ?? '小马支付'
  document.title = `${title} - 小马支付 PonyFlux Pay`
})

export default router
