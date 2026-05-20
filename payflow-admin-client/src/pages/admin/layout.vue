<template>
  <el-container class="h-screen overflow-hidden">
    <!-- 侧边栏（森林底图 + 模糊，与登录页一致） -->
    <el-aside width="240px" class="admin-sidebar h-full flex flex-col relative">
      <div class="admin-sidebar__inner flex flex-col h-full min-h-0 relative z-[1]">
      <!-- Logo -->
      <div class="sidebar-logo border-b border-white/10 shrink-0">
        <img src="/ponyflux-logo.svg" alt="PonyFlux Pay Logo" />
        <div class="sidebar-logo-text">
          <span class="text-white font-bold leading-none">小马支付</span>
          <span class="text-white/50 text-xs ml-1.5">管理平台</span>
        </div>
      </div>

      <!-- 菜单：优先使用登录/Profile 返回的 sys_menus（按角色）；无数据时回退静态 -->
      <el-menu
        :key="sidebarMenuKey"
        :default-active="activeMenu"
        :default-openeds="menuDefaultOpeneds"
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
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.dashboard') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/insights/funnel">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.insightsFunnel') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/dashboard/churn-alerts">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.churnAlerts') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/notifications">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.notifications') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/search">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.globalSearch') }}</span>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="trade-group">
          <template #title>
            <el-icon class="menu-icon"><Document /></el-icon>
            <span class="menu-text">{{ t('menu.groupTrade') }}</span>
          </template>
          <el-menu-item index="/admin/orders">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.orders') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/refunds">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.refunds') }}</span>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="reconcile-group">
          <template #title>
            <el-icon class="menu-icon"><Memo /></el-icon>
            <span class="menu-text">{{ t('menu.groupReconcile') }}</span>
          </template>
          <el-menu-item index="/admin/reconcile/tasks">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.reconcileTasks') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/reconcile/results">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.reconcileResults') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/reconcile/summary">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.reconcileSummary') }}</span>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="channel-account-group">
          <template #title>
            <el-icon class="menu-icon"><CreditCard /></el-icon>
            <span class="menu-text">{{ t('menu.groupChannelAccount') }}</span>
          </template>
          <el-menu-item index="/admin/channels">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.channels') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/payment-methods">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.paymentMethods') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/payment-accounts">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.paymentAccounts') }}</span>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="routing-group">
          <template #title>
            <el-icon class="menu-icon"><Connection /></el-icon>
            <span class="menu-text">{{ t('menu.groupRouting') }}</span>
          </template>
          <el-menu-item index="/admin/channel-routing/health">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.channelRoutingHealth') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/routing/logs">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.routingLogs') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/fee-rate/config">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.feeRateConfig') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/fee-rate/audit-log">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.feeRateAuditLog') }}</span>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="merchant-risk-group">
          <template #title>
            <el-icon class="menu-icon"><Shop /></el-icon>
            <span class="menu-text">{{ t('menu.groupMerchantRisk') }}</span>
          </template>
          <el-menu-item index="/admin/merchants">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.merchants') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/onboarding">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.onboarding') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/risk">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.risk') }}</span>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="system-group">
          <template #title>
            <el-icon class="menu-icon"><Setting /></el-icon>
            <span class="menu-text">{{ t('menu.groupSystem') }}</span>
          </template>
          <el-menu-item index="/admin/settings">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.settings') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.users') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/roles">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.roles') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/menus">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.menus') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/dicts">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.dicts') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/audit-logs">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.auditLogs') }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/security-audit">
            <span class="menu-leaf-dot" />
            <span class="menu-text">{{ t('menu.securityAudit') }}</span>
          </el-menu-item>
        </el-sub-menu>
        </template>
      </el-menu>

      <!-- 底部用户信息 -->
      <div class="px-4 py-3 border-t border-white/10 shrink-0">
        <div class="flex items-center gap-2">
          <el-avatar :size="28" class="bg-gradient-to-br from-[#064e3b] to-[#0d9488] text-white text-xs">A</el-avatar>
          <div class="min-w-0 flex-1">
            <p class="text-xs text-white/80 font-medium truncate">{{ adminName }}</p>
            <p class="text-xs text-white/50 truncate">管理员</p>
          </div>
          <el-tooltip content="退出登录" placement="top">
            <button class="logout-btn" @click="handleLogout" title="退出登录">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </el-tooltip>
        </div>
      </div>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="flex-col flex-1 min-h-0 overflow-hidden">
      <!-- 顶部工具栏（与侧栏同一套森林模糊底） -->
      <div class="admin-topbar h-[60px] shrink-0 topbar relative border-b border-white/10">
        <div class="admin-topbar__inner relative z-[1] flex h-full w-full items-center px-6">
        <h1 class="topbar-title text-white/95 font-semibold text-base m-0 tracking-tight">{{ pageTitle }}</h1>
        <el-input
          v-model="topSearchQ"
          clearable
          size="default"
          placeholder="搜索订单号 / 商户订单号…"
          class="topbar-search ml-4 w-[min(280px,28vw)]"
          @keyup.enter="goGlobalSearch"
        />
        <div class="ml-auto flex items-center gap-3 topbar-actions">
          <el-badge :value="0" class="cursor-pointer" :hidden="true">
            <el-button circle class="!bg-white/10 !border-white/15 !text-slate-100">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M13.73 21a2 2 0 01-3.46 0" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </el-button>
          </el-badge>
          <el-avatar :size="32" class="bg-gradient-to-br from-[#064e3b] to-[#0d9488] text-white cursor-pointer ring-1 ring-white/20">{{ adminName?.charAt(0) }}</el-avatar>
        </div>
        </div>
      </div>

      <!-- 页面内容 -->
      <el-main class="admin-main flex-1 min-h-0 overflow-y-auto scrollbar-light">
        <router-view v-slot="{ Component }">
          <transition name="page-zoom" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
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
  Connection,
  Shop,
  Setting,
} from '@element-plus/icons-vue'
import { useAdminStore } from '@/stores/admin'
import { getAdminProfile } from '@/api/auth'
import AdminSidebarMenu from '@/components/AdminSidebarMenu.vue'
import type { SysMenu } from '@/types'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

