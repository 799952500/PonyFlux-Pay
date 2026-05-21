<template>
  <div class="page-table-shell">
    <el-tabs v-model="activeTab" class="risk-tabs">
      <el-tab-pane label="风控规则" name="rules" />
      <el-tab-pane label="命中记录" name="hits" />
    </el-tabs>

    <!-- 规则列表 -->
    <template v-if="activeTab === 'rules'">
      <div class="filter-bar">
        <el-form :inline="true" :model="queryForm" size="default">
          <el-form-item label="关键词">
            <el-input
              v-model="queryForm.keyword"
              placeholder="规则编码 / 名称"
              clearable
              style="width: 160px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="规则来源">
            <el-select v-model="queryForm.ownerType" placeholder="全部" clearable style="width: 130px">
              <el-option label="平台规则" value="PLATFORM" />
              <el-option label="商户自建" value="MERCHANT" />
            </el-select>
          </el-form-item>
          <el-form-item label="作用范围">
            <el-select v-model="queryForm.scopeType" placeholder="全部" clearable style="width: 150px">
              <el-option label="全部商户" value="ALL_MERCHANTS" />
              <el-option label="指定商户" value="SELECTED_MERCHANTS" />
              <el-option label="仅归属商户" value="OWNER_MERCHANT_ONLY" />
            </el-select>
          </el-form-item>
          <el-form-item label="商户">
            <el-select
              v-model="queryForm.merchantId"
              placeholder="全部商户"
              clearable
              filterable
              style="width: 180px"
            >
              <el-option
                v-for="m in merchantOptions"
                :key="m.merchantId"
                :label="`${m.merchantName} (${m.merchantId})`"
                :value="m.merchantId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="规则类型">
            <el-select v-model="queryForm.ruleType" placeholder="全部" clearable style="width: 140px">
              <el-option v-for="(label, key) in ruleTypeLabel" :key="key" :label="label" :value="key" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryForm.enabled" placeholder="全部" clearable style="width: 100px">
              <el-option label="启用" :value="true" />
              <el-option label="停用" :value="false" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
            <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="content-card">
        <TableToolbar title="风控规则（全平台）" :total="total">
          <template #actions>
            <el-tooltip v-if="!canManagePlatform" content="仅风控/管理员角色可新增平台规则" placement="top">
              <span>
                <el-button type="primary" class="btn-primary" icon="Plus" disabled>新增平台规则</el-button>
              </span>
            </el-tooltip>
            <el-button v-else type="primary" class="btn-primary" icon="Plus" @click="openCreate">新增平台规则</el-button>
          </template>
        </TableToolbar>

        <p class="text-xs text-gray-500 mb-3 px-1">
          平台规则可作用于全部商户或指定商户；商户自建规则仅对归属商户生效，管理员可查看与启停，不可修改规则内容。
        </p>

        <el-table v-loading="loading" :data="ruleList" stripe size="small" class="data-table">
          <el-table-column label="规则名称" min-width="160">
            <template #default="{ row }">
              <div class="font-medium text-gray-800">{{ row.ruleName }}</div>
              <div class="text-xs text-gray-400 font-mono">{{ row.ruleCode }}</div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="(ruleTypeTag[row.ruleType] as any) ?? 'info'" effect="plain">
                {{ ruleTypeLabel[row.ruleType] ?? row.ruleType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="规则来源" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="row.ownerType === 'PLATFORM' ? 'primary' : 'warning'" effect="light">
                {{ row.ownerType === 'PLATFORM' ? '平台' : '商户自建' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="归属 / 作用范围" min-width="180">
            <template #default="{ row }">
              <div v-if="row.ownerType === 'MERCHANT'" class="text-sm">
                <span class="text-gray-600">{{ row.ownerMerchantName || row.ownerMerchantId }}</span>
                <el-tag size="small" type="info" effect="plain" class="ml-1">仅本商户</el-tag>
              </div>
              <div v-else class="text-sm">
                <el-tag size="small" :type="scopeTagType(row.scopeType)" effect="plain">
                  {{ scopeLabel(row) }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="阈值" width="130">
            <template #default="{ row }">
              <span class="text-sm tabular-nums font-medium">{{ formatThreshold(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="动作" width="88" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="actionTag[row.action] ?? 'info'" effect="plain">
                {{ actionLabel[row.action] ?? row.action }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="优先级" prop="priority" width="72" align="center" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.enabled"
                size="small"
                :disabled="!canToggle(row)"
                @change="(val: boolean) => handleToggle(row, val)"
              />
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="168">
            <template #default="{ row }">
              <span class="text-xs text-slate-600 tabular-nums">{{ formatDateTime(row.updatedAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.ownerType === 'PLATFORM' && canManagePlatform"
                link
                type="primary"
                size="small"
                @click="openEdit(row)"
              >编辑</el-button>
              <el-button v-else link type="primary" size="small" @click="openView(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>

        <AdminPagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.pageSize"
          :total="total"
          @size-change="loadRules"
          @current-change="loadRules"
        />
      </div>
    </template>

    <!-- 命中记录 -->
    <template v-else>
      <div class="filter-bar">
        <el-form :inline="true" :model="hitQuery" size="default">
          <el-form-item label="商户">
            <el-select v-model="hitQuery.merchantId" placeholder="全部" clearable filterable style="width: 180px">
              <el-option
                v-for="m in merchantOptions"
                :key="m.merchantId"
                :label="`${m.merchantName} (${m.merchantId})`"
                :value="m.merchantId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="决策">
            <el-select v-model="hitQuery.decision" placeholder="全部" clearable style="width: 130px">
              <el-option label="拒绝" value="REJECTED" />
              <el-option label="人工审核" value="REVIEW_REQUIRED" />
              <el-option label="告警" value="WARN_ONLY" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="btn-primary" icon="Search" @click="loadHits">查询</el-button>
          </el-form-item>
        </el-form>
      </div>
      <div class="content-card">
        <TableToolbar title="风控命中记录" :total="hitTotal" />
        <el-table v-loading="hitLoading" :data="hitList" stripe size="small" class="data-table">
          <el-table-column label="时间" prop="createdAt" width="168">
            <template #default="{ row }">
              <span class="text-xs tabular-nums">{{ formatDateTime(row.createdAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="商户" min-width="140">
            <template #default="{ row }">
              <div class="text-sm">{{ row.merchantName || row.merchantId }}</div>
              <div class="text-xs text-gray-400">{{ row.merchantId }}</div>
            </template>
          </el-table-column>
          <el-table-column label="规则" min-width="140">
            <template #default="{ row }">
              <div class="text-sm">{{ row.ruleName }}</div>
              <div class="text-xs text-gray-400">{{ row.ruleCode }}</div>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="88">
            <template #default="{ row }">
              {{ row.ownerType === 'PLATFORM' ? '平台' : '商户' }}
            </template>
          </el-table-column>
          <el-table-column label="决策" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="decisionTag[row.decision] ?? 'info'" effect="plain">
                {{ decisionLabel[row.decision] ?? row.decision }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="原因" prop="hitReason" min-width="160" show-overflow-tooltip />
        </el-table>
        <AdminPagination
          v-model:current-page="hitQuery.page"
          v-model:page-size="hitQuery.pageSize"
          :total="hitTotal"
          @size-change="loadHits"
          @current-change="loadHits"
        />
      </div>
    </template>

    <!-- 平台规则编辑 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增平台风控规则' : '编辑平台风控规则'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px" size="default">
        <el-form-item label="规则编码" prop="ruleCode">
          <el-input v-model="formData.ruleCode" placeholder="如 RISK_AMT_SINGLE_XXX" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="formData.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="formData.ruleType" style="width: 100%" @change="onRuleTypeChange">
            <el-option v-for="(label, key) in ruleTypeLabel" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="作用范围" prop="scopeType">
          <el-radio-group v-model="formData.scopeType">
            <el-radio value="ALL_MERCHANTS">全部商户</el-radio>
            <el-radio value="SELECTED_MERCHANTS">指定商户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="formData.scopeType === 'SELECTED_MERCHANTS'" label="适用商户" prop="scopeMerchantIds">
          <el-select
            v-model="formData.scopeMerchantIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择适用商户"
            style="width: 100%"
          >
            <el-option
              v-for="m in merchantOptions"
              :key="m.merchantId"
              :label="`${m.merchantName} (${m.merchantId})`"
              :value="m.merchantId"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="thresholdInputLabel" prop="thresholdDisplay">
          <el-input-number v-model="formData.thresholdDisplay" :min="1" :max="999999999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="命中动作" prop="action">
          <el-select v-model="formData.action" style="width: 100%">
            <el-option label="拒绝支付" value="REJECT" />
            <el-option label="人工审核" value="REVIEW" />
            <el-option label="仅告警" value="WARN" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="formData.priority" :min="0" :max="9999" style="width: 100%" />
          <p class="text-xs text-gray-400 mt-1">数值越小越先评估</p>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 商户规则只读查看 -->
    <el-dialog v-model="viewVisible" title="风控规则详情" width="520px" destroy-on-close>
      <el-descriptions v-if="viewRule" :column="1" border size="small">
        <el-descriptions-item label="规则名称">{{ viewRule.ruleName }}</el-descriptions-item>
        <el-descriptions-item label="规则编码">{{ viewRule.ruleCode }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ ruleTypeLabel[viewRule.ruleType] }}</el-descriptions-item>
        <el-descriptions-item label="来源">商户自建</el-descriptions-item>
        <el-descriptions-item label="归属商户">
          {{ viewRule.ownerMerchantName || viewRule.ownerMerchantId }}
        </el-descriptions-item>
        <el-descriptions-item label="阈值">{{ formatThreshold(viewRule) }}</el-descriptions-item>
        <el-descriptions-item label="动作">{{ actionLabel[viewRule.action] }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ viewRule.priority }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ viewRule.enabled ? '启用' : '停用' }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ viewRule.description || '—' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import {
  createRiskRule,
  getMerchantsSimple,
  getRiskHitRecords,
  getRiskRuleScopes,
  getRiskRules,
  updateRiskRule,
  updateRiskRuleStatus,
} from '@/api/admin'
import { useAdminStore } from '@/stores/admin'
import type {
  RiskHitRecord,
  RiskRule,
  RiskRuleAction,
  RiskRuleScopeType,
  RiskRuleType,
  RiskRuleUpsertRequest,
} from '@/types'

const adminStore = useAdminStore()
const activeTab = ref<'rules' | 'hits'>('rules')

const canManagePlatform = computed(() => {
  const role = adminStore.user?.role ?? ''
  return role === 'SUPER_ADMIN' || role === 'ADMIN' || role === 'RISK'
})

function canToggle(row: RiskRule) {
  if (row.ownerType === 'PLATFORM') return canManagePlatform.value
  return canManagePlatform.value
}

const loading = ref(false)
const ruleList = ref<RiskRule[]>([])
const total = ref(0)
const merchantOptions = ref<Array<{ merchantId: string; merchantName: string }>>([])

const queryForm = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  ownerType: '' as '' | 'PLATFORM' | 'MERCHANT',
  scopeType: '' as '' | RiskRuleScopeType,
  merchantId: '',
  ruleType: '' as '' | RiskRuleType,
  enabled: undefined as boolean | undefined,
})

const ruleTypeLabel: Record<string, string> = {
  AMOUNT_SINGLE: '单笔限额',
  AMOUNT_DAILY: '日累计',
  IP_LIMIT: 'IP 限制',
  MOBILE_LIMIT: '手机号限制',
  CUSTOM: '自定义',
}
const ruleTypeTag: Record<string, string> = {
  AMOUNT_SINGLE: 'warning',
  AMOUNT_DAILY: 'primary',
  IP_LIMIT: 'danger',
  MOBILE_LIMIT: 'success',
  CUSTOM: 'info',
}
const actionLabel: Record<string, string> = {
  REJECT: '拒绝',
  REVIEW: '审核',
  WARN: '告警',
}
const actionTag: Record<string, string> = {
  REJECT: 'danger',
  REVIEW: 'warning',
  WARN: 'info',
}
const decisionLabel: Record<string, string> = {
  REJECTED: '拒绝',
  REVIEW_REQUIRED: '待审核',
  WARN_ONLY: '告警',
}
const decisionTag: Record<string, string> = {
  REJECTED: 'danger',
  REVIEW_REQUIRED: 'warning',
  WARN_ONLY: 'info',
}

function scopeLabel(row: RiskRule) {
  if (row.scopeType === 'ALL_MERCHANTS') return '全部商户'
  if (row.scopeType === 'SELECTED_MERCHANTS') {
    const n = row.scopeMerchantCount ?? 0
    return n > 0 ? `指定 ${n} 个商户` : '指定商户（未配置）'
  }
  return '仅归属商户'
}

function scopeTagType(scopeType: string) {
  if (scopeType === 'ALL_MERCHANTS') return 'success'
  if (scopeType === 'SELECTED_MERCHANTS') return 'warning'
  return 'info'
}

function formatThreshold(row: RiskRule) {
  const fen = row.thresholdFen ?? 0
  if (row.unit === 'CNY_FEN') return `¥${(fen / 100).toFixed(2)}`
  if (row.unit === 'TIMES_PER_HOUR') return `${fen} 次/小时`
  return `${fen} ${row.unit}`
}

function formatDateTime(v: string) {
  if (!v) return '—'
  return v.replace('T', ' ').slice(0, 19)
}

function defaultUnit(ruleType: RiskRuleType) {
  if (ruleType === 'AMOUNT_SINGLE' || ruleType === 'AMOUNT_DAILY') return 'CNY_FEN'
  if (ruleType === 'IP_LIMIT' || ruleType === 'MOBILE_LIMIT') return 'TIMES_PER_HOUR'
  return 'CNY_FEN'
}

const thresholdInputLabel = computed(() => {
  const u = formData.unit
  if (u === 'CNY_FEN') return '阈值（元）'
  if (u === 'TIMES_PER_HOUR') return '阈值（次/小时）'
  return '阈值'
})

async function loadMerchants() {
  try {
    merchantOptions.value = await getMerchantsSimple()
  } catch {
    merchantOptions.value = []
  }
}

async function loadRules() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: queryForm.page,
      pageSize: queryForm.pageSize,
    }
    if (queryForm.keyword) params.keyword = queryForm.keyword
    if (queryForm.ownerType) params.ownerType = queryForm.ownerType
    if (queryForm.scopeType) params.scopeType = queryForm.scopeType
    if (queryForm.merchantId) params.merchantId = queryForm.merchantId
    if (queryForm.ruleType) params.ruleType = queryForm.ruleType
    if (queryForm.enabled !== undefined) params.enabled = queryForm.enabled
    const res = await getRiskRules(params)
    ruleList.value = (res.list ?? []).map(normalizeRule)
    total.value = res.total ?? 0
  } catch {
    ElMessage.error('加载风控规则失败')
  } finally {
    loading.value = false
  }
}

function normalizeRule(r: RiskRule): RiskRule {
  return {
    ...r,
    id: r.id,
    enabled: Boolean(r.enabled),
  }
}

function handleSearch() {
  queryForm.page = 1
  loadRules()
}

function handleReset() {
  queryForm.keyword = ''
  queryForm.ownerType = ''
  queryForm.scopeType = ''
  queryForm.merchantId = ''
  queryForm.ruleType = ''
  queryForm.enabled = undefined
  queryForm.page = 1
  loadRules()
}

async function handleToggle(row: RiskRule, enabled: boolean) {
  try {
    await updateRiskRuleStatus(row.id, { enabled })
    row.enabled = enabled
    ElMessage.success(enabled ? '已启用' : '已停用')
  } catch {
    ElMessage.error('状态更新失败')
    loadRules()
  }
}

// ---------- 表单 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | string | null>(null)

interface FormModel {
  ruleCode: string
  ruleName: string
  ruleType: RiskRuleType
  scopeType: RiskRuleScopeType
  scopeMerchantIds: string[]
  thresholdDisplay: number
  unit: string
  action: RiskRuleAction
  priority: number
  description: string
  enabled: boolean
}

const formData = reactive<FormModel>({
  ruleCode: '',
  ruleName: '',
  ruleType: 'AMOUNT_SINGLE',
  scopeType: 'ALL_MERCHANTS',
  scopeMerchantIds: [],
  thresholdDisplay: 5000,
  unit: 'CNY_FEN',
  action: 'REJECT',
  priority: 100,
  description: '',
  enabled: true,
})

const formRules: FormRules = {
  ruleCode: [{ required: true, message: '请输入规则编码', trigger: 'blur' }],
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  scopeType: [{ required: true, message: '请选择作用范围', trigger: 'change' }],
  scopeMerchantIds: [{
    validator: (_r, _v, cb) => {
      if (formData.scopeType === 'SELECTED_MERCHANTS' && formData.scopeMerchantIds.length === 0) {
        cb(new Error('请至少选择一个商户'))
      } else cb()
    },
    trigger: 'change',
  }],
  thresholdDisplay: [{ required: true, message: '请输入阈值', trigger: 'blur' }],
  action: [{ required: true, message: '请选择动作', trigger: 'change' }],
  priority: [{ required: true, message: '请输入优先级', trigger: 'blur' }],
}

function onRuleTypeChange() {
  formData.unit = defaultUnit(formData.ruleType)
  if (formData.unit === 'CNY_FEN' && formData.thresholdDisplay < 100) {
    formData.thresholdDisplay = 5000
  }
  if (formData.unit === 'TIMES_PER_HOUR' && formData.thresholdDisplay > 1000) {
    formData.thresholdDisplay = 50
  }
}

function toThresholdFen(): number {
  if (formData.unit === 'CNY_FEN') return Math.round(formData.thresholdDisplay * 100)
  return Math.round(formData.thresholdDisplay)
}

function buildUpsertPayload(): RiskRuleUpsertRequest {
  return {
    ruleCode: formData.ruleCode,
    ruleName: formData.ruleName,
    ruleType: formData.ruleType,
    thresholdFen: toThresholdFen(),
    unit: formData.unit,
    action: formData.action,
    enabled: formData.enabled,
    priority: formData.priority,
    ownerType: 'PLATFORM',
    scopeType: formData.scopeType,
    scopeMerchantIds: formData.scopeType === 'SELECTED_MERCHANTS' ? formData.scopeMerchantIds : [],
    description: formData.description,
  }
}

function resetForm() {
  Object.assign(formData, {
    ruleCode: `RISK_${Date.now()}`,
    ruleName: '',
    ruleType: 'AMOUNT_SINGLE' as RiskRuleType,
    scopeType: 'ALL_MERCHANTS' as RiskRuleScopeType,
    scopeMerchantIds: [] as string[],
    thresholdDisplay: 5000,
    unit: 'CNY_FEN',
    action: 'REJECT' as RiskRuleAction,
    priority: 100,
    description: '',
    enabled: true,
  })
}

function openCreate() {
  dialogMode.value = 'add'
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: RiskRule) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  formData.ruleCode = row.ruleCode
  formData.ruleName = row.ruleName
  formData.ruleType = row.ruleType
  formData.scopeType = row.scopeType === 'OWNER_MERCHANT_ONLY' ? 'ALL_MERCHANTS' : row.scopeType
  formData.unit = row.unit
  formData.action = row.action
  formData.priority = row.priority
  formData.description = row.description ?? ''
  formData.enabled = row.enabled
  if (row.unit === 'CNY_FEN') {
    formData.thresholdDisplay = (row.thresholdFen ?? 0) / 100
  } else {
    formData.thresholdDisplay = row.thresholdFen ?? 0
  }
  formData.scopeMerchantIds = []
  if (row.scopeType === 'SELECTED_MERCHANTS') {
    try {
      const scopes = await getRiskRuleScopes(row.id)
      formData.scopeMerchantIds = (scopes.merchants ?? []).map((m) => m.merchantId)
    } catch {
      ElMessage.warning('加载适用商户失败')
    }
  }
  dialogVisible.value = true
}

const viewVisible = ref(false)
const viewRule = ref<RiskRule | null>(null)

function openView(row: RiskRule) {
  viewRule.value = row
  viewVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = buildUpsertPayload()
      if (dialogMode.value === 'add') {
        await createRiskRule(payload)
        ElMessage.success('平台规则已创建')
      } else if (editingId.value != null) {
        await updateRiskRule(editingId.value, payload)
        ElMessage.success('规则已保存')
      }
      dialogVisible.value = false
      loadRules()
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : '保存失败'
      ElMessage.error(msg || '保存失败')
    } finally {
      submitting.value = false
    }
  })
}

// ---------- 命中记录 ----------
const hitLoading = ref(false)
const hitList = ref<RiskHitRecord[]>([])
const hitTotal = ref(0)
const hitQuery = reactive({
  page: 1,
  pageSize: 10,
  merchantId: '',
  decision: '' as '' | 'REJECTED' | 'REVIEW_REQUIRED' | 'WARN_ONLY',
})

async function loadHits() {
  hitLoading.value = true
  try {
    const params: Record<string, unknown> = {
      page: hitQuery.page,
      pageSize: hitQuery.pageSize,
    }
    if (hitQuery.merchantId) params.merchantId = hitQuery.merchantId
    if (hitQuery.decision) params.decision = hitQuery.decision
    const res = await getRiskHitRecords(params)
    hitList.value = res.list ?? []
    hitTotal.value = res.total ?? 0
  } catch {
    ElMessage.error('加载命中记录失败')
  } finally {
    hitLoading.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'hits' && hitList.value.length === 0) loadHits()
})

onMounted(async () => {
  await loadMerchants()
  await loadRules()
})
</script>

<style scoped>
.risk-tabs {
  margin-bottom: 12px;
}
.risk-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
</style>
