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
    </template>

    <el-empty v-else class="detail-empty" description="未找到该订单" />

    <MerchantNotifyDetailDrawer ref="notifyDrawerRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import MerchantNotifyDetailDrawer from '@/components/merchant-notifies/DetailDrawer.vue'
import { ElMessage } from 'element-plus'
import { getOrderDetail } from '@/api/admin'
import { useMerchantScope } from '@/composables/useMerchantScope'
import type { Order } from '@/types'
import {
  channelLabel,
  channelTagType,
  formatDateTime,
  formatMoneyFen,
  labelOf,
  ORDER_STATUS_LABEL,
  ORDER_STATUS_TAG,
  tagTypeOf,
} from '@/utils/format'

const props = defineProps<{
  orderId: string | null
  active?: boolean
}>()

const { isMerchantAllowed } = useMerchantScope()
const loading = ref(false)
const order = ref<Order | null>(null)
const notifyDrawerRef = ref<InstanceType<typeof MerchantNotifyDetailDrawer> | null>(null)

function openMerchantNotify() {
  if (!order.value?.orderId) return
  notifyDrawerRef.value?.openByOrderId(order.value.orderId)
}
const viewportWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1280)

const descColumn = computed(() => (viewportWidth.value < 768 ? 1 : 2))

function syncViewport() {
  viewportWidth.value = window.innerWidth
}

async function loadDetail(id: string) {
  loading.value = true
  order.value = null
  try {
    const detail = await getOrderDetail(id)
    if (detail && !isMerchantAllowed(detail.merchantId)) {
      order.value = null
      ElMessage.warning('无权查看该订单')
    } else {
      order.value = detail
    }
  } catch {
    order.value = null
    ElMessage.error('加载订单详情失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.orderId, props.active] as const,
  ([id, active]) => {
    if (active !== false && id) loadDetail(id)
    if (!id) order.value = null
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
</style>
