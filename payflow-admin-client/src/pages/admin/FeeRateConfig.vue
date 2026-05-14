<template>
  <div>
    <div class="flex items-center justify-between mb-5">
      <h2 class="text-lg font-semibold text-[#0F172A]">阶梯费率配置</h2>
      <el-button type="primary" size="small" @click="openCreate">
        <el-icon class="mr-1"><Plus /></el-icon>新增规则
      </el-button>
    </div>

    <div class="content-card">
      <el-table :data="rules" v-loading="loading" size="small" class="data-table">
        <el-table-column label="适用范围" width="140">
          <template #default="{ row }">
            <el-tag size="small" :type="row.scopeType === 'global' ? 'info' : 'warning'">
              {{ row.scopeType === 'global' ? '全局默认' : row.scopeValue }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="渠道" prop="channelCode" width="90" />
        <el-table-column label="档位下限" width="110">
          <template #default="{ row }">
            ¥{{ ((Number(row.tierMin) || 0) / 100).toFixed(0) }}
          </template>
        </el-table-column>
        <el-table-column label="档位上限" width="110">
          <template #default="{ row }">
            {{ row.tierMax ? `¥${(Number(row.tierMax) / 100).toFixed(0)}` : '无上限' }}
          </template>
        </el-table-column>
        <el-table-column label="费率" width="90">
          <template #default="{ row }">
            <span class="font-medium">{{ Number(row.feeRate).toFixed(4) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="计算模式" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.calcMode === 'segmented' ? 'success' : ''">
              {{ row.calcMode === 'segmented' ? '分段累计' : '全额匹配' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" prop="priority" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'enabled'"
              size="small"
              @change="(val: boolean) => toggleRule(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="140">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑费率规则' : '新增费率规则'" width="520px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="适用范围">
          <el-radio-group v-model="form.scopeType">
            <el-radio value="global">全局默认</el-radio>
            <el-radio value="merchant_group">商户组</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.scopeType === 'merchant_group'" label="商户组名">
          <el-input v-model="form.scopeValue" placeholder="例如 VIP" />
        </el-form-item>
        <el-form-item label="适用渠道">
          <el-select v-model="form.channelCode" placeholder="选择渠道">
            <el-option label="全部渠道" value="ALL" />
            <el-option label="微信支付" value="wxpay" />
            <el-option label="支付宝" value="alipay" />
            <el-option label="银联" value="unionpay" />
          </el-select>
        </el-form-item>
        <el-form-item label="档位下限(元)">
          <el-input-number v-model="form.tierMinYuan" :min="0" :step="10000" :precision="0" />
        </el-form-item>
        <el-form-item label="档位上限(元)">
          <el-input-number v-model="form.tierMaxYuan" :min="0" :step="10000" :precision="0" />
          <span class="text-xs text-[#94a3b8] ml-2">留空为无上限</span>
        </el-form-item>
        <el-form-item label="费率">
          <el-input-number v-model="form.feeRate" :min="0" :max="1" :step="0.0001" :precision="4" />
          <span class="text-xs text-[#94a3b8] ml-2">例如 0.0060 = 0.6%</span>
        </el-form-item>
        <el-form-item label="计算模式">
          <el-radio-group v-model="form.calcMode">
            <el-radio value="flat">全额匹配</el-radio>
            <el-radio value="segmented">分段累计</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="dialogVisible = false">取消</el-button>
        <el-button size="small" type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getFeeRates, createFeeRate, updateFeeRate, deleteFeeRate } from '@/api/admin'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const rules = ref<any[]>([])

const defaultForm = () => ({
  scopeType: 'global',
  scopeValue: '',
  channelCode: 'ALL',
  tierMinYuan: 0,
  tierMaxYuan: 0,
  feeRate: 0.006,
  calcMode: 'flat',
  priority: 0,
})
const form = reactive(defaultForm())

async function loadRules() {
  loading.value = true
  try {
    rules.value = await getFeeRates()
  } catch {
    ElMessage.error('加载费率规则失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEdit(row: any) {
  isEdit.value = true
  form.scopeType = row.scopeType
  form.scopeValue = row.scopeValue || ''
  form.channelCode = row.channelCode
  form.tierMinYuan = Math.floor((Number(row.tierMin) || 0) / 100)
  form.tierMaxYuan = row.tierMax ? Math.floor(Number(row.tierMax) / 100) : 0
  form.feeRate = Number(row.feeRate)
  form.calcMode = row.calcMode || 'flat'
  form.priority = row.priority || 0
  ;(form as any).editId = row.id
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    const payload = {
      scopeType: form.scopeType,
      scopeValue: form.scopeType === 'global' ? 'ALL' : form.scopeValue,
      channelCode: form.channelCode,
      tierMin: form.tierMinYuan * 100,
      tierMax: form.tierMaxYuan > 0 ? form.tierMaxYuan * 100 : null,
      feeRate: form.feeRate,
      calcMode: form.calcMode,
      priority: form.priority,
      status: 'enabled',
    }
    if (isEdit.value) {
      await updateFeeRate((form as any).editId, payload)
      ElMessage.success('规则已更新')
    } else {
      await createFeeRate(payload)
      ElMessage.success('规则已创建')
    }
    dialogVisible.value = false
    loadRules()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

async function toggleRule(row: any, enabled: boolean) {
  try {
    await updateFeeRate(row.id, { ...row, status: enabled ? 'enabled' : 'disabled' })
    ElMessage.success(enabled ? '规则已启用' : '规则已停用')
    loadRules()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该费率规则？', '提示', { type: 'warning' })
    await deleteFeeRate(row.id)
    ElMessage.success('规则已删除')
    loadRules()
  } catch { /* 取消 */ }
}

loadRules()
</script>

<style scoped>
.content-card {
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid rgba(99, 102, 241, 0.08);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  padding: 24px;
}

.data-table :deep(th) {
  background: linear-gradient(90deg, rgba(99,102,241,0.06) 0%, rgba(129,140,248,0.04) 100%) !important;
  color: #374151;
  font-weight: 600;
  font-size: 13px;
  border-bottom: 2px solid rgba(99, 102, 241, 0.1) !important;
}
</style>
