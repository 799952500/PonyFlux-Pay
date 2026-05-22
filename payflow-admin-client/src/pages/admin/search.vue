<template>
  <div class="page-table-shell">
    <div class="filter-bar">
      <el-form :inline="true" @submit.prevent="runSearch">
        <el-form-item label="关键词">
          <el-input
            v-model="keyword"
            placeholder="订单号 / 商户订单号"
            clearable
            style="width: 280px"
            @keyup.enter="runSearch"
          />
        </el-form-item>
        <el-form-item label="条数">
          <el-input-number v-model="limit" :min="1" :max="50" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" icon="Search" @click="runSearch">搜索</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <TableToolbar title="搜索结果" :total="hits.length" />

      <el-table table-layout="auto" v-loading="loading" :data="hits" stripe size="small" class="data-table" @row-click="openOrder">
        <el-table-column label="订单号" prop="orderId" min-width="170">
          <template #default="{ row }">
            <span
              :data-flip="`order-${row.orderId}`"
              class="cell-mono pf-link cursor-pointer"
            >#{{ row.orderId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商户" prop="merchantId" width="140" />
        <el-table-column label="商户订单号" prop="merchantOrderNo" min-width="140" />
        <el-table-column label="金额（元）" prop="amount" width="110" align="right" class-name="col-amount">
          <template #default="{ row }">
            <span class="cell-amount">¥{{ formatMoneyFen(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="tagTypeOf(ORDER_STATUS_TAG, row.status)">
              {{ labelOf(ORDER_STATUS_LABEL, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" min-width="168" class-name="col-datetime" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-xs text-slate-600 tabular-nums">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && searched && !hits.length" description="无匹配订单" class="py-10" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOrderDetailOverlay } from '@/composables/useOrderDetailOverlay'
import { ElMessage } from 'element-plus'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import { adminSearchOrders } from '@/api/admin'
import type { AdminSearchOrderHit } from '@/types'
import {
  formatDateTime,
  formatMoneyFen,
  labelOf,
  ORDER_STATUS_LABEL,
  ORDER_STATUS_TAG,
  tagTypeOf,
} from '@/utils/format'

const route = useRoute()
const router = useRouter()
const { open: openOrderDetail } = useOrderDetailOverlay()
const keyword = ref(String(route.query.q ?? ''))
const limit = ref(20)
const loading = ref(false)
const hits = ref<AdminSearchOrderHit[]>([])
const searched = ref(false)

async function runSearch() {
  const q = keyword.value.trim()
  if (!q) {
    ElMessage.warning('请输入关键词')
    return
  }
  loading.value = true
  searched.value = true
  try {
    hits.value = await adminSearchOrders(q, limit.value)
    router.replace({ path: '/admin/search', query: { q } })
  } catch {
    ElMessage.error('搜索失败')
    hits.value = []
  } finally {
    loading.value = false
  }
}

function openOrder(row: AdminSearchOrderHit) {
  openOrderDetail(row.orderId)
}

watch(
  () => route.query.q,
  (q) => {
    if (q != null && String(q) !== keyword.value) {
      keyword.value = String(q)
    }
  },
)

onMounted(() => {
  if (keyword.value.trim()) runSearch()
})
</script>
