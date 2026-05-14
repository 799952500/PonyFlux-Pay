<template>
  <div>
    <h2 class="text-lg font-semibold text-[#0F172A] mb-5">路由决策日志</h2>

    <div class="content-card mb-5">
      <el-form :inline="true" :model="filters" size="small">
        <el-form-item label="交易流水号">
          <el-input v-model="filters.tradeNo" placeholder="交易号" clearable />
        </el-form-item>
        <el-form-item label="商户ID">
          <el-input v-model="filters.merchantId" placeholder="商户ID" clearable />
        </el-form-item>
        <el-form-item label="选中渠道">
          <el-select v-model="filters.selectedChannel" placeholder="全部" clearable>
            <el-option label="微信支付" value="wxpay" />
            <el-option label="支付宝" value="alipay" />
            <el-option label="银联" value="unionpay" />
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
            style="width: 340px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogs">查询</el-button>
          <el-button @click="handleExport" :loading="exporting">导出 Excel</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <el-table :data="logs" v-loading="loading" size="small" class="data-table">
        <el-table-column label="交易流水号" prop="tradeNo" min-width="160" />
        <el-table-column label="商户ID" prop="merchantId" width="90" />
        <el-table-column label="可选渠道" min-width="160">
          <template #default="{ row }">
            <span class="text-xs text-[#64748B]">{{ row.availableChannels }}</span>
          </template>
        </el-table-column>
        <el-table-column label="选中渠道" prop="selectedChannel" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.selectedChannel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="选择原因" prop="selectionReason" min-width="180">
          <template #default="{ row }">
            <span class="text-xs">{{ row.selectionReason }}</span>
          </template>
        </el-table-column>
        <el-table-column label="耗时(ms)" prop="decisionCostMs" width="85" />
        <el-table-column label="降级次数" prop="fallbackCount" width="90" />
        <el-table-column label="时间" prop="createTime" width="170" />
      </el-table>

      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          small
          @change="loadLogs"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getRoutingLogs, exportRoutingLogs } from '@/api/admin'

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
.content-card {
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid rgba(99, 102, 241, 0.08);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  padding: 24px;
}

.data-table :deep(th) {
  background: linear-gradient(90deg, rgba(99,102,241,0.06) 0%, rgba(129,140,248,0.04) 100%) !important;
  color: #374151;
  font-weight: 600;
  font-size: 13px;
  border-bottom: 2px solid rgba(99, 102, 241, 0.1) !important;
}
</style>
