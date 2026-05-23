<template>
  <div class="page-table-shell">
    <div class="filter-bar">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="分类">
          <el-select v-model="query.classification" placeholder="全部" clearable style="width: 150px">
            <el-option label="商户级" value="MERCHANT" />
            <el-option label="全局级" value="GLOBAL" />
            <el-option label="系统审计" value="SYSTEM_AUDIT" />
            <el-option label="待人工确认" value="MANUAL_REVIEW" />
          </el-select>
        </el-form-item>
        <el-form-item label="风险">
          <el-select v-model="query.riskLevel" placeholder="全部" clearable style="width: 120px">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="整改状态">
          <el-select v-model="query.remediationStatus" placeholder="全部" clearable style="width: 160px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="IN_PROGRESS" />
            <el-option label="已完成" value="DONE" />
            <el-option label="已豁免" value="EXEMPTED" />
            <el-option label="需人工确认" value="NEEDS_MANUAL_REVIEW" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isPlatformAdmin" label="商户号">
          <el-input v-model="query.merchantId" placeholder="可选" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" @click="handleReset">重置</el-button>
          <el-button v-if="isPlatformAdmin" type="warning" :loading="scanning" @click="handleScan">执行扫描</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <TableToolbar title="数据隔离检查项" :total="total" />
      <el-table table-layout="auto" v-loading="loading" :data="list" stripe size="small" class="data-table">
        <el-table-column label="检查项" prop="checkId" min-width="140" />
        <el-table-column label="目标" prop="targetName" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" prop="targetType" width="110" />
        <el-table-column label="分类" prop="classification" width="120" />
        <el-table-column label="风险" prop="riskLevel" width="80" />
        <el-table-column label="整改状态" prop="remediationStatus" width="130" />
        <el-table-column label="商户" prop="merchantId" width="110" />
        <el-table-column label="最近扫描" prop="lastScannedAt" min-width="168" class-name="col-datetime" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-xs tabular-nums">{{ formatDateTime(row.lastScannedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isPlatformAdmin" label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-select
              :model-value="row.remediationStatus"
              size="small"
              style="width: 140px"
              @change="(v: string) => updateStatus(row, v)"
            >
              <el-option label="待处理" value="PENDING" />
              <el-option label="处理中" value="IN_PROGRESS" />
              <el-option label="已完成" value="DONE" />
              <el-option label="已豁免" value="EXEMPTED" />
              <el-option label="需人工确认" value="NEEDS_MANUAL_REVIEW" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
      <AdminPagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        @size-change="load"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import { getDataIsolationChecks, scanDataIsolation, updateDataIsolationRemediation } from '@/api/admin'
import { useMerchantScope } from '@/composables/useMerchantScope'
import type { DataIsolationCheckItem } from '@/types'
import { formatDateTime } from '@/utils/format'

const { isPlatformAdmin, applyDefaultMerchantFilter } = useMerchantScope()
const loading = ref(false)
const scanning = ref(false)
const list = ref<DataIsolationCheckItem[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: DEFAULT_PAGE_SIZE,
  classification: '',
  riskLevel: '',
  remediationStatus: '',
  merchantId: '',
})

async function load() {
  loading.value = true
  try {
    const res = await getDataIsolationChecks({
      page: query.page,
      size: query.size,
      classification: query.classification || undefined,
      riskLevel: query.riskLevel || undefined,
      remediationStatus: query.remediationStatus || undefined,
      merchantId: query.merchantId || undefined,
    })
    list.value = res.list
    total.value = res.total
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleScan() {
  scanning.value = true
  try {
    const res = await scanDataIsolation()
    ElMessage.success(`扫描完成，更新 ${res.updatedCount} 项`)
    await load()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '扫描失败')
  } finally {
    scanning.value = false
  }
}

async function updateStatus(row: DataIsolationCheckItem, status: string) {
  try {
    await updateDataIsolationRemediation(row.checkId, { remediationStatus: status })
    ElMessage.success('已更新')
    await load()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '更新失败')
  }
}

function handleSearch() {
  query.page = 1
  load()
}

function handleReset() {
  Object.assign(query, {
    page: 1,
    size: DEFAULT_PAGE_SIZE,
    classification: '',
    riskLevel: '',
    remediationStatus: '',
    merchantId: '',
  })
  applyDefaultMerchantFilter(query)
  load()
}

onMounted(() => {
  applyDefaultMerchantFilter(query)
  load()
})
</script>
