<template>
  <div class="page-table-shell">
    <el-alert
      v-if="merchantFilterLocked"
      type="info"
      :closable="false"
      show-icon
      class="mb-4"
      title="商户数据范围"
      description="当前仅展示与您授权商户关联的支付账号，由服务端按路由绑定关系自动过滤。"
    />
    <div class="filter-bar">
      <el-form :inline="true" :model="queryForm" size="default" class="filter-bar__form">
        <el-form-item label="所属渠道">
          <el-select v-model="queryForm.channelId" placeholder="全部渠道" clearable style="width: 180px">
            <el-option v-for="ch in channelList" :key="ch.id" :label="ch.channelName" :value="ch.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="账号编码 / 账号名称" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item class="filter-bar__actions">
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <TableToolbar title="支付账号列表" :total="total">
        <template #actions>
          <el-button type="primary" class="btn-primary" icon="Plus" @click="openCreate">新增账号</el-button>
        </template>
      </TableToolbar>

      <el-table
        table-layout="auto"
        v-loading="loading"
        :data="accountList"
        stripe
        size="small"
        class="data-table"
        @row-click="openEdit"
      >
        <el-table-column label="账号编码" prop="accountCode" min-width="140">
          <template #default="{ row }">
            <span class="cell-mono pf-link cursor-pointer">{{ row.accountCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="账号名称" prop="accountName" min-width="160" show-overflow-tooltip />
        <el-table-column label="所属渠道" prop="channelName" min-width="120">
          <template #default="{ row }">
            <el-tag v-if="row.channelName" size="small" effect="plain">{{ row.channelName }}</el-tag>
            <span v-else class="cell-empty">—</span>
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="handleToggleEnabled(row)" />
          </template>
        </el-table-column>
        <el-table-column label="优先级" prop="priority" width="90" align="center">
          <template #default="{ row }">{{ row.priority ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="描述" prop="description" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" min-width="168" class-name="col-datetime" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-datetime">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" class-name="col-actions" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <AdminPagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        @size-change="loadAccounts"
        @current-change="loadAccounts"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑支付账号' : '新增支付账号'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item v-if="merchantFilterLocked" label="归属商户">
          <el-input :model-value="form.merchantId" disabled />
        </el-form-item>
        <el-form-item label="所属渠道" prop="channelId">
          <el-select v-model="form.channelId" placeholder="请选择渠道" style="width: 100%">
            <el-option v-for="ch in channelList" :key="ch.id" :label="ch.channelName" :value="ch.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号编码" prop="accountCode">
          <el-input v-model="form.accountCode" placeholder="请输入账号编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="账号名称" prop="accountName">
          <el-input v-model="form.accountName" placeholder="请输入账号名称" />
        </el-form-item>
        <el-form-item label="AppID">
          <el-input v-model="form.appId" placeholder="应用 ID（可选）" />
        </el-form-item>
        <el-form-item label="AppSecret">
          <el-input v-model="form.appSecret" placeholder="应用密钥（可选）" show-password />
        </el-form-item>
        <el-form-item label="商户号">
          <el-input v-model="form.mchId" placeholder="商户号（可选）" />
        </el-form-item>
        <el-form-item label="商户密钥">
          <el-input v-model="form.mchKey" placeholder="商户密钥（可选）" show-password />
        </el-form-item>
        <el-form-item label="证书路径">
          <el-input v-model="form.certPath" placeholder="证书文件路径（可选）" />
        </el-form-item>
        <el-form-item label="证书密码">
          <el-input v-model="form.certPassword" placeholder="证书密码（可选）" show-password />
        </el-form-item>
        <el-form-item label="扩展配置">
          <el-input v-model="form.extConfig" type="textarea" :rows="3" placeholder="JSON 格式扩展配置（可选）" />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" :max="9999" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getPaymentAccounts, createPaymentAccount, updatePaymentAccount, deletePaymentAccount, getChannels } from '@/api/admin'
import { confirmDeleteWithGuard } from '@/composables/useResourceDeleteGuard'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import { formatDateTime } from '@/utils/format'
import type { PaymentAccount, Channel } from '@/types'
import { useMerchantScope } from '@/composables/useMerchantScope'

const { merchantFilterLocked, defaultMerchantId, applyMerchantIdForCreate } = useMerchantScope()
const loading = ref(false)
const accountList = ref<PaymentAccount[]>([])
const total = ref(0)
const channelList = ref<Channel[]>([])

const queryForm = reactive({
  page: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  channelId: undefined as number | undefined,
  keyword: '',
})

// 表单相关
const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editId = ref<number | null>(null)

const form = reactive({
  merchantId: '' as string,
  channelId: '' as string | number,
  accountCode: '',
  accountName: '',
  appId: '',
  appSecret: '',
  mchId: '',
  mchKey: '',
  certPath: '',
  certPassword: '',
  extConfig: '',
  enabled: true,
  priority: 0,
  description: '',
})

const formRules: FormRules = {
  channelId: [{ required: true, message: '请选择所属渠道', trigger: 'change' }],
  accountCode: [{ required: true, message: '请输入账号编码', trigger: 'blur' }],
  accountName: [{ required: true, message: '请输入账号名称', trigger: 'blur' }],
}

async function loadChannels() {
  try {
    channelList.value = await getChannels()
  } catch {
    // 静默处理
  }
}

async function loadAccounts() {
  loading.value = true
  try {
    const resp = await getPaymentAccounts(queryForm as Parameters<typeof getPaymentAccounts>[0])
    accountList.value = resp.list
    total.value = resp.total
  } catch {
    ElMessage.error('加载支付账号列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.page = 1
  loadAccounts()
}

function handleReset() {
  Object.assign(queryForm, { page: 1, pageSize: DEFAULT_PAGE_SIZE, channelId: undefined, keyword: '' })
  loadAccounts()
}

function resetForm() {
  Object.assign(form, {
    merchantId: defaultMerchantId.value ?? '',
    channelId: '',
    accountCode: '',
    accountName: '',
    appId: '',
    appSecret: '',
    mchId: '',
    mchKey: '',
    certPath: '',
    certPassword: '',
    extConfig: '',
    enabled: true,
    priority: 0,
    description: '',
  })
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  resetForm()
  formVisible.value = true
}

function openEdit(row: PaymentAccount) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    channelId: row.channelId,
    accountCode: row.accountCode,
    accountName: row.accountName,
    appId: (row as any).appId ?? '',
    appSecret: (row as any).appSecret ?? '',
    mchId: (row as any).mchId ?? '',
    mchKey: (row as any).mchKey ?? '',
    certPath: (row as any).certPath ?? '',
    certPassword: (row as any).certPassword ?? '',
    extConfig: (row as any).extConfig ?? '',
    enabled: row.enabled,
    priority: row.priority ?? 0,
    description: row.description ?? '',
  })
  formVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const data: Record<string, unknown> = {
        channelId: form.channelId,
        accountCode: form.accountCode,
        accountName: form.accountName,
        appId: form.appId || undefined,
        appSecret: form.appSecret || undefined,
        mchId: form.mchId || undefined,
        mchKey: form.mchKey || undefined,
        certPath: form.certPath || undefined,
        certPassword: form.certPassword || undefined,
        extConfig: form.extConfig || undefined,
        enabled: form.enabled,
        priority: form.priority,
        description: form.description || undefined,
      }
      if (!isEdit.value) {
        applyMerchantIdForCreate(data)
      }
      if (isEdit.value && editId.value !== null) {
        await updatePaymentAccount(editId.value, data)
        ElMessage.success('支付账号已更新')
      } else {
        await createPaymentAccount(data)
        ElMessage.success('支付账号已创建')
      }
      formVisible.value = false
      loadAccounts()
    } catch (e: any) {
      const msg = e?.message || (isEdit.value ? '更新支付账号失败' : '创建支付账号失败')
      ElMessage.error(msg)
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row: PaymentAccount) {
  await confirmDeleteWithGuard({
    resourceType: 'PAYMENT_ACCOUNT',
    resourceId: row.id,
    displayName: row.accountName,
    deleteFn: () => deletePaymentAccount(row.id),
    onSuccess: loadAccounts,
  })
}

async function handleToggleEnabled(row: PaymentAccount) {
  try {
    await updatePaymentAccount(row.id, { enabled: row.enabled } as Partial<PaymentAccount>)
    ElMessage.success(row.enabled ? '已启用' : '已禁用')
  } catch {
    row.enabled = !row.enabled
    ElMessage.error('状态更新失败')
  }
}

onMounted(() => {
  loadChannels()
  loadAccounts()
})
</script>

