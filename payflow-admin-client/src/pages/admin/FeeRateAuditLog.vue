<template>
  <div>
    <h2 class="text-lg font-semibold text-[#0F172A] mb-5">费率变更审计日志</h2>

    <div class="content-card mb-5">
      <el-form :inline="true" :model="filters" size="small">
        <el-form-item label="商户ID">
          <el-input v-model="filters.merchantId" placeholder="商户ID" clearable @keyup.enter="loadLogs" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogs">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <el-table :data="logs" v-loading="loading" size="small" class="data-table">
        <el-table-column label="商户ID" prop="merchantId" width="100" />
        <el-table-column label="变更时间" prop="changeTime" width="170" />
        <el-table-column label="旧费率" width="100">
          <template #default="{ row }">
            <span v-if="row.oldRate !== null && row.oldRate !== undefined">{{ Number(row.oldRate).toFixed(4) }}</span>
            <span v-else class="text-[#94a3b8]">—</span>
          </template>
        </el-table-column>
        <el-table-column label="新费率" width="100">
          <template #default="{ row }">
            <span class="font-medium">{{ Number(row.newRate).toFixed(4) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="触发原因" prop="triggerReason" min-width="140" />
        <el-table-column label="操作人" prop="operator" width="100" />
      </el-table>

      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          small
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getFeeRateAuditLog } from '@/api/admin'

const loading = ref(false)
const logs = ref<any[]>([])

const filters = reactive({ merchantId: '' })
const pagination = reactive({ page: 1, size: 20, total: 0 })

async function loadLogs() {
  loading.value = true
  try {
    const result = await getFeeRateAuditLog({
      merchantId: filters.merchantId || undefined,
      page: pagination.page,
      size: pagination.size,
    })
    logs.value = result.list ?? []
    pagination.total = result.total ?? 0
  } catch {
    logs.value = []
    pagination.total = 0
    ElMessage.error('加载审计日志失败')
  } finally {
    loading.value = false
  }
}

loadLogs()
</script>

