<template>
  <div>
    <!-- KPI 卡片区 -->
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
            昨日 {{ kpi.sub }}
            <span
              class="ml-1 font-medium"
              :class="kpi.trend > 0 ? 'text-[#EF4444]' : 'text-[#10B981]'"
            >
              {{ kpi.trend > 0 ? '↑' : '↓' }}{{ Math.abs(kpi.trend) }}%
            </span>
          </p>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
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

    <!-- 最新交易 -->
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
            <span class="font-medium">¥{{ (row.amount / 100).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="渠道" prop="channel" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.channel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="90">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="statusTypeMap[row.status] ?? 'info'"
            >
              {{ statusLabelMap[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="170" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { Order } from '@/types'

echarts.use([LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, CanvasRenderer])

const loading = ref(false)
const trendRange = ref('7d')
const trendChartRef = ref<HTMLDivElement | null>(null)
const pieChartRef = ref<HTMLDivElement | null>(null)

let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const kpiCards = reactive([
  { label: '今日收入', value: '¥12,580.00', sub: '¥9,230.00', trend: 36, icon: '💰' },
  { label: '今日订单', value: '256', sub: '198', trend: 29, icon: '📋' },
  { label: '已付订单', value: '231', sub: '182', trend: 27, icon: '✅' },
  { label: '转化率', value: '90.2%', sub: '91.9%', trend: -2, icon: '📈' },
])

const recentOrders = ref<Order[]>([
  { orderId: 'PF202604100001', merchantId: 'M001', merchantOrderNo: 'ORD20260410001', subject: '测试商品', amount: 10000, currency: 'CNY', channel: 'ALIPAY', status: 'PAID', expireTime: '', createdAt: '2026-04-10 20:30:00', updatedAt: '2026-04-10 20:31:00' },
  { orderId: 'PF202604100002', merchantId: 'M001', merchantOrderNo: 'ORD20260410002', subject: 'VIP会员', amount: 29900, currency: 'CNY', channel: 'WECHAT_PAY', status: 'PAID', expireTime: '', createdAt: '2026-04-10 19:45:00', updatedAt: '2026-04-10 19:47:00' },
  { orderId: 'PF202604100003', merchantId: 'M001', merchantOrderNo: 'ORD20260410003', subject: '企业认证', amount: 50000, currency: 'CNY', channel: 'ALIPAY', status: 'CREATED', expireTime: '', createdAt: '2026-04-10 19:00:00', updatedAt: '2026-04-10 19:00:00' },
])

const trendData = [
  { date: '04-04', orders: 180, revenue: 890000 },
  { date: '04-05', orders: 195, revenue: 920000 },
  { date: '04-06', orders: 210, revenue: 1050000 },
  { date: '04-07', orders: 188, revenue: 870000 },
  { date: '04-08', orders: 230, revenue: 1180000 },
  { date: '04-09', orders: 198, revenue: 923000 },
  { date: '04-10', orders: 256, revenue: 1258000 },
]

const channelData = [
  { name: '支付宝', value: 58 },
  { name: '微信支付', value: 35 },
  { name: '银联', value: 7 },
]

const statusTypeMap: Record<string, string> = {
  PAID: 'success', PAYING: 'warning', CREATED: 'info', EXPIRED: 'info', FAILED: 'danger',
}

const statusLabelMap: Record<string, string> = {
  PAID: '已支付', PAYING: '支付中', CREATED: '待支付', EXPIRED: '已过期', FAILED: '失败',
}

function initTrendChart() {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { show: false },
    grid: { top: 10, right: 10, bottom: 30, left: 10, containLabel: true },
    xAxis: {
      type: 'category', data: trendData.map((d) => d.date),
      boundaryGap: false, axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#9ca3af', fontSize: 11 },
    },
    yAxis: [{ type: 'value', show: false, max: 300 }, { type: 'value', show: false, max: 1500000 }],
    series: [
      {
        name: '订单数', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        lineStyle: { color: '#4F46E5', width: 2 }, itemStyle: { color: '#4F46E5' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(79,70,229,0.15)' }, { offset: 1, color: 'rgba(79,70,229,0)' }]) },
        data: trendData.map((d) => d.orders),
      },
      {
        name: '收入', type: 'line', smooth: true, symbol: 'none',
        lineStyle: { color: '#10B981', width: 1.5 }, yAxisIndex: 1,
        data: trendData.map((d) => d.revenue),
      },
    ],
  })
}

function initPieChart() {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
    legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { color: '#6b7280', fontSize: 12 } },
    series: [{
      type: 'pie', radius: ['50%', '75%'], center: ['35%', '50%'],
      avoidLabelOverlap: false, label: { show: false }, emphasis: { label: { show: false } },
      data: channelData.map((item, index) => ({
        ...item,
        itemStyle: { color: ['#4F46E5', '#10B981', '#F59E0B'][index] },
      })),
    }],
  })
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  initTrendChart()
  initPieChart()
  resizeObserver = new ResizeObserver(() => {
    trendChart?.resize()
    pieChart?.resize()
  })
  if (trendChartRef.value) resizeObserver.observe(trendChartRef.value)
})

onUnmounted(() => {
  trendChart?.dispose()
  pieChart?.dispose()
  resizeObserver?.disconnect()
})
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
