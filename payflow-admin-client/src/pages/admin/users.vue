<template>
  <div>
    <!-- 顶部操作栏 -->
    <div class="bg-white rounded-xl p-5 card-shadow mb-4">
      <div class="flex items-center justify-between">
        <h3 class="text-base font-semibold text-gray-700 m-0">用户列表</h3>
        <el-button type="primary" class="btn-primary" icon="Plus" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <!-- 表格区 -->
    <div class="bg-white rounded-xl card-shadow">
      <el-table v-loading="loading" :data="userList" stripe size="small">
        <el-table-column label="用户名" prop="username" min-width="140">
          <template #default="{ row }">
            <span class="text-xs font-mono font-medium text-primary">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column label="昵称" prop="nickname" min-width="120">
          <template #default="{ row }">{{ row.nickname ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="手机" prop="mobile" min-width="130">
          <template #default="{ row }">{{ row.mobile ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="邮箱" prop="email" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="角色" prop="roleId" min-width="120" align="center">
          <template #default="{ row }">
            <span>{{ getRoleName(row.roleId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'danger'" effect="plain">
              {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="openResetPwd(row)">重置密码</el-button>
            <el-button link type="danger" size="small" @click="handleDisable(row)">
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑用户弹窗 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机" prop="mobile">
          <el-input v-model="form.mobile" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roleList" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="420px" destroy-on-close>
      <el-form ref="resetPwdRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="80px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetPwdForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" :loading="resetPwdLoading" @click="handleResetPwdSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getUsers, createUser, updateUser, resetUserPassword, disableUser, getRoles } from '@/api/admin'
import type { SysRole } from '@/types'

// ============================================================
// 类型定义
// ============================================================
interface SysUser {
  id: number
  username: string
  nickname?: string
  mobile?: string
  email?: string
  roleId: number
  status: 'ACTIVE' | 'DISABLED'
  createdAt: string
  updatedAt: string
}

// ============================================================
// 状态
// ============================================================
const loading = ref(false)
const userList = ref<SysUser[]>([])
const roleList = ref<SysRole[]>([])

// 表单弹窗
const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editId = ref<number | null>(null)

const form = reactive({
  username: '',
  nickname: '',
  mobile: '',
  email: '',
  roleId: '' as number | '',
  status: 'ACTIVE' as 'ACTIVE' | 'DISABLED',
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

// 重置密码弹窗
const resetPwdVisible = ref(false)
const resetPwdLoading = ref(false)
const resetPwdRef = ref<FormInstance>()
const resetPwdUserId = ref<number | null>(null)

const resetPwdForm = reactive({
  newPassword: '',
})

const resetPwdRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
}

// ============================================================
// 数据加载
// ============================================================
async function loadUsers() {
  loading.value = true
  try {
    const resp: any = await getUsers()
    userList.value = Array.isArray(resp) ? resp : resp?.list ?? []
  } catch {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const resp: any = await getRoles()
    roleList.value = Array.isArray(resp) ? resp : resp?.list ?? []
  } catch {
    ElMessage.error('加载角色列表失败')
  }
}

// ============================================================
// 工具方法
// ============================================================
function getRoleName(roleId: number): string {
  const role = roleList.value.find((r) => r.id === roleId)
  return role?.roleName ?? '—'
}

function resetForm() {
  Object.assign(form, {
    username: '',
    nickname: '',
    mobile: '',
    email: '',
    roleId: '',
    status: 'ACTIVE' as const,
  })
}

// ============================================================
// 事件处理
// ============================================================
function openCreate() {
  isEdit.value = false
  editId.value = null
  resetForm()
  formVisible.value = true
}

function openEdit(row: SysUser) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    username: row.username,
    nickname: row.nickname ?? '',
    mobile: row.mobile ?? '',
    email: row.email ?? '',
    roleId: row.roleId,
    status: row.status,
  })
  formVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const data: any = {
        nickname: form.nickname || undefined,
        mobile: form.mobile || undefined,
        email: form.email || undefined,
        roleId: form.roleId,
        status: form.status,
      }
      if (isEdit.value && editId.value !== null) {
        await updateUser(editId.value, data)
        ElMessage.success('用户已更新')
      } else {
        data.username = form.username
        await createUser(data)
        ElMessage.success('用户已创建')
      }
      formVisible.value = false
      loadUsers()
    } catch (e: any) {
      ElMessage.error(e?.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

async function handleDisable(row: SysUser) {
  const action = row.status === 'ACTIVE' ? '禁用' : '启用'
  const actionApi = row.status === 'ACTIVE' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确认${action}用户「${row.username}」吗？`,
      `${action}确认`,
      { confirmButtonText: action, cancelButtonText: '取消', type: 'warning' }
    )
    await disableUser(row.id)
    ElMessage.success(`用户已${actionApi}`)
    loadUsers()
  } catch (e: any) {
    if (e === 'cancel' || e?.toString?.().includes('cancel')) return
    ElMessage.error(e?.message || `${action}用户失败`)
  }
}

function openResetPwd(row: SysUser) {
  resetPwdUserId.value = row.id
  resetPwdForm.newPassword = ''
  resetPwdVisible.value = true
}

async function handleResetPwdSubmit() {
  if (!resetPwdRef.value) return
  await resetPwdRef.value.validate(async (valid) => {
    if (!valid) return
    if (resetPwdUserId.value === null) return
    resetPwdLoading.value = true
    try {
      await resetUserPassword(resetPwdUserId.value, resetPwdForm.newPassword)
      ElMessage.success('密码已重置')
      resetPwdVisible.value = false
    } catch (e: any) {
      ElMessage.error(e?.message || '重置密码失败')
    } finally {
      resetPwdLoading.value = false
    }
  })
}

// ============================================================
// 初始化
// ============================================================
onMounted(() => {
  loadUsers()
  loadRoles()
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
