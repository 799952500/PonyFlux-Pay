<template>
  <div class="page-table-shell">
    <div class="filter-bar">
      <el-form :inline="true" :model="queryForm" size="default" class="filter-bar__form">
        <el-form-item :label="t('merchants.keyword')"><el-input v-model="queryForm.keyword" :placeholder="t('merchants.keywordPlaceholder')" clearable style="width: 180px" @keyup.enter="handleSearch" /></el-form-item>
        <el-form-item :label="t('merchants.status')">
          <el-select v-model="queryForm.status" :placeholder="t('merchants.all')" clearable style="width: 140px">
            <el-option :label="t('merchants.all')" value="" /><el-option :label="t('merchants.active')" value="ACTIVE" /><el-option :label="t('merchants.suspended')" value="SUSPENDED" /><el-option :label="t('merchants.closed')" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-bar__actions">
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">{{ t('merchants.search') }}</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">{{ t('merchants.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <TableToolbar :title="t('merchants.listTitle')" :total="total" />

      <el-table table-layout="auto" v-loading="loading" :data="merchantList" stripe size="small" class="data-table">
        <el-table-column :label="t('merchants.merchantId')" prop="merchantId" min-width="150">
          <template #default="{ row }">
            <span
              :data-flip="`merchant-${row.merchantId}`"
              class="cell-mono pf-link cursor-pointer"
              @click.stop="goMerchantInsight(row)"
            >{{ row.merchantId }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('merchants.merchantName')" prop="merchantName" min-width="180"><template #default="{ row }"><span class="font-medium">{{ row.merchantName }}</span></template></el-table-column>
        <el-table-column :label="t('merchants.type')" prop="merchantType" width="100">
          <template #default="{ row }"><el-tag size="small" :type="row.merchantType === 'ENTERPRISE' ? 'primary' : 'info'" effect="plain">{{ row.merchantType === 'ENTERPRISE' ? t('merchants.enterprise') : t('merchants.individual') }}</el-tag></template>
        </el-table-column>
        <el-table-column label="联系人" min-width="140">
          <template #default="{ row }"><div class="text-sm"><p>{{ row.contactPhone ?? '—' }}</p><p class="text-xs text-gray-400">{{ row.contactEmail ?? '' }}</p></div></template>
        </el-table-column>
        <el-table-column :label="t('merchants.merchantKey')" prop="merchantKey" width="140">
          <template #default="{ row }">
            <span class="text-xs font-mono text-gray-500">{{ maskSecret(row.merchantKey) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('merchants.feeRate')" width="110">
          <template #default="{ row }">
            <span class="text-sm tabular-nums">{{ formatRatePercent(row.commissionRate) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('merchants.status')" prop="status" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="tagTypeOf(MERCHANT_STATUS_TAG, row.status)">
              {{ labelOf(MERCHANT_STATUS_LABEL, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('merchants.createdAt')" prop="createdAt" min-width="168" class-name="col-datetime" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-datetime">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" min-width="280" class-name="col-actions" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row)">{{ t('merchants.detail') }}</el-button>
            <el-button link type="success" size="small" @click.stop="openPaymentConfig(row)">{{ t('merchants.paymentConfig') }}</el-button>
            <el-button link type="primary" size="small" @click.stop="openEdit(row)">{{ t('merchants.edit') }}</el-button>
            <el-button link type="primary" size="small" @click.stop="goMerchantOrders(row)">{{ t('merchants.orders') }}</el-button>
            <el-button link type="primary" size="small" @click.stop="goMerchantInsight(row)">{{ t('merchants.insight') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AdminPagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        @size-change="loadMerchants"
        @current-change="loadMerchants"
      />
    </div>

    <!-- 商户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="商户详情" width="580px" destroy-on-close>
      <div v-if="detailLoading" class="p-4"><el-skeleton animated :rows="5" /></div>
      <div v-else-if="currentMerchant" class="space-y-5">
        <section>
          <h3 class="text-sm font-semibold text-gray-700 mb-3 border-b pb-2">基本信息</h3>
          <dl class="grid grid-cols-2 gap-y-3 gap-x-4 text-sm">
            <dt class="text-gray-400">商户号</dt><dd class="text-gray-800 font-medium tabular-nums">{{ currentMerchant.merchantId }}</dd>
            <dt class="text-gray-400">商户名称</dt><dd class="text-gray-800">{{ currentMerchant.merchantName }}</dd>
            <dt class="text-gray-400">类型</dt><dd><el-tag size="small" :type="currentMerchant.merchantType === 'ENTERPRISE' ? 'primary' : 'info'">{{ currentMerchant.merchantType === 'ENTERPRISE' ? '企业' : '个人' }}</el-tag></dd>
            <dt class="text-gray-400">状态</dt>
            <dd>
              <el-tag size="small" :type="tagTypeOf(MERCHANT_STATUS_TAG, currentMerchant.status)">
                {{ labelOf(MERCHANT_STATUS_LABEL, currentMerchant.status) }}
              </el-tag>
            </dd>
            <dt class="text-gray-400">手续费率</dt><dd class="text-gray-800">{{ formatRatePercent(currentMerchant.commissionRate) }}</dd>
            <dt class="text-gray-400">创建时间</dt><dd class="text-gray-800 tabular-nums">{{ formatDateTime(currentMerchant.createdAt) }}</dd>
          </dl>
        </section>
        <section>
          <h3 class="text-sm font-semibold text-gray-700 mb-3 border-b pb-2">联系信息</h3>
          <dl class="grid grid-cols-2 gap-y-3 gap-x-4 text-sm">
            <dt class="text-gray-400">联系电话</dt><dd class="text-gray-800">{{ currentMerchant.contactPhone ?? '—' }}</dd>
            <dt class="text-gray-400">联系邮箱</dt><dd class="text-gray-800">{{ currentMerchant.contactEmail ?? '—' }}</dd>
          </dl>
        </section>
        <section>
          <h3 class="text-sm font-semibold text-gray-700 mb-3 border-b pb-2">支付配置</h3>
          <dl class="grid grid-cols-2 gap-y-3 gap-x-4 text-sm">
            <dt class="text-gray-400">商户密钥</dt><dd class="text-gray-800 font-mono text-xs">{{ maskSecret(currentMerchant.merchantKey) }}</dd>
            <dt class="text-gray-400">回调地址</dt><dd class="text-gray-800 break-all">{{ currentMerchant.callbackUrl ?? '—' }}</dd>
            <dt class="text-gray-400">通知地址</dt><dd class="text-gray-800 break-all">{{ currentMerchant.notifyUrl ?? '—' }}</dd>
          </dl>
        </section>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="currentMerchant"
          type="primary"
          class="btn-primary"
          @click="openPaymentConfigFromDetail"
        >
          支付配置
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑商户弹窗 -->
    <el-dialog v-model="editVisible" :title="editTitle" width="520px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="商户号">
          <el-input :model-value="editForm.merchantId" disabled />
        </el-form-item>
        <el-form-item label="商户名称" prop="merchantName">
          <el-input v-model="editForm.merchantName" placeholder="请输入商户名称" />
        </el-form-item>
        <el-form-item label="商户密钥" prop="merchantKey">
          <el-input v-model="editForm.merchantKey" placeholder="用于签名的商户密钥" />
        </el-form-item>
        <el-form-item label="回调地址" prop="callbackUrl">
          <el-input v-model="editForm.callbackUrl" placeholder="支付结果回调地址" />
        </el-form-item>
        <el-form-item label="通知地址" prop="notifyUrl">
          <el-input v-model="editForm.notifyUrl" placeholder="异步通知地址" />
        </el-form-item>
        <el-form-item label="手续费率" prop="commissionRate">
          <el-input-number
            v-model="editForm.commissionRate"
            :min="0"
            :max="1"
            :precision="4"
            :step="0.001"
            controls-position="right"
            style="width: 100%"
            placeholder="如 0.005 表示 0.5%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="停用" value="SUSPENDED" />
            <el-option label="关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" :loading="editSubmitting" @click="handleEditSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 商户支付配置弹窗（方式 + 收款账号 + 终端，按商户维度维护） -->
    <el-dialog
      v-model="paymentConfigVisible"
      :title="`支付配置 · ${currentMerchant?.merchantName || currentMerchant?.merchantId || ''}`"
      width="920px"
      destroy-on-close
      class="merchant-payment-dialog"
    >
      <div v-if="paymentConfigLoading" class="p-4"><el-skeleton animated :rows="4" /></div>
      <div v-else>
        <div class="flex items-center justify-between mb-3">
          <div class="text-sm text-gray-500">
            商户号 <span class="font-mono text-slate-700">{{ currentMerchant?.merchantId }}</span> — 配置可用支付方式、收款账号及终端可见范围（PC/H5/APP）
          </div>
          <el-button type="primary" class="btn-primary" size="small" @click="addRoute">新增路由</el-button>
        </div>
        <el-table table-layout="auto" :data="merchantRoutes" stripe size="small" class="data-table" max-height="420">
          <el-table-column label="支付方式" min-width="220">
            <template #default="{ row }">
              <el-select v-model="row.paymentMethodId" placeholder="请选择支付方式" filterable style="width: 100%" @change="handleRouteMethodChange(row)">
                <el-option
                  v-for="m in allPaymentMethods"
                  :key="m.id"
                  :label="`${m.channelName ?? ''} / ${m.methodName} (${m.methodCode})`"
                  :value="m.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="收款账号" min-width="220">
            <template #default="{ row }">
              <el-select v-model="row.paymentAccountId" placeholder="请选择收款账号" filterable style="width: 100%">
                <el-option
                  v-for="a in getAccountOptions(row)"
                  :key="a.id"
                  :label="`${a.channelName ?? ''} / ${a.accountName} (${a.accountCode})`"
                  :value="a.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.priority" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="终端可见" min-width="200" class-name="col-tags">
            <template #default="{ row }">
              <el-checkbox-group v-model="row.clientScopes" size="small" class="merchant-route-scopes">
                <el-checkbox label="PC">{{ labelOf(CLIENT_SCOPE_LABEL, 'PC') }}</el-checkbox>
                <el-checkbox label="H5">{{ labelOf(CLIENT_SCOPE_LABEL, 'H5') }}</el-checkbox>
                <el-checkbox label="APP">{{ labelOf(CLIENT_SCOPE_LABEL, 'APP') }}</el-checkbox>
              </el-checkbox-group>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="90" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" />
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="100" class-name="col-actions" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="removeRoute($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="paymentConfigVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" :loading="paymentConfigSaving" @click="savePaymentRoutes">保存配置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMerchantInsightOverlay } from '@/composables/useMerchantInsightOverlay'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import { getMerchants, getPaymentMethods, getPaymentAccounts, getMerchantPaymentRoutes, replaceMerchantPaymentRoutes, updateMerchant } from '@/api/admin'
import { useMerchantScope } from '@/composables/useMerchantScope'
import type { Merchant, PaymentMethod, PaymentAccount, MerchantPaymentRoute } from '@/types'
import {
  formatDateTime,
  formatRatePercent,
  labelOf,
  maskSecret,
  CLIENT_SCOPE_LABEL,
  MERCHANT_STATUS_LABEL,
  MERCHANT_STATUS_TAG,
  tagTypeOf,
} from '@/utils/format'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { open: openMerchantInsight } = useMerchantInsightOverlay()
const { resolveMerchantIdForCreate } = useMerchantScope()

const loading = ref(false)
const detailLoading = ref(false)
const merchantList = ref<Merchant[]>([])
const total = ref(0)
const detailVisible = ref(false)
const editVisible = ref(false)
const editSubmitting = ref(false)
const currentMerchant = ref<Merchant | null>(null)
const editFormRef = ref<FormInstance>()
const isEdit = ref(false)
const queryForm = reactive({ page: 1, pageSize: DEFAULT_PAGE_SIZE, keyword: '', status: '' })

// 支付方式配置相关状态
const paymentConfigVisible = ref(false)
const paymentConfigLoading = ref(false)
const paymentConfigSaving = ref(false)
const allPaymentMethods = ref<PaymentMethod[]>([])
const allPaymentAccounts = ref<PaymentAccount[]>([])
const merchantRoutes = ref<Array<MerchantPaymentRoute & { _tmpId: string }>>([])

const editTitle = computed(() => isEdit.value ? '编辑商户' : '新建商户')

const editForm = reactive({
  merchantId: '',
  merchantName: '',
  merchantKey: '',
  callbackUrl: '',
  notifyUrl: '',
  commissionRate: undefined as number | undefined,
  status: 'ACTIVE' as Merchant['status'],
})

const editRules: FormRules = {
  merchantName: [{ required: true, message: '请输入商户名称', trigger: 'blur' }],
}

async function loadMerchants() {
  loading.value = true
  try {
    const resp = await getMerchants(queryForm as Parameters<typeof getMerchants>[0])
    merchantList.value = resp.list
    total.value = resp.total
    await tryOpenPaymentFromQuery()
  } catch {
    ElMessage.error(t('merchants.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handleSearch() { queryForm.page = 1; loadMerchants() }
function handleReset() { Object.assign(queryForm, { page: 1, pageSize: DEFAULT_PAGE_SIZE, keyword: '', status: '' }); loadMerchants() }

async function openDetail(merchant: Merchant) {
  currentMerchant.value = merchant
  detailVisible.value = true
}

function openPaymentConfigFromDetail() {
  const m = currentMerchant.value
  if (!m) return
  detailVisible.value = false
  openPaymentConfig(m)
}

function goMerchantOrders(merchant: Merchant) {
  router.push({ path: '/admin/orders', query: { merchantId: merchant.merchantId } })
}

function goMerchantInsight(merchant: Merchant) {
  openMerchantInsight(merchant.merchantId)
}

function openEdit(merchant: Merchant) {
  isEdit.value = true
  currentMerchant.value = merchant
  Object.assign(editForm, {
    merchantId: merchant.merchantId,
    merchantName: merchant.merchantName,
    merchantKey: merchant.merchantKey ?? '',
    callbackUrl: merchant.callbackUrl ?? '',
    notifyUrl: merchant.notifyUrl ?? '',
    commissionRate: merchant.commissionRate,
    status: merchant.status,
  })
  editVisible.value = true
}

async function handleEditSubmit() {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editSubmitting.value = true
    try {
      // 后端需实现 PUT /admin/merchants/{merchantId} 接口
      await updateMerchant(editForm.merchantId, {
        merchantName: editForm.merchantName,
        merchantKey: editForm.merchantKey || undefined,
        callbackUrl: editForm.callbackUrl || undefined,
        notifyUrl: editForm.notifyUrl || undefined,
        commissionRate: editForm.commissionRate,
        status: editForm.status,
      } as Partial<Merchant>)
      ElMessage.success('商户信息已更新')
      editVisible.value = false
      loadMerchants()
    } catch {
      // 如果后端尚未实现该接口，用户会看到错误提示
      ElMessage.error('更新商户信息失败，请确认后端已实现 PUT /admin/merchants/{id} 接口')
    } finally {
      editSubmitting.value = false
    }
  })
}

// ========== 支付方式配置 ==========
async function openPaymentConfig(merchant: Merchant) {
  currentMerchant.value = merchant
  paymentConfigVisible.value = true
  paymentConfigLoading.value = true
  
  try {
    const [methods, accounts, routes] = await Promise.all([
      getPaymentMethods({ page: 1, pageSize: 100 }),
      getPaymentAccounts({ page: 1, pageSize: 200 }),
      getMerchantPaymentRoutes(merchant.merchantId),
    ])
    
    allPaymentMethods.value = methods.list as PaymentMethod[]
    allPaymentAccounts.value = accounts.list as PaymentAccount[]
    merchantRoutes.value = (routes ?? []).map((r) => ({
      ...r,
      _tmpId: cryptoRandomId(),
      enabled: r.enabled ?? true,
      priority: r.priority ?? 0,
      clientScopes:
        Array.isArray(r.clientScopes) && r.clientScopes.length > 0 ? [...r.clientScopes] : ['PC', 'H5', 'APP'],
    }))
    if (merchantRoutes.value.length === 0) {
      addRoute()
    }
  } catch (e) {
    ElMessage.error('加载支付方式失败')
    console.error(e)
  } finally {
    paymentConfigLoading.value = false
  }
}

function cryptoRandomId() {
  return Math.random().toString(36).slice(2) + Date.now().toString(36)
}

function addRoute() {
  if (!currentMerchant.value) return
  merchantRoutes.value.push({
    _tmpId: cryptoRandomId(),
    merchantId: currentMerchant.value.merchantId,
    paymentMethodId: 0,
    paymentAccountId: 0,
    enabled: true,
    priority: 0,
    clientScopes: ['PC', 'H5', 'APP'],
  })
}

function removeRoute(index: number) {
  merchantRoutes.value.splice(index, 1)
}

function getAccountOptions(route: Pick<MerchantPaymentRoute, 'paymentMethodId' | 'paymentAccountId'>) {
  const method = allPaymentMethods.value.find(m => m.id === route.paymentMethodId)
  if (!method) return allPaymentAccounts.value
  const channelIdNum = typeof method.channelId === 'string' ? Number(method.channelId) : (method.channelId as unknown as number)
  if (!Number.isFinite(channelIdNum)) return allPaymentAccounts.value
  return allPaymentAccounts.value.filter(a => a.channelId === channelIdNum)
}

function handleRouteMethodChange(route: MerchantPaymentRoute) {
  const options = getAccountOptions(route)
  if (options.length === 0) {
    route.paymentAccountId = 0
    return
  }
  if (!options.some(a => a.id === route.paymentAccountId)) {
    route.paymentAccountId = 0
  }
}

async function savePaymentRoutes() {
  if (!currentMerchant.value) return
  
  paymentConfigSaving.value = true
  try {
    const routes = merchantRoutes.value.map(r => ({
      paymentMethodId: Number(r.paymentMethodId),
      paymentAccountId: Number(r.paymentAccountId),
      enabled: Boolean(r.enabled),
      priority: Number(r.priority ?? 0),
      clientScopes:
        Array.isArray(r.clientScopes) && r.clientScopes.length > 0 ? r.clientScopes : ['PC', 'H5', 'APP'],
    }))

    const invalidIndex = routes.findIndex(r => !r.paymentMethodId || !r.paymentAccountId)
    if (invalidIndex >= 0) {
      ElMessage.warning(`第 ${invalidIndex + 1} 行请先选择支付方式与收款账号`)
      return
    }

    const merchantId =
      resolveMerchantIdForCreate(currentMerchant.value.merchantId) ?? currentMerchant.value.merchantId
    await replaceMerchantPaymentRoutes(merchantId, routes)
    ElMessage.success('支付方式配置已保存')
    paymentConfigVisible.value = false
  } catch (e) {
    ElMessage.error('保存失败')
    console.error(e)
  } finally {
    paymentConfigSaving.value = false
  }
}

/** 兼容旧链接 /admin/merchant-payments?merchantId=xxx */
async function tryOpenPaymentFromQuery() {
  const raw = route.query.openPayment ?? route.query.merchantId
  const merchantId = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : ''
  if (!merchantId) return
  const merchant = merchantList.value.find((m) => m.merchantId === merchantId)
  if (merchant) {
    await openPaymentConfig(merchant)
    router.replace({ path: '/admin/merchants' })
  }
}

function tryOpenInsightFromQuery() {
  const raw = route.query.insight
  const id = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : ''
  if (!id?.trim()) return
  openMerchantInsight(id.trim())
  const q = { ...route.query }
  delete q.insight
  router.replace({ path: '/admin/merchants', query: q })
}

onMounted(() => {
  loadMerchants()
  tryOpenInsightFromQuery()
})

watch(() => route.query.insight, () => tryOpenInsightFromQuery())
</script>

<style scoped>
.merchant-route-scopes {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 10px;
}

.merchant-route-scopes :deep(.el-checkbox) {
  margin-right: 0;
}
</style>

