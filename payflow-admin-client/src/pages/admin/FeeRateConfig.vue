<template>
  <div class="page-table-shell">
    <div class="content-card">
      <TableToolbar title="阶梯费率配置" :total="rules.length">
        <template #actions>
          <el-button type="primary" class="btn-primary" icon="Plus" @click="openCreate">新增规则</el-button>
        </template>
      </TableToolbar>

      <el-table
        table-layout="auto"
        :data="rules"
        v-loading="loading"
        stripe
        size="small"
        class="data-table"
        @row-click="openDetail"
      >
        <el-table-column label="适用范围" min-width="128" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" :type="row.scopeType === 'global' ? 'info' : 'warning'" effect="plain">
              {{ row.scopeType === 'global' ? '全局默认' : row.scopeValue }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="渠道" prop="channelCode" min-width="108">
          <template #default="{ row }">
            <el-tag size="small" :type="channelTagType(row.channelCode)" effect="plain">
              {{ row.channelCode === 'ALL' ? '全部渠道' : channelLabel(row.channelCode) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="档位下限" min-width="96" align="right">
          <template #default="{ row }">
            <span class="tabular-nums">¥{{ ((Number(row.tierMin) || 0) / 100).toFixed(0) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="档位上限" min-width="96" align="right">
          <template #default="{ row }">
            <span class="tabular-nums">{{ row.tierMax ? `¥${(Number(row.tierMax) / 100).toFixed(0)}` : '无上限' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="费率" min-width="88" align="right" class-name="col-amount">
          <template #default="{ row }">
            <span class="cell-amount tabular-nums">{{ formatRatePercent(row.feeRate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="计算模式" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.calcMode === 'segmented' ? 'success' : 'info'" effect="plain">
              {{ row.calcMode === 'segmented' ? '分段累计' : '全额匹配' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" prop="priority" min-width="72" align="center" />
        <el-table-column label="状态" min-width="88" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'enabled'"
              size="small"
              @click.stop
              @change="(val: boolean) => toggleRule(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" class-name="col-actions" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click.stop="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="费率规则详情" direction="rtl" size="480px" destroy-on-close>
      <template v-if="detailRow">
        <el-descriptions :column="1" border class="detail-descriptions">
          <el-descriptions-item label="规则 ID">{{ detailRow.id }}</el-descriptions-item>
          <el-descriptions-item label="适用范围">
            <el-tag size="small" :type="detailRow.scopeType === 'global' ? 'info' : 'warning'" effect="plain">
              {{ detailRow.scopeType === 'global' ? '全局默认' : detailRow.scopeValue }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="适用渠道">
            <el-tag size="small" :type="channelTagType(detailRow.channelCode)" effect="plain">
              {{ detailRow.channelCode === 'ALL' ? '全部渠道' : channelLabel(detailRow.channelCode) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="档位下限">
            ¥{{ ((Number(detailRow.tierMin) || 0) / 100).toFixed(0) }}
          </el-descriptions-item>
          <el-descriptions-item label="档位上限">
            {{ detailRow.tierMax ? `¥${(Number(detailRow.tierMax) / 100).toFixed(0)}` : '无上限' }}
          </el-descriptions-item>
          <el-descriptions-item label="费率">
            <span class="cell-amount">{{ formatRatePercent(detailRow.feeRate) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="计算模式">
            {{ detailRow.calcMode === 'segmented' ? '分段累计' : '全额匹配' }}
          </el-descriptions-item>
          <el-descriptions-item label="优先级">{{ detailRow.priority ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="detailRow.status === 'enabled' ? 'success' : 'info'" effect="plain">
              {{ detailRow.status === 'enabled' ? '已启用' : '已停用' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div class="mt-6 flex gap-2">
          <el-button type="primary" class="btn-primary" @click="openEditFromDetail">编辑</el-button>
          <el-button class="btn-outline" @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 新增/编辑抽屉 -->
    <el-drawer
      v-model="formVisible"
      :title="isEdit ? '编辑费率规则' : '新增费率规则'"
      direction="rtl"
      size="520px"
      destroy-on-close
    >
      <el-form :model="form" label-width="110px" class="pr-2">
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
          <el-select v-model="form.channelCode" placeholder="选择渠道" style="width: 100%">
            <el-option label="全部渠道" value="ALL" />
            <el-option label="微信支付" value="wxpay" />
            <el-option label="支付宝" value="alipay" />
            <el-option label="银联" value="unionpay" />
          </el-select>
        </el-form-item>
        <el-form-item label="档位下限(元)">
          <el-input-number v-model="form.tierMinYuan" :min="0" :step="10000" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="档位上限(元)">
          <el-input-number v-model="form.tierMaxYuan" :min="0" :step="10000" :precision="0" style="width: 100%" />
          <p class="page-hint-text mt-1">留空或填 0 表示无上限</p>
        </el-form-item>
        <el-form-item label="费率">
          <el-input-number v-model="form.feeRate" :min="0" :max="1" :step="0.0001" :precision="4" style="width: 100%" />
          <p class="page-hint-text mt-1">例如 0.0060 = 0.6%</p>
        </el-form-item>
        <el-form-item label="计算模式">
          <el-radio-group v-model="form.calcMode">
            <el-radio value="flat">全额匹配</el-radio>
            <el-radio value="segmented">分段累计</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2 px-4 pb-4">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" class="btn-primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFeeRates, createFeeRate, updateFeeRate, deleteFeeRate } from '@/api/admin'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import { channelLabel, channelTagType, formatRatePercent } from '@/utils/format'

interface FeeRateRule {
  id: number
  scopeType: string
  scopeValue?: string
  channelCode: string
  tierMin: number
  tierMax?: number | null
  feeRate: number
  calcMode: string
  priority: number
  status: string
}

const loading = ref(false)
const submitting = ref(false)
const detailVisible = ref(false)
const formVisible = ref(false)
const isEdit = ref(false)
const rules = ref<FeeRateRule[]>([])
const detailRow = ref<FeeRateRule | null>(null)

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
const form = reactive({
  ...defaultForm(),
  editId: undefined as number | undefined,
})

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

function openDetail(row: FeeRateRule) {
  detailRow.value = row
  detailVisible.value = true
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { ...defaultForm(), editId: undefined })
  formVisible.value = true
}

function openEdit(row: FeeRateRule) {
  isEdit.value = true
  form.scopeType = row.scopeType
  form.scopeValue = row.scopeValue || ''
  form.channelCode = row.channelCode
  form.tierMinYuan = Math.floor((Number(row.tierMin) || 0) / 100)
  form.tierMaxYuan = row.tierMax ? Math.floor(Number(row.tierMax) / 100) : 0
  form.feeRate = Number(row.feeRate)
  form.calcMode = row.calcMode || 'flat'
  form.priority = row.priority || 0
  form.editId = row.id
  formVisible.value = true
}

function openEditFromDetail() {
  if (!detailRow.value) return
  detailVisible.value = false
  openEdit(detailRow.value)
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
    if (isEdit.value && form.editId != null) {
      await updateFeeRate(form.editId, payload)
      ElMessage.success('规则已更新')
    } else {
      await createFeeRate(payload)
      ElMessage.success('规则已创建')
    }
    formVisible.value = false
    loadRules()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

async function toggleRule(row: FeeRateRule, enabled: boolean) {
  try {
    await updateFeeRate(row.id, { ...row, status: enabled ? 'enabled' : 'disabled' })
    ElMessage.success(enabled ? '规则已启用' : '规则已停用')
    loadRules()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDelete(row: FeeRateRule) {
  try {
    await ElMessageBox.confirm('确认删除该费率规则？', '提示', { type: 'warning' })
    await deleteFeeRate(row.id)
    ElMessage.success('规则已删除')
    if (detailRow.value?.id === row.id) {
      detailVisible.value = false
    }
    loadRules()
  } catch {
    /* 取消 */
  }
}

loadRules()
</script>
