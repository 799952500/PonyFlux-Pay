<template>
  <div>
    <!-- 顶部筛选工具栏 -->
    <div class="bg-white rounded-xl p-5 card-shadow mb-4">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="渠道编码 / 名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.enabled" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="启用" :value="true" />
            <el-option label="禁用" :value="false" />
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
    <div v-loading="loading">
      <el-row :gutter="16" class="mb-1" v-if="channelList.length">
        <el-col v-for="channel in channelList" :key="channel.channelCode || channel.id" :xs="24" :sm="12" :md="6">
          <div class="content-card mb-4">
            <div class="flex items-center justify-between mb-3">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-lg flex items-center justify-center text-xl"
                :style="{ background: channelIconBg[channel.channelCode] ?? '#f3f4f6' }">
                <img v-if="channel.icon" :src="channel.icon" class="w-6 h-6 object-contain" />
                <span v-else>{{ channelIcon[channel.channelCode] ?? '🏦' }}</span>
              </div>
              <div>
                <p class="font-semibold text-[#0F172A] text-sm">{{ channel.channelName || '未命名渠道' }}</p>
                <p class="text-xs text-[#64748B]">{{ channel.channelCode || '—' }}</p>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <el-button
                class="card-action-btn"
                type="primary"
                plain
                size="small"
                @click.stop="openEditDialog(channel)"
              >
                编辑
              </el-button>
              <el-button
                class="card-action-btn"
                type="danger"
                plain
                size="small"
                @click.stop="handleDelete(channel)"
              >
                删除
              </el-button>
              <el-switch
                v-model="channel.enabled"
                :active-value="true"
                :inactive-value="false"
                active-color="#10b981"
                inactive-color="#d1d5db"
                @change="handleToggle(channel)"
              />
            </div>
          </div>
          <div class="text-xs text-[#64748B] mb-2 line-clamp-2">{{ channel.description ?? '暂无描述' }}</div>
          <div class="flex items-center justify-between pt-3 border-t border-[#E2E8F0]">
            <span class="text-xs text-[#64748B]">优先级</span>
            <span class="text-sm font-medium text-[#047857]">{{ channel.priority ?? 0 }}</span>
          </div>
          </div>
        </el-col>
      </el-row>

      <!-- 空状态 -->
      <el-empty v-if="!loading && !channelList.length" description="暂无渠道数据" class="py-12" />
    </div>

    <!-- 新建 / 编辑 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="渠道编码" prop="channelCode">
          <el-input v-model="form.channelCode" placeholder="如：WECHAT、ALIPAY" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="渠道名称" prop="channelName">
          <el-input v-model="form.channelName" placeholder="如：微信支付" />
        </el-form-item>
        <el-form-item label="渠道类型" prop="channelType">
          <el-select v-model="form.channelType" placeholder="请选择" style="width: 100%">
            <el-option label="微信支付" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银联" value="UNION" />
            <el-option label="银行卡" value="CARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="API 地址" prop="apiUrl">
          <el-input v-model="form.apiUrl" placeholder="https://api.example.com/pay" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" placeholder="请输入 API Key" show-password />
        </el-form-item>
        <el-form-item label="图标 URL" prop="icon">
          <el-input v-model="form.icon" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" :min="0" :max="9999" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="启用状态" prop="enabled">
          <el-switch v-model="form.enabled" :active-value="true" :inactive-value="false" active-color="#10b981" inactive-color="#d1d5db" />
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
import { getChannels, createChannel, updateChannel, toggleChannel, deleteChannel } from '@/api/admin'
import type { Channel } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const channelList = ref<Channel[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentChannel = ref<Channel | null>(null)
const formRef = ref<FormInstance>()

const queryForm = reactive({ keyword: '', enabled: null as boolean | null })

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
  channelCode: '',
  channelName: '',
  channelType: '',
  apiUrl: '',
  apiKey: '',
  enabled: true,
  priority: 0,
  icon: '',
  description: '',
})

const form = reactive<Omit<Channel, 'id' | 'createdAt' | 'updatedAt'>>(defaultForm())

const rules: FormRules = {
  channelCode: [{ required: true, message: '请输入渠道编码', trigger: 'blur' }],
  channelName: [{ required: true, message: '请输入渠道名称', trigger: 'blur' }],
}

async function loadChannels() {
  loading.value = true
  try {
    const data = await getChannels()
    let list: Channel[] = data ?? []
    if (queryForm.keyword) {
      const kw = queryForm.keyword.toLowerCase()
      list = list.filter((c) =>
        c.channelCode.toLowerCase().includes(kw) || c.channelName.toLowerCase().includes(kw)
      )
    }
    if (queryForm.enabled !== null) {
      list = list.filter((c) => c.enabled === queryForm.enabled)
    }
    channelList.value = list
  } catch {
    ElMessage.error('加载渠道列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() { loadChannels() }
function handleReset() {
  queryForm.keyword = ''
  queryForm.enabled = null
  loadChannels()
}

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
    channelCode: row.channelCode,
    channelName: row.channelName,
    channelType: row.channelType ?? '',
    apiUrl: row.apiUrl ?? '',
    apiKey: row.apiKey ?? '',
    enabled: row.enabled,
    priority: row.priority ?? 0,
    icon: row.icon ?? '',
    description: row.description ?? '',
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
  try {
    await toggleChannel(channel.id)
    ElMessage.success(`已${channel.enabled ? '启用' : '禁用'} ${channel.channelName}`)
  } catch {
    channel.enabled = !channel.enabled
    ElMessage.error('操作失败')
  }
}

async function handleDelete(row: Channel) {
  const name = row.channelName
  try {
    await ElMessageBox.confirm(`确认删除渠道「${name}」？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteChannel(row.id)
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

.card-action-btn {
  border-radius: 10px;
  padding: 6px 10px;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
