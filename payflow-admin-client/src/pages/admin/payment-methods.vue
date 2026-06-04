<template>
  <div class="page-table-shell">
    <el-alert
      class="mb-4"
      type="info"
      :closable="false"
      show-icon
      title="支付方式与支付账号"
      description="支付方式只定义「用什么能力收款」（如 WECHAT_H5、ALIPAY_WAP）。appId、商户号、密钥、证书等渠道凭证请在「支付账号」维护，并在「商户管理 → 支付路由」里绑定到对应支付方式。"
    />
    <div class="filter-bar filter-bar--stacked">
      <div class="filter-row">
        <span class="filter-label">渠道快筛</span>
        <el-radio-group v-model="selectedChannel" size="default" @change="handleChannelChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="WECHAT">微信支付</el-radio-button>
          <el-radio-button value="ALIPAY">支付宝</el-radio-button>
          <el-radio-button value="UNION">银联</el-radio-button>
        </el-radio-group>
      </div>
      <el-form :inline="true" :model="queryForm" size="default" class="filter-bar__form">
        <el-form-item label="所属渠道">
          <el-select v-model="queryForm.channel" placeholder="全部渠道" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="微信支付" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银联" value="UNION" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-input v-model="queryForm.name" placeholder="支付方式名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-bar__actions">
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <TableToolbar title="支付方式列表" :total="total">
        <template #hint>
          <span v-if="activeChannelIdFromQuery != null">
            已按渠道筛选（渠道 ID：{{ activeChannelIdFromQuery }}）
            <el-button type="primary" link class="!p-0 !h-auto align-baseline ml-1" @click="clearChannelQuery">清除筛选</el-button>
          </span>
        </template>
        <template #actions>
          <el-button v-permission="'payment_method:create'" type="primary" class="btn-primary" icon="Plus" @click="openAdd">
            新建支付方式
          </el-button>
        </template>
      </TableToolbar>

      <el-table table-layout="auto" v-loading="loading" :data="tableData" stripe size="small" class="data-table">
        <el-table-column label="支付方式编号" prop="methodCode" min-width="148">
          <template #default="{ row }">
            <span class="cell-mono pf-link cursor-pointer" @click.stop="openDetail(row)">{{ row.methodCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="支付方式名称" prop="methodNameZhCn" min-width="160">
          <template #default="{ row }">
            <span class="font-medium text-slate-800">{{ row.methodNameZhCn ?? row.methodName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="所属渠道" prop="channelType" width="112" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="channelTagType(row.channelType)" effect="light">
              {{ channelLabel(row.channelType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" prop="priority" width="88" align="center">
          <template #default="{ row }">{{ row.priority ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="描述" prop="description" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description ?? row.remark ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="88" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'danger'" effect="plain">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" min-width="168" class-name="col-datetime" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-datetime">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="canManagePaymentMethods" label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-permission="'payment_method:edit'" link type="primary" size="small" @click.stop="openEdit(row)">编辑</el-button>
            <el-button v-permission="'payment_method:delete'" link type="danger" size="small" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AdminPagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <el-alert
      v-if="!platformAdmin"
      class="mb-4"
      type="info"
      :closable="false"
      show-icon
      title="支付方式为平台公共定义，商户管理员仅可查看已启用的方式；账号与路由请在「支付账号」「商户管理」中配置。"
    />

    <!-- 新建/编辑弹窗（仅平台管理员） -->
    <el-dialog
      v-if="platformAdmin"
      v-model="dialogVisible"
      :title="isEdit ? '编辑支付方式' : '新建支付方式'"
      width="720px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px" size="default">
        <el-form-item label="所属渠道" prop="channelType">
          <el-select v-model="form.channelType" placeholder="请选择渠道" style="width: 100%">
            <el-option label="微信支付" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银联" value="UNION" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式编号" prop="methodCode">
          <el-input v-model="form.methodCode" placeholder="如: WECHAT_NATIVE" />
        </el-form-item>
        <el-form-item label="展示名（简体中文）" prop="methodNameZhCn">
          <el-input v-model="form.methodNameZhCn" placeholder="如: 微信扫码支付" />
        </el-form-item>
        <el-form-item label="展示名（繁體中文）" prop="methodNameZhTw">
          <el-input v-model="form.methodNameZhTw" placeholder="如: 微信掃碼支付" />
        </el-form-item>
        <el-form-item label="展示名（English）" prop="methodNameEn">
          <el-input v-model="form.methodNameEn" placeholder="e.g. WeChat Pay (QR)" />
        </el-form-item>
        <el-form-item label="描述（简体中文）" prop="descriptionZhCn">
          <el-input v-model="form.descriptionZhCn" type="textarea" :rows="2" placeholder="支付方式说明" />
        </el-form-item>
        <el-form-item label="描述（繁體中文）" prop="descriptionZhTw">
          <el-input v-model="form.descriptionZhTw" type="textarea" :rows="2" placeholder="支付方式說明" />
        </el-form-item>
        <el-form-item label="描述（English）" prop="descriptionEn">
          <el-input v-model="form.descriptionEn" type="textarea" :rows="2" placeholder="Payment method description" />
        </el-form-item>
        <el-form-item label="扩展配置" prop="extraConfig">
          <el-input
            v-model="form.extraConfig"
            type="textarea"
            :rows="3"
            placeholder='场景参数 JSON，如: {"tradeType":"MWEB"}；勿填写 appId/密钥'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="支付方式详情" width="580px" destroy-on-close>
      <div v-if="detailLoading" class="p-4"><el-skeleton animated :rows="8" /></div>
      <div v-else-if="currentRow" class="space-y-5">
        <section>
          <h3 class="text-sm font-semibold text-gray-700 mb-3 border-b pb-2">基本信息</h3>
          <dl class="grid grid-cols-2 gap-y-3 gap-x-4 text-sm">
            <dt class="text-gray-400">支付方式编号</dt><dd class="text-gray-800 font-medium">{{ currentRow.methodCode }}</dd>
            <dt class="text-gray-400">展示名（简体）</dt><dd class="text-gray-800">{{ currentRow.methodNameZhCn ?? currentRow.methodName }}</dd>
            <dt class="text-gray-400">展示名（繁体）</dt><dd class="text-gray-800">{{ currentRow.methodNameZhTw ?? '—' }}</dd>
            <dt class="text-gray-400">展示名（英文）</dt><dd class="text-gray-800">{{ currentRow.methodNameEn ?? '—' }}</dd>
            <dt class="text-gray-400">所属渠道</dt><dd><el-tag size="small" :type="channelTagType(currentRow.channelId ?? currentRow.channelType)">{{ currentRow.channelName ?? channelLabel(currentRow.channelType) }}</el-tag></dd>
            <dt class="text-gray-400">状态</dt><dd><el-tag size="small" :type="currentRow.status === 'ACTIVE' ? 'success' : 'danger'">{{ currentRow.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag></dd>
            <dt class="text-gray-400">创建时间</dt><dd class="text-gray-800 tabular-nums">{{ formatDateTime(currentRow.createdAt) }}</dd>
          </dl>
        </section>
        <section>
          <h3 class="text-sm font-semibold text-gray-700 mb-3 border-b pb-2">说明</h3>
          <p class="text-sm text-gray-600 leading-relaxed">
            渠道凭证（appId、商户号、密钥、证书）在
            <router-link to="/admin/payment-accounts" class="text-primary">支付账号</router-link>
            中配置，并通过商户支付路由关联到本支付方式。
          </p>
        </section>
        <section v-if="currentRow.extraConfig">
          <h3 class="text-sm font-semibold text-gray-700 mb-3 border-b pb-2">扩展配置</h3>
          <pre class="bg-gray-50 rounded p-3 text-xs font-mono text-gray-700 overflow-x-auto">{{ currentRow.extraConfig }}</pre>
        </section>
        <section v-if="currentRow.remark">
          <h3 class="text-sm font-semibold text-gray-700 mb-3 border-b pb-2">备注</h3>
          <p class="text-sm text-gray-600">{{ currentRow.remark }}</p>
        </section>
      </div>
      <template #footer><el-button type="primary" @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'
import { getPaymentMethods, deletePaymentMethod, createPaymentMethod, updatePaymentMethod, getChannels, getPaymentMethodById } from '@/api/admin'
import { confirmDeleteWithGuard } from '@/composables/useResourceDeleteGuard'
import { channelLabel, channelTagType, formatDateTime } from '@/utils/format'
import { isPlatformAdmin } from '@/utils/adminAccess'
import { usePermission } from '@/composables/usePermission'

const platformAdmin = isPlatformAdmin()
const { hasPermission } = usePermission()
const canManagePaymentMethods = computed(() =>
  hasPermission(['payment_method:create', 'payment_method:edit', 'payment_method:delete'], 'OR'),
)
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const isEdit = ref(false)
const currentRow = ref<any>(null)
const selectedChannel = ref('')
const channelOptions = ref<any[]>([])

const queryForm = reactive({
  page: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  channel: '',
  name: '',
  status: '',
})

const formRef = ref<FormInstance>()
const form = reactive({
  id: '',
  channelType: '',
  methodCode: '',
  methodNameZhCn: '',
  methodNameZhTw: '',
  methodNameEn: '',
  descriptionZhCn: '',
  descriptionZhTw: '',
  descriptionEn: '',
  extraConfig: '',
})

const requiredTrim = (message: string) => ({
  required: true,
  message,
  trigger: 'blur' as const,
  validator: (_: unknown, value: string, cb: (e?: Error) => void) => {
    if (value != null && String(value).trim()) cb()
    else cb(new Error(message))
  },
})

const rules: FormRules = {
  channelType: [{ required: true, message: '请选择所属渠道', trigger: 'change' }],
  methodCode: [{ required: true, message: '请输入支付方式编号', trigger: 'blur' }],
  methodNameZhCn: [requiredTrim('请输入简体中文展示名')],
  methodNameZhTw: [requiredTrim('请输入繁体中文展示名')],
  methodNameEn: [requiredTrim('请输入英文展示名')],
  descriptionZhCn: [requiredTrim('请输入简体中文描述')],
  descriptionZhTw: [requiredTrim('请输入繁体中文描述')],
  descriptionEn: [requiredTrim('请输入英文描述')],
}

/** 从渠道管理抽屉跳转时的 ?channelId= */
const activeChannelIdFromQuery = computed(() => {
  const raw = route.query.channelId
  const s = Array.isArray(raw) ? raw[0] : raw
  if (s == null || s === '') return null
  const n = Number(s)
  return Number.isFinite(n) ? n : null
})

function clearChannelQuery() {
  router.replace({ path: '/admin/payment-methods' })
}

async function loadChannels() {
  try {
    const res: any = await getChannels()
    channelOptions.value = Array.isArray(res) ? res : (res?.list ?? [])
  } catch {
    // ignore
  }
}

function buildListParams() {
  const params: Parameters<typeof getPaymentMethods>[0] = {
    page: queryForm.page,
    pageSize: queryForm.pageSize,
  }
  const fid = activeChannelIdFromQuery.value
  if (fid != null) {
    params.channelId = fid
  } else if (queryForm.channel) {
    params.channelType = queryForm.channel
  }
  const name = queryForm.name?.trim()
  if (name) {
    params.keyword = name
  }
  if (queryForm.status) {
    params.status = queryForm.status
  }
  return params
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getPaymentMethods(buildListParams())
    tableData.value = Array.isArray(res) ? res : (res.list ?? [])
    total.value = Number(res?.total ?? tableData.value.length)
  } catch {
    ElMessage.error('加载支付方式列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.page = 1
  loadData()
}

function handleReset() {
  selectedChannel.value = ''
  Object.assign(queryForm, { page: 1, pageSize: DEFAULT_PAGE_SIZE, channel: '', name: '', status: '' })
  if (activeChannelIdFromQuery.value != null) {
    clearChannelQuery()
    return
  }
  loadData()
}

function handleChannelChange() {
  queryForm.channel = selectedChannel.value
  queryForm.page = 1
  loadData()
}

function openAdd() {
  isEdit.value = false
  Object.assign(form, {
    id: '',
    channelType: '',
    methodCode: '',
    methodNameZhCn: '',
    methodNameZhTw: '',
    methodNameEn: '',
    descriptionZhCn: '',
    descriptionZhTw: '',
    descriptionEn: '',
    extraConfig: '',
  })
  dialogVisible.value = true
}

function openEdit(row: any) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    channelType: row.channelType,
    methodCode: row.methodCode,
    methodNameZhCn: row.methodNameZhCn ?? row.methodName ?? '',
    methodNameZhTw: row.methodNameZhTw ?? row.methodName ?? '',
    methodNameEn: row.methodNameEn ?? row.methodName ?? '',
    descriptionZhCn: row.descriptionZhCn ?? row.description ?? '',
    descriptionZhTw: row.descriptionZhTw ?? row.description ?? '',
    descriptionEn: row.descriptionEn ?? row.description ?? '',
    extraConfig: row.extraConfig ?? row.configJson ?? '',
  })
  dialogVisible.value = true
}

async function openDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  currentRow.value = null
  try {
    currentRow.value = await getPaymentMethodById(Number(row.id))
  } catch {
    currentRow.value = row
    ElMessage.warning('已展示列表行数据，完整详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload: Record<string, unknown> = {
        methodCode: form.methodCode,
        methodNameZhCn: form.methodNameZhCn.trim(),
        methodNameZhTw: form.methodNameZhTw.trim(),
        methodNameEn: form.methodNameEn.trim(),
        descriptionZhCn: form.descriptionZhCn.trim(),
        descriptionZhTw: form.descriptionZhTw.trim(),
        descriptionEn: form.descriptionEn.trim(),
        configJson: form.extraConfig || undefined,
      }
      const selected = channelOptions.value.find(c => c.channelType === form.channelType)
      if (selected) {
        payload.channelId = selected.id
      }

      if (isEdit.value) {
        await updatePaymentMethod(Number(form.id), payload)
        ElMessage.success('更新成功')
      } else {
        await createPaymentMethod(payload)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    } finally {
      submitLoading.value = false
    }
  })
}

async function handleDelete(row: any) {
  await confirmDeleteWithGuard({
    resourceType: 'PAYMENT_METHOD',
    resourceId: row.id,
    displayName: row.methodName,
    deleteFn: () => deletePaymentMethod(row.id),
    onSuccess: loadData,
  })
}

watch(
  () => route.query.channelId,
  () => {
    queryForm.page = 1
    loadData()
  }
)

onMounted(() => {
  loadData()
  loadChannels()
})
</script>

