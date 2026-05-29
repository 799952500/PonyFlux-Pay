<template>
  <div class="page-table-shell">
    <div class="filter-bar">
      <el-form :inline="true" :model="filters" size="default" class="filter-bar__form">
        <el-form-item label="交易流水号">
          <el-input v-model="filters.tradeNo" placeholder="输入交易号" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="商户ID">
          <el-input v-model="filters.merchantId" placeholder="输入商户ID" clearable style="width: 140px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="选中渠道">
          <el-select v-model="filters.selectedChannel" placeholder="全部" clearable style="width: 140px">
            <el-option label="微信支付" value="WECHAT_PAY" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银联" value="UNION_PAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item class="filter-bar__actions">
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" :loading="exporting" @click="handleExport">导出 Excel</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <TableToolbar title="决策记录" :total="pagination.total" hint="路由决策审计" />

      <el-table
        table-layout="auto"
        :data="logs"
        v-loading="loading"
        stripe
        size="small"
        class="data-table"
        @row-click="openLogDetail"
      >
        <el-table-column label="交易流水号" prop="tradeNo" min-width="168">
          <template #default="{ row }">
            <span class="cell-mono pf-link cursor-pointer">{{ row.tradeNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商户ID" prop="merchantId" width="96" align="center">
          <template #default="{ row }">
            <span class="cell-mono">{{ row.merchantId ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="可选渠道" min-width="220">
          <template #default="{ row }">
            <div v-if="parseChannels(row.availableChannels).length" class="channel-tag-list">
              <el-tag
                v-for="item in parseChannels(row.availableChannels)"
                :key="item.code"
                size="small"
                :type="item.available === false ? 'info' : channelTagType(item.code)"
                :effect="item.available === false ? 'plain' : 'light'"
              >
                {{ channelLabel(item.code) }}
                <span v-if="item.rate != null" class="opacity-75"> · {{ formatRatePercent(item.rate) }}</span>
              </el-tag>
            </div>
            <span v-else class="cell-empty">—</span>
          </template>
        </el-table-column>
        <el-table-column label="选中渠道" prop="selectedChannel" width="108" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="channelTagType(row.selectedChannel)" effect="dark">
              {{ channelLabel(row.selectedChannel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="选择原因" prop="selectionReason" min-width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">
              {{ labelOf(ROUTING_REASON_LABEL, row.selectionReason) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" prop="decisionCostMs" width="88" align="right">
          <template #default="{ row }">
            <span :class="latencyClass(row.decisionCostMs)">{{ row.decisionCostMs }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="降级" prop="fallbackCount" width="72" align="center">
          <template #default="{ row }">
            <el-tag v-if="Number(row.fallbackCount) > 0" size="small" type="warning">{{ row.fallbackCount }}</el-tag>
            <span v-else class="cell-empty">0</span>
          </template>
        </el-table-column>
        <el-table-column label="决策时间" prop="createTime" min-width="168" class-name="col-datetime" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-datetime">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="72" class-name="col-actions" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openLogDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <AdminPagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        @size-change="loadLogs"
        @current-change="loadLogs"
      />
    </div>

    <el-drawer v-model="detailVisible" title="路由决策详情" direction="rtl" size="480px" destroy-on-close>
      <el-descriptions v-if="currentLog" :column="1" border class="detail-descriptions">
        <el-descriptions-item label="交易流水号">{{ currentLog.tradeNo }}</el-descriptions-item>
        <el-descriptions-item label="商户 ID">{{ currentLog.merchantId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="选中渠道">
          <el-tag size="small" :type="channelTagType(String(currentLog.selectedChannel ?? ''))">
            {{ channelLabel(String(currentLog.selectedChannel ?? '')) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="选择原因">
          {{ labelOf(ROUTING_REASON_LABEL, String(currentLog.selectionReason ?? '')) }}
        </el-descriptions-item>
        <el-descriptions-item label="决策耗时">{{ currentLog.decisionCostMs }} ms</el-descriptions-item>
        <el-descriptions-item label="降级次数">{{ currentLog.fallbackCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="决策时间">{{ formatDateTime(String(currentLog.createTime ?? '')) }}</el-descriptions-item>
        <el-descriptions-item label="可选渠道">
          <pre class="routing-log-json">{{ formatJson(currentLog.availableChannels) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getRoutingLogs, exportRoutingLogs } from '@/api/admin'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import {
  channelLabel,
  channelTagType,
  formatDateTime,
  formatRatePercent,
  labelOf,
  parseChannelOptions,
  ROUTING_REASON_LABEL,
} from '@/utils/format'

const loading = ref(false)
const exporting = ref(false)
const logs = ref<any[]>([])
const detailVisible = ref(false)
const currentLog = ref<Record<string, unknown> | null>(null)

const filters = reactive({
  tradeNo: '',
  merchantId: '',
  selectedChannel: '',
  timeRange: null as [string, string] | null,
})

const pagination = reactive({ page: 1, size: DEFAULT_PAGE_SIZE, total: 0 })

function parseChannels(raw: unknown) {
  return parseChannelOptions(raw)
}

function latencyClass(ms?: number) {
  const value = Number(ms)
  if (Number.isNaN(value)) return 'cell-empty'
  if (value >= 40) return 'text-amber-600 font-medium tabular-nums text-xs'
  return 'text-slate-600 tabular-nums text-xs'
}

function formatJson(raw: unknown) {
  if (raw == null || raw === '') return '—'
  if (typeof raw === 'string') {
    try {
      return JSON.stringify(JSON.parse(raw), null, 2)
    } catch {
      return raw
    }
  }
  try {
    return JSON.stringify(raw, null, 2)
  } catch {
    return String(raw)
  }
}

function openLogDetail(row: Record<string, unknown>) {
  currentLog.value = row
  detailVisible.value = true
}

function handleSearch() {
  pagination.page = 1
  loadLogs()
}

async function loadLogs() {
  loading.value = true
  try {
    const result = await getRoutingLogs({
      page: pagination.page,
      size: pagination.size,
      tradeNo: filters.tradeNo || undefined,
      merchantId: filters.merchantId || undefined,
      selectedChannel: filters.selectedChannel || undefined,
      startTime: filters.timeRange?.[0],
      endTime: filters.timeRange?.[1],
    })
    logs.value = result.list
    pagination.total = result.total
  } catch {
    ElMessage.error('加载路由日志失败')
  } finally {
    loading.value = false
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const data = await exportRoutingLogs({
      startTime: filters.timeRange?.[0],
      endTime: filters.timeRange?.[1],
    })
    if (data && data.length > 0) {
      ElMessage.success(`导出成功，共 ${data.length} 条记录`)
    } else {
      ElMessage.info('无数据可导出')
    }
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

loadLogs()
</script>

<style scoped>
.routing-log-json {
  margin: 0;
  max-height: 200px;
  overflow: auto;
  font-size: 11px;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
