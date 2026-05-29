<template>
  <div>
    <div class="flex justify-between mb-3">
      <span class="text-sm text-slate-600">订阅对账日报/周报，生成后将在通知中心提醒</span>
      <el-button type="primary" size="small" @click="openCreate">新增订阅</el-button>
    </div>
    <el-table :data="list" v-loading="loading" size="small" stripe>
      <el-table-column label="类型" prop="reportType" width="100" />
      <el-table-column label="范围" prop="scope" width="140" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? 'ON' : 'OFF' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上次发送" prop="lastSentAt" min-width="160" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="danger" size="small" @click="remove(row.id!)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增订阅" width="420px">
      <el-form label-width="90px">
        <el-form-item label="报告类型">
          <el-select v-model="form.reportType">
            <el-option label="日报" value="DAILY" />
            <el-option label="周报" value="WEEKLY" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.scope">
            <el-option label="我负责的" value="OWNED" />
            <el-option label="授权范围全部" value="ALL_AUTHORIZED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createReconSubscription,
  deleteReconSubscription,
  getReconSubscriptions,
  type ReconReportSubscription,
} from '@/api/admin'

const loading = ref(false)
const list = ref<ReconReportSubscription[]>([])
const dialogVisible = ref(false)
const form = reactive({ reportType: 'WEEKLY', scope: 'OWNED' })

const load = async () => {
  loading.value = true
  try {
    list.value = await getReconSubscriptions()
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  form.reportType = 'WEEKLY'
  form.scope = 'OWNED'
  dialogVisible.value = true
}

const save = async () => {
  await createReconSubscription({ ...form, enabled: true })
  dialogVisible.value = false
  ElMessage.success('订阅成功')
  await load()
}

const remove = async (id: number) => {
  await deleteReconSubscription(id)
  ElMessage.success('已取消订阅')
  await load()
}

load()
</script>
