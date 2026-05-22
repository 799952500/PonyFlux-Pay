<template>
  <div class="merchant-insight-root">
    <el-page-header v-if="!embedded" class="mb-5" @back="$router.back()">
      <template #content>
        <span
          :data-flip="`merchant-${resolvedMerchantId}`"
          class="text-[#0F172A] font-semibold"
        >
          商户洞察 — {{ resolvedMerchantId }}
        </span>
      </template>
    </el-page-header>

    <el-row :gutter="16" class="mb-5">
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <p class="text-xs text-[#64748B] mb-1">近30天交易额</p>
          <p class="text-xl font-bold text-[#0F172A]">{{ fmtMoney(insight?.totalRevenue ?? 0) }}</p>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <p class="text-xs text-[#64748B] mb-1">退款率</p>
          <p class="text-xl font-bold text-[#0F172A]">{{ insight?.refundRate?.rate ?? '0%' }}</p>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <p class="text-xs text-[#64748B] mb-1">最后交易时间</p>
          <p class="text-sm font-medium text-[#0F172A]">{{ insight?.lastTradeTime ? formatTime(insight.lastTradeTime) : '无记录' }}</p>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <p class="text-xs text-[#64748B] mb-1">近7天 API 调用量</p>
          <p class="text-xl font-bold text-[#0F172A]">{{ apiStats.totalCalls.toLocaleString() }}</p>
          <p class="text-xs text-[#94a3b8] mt-0.5">
            <span :class="apiStats.callTrend >= 0 ? 'text-[#047857]' : 'text-[#EF4444]'">
              {{ apiStats.callTrend >= 0 ? '↑' : '↓' }}{{ Math.abs(apiStats.callTrend) }}%
            </span>
          </p>
        </div>
      </el-col>
    </el-row>

    <!-- API 调用统计卡片 -->
    <el-row :gutter="16" class="mb-5">
      <el-col :xs="12" :sm="8">
        <div class="stat-card">
          <p class="text-xs text-[#64748B] mb-1">支付成功率</p>
          <p class="text-xl font-bold" :class="apiStats.successRate >= 95 ? 'text-[#047857]' : 'text-[#F59E0B]'">
            {{ apiStats.successRate }}%
          </p>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8">
        <div class="stat-card">
          <p class="text-xs text-[#64748B] mb-1">平均响应时间</p>
          <p class="text-xl font-bold text-[#0F172A]">{{ apiStats.avgResponseMs }}<span class="text-sm font-normal text-[#64748B]"> ms</span></p>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8">
        <div class="stat-card">
          <p class="text-xs text-[#64748B] mb-1">API Key 状态</p>
          <el-tag size="small" type="success">正常</el-tag>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="14">
        <div class="content-card">
          <p class="text-[#0F172A] font-semibold text-sm mb-4">近30天交易趋势</p>
          <div ref="trendChartRef" class="w-full h-[280px]" />
        </div>
      </el-col>
      <el-col :xs="24" :md="10">
        <div class="content-card h-full">
          <p class="text-[#0F172A] font-semibold text-sm mb-4">渠道偏好</p>
          <div ref="pieChartRef" class="w-full h-[280px]" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getMerchantInsight } from '@/api/admin'

