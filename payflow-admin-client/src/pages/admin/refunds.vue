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
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="退款单号 / 订单号"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 136px">
            <el-option label="全部" value="" />
            <el-option label="申请中" value="PENDING" />
            <el-option label="审批通过" value="APPROVED" />
            <el-option label="已退款" value="COMPLETED" />
            <el-option label="已拒绝" value="REJECTED" />
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
      <TableToolbar title="退款列表" :total="total" />

      <el-table
        v-loading="loading"
        :data="refundList"
        stripe
        size="small"
        table-layout="auto"
        class="data-table"
        @row-click="openRefundDetail"
      >
        <el-table-column label="退款单号" prop="refundId" min-width="168" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-mono cell-ellipsis">#{{ row.refundId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="订单号" prop="orderId" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span
              class="cell-mono pf-link cursor-pointer cell-ellipsis"
              @click.stop="openOrderFromRefund(row)"
            >#{{ row.orderId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商户订单号" prop="merchantOrderNo" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-ellipsis">{{ row.merchantOrderNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="退款金额" prop="amount" width="110" align="right" class-name="col-amount">
          <template #default="{ row }">
            <span class="cell-amount cell-amount--danger">¥{{ formatMoneyFen(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="退款原因" prop="reason" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-ellipsis">{{ row.reason || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" class="table-tag-compact" :type="tagTypeOf(REFUND_STATUS_TAG, row.status)">
              {{ labelOf(REFUND_STATUS_LABEL, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" prop="createdAt" min-width="168" class-name="col-datetime" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-datetime">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="168" fixed="right" class-name="col-actions">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" size="small" @click.stop="openRefundDetail(row)">详情</el-button>
              <template v-if="row.status === 'PENDING'">
                <el-button v-permission="'refund:approve'" link type="success" size="small" @click.stop="handleApprove(row)">审批通过</el-button>
                <el-button v-permission="'refund:reject'" link type="danger" size="small" @click.stop="handleReject(row)">拒绝</el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <AdminPagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        @size-change="loadRefunds"
        @current-change="loadRefunds"
      />
    </div>

    <el-dialog v-model="detailVisible" title="退款详情" width="520px" destroy-on-close>
      <el-descriptions v-if="currentRefund" :column="1" border class="detail-descriptions">
        <el-descriptions-item label="退款单号">{{ currentRefund.refundId }}</el-descriptions-item>
        <el-descriptions-item label="订单号">
          <span class="pf-link cursor-pointer" @click="openOrderFromRefund(currentRefund)">#{{ currentRefund.orderId }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="商户订单号">{{ currentRefund.merchantOrderNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="退款金额">
          <span class="cell-amount cell-amount--danger">¥{{ formatMoneyFen(currentRefund.amount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="退款原因">{{ currentRefund.reason || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" class="table-tag-compact" :type="tagTypeOf(REFUND_STATUS_TAG, currentRefund.status)">
            {{ labelOf(REFUND_STATUS_LABEL, currentRefund.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatDateTime(currentRefund.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useOrderDetailOverlay } from '@/composables/useOrderDetailOverlay'
import { useMerchantScope } from '@/composables/useMerchantScope'
import { ElMessage, ElMessageBox } from 'element-plus'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import { getRefunds, approveRefund, rejectRefund } from '@/api/admin'
import type { RefundItem } from '@/types'
import {
  formatDateTime,
  formatMoneyFen,
  labelOf,
  REFUND_STATUS_LABEL,
  REFUND_STATUS_TAG,
  tagTypeOf,
} from '@/utils/format'

const route = useRoute()
const { open: openOrderDetail } = useOrderDetailOverlay()
const { merchantFilterLocked, authorizedMerchantIds, defaultMerchantId } = useMerchantScope()

const loading = ref(false)
const refundList = ref<RefundItem[]>([])
const detailVisible = ref(false)
const currentRefund = ref<RefundItem | null>(null)
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const queryForm = reactive({
  page: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  status: '',
  keyword: '',
  merchantId: '',
})

async function loadRefunds() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: queryForm.page,
      pageSize: queryForm.pageSize,
      status: queryForm.status || undefined,
      keyword: queryForm.keyword || undefined,
    }
    const mid = queryForm.merchantId || (merchantFilterLocked.value ? defaultMerchantId.value : '')
    if (mid) params.merchantId = mid
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const resp = await getRefunds(params as Parameters<typeof getRefunds>[0])
    refundList.value = resp.list ?? []
    total.value = resp.total ?? 0
  } catch {
    ElMessage.error('加载退款列表失败')
  } finally {
    loading.value = false
  }
}

function openRefundDetail(row: RefundItem) {
  currentRefund.value = row
  detailVisible.value = true
}

function openOrderFromRefund(row: RefundItem) {
  if (row.orderId) openOrderDetail(row.orderId)
}

function handleSearch() {
  queryForm.page = 1
  loadRefunds()
}

function handleReset() {
  queryForm.page = 1
  queryForm.pageSize = DEFAULT_PAGE_SIZE
  queryForm.status = ''
  queryForm.keyword = ''
  queryForm.merchantId = merchantFilterLocked.value ? (defaultMerchantId.value ?? '') : ''
  dateRange.value = null
  loadRefunds()
}

async function handleApprove(row: RefundItem) {
  try {
    await ElMessageBox.confirm(
      `确认通过退款申请？退款金额 ¥${formatMoneyFen(row.amount)} 将原路退回。`,
      '审批确认',
      { confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'warning' },
    )
    await approveRefund(row.refundId)
    ElMessage.success('退款申请已审批通过')
    loadRefunds()
  } catch (err: unknown) {
    if ((err as { message?: string })?.message !== 'cancel') ElMessage.error('操作失败，请重试')
  }
}

async function handleReject(row: RefundItem) {
  try {
    await ElMessageBox.prompt('请输入拒绝原因（选填）：', '拒绝退款申请', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
    })
    await rejectRefund(row.refundId)
    ElMessage.success('退款申请已拒绝')
    loadRefunds()
  } catch (err: unknown) {
    if ((err as { message?: string })?.message !== 'cancel') ElMessage.error('操作失败，请重试')
  }
}

onMounted(() => {
  if (merchantFilterLocked.value && defaultMerchantId.value) {
    queryForm.merchantId = defaultMerchantId.value
  }
  const kw = route.query.keyword
  if (typeof kw === 'string' && kw.trim()) {
    queryForm.keyword = kw.trim()
  }
  loadRefunds()
})
</script>
