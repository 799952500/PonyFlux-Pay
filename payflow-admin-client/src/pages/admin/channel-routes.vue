<template>
  <div class="page-table-shell">
    <div class="filter-bar">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item v-if="!merchantFilterLocked || authorizedMerchantIds.length > 1" label="商户">
          <el-select v-model="queryForm.merchantId" placeholder="全部商户" clearable style="width: 200px" @change="handleSearch">
            <el-option v-if="!merchantFilterLocked" label="全部商户" value="" />
            <el-option v-for="m in merchantList" :key="m.merchantId" :label="m.merchantName" :value="m.merchantId" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <TableToolbar title="支付路由列表" :total="total">
        <template #actions>
          <el-button type="primary" class="btn-primary" icon="Plus" @click="openCreateDialog">新建支付路由</el-button>
        </template>
      </TableToolbar>
      <el-table table-layout="auto" v-loading="loading" :data="routeList" stripe size="small" class="data-table">
        <el-table-column label="路由ID" prop="routeId" min-width="100">
          <template #default="{ row }">
            <span class="text-xs tabular-nums font-medium text-[#047857]">{{ row.routeId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商户ID" prop="merchantId" min-width="140">
          <template #default="{ row }">
            <span class="text-xs tabular-nums text-[#64748B]">{{ row.merchantId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商户名称" min-width="160">
          <template #default="{ row }">
            <span class="font-medium">{{ row.merchantName ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="渠道" min-width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="channelTagType(row.channelName ?? row.channelCode)" effect="light">
              {{ channelLabel(row.channelName ?? row.channelCode ?? row.channelId) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账户名称" prop="accountName" min-width="150">
          <template #default="{ row }">
            <span class="text-sm">{{ row.accountName ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="账户编号" prop="accountNo" min-width="140">
          <template #default="{ row }">
            <span class="text-xs tabular-nums text-[#64748B]">{{ row.accountNo ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="优先级" prop="priority" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="warning" effect="plain">{{ row.priority ?? 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用状态" prop="enabled" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="tagTypeOf(ENABLE_STATUS_TAG, row.enabled ? 'ENABLED' : 'DISABLED')"
              effect="plain"
            >
              {{ labelOf(ENABLE_STATUS_LABEL, row.enabled ? 'ENABLED' : 'DISABLED') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="140" class-name="col-actions" fixed="right">
          <template #default="{ row }">
            <el-button link :type="row.enabled ? 'warning' : 'success'" size="small"
              @click.stop="handleToggle(row)">
              {{ row.enabled ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="small" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="!loading && !routeList.length" description="暂无支付路由，请先创建支付路由" class="py-12" />

      <!-- 分页 -->
      <AdminPagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @size-change="loadRoutes"
        @current-change="loadRoutes"
      />
    </div>

    <!-- 新建支付路由弹窗 -->
    <el-dialog v-model="dialogVisible" title="新建支付路由" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="商户" prop="merchantId">
          <el-input v-if="merchantFilterLocked && authorizedMerchantIds.length <= 1" :model-value="form.merchantId" disabled />
          <el-select v-else v-model="form.merchantId" placeholder="请选择商户" style="width: 100%">
            <el-option v-for="m in merchantList" :key="m.merchantId" :label="m.merchantName" :value="m.merchantId" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道" prop="channelId">
          <el-select v-model="form.channelId" placeholder="请选择渠道" style="width: 100%" filterable @change="onChannelChange">
            <el-option
              v-for="ch in channelSelectList"
              :key="ch.id"
              :label="`${ch.channelName}（${ch.channelCode}）`"
              :value="ch.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="支付账号" prop="accountId">
          <el-select v-model="form.accountId" placeholder="请先选择渠道" style="width: 100%" :disabled="!form.channelId">
            <el-option v-for="acc in filteredAccounts" :key="acc.id" :label="`${acc.accountName}（${acc.accountNo}）`" :value="acc.id!" />
            <template #empty>
              <div class="text-center py-3 text-xs text-[#94A3B8]">该渠道下暂无账户</div>
            </template>
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" :min="0" :max="9999" controls-position="right" style="width: 100%" />
          <div class="text-xs text-[#94A3B8] mt-1">数字越小优先级越高（0 最高）</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getChannelRoutes,
  createChannelRoute,
  deleteChannelRoute,
  toggleChannelRoute,
  getChannels,
  getPaymentAccounts,
  getMerchantsSimple,
} from '@/api/admin'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import {
  channelLabel,
  channelTagType,
  ENABLE_STATUS_LABEL,
  ENABLE_STATUS_TAG,
  labelOf,
  tagTypeOf,
} from '@/utils/format'
import type { Channel } from '@/types'
import { useMerchantScope } from '@/composables/useMerchantScope'

const {
  merchantFilterLocked,
  authorizedMerchantIds,
  filterMerchantOptions,
  applyDefaultMerchantFilter,
  applyMerchantIdForCreate,
} = useMerchantScope()

interface ChannelAccount {
  id?: number
  channelId: number
  accountNo: string
  accountName: string
  status: 'ENABLED' | 'DISABLED' | string
}

interface LocalChannelRoute {
  id: number
  routeId?: number
  merchantId: string
  merchantName?: string
  channelId: string
  channelName?: string
  accountId: number | string
  accountNo?: string
  accountName?: string
  priority: number
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

const loading = ref(false)
const submitting = ref(false)
const routeList = ref<LocalChannelRoute[]>([])
const merchantList = ref<Array<{ merchantId: string; merchantName: string }>>([])
const channelSelectList = ref<Channel[]>([])
const allAccounts = ref<ChannelAccount[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const page = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const total = ref(0)

const queryForm = reactive({ merchantId: '' })

const form = reactive({
  merchantId: '',
  channelId: '' as number | '',
  accountId: '' as number | string,
  priority: 100,
})

const rules: FormRules = {
  merchantId: [{ required: true, message: '请选择商户', trigger: 'change' }],
  channelId: [{ required: true, message: '请选择渠道', trigger: 'change' }],
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }],
}

const filteredAccounts = computed(() => {
  if (form.channelId === '' || form.channelId == null) return []
  const cid = Number(form.channelId)
  return allAccounts.value.filter(
    (a: ChannelAccount) => Number(a.channelId) === cid && a.status === 'ENABLED'
  )
})

function onChannelChange() {
  form.accountId = ''
}

async function loadRoutes() {
  loading.value = true
  try {
    const resp: any = await getChannelRoutes({ merchantId: queryForm.merchantId || undefined })
    const rawList = resp?.data?.list ?? resp?.list ?? []
    routeList.value = rawList.map((item: any) => ({ ...item, routeId: item.id }))
    total.value = resp?.data?.total ?? resp?.total ?? rawList.length
  } catch {
    ElMessage.error('加载支付路由列表失败')
  } finally {
    loading.value = false
  }
}

async function loadMerchants() {
  try {
    merchantList.value = filterMerchantOptions(await getMerchantsSimple())
    applyDefaultMerchantFilter(queryForm)
  } catch { /* ignore */ }
}

async function loadAccounts() {
  try {
    const res = await getPaymentAccounts({ page: 1, pageSize: 500 })
    const raw = res?.list ?? []
    allAccounts.value = raw.map(
      (a: { id?: number; channelId?: number; accountCode?: string; accountName?: string; enabled?: boolean }) => ({
        id: a.id,
        channelId: Number(a.channelId),
        accountNo: a.accountCode ?? '',
        accountName: a.accountName ?? '',
        status: a.enabled ? 'ENABLED' : 'DISABLED',
      })
    )
  } catch {
    /* ignore */
  }
}

async function loadChannelOptions() {
  try {
    const data = await getChannels()
    channelSelectList.value = Array.isArray(data) ? data : []
  } catch {
    channelSelectList.value = []
  }
}

function handleSearch() { page.value = 1; loadRoutes() }
function handleReset() { Object.assign(queryForm, { merchantId: '' }); handleSearch() }

function openCreateDialog() {
  Object.assign(form, { merchantId: '', channelId: '', accountId: '' as number | string, priority: 100 })
  applyDefaultMerchantFilter(form)
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch { return }

  submitting.value = true
  try {
    const payload: Record<string, unknown> = {
      merchantId: form.merchantId,
      channelId: Number(form.channelId),
      paymentAccountId: Number(form.accountId),
      priority: form.priority,
      enabled: true,
    }
    applyMerchantIdForCreate(payload)
    await createChannelRoute(payload as unknown as Partial<import('@/types').ChannelRoute>)
    ElMessage.success('支付路由创建成功')
    dialogVisible.value = false
    loadRoutes()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '操作失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function handleToggle(row: LocalChannelRoute) {
  const action = row.enabled ? '禁用' : '启用'
  try {
    await toggleChannelRoute(row.id)
    row.enabled = !row.enabled
    ElMessage.success(`支付路由已${action}`)
  } catch {
    ElMessage.error('操作失败，请重试')
  }
}

async function handleDelete(row: LocalChannelRoute) {
  try {
    await ElMessageBox.confirm('确认删除该支付路由？删除后不可恢复。', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteChannelRoute(row.id)
    ElMessage.success('删除成功')
    loadRoutes()
  } catch {
    // cancelled
  }
}

onMounted(() => {
  loadChannelOptions()
  loadRoutes()
  loadMerchants()
  loadAccounts()
})
</script>

