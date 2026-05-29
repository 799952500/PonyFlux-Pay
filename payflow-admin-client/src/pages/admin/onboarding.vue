<template>
  <div class="page-table-shell">
    <div class="filter-bar">
      <el-form :inline="true" :model="queryForm" size="default" class="filter-bar__form">
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="待审核" value="SUBMITTED" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="queryForm.keyword"
            clearable
            placeholder="商户名 / 单号 / 手机 / 邮箱"
            style="width: 220px"
            @keyup.enter="loadList"
          />
        </el-form-item>
        <el-form-item class="filter-bar__actions">
          <el-button type="primary" @click="loadList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <TableToolbar title="商户进件审核" :total="total" />

      <el-table
        table-layout="auto"
        :data="list"
        stripe
        size="small"
        class="data-table"
        v-loading="loading"
      >
        <el-table-column prop="applicationNo" label="申请单号" min-width="160">
          <template #default="{ row }">
            <span class="cell-mono pf-link">{{ row.applicationNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="merchantName" label="商户名称" min-width="140">
          <template #default="{ row }">
            <span class="font-medium">{{ row.merchantName ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="tagTypeOf(ONBOARDING_STATUS_TAG, row.status)" effect="plain" class="table-tag-compact">
              {{ labelOf(ONBOARDING_STATUS_LABEL, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contactPhone" label="联系电话" width="120">
          <template #default="{ row }">
            <span class="cell-mono">{{ row.contactPhone ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contactEmail" label="邮箱" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-ellipsis">{{ row.contactEmail ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="172" class-name="col-datetime">
          <template #default="{ row }">
            <span class="cell-datetime">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" class-name="col-actions">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row.id)">详情</el-button>
            <template v-if="row.status === 'SUBMITTED' || row.status === 'REVIEWING'">
              <el-button type="success" link @click="quickApprove(row.id)">通过</el-button>
              <el-button type="danger" link @click="openDetail(row.id)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <AdminPagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        @size-change="loadList"
        @current-change="loadList"
      />
    </div>

    <ApplicationDetailDrawer
      v-model="drawerVisible"
      :application-id="activeId"
      @updated="loadList"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveOnboarding,
  listOnboardingApplications,
  type OnboardingApplicationRow,
} from '@/api/onboarding'
import ApplicationDetailDrawer from '@/components/onboarding/ApplicationDetailDrawer.vue'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import {
  formatDateTime,
  labelOf,
  ONBOARDING_STATUS_LABEL,
  ONBOARDING_STATUS_TAG,
  tagTypeOf,
} from '@/utils/format'

const loading = ref(false)
const list = ref<OnboardingApplicationRow[]>([])
const total = ref(0)
const drawerVisible = ref(false)
const activeId = ref<number | null>(null)

const queryForm = reactive({
  page: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  status: '' as string,
  keyword: '',
})

async function loadList() {
  loading.value = true
  try {
    const data = await listOnboardingApplications({
      page: queryForm.page,
      pageSize: queryForm.pageSize,
      status: queryForm.status || undefined,
      keyword: queryForm.keyword || undefined,
    })
    list.value = data.list ?? []
    total.value = data.total ?? 0
  } catch (e: any) {
    ElMessage.error(e?.message || '加载进件列表失败')
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.status = ''
  queryForm.keyword = ''
  queryForm.page = 1
  loadList()
}

function openDetail(id: number) {
  activeId.value = id
  drawerVisible.value = true
}

async function quickApprove(id: number) {
  try {
    await ElMessageBox.confirm(
      '商户将通过收银台自助查询页获取密钥，运营无需手动转发。',
      '确认审批通过',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await approveOnboarding(id)
    ElMessage.success('审批通过')
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '审批失败')
  }
}

onMounted(loadList)
</script>
