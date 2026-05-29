<template>
  <div class="page-table-shell">
    <div class="content-card" v-loading="loading">
      <el-alert v-if="loadError" type="error" :title="loadError" show-icon class="mb-4">
        <template #default>
          <el-button type="primary" link @click="load">重试</el-button>
        </template>
      </el-alert>

      <div class="flex items-center justify-between mb-4">
        <div>
          <p class="dashboard-section-title m-0">对账报告详情</p>
          <p v-if="report" class="text-xs text-slate-500 mt-1 cell-mono">{{ report.snapshotId }}</p>
        </div>
        <el-button class="btn-outline" icon="Back" @click="router.back()">返回</el-button>
      </div>

      <template v-if="report">
        <el-descriptions :column="2" border size="small" class="mb-4">
          <el-descriptions-item label="报告类型">
            <el-tag size="small">{{ report.reportType === 'WEEKLY' ? '周报' : '日报' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="生成时间">
            <span class="tabular-nums text-xs">{{ formatDateTime(report.generatedAt) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="统计周期" :span="2">
            <span class="tabular-nums text-xs">{{ report.periodStart }} ~ {{ report.periodEnd }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <p class="dashboard-section-title mb-2">报告数据</p>
        <div class="report-payload-wrap">
          <pre class="report-payload">{{ payloadText }}</pre>
        </div>
      </template>

      <el-empty v-else-if="!loading && !loadError" description="报告不存在或已被删除" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getReconReportDetail } from '@/api/admin'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadError = ref<string | null>(null)
const report = ref<any>(null)

const payloadText = computed(() =>
  report.value?.payload ? JSON.stringify(report.value.payload, null, 2) : '{}',
)

const load = async () => {
  const id = String(route.params.snapshotId ?? '')
  if (!id) {
    loadError.value = '缺少报告 ID'
    return
  }
  loading.value = true
  loadError.value = null
  try {
    report.value = await getReconReportDetail(id)
    if (!report.value) {
      loadError.value = '未找到报告详情'
    }
  } catch {
    report.value = null
    loadError.value = '加载报告详情失败'
    ElMessage.warning('加载报告详情失败')
  } finally {
    loading.value = false
  }
}

load()
</script>

<style scoped>
.report-payload-wrap {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
  padding: 16px;
  max-height: 520px;
  overflow: auto;
}
.report-payload {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
