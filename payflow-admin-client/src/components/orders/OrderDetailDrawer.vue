<template>
  <el-drawer
    v-model="visible"
    title="订单详情"
    direction="rtl"
    size="520px"
    destroy-on-close
    class="pf-order-detail-drawer"
    @closed="close"
  >
    <OrderDetailPanel :order-id="orderId" :active="visible" />
    <template #footer>
      <el-button class="btn-outline" @click="close">关闭</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import OrderDetailPanel from '@/components/orders/OrderDetailPanel.vue'
import { useOrderDetailOverlay } from '@/composables/useOrderDetailOverlay'

const { visible, orderId, close } = useOrderDetailOverlay()
</script>
