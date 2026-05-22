<template>
  <div class="page-table-shell">
    <div v-if="orderStats" class="content-card stats-panel">
      <div class="stats-panel__label">订单统计</div>
      <div class="stats-panel__tags">
        <el-tag type="info" effect="plain" size="default">全部 {{ orderStats.total }}</el-tag>
        <el-tag
          v-for="row in statsTags"
          :key="row.status"
          size="default"
          :type="tagTypeOf(ORDER_STATUS_TAG, row.status)"
          effect="plain"
        >
          {{ labelOf(ORDER_STATUS_LABEL, row.status) }} {{ row.cnt }}
        </el-tag>
      </div>
    </div>
    <!-- 筛选工具栏 -->
    <div class="filter-bar">
      <el-form :inline="true" :model="queryForm" size="default" class="filter-bar__form">
        <el-form-item v-if="!merchantFilterLocked || authorizedMerchantIds.length > 1" label="商户号">
          <el-select
            v-if="merchantFilterLocked && authorizedMerchantIds.length > 1"
            v-model="queryForm.merchantId"
            placeholder="选择商户"
            clearable
            style="width: 168px"
            @change="handleSearch"
          >
            <el-option v-for="mid in authorizedMerchantIds" :key="mid" :label="mid" :value="mid" />
          </el-select>
          <el-input
            v-else
            v-model="queryForm.merchantId"
            placeholder="筛选商户"
            clearable
            style="width: 168px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="订单号 / 商户订单号" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 136px">
            <el-option label="全部" value="" />
            <el-option label="待支付" value="CREATED" />
            <el-option label="支付中" value="PAYING" />
            <el-option label="已支付" value="PAID" />
            <el-option label="已过期" value="EXPIRED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="queryForm.channel" placeholder="全部" clearable style="width: 136px">
            <el-option label="全部" value="" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信支付" value="WECHAT_PAY" />
            <el-option label="银联" value="UNION_PAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item class="filter-bar__actions">
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
          <el-button class="btn-outline" :loading="exporting" @click="handleExportCsv">导出 CSV</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <TableToolbar title="订单列表" :total="total" />

      <el-table table-layout="auto" v-loading="loading" :data="orderList" stripe size="small" @row-click="openDetail" class="data-table">
        <el-table-column label="订单号" prop="orderId" min-width="160">
          <template #default="{ row }">
            <span
              :data-flip="`order-${row.orderId}`"
              class="cell-mono pf-link cursor-pointer"
            >#{{ row.orderId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商户订单号" prop="merchantOrderNo" min-width="150" />
        <el-table-column label="商品" prop="subject" min-width="140">
          <template #default="{ row }"><span class="truncate block max-w-[140px]">{{ row.subject }}</span></template>
        </el-table-column>
        <el-table-column label="金额（元）" prop="amount" width="110" align="right" class-name="col-amount">
          <template #default="{ row }">
            <span class="cell-amount">¥{{ formatMoneyFen(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="渠道" prop="channel" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="channelTagType(row.channel)">{{ channelLabel(row.channel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="tagTypeOf(ORDER_STATUS_TAG, row.status)">
              {{ labelOf(ORDER_STATUS_LABEL, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" min-width="168" class-name="col-datetime" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-datetime">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="80" class-name="col-actions" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <AdminPagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        @size-change="loadOrders"
        @current-change="loadOrders"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useMerchantScope } from '@/composables/useMerchantScope'
import { useOrderDetailOverlay } from '@/composables/useOrderDetailOverlay'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import { getOrders, getOrderStats, exportOrdersCsv } from '@/api/admin'
import type { Order, OrderListQuery, OrderStats } from '@/types'
import {
  channelLabel,
  channelTagType,
  formatDateTime,
  formatMoneyFen,
  labelOf,
  ORDER_STATUS_LABEL,
  ORDER_STATUS_TAG,
  tagTypeOf,
} from '@/utils/format'

const route = useRoute()
const router = useRouter()
const { open: openOrderDetail } = useOrderDetailOverlay()
const { merchantFilterLocked, authorizedMerchantIds, applyDefaultMerchantFilter, isMerchantAllowed } =
  useMerchantScope()

const loading = ref(false)
const exporting = ref(false)
const orderList = ref<Order[]>([])
const total = ref(0)
const orderStats = ref<OrderStats | null>(null)
const dateRange = ref<[string, string] | null>(null)

const queryForm = reactive<OrderListQuery>({
  page: 1,
  pageSize: 20,
  status: undefined,
  channel: undefined,
  keyword: undefined,
  merchantId: undefined,
})

/** 统计区固定展示的状态（无数据时显示 0） */
const ORDER_STATUSES_FOR_STATS = ['CREATED', 'PAYING', 'PAID', 'EXPIRED', 'FAILED', 'CLOSED'] as const

const statsTags = computed(() => {
  if (!orderStats.value) return []
  const map = new Map(orderStats.value.statusCount.map((r) => [r.status, r.cnt]))
  const tags: Array<{ status: string; cnt: number }> = ORDER_STATUSES_FOR_STATS.map((status) => ({
    status,
    cnt: map.get(status) ?? 0,
  }))
  for (const row of orderStats.value.statusCount) {
    if (!(ORDER_STATUSES_FOR_STATS as readonly string[]).includes(row.status)) {
      tags.push(row)
    }
  }
  return tags
})

async function loadOrders() {
  loading.value = true
  try {
    const params: OrderListQuery & { dateRange?: [string, string] } = { ...queryForm }
    if (dateRange.value) params.dateRange = dateRange.value
    const resp = await getOrders(params)
    orderList.value = resp.list
    total.value = resp.total
  } catch { ElMessage.error('加载订单列表失败') }
  finally { loading.value = false }
}

async function loadStats() {
  try {
    const params: { merchantId?: string } = {}
    if (queryForm.merchantId?.trim()) params.merchantId = queryForm.merchantId.trim()
    orderStats.value = await getOrderStats(params)
  } catch {
    orderStats.value = null
  }
}

async function handleExportCsv() {
  exporting.value = true
  try {
    const f: { merchantId?: string; status?: string; startTime?: string; endTime?: string } = {}
    if (queryForm.merchantId) f.merchantId = queryForm.merchantId
    if (queryForm.status) f.status = queryForm.status
    if (dateRange.value?.length === 2) {
      f.startTime = `${dateRange.value[0]} 00:00:00`
      f.endTime = `${dateRange.value[1]} 23:59:59`
    }
    await exportOrdersCsv(f)
    ElMessage.success('已开始下载')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '导出失败'
    ElMessage.error(msg)
  } finally {
    exporting.value = false
  }
}

function handleSearch() {
  syncMerchantIdToUrl()
  queryForm.page = 1
  loadOrders()
  loadStats()
}

function handleReset() {
  Object.assign(queryForm, {
    page: 1,
    pageSize: 20,
    status: undefined,
    channel: undefined,
    keyword: undefined,
    merchantId: undefined,
  })
  dateRange.value = null
  const hadMerchantInUrl = !!route.query.merchantId
  if (hadMerchantInUrl) router.replace({ path: '/admin/orders' })
  else {
    loadOrders()
    loadStats()
  }
}

function syncMerchantIdToUrl() {
  const mid = queryForm.merchantId?.trim()
  if (mid) {
    router.replace({ path: '/admin/orders', query: { merchantId: mid } })
  } else if (route.query.merchantId) {
    router.replace({ path: '/admin/orders' })
  }
}

function openDetail(row: Order) {
  openOrderDetail(row.orderId)
}

function tryOpenOrderFromQuery() {
  const raw = route.query.orderId
  const id = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : ''
  if (!id?.trim()) return
  openOrderDetail(id.trim())
  const q = { ...route.query }
  delete q.orderId
  router.replace({ path: '/admin/orders', query: q })
}

onMounted(() => {
  if (!queryForm.merchantId) {
    applyDefaultMerchantFilter(queryForm)
  }
  tryOpenOrderFromQuery()
})

watch(
  () => route.query.merchantId,
  (mid) => {
    const s = typeof mid === 'string' ? mid : Array.isArray(mid) ? mid[0] : ''
    queryForm.merchantId = s || undefined
    if (!queryForm.merchantId) {
      applyDefaultMerchantFilter(queryForm)
    }
    if (queryForm.merchantId && !isMerchantAllowed(queryForm.merchantId)) {
      queryForm.merchantId = defaultMerchantIdFromScope()
      ElMessage.warning('无权查看该商户订单')
    }
    queryForm.page = 1
    loadOrders()
    loadStats()
  },
  { immediate: true }
)

watch(
  () => route.query.orderId,
  () => tryOpenOrderFromQuery(),
)

function defaultMerchantIdFromScope() {
  return authorizedMerchantIds.value.length === 1 ? authorizedMerchantIds.value[0] : undefined
}
</script>

