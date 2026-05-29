<template>
  <div class="page-table-shell long-tail-page">
    <div class="filter-bar">
      <el-form :inline="true" size="default" class="filter-bar__form">
        <el-form-item label="统计截止">
          <el-date-picker
            v-model="asOf"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="默认今天"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item class="filter-bar__actions">
          <el-button type="primary" class="btn-primary" icon="Search" @click="load">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
          <el-button class="btn-outline" icon="List" @click="openAllWorkItems">全部工单</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-loading="loading">
      <el-row :gutter="16" class="mb-4">
        <el-col v-for="kpi in kpiCards" :key="kpi.key" :xs="24" :sm="8">
          <div class="content-card long-tail-kpi">
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <p class="dashboard-kpi-label">{{ kpi.label }}</p>
                <p class="dashboard-kpi-value tabular-nums mt-2">{{ kpi.value }}</p>
                <p v-if="kpi.hint" class="dashboard-kpi-sub mt-1">{{ kpi.hint }}</p>
              </div>
              <span class="long-tail-kpi__icon" :class="kpi.tone">{{ kpi.icon }}</span>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="mb-4">
        <el-col :xs="24" :lg="14">
          <div class="content-card">
            <div class="flex items-center justify-between mb-4">
              <p class="dashboard-section-title">账龄分布</p>
              <span class="text-xs text-slate-500">未终态差异 · 按账龄分桶</span>
            </div>
            <div v-if="!hasBucketData" class="py-10">
              <el-empty description="当前无长尾差异，账务健康" :image-size="80" />
            </div>
            <div v-else ref="bucketChartRef" class="w-full h-[300px]" />
          </div>
        </el-col>
        <el-col :xs="24" :lg="10">
          <div class="content-card h-full flex flex-col">
            <TableToolbar title="分桶明细" :total="sortedBuckets.length" />
            <el-table table-layout="auto" :data="sortedBuckets" stripe size="small" class="data-table flex-1">
              <el-table-column label="账龄" min-width="108">
                <template #default="{ row }">
                  <el-tag size="small" :type="tagTypeOf(RECON_AGE_BUCKET_TAG, row.ageBucket)">
                    {{ labelOf(RECON_AGE_BUCKET_LABEL, row.ageBucket) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="笔数" width="72" align="right">
                <template #default="{ row }">
                  <span class="tabular-nums font-medium" :class="{ 'text-slate-400': !row.diffCount }">
                    {{ row.diffCount }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="金额" width="100" align="right" class-name="col-amount">
                <template #default="{ row }">
                  <span class="cell-amount">¥{{ formatMoneyFen(row.diffAmount) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="占比" width="72" align="right">
                <template #default="{ row }">
                  <span class="tabular-nums text-xs text-slate-500">{{ bucketPercent(row) }}%</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="72" align="center" class-name="col-actions">
                <template #default="{ row }">
                  <el-button
                    link
                    type="primary"
                    size="small"
                    :disabled="!row.diffCount"
                    @click="openWorkItems(row.ageBucket)"
                  >
                    工单
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>

      <div class="content-card">
        <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
          <div>
            <p class="dashboard-section-title">批量挂账</p>
            <p class="text-xs text-slate-500 mt-1">终态 ACCEPTED_LOSS · 需 recon:manage 权限</p>
          </div>
          <el-tag type="warning" effect="plain" size="small">单次 ≤ 50 条 · 原因 ≥ 20 字</el-tag>
        </div>

        <el-row :gutter="24">
          <el-col :xs="24" :md="14">
            <el-form label-position="top" class="long-tail-form">
              <el-form-item label="差异 ID（支持逗号、换行分隔）">
                <el-input
                  v-model="diffIdsText"
                  type="textarea"
                  :rows="4"
                  placeholder="例如：101, 102, 103"
                  resize="none"
                />
              </el-form-item>
            </el-form>
          </el-col>
          <el-col :xs="24" :md="10">
            <el-form label-position="top" class="long-tail-form">
              <el-form-item label="挂账原因">
                <el-input
                  v-model="remark"
                  type="textarea"
                  :rows="4"
                  placeholder="请说明业务背景与审批依据…"
                  resize="none"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item class="!mb-0">
                <el-button type="danger" class="btn-primary w-full sm:w-auto" @click="submitAcceptLoss">
                  提交挂账
                </el-button>
              </el-form-item>
            </el-form>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import { batchReconAcceptLoss, getReconLongTailSummary, type ReconLongTailSummary } from '@/api/admin'
import {
  RECON_AGE_BUCKET_LABEL,
  RECON_AGE_BUCKET_TAG,
  formatMoneyFen,
  labelOf,
  sortReconAgeBuckets,
  tagTypeOf,
} from '@/utils/format'

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const router = useRouter()
const loading = ref(false)
const summary = ref<ReconLongTailSummary | null>(null)
const asOf = ref('')
const diffIdsText = ref('')
const remark = ref('')
const bucketChartRef = ref<HTMLElement | null>(null)
let bucketChart: echarts.ECharts | null = null

const sortedBuckets = computed(() => sortReconAgeBuckets(summary.value?.buckets ?? []))

const bucketTotalCount = computed(() =>
  sortedBuckets.value.reduce((s, b) => s + Number(b.diffCount ?? 0), 0),
)

const bucketTotalAmountFen = computed(() =>
  sortedBuckets.value.reduce((s, b) => s + Number(b.diffAmount ?? 0), 0),
)

const hasBucketData = computed(() => bucketTotalCount.value > 0)

const kpiCards = computed(() => [
  {
    key: 'maxAge',
    label: '最长账龄',
    value: `${summary.value?.maxAgeDays ?? 0} 天`,
    hint: hasBucketData.value ? '未终态差异中最久未关闭' : '暂无待处理长尾',
    icon: '⏱',
    tone: 'tone-warn',
  },
  {
    key: 'count',
    label: '长尾笔数',
    value: String(bucketTotalCount.value),
    hint: '各账龄桶合计',
    icon: '📋',
    tone: 'tone-primary',
  },
  {
    key: 'amount',
    label: '长尾金额',
    value: `¥${formatMoneyFen(bucketTotalAmountFen.value)}`,
    hint: '各账龄桶合计',
    icon: '💰',
    tone: 'tone-muted',
  },
])

const bucketPercent = (row: { diffCount: number }) => {
  const total = bucketTotalCount.value
  if (!total) return '0.0'
  return ((Number(row.diffCount) / total) * 100).toFixed(1)
}

const renderChart = () => {
  const buckets = sortedBuckets.value
  if (!bucketChartRef.value) return
  if (!hasBucketData.value) {
    bucketChart?.dispose()
    bucketChart = null
    return
  }
  if (!bucketChart) bucketChart = echarts.init(bucketChartRef.value)
  const labels = buckets.map((b) => labelOf(RECON_AGE_BUCKET_LABEL, b.ageBucket))
  const counts = buckets.map((b) => b.diffCount)
  bucketChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: unknown) => {
        const p = (params as Array<{ name: string; value: number; dataIndex: number }>)[0]
        if (!p) return ''
        const b = buckets[p.dataIndex]
        return `${p.name}<br/>笔数：${p.value}<br/>金额：${formatMoneyFen(Number(b?.diffAmount ?? 0))} 分`
      },
    },
    grid: { left: 12, right: 24, top: 12, bottom: 8, containLabel: true },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: {
      type: 'category',
      data: labels,
      axisLabel: { width: 72, overflow: 'truncate' },
    },
    series: [
      {
        type: 'bar',
        data: counts,
        barMaxWidth: 28,
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: (params: { dataIndex: number }) => {
            const key = buckets[params.dataIndex]?.ageBucket
            const colors: Record<string, string> = {
              LT_1D: '#10b981',
              D1_3: '#3b82f6',
              D3_7: '#f59e0b',
              D7_30: '#f97316',
              GT_30: '#ef4444',
            }
            return colors[key ?? ''] ?? '#64748b'
          },
        },
      },
    ],
  })
}

