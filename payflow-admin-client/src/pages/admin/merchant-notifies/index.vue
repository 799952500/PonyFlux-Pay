<template>
  <div class="page-table-shell">
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
        <el-form-item label="订单号">
          <el-input
            v-model="queryForm.orderId"
            placeholder="平台订单号"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="商户订单号">
          <el-input
            v-model="queryForm.merchantOrderNo"
            placeholder="商户订单号"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryForm.notifyType" placeholder="全部" clearable style="width: 136px">
            <el-option label="全部" value="" />
            <el-option label="支付通知" value="PAYMENT" />
            <el-option label="退款通知" value="REFUND" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.summaryStatus" placeholder="全部" clearable style="width: 136px">
            <el-option label="全部" value="" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="处理中" value="IN_PROGRESS" />
            <el-option label="待投递" value="PENDING" />
            <el-option label="未配置" value="NOT_CONFIGURED" />
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
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <TableToolbar title="回调记录" :total="total" />

      <el-table
        v-loading="loading"
        :data="list"
        stripe
        size="small"
        table-layout="auto"
        class="data-table"
        @row-click="openDetail"
      >
        <el-table-column label="订单号" prop="orderId" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span
              class="cell-mono pf-link cursor-pointer cell-ellipsis"
              @click.stop="openOrderDetail(row.orderId)"
            >#{{ row.orderId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商户订单号" prop="merchantOrderNo" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-ellipsis">{{ row.merchantOrderNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商户" prop="merchantId" min-width="100" show-overflow-tooltip />
        <el-table-column label="类型" prop="notifyType" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" class="table-tag-compact" :type="tagTypeOf(MERCHANT_NOTIFY_TYPE_TAG, row.notifyType)">
              {{ labelOf(MERCHANT_NOTIFY_TYPE_LABEL, row.notifyType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="汇总状态" prop="summaryStatus" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" class="table-tag-compact" :type="tagTypeOf(MERCHANT_NOTIFY_SUMMARY_TAG, row.summaryStatus)">
              {{ labelOf(MERCHANT_NOTIFY_SUMMARY_LABEL, row.summaryStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" prop="orderStatus" width="96" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.orderStatus" size="small" class="table-tag-compact" :type="tagTypeOf(ORDER_STATUS_TAG, row.orderStatus)">
              {{ labelOf(ORDER_STATUS_LABEL, row.orderStatus) }}
            </el-tag>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>
        <el-table-column label="次数" prop="attemptCount" width="72" align="center">
          <template #default="{ row }">
            <span class="tabular-nums">{{ row.attemptCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最近尝试" prop="lastAttemptAt" min-width="168" class-name="col-datetime" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-datetime">{{ formatDateTime(row.lastAttemptAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="80" class-name="col-actions" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" size="small" @click.stop="openDetail(row)">详情</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <AdminPagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        @size-change="loadList"
        @current-change="loadList"
      />
    </div>

    <DetailDrawer ref="detailRef" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import DetailDrawer from '@/components/merchant-notifies/DetailDrawer.vue'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import { listMerchantNotifies, type MerchantNotifyListItem } from '@/api/merchantNotify'
import { useMerchantScope } from '@/composables/useMerchantScope'
import { useOrderDetailOverlay } from '@/composables/useOrderDetailOverlay'
import {
  formatDateTime,
  labelOf,
  MERCHANT_NOTIFY_SUMMARY_LABEL,
  MERCHANT_NOTIFY_SUMMARY_TAG,
  MERCHANT_NOTIFY_TYPE_LABEL,
  MERCHANT_NOTIFY_TYPE_TAG,
  ORDER_STATUS_LABEL,
  ORDER_STATUS_TAG,
  tagTypeOf,
} from '@/utils/format'

const route = useRoute()
const { open: openOrderDetail } = useOrderDetailOverlay()
const { merchantFilterLocked, authorizedMerchantIds, defaultMerchantId } = useMerchantScope()

const loading = ref(false)
const list = ref<MerchantNotifyListItem[]>([])
const total = ref(0)
const dateRange = ref<string[] | null>(null)
const detailRef = ref<InstanceType<typeof DetailDrawer> | null>(null)

const queryForm = reactive({
  merchantId: '',
  orderId: '',
  merchantOrderNo: '',
  notifyType: '',
  summaryStatus: '',
  page: 1,
  pageSize: DEFAULT_PAGE_SIZE,
})

function buildParams() {
  const params: Record<string, string | number> = {
    page: queryForm.page,
    size: queryForm.pageSize,
  }
  const mid = queryForm.merchantId || (merchantFilterLocked.value ? defaultMerchantId.value : '')
  if (mid) params.merchantId = mid
  if (queryForm.orderId) params.orderId = queryForm.orderId
  if (queryForm.merchantOrderNo) params.merchantOrderNo = queryForm.merchantOrderNo
  if (queryForm.notifyType) params.notifyType = queryForm.notifyType
  if (queryForm.summaryStatus) params.summaryStatus = queryForm.summaryStatus
  if (dateRange.value?.length === 2) {
    params.startTime = `${dateRange.value[0]} 00:00:00`
    params.endTime = `${dateRange.value[1]} 23:59:59`
  }
  return params
}

async function loadList() {
  loading.value = true
  try {
    const res = await listMerchantNotifies(buildParams())
    list.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.page = 1
  loadList()
}

function handleReset() {
  queryForm.merchantId = merchantFilterLocked.value ? (defaultMerchantId.value ?? '') : ''
  queryForm.orderId = ''
  queryForm.merchantOrderNo = ''
  queryForm.notifyType = ''
  queryForm.summaryStatus = ''
  dateRange.value = null
  handleSearch()
}

function openDetail(row: MerchantNotifyListItem) {
  detailRef.value?.openByNotifyId(row.notifyId)
}

onMounted(() => {
  if (merchantFilterLocked.value && defaultMerchantId.value) {
    queryForm.merchantId = defaultMerchantId.value
  }
  const qOrder = route.query.orderId
  if (typeof qOrder === 'string' && qOrder) {
    queryForm.orderId = qOrder
  }
  loadList()
  if (typeof qOrder === 'string' && qOrder) {
    detailRef.value?.openByOrderId(qOrder)
  }
})
</script>
