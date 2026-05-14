<template>
  <div>
    <div class="flex items-center justify-between mb-5">
      <h2 class="text-lg font-semibold text-[#0F172A]">流失预警</h2>
      <el-button size="small" @click="loadAlerts">
        <el-icon class="mr-1"><Refresh /></el-icon>刷新
      </el-button>
    </div>

    <div class="content-card mb-5">
      <el-form :inline="true" :model="filters" size="small">
        <el-form-item label="预警状态">
          <el-select v-model="filters.status" placeholder="全部" clearable @change="loadAlerts">
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="in_progress" />
            <el-option label="已解决" value="resolved" />
            <el-option label="误报" value="false_alarm" />
          </el-select>
        </el-form-item>
        <el-form-item label="商户ID">
          <el-input v-model="filters.merchantId" placeholder="商户ID" clearable @keyup.enter="loadAlerts" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadAlerts">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <el-table :data="alerts" v-loading="loading" size="small" class="data-table">
        <el-table-column label="商户ID" prop="merchantId" width="100" />
        <el-table-column label="预警等级" width="100">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.alertLevel)" size="small">
              {{ levelLabel(row.alertLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前日均(笔)" prop="currentAvgCount" width="120" />
        <el-table-column label="基线日均(笔)" prop="baselineAvgCount" width="120" />
        <el-table-column label="下降幅度" width="110">
          <template #default="{ row }">
            <span class="font-medium" :class="Number(row.declinePct) > 70 ? 'text-[#EF4444]' : 'text-[#F59E0B]'">
              ↓{{ row.declinePct }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="连续天数" prop="consecutiveDays" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="跟进人" prop="assignee" width="100" />
        <el-table-column label="创建时间" prop="createTime" width="170" />
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

      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          small
          @change="loadAlerts"
        />
      </div>
    </div>

    <!-- 状态更新对话框 -->
    <el-dialog v-model="dialogVisible" title="更新预警状态" width="420px">
      <el-form :model="dialogForm" label-width="80px">
        <el-form-item label="新状态">
          <el-tag :type="statusTagType(dialogForm.status)" size="small">
            {{ statusLabel(dialogForm.status) }}
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
        <el-button size="small" @click="dialogVisible = false">取消</el-button>
        <el-button size="small" type="primary" @click="confirmUpdate" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getChurnAlerts, updateChurnAlertStatus } from '@/api/admin'

const loading = ref(false)
const submitting = ref(false)
const alerts = ref<any[]>([])
const dialogVisible = ref(false)
const dialogForm = reactive({ id: 0, status: '', note: '', assignee: '' })

const filters = reactive({ status: 'pending', merchantId: '' })
const pagination = reactive({ page: 1, size: 20, total: 0 })

const levelMap: Record<string, string> = { yellow: '黄色预警', orange: '橙色预警', red: '红色预警' }
const levelTagMap: Record<string, string> = { yellow: 'warning', orange: 'warning', red: 'danger' }
const statusMap: Record<string, string> = { pending: '待处理', in_progress: '处理中', resolved: '已解决', false_alarm: '误报' }
const statusTagMap: Record<string, string> = { pending: 'danger', in_progress: 'warning', resolved: 'success', false_alarm: 'info' }

function levelLabel(l: string) { return levelMap[l] ?? l }
function levelTagType(l: string) { return levelTagMap[l] ?? 'info' }
function statusLabel(s: string) { return statusMap[s] ?? s }
function statusTagType(s: string) { return statusTagMap[s] ?? 'info' }

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
