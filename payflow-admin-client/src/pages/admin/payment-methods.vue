<template>
  <div>
    <!-- 顶部渠道筛选 -->
    <div class="bg-white rounded-xl p-4 card-shadow mb-4 flex items-center gap-3">
      <span class="text-sm text-gray-500 font-medium">按渠道筛选：</span>
      <el-radio-group v-model="selectedChannel" size="default" @change="handleChannelChange">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="WECHAT">微信支付</el-radio-button>
        <el-radio-button value="ALIPAY">支付宝</el-radio-button>
        <el-radio-button value="UNIONPAY">银联</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 查询表单 -->
    <div class="bg-white rounded-xl p-5 card-shadow mb-4">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="所属渠道">
          <el-select v-model="queryForm.channel" placeholder="全部渠道" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="微信支付" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银联" value="UNIONPAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式"><el-input v-model="queryForm.name" placeholder="支付方式名称" clearable style="width: 180px" @keyup.enter="handleSearch" /></el-form-item>
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
        <span class="text-sm font-semibold text-gray-600">支付方式列表</span>
        <el-button type="primary" class="btn-primary" icon="Plus" @click="openAdd">新建支付方式</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" stripe size="small">
        <el-table-column label="支付方式编号" prop="methodCode" min-width="140">
          <template #default="{ row }"><span class="text-xs tabular-nums font-medium text-primary cursor-pointer" @click="openDetail(row)">{{ row.methodCode }}</span></template>
        </el-table-column>
        <el-table-column label="支付方式名称" prop="methodName" min-width="160">
          <template #default="{ row }"><span class="font-medium">{{ row.methodName }}</span></template>
        </el-table-column>
        <el-table-column label="所属渠道" prop="channelType" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="channelTagType[row.channelType]">{{ channelLabel[row.channelType] ?? row.channelType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="appId" prop="appId" min-width="200">
          <template #default="{ row }"><span class="text-xs font-mono text-gray-500">{{ maskValue(row.appId) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="90">
          <template #default="{ row }"><el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openEdit(row)">编辑</el-button>
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

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑支付方式' : '新建支付方式'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px" size="default">
        <el-form-item label="所属渠道" prop="channelType">
          <el-select v-model="form.channelType" placeholder="请选择渠道" style="width: 100%">
            <el-option label="微信支付" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银联" value="UNIONPAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式编号" prop="methodCode">
          <el-input v-model="form.methodCode" placeholder="如: WECHAT_NATIVE" />
        </el-form-item>
        <el-form-item label="支付方式名称" prop="methodName">
          <el-input v-model="form.methodName" placeholder="如: 微信扫码支付" />
        </el-form-item>
        <el-form-item label="appId" prop="appId">
          <el-input v-model="form.appId" placeholder="渠道分配的 appId" />
        </el-form-item>
        <el-form-item label="appSecret" prop="appSecret">
          <el-input v-model="form.appSecret" placeholder="渠道分配的 appSecret" />
        </el-form-item>
        <el-form-item label="mchId" prop="mchId">
          <el-input v-model="form.mchId" placeholder="商户号 mchId" />
        </el-form-item>
        <el-form-item label="证书路径" prop="certPath">
          <el-input v-model="form.certPath" placeholder="如: /cert/apiclient_cert.p12" />
        </el-form-item>
        <el-form-item label="证书密码" prop="certPassword">
          <el-input v-model="form.certPassword" placeholder="证书密码" show-password />
        </el-form-item>
        <el-form-item label="扩展配置" prop="extraConfig">
          <el-input v-model="form.extraConfig" type="textarea" :rows="3" placeholder='JSON 格式，如: {"key": "value"}' />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="支付方式详情" width="580px" destroy-on-close>
      <div v-if="currentRow" class="space-y-5">
        <section>
          <h3 class="text-sm font-semibold text-gray-700 mb-3 border-b pb-2">基本信息</h3>
          <dl class="grid grid-cols-2 gap-y-3 gap-x-4 text-sm">
            <dt class="text-gray-400">支付方式编号</dt><dd class="text-gray-800 font-medium">{{ currentRow.methodCode }}</dd>
            <dt class="text-gray-400">支付方式名称</dt><dd class="text-gray-800">{{ currentRow.methodName }}</dd>
            <dt class="text-gray-400">所属渠道</dt><dd><el-tag size="small" :type="channelTagType[currentRow.channelId]">{{ currentRow.channelName }}</el-tag></dd>
            <dt class="text-gray-400">状态</dt><dd><el-tag size="small" :type="currentRow.status === 'ACTIVE' ? 'success' : 'danger'">{{ currentRow.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag></dd>
            <dt class="text-gray-400">创建时间</dt><dd class="text-gray-800">{{ currentRow.createdAt }}</dd>
          </dl>
        </section>
        <section>
          <h3 class="text-sm font-semibold text-gray-700 mb-3 border-b pb-2">渠道配置</h3>
          <dl class="grid grid-cols-2 gap-y-3 gap-x-4 text-sm">
            <dt class="text-gray-400">appId</dt><dd class="text-gray-800 font-mono">{{ currentRow.appId ?? '—' }}</dd>
            <dt class="text-gray-400">mchId</dt><dd class="text-gray-800 font-mono">{{ currentRow.mchId ?? '—' }}</dd>
            <dt class="text-gray-400">appSecret</dt><dd class="text-gray-800">{{ currentRow.appSecret ? '******' : '—' }}</dd>
            <dt class="text-gray-400">证书路径</dt><dd class="text-gray-800">{{ currentRow.certPath ?? '—' }}</dd>
            <dt class="text-gray-400">证书密码</dt><dd class="text-gray-800">{{ currentRow.certPassword ? '******' : '—' }}</dd>
          </dl>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getPaymentMethods, deletePaymentMethod, createPaymentMethod, updatePaymentMethod, getChannels } from '@/api/admin'

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
  pageSize: 20,
  channel: '',
  name: '',
  status: '',
})

const formRef = ref<FormInstance>()
const form = reactive({
  id: '',
  channelType: '',
  methodCode: '',
  methodName: '',
  appId: '',
  appSecret: '',
  mchId: '',
  certPath: '',
  certPassword: '',
  extraConfig: '',
  remark: '',
})

const rules: FormRules = {
  channelType: [{ required: true, message: '请选择所属渠道', trigger: 'change' }],
  methodCode: [{ required: true, message: '请输入支付方式编号', trigger: 'blur' }],
  methodName: [{ required: true, message: '请输入支付方式名称', trigger: 'blur' }],
}

const channelTagType: Record<string, string> = {
  WECHAT: 'success',
  ALIPAY: 'primary',
  UNIONPAY: 'warning',
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

async function loadChannels() {
  try {
    const res: any = await getChannels()
    channelOptions.value = Array.isArray(res) ? res : (res?.list ?? [])
  } catch {
    // ignore
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getPaymentMethods({ page: queryForm.page, pageSize: queryForm.pageSize })
    tableData.value = Array.isArray(res) ? res : (res.list ?? [])
    total.value = res.total ?? tableData.value.length
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
  Object.assign(queryForm, { page: 1, pageSize: 20, channel: '', name: '', status: '' })
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
    id: '', channelType: '', methodCode: '', methodName: '',
    appId: '', appSecret: '', mchId: '',
    certPath: '', certPassword: '', extraConfig: '', remark: '',
  })
  dialogVisible.value = true
}

function openEdit(row: any) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    channelType: row.channelType,
    methodCode: row.methodCode,
    methodName: row.methodName,
    appId: row.appId ?? '',
    appSecret: '',
    mchId: row.mchId ?? '',
    certPath: row.certPath ?? '',
    certPassword: '',
    extraConfig: row.extraConfig ?? '',
    remark: row.remark ?? '',
  })
  dialogVisible.value = true
}

function openDetail(row: any) {
  currentRow.value = row
  detailVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload: Record<string, any> = { ...form }
      // channelType 字符串转 channelId 数字（channelOptions 来自 getChannels）
      const selected = channelOptions.value.find(c => c.channelType === form.channelType)
      if (selected) {
        payload.channelId = selected.id
      }
      delete payload.channelType
      // 不传空字符串的敏感字段
      if (!payload.appSecret) delete payload.appSecret
      if (!payload.certPassword) delete payload.certPassword

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
  await ElMessageBox.confirm(`确定要删除支付方式「${row.methodName}」吗？`, '删除确认', { type: 'warning' })
  try {
    await deletePaymentMethod(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadData()
  loadChannels()
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