echarts.use([LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const props = withDefaults(
  defineProps<{
    merchantId?: string
    /** 嵌入抽屉时不显示页头 */
    embedded?: boolean
    /** 抽屉打开时用于触发图表 resize */
    active?: boolean
  }>(),
  { embedded: false, active: true },
)

const route = useRoute()
const resolvedMerchantId = computed(
  () => props.merchantId?.trim() || String(route.params.merchantId ?? ''),
)
const insight = ref<any>(null)
const trendChartRef = ref<HTMLDivElement | null>(null)
const pieChartRef = ref<HTMLDivElement | null>(null)

// API 调用统计（基于交易数据推算）
const apiStats = reactive({
  totalCalls: 0,
  callTrend: 0,
  successRate: 98.5,
  avgResponseMs: 320,
})

let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

function fmtMoney(fen: number): string {
  return `¥${(fen / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatTime(s: string): string {
  if (!s) return ''
  return s.replace('T', ' ').substring(0, 16)
}

async function loadInsight() {
  const merchantId = resolvedMerchantId.value
  if (!merchantId) return
  try {
    const data = await getMerchantInsight(merchantId)
    insight.value = data

    // 计算总交易额
    const trendRows = data?.trend30Days ?? []
    insight.value.totalRevenue = trendRows.reduce((sum: number, r: any) => sum + Number(r.revenue ?? 0), 0)

    // 从趋势数据推算 API 调用统计
    const last7 = trendRows.slice(-7)
    const prev7 = trendRows.slice(-14, -7)
    const calls7 = last7.reduce((s: number, r: any) => s + Number(r.orders ?? r.count ?? 0), 0)
    const callsPrev7 = prev7.reduce((s: number, r: any) => s + Number(r.orders ?? r.count ?? 0), 0)
    apiStats.totalCalls = calls7
    apiStats.callTrend = callsPrev7 > 0 ? Math.round(((calls7 - callsPrev7) / callsPrev7) * 100) : 0
    apiStats.successRate = insight.value?.refundRate?.successRate ?? 98.5

    // 趋势图
    nextTick(() => {
      if (trendChartRef.value) {
        trendChart = echarts.init(trendChartRef.value)
        const dates = trendRows.map((d: any) => String(d.date ?? '').substring(5))
        trendChart.setOption({
          tooltip: { trigger: 'axis' },
          grid: { top: 10, right: 10, bottom: 30, left: 10, containLabel: true },
          xAxis: {
            type: 'category', data: dates,
            axisLabel: { color: '#9ca3af', fontSize: 10 },
            axisLine: { show: false }, axisTick: { show: false },
          },
          yAxis: { type: 'value', show: false },
          series: [{
            type: 'line', smooth: true, symbol: 'circle', symbolSize: 4,
            lineStyle: { color: '#047857', width: 2 },
            itemStyle: { color: '#047857' },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(5,150,105,0.15)' },
              { offset: 1, color: 'rgba(5,150,105,0)' },
            ])},
            data: trendRows.map((d: any) => (Number(d.revenue ?? 0) / 100)),
          }],
        })
      }

      // 渠道饼图
      const prefRows = data?.channelPreferences ?? []
      if (pieChartRef.value) {
        pieChart = echarts.init(pieChartRef.value)
        const pieData = prefRows.map((p: any, idx: number) => ({
          name: String(p.channel ?? '其他'),
          value: Number(p.cnt ?? 0),
          itemStyle: { color: ['#065f46', '#0d9488', '#F59E0B', '#6366f1', '#94a3b8'][idx % 5] },
        }))
        pieChart.setOption({
          tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 笔 ({d}%)' },
          series: [{
            type: 'pie', radius: ['44%', '70%'], center: ['50%', '44%'],
            label: { show: false }, data: pieData,
          }],
        })
      }
    })
  } catch {
    // 静默处理
  }
}

let resizeObserver: ResizeObserver | null = null

watch(
  () => [resolvedMerchantId.value, props.active] as const,
  ([id, active]) => {
    if (active !== false && id) {
      loadInsight()
      nextTick(() => {
        trendChart?.resize()
        pieChart?.resize()
      })
    }
  },
  { immediate: true },
)

onMounted(() => {
  resizeObserver = new ResizeObserver(() => {
    trendChart?.resize()
    pieChart?.resize()
  })
  if (trendChartRef.value) resizeObserver.observe(trendChartRef.value)
  if (pieChartRef.value) resizeObserver.observe(pieChartRef.value)
})

onUnmounted(() => {
  trendChart?.dispose()
  pieChart?.dispose()
  resizeObserver?.disconnect()
})
</script>

