<template>
  <div>
    <div class="bg-white rounded-xl p-5 card-shadow mb-4">
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

    <div class="bg-white rounded-xl card-shadow">
      <el-table v-loading="loading" :data="refundList" stripe size="small">
        <el-table-column label="退款单号" prop="refundId" min-width="170">
          <template #default="{ row }"><span class="text-xs tabular-nums font-medium text-gray-700">{{ row.refundId }}</span></template>
        </el-table-column>
        <el-table-column label="订单号" prop="orderId" min-width="160">
          <template #default="{ row }"><span class="text-xs tabular-nums">{{ row.orderId }}</span></template>
        </el-table-column>
        <el-table-column label="商户订单号" prop="merchantOrderNo" min-width="150" />
        <el-table-column label="退款金额" prop="amount" width="120" align="right">
          <template #default="{ row }"><span class="font-semibold text-danger">¥{{ (row.amount / 100).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="退款原因" prop="reason" min-width="160">
          <template #default="{ row }"><span class="truncate block max-w-[160px]" :title="row.reason">{{ row.reason || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTypeMap[row.status]">{{ statusLabelMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" prop="createdAt" width="170" />
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

      <div class="flex justify-end p-4">
        <el-pagination v-model:current-page="queryForm.page" v-model:page-size="queryForm.pageSize" :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" @size-change="loadRefunds" @current-change="loadRefunds" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRefunds, approveRefund, rejectRefund } from '@/api/admin'
import type { RefundItem } from '@/types'

const loading = ref(false)
const refundList = ref<RefundItem[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const queryForm = reactive({ page: 1, pageSize: 20, status: '', keyword: '' })

const statusTypeMap: Record<string, string> = { PENDING: 'warning', APPROVED: 'primary', COMPLETED: 'success', REJECTED: 'danger' }
const statusLabelMap: Record<string, string> = { PENDING: '申请中', APPROVED: '审批通过', COMPLETED: '已退款', REJECTED: '已拒绝' }

async function loadRefunds() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { ...queryForm }
    if (dateRange.value) params.dateRange = dateRange.value
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
    await ElMessageBox.confirm(`确认通过退款申请？退款金额 ¥${(row.amount / 100).toFixed(2)} 将原路退回。`, '审批确认', { confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'warning' })
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

<style scoped>
/* 按钮样式（与 orders/index.vue 保持一致） */
.btn-primary {
  background: linear-gradient(135deg, #065f46 0%, #0d9488 100%);
  border: none;
  color: white;
  border-radius: 10px;
  padding: 10px 20px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.35);
}

.btn-outline {
  background: transparent;
  border: 1.5px solid #E2E8F0;
  color: #374151;
  border-radius: 10px;
  padding: 10px 20px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-outline:hover {
  border-color: #047857;
  color: #047857;
}
</style>
