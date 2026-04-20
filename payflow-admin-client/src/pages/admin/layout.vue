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

      <!-- 菜单 -->
      <el-menu :default-active="activeMenu" class="flex-1 overflow-y-auto border-none admin-menu" router>
        <!-- 数据概览 -->
        <el-menu-item index="/admin/dashboard">
          <span class="menu-icon">📊</span>
          <span class="menu-text">数据概览</span>
        </el-menu-item>

        <!-- 订单管理 -->
        <el-sub-menu index="order-group">
          <template #title>
            <span class="menu-icon">📋</span>
            <span class="menu-text">订单管理</span>
          </template>
          <el-menu-item index="/admin/orders">
            <span class="menu-text">订单列表</span>
          </el-menu-item>
          <el-menu-item index="/admin/refunds">
            <span class="menu-text">退款管理</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 支付管理 -->
        <el-sub-menu index="payment-group">
          <template #title>
            <span class="menu-icon">💳</span>
            <span class="menu-text">支付管理</span>
          </template>
          <el-menu-item index="/admin/channels">
            <span class="menu-text">渠道管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/payment-methods">
            <span class="menu-text">支付方式</span>
          </el-menu-item>
          <el-menu-item index="/admin/merchant-payments">
            <span class="menu-text">商户支付方式</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 商户管理 -->
        <el-menu-item index="/admin/merchants">
          <span class="menu-icon">👥</span>
          <span class="menu-text">商户管理</span>
        </el-menu-item>

        <!-- 风控管理 -->
        <el-menu-item index="/admin/risk">
          <span class="menu-icon">⚠️</span>
          <span class="menu-text">风控管理</span>
        </el-menu-item>

        <!-- 系统设置 -->
        <el-menu-item index="/admin/settings">
          <span class="menu-icon">⚙️</span>
          <span class="menu-text">系统设置</span>
        </el-menu-item>
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
    <el-container class="flex-col">
      <!-- 顶部工具栏（与侧栏同一套森林模糊底） -->
      <div class="admin-topbar h-[60px] shrink-0 topbar relative border-b border-white/10">
        <div class="admin-topbar__inner relative z-[1] flex h-full w-full items-center px-6">
        <h1 class="text-white/95 font-semibold text-base m-0 tracking-tight">{{ pageTitle }}</h1>
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
      <el-main class="admin-main overflow-y-auto">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => {
  return (route.meta?.title as string) ?? '控制台'
})

const adminName = computed(() => {
  const user = adminStore.user
  return user?.username ?? localStorage.getItem('adminUser') ? JSON.parse(localStorage.getItem('adminUser')!).username : '管理员'
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
  font-size: 16px;
  width: 24px;
  text-align: center;
  flex-shrink: 0;
}

/* 菜单文字 */
.menu-text {
  font-size: 14px;
  font-weight: 500;
}

/* 一级菜单项 */
.admin-menu :deep(.el-menu-item) {
  border-radius: 8px;
  margin: 4px 12px;
  height: 44px;
  line-height: 44px;
  color: #94A3B8 !important;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px !important;
  transition: all 0.2s;
}

.admin-menu :deep(.el-menu-item:hover) {
  background: rgba(13, 148, 136, 0.12) !important;
  color: #ccfbf1 !important;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: rgba(4, 120, 87, 0.28) !important;
  color: #ffffff !important;
  border-left: 3px solid #0d9488;
  font-weight: 600;
}

/* ========== 子菜单样式 ========== */
.admin-menu :deep(.el-sub-menu__title) {
  border-radius: 8px;
  margin: 4px 12px;
  height: 44px !important;
  line-height: 44px !important;
  color: #94A3B8 !important;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px !important;
  transition: all 0.2s;
}

.admin-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(13, 148, 136, 0.12) !important;
  color: #ccfbf1 !important;
}

/* 子菜单展开后的容器 */
.admin-menu :deep(.el-menu--inline) {
  background: rgba(2, 22, 20, 0.32) !important;
  border-radius: 0 0 8px 8px;
  margin: 0 12px;
  padding: 4px 0;
}

/* 子菜单项 */
.admin-menu :deep(.el-menu--inline .el-menu-item) {
  margin: 2px 8px;
  height: 38px;
  line-height: 38px;
  padding-left: 44px !important;
  font-size: 13px;
}

.admin-menu :deep(.el-menu--inline .el-menu-item:hover) {
  background: rgba(4, 120, 87, 0.2) !important;
}

.admin-menu :deep(.el-menu--inline .el-menu-item.is-active) {
  background: rgba(4, 120, 87, 0.3) !important;
  border-left: 3px solid #14b8a6;
}

/* ========== 主内容区 ========== */
.admin-main {
  background: rgba(236, 253, 245, 0.72);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  min-height: 100vh;
  padding: 24px;
  border-left: 1px solid rgba(6, 78, 59, 0.12);
}

.topbar {
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.18);
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
