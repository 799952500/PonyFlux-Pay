<template>
  <div class="p-6">
    <div class="flex items-center gap-3 mb-6">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h2 class="text-gray-800 font-bold text-lg m-0">订单详情</h2>
    </div>

    <div v-if="loading" class="space-y-4">
      <el-skeleton animated :rows="8" />
    </div>

    <div v-else-if="order" class="space-y-6">
      <div class="bg-white rounded-xl card-shadow p-6">
        <h3 class="text-sm font-semibold text-gray-700 mb-4 border-b pb-2">基本信息</h3>
        <dl class="grid grid-cols-2 gap-y-3 gap-x-4 text-sm">
          <dt class="text-gray-400">订单号</dt><dd class="text-gray-800 font-medium tabular-nums">{{ order.orderId }}</dd>
          <dt class="text-gray-400">商户订单号</dt><dd class="text-gray-800 tabular-nums">{{ order.merchantOrderNo }}</dd>
          <dt class="text-gray-400">商户ID</dt><dd class="text-gray-800">{{ order.merchantId }}</dd>
          <dt class="text-gray-400">商品</dt><dd class="text-gray-800">{{ order.subject }}</dd>
          <dt class="text-gray-400">金额</dt><dd class="text-gray-800 font-bold">¥{{ (order.amount / 100).toFixed(2) }}</dd>
        </dl>
      </div>
      <div class="bg-white rounded-xl card-shadow p-6">
        <h3 class="text-sm font-semibold text-gray-700 mb-4 border-b pb-2">支付信息</h3>
        <dl class="grid grid-cols-2 gap-y-3 gap-x-4 text-sm">
          <dt class="text-gray-400">渠道</dt><dd>{{ order.channel }}</dd>
          <dt class="text-gray-400">状态</dt><dd><el-tag size="small" :type="statusTypeMap[order.status]">{{ statusLabelMap[order.status] }}</el-tag></dd>
          <dt class="text-gray-400">创建时间</dt><dd class="text-gray-800">{{ order.createdAt }}</dd>
          <dt class="text-gray-400">过期时间</dt><dd class="text-gray-800">{{ order.expireTime }}</dd>
        </dl>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderDetail } from '@/api/admin'
import type { Order } from '@/types'

const route = useRoute()
const loading = ref(false)
const order = ref<Order | null>(null)

const statusTypeMap: Record<string, string> = { PAID: 'success', PAYING: 'warning', CREATED: 'info', EXPIRED: 'info', FAILED: 'danger' }
const statusLabelMap: Record<string, string> = { PAID: '已支付', PAYING: '支付中', CREATED: '待支付', EXPIRED: '已过期', FAILED: '失败' }

onMounted(async () => {
  loading.value = true
  try {
    order.value = await getOrderDetail(route.params.orderId as string)
  } catch { ElMessage.error('加载订单详情失败') }
  finally { loading.value = false }
})
</script>
