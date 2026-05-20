<template>
  <div class="page-table-shell">
    <div class="filter-bar">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="操作者">
          <el-input v-model="queryForm.username" placeholder="用户名模糊" clearable style="width: 140px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryForm.action" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="登录" value="LOGIN" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
            <el-option label="PATCH" value="PATCH" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <TableToolbar title="操作日志" :total="total" />

      <el-table v-loading="loading" :data="list" stripe size="small" class="data-table">
        <el-table-column label="时间" prop="createdAt" width="172">
          <template #default="{ row }">
            <span class="text-xs text-slate-600 tabular-nums">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作者" prop="username" width="120">
          <template #default="{ row }">
            <span class="text-sm">{{ row.username || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" prop="action" width="88" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="tagTypeOf(AUDIT_ACTION_TAG, row.action)" effect="plain">
              {{ labelOf(AUDIT_ACTION_LABEL, row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="请求路径" prop="resourcePath" min-width="220">
          <template #default="{ row }">
            <span class="cell-mono text-xs break-all">{{ row.resourcePath }}</span>
          </template>
        </el-table-column>
        <el-table-column label="摘要" prop="detail" min-width="260">
          <template #default="{ row }">
            <span class="text-xs text-slate-600 break-words">{{ row.detail }}</span>
          </template>
        </el-table-column>
        <el-table-column label="IP" prop="clientIp" width="130">
          <template #default="{ row }">
            <span class="cell-mono tabular-nums">{{ row.clientIp || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <AdminPagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs } from '@/api/admin'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import {
  AUDIT_ACTION_LABEL,
  AUDIT_ACTION_TAG,
  formatDateTime,
  labelOf,
  tagTypeOf,
} from '@/utils/format'
import type { AuditLogItem } from '@/types'

const loading = ref(false)
const list = ref<AuditLogItem[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const queryForm = reactive({
  page: 1,
  pageSize: 20,
  username: '',
  action: '',
})

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { ...queryForm }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const resp = await getAuditLogs(params as Parameters<typeof getAuditLogs>[0])
    list.value = resp.list
    total.value = resp.total
  } catch {
    ElMessage.error('加载操作日志失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.page = 1
  loadData()
}

function handleReset() {
  Object.assign(queryForm, { page: 1, pageSize: 20, username: '', action: '' })
  dateRange.value = null
  loadData()
}

onMounted(() => loadData())
</script>
