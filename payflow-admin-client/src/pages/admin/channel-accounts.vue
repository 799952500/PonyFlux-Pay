<template>
  <div>
    <!-- 顶部查询栏 -->
    <div class="bg-white rounded-xl p-5 card-shadow mb-4">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="渠道">
          <el-select v-model="queryForm.channelId" placeholder="全部渠道" clearable style="width: 160px" @change="handleSearch">
            <el-option label="全部渠道" value="" />
            <el-option label="💚 微信支付" value="WECHAT_PAY" />
            <el-option label="💳 支付宝" value="ALIPAY" />
            <el-option label="🏦 银联" value="UNION_PAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
        <el-form-item class="ml-auto">
          <el-button type="primary" class="btn-primary" icon="Plus" @click="openCreateDialog">新建账户</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 账户表格 -->
    <div class="bg-white rounded-xl card-shadow">
      <el-table v-loading="loading" :data="accountList" stripe size="small" class="data-table">
        <el-table-column label="账户编号" prop="accountNo" min-width="160">
          <template #default="{ row }">
            <span class="text-xs tabular-nums font-medium text-[#047857] cursor-pointer">{{ row.accountNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="账户名称" prop="accountName" min-width="160">
          <template #default="{ row }">
            <span class="font-medium">{{ row.accountName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="所属渠道" min-width="130">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <span>{{ channelEmoji[row.channelId] ?? '🏦' }}</span>
              <el-tag size="small" :type="channelTagType[row.channelId]" effect="plain">
                {{ channelNameMap[row.channelId] ?? row.channelId }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="160">
          <template #default="{ row }">
            <span class="text-sm text-[#64748B]">{{ row.remark ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openEditDialog(row)">编辑</el-button>
            <el-button link :type="row.status === 'ENABLED' ? 'warning' : 'success'" size="small" @click.stop="handleToggle(row)">
              {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="small" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="!loading && !accountList.length" description="暂无账户数据" class="py-12" />

      <!-- 分页 -->
      <div class="flex justify-end p-4">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadAccounts"
          @current-change="loadAccounts"
        />
      </div>
    </div>

    <!-- 新建 / 编辑 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属渠道" prop="channelId">
          <el-select v-model="form.channelId" placeholder="请选择渠道" style="width: 100%" :disabled="isEdit">
            <el-option label="💚 微信支付" value="WECHAT_PAY" />
            <el-option label="💳 支付宝" value="ALIPAY" />
            <el-option label="🏦 银联" value="UNION_PAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="账户编号" prop="accountNo">
          <el-input v-model="form.accountNo" placeholder="渠道分配的商户号 / AppId 等" />
        </el-form-item>
        <el-form-item label="账户名称" prop="accountName">
          <el-input v-model="form.accountName" placeholder="便于识别的账户别名" />
        </el-form-item>
        <el-form-item label="配置 JSON" prop="configJson">
          <el-input v-model="form.configJson" type="textarea" :rows="5" placeholder='{"appId":"","appSecret":"","mchId":""}' style="font-family: 'Courier New', monospace;" />
          <div class="text-xs text-[#94A3B8] mt-1">请输入 JSON 格式的渠道配置信息</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息..." />
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
import { getChannelAccounts, createChannelAccount, updateChannelAccount, toggleChannelAccount, deleteChannelAccount, getChannels } from '@/api/admin'

interface ChannelAccount {
  id?: number
  accountNo: string
  accountName: string
  channelId: string
  channelName?: string
  configJson?: string
  remark?: string
  status: string
  createdAt?: string
  updatedAt?: string
}

const loading = ref(false)
const submitting = ref(false)
const accountList = ref<ChannelAccount[]>([])
const allAccounts = ref<ChannelAccount[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentAccount = ref<ChannelAccount | null>(null)
const formRef = ref<FormInstance>()
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const queryForm = reactive({ channelId: '', status: '' })

const channelEmoji: Record<string, string> = {
  WECHAT_PAY: '💚', ALIPAY: '💳', UNION_PAY: '🏦',
}
const channelTagType: Record<string, string> = {
  WECHAT_PAY: 'success', ALIPAY: 'primary', UNION_PAY: 'warning',
}
const channelNameMap: Record<string, string> = {
  WECHAT_PAY: '微信支付', ALIPAY: '支付宝', UNION_PAY: '银联',
}

const dialogTitle = computed(() => isEdit.value ? '编辑账户' : '新建账户')

const defaultForm = () => ({
  accountNo: '',
  accountName: '',
  channelId: '',
  configJson: '{\n  "appId": "",\n  "appSecret": "",\n  "mchId": ""\n}',
  remark: '',
  status: 'ENABLED',
})

const form = reactive(defaultForm())

const rules: FormRules = {
  channelId: [{ required: true, message: '请选择所属渠道', trigger: 'change' }],
  accountNo: [{ required: true, message: '请输入账户编号', trigger: 'blur' }],
  accountName: [{ required: true, message: '请输入账户名称', trigger: 'blur' }],
  configJson: [
    { required: true, message: '请输入配置 JSON', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        try { JSON.parse(value); callback() } catch { callback(new Error('JSON 格式不正确')) }
      },
      trigger: 'blur',
    },
  ],
}

async function loadAccounts() {
  loading.value = true
  try {
    const data: any = await getChannelAccounts()
    allAccounts.value = data?.list ?? data ?? []
    filterAndPaginate()
  } catch {
    ElMessage.error('加载账户列表失败')
  } finally {
    loading.value = false
  }
}

function filterAndPaginate() {
  let list = allAccounts.value
  if (queryForm.channelId) {
    list = list.filter(a => a.channelId === queryForm.channelId)
  }
  if (queryForm.status) {
    list = list.filter(a => a.status === queryForm.status)
  }
  total.value = list.length
  accountList.value = list.slice((page.value - 1) * pageSize.value, page.value * pageSize.value)
}

function handleSearch() {
  page.value = 1
  filterAndPaginate()
}

function handleReset() {
  queryForm.channelId = ''
  queryForm.status = ''
  handleSearch()
}

function openCreateDialog() {
  isEdit.value = false
  currentAccount.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEditDialog(row: ChannelAccount) {
  isEdit.value = true
  currentAccount.value = row
  Object.assign(form, {
    channelId: row.channelId,
    accountNo: row.accountNo,
    accountName: row.accountName,
    configJson: row.configJson ?? '{}',
    remark: row.remark ?? '',
    status: row.status,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }

  submitting.value = true
  try {
    if (isEdit.value && currentAccount.value?.id) {
      await updateChannelAccount(currentAccount.value.id, form)
      ElMessage.success('账户更新成功')
    } else {
      await createChannelAccount(form)
      ElMessage.success('账户创建成功')
    }
    dialogVisible.value = false
    loadAccounts()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '操作失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function handleToggle(row: ChannelAccount) {
  const action = row.status === 'ENABLED' ? '禁用' : '启用'
  try {
    if (!row.id) return
    await toggleChannelAccount(row.id)
    row.status = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
    ElMessage.success(`${row.accountName} 已${action}`)
  } catch {
    ElMessage.error('操作失败，请重试')
  }
}

async function handleDelete(row: ChannelAccount) {
  try {
    await ElMessageBox.confirm(`确认删除账户「${row.accountName}」？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    if (!row.id) return
    await deleteChannelAccount(row.id)
    ElMessage.success('删除成功')
    loadAccounts()
  } catch { /* cancelled */ }
}

onMounted(() => {
  loadAccounts()
})
</script>

<style scoped>
.card-shadow {
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(99, 102, 241, 0.08);
}

.btn-primary {
  background: linear-gradient(135deg, #065f46 0%, #0d9488 100%);
  border: none;
  color: white;
  border-radius: 10px;
  padding: 9px 20px;
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
  padding: 9px 20px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-outline:hover {
  border-color: #047857;
  color: #047857;
}

.data-table :deep(th) {
  background: linear-gradient(90deg, rgba(99,102,241,0.06) 0%, rgba(129,140,248,0.04) 100%) !important;
  color: #374151;
  font-weight: 600;
  font-size: 13px;
  border-bottom: 2px solid rgba(99, 102, 241, 0.1) !important;
}

.data-table :deep(tr:hover td) {
  background: rgba(99, 102, 241, 0.03) !important;
}

.data-table :deep(td) {
  border-bottom: 1px solid rgba(99, 102, 241, 0.06) !important;
}
</style>
