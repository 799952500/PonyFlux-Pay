<template>
  <div>
    <!-- 顶部工具栏 -->
    <div class="bg-white rounded-xl p-5 card-shadow mb-4">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="商户">
          <el-select v-model="queryForm.merchantId" placeholder="全部商户" clearable style="width: 200px" @change="handleSearch">
            <el-option label="全部商户" value="" />
            <el-option v-for="m in merchantList" :key="m.merchantId" :label="m.merchantName" :value="m.merchantId" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn-primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button class="btn-outline" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
        <el-form-item class="ml-auto">
          <el-button type="primary" class="btn-primary" icon="Plus" @click="openCreateDialog">新建路由</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 路由表格 -->
    <div class="bg-white rounded-xl card-shadow">
      <el-table v-loading="loading" :data="routeList" stripe size="small" class="data-table">
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
            <div class="flex items-center gap-2">
              <span>{{ channelEmoji[row.channelId] ?? '🏦' }}</span>
              <span class="font-medium">{{ row.channelName ?? row.channelId }}</span>
            </div>
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
        <el-table-column label="启用状态" prop="enabled" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled ? 'success' : 'info'" effect="plain">
              {{ row.enabled ? 'ENABLED' : 'DISABLED' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
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
      <el-empty v-if="!loading && !routeList.length" description="暂无路由数据，请先创建路由规则" class="py-12" />

      <!-- 分页 -->
      <div class="flex justify-end p-4">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadRoutes"
          @current-change="loadRoutes"
        />
      </div>
    </div>

    <!-- 新建路由弹窗 -->
    <el-dialog v-model="dialogVisible" title="新建路由" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="商户" prop="merchantId">
          <el-select v-model="form.merchantId" placeholder="请选择商户" style="width: 100%">
            <el-option v-for="m in merchantList" :key="m.merchantId" :label="m.merchantName" :value="m.merchantId" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道" prop="channelId">
          <el-select v-model="form.channelId" placeholder="请先选择渠道" style="width: 100%" @change="onChannelChange">
            <el-option label="💚 微信支付" value="WECHAT_PAY" />
            <el-option label="💳 支付宝" value="ALIPAY" />
            <el-option label="🏦 银联" value="UNION_PAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="账户" prop="accountId">
          <el-select v-model="form.accountId" placeholder="请先选择渠道" style="width: 100%" :disabled="!form.channelId">
            <el-option v-for="acc in filteredAccounts" :key="acc.id" :label="acc.accountName + ' (' + acc.accountNo + ')'" :value="acc.id!" />
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
  listRoutes, createRoute, deleteRoute, toggleRoute, listChannels, listAllAccounts, getMerchantsSimple,
} from '@/api/channel'

const loading = ref(false)
const submitting = ref(false)
const routeList = ref<ChannelRoute[]>([])
const merchantList = ref<Array<{ merchantId: string; merchantName: string }>>([])
const allAccounts = ref<ChannelAccount[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const queryForm = reactive({ merchantId: '' })

const channelEmoji: Record<string, string> = {
  WECHAT_PAY: '💚', ALIPAY: '💳', UNION_PAY: '🏦',
}

const form = reactive({
  merchantId: '',
  channelId: '',
  accountId: '' as number | string,
  priority: 100,
})

const rules: FormRules = {
  merchantId: [{ required: true, message: '请选择商户', trigger: 'change' }],
  channelId: [{ required: true, message: '请选择渠道', trigger: 'change' }],
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }],
}

const filteredAccounts = computed(() => {
  if (!form.channelId) return []
  return allAccounts.value.filter((a) => a.channelId === form.channelId && a.status === 'ENABLED')
})

function onChannelChange() {
  form.accountId = ''
}

async function loadRoutes() {
  loading.value = true
  try {
    const resp = await listRoutes(queryForm.merchantId || undefined)
    const list = resp?.list ?? []
    total.value = list.length
    routeList.value = list.slice((page.value - 1) * pageSize.value, page.value * pageSize.value)
  } catch {
    ElMessage.error('加载路由列表失败')
  } finally {
    loading.value = false
  }
}

async function loadMerchants() {
  try {
    merchantList.value = await getMerchantsSimple()
  } catch { /* ignore */ }
}

async function loadAccounts() {
  try {
    allAccounts.value = await listAllAccounts()
  } catch { /* ignore */ }
}

function handleSearch() { page.value = 1; loadRoutes() }
function handleReset() { Object.assign(queryForm, { merchantId: '' }); handleSearch() }

function openCreateDialog() {
  Object.assign(form, { merchantId: '', channelId: '', accountId: '', priority: 100 })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch { return }

  submitting.value = true
  try {
    const payload = {
      merchantId: form.merchantId,
      channelId: form.channelId,
      accountId: form.accountId,
      priority: form.priority,
      enabled: true,
    }
    await createRoute(payload)
    ElMessage.success('路由创建成功')
    dialogVisible.value = false
    loadRoutes()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '操作失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function handleToggle(row: ChannelRoute) {
  const action = row.enabled ? '禁用' : '启用'
  try {
    await toggleRoute(row.routeId)
    row.enabled = !row.enabled
    ElMessage.success(`路由已${action}`)
  } catch {
    ElMessage.error('操作失败，请重试')
  }
}

async function handleDelete(row: ChannelRoute) {
  try {
    await ElMessageBox.confirm('确认删除该路由规则？删除后不可恢复。', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteRoute(row.routeId)
    ElMessage.success('删除成功')
    loadRoutes()
  } catch {
    // cancelled
  }
}

onMounted(() => {
  loadRoutes()
  loadMerchants()
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
