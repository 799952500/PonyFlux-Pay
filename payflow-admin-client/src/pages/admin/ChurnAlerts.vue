<template>
  <div class="page-table-shell">
    <div class="filter-bar">
      <el-form :inline="true" :model="filters" size="default">
        <el-form-item label="预警状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 140px" @change="loadAlerts">
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="in_progress" />
            <el-option label="已解决" value="resolved" />
            <el-option label="误报" value="false_alarm" />
          </el-select>
        </el-form-item>
        <el-form-item label="商户ID">
          <el-input v-model="filters.merchantId" placeholder="商户ID" clearable style="width: 140px" @keyup.enter="loadAlerts" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" icon="Search" @click="loadAlerts">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="loadAlerts">刷新</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <TableToolbar title="流失预警" :total="pagination.total" />

      <el-table :data="alerts" v-loading="loading" stripe size="small" class="data-table">
        <el-table-column label="商户ID" prop="merchantId" width="100">
          <template #default="{ row }">
            <span class="cell-mono">{{ row.merchantId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预警等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="tagTypeOf(CHURN_LEVEL_TAG, row.alertLevel)" size="small" effect="plain">
              {{ labelOf(CHURN_LEVEL_LABEL, row.alertLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前日均(笔)" prop="currentAvgCount" width="120" align="right" />
        <el-table-column label="基线日均(笔)" prop="baselineAvgCount" width="120" align="right" />
        <el-table-column label="下降幅度" width="110" align="right">
          <template #default="{ row }">
            <span class="font-medium tabular-nums" :class="Number(row.declinePct) > 70 ? 'text-[#EF4444]' : 'text-[#F59E0B]'">
              ↓{{ row.declinePct }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="连续天数" prop="consecutiveDays" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="tagTypeOf(CHURN_STATUS_TAG, row.status)" size="small" effect="plain">
              {{ labelOf(CHURN_STATUS_LABEL, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="跟进人" prop="assignee" width="100">
          <template #default="{ row }">{{ row.assignee || '—' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="172">
          <template #default="{ row }">
            <span class="text-xs text-slate-600 tabular-nums">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'pending'" link type="primary" size="small" @click="handleStatus(row, 'in_progress')">
              开始处理
            </el-button>
            <el-button v-if="row.status === 'in_progress'" link type="success" size="small" @click="handleStatus(row, 'resolved')">
              标记解决
            </el-button>
            <el-button v-if="row.status === 'pending' || row.status === 'in_progress'" link type="warning" size="small" @click="handleStatus(row, 'false_alarm')">
              误报
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <AdminPagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        @size-change="loadAlerts"
        @current-change="loadAlerts"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="更新预警状态" width="420px">
      <el-form :model="dialogForm" label-width="80px">
        <el-form-item label="新状态">
          <el-tag :type="tagTypeOf(CHURN_STATUS_TAG, dialogForm.status)" size="small">
            {{ labelOf(CHURN_STATUS_LABEL, dialogForm.status) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dialogForm.note" type="textarea" :rows="3" placeholder="处理备注（可选）" />
        </el-form-item>
        <el-form-item label="跟进人">
          <el-input v-model="dialogForm.assignee" placeholder="跟进人（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" :loading="submitting" @click="confirmUpdate">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getChurnAlerts, updateChurnAlertStatus } from '@/api/admin'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import {
  CHURN_LEVEL_LABEL,
  CHURN_LEVEL_TAG,
  CHURN_STATUS_LABEL,
  CHURN_STATUS_TAG,
  formatDateTime,
  labelOf,
  tagTypeOf,
} from '@/utils/format'

const loading = ref(false)
const submitting = ref(false)
const alerts = ref<any[]>([])
const dialogVisible = ref(false)
const dialogForm = reactive({ id: 0, status: '', note: '', assignee: '' })

const filters = reactive({ status: 'pending', merchantId: '' })
const pagination = reactive({ page: 1, size: 20, total: 0 })

async function loadAlerts() {
  loading.value = true
  try {
    const result = await getChurnAlerts({
      page: pagination.page,
      size: pagination.size,
      status: filters.status || undefined,
      merchantId: filters.merchantId || undefined,
    })
    alerts.value = result.list
    pagination.total = result.total
  } catch {
    ElMessage.error('加载流失预警失败')
  } finally {
    loading.value = false
  }
}

function handleStatus(row: any, status: string) {
  dialogForm.id = row.id
  dialogForm.status = status
  dialogForm.note = ''
  dialogForm.assignee = ''
  dialogVisible.value = true
}

async function confirmUpdate() {
  submitting.value = true
  try {
    await updateChurnAlertStatus(dialogForm.id, {
      status: dialogForm.status,
      note: dialogForm.note || undefined,
      assignee: dialogForm.assignee || undefined,
    })
    ElMessage.success('状态更新成功')
    dialogVisible.value = false
    loadAlerts()
  } catch {
    ElMessage.error('状态更新失败')
  } finally {
    submitting.value = false
  }
}

loadAlerts()
</script>
