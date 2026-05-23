<template>
  <el-drawer
    v-model="visible"
    title="进件申请详情"
    direction="rtl"
    size="560px"
    destroy-on-close
    @closed="emit('closed')"
  >
    <div v-loading="loading">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small" class="mb-4">
          <el-descriptions-item label="申请单号">
            <span class="cell-mono">{{ detail.applicationNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="商户名称">{{ detail.merchantName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="tagTypeOf(ONBOARDING_STATUS_TAG, detail.status)" effect="plain">
              {{ labelOf(ONBOARDING_STATUS_LABEL, detail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.contactName ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="手机">{{ detail.contactPhone ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.contactEmail ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="营业执照">{{ detail.bizLicenseNo ?? '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.allocatedMerchantId" label="分配商户号">
            <span class="cell-mono">{{ detail.allocatedMerchantId }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.approvedAt" label="通过时间">{{ formatDateTime(detail.approvedAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.rejectedAt" label="拒绝时间">{{ formatDateTime(detail.rejectedAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.rejectReason" label="拒绝原因">{{ detail.rejectReason }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.resultQueryCount != null" label="密钥查询次数">
            {{ detail.resultQueryCount }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="payloadPreview" class="content-card detail-card mb-4">
          <h3 class="detail-section__title">扩展信息</h3>
          <pre class="payload-pre">{{ payloadPreview }}</pre>
        </div>

        <div v-if="canReview" class="flex gap-2">
          <el-button type="primary" link :loading="approving" @click="handleApprove">审批通过</el-button>
          <el-button type="danger" link @click="showReject = true">拒绝</el-button>
        </div>

        <el-dialog v-model="showReject" title="拒绝申请" width="400px" append-to-body>
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="3"
            placeholder="请填写拒绝原因，商户可在查询页看到"
          />
          <template #footer>
            <el-button @click="showReject = false">取消</el-button>
            <el-button type="danger" :loading="rejecting" @click="handleReject">确认拒绝</el-button>
          </template>
        </el-dialog>
      </template>
      <el-empty v-else-if="!loading" description="未找到申请" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveOnboarding,
  getOnboardingDetail,
  rejectOnboarding,
  type OnboardingDetail,
} from '@/api/onboarding'
import {
  formatDateTime,
  labelOf,
  ONBOARDING_STATUS_LABEL,
  ONBOARDING_STATUS_TAG,
  tagTypeOf,
} from '@/utils/format'

const props = defineProps<{
  applicationId: number | null
}>()

const emit = defineEmits<{
  closed: []
  updated: []
}>()

const visible = defineModel<boolean>({ default: false })

const loading = ref(false)
const detail = ref<OnboardingDetail | null>(null)
const approving = ref(false)
const rejecting = ref(false)
const showReject = ref(false)
const rejectReason = ref('')

const canReview = computed(
  () => detail.value?.status === 'SUBMITTED' || detail.value?.status === 'REVIEWING',
)

const payloadPreview = computed(() => {
  if (!detail.value?.payloadJson) return ''
  try {
    return JSON.stringify(JSON.parse(detail.value.payloadJson), null, 2)
  } catch {
    return detail.value.payloadJson
  }
})

watch(
  () => [visible.value, props.applicationId] as const,
  async ([open, id]) => {
    if (!open || id == null) return
    loading.value = true
    detail.value = null
    try {
      detail.value = await getOnboardingDetail(id)
    } catch (e: any) {
      ElMessage.error(e?.message || '加载详情失败')
    } finally {
      loading.value = false
    }
  },
)

async function handleApprove() {
  if (props.applicationId == null) return
  try {
    await ElMessageBox.confirm(
      '审批通过后将自动生成商户号、签名密钥与管理后台账号。商户需通过收银台「入驻结果查询」页自助获取密钥，运营无需手动转发。',
      '确认审批通过',
      { type: 'warning' },
    )
  } catch {
    return
  }
  approving.value = true
  try {
    await approveOnboarding(props.applicationId)
    ElMessage.success('审批通过')
    detail.value = await getOnboardingDetail(props.applicationId)
    emit('updated')
  } catch (e: any) {
    ElMessage.error(e?.message || '审批失败')
  } finally {
    approving.value = false
  }
}

async function handleReject() {
  if (props.applicationId == null || !rejectReason.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  rejecting.value = true
  try {
    await rejectOnboarding(props.applicationId, rejectReason.value.trim())
    ElMessage.success('已拒绝')
    showReject.value = false
    detail.value = await getOnboardingDetail(props.applicationId)
    emit('updated')
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    rejecting.value = false
  }
}
</script>

<style scoped>
.payload-pre {
  font-size: 12px;
  font-family: ui-monospace, monospace;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  padding: 12px;
  background: var(--pf-surface-muted, #f8fafc);
  border-radius: 8px;
}
</style>
