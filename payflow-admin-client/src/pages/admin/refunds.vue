<template>
  <div class="page-table-shell">
    <div class="filter-bar">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="退款单号 / 订单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="申请中" value="PENDING" />
            <el-option label="审批通过" value="APPROVED" />
            <el-option label="已退款" value="COMPLETED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <TableToolbar title="退款列表" :total="total" />

      <el-table v-loading="loading" :data="refundList" stripe size="small" class="data-table">
        <el-table-column label="退款单号" prop="refundId" min-width="170">
          <template #default="{ row }"><span class="text-xs tabular-nums font-medium text-gray-700">{{ row.refundId }}</span></template>
        </el-table-column>
        <el-table-column label="订单号" prop="orderId" min-width="160">
          <template #default="{ row }"><span class="text-xs tabular-nums">{{ row.orderId }}</span></template>
        </el-table-column>
        <el-table-column label="商户订单号" prop="merchantOrderNo" min-width="150" />
        <el-table-column label="退款金额" prop="amount" width="120" align="right">
          <template #default="{ row }">
            <span class="font-semibold text-danger tabular-nums">¥{{ formatMoneyFen(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="退款原因" prop="reason" min-width="160">
          <template #default="{ row }"><span class="truncate block max-w-[160px]" :title="row.reason">{{ row.reason || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="tagTypeOf(REFUND_STATUS_TAG, row.status)">
              {{ labelOf(REFUND_STATUS_LABEL, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" prop="createdAt" width="172">
          <template #default="{ row }">
            <span class="text-xs text-slate-600 tabular-nums">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" size="small" @click="handleApprove(row)">审批通过</el-button>
              <el-button type="danger" size="small" plain @click="handleReject(row)">拒绝</el-button>
            </template>
            <span v-else class="text-gray-400 text-xs">—</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
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

const loading = ref(false)
const refundList = ref<RefundItem[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const queryForm = reactive({ page: 1, pageSize: 20, status: '', keyword: '' })

async function loadRefunds() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { ...queryForm }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const resp = await getRefunds(params as Parameters<typeof getRefunds>[0])
    refundList.value = resp.list
    total.value = resp.total
  } catch { ElMessage.error('加载退款列表失败') }
  finally { loading.value = false }
}

function handleSearch() { queryForm.page = 1; loadRefunds() }
function handleReset() { Object.assign(queryForm, { page: 1, pageSize: 20, status: '', keyword: '' }); dateRange.value = null; loadRefunds() }

async function handleApprove(row: RefundItem) {
  try {
    await ElMessageBox.confirm(`确认通过退款申请？退款金额 ¥${formatMoneyFen(row.amount)} 将原路退回。`, '审批确认', { confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'warning' })
    await approveRefund(row.refundId)
    ElMessage.success('退款申请已审批通过')
    loadRefunds()
  } catch (err: unknown) { if ((err as { message?: string })?.message !== 'cancel') ElMessage.error('操作失败，请重试') }
}

async function handleReject(row: RefundItem) {
  try {
    await ElMessageBox.prompt('请输入拒绝原因（选填）：', '拒绝退款申请', { confirmButtonText: '确认拒绝', cancelButtonText: '取消' })
    await rejectRefund(row.refundId)
    ElMessage.success('退款申请已拒绝')
    loadRefunds()
  } catch (err: unknown) { if ((err as { message?: string })?.message !== 'cancel') ElMessage.error('操作失败，请重试') }
}

onMounted(() => { loadRefunds() })
</script>

