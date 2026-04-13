<template>
  <el-container class="h-screen overflow-hidden">
    <!-- 侧边栏 -->
    <el-aside
      width="240px"
      class="admin-sidebar flex flex-col h-full"
    >
      <!-- Logo -->
      <div class="sidebar-logo border-b border-white/10 shrink-0">
        <img src="/ponyflux-logo.svg" alt="PonyFlux Pay Logo" />
        <div class="sidebar-logo-text">
          <span class="text-white font-bold leading-none">小马支付</span>
          <span class="text-white/50 text-xs ml-1.5">管理平台</span>
        </div>
      </div>

      <!-- 菜单 -->
      <el-menu
        :default-active="activeMenu"
        class="flex-1 overflow-y-auto border-none admin-menu"
        router
      >
        <el-menu-item index="/admin/dashboard">
          <span class="mr-2">📊</span> 数据概览
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <span class="mr-2">📋</span> 订单管理
        </el-menu-item>
        <el-menu-item index="/admin/refunds">
          <span class="mr-2">💰</span> 退款管理
        </el-menu-item>
        <el-menu-item index="/admin/channels">
          <span class="mr-2">🏦</span> 渠道管理
        </el-menu-item>
        <el-menu-item index="/admin/risk">
          <span class="mr-2">⚠️</span> 风控配置
        </el-menu-item>
        <el-menu-item index="/admin/merchants">
          <span class="mr-2">👥</span> 商户管理
        </el-menu-item>
        <el-menu-item index="/admin/settings">
          <span class="mr-2">⚙️</span> 系统设置
        </el-menu-item>
      </el-menu>

      <!-- 底部用户信息 -->
      <div class="px-4 py-3 border-t border-white/10 shrink-0">
        <div class="flex items-center gap-2">
          <el-avatar :size="28" class="bg-gradient-to-br from-[#6366F1] to-[#818CF8] text-white text-xs">A</el-avatar>
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
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="flex-col">
      <!-- 顶部工具栏 -->
      <div class="h-[60px] bg-white/90 backdrop-blur-xl border-b border-[#6366F1]/10 flex items-center px-6 shrink-0 topbar">
        <h1 class="text-[#0F172A] font-semibold text-base m-0">{{ pageTitle }}</h1>
        <div class="ml-auto flex items-center gap-3">
          <el-badge :value="0" class="cursor-pointer" :hidden="true">
            <el-button circle>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M13.73 21a2 2 0 01-3.46 0" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </el-button>
          </el-badge>
          <el-avatar :size="32" class="bg-gradient-to-br from-[#6366F1] to-[#818CF8] text-white cursor-pointer">{{ adminName?.charAt(0) }}</el-avatar>
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

/* 侧边栏渐变背景 */
.admin-sidebar {
  background: linear-gradient(180deg, #0F172A 0%, #1E293B 70%, #1E3A5F 100%);
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.15);
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
  filter: drop-shadow(0 0 10px rgba(165, 180, 252, 0.5));
}

.sidebar-logo-text {
  display: flex;
  align-items: baseline;
  gap: 0;
  font-size: 16px;
  font-weight: 700;
}

/* 菜单样式重写 */
.admin-menu {
  background: transparent !important;
  border: none !important;
}

.admin-menu :deep(.el-menu-item) {
  border-radius: 8px;
  margin: 4px 12px;
  height: 44px;
  line-height: 44px;
  color: #94A3B8 !important;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px !important;
  transition: all 0.2s;
}

.admin-menu :deep(.el-menu-item:hover) {
  background: rgba(99, 102, 241, 0.12) !important;
  color: #E0E7FF !important;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: rgba(99, 102, 241, 0.18) !important;
  color: #FFFFFF !important;
  border-left: 3px solid #6366F1;
  font-weight: 600;
}

.admin-menu :deep(.el-menu-item span) {
  font-size: 14px;
}

/* 主内容区背景 */
.admin-main {
  background: linear-gradient(135deg, #F8FAFC 0%, #EEF2FF 50%, #F8FAFC 100%);
  min-height: 100vh;
  padding: 24px;
}

/* 顶部导航栏 */
.topbar {
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.04);
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
  background: rgba(99, 102, 241, 0.2);
  color: #818CF8;
}
</style>