const activeMenu = computed(() => route.path)
const topSearchQ = ref('')

function filterMenusForSidebar(menus: SysMenu[] | undefined): SysMenu[] {
  if (!menus?.length) return []
  const out: SysMenu[] = []
  for (const m of menus) {
    if (m.status === 'DISABLED') continue
    if (m.visible === false) continue
    const children = m.children?.length ? filterMenusForSidebar(m.children) : undefined
    if ((children && children.length > 0) || (m.path && m.path.length > 0)) {
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

/** 切换顶层分组时通过 :key 重挂载侧栏，使 default-openeds 与当前模块一致 */
const sidebarMenuKey = computed(() => {
  if (useDynamicMenu.value) {
    const openKeys = collectDynamicOpenKeys(dynamicMenuRoots.value, route.path) ?? []
    return 'dyn-' + (openKeys[0] ?? 'root')
  }
  return menuDefaultOpeneds.value[0] ?? 'workspace-group'
})

const pageTitle = computed(() => {
  return (route.meta?.title as string) ?? '控制台'
})

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
    /* 静默：Token 失效时由拦截器处理 */
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
    // cancelled
  }
}
</script>

<style scoped>
.el-aside {
  transition: width 0.3s;
}

/* 侧栏：草坪底图 + 轻模糊 + 半透明罩层（能辨认草地/树影，文字仍可读） */
.admin-sidebar {
  overflow: hidden;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 4px 0 36px rgba(0, 0, 0, 0.22);
  isolation: isolate;
  background-color: #0d2822;
}

.admin-sidebar::before {
  content: '';
  position: absolute;
  inset: -12px;
  z-index: 0;
  background-image: url('/forest-hero.png');
  background-repeat: no-repeat;
  background-size: cover;
  background-position: left center;
  background-attachment: scroll;
  filter: blur(9px) saturate(1.05);
  -webkit-filter: blur(9px) saturate(1.05);
  transform: translateZ(0);
}

.admin-sidebar::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: linear-gradient(
    180deg,
    rgba(2, 36, 32, 0.52) 0%,
    rgba(3, 48, 42, 0.48) 45%,
    rgba(2, 28, 26, 0.55) 100%
  );
}

/* 顶栏：同一张草坪图，与侧栏一致略偏左对齐，便于看成连续场景 */
.admin-topbar {
  overflow: hidden;
  isolation: isolate;
  background-color: #0d2822;
}

.admin-topbar::before {
  content: '';
  position: absolute;
  inset: -12px;
  z-index: 0;
  background-image: url('/forest-hero.png');
  background-repeat: no-repeat;
  background-size: cover;
  background-position: left top;
  background-attachment: scroll;
  filter: blur(9px) saturate(1.05);
  -webkit-filter: blur(9px) saturate(1.05);
  transform: translateZ(0);
}

.admin-topbar::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: linear-gradient(
    90deg,
    rgba(2, 36, 32, 0.5) 0%,
    rgba(3, 44, 40, 0.46) 40%,
    rgba(2, 30, 28, 0.52) 100%
  );
}

/* Logo 区域 */
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  height: 76px;
}

.sidebar-logo img {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  filter: drop-shadow(0 0 12px rgba(13, 148, 136, 0.35));
}

.sidebar-logo-text {
  display: flex;
  align-items: baseline;
  gap: 0;
  font-size: 16px;
  font-weight: 700;
}

/* ========== 菜单样式统一 ========== */
.admin-menu {
  background: transparent !important;
  border: none !important;
}

/* 菜单图标 */
.menu-icon {
  font-size: 18px;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  color: #94a3b8;
}

