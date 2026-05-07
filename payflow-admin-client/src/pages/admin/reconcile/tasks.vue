<template>
  <div>
    <div class="content-card mb-4">
      <el-form :inline="true" :model="queryForm" size="default">
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
        <el-form-item label="渠道">
          <el-select v-model="queryForm.channel" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="支付宝" value="alipay" />
            <el-option label="微信" value="wxpay" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAIL" />
            <el-option label="比对中" value="COMPARING" />
            <el-option label="解析中" value="PARSING" />
            <el-option label="下载中" value="DOWNLOADING" />
            <el-option label="初始" value="INIT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="success" class="btn-primary" @click="openManual">手动对账</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <el-table v-loading="loading" :data="taskList" stripe size="small" class="data-table">
        <el-table-column label="任务号" prop="taskId" min-width="200" show-overflow-tooltip />
        <el-table-column label="渠道" prop="channel" width="90" />
        <el-table-column label="账户" prop="accountCode" width="140" show-overflow-tooltip />
        <el-table-column label="账单日" prop="billDate" width="120" />
        <el-table-column label="状态" prop="status" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="差异数" prop="diffCount" width="80" align="right" />
        <el-table-column label="触发" prop="triggeredBy" width="100" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDrawer(row)">详情</el-button>
            <el-button link type="primary" size="small" :disabled="!row.fileObjectKey" @click="downloadFile(row.taskId)">
              下载
            </el-button>
            <el-button link type="warning" size="small" @click="rerun(row)">重跑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex justify-end p-4">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadTasks"
          @current-change="loadTasks"
        />
      </div>
    </div>

    <el-drawer v-model="drawerVisible" title="对账任务" direction="rtl" size="640px">
      <div v-if="detailLoading" class="p-4"><el-skeleton animated :rows="8" /></div>
      <div v-else-if="currentTask" class="px-4 pb-4">
        <el-tabs v-model="drawerTab">
          <el-tab-pane label="概览" name="sum">
            <dl class="grid grid-cols-2 gap-y-2 gap-x-4 text-sm mt-2">
              <dt class="text-gray-400">任务号</dt>
              <dd class="text-gray-800 font-mono break-all">{{ currentTask.taskId }}</dd>
              <dt class="text-gray-400">渠道 / 账户</dt>
              <dd>{{ currentTask.channel }} / {{ currentTask.accountCode }}</dd>
              <dt class="text-gray-400">账单日</dt>
              <dd>{{ currentTask.billDate }}</dd>
              <dt class="text-gray-400">状态</dt>
              <dd><el-tag size="small" :type="statusTag(currentTask.status)">{{ currentTask.status }}</el-tag></dd>
              <dt class="text-gray-400">账单笔数 / 金额(分)</dt>
              <dd>{{ currentTask.billTotalCount ?? '-' }} / {{ currentTask.billTotalAmount ?? '-' }}</dd>
              <dt class="text-gray-400">本地笔数 / 金额(分)</dt>
              <dd>{{ currentTask.localTotalCount ?? '-' }} / {{ currentTask.localTotalAmount ?? '-' }}</dd>
              <dt class="text-gray-400">差异数</dt>
              <dd>{{ currentTask.diffCount ?? 0 }}</dd>
              <dt class="text-gray-400">耗时(ms)</dt>
              <dd>{{ currentTask.elapsedMs ?? '-' }}</dd>
              <dt class="text-gray-400">错误信息</dt>
              <dd class="break-all text-red-600">{{ currentTask.errorMsg || '-' }}</dd>
            </dl>
          </el-tab-pane>
          <el-tab-pane label="差异" name="diff">
            <div class="mb-3 flex flex-wrap gap-2 items-center">
              <el-select v-model="diffQuery.diffType" placeholder="差异类型" clearable style="width: 160px" size="small">
                <el-option label="CHANNEL_ONLY" value="CHANNEL_ONLY" />
                <el-option label="LOCAL_ONLY" value="LOCAL_ONLY" />
                <el-option label="AMOUNT_MISMATCH" value="AMOUNT_MISMATCH" />
                <el-option label="STATUS_MISMATCH" value="STATUS_MISMATCH" />
              </el-select>
              <el-select v-model="diffQuery.handleStatus" placeholder="处理状态" clearable style="width: 140px" size="small">
                <el-option label="PENDING" value="PENDING" />
                <el-option label="PROCESSED" value="PROCESSED" />
                <el-option label="IGNORED" value="IGNORED" />
              </el-select>
              <el-button size="small" type="primary" @click="loadDiffs">筛选</el-button>
            </div>
            <el-table v-loading="diffLoading" :data="diffList" size="small" max-height="420">
              <el-table-column prop="diffType" label="类型" width="130" />
              <el-table-column prop="channelTradeNo" label="渠道单号" min-width="120" show-overflow-tooltip />
              <el-table-column prop="localOrderId" label="本地订单" width="120" show-overflow-tooltip />
              <el-table-column label="金额(分)" width="160">
                <template #default="{ row }">
                  <span class="tabular-nums">{{ row.channelAmount ?? '-' }} / {{ row.localAmount ?? '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="handleStatus" label="处理" width="100" />
              <el-table-column label="操作" width="160" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-if="row.handleStatus === 'PENDING'"
                    link
                    type="primary"
                    size="small"
                    @click="openHandle(row)"
                  >
                    处理
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="flex justify-end mt-2">
              <el-pagination
                v-model:current-page="diffQuery.page"
                v-model:page-size="diffQuery.pageSize"
                small
                :total="diffTotal"
                layout="total, prev, pager, next"
                @current-change="loadDiffs"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <el-dialog v-model="manualVisible" title="手动触发对账" width="420px" destroy-on-close>
      <el-form :model="manualForm" label-width="100px">
        <el-form-item label="渠道" required>
          <el-select v-model="manualForm.reconChannel" style="width: 100%">
            <el-option label="alipay" value="alipay" />
            <el-option label="wxpay" value="wxpay" />
          </el-select>
        </el-form-item>
        <el-form-item label="账户编码" required>
          <el-input v-model="manualForm.accountCode" placeholder="如 CASHIER_ALI_001" />
        </el-form-item>
        <el-form-item label="账单日" required>
          <el-date-picker v-model="manualForm.billDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualVisible = false">取消</el-button>
        <el-button type="primary" :loading="manualLoading" @click="submitManual">执行</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="handleVisible" title="处理差异" width="400px" destroy-on-close>
      <el-form :model="handleForm" label-width="88px">
        <el-form-item label="动作" required>
          <el-select v-model="handleForm.action" style="width: 100%">
            <el-option label="PROCESSED" value="PROCESSED" />
            <el-option label="IGNORED" value="IGNORED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="handleForm.remark" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="handleLoading" @click="submitHandle">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getReconTasks,
  getReconTaskDetail,
  getReconDiffs,
  triggerReconManual,
  handleReconDiff,
  type ReconTaskItem,
  type ReconDiffItem,
} from '@/api/admin'

const loading = ref(false)
const taskList = ref<ReconTaskItem[]>([])
const total = ref(0)

const queryForm = reactive({
  page: 1,
  pageSize: 20,
  billDate: '' as string | undefined,
  channel: '',
  status: '',
})

const drawerVisible = ref(false)
const drawerTab = ref('sum')
const detailLoading = ref(false)
const currentTask = ref<ReconTaskItem | null>(null)

const diffLoading = ref(false)
const diffList = ref<ReconDiffItem[]>([])
const diffTotal = ref(0)
const diffQuery = reactive({
  page: 1,
  pageSize: 10,
  diffType: '',
  handleStatus: '',
})

const manualVisible = ref(false)
const manualLoading = ref(false)
const manualForm = reactive({
  reconChannel: 'alipay',
  accountCode: 'CASHIER_ALI_001',
  billDate: '',
})

const handleVisible = ref(false)
const handleLoading = ref(false)
const handleTarget = ref<ReconDiffItem | null>(null)
const handleForm = reactive({
  action: 'PROCESSED',
  remark: '',
})

function statusTag(s: string) {
  if (s === 'SUCCESS') return 'success'
  if (s === 'FAIL') return 'danger'
  return 'info'
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await getReconTasks({
      page: queryForm.page,
      size: queryForm.pageSize,
      billDate: queryForm.billDate || undefined,
      channel: queryForm.channel || undefined,
      status: queryForm.status || undefined,
    })
    taskList.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.page = 1
  loadTasks()
}

function handleReset() {
  queryForm.billDate = ''
  queryForm.channel = ''
  queryForm.status = ''
  queryForm.page = 1
  loadTasks()
}

async function openDrawer(row: ReconTaskItem) {
  drawerVisible.value = true
  drawerTab.value = 'sum'
  detailLoading.value = true
  currentTask.value = null
  try {
    currentTask.value = await getReconTaskDetail(row.taskId)
    diffQuery.page = 1
    await loadDiffs()
  } finally {
    detailLoading.value = false
  }
}

watch(drawerTab, (tab) => {
  if (tab === 'diff' && currentTask.value) {
    loadDiffs()
  }
})

async function loadDiffs() {
  if (!currentTask.value) return
  diffLoading.value = true
  try {
    const res = await getReconDiffs(currentTask.value.taskId, {
      page: diffQuery.page,
      size: diffQuery.pageSize,
      diffType: diffQuery.diffType || undefined,
      handleStatus: diffQuery.handleStatus || undefined,
    })
    diffList.value = res.list
    diffTotal.value = res.total
  } finally {
    diffLoading.value = false
  }
}

async function downloadFile(taskId: string) {
  const token = localStorage.getItem('adminToken')
  const url = `/api/v1/admin/reconcile/tasks/${encodeURIComponent(taskId)}/file`
  try {
    const res = await fetch(url, {
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    })
    if (res.status === 401) {
      localStorage.removeItem('adminToken')
      window.location.href = '/login'
      return
    }
    if (!res.ok) {
      const t = await res.text()
      throw new Error(t || `HTTP ${res.status}`)
    }
    const blob = await res.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `recon-${taskId}.csv`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(a.href)
    ElMessage.success('已开始下载')
  } catch (e: any) {
    ElMessage.error(e?.message || '下载失败')
  }
}

function openManual() {
  manualForm.reconChannel = 'alipay'
  manualForm.accountCode = 'CASHIER_ALI_001'
  manualForm.billDate = ''
  manualVisible.value = true
}

async function submitManual() {
  if (!manualForm.billDate) {
    ElMessage.warning('请选择账单日')
    return
  }
  manualLoading.value = true
  try {
    const r = await triggerReconManual({
      reconChannel: manualForm.reconChannel,
      accountCode: manualForm.accountCode,
      billDate: manualForm.billDate,
    })
    ElMessage.success(`已触发，任务号 ${r.taskId}`)
    manualVisible.value = false
    loadTasks()
  } finally {
    manualLoading.value = false
  }
}

function rerun(row: ReconTaskItem) {
  manualForm.reconChannel = row.channel
  manualForm.accountCode = row.accountCode
  manualForm.billDate = String(row.billDate)
  manualVisible.value = true
}

function openHandle(row: ReconDiffItem) {
  handleTarget.value = row
  handleForm.action = 'PROCESSED'
  handleForm.remark = ''
  handleVisible.value = true
}

async function submitHandle() {
  if (!handleTarget.value) return
  handleLoading.value = true
  try {
    await handleReconDiff(handleTarget.value.id, {
      action: handleForm.action,
      remark: handleForm.remark,
    })
    ElMessage.success('已提交')
    handleVisible.value = false
    await loadDiffs()
  } finally {
    handleLoading.value = false
  }
}

loadTasks()
</script>
