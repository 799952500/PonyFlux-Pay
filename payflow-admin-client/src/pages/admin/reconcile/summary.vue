<template>
  <div class="page-table-shell">
    <div class="filter-bar">
      <el-form :inline="true" :model="queryForm" size="default" class="filter-bar__form">
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
        <el-form-item label="支付账号">
          <el-input v-model="queryForm.accountCode" placeholder="账户编码，可选" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item class="filter-bar__actions">
          <el-button type="primary" class="btn-primary" icon="Search" @click="load">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-loading="loading" class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <div class="content-card !p-4">
        <p class="text-xs text-slate-500 m-0">支付收款（本地成功）</p>
        <p class="text-xl font-semibold text-slate-800 m-1 tabular-nums">{{ formatMoneyFen(summary?.totalLocalAmountFen) }} 元</p>
      </div>
      <div class="content-card !p-4">
        <p class="text-xs text-slate-500 m-0">渠道账单（对账文件）</p>
        <p class="text-xl font-semibold text-slate-800 m-1 tabular-nums">{{ formatMoneyFen(summary?.totalChannelBillAmountFen) }} 元</p>
      </div>
      <div class="content-card !p-4">
        <p class="text-xs text-slate-500 m-0">金额差额（本地 − 账单）</p>
        <p
          class="text-xl font-semibold m-1 tabular-nums"
          :class="deltaClass(summary?.totalAmountDeltaFen)"
        >
          {{ formatMoneyFen(summary?.totalAmountDeltaFen) }} 元
        </p>
      </div>
      <div class="content-card !p-4">
        <p class="text-xs text-slate-500 m-0">待处理差异笔数</p>
        <p class="text-xl font-semibold text-amber-700 m-1 tabular-nums">{{ summary?.pendingDiffCount ?? 0 }}</p>
        <el-button type="primary" link class="!p-0 !h-auto text-sm" @click="openDetail">查看差额订单</el-button>
      </div>
    </div>

    <div class="content-card">
      <TableToolbar
        title="按支付账号汇总"
        :total="(summary?.byAccount ?? []).length"
        hint="与对账任务一致按收款账户维度统计；历史支付未写入 account_code 时归入「__NO_ACCOUNT__」。"
      />

      <el-table table-layout="auto" :data="summary?.byAccount ?? []" stripe size="small" class="data-table">
        <el-table-column prop="accountCode" label="支付账号" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-mono">{{ row.accountCode || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="channel" label="对账渠道" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="channelTagType(row.channel)">{{ channelLabel(row.channel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="本地笔数" prop="localSuccessCount" width="100" align="right">
          <template #default="{ row }">
            <span class="tabular-nums">{{ row.localSuccessCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="本地金额" width="120" align="right" class-name="col-amount">
          <template #default="{ row }">
            <span class="cell-amount">¥{{ formatMoneyFen(row.localSuccessAmountFen) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="账单笔数" prop="channelBillCount" width="100" align="right">
          <template #default="{ row }">
            <span class="tabular-nums">{{ row.channelBillCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="账单金额" width="120" align="right" class-name="col-amount">
          <template #default="{ row }">
            <span class="cell-amount">¥{{ formatMoneyFen(row.channelBillAmountFen) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="差额" width="120" align="right" class-name="col-amount">
          <template #default="{ row }">
            <span class="cell-amount" :class="deltaClass(row.amountDeltaFen)">¥{{ formatMoneyFen(row.amountDeltaFen) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="100" class-name="col-actions" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetailForAccount(row)">差额明细</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="drawerVisible" title="产生差额的订单（对账差异）" direction="rtl" size="720px">
      <el-form :inline="true" size="small" class="mb-3 px-4">
        <el-form-item label="处理状态">
          <el-select v-model="detailQuery.handleStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已处理" value="PROCESSED" />
            <el-option label="已忽略" value="IGNORED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" @click="loadAnomalies">筛选</el-button>
        </el-form-item>
      </el-form>
      <div class="px-4">
        <TableToolbar title="差额订单" :total="anomalyTotal" />
        <el-table table-layout="auto" v-loading="detailLoading" :data="anomalyList" stripe size="small" max-height="520" class="data-table">
          <el-table-column prop="diffType" label="类型" width="130">
            <template #default="{ row }">
              <el-tag size="small" type="warning" effect="plain">
                {{ labelOf(RECON_DIFF_LABEL, row.diffType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="localOrderId" label="订单号" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.localOrderId" class="cell-mono pf-link cursor-pointer" @click="goOrder(row.localOrderId)">
                #{{ row.localOrderId }}
              </span>
              <span v-else class="cell-empty">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="merchantId" label="商户" width="100" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="cell-mono">{{ row.merchantId || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="channelTradeNo" label="渠道单号" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="cell-mono">{{ row.channelTradeNo || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="渠道/本地金额" min-width="150" align="right" class-name="col-amount">
            <template #default="{ row }">
              <span class="cell-amount text-xs">
                ¥{{ formatMoneyFen(row.channelAmount) }} / ¥{{ formatMoneyFen(row.localAmount) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="accountCode" label="支付账号" width="120" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="cell-mono">{{ row.accountCode || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="handleStatus" label="处理" width="90">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ labelOf(RECON_HANDLE_LABEL, row.handleStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="账单日" prop="billDate" width="120" class-name="col-datetime">
            <template #default="{ row }">
              <span class="cell-datetime">{{ formatDate(row.billDate) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="80" class-name="col-actions" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.localOrderId"
                link
                type="primary"
                size="small"
                @click="goOrder(row.localOrderId)"
              >
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="detailQuery.page"
            v-model:page-size="detailQuery.pageSize"
            small
            :total="anomalyTotal"
            layout="total, prev, pager, next"
            @current-change="loadAnomalies"
          />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useOrderDetailOverlay } from '@/composables/useOrderDetailOverlay'
import { ElMessage } from 'element-plus'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import {
  formatDate,
  formatMoneyFen,
  channelLabel,
  channelTagType,
  labelOf,
  RECON_DIFF_LABEL,
  RECON_HANDLE_LABEL,
} from '@/utils/format'
import {
  getReconSummary,
  getReconAnomalies,
  type ReconSummaryData,
  type ReconAnomalyItem,
} from '@/api/admin'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

const { open: openOrderDetail } = useOrderDetailOverlay()
const loading = ref(false)
const summary = ref<ReconSummaryData | null>(null)

const drawerVisible = ref(false)
const detailLoading = ref(false)
const anomalyList = ref<ReconAnomalyItem[]>([])
const anomalyTotal = ref(0)
const detailChannel = ref('')
const detailAccountCode = ref('')

function yesterday() {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  return d.toISOString().slice(0, 10)
}

const queryForm = reactive({
  billDate: yesterday(),
  channel: '',
  accountCode: '',
})

const detailQuery = reactive({
  page: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  handleStatus: '',
})

function deltaClass(fen?: number | null) {
  if (fen == null) return ''
  if (fen === 0) return ''
  return 'cell-amount--danger'
}

function handleReset() {
  queryForm.billDate = yesterday()
  queryForm.channel = ''
  queryForm.accountCode = ''
  load()
}

async function load() {
  if (!queryForm.billDate) {
    ElMessage.warning('请选择账单日')
    return
  }
  loading.value = true
  try {
    summary.value = await getReconSummary({
      billDate: queryForm.billDate,
      channel: queryForm.channel || undefined,
      accountCode: queryForm.accountCode || undefined,
    })
  } catch (e: any) {
    ElMessage.error(e?.message || '加载对账汇总失败')
  } finally {
    loading.value = false
  }
}

function openDetail() {
  detailChannel.value = queryForm.channel || ''
  detailAccountCode.value = queryForm.accountCode || ''
  detailQuery.page = 1
  detailQuery.handleStatus = ''
  drawerVisible.value = true
  loadAnomalies()
}

function openDetailForAccount(row: { channel?: string; accountCode?: string }) {
  detailChannel.value = row.channel || ''
  detailAccountCode.value =
    row.accountCode && row.accountCode !== '__NO_ACCOUNT__' ? row.accountCode : ''
  detailQuery.page = 1
  detailQuery.handleStatus = ''
  drawerVisible.value = true
  loadAnomalies()
}

async function loadAnomalies() {
  if (!queryForm.billDate) return
  detailLoading.value = true
  try {
    const res = await getReconAnomalies({
      billDate: queryForm.billDate,
      channel: detailChannel.value || undefined,
      accountCode: detailAccountCode.value || undefined,
      handleStatus: detailQuery.handleStatus || undefined,
      page: detailQuery.page,
      size: detailQuery.pageSize,
    })
    anomalyList.value = res.list
    anomalyTotal.value = res.total
  } catch (e: any) {
    ElMessage.error(e?.message || '加载异常明细失败')
  } finally {
    detailLoading.value = false
  }
}

function goOrder(orderId: string) {
  openOrderDetail(orderId)
}

load()
</script>
