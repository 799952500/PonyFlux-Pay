<template>
  <div>
    <div class="filter-bar">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="选择商户">
          <el-select v-model="queryForm.merchantId" placeholder="全部商户" clearable filterable style="width: 240px" @change="handleMerchantChange">
            <el-option v-for="m in merchantOptions" :key="m.merchantId" :label="m.merchantName" :value="m.merchantId" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="queryForm.paymentMethodId" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="p in paymentMethodOptions" :key="p.id" :label="p.methodName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <div class="p-4 flex justify-between items-center border-b">
        <div>
          <span class="text-sm font-semibold text-gray-600">商户支付配置</span>
          <p class="text-xs text-gray-400 mt-1">配置支付方式、收款账号及 PC / H5 / APP 展示范围（与收银台订单渠道一致时生效）</p>
        </div>
        <el-button type="primary" class="btn-primary" icon="Plus" @click="openAdd">新增配置</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" stripe size="small" class="data-table">
        <el-table-column label="商户ID" prop="merchantId" min-width="140">
          <template #default="{ row }"><span class="text-xs tabular-nums font-medium text-primary">{{ row.merchantId }}</span></template>
        </el-table-column>
        <el-table-column label="商户名称" prop="merchantName" min-width="160">
          <template #default="{ row }"><span class="font-medium">{{ row.merchantName ?? '—' }}</span></template>
        </el-table-column>
        <el-table-column label="支付方式" min-width="160">
          <template #default="{ row }">
            <div>
              <p class="font-medium">{{ row.paymentMethod?.methodName ?? row.paymentMethodName ?? '—' }}</p>
              <p class="text-xs text-gray-400">{{ row.paymentMethod?.methodCode ?? row.methodCode ?? '' }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="收款账号" min-width="160">
          <template #default="{ row }">
            <div>
              <p class="font-medium">{{ row.paymentAccount?.accountName ?? '—' }}</p>
              <p class="text-xs text-gray-400">{{ row.paymentAccount?.accountCode ?? '' }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="终端" min-width="120">
          <template #default="{ row }">
            <el-tag v-for="t in (row.clientScopes || [])" :key="t" size="small" class="mr-1" type="info">{{ t }}</el-tag>
            <span v-if="!(row.clientScopes || []).length" class="text-gray-400 text-xs">—</span>
          </template>
        </el-table-column>
        <el-table-column label="优先级" prop="priority" width="80">
          <template #default="{ row }"><el-tag size="small" type="info">{{ row.priority ?? 0 }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="90">
          <template #default="{ row }"><el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click.stop="handleToggle(row)">{{ row.status === 'ACTIVE' ? '禁用' : '启用' }}</el-button>
            <el-button link type="danger" size="small" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑支付配置' : '新增支付配置'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" size="default">
        <el-form-item label="选择商户" prop="merchantId">
          <el-select v-model="form.merchantId" placeholder="请选择商户" filterable style="width: 100%" :disabled="!!editingId">
            <el-option v-for="m in merchantOptions" :key="m.merchantId" :label="m.merchantName" :value="m.merchantId" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式" prop="paymentMethodId">
          <el-select v-model="form.paymentMethodId" placeholder="请选择支付方式" style="width: 100%" @change="onFormMethodChange">
            <el-option v-for="p in paymentMethodOptions" :key="p.id" :label="`${p.methodName}（${channelLabel[p.channelType ?? ''] ?? p.channelName ?? ''}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="收款账号" prop="paymentAccountId">
          <el-select v-model="form.paymentAccountId" placeholder="请先选择支付方式" style="width: 100%" filterable>
            <el-option v-for="a in filteredAccounts" :key="a.id" :label="`${a.accountName}（${a.accountCode}）`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="终端可见" prop="clientScopes">
          <el-checkbox-group v-model="form.clientScopes">
            <el-checkbox label="PC">PC</el-checkbox>
            <el-checkbox label="H5">H5</el-checkbox>
            <el-checkbox label="APP">APP</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" :min="0" :max="999" placeholder="数值越大越优先" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getMerchantPaymentRoutes,
  createMerchantPaymentRouteItem,
  updateMerchantPaymentRoute,
  toggleMerchantPaymentRoute,
  deleteMerchantPaymentRoute,
  getMerchants,
  getPaymentMethods,
  getPaymentAccounts,
} from '@/api/admin'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const merchantOptions = ref<any[]>([])
const paymentMethodOptions = ref<any[]>([])
const paymentAccountOptions = ref<any[]>([])

const queryForm = reactive({
  page: 1,
  pageSize: 20,
  merchantId: '',
  paymentMethodId: '',
  status: '',
})

const formRef = ref<FormInstance>()
const form = reactive({
  merchantId: '',
  paymentMethodId: undefined as number | undefined,
  paymentAccountId: undefined as number | undefined,
  priority: 0,
  clientScopes: ['PC', 'H5', 'APP'] as string[],
})

const rules: FormRules = {
  merchantId: [{ required: true, message: '请选择商户', trigger: 'change' }],
  paymentMethodId: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
  paymentAccountId: [{ required: true, message: '请选择收款账号', trigger: 'change' }],
  clientScopes: [
    {
      type: 'array',
      required: true,
      min: 1,
      message: '至少选择一个终端',
      trigger: 'change',
    },
  ],
}

const channelLabel: Record<string, string> = {
  WECHAT: '微信支付',
  ALIPAY: '支付宝',
  UNION: '银联',
  CARD: '银行卡',
}

const filteredAccounts = computed(() => {
  const pm = paymentMethodOptions.value.find((p: { id: number }) => p.id === form.paymentMethodId)
  if (!pm) return []
  const cid = Number(pm.channelId)
  if (!Number.isFinite(cid)) return []
  return paymentAccountOptions.value.filter((a: { channelId: number; enabled?: boolean }) => a.channelId === cid && a.enabled !== false)
})

function onFormMethodChange() {
  form.paymentAccountId = undefined
}

async function loadMerchants() {
  try {
    const res: any = await getMerchants({ page: 1, pageSize: 500 })
    merchantOptions.value = Array.isArray(res) ? res : (res.list ?? [])
  } catch {
    // ignore
  }
}

async function loadPaymentMethods() {
  try {
    const res: any = await getPaymentMethods({ page: 1, pageSize: 500 })
    paymentMethodOptions.value = Array.isArray(res) ? res : (res.list ?? [])
  } catch {
    // ignore
  }
}

async function loadPaymentAccounts() {
  try {
    const res: any = await getPaymentAccounts({ page: 1, pageSize: 500 })
    paymentAccountOptions.value = Array.isArray(res) ? res : (res.list ?? [])
  } catch {
    // ignore
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getMerchantPaymentRoutes(queryForm.merchantId || undefined)
    const list = Array.isArray(res) ? res : []

    const merchantMap = new Map<string, string>(
      merchantOptions.value.map((m: { merchantId: string; merchantName: string }) => [m.merchantId, m.merchantName])
    )

    let rows = list.map((r: any) => {
      const enabled = r.enabled === true || r.enabled === 1
      const scopes = Array.isArray(r.clientScopes) ? r.clientScopes : []
      return {
        ...r,
        merchantName: merchantMap.get(r.merchantId) ?? r.merchantName,
        clientScopes: scopes,
        status: enabled ? 'ACTIVE' : 'DISABLED',
      }
    })

    if (queryForm.paymentMethodId) {
      rows = rows.filter((r) => String(r.paymentMethodId) === String(queryForm.paymentMethodId))
    }
    if (queryForm.status) {
      rows = rows.filter((r) => r.status === queryForm.status)
    }

    total.value = rows.length
    const from = (queryForm.page - 1) * queryForm.pageSize
    tableData.value = rows.slice(from, from + queryForm.pageSize)
  } catch {
    ElMessage.error('加载列表失败')
  } finally {
    loading.value = false
  }
}

function handleMerchantChange() {
  queryForm.page = 1
  loadData()
}

function handleSearch() {
  queryForm.page = 1
  loadData()
}

function handleReset() {
  Object.assign(queryForm, { page: 1, pageSize: 20, merchantId: '', paymentMethodId: '', status: '' })
  loadData()
}

function openAdd() {
  editingId.value = null
  Object.assign(form, {
    merchantId: '',
    paymentMethodId: undefined,
    paymentAccountId: undefined,
    priority: 0,
    clientScopes: ['PC', 'H5', 'APP'],
  })
  dialogVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.id
  Object.assign(form, {
    merchantId: row.merchantId,
    paymentMethodId: row.paymentMethodId,
    paymentAccountId: row.paymentAccountId,
    priority: row.priority ?? 0,
    clientScopes:
      Array.isArray(row.clientScopes) && row.clientScopes.length > 0 ? [...row.clientScopes] : ['PC', 'H5', 'APP'],
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload = {
        merchantId: form.merchantId,
        paymentMethodId: form.paymentMethodId!,
        paymentAccountId: form.paymentAccountId!,
        priority: form.priority,
        enabled: true,
        clientScopes: form.clientScopes,
      }
      if (editingId.value != null) {
        await updateMerchantPaymentRoute(editingId.value, {
          paymentMethodId: payload.paymentMethodId,
          paymentAccountId: payload.paymentAccountId,
          priority: payload.priority,
          clientScopes: payload.clientScopes,
        })
        ElMessage.success('已保存')
      } else {
        await createMerchantPaymentRouteItem(payload)
        ElMessage.success('已新增')
      }
      dialogVisible.value = false
      loadData()
    } catch {
      ElMessage.error(editingId.value != null ? '保存失败' : '新增失败')
    } finally {
      submitLoading.value = false
    }
  })
}

async function handleToggle(row: any) {
  const action = row.status === 'ACTIVE' ? '禁用' : '启用'
  const name = row.paymentMethod?.methodName ?? row.paymentMethodName ?? '该项'
  await ElMessageBox.confirm(`确定要${action}「${name}」吗？`, `${action}确认`, { type: 'warning' })
  try {
    await toggleMerchantPaymentRoute(row.id)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch {
    ElMessage.error(`${action}失败`)
  }
}

async function handleDelete(row: any) {
  const name = row.paymentMethod?.methodName ?? row.paymentMethodName ?? '该项'
  await ElMessageBox.confirm(`确定要删除「${name}」的配置吗？`, '删除确认', { type: 'warning' })
  try {
    await deleteMerchantPaymentRoute(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(async () => {
  await Promise.all([loadMerchants(), loadPaymentMethods(), loadPaymentAccounts()])
  await loadData()
})
</script>

