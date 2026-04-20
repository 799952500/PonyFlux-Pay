<template>
  <div>
    <!-- 查询表单 -->
    <div class="bg-white rounded-xl p-5 card-shadow mb-4">
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

    <!-- 表格 -->
    <div class="bg-white rounded-xl card-shadow">
      <div class="p-4 flex justify-between items-center border-b">
        <span class="text-sm font-semibold text-gray-600">商户支付方式列表</span>
        <el-button type="primary" class="btn-primary" icon="Plus" @click="openAdd">绑定支付方式</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" stripe size="small">
        <el-table-column label="商户ID" prop="merchantId" min-width="160">
          <template #default="{ row }"><span class="text-xs tabular-nums font-medium text-primary">{{ row.merchantId }}</span></template>
        </el-table-column>
        <el-table-column label="商户名称" prop="merchantName" min-width="180">
          <template #default="{ row }"><span class="font-medium">{{ row.merchantName ?? '—' }}</span></template>
        </el-table-column>
        <el-table-column label="支付方式" min-width="160">
          <template #default="{ row }">
            <div>
              <p class="font-medium">{{ row.paymentMethodName ?? row.methodName }}</p>
              <p class="text-xs text-gray-400">{{ row.methodCode ?? row.paymentMethodCode }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="appId" prop="appId" min-width="200">
          <template #default="{ row }"><span class="text-xs font-mono text-gray-500">{{ maskValue(row.appId) }}</span></template>
        </el-table-column>
        <el-table-column label="优先级" prop="priority" width="80">
          <template #default="{ row }"><el-tag size="small" type="info">{{ row.priority ?? 0 }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="90">
          <template #default="{ row }"><el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="handleToggle(row)">{{ row.status === 'ACTIVE' ? '禁用' : '启用' }}</el-button>
            <el-button link type="danger" size="small" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end p-4">
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

    <!-- 新建绑定弹窗 -->
    <el-dialog v-model="dialogVisible" title="绑定支付方式" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px" size="default">
        <el-form-item label="选择商户" prop="merchantId">
          <el-select v-model="form.merchantId" placeholder="请选择商户" filterable style="width: 100%">
            <el-option v-for="m in merchantOptions" :key="m.merchantId" :label="m.merchantName" :value="m.merchantId" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择支付方式" prop="paymentMethodId">
          <el-select v-model="form.paymentMethodId" placeholder="请选择支付方式" style="width: 100%">
            <el-option v-for="p in paymentMethodOptions" :key="p.id" :label="`${p.methodName}（${channelLabel[p.channel] ?? p.channel}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" :min="0" :max="999" placeholder="数值越大优先级越高" style="width: 100%" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { listMerchantPayments, createMerchantPayment, deleteMerchantPayment, toggleMerchantPayment } from '@/api/merchant-api'
import { listMerchants } from '@/api/merchant-api'
import { listPaymentMethods } from '@/api/payment-method'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const merchantOptions = ref<any[]>([])
const paymentMethodOptions = ref<any[]>([])

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
  paymentMethodId: '',
  priority: 0,
})

const rules: FormRules = {
  merchantId: [{ required: true, message: '请选择商户', trigger: 'change' }],
  paymentMethodId: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
}

const channelLabel: Record<string, string> = {
  WECHAT: '微信支付',
  ALIPAY: '支付宝',
  UNIONPAY: '银联',
}

function maskValue(val: string | undefined) {
  if (!val) return '—'
  if (val.length <= 8) return val
  return val.substring(0, 4) + '****' + val.substring(val.length - 4)
}

async function loadMerchants() {
  try {
    const res: any = await listMerchants({ page: 1, pageSize: 500 })
    merchantOptions.value = Array.isArray(res) ? res : (res.list ?? [])
  } catch {
    // ignore
  }
}

async function loadPaymentMethods() {
  try {
    const res: any = await listPaymentMethods({ page: 1, pageSize: 500 })
    paymentMethodOptions.value = Array.isArray(res) ? res : (res.list ?? [])
  } catch {
    // ignore
  }
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { ...queryForm }
    if (!params.merchantId) delete params.merchantId
    if (!params.paymentMethodId) delete params.paymentMethodId
    if (!params.status) delete params.status

    if (params.merchantId) {
      const res: any = await listMerchantPayments(params.merchantId)
      tableData.value = Array.isArray(res) ? res : []
      total.value = tableData.value.length
    } else {
      // 未选商户时，尝试获取所有（如果接口支持的话，这里给空列表）
      tableData.value = []
      total.value = 0
    }
  } catch {
    ElMessage.error('加载商户支付方式列表失败')
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
  Object.assign(form, { merchantId: '', paymentMethodId: '', priority: 0 })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      await createMerchantPayment({ ...form })
      ElMessage.success('绑定成功')
      dialogVisible.value = false
      loadData()
    } catch {
      ElMessage.error('绑定失败')
    } finally {
      submitLoading.value = false
    }
  })
}

async function handleToggle(row: any) {
  const action = row.status === 'ACTIVE' ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定要${action}「${row.paymentMethodName ?? row.methodName}」吗？`, `${action}确认`, { type: 'warning' })
  try {
    await toggleMerchantPayment(row.id)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch {
    ElMessage.error(`${action}失败`)
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确定要删除「${row.paymentMethodName ?? row.methodName}」的绑定吗？`, '删除确认', { type: 'warning' })
  try {
    await deleteMerchantPayment(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadMerchants()
  loadPaymentMethods()
  loadData()
})
</script>

<style scoped>
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
