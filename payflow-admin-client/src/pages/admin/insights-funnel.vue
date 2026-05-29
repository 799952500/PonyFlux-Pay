<template>
  <div class="page-table-shell">
    <div class="content-card">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-lg font-semibold">{{ t('funnel.title') }}</h2>
        <div class="flex items-center gap-3">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            :shortcuts="dateShortcuts"
            value-format="YYYY-MM-DD"
            :start-placeholder="t('funnel.filters.dateFrom')"
            :end-placeholder="t('funnel.filters.dateTo')"
            size="default"
            @change="onFilterChange"
          />
          <el-select
            v-model="merchantId"
            :placeholder="t('funnel.filters.merchant')"
            clearable
            class="w-40"
            @change="onFilterChange"
          >
            <el-option
              v-for="m in merchantOptions"
              :key="m.merchantId"
              :label="m.merchantName"
              :value="m.merchantId"
            />
          </el-select>
          <el-select
            v-model="channel"
            :placeholder="t('funnel.filters.channel')"
            clearable
            class="w-36"
            @change="onFilterChange"
          >
            <el-option label="微信支付" value="WECHAT_PAY" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银联" value="UNION_PAY" />
          </el-select>
        </div>
      </div>

      <div v-loading="loading">
        <div v-if="isEmpty && !loading" class="py-16">
          <el-empty :description="t('funnel.empty')" />
        </div>

        <template v-else-if="funnelData">
          <div class="grid grid-cols-4 gap-4 mb-6">
            <div class="stat-card">
              <div class="stat-label">{{ t('funnel.overallRate') }}</div>
              <div class="stat-value text-blue-600">{{ funnelData.overallConversionRate ?? 0 }}%</div>
            </div>
            <div v-for="stage in funnelData.stages" :key="stage.name" class="stat-card">
              <div class="stat-label">{{ stageLabel(stage.name) }}</div>
              <div class="stat-value">{{ formatNumber(stage.count) }}</div>
              <div v-if="stage.rate != null" class="stat-sub text-green-600">↑ {{ stage.rate }}%</div>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-6">
            <div class="chart-container">
              <div ref="funnelChartRef" class="w-full h-[360px]" />
            </div>
            <div class="chart-container">
              <h3 class="text-sm font-medium mb-2 text-gray-600">{{ t('funnel.lossTitle') }}</h3>
              <div ref="lossChartRef" class="w-full h-[320px]" />
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getInsightsFunnel, getMerchantsSimple } from '@/api/admin'
import * as echarts from 'echarts/core'
import { FunnelChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import dayjs from 'dayjs'

echarts.use([FunnelChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const { t } = useI18n()

const loading = ref(false)
const funnelData = ref<any>(null)
const isEmpty = ref(false)

const dateRange = ref<[string, string]>([
  dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
  dayjs().format('YYYY-MM-DD'),
])
const merchantId = ref('')
const channel = ref('')
const merchantOptions = ref<any[]>([])

let abortController: AbortController | null = null

const funnelChartRef = ref<HTMLElement>()
const lossChartRef = ref<HTMLElement>()
let funnelChart: echarts.ECharts | null = null
let lossChart: echarts.ECharts | null = null

const dateShortcuts = [
  { text: '今日', value: () => { const d = dayjs().format('YYYY-MM-DD'); return [d, d] as [string, string] } },
  { text: '昨日', value: () => { const d = dayjs().subtract(1, 'day').format('YYYY-MM-DD'); return [d, d] as [string, string] } },
  { text: '最近 7 天', value: () => [dayjs().subtract(6, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')] as [string, string] },
  { text: '最近 30 天', value: () => [dayjs().subtract(29, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')] as [string, string] },
]

const stageLabels: Record<string, string> = {
  CREATED: '订单创建',
  PAYING: '进入支付',
  PAID: '支付成功',
}

function stageLabel(name: string) { return stageLabels[name] || name }

function formatNumber(n: number) {
  return n?.toLocaleString() ?? '0'
}

async function fetchFunnel() {
  if (abortController) abortController.abort()
  abortController = new AbortController()

  loading.value = true
  isEmpty.value = false
  try {
    const params: Record<string, string> = {}
    if (dateRange.value?.[0]) params.dateFrom = dateRange.value[0]
    if (dateRange.value?.[1]) params.dateTo = dateRange.value[1]
    if (merchantId.value) params.merchantId = merchantId.value
    if (channel.value) params.channel = channel.value

    const data = await getInsightsFunnel(params) as any
    funnelData.value = data

    const totalCreated = data?.stages?.[0]?.count ?? 0
    isEmpty.value = totalCreated === 0

    await nextTick()
    if (!isEmpty.value) {
      renderFunnelChart(data)
      renderLossChart(data)
    }
  } catch (e: any) {
    if (e?.name !== 'AbortError') {
      ElMessage.error(e?.message || '加载漏斗数据失败')
    }
  } finally {
    loading.value = false
  }
}

function renderFunnelChart(data: any) {
  if (!funnelChartRef.value) return
  if (!funnelChart) {
    funnelChart = echarts.init(funnelChartRef.value)
  }
  const stages = data.stages || []
  funnelChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'funnel',
      left: '10%',
      top: 20,
      bottom: 20,
      width: '80%',
      min: 0,
      max: stages[0]?.count || 100,
      minSize: '20%',
      maxSize: '100%',
      sort: 'descending',
      gap: 2,
      label: {
        show: true,
        position: 'inside',
        formatter: (p: any) => {
          const stage = stages.find((s: any) => s.name === p.name || stageLabel(s.name) === p.name)
          const rate = stage?.rate != null ? ` (${stage.rate}%)` : ''
          return `${p.name}\n${p.value.toLocaleString()}${rate}`
        },
      },
      itemStyle: { borderWidth: 0 },
      data: stages.map((s: any, i: number) => ({
        name: stageLabel(s.name),
        value: s.count,
        itemStyle: { color: ['#409EFF', '#67C23A', '#E6A23C'][i] || '#909399' },
      })),
    }],
  }, true)
}

function renderLossChart(data: any) {
  if (!lossChartRef.value) return
  if (!lossChart) {
    lossChart = echarts.init(lossChartRef.value)
  }
  const loss = data.lossBreakdown || []
  const lossLabels: Record<string, string> = { FAILED: '支付失败', CLOSED: '超时关单', EXPIRED: '过期' }
  const colors = ['#F56C6C', '#E6A23C', '#909399']

  lossChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 30, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: loss.map((l: any) => lossLabels[l.name] || l.name),
    },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      barWidth: 40,
      data: loss.map((l: any, i: number) => ({
        value: l.count,
        itemStyle: { color: colors[i] || '#909399' },
      })),
      label: {
        show: true,
        position: 'top',
        formatter: (p: any) => {
          const item = loss[p.dataIndex]
          return `${p.value} (${item?.percentage ?? 0}%)`
        },
      },
    }],
  }, true)
}

function onFilterChange() {
  fetchFunnel()
}

async function fetchMerchants() {
  try {
    merchantOptions.value = await getMerchantsSimple() ?? []
  } catch { /* 静默 */ }
}

onMounted(() => {
  fetchMerchants()
  fetchFunnel()
})

onUnmounted(() => {
  funnelChart?.dispose()
  lossChart?.dispose()
})

const handleResize = () => {
  funnelChart?.resize()
  lossChart?.resize()
}
window.addEventListener('resize', handleResize)
onUnmounted(() => window.removeEventListener('resize', handleResize))
</script>

<style scoped>
.stat-card {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}
.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.stat-sub {
  font-size: 12px;
  margin-top: 2px;
}
.chart-container {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  padding: 16px;
}
</style>
