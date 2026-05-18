<template>
  <motion.div
    :initial="{ opacity: 0, y: 8 }"
    :animate="{ opacity: 1, y: 0 }"
    class="space-y-4"
  >
    <motion.div
      :initial="{ opacity: 0 }"
      :animate="{ opacity: 1 }"
      class="bg-white rounded-xl p-5 card-shadow"
    >
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="商户号">
          <el-input v-model="queryForm.merchantId" placeholder="merchantId" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="queryForm.outcome" placeholder="全部" clearable style="width: 110px">
            <el-option label="全部" value="" />
            <el-option label="DENIED" value="DENIED" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因码">
          <el-select v-model="queryForm.reasonCode" placeholder="全部" clearable style="width: 110px">
            <el-option label="全部" value="" />
            <el-option label="5101" value="5101" />
            <el-option label="5102" value="5102" />
            <el-option label="5103" value="5103" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径">
          <el-input v-model="queryForm.requestPath" placeholder="请求路径" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </motion.div>

    <motion.div
      :initial="{ opacity: 0 }"
      :animate="{ opacity: 1 }"
      :transition="{ delay: 0.05 }"
      class="bg-white rounded-xl card-shadow"
    >
      <el-table v-loading="loading" :data="list" stripe size="small">
        <el-table-column label="时间" prop="createdAt" width="170" />
        <el-table-column label="商户" prop="merchantId" width="120" />
        <el-table-column label="声称商户" prop="targetMerchantId" width="120">
          <template #default="{ row }">
            <span class="text-xs">{{ row.targetMerchantId || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="认证" prop="authMode" width="72" align="center" />
        <el-table-column label="方法" prop="httpMethod" width="72" align="center" />
        <el-table-column label="路径" prop="requestPath" min-width="200">
          <template #default="{ row }">
            <span class="text-xs font-mono break-all">{{ row.requestPath }}</span>
          </template>
        </el-table-column>
        <el-table-column label="资源" width="140">
          <template #default="{ row }">
            <span class="text-xs">{{ row.resourceType || '—' }} {{ row.resourceId || '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="原因" prop="reasonCode" width="72" align="center" />
        <el-table-column label="IP" prop="clientIp" width="120" />
      </el-table>

      <motion.div
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        class="flex justify-end p-4"
      >
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </motion.div>
    </motion.div>
  </motion.div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { motion } from 'motion-v'
import { getSecurityAuditList, type SecurityAuditItem } from '@/api/securityAudit'

const loading = ref(false)
const list = ref<SecurityAuditItem[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const queryForm = reactive({
  page: 1,
  pageSize: 20,
  merchantId: '',
  outcome: '',
  reasonCode: '',
  requestPath: '',
})

async function loadData() {
  loading.value = true
  try {
    const [startDate, endDate] = dateRange.value ?? []
    const data = await getSecurityAuditList({
      page: queryForm.page,
      pageSize: queryForm.pageSize,
      merchantId: queryForm.merchantId || undefined,
      outcome: queryForm.outcome || undefined,
      reasonCode: queryForm.reasonCode || undefined,
      requestPath: queryForm.requestPath || undefined,
      startDate,
      endDate,
    })
    list.value = data.list
    total.value = data.total
  } catch (e: unknown) {
    const err = e as { message?: string }
    ElMessage.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.page = 1
  loadData()
}

function handleReset() {
  queryForm.page = 1
  queryForm.merchantId = ''
  queryForm.outcome = ''
  queryForm.reasonCode = ''
  queryForm.requestPath = ''
  dateRange.value = null
  loadData()
}

onMounted(() => loadData())
</script>
