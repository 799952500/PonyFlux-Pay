<template>
  <div class="page-table-shell">
    <div class="content-card">
      <TableToolbar title="商户进件审核" :total="list.length" />

      <el-table table-layout="auto" :data="list" stripe size="small" class="data-table" v-loading="loading">
        <el-table-column prop="applicationNo" label="申请单号" min-width="160">
          <template #default="{ row }">
            <span class="cell-mono font-medium">{{ row.applicationNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="merchantName" label="商户名称" min-width="140">
          <template #default="{ row }">
            <span class="font-medium">{{ row.merchantName ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="tagTypeOf(ONBOARDING_STATUS_TAG, row.status)" effect="plain">
              {{ labelOf(ONBOARDING_STATUS_LABEL, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contactPhone" label="联系电话" width="120">
          <template #default="{ row }">{{ row.contactPhone ?? '—' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="172">
          <template #default="{ row }">
            <span class="text-xs text-slate-600 tabular-nums">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listOnboardingApplications } from '@/api/admin'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import {
  formatDateTime,
  labelOf,
  ONBOARDING_STATUS_LABEL,
  ONBOARDING_STATUS_TAG,
  tagTypeOf,
} from '@/utils/format'

interface OnboardingRow {
  applicationNo?: string
  merchantName?: string
  status?: string
  contactPhone?: string
  createdAt?: string
}

const loading = ref(false)
const list = ref<OnboardingRow[]>([])

onMounted(async () => {
  loading.value = true
  try {
    list.value = (await listOnboardingApplications()) as OnboardingRow[]
  } catch (e: any) {
    ElMessage.error(e?.message || '加载进件列表失败')
  } finally {
    loading.value = false
  }
})
</script>
