<template>
  <div class="order-detail-panel">
    <div v-if="loading" class="detail-card content-card">
      <el-skeleton animated :rows="6" />
    </div>

    <template v-else-if="order">
      <header class="detail-hero content-card detail-card">
        <div class="detail-hero__main">
          <p class="detail-hero__eyebrow">订单号</p>
          <h2 class="detail-hero__id" :data-flip="`order-${order.orderId}`">{{ order.orderId }}</h2>
          <div class="detail-hero__tags">
            <el-tag size="small" :type="tagTypeOf(ORDER_STATUS_TAG, order.status)">
              {{ labelOf(ORDER_STATUS_LABEL, order.status) }}
            </el-tag>
            <el-tag v-if="order.channel" size="small" :type="channelTagType(order.channel)" effect="plain">
              {{ channelLabel(order.channel) }}
            </el-tag>
          </div>
        </div>
        <div class="detail-hero__amount">
          <span class="detail-hero__amount-label">订单金额</span>
          <span class="detail-hero__amount-value">¥{{ formatMoneyFen(order.amount) }}</span>
          <span v-if="order.currency && order.currency !== 'CNY'" class="detail-hero__currency">{{ order.currency }}</span>
          <el-button class="mt-3" size="small" type="primary" plain @click="openMerchantNotify">
            查看回调
          </el-button>
        </div>
      </header>

      <div class="content-card detail-card">
        <h3 class="detail-section__title">订单信息</h3>
        <el-descriptions :column="descColumn" border class="detail-descriptions">
          <el-descriptions-item label="商户订单号">
            <span class="tabular-nums">{{ order.merchantOrderNo || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="商户 ID">{{ order.merchantId || '—' }}</el-descriptions-item>
          <el-descriptions-item label="商品名称" :span="descColumn">
            {{ order.subject || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="支付渠道">
            <el-tag v-if="order.channel" size="small" :type="channelTagType(order.channel)">
              {{ channelLabel(order.channel) }}
            </el-tag>
            <span v-else class="cell-empty">—</span>
          </el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag size="small" :type="tagTypeOf(ORDER_STATUS_TAG, order.status)">
              {{ labelOf(ORDER_STATUS_LABEL, order.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            <span class="tabular-nums">{{ formatDateTime(order.createdAt) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="过期时间">
            <span class="tabular-nums">{{ formatDateTime(order.expireTime) }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="order.payTime" label="支付时间">
            <span class="tabular-nums">{{ formatDateTime(order.payTime) }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="order.payAmount != null" label="实付金额">
            <span class="tabular-nums font-semibold text-emerald-700">¥{{ formatMoneyFen(order.payAmount) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="content-card detail-card">
        <h3 class="detail-section__title">支付记录</h3>
        <p class="detail-section__hint text-xs text-[var(--pf-text-secondary)] mb-3">
          与支付机构状态不一致时，可对子单「查单」；需退款请「申请退款」，审批通过后在退款管理执行渠道退款。
        </p>
        <el-table
          v-if="payments.length"
          :data="payments"
          size="small"
          stripe
          class="data-table"
        >
          <el-table-column label="支付单号" prop="paymentId" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="cell-mono cell-ellipsis">{{ row.paymentId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="渠道" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.payChannel" size="small" :type="channelTagType(row.payChannel)" effect="plain">
                {{ channelLabel(row.payChannel) }}
              </el-tag>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="100" align="right">
            <template #default="{ row }">
              <span class="tabular-nums">¥{{ formatMoneyFen(row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="108">
            <template #default="{ row }">
              <el-tag size="small" :type="tagTypeOf(PAYMENT_STATUS_TAG, row.status)">
                {{ labelOf(PAYMENT_STATUS_LABEL, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button
                v-permission="'order:payment:query'"
                link
                type="primary"
                size="small"
                :loading="queryingPaymentId === row.paymentId"
                @click="handleQueryChannel(row, false)"
              >
                查单
              </el-button>
              <el-button
                v-permission="'order:payment:query'"
                link
                type="warning"
                size="small"
                :loading="queryingPaymentId === row.paymentId"
                @click="handleQueryChannel(row, true)"
              >
                查单并同步
              </el-button>
              <el-button
                v-if="canRequestRefund(row)"
                v-permission="'refund:create'"
                link
                type="danger"
                size="small"
                @click="openRefundDialog(row)"
              >
                申请退款
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无支付子单" :image-size="64" />
      </div>
    </template>

    <el-empty v-else class="detail-empty" description="未找到该订单" />

    <MerchantNotifyDetailDrawer ref="notifyDrawerRef" />

    <el-dialog
      v-model="refundDialogVisible"
      title="申请退款"
      width="420px"
      destroy-on-close
      append-to-body
      @closed="resetRefundForm"
    >
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        class="mb-4"
        title="提交后仅生成待审批退款单，不会立即向支付机构退款。请在「退款管理」中审批通过后才会执行渠道退款。"
      />
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="96px">
        <el-form-item label="支付单号">
          <span class="cell-mono text-sm">{{ refundForm.paymentId }}</span>
        </el-form-item>
        <el-form-item label="退款金额" prop="refundAmountYuan">
          <el-input-number
            v-model="refundForm.refundAmountYuan"
            :min="0.01"
            :max="refundMaxYuan"
            :precision="2"
            :step="0.01"
            controls-position="right"
            class="w-full"
          />
          <p class="text-xs text-[var(--pf-text-secondary)] mt-1">
            可退上限 ¥{{ formatMoneyFen(refundMaxFen) }}
          </p>
        </el-form-item>
        <el-form-item label="退款原因" prop="reason">
          <el-input
            v-model="refundForm.reason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请填写退款原因，便于审批"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="refundSubmitting" @click="submitRefundRequest">
          提交申请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import MerchantNotifyDetailDrawer from '@/components/merchant-notifies/DetailDrawer.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createOrderRefundRequest,
  getOrderDetailFull,
  queryOrderPaymentChannel,
} from '@/api/admin'
import { useMerchantScope } from '@/composables/useMerchantScope'
import type { Order, OrderPayment } from '@/types'
import {
  channelLabel,
  channelTagType,
  formatDateTime,
  formatMoneyFen,
  labelOf,
  ORDER_STATUS_LABEL,
  ORDER_STATUS_TAG,
  PAYMENT_STATUS_LABEL,
  PAYMENT_STATUS_TAG,
  tagTypeOf,
} from '@/utils/format'

const props = defineProps<{
  orderId: string | null
  active?: boolean
}>()

const router = useRouter()
const { isMerchantAllowed } = useMerchantScope()
const loading = ref(false)
const order = ref<Order | null>(null)
const payments = ref<OrderPayment[]>([])
const notifyDrawerRef = ref<InstanceType<typeof MerchantNotifyDetailDrawer> | null>(null)
const queryingPaymentId = ref<string | null>(null)

const refundDialogVisible = ref(false)
const refundSubmitting = ref(false)
const refundFormRef = ref<FormInstance>()
const refundMaxFen = ref(0)
const refundForm = ref({
  paymentId: '',
  refundAmountYuan: 0,
  reason: '',
})

const refundMaxYuan = computed(() => Math.max(refundMaxFen.value / 100, 0.01))

const refundRules: FormRules = {
  refundAmountYuan: [{ required: true, message: '请输入退款金额', trigger: 'blur' }],
  reason: [{ required: true, message: '请填写退款原因', trigger: 'blur' }],
}

function openMerchantNotify() {
  if (!order.value?.orderId) return
  notifyDrawerRef.value?.openByOrderId(order.value.orderId)
}

const viewportWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1280)
const descColumn = computed(() => (viewportWidth.value < 768 ? 1 : 2))

function syncViewport() {
  viewportWidth.value = window.innerWidth
}

function canRequestRefund(payment: OrderPayment): boolean {
  return payment.status === 'SUCCESS' || payment.status === 'PARTIAL_REFUND'
}

async function loadDetail(id: string) {
  loading.value = true
  order.value = null
  payments.value = []
  try {
    const detail = await getOrderDetailFull(id)
    if (!isMerchantAllowed(detail.order.merchantId)) {
      order.value = null
      ElMessage.warning('无权查看该订单')
    } else {
      order.value = detail.order
      payments.value = detail.payments
    }
  } catch {
    order.value = null
    payments.value = []
    ElMessage.error('加载订单详情失败')
  } finally {
    loading.value = false
  }
}

async function handleQueryChannel(payment: OrderPayment, sync: boolean) {
  if (!order.value?.orderId) return
  const actionLabel = sync ? '查单并同步本地状态' : '向支付机构查单'
  try {
    await ElMessageBox.confirm(
      sync
        ? '将向支付机构查询支付结果；若渠道已支付且本地仍为处理中，将回写为支付成功。请确认订单号与金额无误。'
        : '仅查询支付机构侧状态，不会修改本地订单。若需修复不一致请选择「查单并同步」。',
      actionLabel,
      { type: 'warning', confirmButtonText: '继续', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  queryingPaymentId.value = payment.paymentId
  try {
    const result = await queryOrderPaymentChannel(order.value.orderId, payment.paymentId, sync)
    await ElMessageBox.alert(result.message || '查单完成', '查单结果', { type: 'info' })
    if (result.synced) {
      await loadDetail(order.value.orderId)
    }
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '查单失败'
    ElMessage.error(msg)
  } finally {
    queryingPaymentId.value = null
  }
}

function openRefundDialog(payment: OrderPayment) {
  refundForm.value = {
    paymentId: payment.paymentId,
    refundAmountYuan: payment.amount / 100,
    reason: '',
  }
  refundMaxFen.value = payment.amount
  refundDialogVisible.value = true
}

function resetRefundForm() {
  refundForm.value = { paymentId: '', refundAmountYuan: 0, reason: '' }
  refundMaxFen.value = 0
}

async function submitRefundRequest() {
  if (!order.value?.orderId || !refundFormRef.value) return
  const valid = await refundFormRef.value.validate().catch(() => false)
  if (!valid) return

  const amountFen = Math.round(refundForm.value.refundAmountYuan * 100)
  if (amountFen < 1 || amountFen > refundMaxFen.value) {
    ElMessage.warning('退款金额超出可退范围')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认为支付单 ${refundForm.value.paymentId} 提交退款申请 ¥${formatMoneyFen(amountFen)}？\n\n提交后不会立即退款，须在「退款管理」审批通过后才会调用支付机构。`,
      '二次确认',
      { type: 'warning', confirmButtonText: '提交申请', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  refundSubmitting.value = true
  try {
    const result = await createOrderRefundRequest(order.value.orderId, {
      paymentId: refundForm.value.paymentId,
      refundAmount: amountFen,
      reason: refundForm.value.reason.trim(),
    })
    refundDialogVisible.value = false
    ElMessage.success(`已提交退款申请：${result.refundId}`)
    try {
      await ElMessageBox.confirm('是否前往退款管理进行审批？', '申请已提交', {
        confirmButtonText: '去审批',
        cancelButtonText: '留在此页',
        type: 'info',
      })
      router.push({ path: '/admin/refunds', query: { keyword: result.refundId } })
    } catch {
      /* 用户选择留在此页 */
    }
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '提交失败'
    ElMessage.error(msg)
  } finally {
    refundSubmitting.value = false
  }
}

watch(
  () => [props.orderId, props.active] as const,
  ([id, active]) => {
    if (active !== false && id) loadDetail(id)
    if (!id) {
      order.value = null
      payments.value = []
    }
  },
  { immediate: true },
)

onMounted(() => window.addEventListener('resize', syncViewport))
onUnmounted(() => window.removeEventListener('resize', syncViewport))
</script>

<style scoped>
.order-detail-panel {
  padding: 0 4px 16px;
}

.detail-section__hint {
  line-height: 1.5;
}
</style>
