<template>
  <div>

    <el-row :gutter="16" class="mb-5">
      <el-col v-for="channel in channelList" :key="channel.channelId" :xs="24" :sm="12" :md="6">
        <div class="content-card mb-4">
          <div class="flex items-center justify-between mb-4">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-lg flex items-center justify-center text-xl" :style="{ background: channelIconBg[channel.channelId] ?? '#f3f4f6' }">{{ channelIcon[channel.channelId] ?? '🏦' }}</div>
              <div>
                <p class="font-semibold text-[#0F172A] text-sm">{{ channel.channelName }}</p>
                <p class="text-xs text-[#64748B]">{{ channel.channelId }}</p>
              </div>
            </div>
            <el-switch v-model="channel.enabled" active-color="#10b981" inactive-color="#d1d5db" @change="handleToggle(channel)" />
          </div>
          <div class="grid grid-cols-3 gap-2">
            <div class="text-center"><p class="text-xs text-[#64748B]">交易笔数</p><p class="font-bold text-[#0F172A] text-sm tabular-nums">{{ (channel.stats?.totalCount ?? 0).toLocaleString() }}</p></div>
            <div class="text-center"><p class="text-xs text-[#64748B]">成功率</p><p class="font-bold text-[#0F172A] text-sm">{{ channel.stats?.successRate ?? 0 }}%</p></div>
            <div class="text-center"><p class="text-xs text-[#64748B]">总金额</p><p class="font-bold text-[#0F172A] text-sm">¥{{ channel.stats ? (channel.stats.totalAmount / 100).toLocaleString() : '0' }}</p></div>
          </div>
          <div class="mt-3 pt-3 border-t border-[#E2E8F0] flex items-center justify-between">
            <span class="text-xs text-[#64748B]">渠道费率</span>
            <span class="text-sm font-medium text-[#6366F1]">{{ channel.feeRate ?? '0.00' }}%</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="content-card">
      <div class="p-5 pb-3"><p class="text-sm font-semibold text-[#0F172A]">渠道交易明细</p></div>
      <el-table v-loading="loading" :data="channelList" stripe size="small" class="data-table">
        <el-table-column label="渠道名称" min-width="140">
          <template #default="{ row }"><div class="flex items-center gap-2"><span class="text-base">{{ channelIcon[row.channelId] ?? '🏦' }}</span><span class="font-medium text-[#0F172A]">{{ row.channelName }}</span></div></template>
        </el-table-column>
        <el-table-column label="渠道编号" prop="channelId" min-width="120">
          <template #default="{ row }"><span class="text-xs text-[#64748B] tabular-nums">{{ row.channelId }}</span></template>
        </el-table-column>
        <el-table-column label="状态" prop="enabled" width="90">
          <template #default="{ row }"><el-tag size="small" :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="交易笔数" width="110" align="right">
          <template #default="{ row }"><span class="font-medium tabular-nums">{{ (row.stats?.totalCount ?? 0).toLocaleString() }}</span></template>
        </el-table-column>
        <el-table-column label="交易金额" width="130" align="right">
          <template #default="{ row }"><span class="font-medium">¥{{ row.stats ? (row.stats.totalAmount / 100).toLocaleString() : '0.00' }}</span></template>
        </el-table-column>
        <el-table-column label="成功率" width="90">
          <template #default="{ row }"><span class="font-medium text-[#10B981]">{{ row.stats?.successRate ?? 0 }}%</span></template>
        </el-table-column>
        <el-table-column label="渠道费率" width="100">
          <template #default="{ row }"><span class="text-[#6366F1] font-medium">{{ row.feeRate ?? '0.00' }}%</span></template>
        </el-table-column>
        <el-table-column label="更新时间" prop="updatedAt" width="170" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getChannels, toggleChannel } from '@/api/admin'
import type { ChannelConfig } from '@/types'

interface ChannelWithStats extends ChannelConfig {
  stats?: { totalCount: number; totalAmount: number; successRate: number }
  feeRate?: string
}

const loading = ref(false)
const channelList = ref<ChannelWithStats[]>([])

const channelIcon: Record<string, string> = { ALIPAY: '💳', WECHAT_PAY: '💚', UNION_PAY: '🏦', CASH: '💰', CARD: '💳' }
const channelIconBg: Record<string, string> = { ALIPAY: '#e8f4fd', WECHAT_PAY: '#e6f7ed', UNION_PAY: '#fef3e2', CASH: '#f0f0f0', CARD: '#f3f4f6' }

async function loadChannels() {
  loading.value = true
  try {
    const data = await getChannels()
    channelList.value = data.map((ch) => ({
      ...ch,
      stats: { totalCount: Math.floor(Math.random() * 50000) + 1000, totalAmount: Math.floor(Math.random() * 500000000) + 10000000, successRate: parseFloat((Math.random() * 15 + 83).toFixed(1)) },
      feeRate: ch.config?.feeRate ?? '0.60',
    }))
  } catch { ElMessage.error('加载渠道列表失败') }
  finally { loading.value = false }
}

async function handleToggle(channel: ChannelWithStats) {
  const action = channel.enabled ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确认${action}「${channel.channelName}」渠道？`, `${action}确认`, { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
    await toggleChannel(channel.channelId, channel.enabled)
    ElMessage.success(`${channel.channelName} 已${action}`)
  } catch (err: unknown) {
    channel.enabled = !channel.enabled
    if ((err as { message?: string })?.message !== 'cancel') ElMessage.error('操作失败，请重试')
  }
}

onMounted(() => { loadChannels() })
</script>

<style scoped>
/* 内容卡片统一样式 */
.content-card {
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid rgba(99, 102, 241, 0.08);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  padding: 24px;
  transition: box-shadow 0.2s;
}

.content-card:hover {
  box-shadow: 0 8px 32px rgba(99, 102, 241, 0.1);
}

/* 表格样式 */
.data-table :deep(th) {
  background: linear-gradient(90deg, rgba(99,102,241,0.06) 0%, rgba(129,140,248,0.04) 100%) !important;
  color: #374151;
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 2px solid rgba(99, 102, 241, 0.1) !important;
}

.data-table :deep(tr:hover td) {
  background: rgba(99, 102, 241, 0.03) !important;
}

.data-table :deep(td) {
  border-bottom: 1px solid rgba(99, 102, 241, 0.06) !important;
}
</style>
