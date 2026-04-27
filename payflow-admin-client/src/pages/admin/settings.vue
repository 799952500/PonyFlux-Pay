<template>
  <div class="p-6">
    <!-- 页面标题 + 刷新缓存区 -->
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-lg font-semibold text-gray-700">系统配置</h2>
      <div class="flex items-center gap-2">
        <!-- 按分类刷新 -->
        <el-select v-model="refreshCategory" placeholder="选择分类" size="default" style="width: 140px">
          <el-option label="payment" value="payment" />
          <el-option label="risk" value="risk" />
          <el-option label="fee" value="fee" />
          <el-option label="system" value="system" />
        </el-select>
        <el-button size="default" @click="refreshByCategory" :loading="refreshLoading">
          刷新分类缓存
        </el-button>
        <!-- 按 key 刷新 -->
        <el-input v-model="refreshKey" placeholder="配置 key" size="default" style="width: 160px" />
        <el-button size="default" @click="refreshByKey" :loading="refreshLoading">刷新 Key</el-button>
        <!-- 全量刷新 -->
        <el-button size="default" type="warning" @click="refreshAll" :loading="refreshLoading">
          刷新全部缓存
        </el-button>
        <!-- 新增 -->
        <el-button size="default" type="primary" @click="openDialog()">新增配置</el-button>
      </div>
    </div>

    <!-- 分类标签栏 -->
    <div class="flex gap-2 mb-4">
      <el-tag
        v-for="cat in categoryOptions"
        :key="cat.value"
        :type="activeCategory === cat.value ? 'primary' : 'info'"
        class="cursor-pointer text-sm px-3 py-1"
        @click="activeCategory = cat.value; loadData()"
      >{{ cat.label }}</el-tag>
    </div>

    <!-- 配置表格 -->
    <el-table :data="tableData" stripe class="w-full" row-key="id">
      <el-table-column prop="configKey" label="配置键" min-width="180" />
      <el-table-column prop="configValue" label="配置值" min-width="200">
        <template #default="{ row }">
          <span v-if="row.valueType === 'BOOLEAN'">
            <el-tag :type="row.configValue === 'true' ? 'success' : 'danger'" size="small">
              {{ row.configValue }}
            </el-tag>
          </span>
          <span v-else-if="row.valueType === 'NUMBER'" class="font-mono text-blue-600">
            {{ row.configValue }}
          </span>
          <span v-else class="font-mono text-gray-600">{{ row.configValue }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="valueType" label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.valueType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ row.category }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            active-text="启用"
            inactive-text="禁用"
            inline-prompt
            @change="toggleStatus(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <!-- 新增/编辑对话框 -->
  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑配置' : '新增配置'" width="500px">
    <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
      <el-form-item label="配置键" prop="configKey">
        <el-input v-model="form.configKey" :disabled="isEdit" placeholder="如：max_refund_rate" />
      </el-form-item>
      <el-form-item label="配置值" prop="configValue">
        <el-input v-model="form.configValue" placeholder="配置值" />
      </el-form-item>
      <el-form-item label="类型" prop="valueType">
        <el-select v-model="form.valueType" placeholder="选择类型" style="width: 100%">
          <el-option label="STRING" value="STRING" />
          <el-option label="NUMBER" value="NUMBER" />
          <el-option label="BOOLEAN" value="BOOLEAN" />
          <el-option label="JSON" value="JSON" />
        </el-select>
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
          <el-option label="payment" value="payment" />
          <el-option label="risk" value="risk" />
          <el-option label="fee" value="fee" />
          <el-option label="system" value="system" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="2" placeholder="配置说明" />
      </el-form-item>
      <el-form-item label="排序" prop="sortOrder">
        <el-input-number v-model="form.sortOrder" :min="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import type { FormInstance, FormRules } from 'element-plus'

interface SystemConfig {
  id?: number
  configKey: string
  configValue: string
  valueType: string
  category: string
  description: string
  sortOrder: number
  status: number
  createdAt?: string
  updatedAt?: string
}

const activeCategory = ref('')
const tableData = ref<SystemConfig[]>([])
const refreshCategory = ref('')
const refreshKey = ref('')
const refreshLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const categoryOptions = [
  { label: '全部', value: '' },
  { label: 'payment', value: 'payment' },
  { label: 'risk', value: 'risk' },
  { label: 'fee', value: 'fee' },
  { label: 'system', value: 'system' },
]

const defaultForm: SystemConfig = {
  configKey: '',
  configValue: '',
  valueType: 'STRING',
  category: 'system',
  description: '',
  sortOrder: 0,
  status: 1,
}

const form = ref<SystemConfig>({ ...defaultForm })

const formRules: FormRules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
  valueType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

async function loadData() {
  const params: any = {}
  if (activeCategory.value) params.category = activeCategory.value
  const data = await request.get('/admin/system-configs', { params })
  tableData.value = Array.isArray(data) ? data : []
}

async function refreshByCategory() {
  if (!refreshCategory.value) {
    ElMessage.warning('请先选择要刷新的分类')
    return
  }
  refreshLoading.value = true
  try {
    await request.post(`/admin/system-configs/cache/refresh/category/${refreshCategory.value}`)
    ElMessage.success(`${refreshCategory.value} 分类缓存已刷新`)
  } catch {
    ElMessage.error('刷新失败')
  } finally {
    refreshLoading.value = false
  }
}

async function refreshByKey() {
  if (!refreshKey.value.trim()) {
    ElMessage.warning('请输入要刷新的 key')
    return
  }
  refreshLoading.value = true
  try {
    await request.post(`/admin/system-configs/cache/refresh/${refreshKey.value.trim()}`)
    ElMessage.success(`key [${refreshKey.value}] 缓存已刷新`)
  } catch {
    ElMessage.error('刷新失败')
  } finally {
    refreshLoading.value = false
  }
}

async function refreshAll() {
  refreshLoading.value = true
  try {
    await request.post('/admin/system-configs/cache/refresh/all')
    ElMessage.success('全量缓存已刷新')
  } catch {
    ElMessage.error('刷新失败')
  } finally {
    refreshLoading.value = false
  }
}

function openDialog(row?: SystemConfig) {
  if (row) {
    isEdit.value = true
    form.value = { ...row }
  } else {
    isEdit.value = false
    form.value = { ...defaultForm }
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await request.put(`/admin/system-configs/${form.value.id}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/admin/system-configs', form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function toggleStatus(row: SystemConfig) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await request.put(`/admin/system-configs/${row.id}`, { ...row, status: newStatus })
    row.status = newStatus
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
  } catch {
    ElMessage.error('状态更新失败')
  }
}

async function handleDelete(row: SystemConfig) {
  await ElMessageBox.confirm(`确认删除配置「${row.configKey}」？`, '删除确认', { type: 'warning' })
  try {
    await request.delete(`/admin/system-configs/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadData()
})
</script>
