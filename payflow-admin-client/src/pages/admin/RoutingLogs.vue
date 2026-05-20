<template>
  <div class="page-table-shell">
    <div class="filter-bar">
      <el-form :inline="true" :model="filters" size="default">
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
        <el-form-item>
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" :loading="exporting" @click="handleExport">导出 Excel</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <div class="table-toolbar">
        <div>
          <div class="table-toolbar__title">决策记录</div>
          <div class="table-toolbar__hint">共 {{ pagination.total }} 条路由决策</div>
        </div>
      </div>

      <el-table :data="logs" v-loading="loading" stripe size="small" class="data-table">
        <el-table-column label="交易流水号" prop="tradeNo" min-width="168">
          <template #default="{ row }">
            <span class="cell-mono text-[#047857] font-medium">{{ row.tradeNo }}</span>
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
        <el-table-column label="决策时间" prop="createTime" width="172">
          <template #default="{ row }">
            <span class="text-xs text-slate-600 tabular-nums">{{ formatDateTime(row.createTime) }}</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getRoutingLogs, exportRoutingLogs } from '@/api/admin'
import AdminPagination from '@/components/admin/AdminPagination.vue'
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

const filters = reactive({
  tradeNo: '',
  merchantId: '',
  selectedChannel: '',
  timeRange: null as [string, string] | null,
})

const pagination = reactive({ page: 1, size: 20, total: 0 })

function parseChannels(raw: unknown) {
  return parseChannelOptions(raw)
}

function latencyClass(ms?: number) {
  const value = Number(ms)
  if (Number.isNaN(value)) return 'cell-empty'
  if (value >= 40) return 'text-amber-600 font-medium tabular-nums text-xs'
  return 'text-slate-600 tabular-nums text-xs'
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
