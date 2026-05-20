<template>
  <div>
    <!-- 顶部筛选工具栏 -->
    <div class="filter-bar">
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
      <el-row :gutter="16" class="channel-card-row mb-1" v-if="channelList.length">
        <!-- 适中密度：平板两列、中屏三列、大屏四列，兼顾可读性与屏占比 -->
        <el-col
          v-for="channel in channelList"
          :key="channel.channelCode || channel.id"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="8"
          :xl="6"
        >
          <div class="content-card channel-card-shell mb-4">
            <!-- 上：图标 + 名称/编码（独占整行，不与按钮抢横向空间） -->
            <div class="channel-card-identity">
              <div
                class="channel-card-icon"
                :style="{ background: channelIconBg[channel.channelCode] ?? '#f3f4f6' }"
              >
                <img v-if="channel.icon" :src="channel.icon" class="channel-card-icon-img" alt="" />
                <span v-else class="channel-card-icon-emoji">{{ channelIcon[channel.channelCode] ?? '🏦' }}</span>
              </div>
              <div class="channel-card-meta">
                <p class="channel-card-title">{{ channel.channelName || '未命名渠道' }}</p>
                <p class="channel-card-code">{{ channel.channelCode || '—' }}</p>
              </div>
            </div>
            <!-- 下：操作栏单独一行 -->
            <div class="channel-card-toolbar">
              <div class="channel-card-toolbar-btns">
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
              </div>
              <div class="channel-card-toolbar-switch">
                <span class="channel-card-switch-label">启用</span>
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
            <div class="channel-card-desc">{{ channel.description ?? '暂无描述' }}</div>
            <button
              type="button"
              class="channel-card-methods"
              @click.stop="openMethodsDrawer(channel)"
            >
              <span class="channel-card-methods-label">支付方式</span>
              <el-tag size="small" type="success" effect="plain" round>
                {{ methodCounts[channel.id] ?? '—' }} 种
              </el-tag>
            </button>
            <div class="channel-card-footer">
              <span class="channel-card-footer-label">优先级</span>
              <span class="channel-card-footer-value">{{ channel.priority ?? 0 }}</span>
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

    <el-drawer
      v-model="methodsDrawerVisible"
      :title="drawerTitle"
      direction="rtl"
      size="min(480px, 92vw)"
      destroy-on-close
    >
      <div v-loading="drawerLoading" class="min-h-[120px]">
        <el-table v-if="drawerMethods.length" :data="drawerMethods" stripe size="small" class="data-table w-full">
          <el-table-column label="编号" prop="methodCode" min-width="120" show-overflow-tooltip />
          <el-table-column label="名称" prop="methodName" min-width="140" show-overflow-tooltip />
          <el-table-column label="状态" width="88" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else-if="!drawerLoading" description="该渠道下暂无支付方式" :image-size="72" />
        <div class="mt-5 flex flex-wrap gap-2">
          <el-button type="primary" class="btn-primary" size="small" @click="goPaymentMethodsManage">
            前往支付方式管理
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getChannels, createChannel, updateChannel, toggleChannel, deleteChannel, getPaymentMethodsByChannelId } from '@/api/admin'
import type { Channel } from '@/types'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const channelList = ref<Channel[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentChannel = ref<Channel | null>(null)
const formRef = ref<FormInstance>()

const methodCounts = ref<Record<number, number>>({})
const methodsDrawerVisible = ref(false)
const drawerChannel = ref<Channel | null>(null)
const drawerMethods = ref<Array<Record<string, unknown>>>([])
const drawerLoading = ref(false)

const drawerTitle = computed(() => {
  const c = drawerChannel.value
  if (!c) return '支付方式'
  return `支付方式 · ${c.channelName || c.channelCode || '渠道'}`
})

const queryForm = reactive({ keyword: '', enabled: null as boolean | null })

const channelIcon: Record<string, string> = {
  WECHAT_PAY: '💚',
  ALIPAY: '💳',
  UNION_PAY: '🏦',
  CASH: '💰',
  CARD: '💳',
  WECHAT: '💚',
  ZFB: '💳',
  YL: '🏦',
  wechat_pay: '💚',
  alipay: '💳',
  union_pay: '🏦',
  bank_card: '💳',
}
const channelIconBg: Record<string, string> = {
  WECHAT_PAY: '#e6f7ed',
  ALIPAY: '#e8f4fd',
  UNION_PAY: '#fef3e2',
  CASH: '#f0f0f0',
  CARD: '#f3f4f6',
  WECHAT: '#e6f7ed',
  ZFB: '#e8f4fd',
  YL: '#fef3e2',
  wechat_pay: '#e6f7ed',
  alipay: '#e8f4fd',
  union_pay: '#fef3e2',
  bank_card: '#f1f5f9',
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
    await loadMethodCounts()
  } catch {
    ElMessage.error('加载渠道列表失败')
  } finally {
    loading.value = false
  }
}

