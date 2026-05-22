<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    direction="rtl"
    size="85%"
    destroy-on-close
    class="pf-merchant-insight-drawer"
    @closed="close"
  >
    <MerchantInsight
      v-if="merchantId"
      :merchant-id="merchantId"
      embedded
      :active="visible"
    />
    <template #footer>
      <el-button class="btn-outline" @click="close">关闭</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MerchantInsight from '@/pages/admin/MerchantInsight.vue'
import { useMerchantInsightOverlay } from '@/composables/useMerchantInsightOverlay'

const { visible, merchantId, close } = useMerchantInsightOverlay()
const drawerTitle = computed(() =>
  merchantId.value ? `商户洞察 — ${merchantId.value}` : '商户洞察',
)
</script>
