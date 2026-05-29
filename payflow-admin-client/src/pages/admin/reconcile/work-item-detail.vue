<template>
  <div class="page-table-shell">
    <div class="content-card mb-4">
      <div class="flex items-center justify-between mb-4">
        <div>
          <p class="dashboard-section-title m-0">工单详情</p>
          <p class="text-xs text-slate-500 mt-1">差异 ID {{ diff?.id ?? '—' }}</p>
        </div>
        <el-button class="btn-outline" icon="Back" @click="router.back()">返回列表</el-button>
      </div>

      <el-descriptions :column="2" border size="small" class="recon-desc">
        <el-descriptions-item label="差异 ID">
          <span class="cell-mono">{{ diff?.id ?? '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="任务号">
          <span class="cell-mono text-xs break-all">{{ diff?.taskId ?? '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="商户">
          <span class="cell-mono">{{ assignment?.merchantId ?? diff?.merchantId ?? '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="差异类型">
          {{ labelOf(RECON_DIFF_LABEL, diff?.diffType) }}
        </el-descriptions-item>
        <el-descriptions-item label="工单状态">
          <el-tag size="small" :type="tagTypeOf(RECON_WORKFLOW_STATUS_TAG, assignment?.workflowStatus)">
            {{ labelOf(RECON_WORKFLOW_STATUS_LABEL, assignment?.workflowStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="负责人">
          {{ assignment?.assigneeId || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="到期时间">
          <span class="tabular-nums text-xs">{{ formatDateTime(assignment?.dueAt) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="渠道/本地金额">
          <span class="tabular-nums text-xs">
            {{ diff?.channelAmount ?? '—' }} / {{ diff?.localAmount ?? '—' }} 分
          </span>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="content-card mb-4">
      <p class="dashboard-section-title mb-3">处置操作</p>
      <div class="flex gap-2 flex-wrap">
        <el-button type="success" class="btn-primary" :disabled="assignment?.workflowStatus !== 'UNASSIGNED'" @click="doClaim">
          认领
        </el-button>
        <el-button type="warning" class="btn-outline" :disabled="!assignment?.assigneeId && assignment?.workflowStatus !== 'UNASSIGNED'" @click="openAssignDialog">
          指派/改派
        </el-button>
        <el-button type="primary" class="btn-primary" :disabled="!canStart" @click="doStart">开始处理</el-button>
        <el-button type="primary" class="btn-outline" :disabled="!canComplete" @click="openCompleteDialog">完成/忽略/挂账</el-button>
      </div>
    </div>

    <div class="content-card mb-4">
      <p class="dashboard-section-title mb-3">留言</p>
      <div class="flex gap-2 max-w-2xl">
        <el-input v-model="comment" placeholder="请输入留言（至少 5 字）" />
        <el-button type="primary" class="btn-primary shrink-0" @click="doComment">提交</el-button>
      </div>
    </div>

    <div class="content-card">
      <TableToolbar title="审计记录" :total="audits.length" />
      <el-timeline v-if="audits.length" class="mt-2 px-2">
        <el-timeline-item v-for="(a, idx) in audits" :key="idx" :timestamp="formatDateTime(a.createdAt)" placement="top">
          <div class="text-sm">
            <el-tag size="small" type="info" class="mr-2">{{ a.action }}</el-tag>
            <span class="text-slate-600">{{ a.operator }}</span>
          </div>
          <div v-if="a.detail" class="text-xs text-slate-500 mt-1">{{ a.detail }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审计记录" :image-size="72" />
    </div>

    <el-dialog v-model="assignDialogVisible" title="指派/改派" width="440px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="负责人">
          <el-input v-model="assignForm.assigneeId" placeholder="输入用户名，如 admin" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="assignForm.remark" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="btn-outline" @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" @click="doAssign">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="completeDialogVisible" title="提交终态" width="440px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="动作">
          <el-select v-model="completeForm.action" style="width: 100%">
            <el-option label="已处理" value="PROCESSED" />
            <el-option label="已忽略" value="IGNORED" />
            <el-option label="挂账" value="ACCEPTED_LOSS" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="completeForm.remark" type="textarea" :rows="3" placeholder="必填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="btn-outline" @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" @click="doComplete">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import {
  assignReconWorkItem,
  claimReconWorkItem,
  commentReconWorkItem,
  completeReconWorkItem,
  getReconWorkItemDetail,
  startReconWorkItem,
} from '@/api/admin'
import {
  RECON_DIFF_LABEL,
  RECON_WORKFLOW_STATUS_LABEL,
  RECON_WORKFLOW_STATUS_TAG,
  formatDateTime,
  labelOf,
  tagTypeOf,
} from '@/utils/format'

const route = useRoute()
const router = useRouter()
const diffId = Number(route.params.diffId)

const diff = ref<any>(null)
const assignment = ref<any>(null)
const audits = ref<any[]>([])

const comment = ref('')

const assignDialogVisible = ref(false)
const assignForm = reactive({ assigneeId: '', remark: '' })

const completeDialogVisible = ref(false)
const completeForm = reactive({ action: 'PROCESSED', remark: '' })

const canStart = computed(() => ['ASSIGNED', 'ESCALATED'].includes(assignment.value?.workflowStatus))
const canComplete = computed(() =>
  ['ASSIGNED', 'ESCALATED', 'IN_PROGRESS'].includes(assignment.value?.workflowStatus),
)

const load = async () => {
  const data = await getReconWorkItemDetail(diffId)
  diff.value = data?.diff
  assignment.value = data?.assignment
  audits.value = data?.audits ?? []
}

const doClaim = async () => {
  await claimReconWorkItem(diffId)
  ElMessage.success('认领成功')
  await load()
}

const openAssignDialog = () => {
  assignForm.assigneeId = assignment.value?.assigneeId ?? ''
  assignForm.remark = ''
  assignDialogVisible.value = true
}

const doAssign = async () => {
  await assignReconWorkItem(diffId, { assigneeId: assignForm.assigneeId, remark: assignForm.remark || undefined })
  assignDialogVisible.value = false
  ElMessage.success('指派成功')
  await load()
}

const doStart = async () => {
  await startReconWorkItem(diffId)
  ElMessage.success('已开始处理')
  await load()
}

const openCompleteDialog = () => {
  completeForm.action = 'PROCESSED'
  completeForm.remark = ''
  completeDialogVisible.value = true
}

const doComplete = async () => {
  await completeReconWorkItem(diffId, { action: completeForm.action, remark: completeForm.remark })
  completeDialogVisible.value = false
  ElMessage.success('提交成功')
  await load()
}

const doComment = async () => {
  await commentReconWorkItem(diffId, { content: comment.value })
  comment.value = ''
  ElMessage.success('留言成功')
  await load()
}

load()
</script>

<style scoped>
.recon-desc :deep(.el-descriptions__label) {
  width: 108px;
}
</style>
