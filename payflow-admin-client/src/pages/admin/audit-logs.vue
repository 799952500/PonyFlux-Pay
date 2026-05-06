<template>
  <div>
    <div class="bg-white rounded-xl p-5 card-shadow mb-4">
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

    <div class="bg-white rounded-xl card-shadow">
      <el-table v-loading="loading" :data="list" stripe size="small">
        <el-table-column label="时间" prop="createdAt" width="170" />
        <el-table-column label="操作者" prop="username" width="120">
          <template #default="{ row }">
            <span class="text-xs">{{ row.username || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" prop="action" width="88" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="actionTagType(row.action)">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="请求路径" prop="resourcePath" min-width="220">
          <template #default="{ row }">
            <span class="text-xs font-mono text-slate-700 break-all">{{ row.resourcePath }}</span>
          </template>
        </el-table-column>
        <el-table-column label="摘要" prop="detail" min-width="260">
          <template #default="{ row }">
            <span class="text-xs text-slate-600 break-words">{{ row.detail }}</span>
          </template>
        </el-table-column>
        <el-table-column label="IP" prop="clientIp" width="130">
          <template #default="{ row }">
            <span class="text-xs tabular-nums">{{ row.clientIp || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex justify-end p-4">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs } from '@/api/admin'
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

function actionTagType(action: string): 'success' | 'warning' | 'danger' | 'info' {
  if (action === 'LOGIN') return 'success'
  if (action === 'DELETE') return 'danger'
  if (action === 'POST') return 'info'
  return 'warning'
}

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

<style scoped>
.card-shadow {
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(99, 102, 241, 0.08);
}

.btn-primary {
  background: linear-gradient(135deg, #065f46 0%, #0d9488 100%);
  border: none;
  color: white;
  border-radius: 10px;
  padding: 10px 20px;
  font-weight: 600;
}

.btn-outline {
  background: transparent;
  border: 1.5px solid #e2e8f0;
  color: #374151;
  border-radius: 10px;
  padding: 10px 20px;
  font-weight: 500;
}
</style>
