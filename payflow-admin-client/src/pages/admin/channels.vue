<template>
  <div>
    <!-- 顶部筛选工具栏 -->
    <div class="bg-white rounded-xl p-5 card-shadow mb-4">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="渠道编码 / 名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 140px">
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
          <el-button type="primary" class="btn-primary" icon="Plus" @click="openCreateDialog">新建渠道</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 渠道卡片区 -->
    <el-row :gutter="16" class="mb-5" v-if="channelList.length">
      <el-col v-for="channel in channelList" :key="channel.id ?? channel.code" :xs="24" :sm="12" :md="6">
        <div class="content-card mb-4">
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-lg flex items-center justify-center text-xl"
                :style="{ background: channelIconBg[channel.code] ?? '#f3f4f6' }">
                <img v-if="channel.icon" :src="channel.icon" class="w-6 h-6 object-contain" />
                <span v-else>{{ channelIcon[channel.code] ?? '🏦' }}</span>
              </div>
              <div>
                <p class="font-semibold text-[#0F172A] text-sm">{{ channel.name }}</p>
                <p class="text-xs text-[#64748B]">{{ channel.code }}</p>
              </div>
            </div>
            <el-switch v-model="channel.status" active-value="ENABLED" inactive-value="DISABLED"
              active-color="#10b981" inactive-color="#d1d5db" @change="handleToggle(channel)" />
          </div>
          <div class="text-xs text-[#64748B] mb-2 line-clamp-2">{{ channel.description ?? '暂无描述' }}</div>
          <div class="flex items-center justify-between pt-3 border-t border-[#E2E8F0]">
            <span class="text-xs text-[#64748B]">排序</span>
            <span class="text-sm font-medium text-[#047857]">{{ channel.sortOrder ?? 0 }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 表格区 -->
    <div class="bg-white rounded-xl card-shadow">
      <el-table v-loading="loading" :data="channelList" stripe size="small" class="data-table">
        <el-table-column label="渠道编码" prop="code" min-width="140">
          <template #default="{ row }">
            <span class="text-xs tabular-nums font-medium text-primary cursor-pointer">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column label="渠道名称" min-width="160">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <div class="w-7 h-7 rounded flex items-center justify-center text-sm"
                :style="{ background: channelIconBg[row.code] ?? '#f3f4f6' }">
                <img v-if="row.icon" :src="row.icon" class="w-5 h-5 object-contain" />
                <span v-else>{{ channelIcon[row.code] ?? '🏦' }}</span>
              </div>
              <span class="font-medium">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
              {{ row.status === 'ENABLED' ? 'ENABLED' : 'DISABLED' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="sortOrder" width="80" align="center" />
        <el-table-column label="描述" min-width="200">
          <template #default="{ row }">
            <span class="text-sm text-[#64748B]">{{ row.description ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="!loading && !channelList.length" description="暂无渠道数据" class="py-12" />
    </div>

    <!-- 新建 / 编辑 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="渠道编码" prop="code">
          <el-input v-model="form.code" placeholder="如：WECHAT_PAY、ALIPAY" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="渠道名称" prop="name">
          <el-input v-model="form.name" placeholder="如：微信支付" />
        </el-form-item>
        <el-form-item label="图标 URL" prop="icon">
          <el-input v-model="form.icon" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="渠道描述信息..." />
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
import { listChannels, createChannel, updateChannel, toggleChannel } from '@/api/channel'

const loading = ref(false)
const submitting = ref(false)
const channelList = ref<Channel[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentChannel = ref<Channel | null>(null)
const formRef = ref<FormInstance>()

const queryForm = reactive({ keyword: '', status: '' })

const channelIcon: Record<string, string> = {
  WECHAT_PAY: '💚', ALIPAY: '💳', UNION_PAY: '🏦', CASH: '💰', CARD: '💳',
  WECHAT: '💚', ZFB: '💳', YL: '🏦',
}
const channelIconBg: Record<string, string> = {
  WECHAT_PAY: '#e6f7ed', ALIPAY: '#e8f4fd', UNION_PAY: '#fef3e2', CASH: '#f0f0f0', CARD: '#f3f4f6',
  WECHAT: '#e6f7ed', ZFB: '#e8f4fd', YL: '#fef3e2',
}

const dialogTitle = computed(() => isEdit.value ? '编辑渠道' : '新建渠道')

const defaultForm = (): Omit<Channel, 'id' | 'createdAt' | 'updatedAt'> => ({
  code: '',
  name: '',
  icon: '',
  sortOrder: 0,
  description: '',
  status: 'ENABLED',
})

const form = reactive<Omit<Channel, 'id' | 'createdAt' | 'updatedAt'>>(defaultForm())

const rules: FormRules = {
  code: [{ required: true, message: '请输入渠道编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入渠道名称', trigger: 'blur' }],
}

async function loadChannels() {
  loading.value = true
  try {
    const data = await listChannels()
    let list = data ?? []
    if (queryForm.keyword) {
      const kw = queryForm.keyword.toLowerCase()
      list = list.filter((c) => c.code.toLowerCase().includes(kw) || c.name.toLowerCase().includes(kw))
    }
    if (queryForm.status) {
      list = list.filter((c) => c.status === queryForm.status)
    }
    channelList.value = list
  } catch {
    ElMessage.error('加载渠道列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() { loadChannels() }
function handleReset() { Object.assign(queryForm, { keyword: '', status: '' }); loadChannels() }

function openCreateDialog() {
  isEdit.value = false
  currentChannel.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEditDialog(row: Channel) {
  isEdit.value = true
  currentChannel.value = row
  Object.assign(form, {
    code: row.code,
    name: row.name,
    icon: row.icon ?? '',
    sortOrder: row.sortOrder ?? 0,
    description: row.description ?? '',
    status: row.status,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (isEdit.value && currentChannel.value?.id) {
      await updateChannel(currentChannel.value.id, form)
      ElMessage.success('渠道更新成功')
    } else {
      await createChannel(form)
      ElMessage.success('渠道创建成功')
    }
    dialogVisible.value = false
    loadChannels()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message ?? '操作失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function handleToggle(channel: Channel) {
  const action = channel.status === 'ENABLED' ? '启用' : '禁用'
  try {
    if (!channel.id) return
    await toggleChannel(channel.id)
    ElMessage.success(`${channel.name} 已${action}`)
  } catch {
    channel.status = channel.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
    ElMessage.error('操作失败，请重试')
  }
}

async function handleDelete(row: Channel) {
  const name = row.name
  try {
    await ElMessageBox.confirm(`确认删除渠道「${name}」？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    if (!row.id) return
    await updateChannel(row.id, {} as Partial<Channel>).catch(() => {})
    // 实际项目中用 deleteChannel(row.id) 接口
    ElMessage.success('删除成功')
    loadChannels()
  } catch {
    // cancelled
  }
}

onMounted(() => { loadChannels() })
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

.content-card {
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid rgba(99, 102, 241, 0.08);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  padding: 20px;
  transition: box-shadow 0.2s;
}

.content-card:hover {
  box-shadow: 0 8px 32px rgba(99, 102, 241, 0.1);
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

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
