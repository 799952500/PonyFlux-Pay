<template>
  <div>
    <div class="filter-bar">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="账单日" required>
          <el-date-picker
            v-model="queryForm.billDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="queryForm.channel" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="支付宝" value="alipay" />
            <el-option label="微信" value="wxpay" />
          </el-select>
        </el-form-item>
        <el-form-item label="商户号">
          <el-input v-model="queryForm.merchantId" placeholder="可选" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="queryForm.orderKeyword" placeholder="模糊" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="视图">
          <el-checkbox v-model="queryForm.onlyAbnormal">仅异常（差异表）</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <p v-if="queryForm.onlyAbnormal" class="text-xs text-amber-700 mt-1">
        仅异常模式按差异记录分页；商户/订单筛选请取消勾选后使用全量支付视图。
      </p>
    </div>

    <div class="content-card">
      <el-table v-loading="loading" :data="list" stripe size="small" class="data-table">
        <el-table-column label="对账状态" prop="reconStatus" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.reconStatus)">{{ row.reconStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单号" prop="orderId" min-width="140" show-overflow-tooltip />
        <el-table-column label="商户号" prop="merchantId" width="120" show-overflow-tooltip />
        <el-table-column label="支付账号" prop="accountCode" width="120" show-overflow-tooltip />
        <el-table-column label="支付号" prop="paymentId" width="140" show-overflow-tooltip />
        <el-table-column label="渠道" prop="payChannel" width="100" />
        <el-table-column label="渠道流水号" prop="channelTransactionId" min-width="140" show-overflow-tooltip />
        <el-table-column label="本地金额" width="100" align="right">
          <template #default="{ row }">{{ fenYuan(row.localAmountFen) }}</template>
        </el-table-column>
        <el-table-column label="账单金额" width="100" align="right">
          <template #default="{ row }">{{ fenYuan(row.channelAmountFen) }}</template>
        </el-table-column>
        <el-table-column label="差异类型" prop="diffType" width="130" />
        <el-table-column label="建议动作" prop="suggestedAction" width="130" show-overflow-tooltip />
        <el-table-column label="处理状态" prop="handleStatus" width="100" />
        <el-table-column label="对账渠道/账户" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.reconChannel">{{ row.reconChannel }} / {{ row.accountCode }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.orderId"
              link
              type="primary"
              size="small"
              @click="goOrder(row.orderId)"
            >
              订单
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getReconOrderResults, type ReconOrderResultItem } from '@/api/admin'

const router = useRouter()
const loading = ref(false)
const list = ref<ReconOrderResultItem[]>([])
const total = ref(0)

function yesterday() {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  return d.toISOString().slice(0, 10)
}

const queryForm = reactive({
  billDate: yesterday(),
  channel: '',
  merchantId: '',
  orderKeyword: '',
  onlyAbnormal: false,
  page: 1,
  pageSize: 20,
})

function fenYuan(fen?: number | null) {
  if (fen == null) return '—'
  return (fen / 100).toFixed(2)
}

function statusType(s: string) {
  if (s === 'MATCHED') return 'success'
  if (s === 'ABNORMAL') return 'danger'
  if (s === 'NO_RECON') return 'info'
  return 'warning'
}

async function load() {
  if (!queryForm.billDate) {
    ElMessage.warning('请选择账单日')
    return
  }
  loading.value = true
  try {
    const res = await getReconOrderResults({
      billDate: queryForm.billDate,
      channel: queryForm.channel || undefined,
      merchantId: queryForm.merchantId || undefined,
      orderKeyword: queryForm.orderKeyword || undefined,
      onlyAbnormal: queryForm.onlyAbnormal,
      page: queryForm.page,
      size: queryForm.pageSize,
    })
    list.value = res.list
    total.value = res.total
  } catch (e: any) {
    ElMessage.error(e?.message || '加载对账结果失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.page = 1
  load()
}

function handleReset() {
  queryForm.billDate = yesterday()
  queryForm.channel = ''
  queryForm.merchantId = ''
  queryForm.orderKeyword = ''
  queryForm.onlyAbnormal = false
  queryForm.page = 1
  load()
}

function goOrder(orderId: string) {
  router.push({ path: `/admin/orders/${encodeURIComponent(orderId)}` })
}

load()
</script>