async function loadMethodCounts() {
  const map: Record<number, number> = {}
  await Promise.all(
    channelList.value.map(async (c) => {
      if (c.id == null) return
      try {
        const list = await getPaymentMethodsByChannelId(c.id)
        map[c.id] = Array.isArray(list) ? list.length : 0
      } catch {
        map[c.id] = 0
      }
    })
  )
  methodCounts.value = map
}

async function openMethodsDrawer(channel: Channel) {
  if (channel.id == null) return
  drawerChannel.value = channel
  methodsDrawerVisible.value = true
  drawerLoading.value = true
  drawerMethods.value = []
  try {
    const list = await getPaymentMethodsByChannelId(channel.id)
    drawerMethods.value = Array.isArray(list) ? (list as Array<Record<string, unknown>>) : []
  } catch {
    ElMessage.error('加载支付方式失败')
  } finally {
    drawerLoading.value = false
  }
}

function goPaymentMethodsManage() {
  const id = drawerChannel.value?.id
  methodsDrawerVisible.value = false
  if (id != null) {
    router.push({ path: '/admin/payment-methods', query: { channelId: String(id) } })
  } else {
    router.push({ path: '/admin/payment-methods' })
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
.channel-card-shell.content-card {
  padding: 18px 20px 16px;
  display: flex;
  flex-direction: column;
}

.card-action-btn {
  border-radius: 10px;
  padding: 6px 12px;
}

/* 主信息区：仅横向排列图标与文案，宽度占满 */
.channel-card-identity {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 2px;
}

.channel-card-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 20px;
}

.channel-card-icon-img {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.channel-card-icon-emoji {
  line-height: 1;
}

.channel-card-meta {
  flex: 1;
  min-width: 0;
}

.channel-card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.4;
  word-break: break-word;
}

.channel-card-code {
  margin: 4px 0 0;
  font-size: 12px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  word-break: break-all;
  line-height: 1.35;
}

/* 操作栏：独立一行，与标题彻底分离 */
.channel-card-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 0 12px;
  margin-bottom: 10px;
  border-bottom: 1px solid #e2e8f0;
}

.channel-card-toolbar-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.channel-card-toolbar-switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.channel-card-switch-label {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
}

.channel-card-desc {
  flex: 1;
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 10px;
}

.channel-card-methods {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin: 0 0 10px;
  padding: 8px 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  background: #f8fafc;
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition: background 0.15s, border-color 0.15s;
}

.channel-card-methods:hover {
  background: #ecfdf5;
  border-color: #6ee7b7;
}

.channel-card-methods-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.channel-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  margin-top: auto;
  border-top: 1px solid #e2e8f0;
}

.channel-card-footer-label {
  font-size: 12px;
  color: #64748b;
}

.channel-card-footer-value {
  font-size: 14px;
  font-weight: 600;
  color: #047857;
}
</style>
