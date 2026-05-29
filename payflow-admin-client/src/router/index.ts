import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAdminStore } from '@/stores/admin'
import { installFlip } from '@/transitions/flipShared'
import request from '@/api/request'
import { isPlatformAdmin, isPlatformOnlyRoute } from '@/utils/adminAccess'

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
          path: 'dashboard/merchant/:merchantId',
          redirect: (to) => ({
            path: '/admin/merchants',
            query: { insight: String(to.params.merchantId ?? '') },
          }),
        },
        {
          path: 'dashboard/churn-alerts',
          name: 'ChurnAlerts',
          component: () => import('@/pages/admin/ChurnAlerts.vue'),
          meta: { title: '流失预警', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'insights/funnel',
          name: 'InsightsFunnel',
          component: () => import('@/pages/admin/insights-funnel.vue'),
          meta: { title: '支付漏斗', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'onboarding',
          name: 'Onboarding',
          component: () => import('@/pages/admin/onboarding.vue'),
          meta: { title: '商户进件', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'channel-routing/health',
          name: 'ChannelRoutingHealth',
          component: () => import('@/pages/admin/channel-routing-health.vue'),
          meta: { title: '路由健康度', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'notifications',
          name: 'Notifications',
          component: () => import('@/pages/admin/notifications.vue'),
          meta: { title: '通知中心', requiresAuth: true },
        },
        {
          path: 'search',
          name: 'AdminSearch',
          component: () => import('@/pages/admin/search.vue'),
          meta: { title: '全局搜索', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'orders',
          name: 'Orders',
          component: () => import('@/pages/admin/orders/index.vue'),
          meta: { title: '订单管理', requiresAuth: true },
        },
        {
          path: 'orders/:orderId',
          redirect: (to) => ({
            path: '/admin/orders',
            query: { orderId: String(to.params.orderId ?? '') },
          }),
        },
        {
          path: 'refunds',
          name: 'Refunds',
          component: () => import('@/pages/admin/refunds.vue'),
          meta: { title: '退款管理', requiresAuth: true },
        },
        {
          path: 'merchant-notifies',
          name: 'MerchantNotifies',
          component: () => import('@/pages/admin/merchant-notifies/index.vue'),
          meta: { title: '回调记录', requiresAuth: true },
        },
        {
          path: 'reconcile',
          name: 'Reconcile',
          component: () => import('@/pages/admin/reconcile/index.vue'),
          meta: { title: '对账管理', requiresAuth: true },
        },
        {
          path: 'reconcile/tasks',
          name: 'ReconcileTasks',
          component: () => import('@/pages/admin/reconcile/tasks.vue'),
          meta: { title: '对账任务', requiresAuth: true },
        },
        {
          path: 'reconcile/results',
          name: 'ReconcileResults',
          component: () => import('@/pages/admin/reconcile/results.vue'),
          meta: { title: '对账结果', requiresAuth: true },
        },
        {
          path: 'reconcile/summary',
          name: 'ReconcileSummary',
          component: () => import('@/pages/admin/reconcile/summary.vue'),
          meta: { title: '对账汇总', requiresAuth: true },
        },
        {
          path: 'reconcile/work-items',
          name: 'ReconcileWorkItems',
          component: () => import('@/pages/admin/reconcile/work-items.vue'),
          meta: { title: '差异工单', requiresAuth: true },
        },
        {
          path: 'reconcile/work-items/:diffId',
          name: 'ReconcileWorkItemDetail',
          component: () => import('@/pages/admin/reconcile/work-item-detail.vue'),
          meta: { title: '工单详情', requiresAuth: true },
        },
        {
          path: 'reconcile/sla-rules',
          name: 'ReconcileSlaRules',
          component: () => import('@/pages/admin/reconcile/sla-rules.vue'),
          meta: { title: 'SLA 规则', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'reconcile/insights-dashboard',
          name: 'ReconcileInsightsDashboard',
          component: () => import('@/pages/admin/reconcile/insights-dashboard.vue'),
          meta: { title: '差异归因看板', requiresAuth: true },
        },
        {
          path: 'reconcile/long-tail',
          name: 'ReconcileLongTail',
          component: () => import('@/pages/admin/reconcile/long-tail.vue'),
          meta: { title: '长尾差异', requiresAuth: true },
        },
        {
          path: 'reconcile/reports/:snapshotId',
          name: 'ReconcileReportDetail',
          component: () => import('@/pages/admin/reconcile/report-detail.vue'),
          meta: { title: '对账报告', requiresAuth: true },
        },
        {
          path: 'channels',
          name: 'Channels',
          component: () => import('@/pages/admin/channels.vue'),
          meta: { title: '渠道管理', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'channel-routes',
          redirect: () => ({ path: '/admin/merchants' }),
        },
        {
          path: 'merchant-payments',
          redirect: (to) => ({
            path: '/admin/merchants',
            query: to.query.merchantId
              ? { openPayment: String(to.query.merchantId) }
              : {},
          }),
        },
        {
          path: 'payment-methods',
          name: 'PaymentMethods',
          component: () => import('@/pages/admin/payment-methods.vue'),
          meta: { title: '支付方式', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'payment-accounts',
          name: 'PaymentAccounts',
          component: () => import('@/pages/admin/payment-accounts.vue'),
          meta: { title: '支付账号', requiresAuth: true },
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
          path: 'profile',
          name: 'Profile',
          component: () => import('@/pages/admin/profile.vue'),
          meta: { title: '个人中心', requiresAuth: true },
        },
        {
          path: 'preferences',
          redirect: { path: '/admin/profile', query: { tab: 'appearance' } },
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/pages/admin/settings.vue'),
          meta: { title: '系统设置', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'roles',
          name: 'Roles',
          component: () => import('@/pages/admin/roles.vue'),
          meta: { title: '角色管理', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'menus',
          name: 'Menus',
          component: () => import('@/pages/admin/menus.vue'),
          meta: { title: '菜单管理', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/pages/admin/users.vue'),
          meta: { title: '用户管理', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'data-isolation',
          name: 'DataIsolation',
          component: () => import('@/pages/admin/data-isolation.vue'),
          meta: { title: '数据隔离治理', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'audit-logs',
          name: 'AuditLogs',
          component: () => import('@/pages/admin/audit-logs.vue'),
          meta: { title: '操作日志', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'security-audit',
          name: 'SecurityAudit',
          component: () => import('@/pages/admin/security-audit.vue'),
          meta: { title: '安全审计', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'fee-rate/config',
          name: 'FeeRateConfig',
          component: () => import('@/pages/admin/FeeRateConfig.vue'),
          meta: { title: '阶梯费率配置', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'fee-rate/audit-log',
          name: 'FeeRateAuditLog',
          component: () => import('@/pages/admin/FeeRateAuditLog.vue'),
          meta: { title: '费率变更审计', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'routing/logs',
          name: 'RoutingLogs',
          component: () => import('@/pages/admin/RoutingLogs.vue'),
          meta: { title: '路由决策日志', requiresAuth: true, platformOnly: true },
        },
        {
          path: 'dicts',
          name: 'Dicts',
          component: () => import('@/pages/admin/dicts.vue'),
          meta: { title: '数据字典', requiresAuth: true, platformOnly: true },
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
router.beforeEach(async (to, _from, next) => {
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

  // 访问登录页且本地有 Token：先校验是否仍有效，避免「以为在登录其实仍在旧会话」
  if (to.path === '/login' && token) {
    try {
      await request.get('/admin/auth/profile')
      next('/admin/dashboard')
      return
    } catch {
      /* 网络异常或Token过期：视为无效，留在登录页 */
    }
    const store = useAdminStore()
    store.clearAuth()
    next()
    return
  }

  if (to.meta?.requiresAuth && to.path.startsWith('/admin')) {
    const store = useAdminStore()
    const platform =
      store.user?.platformAdmin === true || store.user?.role === 'SUPER_ADMIN'
    if (!platform && (to.meta.platformOnly === true || isPlatformOnlyRoute(to.path))) {
      ElMessage.warning('当前账号无权访问该功能')
      next(_from.path && _from.path.startsWith('/admin') ? _from.path : '/admin/dashboard')
      return
    }
  }

  next()
})

installFlip(router)

export default router
