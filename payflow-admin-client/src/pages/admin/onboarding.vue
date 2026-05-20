<template>
  <div class="content-card">
    <h2 class="text-lg font-semibold mb-4">商户进件审核</h2>
    <el-table :data="list" stripe size="small" class="data-table" v-loading="loading">
      <el-table-column prop="applicationNo" label="申请单号" width="160" />
      <el-table-column prop="merchantName" label="商户名称" min-width="140" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="contactPhone" label="联系电话" width="120" />
      <el-table-column prop="createdAt" label="创建时间" width="170" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listOnboardingApplications } from '@/api/admin'

const loading = ref(false)
const list = ref<unknown[]>([])

onMounted(async () => {
  loading.value = true
  try {
    list.value = (await listOnboardingApplications()) as unknown[]
  } catch (e: any) {
    ElMessage.error(e?.message || '加载进件列表失败')
  } finally {
    loading.value = false
  }
})
</script>
