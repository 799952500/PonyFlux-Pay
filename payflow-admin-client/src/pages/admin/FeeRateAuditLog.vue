<template>
  <div class="page-table-shell">
    <div class="filter-bar">
      <el-form :inline="true" :model="filters" size="default">
        <el-form-item label="商户ID">
          <el-input
            v-model="filters.merchantId"
            placeholder="输入商户ID"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <div class="table-toolbar">
        <div>
          <div class="table-toolbar__title">变更记录</div>
          <div class="table-toolbar__hint">共 {{ pagination.total }} 条费率变更审计</div>
        </div>
      </div>

      <el-table table-layout="auto" :data="logs" v-loading="loading" stripe size="small" class="data-table">
        <el-table-column label="商户ID" prop="merchantId" width="96" align="center">
          <template #default="{ row }">
            <span class="cell-mono font-medium">{{ row.merchantId ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="变更时间" prop="changeTime" min-width="168" class-name="col-datetime" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-xs text-slate-600 tabular-nums">{{ formatDateTime(row.changeTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="费率变更" min-width="180">
          <template #default="{ row }">
            <div class="rate-change">
              <span class="rate-change__old">{{ formatRatePercent(row.oldRate) }}</span>
              <span class="rate-change__arrow">→</span>
              <span class="rate-change__new">{{ formatRatePercent(row.newRate) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="触发原因" prop="triggerReason" min-width="140">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">
              {{ labelOf(FEE_TRIGGER_LABEL, row.triggerReason) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作人" prop="operator" width="120">
          <template #default="{ row }">
            <span class="text-sm text-slate-700">{{ row.operator || '—' }}</span>
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
import { getFeeRateAuditLog } from '@/api/admin'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import { FEE_TRIGGER_LABEL, formatDateTime, formatRatePercent, labelOf } from '@/utils/format'

const loading = ref(false)
const logs = ref<any[]>([])

const filters = reactive({ merchantId: '' })
const pagination = reactive({ page: 1, size: DEFAULT_PAGE_SIZE, total: 0 })

function handleSearch() {
  pagination.page = 1
  loadLogs()
}

function handleReset() {
  filters.merchantId = ''
  pagination.page = 1
  loadLogs()
}

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
