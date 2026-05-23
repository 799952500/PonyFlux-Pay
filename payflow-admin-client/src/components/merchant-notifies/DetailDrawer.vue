<template>
  <el-drawer
    v-model="visible"
    title="回调详情"
    direction="rtl"
    size="640px"
    destroy-on-close
    class="pf-merchant-notify-drawer"
    @closed="handleClosed"
  >
    <div v-loading="loading">
      <template v-if="detail">
        <div class="content-card detail-card mb-4">
          <h3 class="detail-section__title">回调汇总</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="回调 ID">{{ detail.summary.notifyId }}</el-descriptions-item>
            <el-descriptions-item label="订单号">{{ detail.summary.orderId }}</el-descriptions-item>
            <el-descriptions-item label="商户订单号">{{ detail.summary.merchantOrderNo || '—' }}</el-descriptions-item>
            <el-descriptions-item label="商户号">{{ detail.summary.merchantId }}</el-descriptions-item>
            <el-descriptions-item label="回调类型">{{ notifyTypeLabel(detail.summary.notifyType) }}</el-descriptions-item>
            <el-descriptions-item label="汇总状态">
              <el-tag size="small" :type="summaryTagType(detail.summary.summaryStatus)">
                {{ summaryStatusLabel(detail.summary.summaryStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="平台订单状态">{{ detail.summary.orderStatus || '—' }}</el-descriptions-item>
            <el-descriptions-item label="通知报文状态">{{ detail.summary.notifyPayloadStatus || '—' }}</el-descriptions-item>
            <el-descriptions-item label="回调地址">{{ detail.summary.notifyUrl || '—' }}</el-descriptions-item>
            <el-descriptions-item label="尝试次数">{{ detail.summary.attemptCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="最近尝试">{{ formatDateTime(detail.summary.lastAttemptAt) }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.summary.lastFailReason" label="最近失败">
              {{ detail.summary.lastFailReason }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="content-card detail-card">
          <h3 class="detail-section__title">尝试明细</h3>
          <el-empty v-if="!detail.attempts.length" description="暂无 HTTP 尝试记录" />
          <el-timeline v-else>
            <el-timeline-item
              v-for="item in detail.attempts"
              :key="item.attemptNo"
              :timestamp="formatDateTime(item.createdAt)"
              placement="top"
            >
              <div class="attempt-card">
                <div class="attempt-card__head">
                  <span>第 {{ item.attemptNo }} 次</span>
                  <el-tag size="small" :type="item.resultStatus === 'SUCCESS' ? 'success' : item.resultStatus === 'FAILED' ? 'danger' : 'info'">
                    {{ item.resultStatus }}
                  </el-tag>
                  <span v-if="item.durationMs != null" class="text-xs text-gray-500">{{ item.durationMs }} ms</span>
                  <span v-if="item.httpStatus != null" class="text-xs text-gray-500">HTTP {{ item.httpStatus }}</span>
                </div>
                <p v-if="item.failReasonDetail" class="text-xs text-red-600 mt-1">{{ item.failReasonDetail }}</p>
                <div class="mt-2">
                  <p class="text-xs font-medium text-gray-600">请求参数</p>
                  <pre class="attempt-pre">{{ formatJson(item.requestParams) }}</pre>
                </div>
                <div class="mt-2">
                  <p class="text-xs font-medium text-gray-600">商户响应</p>
                  <pre class="attempt-pre">{{ item.responseBody || '—' }}</pre>
                  <p v-if="item.truncated" class="text-xs text-amber-600 mt-1">报文已截断存储</p>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>
      <el-empty v-else-if="!loading" description="未找到回调记录" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getMerchantNotifyDetail,
  getMerchantNotifyByOrder,
  type MerchantNotifyDetailResponse,
} from '@/api/merchantNotify'
import { formatDateTime } from '@/utils/format'

const visible = ref(false)
const loading = ref(false)
const detail = ref<MerchantNotifyDetailResponse | null>(null)

function summaryStatusLabel(status: string) {
  const map: Record<string, string> = {
    NOT_CONFIGURED: '未配置',
    PENDING: '待投递',
    IN_PROGRESS: '处理中',
    SUCCESS: '成功',
    FAILED: '失败',
  }
  return map[status] || status
}

function summaryTagType(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'NOT_CONFIGURED') return 'info'
  return 'warning'
}

function notifyTypeLabel(type: string) {
  return type === 'REFUND' ? '退款通知' : '支付通知'
}

function formatJson(payload: Record<string, unknown> | string) {
  if (typeof payload === 'string') return payload
  return JSON.stringify(payload, null, 2)
}

async function openByNotifyId(notifyId: string) {
  visible.value = true
  loading.value = true
  detail.value = null
  try {
    detail.value = await getMerchantNotifyDetail(notifyId)
  } catch {
    ElMessage.error('加载回调详情失败')
  } finally {
    loading.value = false
  }
}

async function openByOrderId(orderId: string, notifyType = 'PAYMENT') {
  visible.value = true
  loading.value = true
  detail.value = null
  try {
    const byOrder = await getMerchantNotifyByOrder(orderId, notifyType)
    const first = byOrder.summaries?.[0]
    if (!first?.notifyId) {
      detail.value = null
      return
    }
    detail.value = await getMerchantNotifyDetail(first.notifyId)
  } catch {
    ElMessage.error('加载商户回调失败')
  } finally {
    loading.value = false
  }
}

function handleClosed() {
  detail.value = null
}

defineExpose({ openByNotifyId, openByOrderId })
</script>

<style scoped>
.attempt-card__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.attempt-pre {
  margin: 4px 0 0;
  padding: 8px;
  font-size: 12px;
  line-height: 1.4;
  background: var(--pf-muted-bg, #f5f5f5);
  border-radius: 6px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
