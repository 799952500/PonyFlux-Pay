<template>
  <div>
    <el-row :gutter="16" class="mb-5">
      <el-col v-for="kpi in kpiCards" :key="kpi.label" :xs="12" :sm="12" :md="6">
        <div class="content-card">
          <div class="flex items-center justify-between mb-3">
            <span class="text-[#64748B] text-xs font-medium">{{ kpi.label }}</span>
            <span class="text-lg">{{ kpi.icon }}</span>
          </div>
          <p class="text-2xl font-bold text-[#0F172A] tabular-nums mb-1">
            {{ kpi.value }}
          </p>
          <p v-if="kpi.sub" class="text-xs text-[#64748B]">
            {{ kpi.sub }} {{ kpi.subVal }}
            <span
              v-if="kpi.trend !== undefined && kpi.trend !== null"
              class="ml-1 font-medium"
              :class="Number(kpi.trend) > 0 ? 'text-[#047857]' : 'text-[#EF4444]'"
            >
              {{ Number(kpi.trend) >= 0 ? '↑' : '↓' }}{{ Math.abs(Number(kpi.trend)) }}%
            </span>
            <span
              v-if="kpi.yoyTrend !== undefined && kpi.yoyTrend !== null"
              class="ml-1 font-medium text-xs"
              :class="Number(kpi.yoyTrend) > 0 ? 'text-[#047857]' : 'text-[#EF4444]'"
            >
              同比{{ Number(kpi.yoyTrend) >= 0 ? '↑' : '↓' }}{{ Math.abs(Number(kpi.yoyTrend)) }}%
            </span>
          </p>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mb-5">
      <el-col :xs="24" :md="16">
        <div class="content-card">
          <div class="flex items-center justify-between mb-4">
            <p class="text-[#0F172A] font-semibold text-sm">交易趋势</p>
            <el-radio-group v-model="trendRange" size="small">
              <el-radio-button value="7d">近7天</el-radio-button>
              <el-radio-button value="30d">近30天</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="trendChartRef" class="w-full h-[260px]" />
        </div>
      </el-col>

      <el-col :xs="24" :md="8">
        <div class="content-card h-full">
          <p class="text-[#0F172A] font-semibold text-sm mb-4">渠道占比</p>
          <div ref="pieChartRef" class="w-full h-[260px]" />
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mb-5">
      <el-col :xs="24">
        <MerchantRanking />
      </el-col>
    </el-row>

    <!-- 流失预警区块 -->
    <div v-if="churnAlerts.length > 0" class="content-card mb-5" style="border-color: rgba(239,68,68,0.3)">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center gap-2">
          <span class="inline-block w-2 h-2 rounded-full bg-[#EF4444] animate-pulse" />
          <p class="text-[#0F172A] font-semibold text-sm">流失预警</p>
          <el-tag size="small" type="danger">{{ churnAlerts.length }}条待处理</el-tag>
        </div>
        <el-button link type="primary" size="small" @click="$router.push('/admin/dashboard/churn-alerts')">
          查看全部 →
        </el-button>
      </div>
      <el-table :data="churnAlerts.slice(0, 5)" size="small" class="data-table">
        <el-table-column label="商户ID" prop="merchantId" width="100" />
        <el-table-column label="预警等级" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.alertLevel === 'red' ? 'danger' : 'warning'">
              {{ row.alertLevel === 'red' ? '红色' : row.alertLevel === 'orange' ? '橙色' : '黄色' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下降幅度" width="100">
          <template #default="{ row }">
            <span class="font-medium text-[#EF4444]">↓{{ row.declinePct }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="连续天数" prop="consecutiveDays" width="90" />
        <el-table-column label="创建时间" prop="createTime" width="170" />
      </el-table>
    </div>

    <div class="content-card">
      <div class="flex items-center justify-between p-5 pb-3">
        <p class="text-[#0F172A] font-semibold text-sm">最新交易</p>
        <el-button link type="primary" size="small" @click="$router.push('/admin/orders')">
          查看全部 →
        </el-button>
      </div>
      <el-table :data="recentOrders" size="small" v-loading="loading" class="data-table">
        <el-table-column label="订单号" prop="orderId" min-width="160">
          <template #default="{ row }">
            <span class="text-xs tabular-nums">#{{ row.orderId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商户订单号" prop="merchantOrderNo" min-width="140" />
        <el-table-column label="金额" prop="amount" width="110">
          <template #default="{ row }">
            <span class="font-medium">¥{{ ((Number(row.amount) || 0) / 100).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="渠道" prop="channel" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.channel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="orderStatusTagType(row.status)">
              {{ orderStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="170" />
      </el-table>
      <p v-if="!loading && recentOrders.length === 0" class="px-5 pb-5 text-sm text-[#64748B]">暂无订单数据</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { Order, OrderStatus } from '@/types'
import type { ChannelDistItem, TrendDataItem } from '@/types'
import { getDashboardStats, getChurnAlerts } from '@/api/admin'
import MerchantRanking from '@/components/dashboard/MerchantRanking.vue'

echarts.use([LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, CanvasRenderer])

const loading = ref(false)
const trendRange = ref<'7d' | '30d'>('7d')
const trendChartRef = ref<HTMLDivElement | null>(null)
const pieChartRef = ref<HTMLDivElement | null>(null)

let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const kpiCards = reactive([
  { label: '今日收入', value: '—', sub: '昨日', subVal: '—', trend: 0 as number | undefined, yoyTrend: 0 as number | undefined, icon: '💰' },
  { label: '今日订单', value: '—', sub: '昨日', subVal: '—', trend: 0 as number | undefined, icon: '📋' },
  { label: '今日已付', value: '—', sub: '—', trend: undefined as number | undefined, icon: '✅' },
  { label: '转化率', value: '—', sub: '按今日订单', trend: undefined as number | undefined, icon: '📈' },
])

const recentOrders = ref<Partial<Order>[]>([])
const churnAlerts = ref<any[]>([])

const statusTypeMap: Record<string, string> = {
  PAID: 'success',
  SUCCESS: 'success',
  PAYING: 'warning',
  CREATED: 'info',
  EXPIRED: 'info',
  FAILED: 'danger',
}

const statusLabelMap: Record<string, string> = {
  PAID: '已支付',
  SUCCESS: '成功',
  PAYING: '支付中',
  CREATED: '待支付',
  EXPIRED: '已过期',
  FAILED: '失败',
}

function orderStatusTagType(status: unknown): string {
  const s = String(status ?? '')
  return statusTypeMap[s] ?? 'info'
}

function orderStatusLabel(status: unknown): string {
  const s = String(status ?? '')
  return statusLabelMap[s] ?? s
}

function pctChange(curr: number, prev: number): number {
  if (prev === 0) {
    return curr === 0 ? 0 : 100
  }
  return Math.round(((curr - prev) / prev) * 1000) / 10
}

function fmtMoneyYuan(n: number): string {
  return `¥${Number(n || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatTrendDate(d: string): string {
  if (!d) return ''
  const parts = d.split('-')
  if (parts.length >= 3) {
    return `${parts[1]}-${parts[2]}`
  }
  return d.slice(5)
}

function ensureTrendChart() {
  if (!trendChartRef.value) return null
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  return trendChart
}

function ensurePieChart() {
  if (!pieChartRef.value) return null
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }
  return pieChart
}

function renderTrend(rows: TrendDataItem[]) {
  const chart = ensureTrendChart()
  if (!chart) return
  const dates = rows.map((d) => formatTrendDate(d.date))
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { show: false },
    grid: { top: 10, right: 10, bottom: 30, left: 10, containLabel: true },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#9ca3af', fontSize: 11 },
    },
    yAxis: [
      { type: 'value', show: false },
      { type: 'value', show: false },
    ],
    series: [
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#047857', width: 2 },
        itemStyle: { color: '#047857' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(5,150,105,0.2)' },
            { offset: 1, color: 'rgba(5,150,105,0)' },
          ]),
        },
        data: rows.map((d) => d.orders),
      },
      {
        name: '收入(元)',
        type: 'line',
        smooth: true,
        symbol: 'none',
        lineStyle: { color: '#0d9488', width: 1.5 },
        yAxisIndex: 1,
        data: rows.map((d) => Number(d.revenue) || 0),
      },
    ],
  })
}

const pieColors = ['#065f46', '#0d9488', '#F59E0B', '#6366f1', '#94a3b8']

function renderPie(dist: ChannelDistItem[]) {
  const chart = ensurePieChart()
  if (!chart) return
  const filtered = (dist ?? []).filter((item) => Number(item.value) > 0)
  const data = filtered.map((item, index) => ({
    name: String(item.name || item.channel || '其他'),
    value: item.value,
    itemStyle: { color: pieColors[index % pieColors.length] },
  }))
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}<br/>订单数 {c} 笔 ({d}%)' },
    legend: {
      type: 'scroll',
      orient: 'horizontal',
      bottom: 4,
      left: 'center',
      width: '92%',
      itemGap: 14,
      pageButtonItemGap: 6,
      textStyle: { color: '#64748b', fontSize: 11 },
      pageIconColor: '#047857',
      pageTextStyle: { color: '#64748b' },
    },
    series: [
      {
        type: 'pie',
        radius: ['44%', '70%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        label: { show: false },
        emphasis: { label: { show: false } },
        data,
      },
    ],
  })
}

/** 兼容后端 camelCase / snake_case，避免表格空白 */
function normalizeRecentOrderRow(raw: Record<string, unknown>): Partial<Order> {
  return {
    orderId: String(raw.orderId ?? raw.order_id ?? ''),
    merchantId: String(raw.merchantId ?? raw.merchant_id ?? ''),
    merchantOrderNo: String(raw.merchantOrderNo ?? raw.merchant_order_no ?? ''),
    subject: String(raw.subject ?? ''),
    amount: Number(raw.amount ?? 0),
    currency: String(raw.currency ?? 'CNY'),
    channel: String(raw.channel ?? ''),
    status: (raw.status != null ? String(raw.status) : undefined) as OrderStatus | undefined,
    expireTime: String(raw.expireTime ?? raw.expire_time ?? ''),
    createdAt: String(raw.createdAt ?? raw.created_at ?? '').replace('T', ' '),
    updatedAt: String(raw.updatedAt ?? raw.updated_at ?? '').replace('T', ' '),
  }
}

async function loadDashboard() {
  loading.value = true
  try {
    const days = trendRange.value === '30d' ? 30 : 7
    const data = await getDashboardStats(days)

    kpiCards[0].value = fmtMoneyYuan(data.todayRevenue)
    kpiCards[0].subVal = fmtMoneyYuan(data.yesterdayRevenue)
    kpiCards[0].trend = data.revenueChangePct ?? pctChange(data.todayRevenue, data.yesterdayRevenue)
    kpiCards[0].yoyTrend = data.revenueYoYPct ?? 0

    kpiCards[1].value = String(data.todayOrders ?? 0)
    kpiCards[1].subVal = String(data.yesterdayOrders ?? 0)
    kpiCards[1].trend = pctChange(data.todayOrders ?? 0, data.yesterdayOrders ?? 0)

    kpiCards[2].value = String(data.todayPaid ?? 0)
    kpiCards[2].sub = '—'

    const rate = data.conversionRate ?? 0
    kpiCards[3].value = `${rate}%`

    const rawRecent = data.recentOrders
    recentOrders.value = Array.isArray(rawRecent)
      ? rawRecent.map((r) => normalizeRecentOrderRow(r as Record<string, unknown>))
      : []

    renderTrend(data.trendData ?? [])
    renderPie(data.channelDistribution ?? [])

    // 加载流失预警摘要
    try {
      const churnResult = await getChurnAlerts({ page: 1, size: 5, status: 'pending' })
      churnAlerts.value = churnResult.list
    } catch {
      churnAlerts.value = []
    }
  } catch {
    ElMessage.error('加载仪表盘数据失败')
    recentOrders.value = []
  } finally {
    loading.value = false
  }
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  loadDashboard()
  resizeObserver = new ResizeObserver(() => {
    trendChart?.resize()
    pieChart?.resize()
  })
  if (trendChartRef.value) resizeObserver.observe(trendChartRef.value)
  if (pieChartRef.value) resizeObserver.observe(pieChartRef.value)
})

watch(trendRange, () => {
  loadDashboard()
})

onUnmounted(() => {
  trendChart?.dispose()
  pieChart?.dispose()
  resizeObserver?.disconnect()
})
</script>

