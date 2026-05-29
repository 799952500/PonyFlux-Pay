<template>
  <div class="page-table-shell profile-page">
    <div class="content-card profile-card">
      <div class="profile-header">
        <el-avatar :size="56" class="profile-header__avatar">{{ displayName.charAt(0) }}</el-avatar>
        <div>
          <h2 class="profile-header__title">个人中心</h2>
          <p class="profile-header__sub">管理账号信息与界面外观，设置将保存到云端</p>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="profile-tabs">
        <el-tab-pane label="账号信息" name="account">
          <el-descriptions :column="1" border size="default" class="profile-descriptions">
            <el-descriptions-item label="用户名">{{ user?.username || '—' }}</el-descriptions-item>
            <el-descriptions-item label="显示名称">{{ user?.nickname || user?.username || '—' }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ roleLabel }}</el-descriptions-item>
            <el-descriptions-item label="数据范围">{{ scopeLabel }}</el-descriptions-item>
            <el-descriptions-item v-if="merchantScopeText" label="授权商户">{{ merchantScopeText }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="外观与显示" name="appearance">
          <AppearanceSettings />
        </el-tab-pane>
        <el-tab-pane label="对账报告订阅" name="recon-reports">
          <ReconReportSubscriptions />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import AppearanceSettings from '@/components/admin/AppearanceSettings.vue'
import ReconReportSubscriptions from '@/components/admin/ReconReportSubscriptions.vue'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()
const user = computed(() => adminStore.user)

const activeTab = ref(
  route.query.tab === 'appearance'
    ? 'appearance'
    : route.query.tab === 'recon-reports'
      ? 'recon-reports'
      : 'account'
)

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'appearance') activeTab.value = 'appearance'
    else if (tab === 'recon-reports') activeTab.value = 'recon-reports'
    else if (tab === 'account') activeTab.value = 'account'
  },
)

watch(activeTab, (tab) => {
  const q: Record<string, string> = {}
  if (tab === 'appearance') q.tab = 'appearance'
  if (tab === 'recon-reports') q.tab = 'recon-reports'
  if (route.query.tab !== q.tab) {
    router.replace({ path: '/admin/profile', query: q })
  }
})

const displayName = computed(() => user.value?.nickname || user.value?.username || '管理员')

const ROLE_LABELS: Record<string, string> = {
  SUPER_ADMIN: '超级管理员',
  ADMIN: '管理员',
  FINANCE: '财务',
  RISK: '风控',
}

const roleLabel = computed(() => ROLE_LABELS[user.value?.role ?? ''] ?? user.value?.role ?? '—')

const scopeLabel = computed(() => {
  if (user.value?.platformAdmin) return '平台全局'
  if (user.value?.scopeMode === 'MERCHANT') return '指定商户'
  return '—'
})

const merchantScopeText = computed(() => {
  const ids = user.value?.authorizedMerchantIds?.filter(Boolean) ?? []
  if (!ids.length) return ''
  return ids.join('、')
})
</script>

<style scoped>
.profile-card {
  padding: 24px 28px;
  max-width: 800px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.profile-header__avatar {
  background: var(--pf-primary);
  color: #fff;
  font-weight: 600;
  font-size: 22px;
}

.profile-header__title {
  font-size: 18px;
  font-weight: 600;
  color: var(--pf-text-primary);
  margin: 0 0 4px;
}

.profile-header__sub {
  font-size: 14px;
  color: var(--pf-text-secondary);
  margin: 0;
}

.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.profile-descriptions {
  max-width: 560px;
}
</style>
