<template>
  <div class="page-table-shell recon-dashboard-page">
    <div class="filter-bar">
      <el-form :inline="true" size="default" class="filter-bar__form">
        <el-form-item label="账单日">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            :shortcuts="dateShortcuts"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="channel" placeholder="全部" clearable style="width: 130px">
            <el-option label="全部" value="" />
            <el-option label="支付宝" value="alipay" />
            <el-option label="微信" value="wxpay" />
            <el-option label="银联" value="unionpay" />
          </el-select>
        </el-form-item>
        <el-form-item label="差异类型">
          <el-select v-model="diffType" placeholder="全部" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option
              v-for="(label, key) in RECON_DIFF_LABEL"
              :key="key"
              :label="label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-bar__actions">
          <el-button type="primary" class="btn-primary" icon="Search" @click="load">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-loading="loading">
      <el-row :gutter="16" class="mb-4">
        <el-col v-for="kpi in kpiCards" :key="kpi.label" :xs="12" :sm="12" :md="6">
          <div class="content-card recon-kpi-card">
            <p class="dashboard-kpi-label">{{ kpi.label }}</p>
            <p class="dashboard-kpi-value tabular-nums mt-2">{{ kpi.value }}</p>
            <p v-if="kpi.sub" class="dashboard-kpi-sub mt-1">{{ kpi.sub }}</p>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="mb-4">
        <el-col :xs="24" :lg="14">
          <div class="content-card">
            <p class="dashboard-section-title mb-4">差异趋势</p>
            <div v-if="!(data?.trend?.length)" class="py-12">
              <el-empty description="所选周期暂无趋势数据" :image-size="72" />
            </div>
            <div v-else ref="trendChartRef" class="w-full h-[260px]" />
          </div>
        </el-col>
        <el-col :xs="24" :lg="10">
          <div class="content-card h-full">
            <p class="dashboard-section-title mb-4">SLA 概览</p>
            <div class="grid grid-cols-2 gap-3">
              <div class="recon-stat-tile">
                <div class="recon-stat-tile__label">样本量</div>
                <div class="recon-stat-tile__value tabular-nums">{{ data?.slaStats?.sample ?? 0 }}</div>
              </div>
              <div class="recon-stat-tile">
                <div class="recon-stat-tile__label">平均处理</div>
                <div class="recon-stat-tile__value tabular-nums">
                  {{ formatMinutes(data?.slaStats?.avgHandleMinutes) }}
                </div>
              </div>
              <div class="recon-stat-tile">
                <div class="recon-stat-tile__label">SLA 达成率</div>
                <div class="recon-stat-tile__value tabular-nums">{{ formatRate(data?.slaStats?.slaMetRate) }}</div>
              </div>
              <div class="recon-stat-tile">
                <div class="recon-stat-tile__label">长尾率</div>
                <div class="recon-stat-tile__value tabular-nums">{{ formatRate(data?.slaStats?.longTailRate) }}</div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <div class="content-card mb-4">
        <TableToolbar title="渠道 × 差异类型矩阵" :total="matrixTotal" hint="点击下钻可跳转差异工单列表" />
        <el-table
          table-layout="auto"
          :data="data?.matrix ?? []"
          stripe
          size="small"
          class="data-table"
          empty-text="暂无差异数据"
        >
          <el-table-column label="渠道" prop="channel" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="channelTagType(row.channel)">{{ channelLabel(row.channel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="差异类型" prop="diffType" min-width="140">
            <template #default="{ row }">
              <el-tag size="small" type="warning" effect="plain">
                {{ labelOf(RECON_DIFF_LABEL, row.diffType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="笔数" prop="diffCount" width="100" align="right">
            <template #default="{ row }">
              <span class="tabular-nums font-medium">{{ row.diffCount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="金额" prop="diffAmount" min-width="120" align="right" class-name="col-amount">
            <template #default="{ row }">
              <span class="cell-amount">¥{{ formatMoneyFen(row.diffAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right" class-name="col-actions">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="drill(row)">下钻</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <div class="content-card">
            <TableToolbar title="TOP 商户" :total="data?.topMerchants?.length ?? 0" />
            <el-table
              table-layout="auto"
              :data="data?.topMerchants ?? []"
              stripe
              size="small"
              class="data-table"
              empty-text="暂无数据"
            >
              <el-table-column label="商户" prop="key" min-width="140" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="cell-mono">{{ row.key || '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="笔数" prop="diffCount" width="88" align="right">
                <template #default="{ row }">
                  <span class="tabular-nums">{{ row.diffCount }}</span>
                </template>
              </el-table-column>
              <el-table-column label="金额" prop="diffAmount" width="120" align="right" class-name="col-amount">
                <template #default="{ row }">
                  <span class="cell-amount">¥{{ formatMoneyFen(row.diffAmount) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="content-card">
            <TableToolbar title="TOP 账户" :total="data?.topAccounts?.length ?? 0" />
            <el-table
              table-layout="auto"
              :data="data?.topAccounts ?? []"
              stripe
              size="small"
              class="data-table"
              empty-text="暂无数据"
            >
              <el-table-column label="账户" prop="key" min-width="140" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="cell-mono">{{ row.key || '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="笔数" prop="diffCount" width="88" align="right">
                <template #default="{ row }">
                  <span class="tabular-nums">{{ row.diffCount }}</span>
                </template>
              </el-table-column>
              <el-table-column label="金额(分)" prop="diffAmount" width="120" align="right">
                <template #default="{ row }">
                  <span class="cell-amount">¥{{ formatMoneyFen(row.diffAmount) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import { getReconAggregationDashboard, type ReconAggregationDashboard } from '@/api/admin'
import {
  RECON_DIFF_LABEL,
  channelLabel,
  channelTagType,
  formatMoneyFen,
  labelOf,
} from '@/utils/format'

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const router = useRouter()
const loading = ref(false)
const data = ref<ReconAggregationDashboard | null>(null)
const channel = ref('')
const diffType = ref('')
const dateRange = ref<[string, string]>([
  new Date(Date.now() - 7 * 86400000).toISOString().slice(0, 10),
  new Date().toISOString().slice(0, 10),
])
const trendChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null

const dateShortcuts = [
  {
    text: '近7天',
    value: () => {
      const end = new Date()
      const start = new Date(Date.now() - 6 * 86400000)
      return [start, end]
    },
  },
  {
    text: '近30天',
    value: () => {
      const end = new Date()
      const start = new Date(Date.now() - 29 * 86400000)
      return [start, end]
    },
  },
]

const matrixTotal = computed(() =>
  (data.value?.matrix ?? []).reduce((s, r) => s + Number(r.diffCount ?? 0), 0),
)

const kpiCards = computed(() => {
  const matrix = data.value?.matrix ?? []
  const diffCount = matrix.reduce((s, r) => s + Number(r.diffCount ?? 0), 0)
  const diffAmount = matrix.reduce((s, r) => s + Number(r.diffAmount ?? 0), 0)
  return [
    { label: '差异笔数', value: String(diffCount), sub: '矩阵汇总' },
    { label: '差异金额', value: `¥${formatMoneyFen(diffAmount)}`, sub: '矩阵汇总' },
    { label: '矩阵维度', value: String(matrix.length), sub: '渠道×类型' },
    {
      label: 'SLA 样本',
      value: String(data.value?.slaStats?.sample ?? 0),
      sub: `达成率 ${formatRate(data.value?.slaStats?.slaMetRate)}`,
    },
  ]
})

const formatRate = (v?: number) => (v != null ? `${(v * 100).toFixed(1)}%` : '—')
const formatMinutes = (v?: number) => (v != null ? `${v.toFixed(1)} 分钟` : '—')
const renderTrendChart = () => {
  const trend = data.value?.trend ?? []
  if (!trendChartRef.value || !trend.length) {
    trendChart?.dispose()
    trendChart = null
    return
  }
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 24, top: 24, bottom: 32 },
    xAxis: { type: 'category', data: trend.map((t) => t.period), boundaryGap: false },
    yAxis: { type: 'value', name: '笔数' },
    series: [
      {
        name: '差异笔数',
        type: 'line',
        smooth: true,
        data: trend.map((t) => t.diffCount),
        areaStyle: { opacity: 0.08 },
      },
    ],
  })
}

const load = async () => {
  if (!dateRange.value?.length) return
  loading.value = true
  try {
    data.value = await getReconAggregationDashboard({
      dateFrom: dateRange.value[0],
      dateTo: dateRange.value[1],
      channel: channel.value || undefined,
      diffType: diffType.value || undefined,
    })
    await nextTick()
    renderTrendChart()
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  dateRange.value = [
    new Date(Date.now() - 7 * 86400000).toISOString().slice(0, 10),
    new Date().toISOString().slice(0, 10),
  ]
  channel.value = ''
  diffType.value = ''
  load()
}

const drill = (row: { channel: string; diffType: string }) => {
  router.push({
    path: '/admin/reconcile/work-items',
    query: { diffType: row.diffType, channel: row.channel },
  })
}

const handleResize = () => trendChart?.resize()
watch(() => data.value?.trend, () => nextTick(renderTrendChart))

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style scoped>
.recon-kpi-card {
  padding: 20px 22px;
}
.recon-stat-tile {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  padding: 14px 16px;
}
.recon-stat-tile__label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}
.recon-stat-tile__value {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>