const load = async () => {
  loading.value = true
  try {
    summary.value = await getReconLongTailSummary(asOf.value || undefined)
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  asOf.value = ''
  load()
}

const openWorkItems = (ageBucket: string) => {
  router.push({ path: '/admin/reconcile/work-items', query: { ageBucket } })
}

const openAllWorkItems = () => {
  router.push({ path: '/admin/reconcile/work-items', query: { onlyOverdue: '0' } })
}

const submitAcceptLoss = async () => {
  const ids = diffIdsText.value
    .split(/[,，\s]+/)
    .map((s) => Number(s.trim()))
    .filter((n) => !Number.isNaN(n))
  if (!ids.length) {
    ElMessage.warning('请填写差异 ID')
    return
  }
  if ((remark.value?.trim().length ?? 0) < 20) {
    ElMessage.warning('挂账原因不少于 20 字')
    return
  }
  await batchReconAcceptLoss({ diffIds: ids, remark: remark.value })
  ElMessage.success('挂账成功')
  diffIdsText.value = ''
  remark.value = ''
  await load()
}

const handleResize = () => bucketChart?.resize()
watch(sortedBuckets, () => nextTick(renderChart))

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  bucketChart?.dispose()
})
</script>

<style scoped>
.long-tail-kpi {
  padding: 20px 22px;
  min-height: 108px;
}
.long-tail-kpi__icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  border-radius: 10px;
  background: var(--el-fill-color-light);
}
.long-tail-kpi__icon.tone-primary {
  background: color-mix(in srgb, var(--pf-primary, #0f766e) 12%, transparent);
}
.long-tail-kpi__icon.tone-warn {
  background: color-mix(in srgb, #f59e0b 14%, transparent);
}
.long-tail-kpi__icon.tone-muted {
  background: var(--el-fill-color);
}
.long-tail-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 500;
  color: var(--pf-text-secondary);
  padding-bottom: 6px;
}
.long-tail-form :deep(.el-textarea__inner) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
}
</style>
