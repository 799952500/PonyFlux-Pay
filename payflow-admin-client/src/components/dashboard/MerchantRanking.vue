<template>
  <div class="content-card">
    <div class="flex items-center justify-between mb-4">
      <p class="dashboard-section-title">商户交易排行</p>
      <el-radio-group v-model="rankDays" size="small" @change="loadRanking">
        <el-radio-button value="7">近7天</el-radio-button>
        <el-radio-button value="30">近30天</el-radio-button>
      </el-radio-group>
    </div>
    <div ref="barChartRef" class="w-full h-[260px]" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useMerchantInsightOverlay } from '@/composables/useMerchantInsightOverlay'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getMerchantRanking } from '@/api/admin'
import { registerFlipSource } from '@/transitions/flipShared'
import { getChartTheme } from '@/utils/chartTheme'
import { useThemeStore } from '@/stores/theme'
import { storeToRefs } from 'pinia'

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const { open: openMerchantInsight } = useMerchantInsightOverlay()
const { themeKey } = storeToRefs(useThemeStore())
const rankDays = ref<'7' | '30'>('7')
const barChartRef = ref<HTMLDivElement | null>(null)
let barChart: echarts.ECharts | null = null

function ensureChart() {
  if (!barChartRef.value) return null
  if (!barChart) {
    barChart = echarts.init(barChartRef.value)
  }
  return barChart
}

function formatAmount(fen: number): string {
  const yuan = fen / 100
  if (yuan >= 10000) {
    return `¥${(yuan / 10000).toFixed(1)}万`
  }
  return `¥${yuan.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`
}

async function loadRanking() {
  try {
    const ranking = await getMerchantRanking(Number(rankDays.value), 10)
    const chart = ensureChart()
    if (!chart) return

    const names = ranking.map((r: any) => {
      const id = String(r.merchantId ?? '')
      return id.length > 8 ? id.substring(0, 8) + '...' : id
    })
    const amounts = ranking.map((r: any) => Number(r.totalAmount ?? 0) / 100)
    const theme = getChartTheme()

    chart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        backgroundColor: theme.tooltipBg,
        borderColor: theme.tooltipBorder,
        textStyle: { color: theme.tooltipText },
        formatter: (params: any) => {
          const p = Array.isArray(params) ? params[0] : params
          const idx = p.dataIndex
          const item = ranking[idx]
          return `商户: ${item?.merchantId ?? ''}<br/>交易额: ¥${amounts[idx]?.toLocaleString() ?? 0}<br/>笔数: ${item?.totalCount ?? 0}`
        },
      },
      grid: { top: 10, right: 20, bottom: 30, left: 10, containLabel: true },
      xAxis: {
        type: 'category',
        data: names,
        axisLabel: { color: theme.axis, fontSize: 10 },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      yAxis: {
        type: 'value',
        show: false,
      },
      series: [
        {
          type: 'bar',
          data: amounts,
          barWidth: '50%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: theme.barGradientStart },
              { offset: 1, color: theme.barGradientEnd },
            ]),
            borderRadius: [4, 4, 0, 0],
          },
          emphasis: {
            itemStyle: { color: theme.linePrimary },
          },
        },
      ],
    })

    // 点击跳转商户详情
    chart.off('click')
    chart.on('click', (params: any) => {
      const idx = params.dataIndex
      const item = ranking[idx]
      if (!item?.merchantId) return

      const mouse = params.event?.event as MouseEvent | undefined
      if (mouse) {
        registerFlipSource(
          `merchant-${item.merchantId}`,
          new DOMRect(mouse.clientX - 48, mouse.clientY - 24, 96, 48),
        )
      }

      openMerchantInsight(item.merchantId)
    })
  } catch {
    // 静默处理
  }
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  nextTick(() => loadRanking())
  resizeObserver = new ResizeObserver(() => barChart?.resize())
  if (barChartRef.value) resizeObserver.observe(barChartRef.value)
})

watch(rankDays, () => loadRanking())
watch(themeKey, () => loadRanking())

onUnmounted(() => {
  barChart?.dispose()
  resizeObserver?.disconnect()
})
</script>