.admin-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title .menu-icon),
.admin-menu :deep(.el-sub-menu__title:hover .menu-icon) {
  color: #5eead4;
}

/* 叶子菜单小圆点 */
.menu-leaf-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(148, 163, 184, 0.6);
  flex-shrink: 0;
  margin-right: 2px;
}

.admin-menu :deep(.el-menu-item.is-active .menu-leaf-dot) {
  background: #14b8a6;
  box-shadow: 0 0 6px rgba(20, 184, 166, 0.6);
}

/* 菜单文字 */
.menu-text {
  font-size: 14px;
  font-weight: 500;
}

.topbar-title {
  max-width: min(280px, 32vw);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 一级菜单项：胶囊指示条 + 渐变填充 */
.admin-menu :deep(.el-menu-item) {
  position: relative;
  border-radius: 10px;
  margin: 4px 12px;
  height: 44px;
  line-height: 44px;
  color: #94A3B8 !important;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px !important;
  transition: background-color .28s cubic-bezier(.32,.72,0,1),
              color .22s ease,
              transform .28s cubic-bezier(.32,.72,0,1);
  overflow: hidden;
}

/* 胶囊指示条（默认隐藏） */
.admin-menu :deep(.el-menu-item)::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 50%;
  width: 3px;
  height: 60%;
  border-radius: 4px;
  background: #5eead4;
  transform: translateY(-50%) scaleY(0);
  opacity: 0;
  transition: transform .28s cubic-bezier(.34,1.28,.64,1),
              opacity .22s ease;
  box-shadow: 0 0 10px rgba(94, 234, 212, .6);
  pointer-events: none;
}

.admin-menu :deep(.el-menu-item:not(.is-active):hover) {
  background: rgba(13, 148, 136, 0.12) !important;
  color: #ccfbf1 !important;
  transform: translateX(2px);
}

.admin-menu :deep(.el-menu-item:not(.is-active):hover)::before {
  transform: translateY(-50%) scaleY(.4);
  opacity: .35;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(13, 148, 136, 0.42) 0%, rgba(4, 120, 87, 0.15) 100%) !important;
  color: #ffffff !important;
  font-weight: 600;
}

.admin-menu :deep(.el-menu-item.is-active)::before {
  transform: translateY(-50%) scaleY(1);
  opacity: 1;
}

/* ========== 子菜单样式 ========== */
.admin-menu :deep(.el-sub-menu__title) {
  position: relative;
  border-radius: 10px;
  margin: 4px 12px;
  height: 44px !important;
  line-height: 44px !important;
  color: #94A3B8 !important;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px !important;
  transition: background-color .28s cubic-bezier(.32,.72,0,1),
              color .22s ease,
              transform .28s cubic-bezier(.32,.72,0,1);
  overflow: hidden;
}

.admin-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(13, 148, 136, 0.12) !important;
  color: #ccfbf1 !important;
  transform: translateX(2px);
}

/* 子菜单展开后的容器 */
.admin-menu :deep(.el-menu--inline) {
  position: relative;
  background: rgba(2, 22, 20, 0.32) !important;
  border-radius: 0 0 8px 8px;
  margin: 0 12px 6px;
  padding: 4px 0 6px 8px;
  border-left: 2px solid transparent;
  border-image: linear-gradient(180deg, #0d9488 0%, rgba(13, 148, 136, 0.15) 70%, transparent 100%) 1;
}

/* 子菜单项：更纤细的胶囊指示条 */
.admin-menu :deep(.el-menu--inline .el-menu-item) {
  margin: 2px 8px;
  height: 38px;
  line-height: 38px;
  padding-left: 20px !important;
  font-size: 13px;
  gap: 8px;
}

.admin-menu :deep(.el-menu--inline .el-menu-item)::before {
  width: 2px;
  height: 50%;
  left: 2px;
}

.admin-menu :deep(.el-menu--inline .el-menu-item:not(.is-active):hover) {
  background: rgba(4, 120, 87, 0.2) !important;
}

.admin-menu :deep(.el-menu--inline .el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(13, 148, 136, 0.42) 0%, rgba(4, 120, 87, 0.12) 100%) !important;
}

/* ========== 主内容区 ========== */
.admin-main {
  background: rgba(236, 253, 245, 0.72);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  padding: 24px 24px 80px;
  border-left: 1px solid rgba(6, 78, 59, 0.12);
}

.topbar {
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.18);
}

.topbar-search :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.12);
  box-shadow: none;
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.topbar-search :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.45);
}

.topbar-actions :deep(.el-button.is-circle) {
  --el-button-hover-bg-color: rgba(255, 255, 255, 0.14);
  --el-button-hover-border-color: rgba(255, 255, 255, 0.22);
}

/* 退出按钮 */
.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #94A3B8;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: rgba(4, 120, 87, 0.35);
  color: #99f6e4;
}
</style>
