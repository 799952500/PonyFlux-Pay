<template>
  <el-container class="h-screen overflow-hidden">
    <el-aside
      :width="themeStore.sidebarCollapsed ? '64px' : '240px'"
      class="admin-sidebar h-full flex flex-col relative"
      :class="{ 'admin-sidebar--collapsed': themeStore.sidebarCollapsed }"
    >
      <div class="admin-sidebar__inner flex flex-col h-full min-h-0">
        <!-- Logo -->
        <div class="sidebar-logo shrink-0">
          <img :src="sidebarLogoSrc" alt="PonyFlux Pay Logo" class="sidebar-logo__img" />
          <div v-show="!themeStore.sidebarCollapsed" class="sidebar-logo-text">
            <span class="sidebar-logo__name">小马支付</span>
            <span class="sidebar-logo__sub">管理平台</span>
          </div>
        </div>

        <!-- 菜单 -->
        <el-menu
          :default-active="route.path"
          :default-openeds="menuDefaultOpeneds"
          :collapse="themeStore.sidebarCollapsed"
          :collapse-transition="true"
          class="flex-1 overflow-y-auto border-none admin-menu scrollbar-dark"
          router
        >
          <template v-if="useDynamicMenu">
            <AdminSidebarMenu :nodes="dynamicMenuRoots" />
          </template>
          <template v-else>
            <el-sub-menu index="workspace-group">
              <template #title>
                <el-icon class="menu-icon"><Monitor /></el-icon>
                <span class="menu-text">{{ t('menu.groupWorkspace') }}</span>
              </template>
              <el-menu-item index="/admin/dashboard">
                <el-icon class="menu-icon"><Odometer /></el-icon>
                <span class="menu-text">{{ t('menu.dashboard') }}</span>
              </el-menu-item>
              <el-menu-item v-if="platformAdmin" index="/admin/insights/funnel">
                <el-icon class="menu-icon"><TrendCharts /></el-icon>
                <span class="menu-text">{{ t('menu.insightsFunnel') }}</span>
              </el-menu-item>
              <el-menu-item v-if="platformAdmin" index="/admin/dashboard/churn-alerts">
                <el-icon class="menu-icon"><Bell /></el-icon>
                <span class="menu-text">{{ t('menu.churnAlerts') }}</span>
              </el-menu-item>
              <el-menu-item v-if="platformAdmin" index="/admin/search">
                <el-icon class="menu-icon"><Search /></el-icon>
                <span class="menu-text">{{ t('menu.globalSearch') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/notifications">
                <el-icon class="menu-icon"><Message /></el-icon>
                <span class="menu-text">{{ t('menu.notifications') }}</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="trade-group">
              <template #title>
                <el-icon class="menu-icon"><Document /></el-icon>
                <span class="menu-text">{{ t('menu.groupTrade') }}</span>
              </template>
              <el-menu-item index="/admin/orders">
                <el-icon class="menu-icon"><List /></el-icon>
                <span class="menu-text">{{ t('menu.orders') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/refunds">
                <el-icon class="menu-icon"><RefreshLeft /></el-icon>
                <span class="menu-text">{{ t('menu.refunds') }}</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="reconcile-group">
              <template #title>
                <el-icon class="menu-icon"><Memo /></el-icon>
                <span class="menu-text">{{ t('menu.groupReconcile') }}</span>
              </template>
              <el-menu-item index="/admin/reconcile/tasks">
                <el-icon class="menu-icon"><Calendar /></el-icon>
                <span class="menu-text">{{ t('menu.reconcileTasks') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/reconcile/results">
                <el-icon class="menu-icon"><Finished /></el-icon>
                <span class="menu-text">{{ t('menu.reconcileResults') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/reconcile/summary">
                <el-icon class="menu-icon"><DataAnalysis /></el-icon>
                <span class="menu-text">{{ t('menu.reconcileSummary') }}</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="channel-account-group">
              <template #title>
                <el-icon class="menu-icon"><CreditCard /></el-icon>
                <span class="menu-text">{{ t('menu.groupChannelAccount') }}</span>
              </template>
              <el-menu-item v-if="platformAdmin" index="/admin/channels">
                <el-icon class="menu-icon"><Connection /></el-icon>
                <span class="menu-text">{{ t('menu.channels') }}</span>
              </el-menu-item>
              <el-menu-item v-if="platformAdmin" index="/admin/payment-methods">
                <el-icon class="menu-icon"><Wallet /></el-icon>
                <span class="menu-text">{{ t('menu.paymentMethods') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/payment-accounts">
                <el-icon class="menu-icon"><Money /></el-icon>
                <span class="menu-text">{{ t('menu.paymentAccounts') }}</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="platformAdmin" index="routing-group">
              <template #title>
                <el-icon class="menu-icon"><Share /></el-icon>
                <span class="menu-text">{{ t('menu.groupRouting') }}</span>
              </template>
              <el-menu-item index="/admin/channel-routing/health">
                <el-icon class="menu-icon"><CircleCheck /></el-icon>
                <span class="menu-text">{{ t('menu.channelRoutingHealth') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/routing/logs">
                <el-icon class="menu-icon"><Notebook /></el-icon>
                <span class="menu-text">{{ t('menu.routingLogs') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/fee-rate/config">
                <el-icon class="menu-icon"><Coin /></el-icon>
                <span class="menu-text">{{ t('menu.feeRateConfig') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/fee-rate/audit-log">
                <el-icon class="menu-icon"><Tickets /></el-icon>
                <span class="menu-text">{{ t('menu.feeRateAuditLog') }}</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="merchant-risk-group">
              <template #title>
                <el-icon class="menu-icon"><Shop /></el-icon>
                <span class="menu-text">{{ t('menu.groupMerchantRisk') }}</span>
              </template>
              <el-menu-item index="/admin/merchants">
                <el-icon class="menu-icon"><OfficeBuilding /></el-icon>
                <span class="menu-text">{{ t('menu.merchants') }}</span>
              </el-menu-item>
              <el-menu-item v-if="platformAdmin" index="/admin/onboarding">
                <el-icon class="menu-icon"><UserFilled /></el-icon>
                <span class="menu-text">{{ t('menu.onboarding') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/risk">
                <el-icon class="menu-icon"><Warning /></el-icon>
                <span class="menu-text">{{ t('menu.risk') }}</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="platformAdmin" index="system-group">
              <template #title>
                <el-icon class="menu-icon"><Setting /></el-icon>
                <span class="menu-text">{{ t('menu.groupSystem') }}</span>
              </template>
              <el-menu-item index="/admin/settings">
                <el-icon class="menu-icon"><Tools /></el-icon>
                <span class="menu-text">{{ t('menu.settings') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/users">
                <el-icon class="menu-icon"><Avatar /></el-icon>
                <span class="menu-text">{{ t('menu.users') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/roles">
                <el-icon class="menu-icon"><Key /></el-icon>
                <span class="menu-text">{{ t('menu.roles') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/menus">
                <el-icon class="menu-icon"><Menu /></el-icon>
                <span class="menu-text">{{ t('menu.menus') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/dicts">
                <el-icon class="menu-icon"><Collection /></el-icon>
                <span class="menu-text">{{ t('menu.dicts') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/audit-logs">
                <el-icon class="menu-icon"><DocumentCopy /></el-icon>
                <span class="menu-text">{{ t('menu.auditLogs') }}</span>
              </el-menu-item>
              <el-menu-item index="/admin/security-audit">
                <el-icon class="menu-icon"><Lock /></el-icon>
                <span class="menu-text">{{ t('menu.securityAudit') }}</span>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>

        <!-- 底部用户 -->
        <div class="sidebar-user shrink-0" role="button" tabindex="0" @click="goProfile" @keyup.enter="goProfile">
          <el-avatar :size="28" class="sidebar-user__avatar">{{ adminName?.charAt(0) }}</el-avatar>
          <div v-show="!themeStore.sidebarCollapsed" class="sidebar-user__info">
            <p class="sidebar-user__name">{{ adminName }}</p>
            <p class="sidebar-user__role">个人中心</p>
          </div>
          <el-tooltip v-if="themeStore.sidebarCollapsed" content="退出登录" placement="right">
            <button class="logout-btn" @click.stop="handleLogout" title="退出登录">
              <el-icon><SwitchButton /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip v-else content="退出登录" placement="top">
            <button class="logout-btn" @click.stop="handleLogout" title="退出登录">
              <el-icon><SwitchButton /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </div>
    </el-aside>

    <el-container class="flex-col flex-1 min-h-0 overflow-hidden">
      <!-- 顶栏 -->
      <div class="admin-topbar h-[60px] shrink-0">
        <div class="admin-topbar__inner flex h-full w-full items-center px-4 gap-3">
          <el-button text class="sidebar-toggle" @click="themeStore.toggleSidebar()">
            <el-icon :size="18">
              <Fold v-if="!themeStore.sidebarCollapsed" />
              <Expand v-else />
            </el-icon>
          </el-button>

          <h1 class="topbar-title font-semibold text-base m-0 tracking-tight">{{ pageTitle }}</h1>

          <el-input
            v-if="platformAdmin"
            v-model="topSearchQ"
            clearable
            size="default"
            placeholder="搜索订单号 / 商户订单号…"
            class="topbar-search ml-2 w-[min(280px,28vw)]"
            @keyup.enter="goGlobalSearch"
          />

          <div class="ml-auto flex items-center gap-2 topbar-actions">
            <el-badge :value="0" class="cursor-pointer" :hidden="true">
              <el-button circle class="topbar-notify-btn">
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-badge>

            <el-dropdown trigger="click" placement="bottom-end" @command="onUserMenuCommand">
              <button type="button" class="topbar-avatar-btn" title="个人中心">
                <el-avatar :size="32" class="topbar-avatar">{{ adminName?.charAt(0) }}</el-avatar>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>

      <el-main class="admin-main flex-1 min-h-0 overflow-y-auto scrollbar-light">
        <router-view v-slot="{ Component }">
          <transition name="page-fade">
            <component v-if="Component" :is="Component" :key="route.fullPath" class="admin-page-root" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <OrderDetailDrawer />
    <MerchantInsightDrawer />
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Monitor,
  Document,
  Memo,
  CreditCard,
  Shop,
  Setting,
  Fold,
  Expand,
  Bell,
  SwitchButton,
  User,
  Odometer,
  TrendCharts,
  Search,
  Message,
  List,
  RefreshLeft,
  Calendar,
  Finished,
  DataAnalysis,
  Connection,
  Wallet,
  Money,
  Share,
  CircleCheck,
  Notebook,
  Coin,
  Tickets,
  OfficeBuilding,
  UserFilled,
  Warning,
  Tools,
  Avatar,
  Key,
  Menu,
  Collection,
  DocumentCopy,
  Lock,
} from '@element-plus/icons-vue'
import { useAdminStore } from '@/stores/admin'
import { useThemeStore } from '@/stores/theme'
import { getAdminProfile } from '@/api/auth'
import AdminSidebarMenu from '@/components/AdminSidebarMenu.vue'
import OrderDetailDrawer from '@/components/orders/OrderDetailDrawer.vue'
import MerchantInsightDrawer from '@/components/merchants/MerchantInsightDrawer.vue'
import { isPlatformAdmin, isPlatformOnlyRoute } from '@/utils/adminAccess'
import type { SysMenu } from '@/types'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()
const themeStore = useThemeStore()
const platformAdmin = computed(() => isPlatformAdmin())
const sidebarLogoSrc = computed(() =>
  themeStore.themeKey === 'dark' ? '/ponyflux-logo-dark.svg' : '/ponyflux-logo.svg',
)

const topSearchQ = ref('')

function goProfile() {
  router.push('/admin/profile')
}

function onUserMenuCommand(cmd: string) {
  if (cmd === 'profile') {
    goProfile()
    return
  }
  if (cmd === 'logout') {
    handleLogout()
  }
}

function filterMenusForSidebar(menus: SysMenu[] | undefined): SysMenu[] {
  if (!menus?.length) return []
  const platform = platformAdmin.value
  const out: SysMenu[] = []
  for (const m of menus) {
    if (m.status === 'DISABLED') continue
    if (m.visible === false) continue
    if (!platform && m.path && isPlatformOnlyRoute(m.path)) continue
    const children = m.children?.length ? filterMenusForSidebar(m.children) : undefined
    if ((children && children.length > 0) || (m.path && m.path.length > 0)) {
      out.push({ ...m, children })
    } else if (!m.path && children && children.length > 0) {
      out.push({ ...m, children })
    }
  }
  return out
}

const dynamicMenuRoots = computed(() => filterMenusForSidebar(adminStore.user?.menus))
const useDynamicMenu = computed(() => dynamicMenuRoots.value.length > 0)

function collectDynamicOpenKeys(menus: SysMenu[], path: string, parents: string[] = []): string[] | null {
  for (const m of menus) {
    if (m.path === path) return parents
    if (m.children?.length) {
      const hit = collectDynamicOpenKeys(m.children, path, [...parents, 'sub-' + String(m.id)])
      if (hit !== null) return hit
    }
  }
  return null
}

function goGlobalSearch() {
  const q = topSearchQ.value.trim()
  if (!q) return
  router.push({ path: '/admin/search', query: { q } })
}

const menuDefaultOpeneds = computed(() => {
  if (useDynamicMenu.value) {
    return collectDynamicOpenKeys(dynamicMenuRoots.value, route.path) ?? []
  }
  const path = route.path
  if (
    path.startsWith('/admin/dashboard')
    || path.startsWith('/admin/insights')
    || path.startsWith('/admin/notifications')
    || path.startsWith('/admin/search')
    || path.startsWith('/admin/profile')
    || path.startsWith('/admin/preferences')
  ) {
    return ['workspace-group']
  }
  if (path.startsWith('/admin/orders') || path.startsWith('/admin/refunds')) {
    return ['trade-group']
  }
  if (path.startsWith('/admin/reconcile')) {
    return ['reconcile-group']
  }
  if (
    path.startsWith('/admin/channels')
    || path.startsWith('/admin/payment-methods')
    || path.startsWith('/admin/payment-accounts')
  ) {
    return ['channel-account-group']
  }
  if (
    path.startsWith('/admin/channel-routes')
    || path.startsWith('/admin/channel-routing')
    || path.startsWith('/admin/fee-rate')
    || path.startsWith('/admin/routing')
  ) {
    return ['routing-group']
  }
  if (
    path.startsWith('/admin/merchants')
    || path.startsWith('/admin/onboarding')
    || path.startsWith('/admin/risk')
  ) {
    return ['merchant-risk-group']
  }
  if (
    path.startsWith('/admin/settings')
    || path.startsWith('/admin/roles')
    || path.startsWith('/admin/menus')
    || path.startsWith('/admin/users')
    || path.startsWith('/admin/audit-logs')
    || path.startsWith('/admin/security-audit')
    || path.startsWith('/admin/dicts')
  ) {
    return ['system-group']
  }
  return ['workspace-group']
})

const pageTitle = computed(() => (route.meta?.title as string) ?? '控制台')

const adminName = computed(() => {
  const u = adminStore.user
  if (u?.username) return u.username
  try {
    const raw = localStorage.getItem('adminUser')
    if (raw) return (JSON.parse(raw) as { username?: string }).username ?? '管理员'
  } catch {
    /* ignore */
  }
  return '管理员'
})

onMounted(async () => {
  if (!adminStore.isLoggedIn()) return
  try {
    const p = await getAdminProfile()
    adminStore.applyProfile(p)
  } catch {
    /* 静默 */
  }
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出登录吗？', '退出确认', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
    adminStore.clearAuth()
    router.push('/login')
  } catch {
    /* cancelled */
  }
}
</script>

<style scoped>
.el-aside {
  transition: width 0.28s cubic-bezier(0.32, 0.72, 0, 1);
}

/* 侧栏 */
.admin-sidebar {
  overflow: hidden;
  background: var(--pf-bg-sidebar);
  border-right: 1px solid var(--pf-bg-sidebar-border);
  box-shadow: 2px 0 16px var(--pf-card-shadow);
}

.admin-sidebar__inner {
  height: 100%;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  height: 64px;
  border-bottom: 1px solid var(--pf-bg-sidebar-border);
  overflow: hidden;
}

.sidebar-logo__img {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 8px;
}

.sidebar-logo-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.sidebar-logo__name {
  font-size: 15px;
  font-weight: 700;
  color: var(--pf-primary);
  line-height: 1.3;
  white-space: nowrap;
  letter-spacing: 0.02em;
}

.sidebar-logo__sub {
  font-size: 12px;
  font-weight: 500;
  color: var(--pf-sidebar-text-muted);
  margin-top: 2px;
  white-space: nowrap;
}

.admin-sidebar--collapsed .sidebar-logo {
  justify-content: center;
  padding: 16px 8px;
}

/* 菜单 */
.admin-menu {
  background: transparent !important;
  border: none !important;
}

.menu-icon {
  font-size: 18px;
  flex-shrink: 0;
  color: var(--pf-sidebar-text-muted);
}

.menu-text {
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.admin-menu :deep(.el-menu-item),
.admin-menu :deep(.el-sub-menu__title) {
  position: relative;
  border-radius: 8px;
  margin: 2px 8px;
  color: var(--pf-sidebar-text) !important;
  font-weight: 500;
  transition: background-color 0.2s, color 0.2s;
}

.admin-menu :deep(.el-sub-menu__title) {
  font-weight: 600;
}

.admin-menu :deep(.el-menu-item:not(.is-active):hover),
.admin-menu :deep(.el-sub-menu__title:hover) {
  background: var(--pf-sidebar-hover-bg) !important;
  color: var(--pf-primary) !important;
}

.admin-menu :deep(.el-menu-item:not(.is-active):hover) .menu-icon,
.admin-menu :deep(.el-sub-menu__title:hover) .menu-icon {
  color: var(--pf-primary);
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: var(--pf-sidebar-active-bg) !important;
  color: var(--pf-sidebar-active-text) !important;
  font-weight: 600;
}

.admin-menu :deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 3px;
  height: 60%;
  border-radius: 0 3px 3px 0;
  background: var(--pf-primary);
  transform: translateY(-50%);
}

.admin-menu :deep(.el-menu-item.is-active) .menu-icon,
.admin-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) .menu-icon {
  color: var(--pf-primary);
}

.admin-menu :deep(.el-menu--inline) {
  background: transparent !important;
  margin: 0 8px 6px;
  padding: 2px 0 4px 4px;
  border-left: 1px solid var(--pf-bg-sidebar-border);
}

.admin-menu :deep(.el-menu--inline .el-menu-item) {
  margin: 1px 4px 1px 8px;
  height: 38px;
  line-height: 38px;
  padding-left: 12px !important;
  font-size: 14px;
  font-weight: 500;
  border-radius: 6px;
}

.admin-menu :deep(.el-menu--inline .el-menu-item .menu-icon--leaf) {
  font-size: 15px;
}

.admin-menu :deep(.el-menu--inline .el-menu-item.is-active) {
  background: var(--pf-sidebar-active-bg) !important;
}

.admin-menu :deep(.el-sub-menu__title .menu-icon--group) {
  color: var(--pf-sidebar-text);
}

.admin-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title .menu-icon--group),
.admin-menu :deep(.el-sub-menu__title:hover .menu-icon--group) {
  color: var(--pf-primary);
}

/* 底部用户 */
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid var(--pf-bg-sidebar-border);
  cursor: pointer;
  transition: background 0.2s;
}

.sidebar-user:hover {
  background: var(--pf-sidebar-hover-bg);
}

.admin-sidebar--collapsed .sidebar-user {
  flex-direction: column;
  gap: 8px;
  padding: 12px 8px;
}

.sidebar-user__avatar {
  background: linear-gradient(135deg, var(--pf-primary-hover), var(--pf-primary));
  color: #fff;
  flex-shrink: 0;
}

.sidebar-user__info {
  min-width: 0;
  flex: 1;
}

.sidebar-user__name {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--pf-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-user__role {
  margin: 2px 0 0;
  font-size: 12px;
  font-weight: 500;
  color: var(--pf-sidebar-text-muted);
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--pf-sidebar-text-muted);
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.logout-btn:hover {
  background: var(--pf-sidebar-hover-bg);
  color: var(--pf-primary);
}

/* 顶栏 */
.admin-topbar {
  background: var(--pf-topbar-bg);
  border-bottom: 1px solid var(--pf-topbar-border);
  box-shadow: 0 1px 8px var(--pf-card-shadow);
}

.topbar-title {
  color: var(--pf-topbar-text);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
  max-width: min(280px, 32vw);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-toggle {
  color: var(--pf-topbar-text) !important;
}

.topbar-search :deep(.el-input__wrapper) {
  background: var(--pf-input-bg, var(--pf-bg-page));
  box-shadow: 0 0 0 1px var(--pf-input-border, var(--pf-card-border)) inset;
}

.topbar-notify-btn,
.topbar-theme-btn {
  background: var(--pf-primary-muted) !important;
  border-color: var(--pf-card-border) !important;
  color: var(--pf-primary) !important;
}

.topbar-avatar-btn {
  display: inline-flex;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 50%;
}

.topbar-avatar {
  background: linear-gradient(135deg, var(--pf-primary-hover), var(--pf-primary));
  color: #fff;
}

.theme-swatch {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  margin-right: 8px;
  vertical-align: middle;
  border: 2px solid transparent;
}

.theme-swatch-active {
  font-weight: 600;
  color: var(--pf-primary);
}

.theme-check {
  margin-left: auto;
  color: var(--pf-primary);
}

/* 主内容 */
.admin-main {
  background: var(--pf-bg-page);
  padding: 24px 24px 80px;
  transition: background-color 0.25s ease;
}
</style>
