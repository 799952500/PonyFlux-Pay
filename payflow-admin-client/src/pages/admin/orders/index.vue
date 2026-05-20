<template>
  <div>
    <div v-if="orderStats" class="content-card mb-4 stats-bar">
      <span class="text-sm text-slate-600 font-medium">订单统计</span>
      <el-tag type="info" effect="plain">全部 {{ orderStats.total }}</el-tag>
      <el-tag
        v-for="row in orderStats.statusCount"
        :key="row.status"
        size="small"
        :type="statusTypeMap[row.status] ?? 'info'"
      >
        {{ statusLabelMap[row.status] ?? row.status }} {{ row.cnt }}
      </el-tag>
    </div>
    <!-- 筛选工具栏 -->
    <div class="filter-bar">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="商户号">
          <el-input v-model="queryForm.merchantId" placeholder="筛选商户" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="订单号 / 商户订单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="全部" value="" />
            <el-option label="待支付" value="CREATED" />
            <el-option label="支付中" value="PAYING" />
            <el-option label="已支付" value="PAID" />
            <el-option label="已过期" value="EXPIRED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="queryForm.channel" placeholder="全部" clearable style="width: 130px">
            <el-option label="全部" value="" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信支付" value="WECHAT_PAY" />
            <el-option label="银联" value="UNION_PAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
          <el-button class="btn-outline" :loading="exporting" @click="handleExportCsv">导出 CSV</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 订单表格 -->
    <div class="content-card">
      <el-table v-loading="loading" :data="orderList" stripe size="small" @row-click="openDetail" class="data-table">
        <el-table-column label="订单号" prop="orderId" min-width="160">
          <template #default="{ row }">
            <span class="text-xs tabular-nums font-medium text-[#047857] cursor-pointer">#{{ row.orderId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商户订单号" prop="merchantOrderNo" min-width="150" />
        <el-table-column label="商品" prop="subject" min-width="140">
          <template #default="{ row }"><span class="truncate block max-w-[140px]">{{ row.subject }}</span></template>
        </el-table-column>
        <el-table-column label="金额（元）" prop="amount" width="110" align="right">
          <template #default="{ row }"><span class="font-semibold">¥{{ (row.amount / 100).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="渠道" prop="channel" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="channelTypeMap[row.channel] ?? 'info'">{{ channelLabelMap[row.channel] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTypeMap[row.status]">{{ statusLabelMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="170" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.pageSize"
          :total="total" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadOrders" @current-change="loadOrders"
        />
      </div>
    </div>

    <!-- 订单详情抽屉 -->
    <el-drawer v-model="showDetail" title="订单详情" direction="rtl" size="480px">
      <div v-if="detailLoading" class="p-4"><el-skeleton animated :rows="6" /></div>
      <div v-else-if="currentOrder" class="px-6 space-y-6">
        <section>
          <h3 class="text-sm font-semibold text-gray-700 mb-3 border-b pb-2">基本信息</h3>
          <dl class="grid grid-cols-2 gap-y-3 gap-x-4 text-sm">
            <dt class="text-gray-400">订单号</dt><dd class="text-gray-800 font-medium tabular-nums break-all">{{ currentOrder.orderId }}</dd>
            <dt class="text-gray-400">商户订单号</dt><dd class="text-gray-800 tabular-nums">{{ currentOrder.merchantOrderNo }}</dd>
            <dt class="text-gray-400">商户ID</dt><dd class="text-gray-800">{{ currentOrder.merchantId }}</dd>
            <dt class="text-gray-400">商品名称</dt><dd class="text-gray-800">{{ currentOrder.subject }}</dd>
            <dt class="text-gray-400">订单金额</dt><dd class="text-gray-800 font-bold">¥{{ (currentOrder.amount / 100).toFixed(2) }}</dd>
          </dl>
        </section>
        <section>
          <h3 class="text-sm font-semibold text-gray-700 mb-3 border-b pb-2">支付信息</h3>
          <dl class="grid grid-cols-2 gap-y-3 gap-x-4 text-sm">
            <dt class="text-gray-400">支付渠道</dt>
            <dd><el-tag size="small" :type="channelTypeMap[currentOrder.channel ?? ''] ?? 'info'">{{ channelLabelMap[currentOrder.channel ?? ''] }}</el-tag></dd>
            <dt class="text-gray-400">订单状态</dt>
            <dd><el-tag size="small" :type="statusTypeMap[currentOrder.status] ?? 'info'">{{ statusLabelMap[currentOrder.status] }}</el-tag></dd>
            <dt class="text-gray-400">创建时间</dt><dd class="text-gray-800">{{ currentOrder.createdAt }}</dd>
            <dt class="text-gray-400">过期时间</dt><dd class="text-gray-800">{{ currentOrder.expireTime }}</dd>
          </dl>
        </section>
        <div class="pt-2 flex gap-3">
          <el-button class="flex-1" @click="showDetail = false">关闭</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrders, getOrderStats, exportOrdersCsv } from '@/api/admin'
import type { Order, OrderListQuery, OrderStats } from '@/types'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const exporting = ref(false)
const orderList = ref<Order[]>([])
const total = ref(0)
const orderStats = ref<OrderStats | null>(null)
const showDetail = ref(false)
const currentOrder = ref<Order | null>(null)
const detailLoading = ref(false)
const dateRange = ref<[string, string] | null>(null)

const queryForm = reactive<OrderListQuery>({
  page: 1,
  pageSize: 20,
  status: undefined,
  channel: undefined,
  keyword: undefined,
  merchantId: undefined,
})

const statusTypeMap: Record<string, string> = { PAID: 'success', PAYING: 'warning', CREATED: 'info', EXPIRED: 'info', FAILED: 'danger' }
const statusLabelMap: Record<string, string> = { PAID: '已支付', PAYING: '支付中', CREATED: '待支付', EXPIRED: '已过期', FAILED: '失败' }
const channelTypeMap: Record<string, string> = { ALIPAY: 'primary', WECHAT_PAY: 'success', UNION_PAY: 'warning' }
const channelLabelMap: Record<string, string> = { ALIPAY: '支付宝', WECHAT_PAY: '微信支付', UNION_PAY: '银联' }

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
    orderStats.value = await getOrderStats()
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
  else loadOrders()
}

function syncMerchantIdToUrl() {
  const mid = queryForm.merchantId?.trim()
  if (mid) {
    router.replace({ path: '/admin/orders', query: { merchantId: mid } })
  } else if (route.query.merchantId) {
    router.replace({ path: '/admin/orders' })
  }
}

async function openDetail(row: Order) {
  currentOrder.value = row
  showDetail.value = true
}

watch(
  () => route.query.merchantId,
  (mid) => {
    const s = typeof mid === 'string' ? mid : Array.isArray(mid) ? mid[0] : ''
    queryForm.merchantId = s || undefined
    queryForm.page = 1
    loadOrders()
  },
  { immediate: true }
)

onMounted(() => {
  loadStats()
})
</script>

