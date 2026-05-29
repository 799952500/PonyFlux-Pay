<template>
  <div class="page-table-shell">
    <div class="filter-bar">
      <el-form :inline="true" :model="queryForm" size="default" class="filter-bar__form">
        <el-form-item label="账单日">
          <el-date-picker
            v-model="queryForm.billDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="全部"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="差异类型">
          <el-select v-model="queryForm.diffType" placeholder="全部" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option
              v-for="(label, key) in RECON_DIFF_LABEL"
              :key="key"
              :label="label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="账龄">
          <el-select v-model="queryForm.ageBucket" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option
              v-for="key in RECON_AGE_BUCKET_ORDER"
              :key="key"
              :label="RECON_AGE_BUCKET_LABEL[key]"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="工单状态">
          <el-select v-model="queryForm.workflowStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option
              v-for="(label, key) in RECON_WORKFLOW_STATUS_LABEL"
              :key="key"
              :label="label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="筛选">
          <el-checkbox v-model="queryForm.onlyMine">只看我负责</el-checkbox>
          <el-checkbox v-model="queryForm.onlyUnassigned" class="ml-3">未指派</el-checkbox>
          <el-checkbox v-model="queryForm.onlyOverdue" class="ml-3">仅超时</el-checkbox>
        </el-form-item>
        <el-form-item class="filter-bar__actions">
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <TableToolbar title="差异工单" :total="total" />

      <el-table
        table-layout="auto"
        v-loading="loading"
        :data="list"
        stripe
        size="small"
        class="data-table"
        @row-click="(row: { diffId: number }) => openDetail(row.diffId)"
      >
        <el-table-column label="差异ID" prop="diffId" width="96">
          <template #default="{ row }">
            <span class="cell-mono pf-link cursor-pointer">#{{ row.diffId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="任务号" prop="taskId" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-mono">{{ row.taskId || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商户" prop="merchantId" width="112" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-mono">{{ row.merchantId || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" prop="diffType" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="warning" effect="plain">
              {{ labelOf(RECON_DIFF_LABEL, row.diffType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="工单状态" prop="workflowStatus" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="tagTypeOf(RECON_WORKFLOW_STATUS_TAG, row.workflowStatus)">
              {{ labelOf(RECON_WORKFLOW_STATUS_LABEL, row.workflowStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="负责人" prop="assigneeId" width="108" show-overflow-tooltip />
        <el-table-column label="到期时间" prop="dueAt" min-width="168" class-name="col-datetime" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-datetime" :class="{ 'cell-amount--danger': isOverdue(row) }">
              {{ formatDateTime(row.dueAt) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="渠道金额" prop="channelAmount" width="110" align="right" class-name="col-amount">
          <template #default="{ row }">
            <span class="cell-amount">¥{{ formatMoneyFen(row.channelAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="本地金额" prop="localAmount" width="110" align="right" class-name="col-amount">
          <template #default="{ row }">
            <span class="cell-amount">¥{{ formatMoneyFen(row.localAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" min-width="168" class-name="col-datetime" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-datetime">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" class-name="col-actions">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row.diffId)">详情</el-button>
            <el-button
              v-if="row.workflowStatus === 'UNASSIGNED'"
              link
              type="success"
              size="small"
              @click.stop="handleClaim(row.diffId)"
            >
              认领
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <AdminPagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        @size-change="load"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import { claimReconWorkItem, getReconWorkItems } from '@/api/admin'
import {
  RECON_AGE_BUCKET_LABEL,
  RECON_AGE_BUCKET_ORDER,
  RECON_DIFF_LABEL,
  RECON_WORKFLOW_STATUS_LABEL,
  RECON_WORKFLOW_STATUS_TAG,
  formatDateTime,
  formatMoneyFen,
  labelOf,
  tagTypeOf,
} from '@/utils/format'

const isOverdue = (row: { dueAt?: string; workflowStatus?: string }) => {
  if (!row.dueAt) return false
  if (['PROCESSED', 'IGNORED', 'ACCEPTED_LOSS'].includes(row.workflowStatus ?? '')) return false
  return new Date(row.dueAt).getTime() < Date.now()
}

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(DEFAULT_PAGE_SIZE)

const queryForm = reactive({
  billDate: '',
  diffType: '',
  workflowStatus: '',
  onlyMine: true,
  onlyUnassigned: false,
  onlyOverdue: false,
  ageBucket: '',
})

const load = async () => {
  loading.value = true
  try {
    const data = await getReconWorkItems({
      billDate: queryForm.billDate || undefined,
      diffType: queryForm.diffType || undefined,
      workflowStatus: queryForm.workflowStatus || undefined,
      onlyMine: queryForm.onlyMine,
      onlyUnassigned: queryForm.onlyUnassigned,
      onlyOverdue: queryForm.onlyOverdue,
      ageBucket: queryForm.ageBucket || undefined,
      page: page.value,
      size: size.value,
    })
    list.value = data.list ?? []
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  load()
}

const handleReset = () => {
  queryForm.billDate = ''
  queryForm.diffType = ''
  queryForm.workflowStatus = ''
  queryForm.onlyMine = true
  queryForm.onlyUnassigned = false
  queryForm.onlyOverdue = false
  queryForm.ageBucket = ''
  page.value = 1
  load()
}

const openDetail = (diffId: number) => {
  router.push({ path: `/admin/reconcile/work-items/${diffId}` })
}

const handleClaim = async (diffId: number) => {
  await claimReconWorkItem(diffId)
  ElMessage.success('认领成功')
  await load()
}

onMounted(() => {
  if (route.query.diffType) queryForm.diffType = String(route.query.diffType)
  if (route.query.ageBucket) queryForm.ageBucket = String(route.query.ageBucket)
  if (route.query.onlyOverdue === '1') queryForm.onlyOverdue = true
  load()
})
</script>
