<template>
  <div class="page-table-shell">
    <div class="content-card">
      <TableToolbar title="SLA 规则" :total="rules.length" hint="按差异类型配置处理时限与升级策略；修改仅对新工单生效">
        <template #actions>
          <el-button class="btn-outline" icon="Refresh" @click="load">刷新</el-button>
        </template>
      </TableToolbar>

      <el-table
        table-layout="auto"
        v-loading="loading"
        :data="rules"
        stripe
        size="small"
        class="data-table"
        empty-text="暂无 SLA 规则"
      >
        <el-table-column label="差异类型" prop="diffType" min-width="140">
          <template #default="{ row }">
            <el-tag size="small" type="warning" effect="plain">
              {{ labelOf(RECON_DIFF_LABEL, row.diffType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="88" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="SLA(小时)" prop="slaHours" width="110" align="right">
          <template #default="{ row }">
            <span class="tabular-nums">{{ row.slaHours }}</span>
          </template>
        </el-table-column>
        <el-table-column label="临近提醒比例" width="130" align="right">
          <template #default="{ row }">
            <span class="tabular-nums">{{ ((row.dueSoonRatio ?? 0) * 100).toFixed(0) }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="升级角色" prop="escalateToRole" min-width="130" show-overflow-tooltip />
        <el-table-column label="更新人" prop="updatedBy" width="110" show-overflow-tooltip />
        <el-table-column label="更新时间" prop="updatedAt" min-width="168" class-name="col-datetime" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-datetime">{{ formatDateTime(row.updatedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="88" fixed="right" class-name="col-actions">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="编辑 SLA 规则" width="520px" destroy-on-close>
      <el-form label-width="128px" class="pr-2">
        <el-form-item label="差异类型">
          <el-input :model-value="labelOf(RECON_DIFF_LABEL, form.diffType)" disabled />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="SLA(小时)">
          <el-input-number v-model="form.slaHours" :min="1" :max="720" class="w-full" />
        </el-form-item>
        <el-form-item label="临近提醒比例">
          <el-slider v-model="dueSoonPercent" :min="5" :max="95" :step="5" show-input />
        </el-form-item>
        <el-form-item label="升级角色">
          <el-input v-model="form.escalateToRole" placeholder="如 recon:manage" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="btn-outline" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import type { ReconSlaRule } from '@/api/admin'
import { getReconSlaRules, saveReconSlaRule } from '@/api/admin'
import { RECON_DIFF_LABEL, formatDateTime, labelOf } from '@/utils/format'

const loading = ref(false)
const rules = ref<ReconSlaRule[]>([])
const dialogVisible = ref(false)
const form = reactive<ReconSlaRule>({
  diffType: '',
  enabled: true,
  slaHours: 24,
  dueSoonRatio: 0.2,
  escalateToRole: 'recon:manage',
})

const dueSoonPercent = computed({
  get: () => Math.round((form.dueSoonRatio ?? 0.2) * 100),
  set: (v: number) => {
    form.dueSoonRatio = v / 100
  },
})

const load = async () => {
  loading.value = true
  try {
    rules.value = await getReconSlaRules()
  } finally {
    loading.value = false
  }
}

const openEdit = (row: ReconSlaRule) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const save = async () => {
  await saveReconSlaRule(form.diffType, {
    enabled: form.enabled,
    slaHours: form.slaHours,
    dueSoonRatio: form.dueSoonRatio,
    escalateToRole: form.escalateToRole,
  })
  dialogVisible.value = false
  ElMessage.success('保存成功')
  await load()
}

load()
</script>
