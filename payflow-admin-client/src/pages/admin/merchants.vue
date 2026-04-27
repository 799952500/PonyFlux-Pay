<template>
  <div>
    <div class="bg-white rounded-xl p-5 card-shadow mb-4">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="关键词"><el-input v-model="queryForm.keyword" placeholder="商户号 / 商户名称" clearable style="width: 180px" @keyup.enter="handleSearch" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" /><el-option label="正常" value="ACTIVE" /><el-option label="停用" value="SUSPENDED" /><el-option label="关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="bg-white rounded-xl card-shadow">
      <el-table v-loading="loading" :data="merchantList" stripe size="small">
        <el-table-column label="商户号" prop="merchantId" min-width="150">
          <template #default="{ row }"><span class="text-xs tabular-nums font-medium text-primary cursor-pointer">{{ row.merchantId }}</span></template>
        </el-table-column>
        <el-table-column label="商户名称" prop="merchantName" min-width="180"><template #default="{ row }"><span class="font-medium">{{ row.merchantName }}</span></template></el-table-column>
        <el-table-column label="类型" prop="merchantType" width="100">
          <template #default="{ row }"><el-tag size="small" :type="row.merchantType === 'ENTERPRISE' ? 'primary' : 'info'" effect="plain">{{ row.merchantType === 'ENTERPRISE' ? '企业' : '个人' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="联系人" min-width="140">
          <template #default="{ row }"><div class="text-sm"><p>{{ row.contactPhone ?? '—' }}</p><p class="text-xs text-gray-400">{{ row.contactEmail ?? '' }}</p></div></template>
        </el-table-column>
        <el-table-column label="商户密钥" prop="merchantKey" width="140">
          <template #default="{ row }">
            <span class="text-xs font-mono text-gray-500">{{ maskKey(row.merchantKey) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="手续费率" width="110">
          <template #default="{ row }">
            <span class="text-sm">{{ row.commissionRate != null ? `${(row.commissionRate * 100).toFixed(2)}%` : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="90">
          <template #default="{ row }"><el-tag size="small" :type="statusTypeMap[row.status]">{{ statusLabelMap[row.status] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click.stop="openDetail(row)">详情</el-button>
            <el-button link type="success" size="small" @click.stop="openPaymentConfig(row)">支付方式</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end p-4">
        <el-pagination v-model:current-page="queryForm.page" v-model:page-size="queryForm.pageSize" :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" @size-change="loadMerchants" @current-change="loadMerchants" />
      </div>
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
            <dt class="text-gray-400">状态</dt><dd><el-tag size="small" :type="statusTypeMap[currentMerchant.status]">{{ statusLabelMap[currentMerchant.status] }}</el-tag></dd>
            <dt class="text-gray-400">手续费率</dt><dd class="text-gray-800">{{ currentMerchant.commissionRate != null ? `${(currentMerchant.commissionRate * 100).toFixed(2)}%` : '—' }}</dd>
            <dt class="text-gray-400">创建时间</dt><dd class="text-gray-800">{{ currentMerchant.createdAt }}</dd>
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
            <dt class="text-gray-400">商户密钥</dt><dd class="text-gray-800 font-mono text-xs">{{ maskKey(currentMerchant.merchantKey) }}</dd>
            <dt class="text-gray-400">回调地址</dt><dd class="text-gray-800 break-all">{{ currentMerchant.callbackUrl ?? '—' }}</dd>
            <dt class="text-gray-400">通知地址</dt><dd class="text-gray-800 break-all">{{ currentMerchant.notifyUrl ?? '—' }}</dd>
          </dl>
        </section>
      </div>
      <template #footer><el-button type="primary" @click="detailVisible = false">关闭</el-button></template>
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

    <!-- 商户支付方式配置弹窗（方式+账号路由） -->
    <el-dialog v-model="paymentConfigVisible" :title="`支付方式配置 - ${currentMerchant?.merchantName || ''}`" width="860px" destroy-on-close>
      <div v-if="paymentConfigLoading" class="p-4"><el-skeleton animated :rows="4" /></div>
      <div v-else>
        <div class="flex items-center justify-between mb-3">
          <div class="text-sm text-gray-500">为该商户配置“支付方式 + 收款账号”的路由列表：</div>
          <el-button type="primary" class="btn-primary" size="small" @click="addRoute">新增路由</el-button>
        </div>
        <el-table :data="merchantRoutes" stripe size="small" max-height="420">
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
          <el-table-column label="启用" width="90" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getMerchants, getPaymentMethods, getPaymentAccounts, getMerchantPaymentRoutes, replaceMerchantPaymentRoutes, updateMerchant } from '@/api/admin'
import type { Merchant, PaymentMethod, PaymentAccount, MerchantPaymentRoute } from '@/types'

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
const queryForm = reactive({ page: 1, pageSize: 20, keyword: '', status: '' })

// 支付方式配置相关状态
const paymentConfigVisible = ref(false)
const paymentConfigLoading = ref(false)
const paymentConfigSaving = ref(false)
const allPaymentMethods = ref<PaymentMethod[]>([])
const allPaymentAccounts = ref<PaymentAccount[]>([])
const merchantRoutes = ref<Array<MerchantPaymentRoute & { _tmpId: string }>>([])

const statusTypeMap: Record<string, string> = { ACTIVE: 'success', SUSPENDED: 'warning', CLOSED: 'danger' }
const statusLabelMap: Record<string, string> = { ACTIVE: '正常', SUSPENDED: '停用', CLOSED: '关闭' }

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

function maskKey(key?: string) {
  if (!key || key.length < 4) return key ?? '—'
  return `${key.slice(0, 4)}****`
}

async function loadMerchants() {
  loading.value = true
  try {
    const resp = await getMerchants(queryForm as Parameters<typeof getMerchants>[0])
    merchantList.value = resp.list
    total.value = resp.total
  } catch {
    ElMessage.error('加载商户列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() { queryForm.page = 1; loadMerchants() }
function handleReset() { Object.assign(queryForm, { page: 1, pageSize: 20, keyword: '', status: '' }); loadMerchants() }

async function openDetail(merchant: Merchant) {
  currentMerchant.value = merchant
  detailVisible.value = true
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
    }))

    const invalidIndex = routes.findIndex(r => !r.paymentMethodId || !r.paymentAccountId)
    if (invalidIndex >= 0) {
      ElMessage.warning(`第 ${invalidIndex + 1} 行请先选择支付方式与收款账号`)
      return
    }

    await replaceMerchantPaymentRoutes(currentMerchant.value.merchantId, routes)
    ElMessage.success('支付方式配置已保存')
    paymentConfigVisible.value = false
  } catch (e) {
    ElMessage.error('保存失败')
    console.error(e)
  } finally {
    paymentConfigSaving.value = false
  }
}

onMounted(() => { loadMerchants() })
</script>

<style scoped>
/* 按钮样式（与 orders/index.vue 保持一致） */
.btn-primary {
  background: linear-gradient(135deg, #065f46 0%, #0d9488 100%);
  border: none;
  color: white;
  border-radius: 10px;
  padding: 10px 20px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.35);
}

.btn-outline {
  background: transparent;
  border: 1.5px solid #E2E8F0;
  color: #374151;
  border-radius: 10px;
  padding: 10px 20px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-outline:hover {
  border-color: #047857;
  color: #047857;
}
</style>
