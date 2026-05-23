<template>
  <div class="page-table-shell">
    <div class="content-card">
      <TableToolbar title="用户列表" :total="userList.length">
        <template #actions>
          <el-button type="primary" class="btn-primary" icon="Plus" @click="openCreate">新增用户</el-button>
        </template>
      </TableToolbar>

      <el-table table-layout="auto" v-loading="loading" :data="userList" stripe size="small" class="data-table">
        <el-table-column label="用户名" prop="username" min-width="140">
          <template #default="{ row }">
            <span class="cell-mono font-medium text-[#047857]">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column label="昵称" prop="nickname" min-width="120">
          <template #default="{ row }">{{ row.nickname ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="手机" prop="phone" min-width="130">
          <template #default="{ row }">{{ row.phone ?? row.mobile ?? '—' }}</template>
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
            <el-tag size="small" :type="tagTypeOf(ENABLE_STATUS_TAG, row.status)" effect="plain">
              {{ labelOf(ENABLE_STATUS_LABEL, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" min-width="168" class-name="col-datetime" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-xs text-slate-600 tabular-nums">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="280" class-name="col-actions" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openUserDetail(row)">详情</el-button>
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
        <el-form-item v-if="!isEdit" label="初始密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="至少 6 位"
            show-password
            autocomplete="new-password"
          />
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

    <el-drawer v-model="detailVisible" title="用户详情" direction="rtl" size="420px">
      <div v-if="detailLoading" class="p-4"><el-skeleton animated :rows="6" /></div>
      <div v-else-if="detailUser" class="px-2 space-y-4 text-sm">
        <dl class="grid grid-cols-[100px_1fr] gap-y-2 gap-x-2">
          <dt class="text-gray-400">用户 ID</dt><dd class="text-gray-800 tabular-nums">{{ detailUser.id }}</dd>
          <dt class="text-gray-400">用户名</dt><dd class="font-mono">{{ detailUser.username }}</dd>
          <dt class="text-gray-400">昵称</dt><dd>{{ detailUser.nickname ?? '—' }}</dd>
          <dt class="text-gray-400">手机</dt><dd>{{ detailUser.phone ?? detailUser.mobile ?? '—' }}</dd>
          <dt class="text-gray-400">邮箱</dt><dd class="break-all">{{ detailUser.email ?? '—' }}</dd>
          <dt class="text-gray-400">角色</dt><dd>{{ getRoleName(detailUser.roleId) }}</dd>
          <dt class="text-gray-400">状态</dt>
          <dd>
            <el-tag size="small" :type="tagTypeOf(ENABLE_STATUS_TAG, detailUser.status)">
              {{ labelOf(ENABLE_STATUS_LABEL, detailUser.status) }}
            </el-tag>
          </dd>
          <dt class="text-gray-400">创建时间</dt><dd>{{ formatDateTime(detailUser.createdAt) }}</dd>
          <dt class="text-gray-400">更新时间</dt><dd>{{ formatDateTime(detailUser.updatedAt) }}</dd>
        </dl>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getUsers, createUser, updateUser, resetUserPassword, disableUser, getRoles, getUserById } from '@/api/admin'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import {
  ENABLE_STATUS_LABEL,
  ENABLE_STATUS_TAG,
  formatDateTime,
  labelOf,
  tagTypeOf,
} from '@/utils/format'
import type { SysRole } from '@/types'

interface SysUser {
  id: number
  username: string
  nickname?: string
  phone?: string
  mobile?: string
  email?: string
  roleId?: number | null
  status: 'ACTIVE' | 'DISABLED'
  createdAt: string
  updatedAt: string
}

const loading = ref(false)
const userList = ref<SysUser[]>([])
const roleList = ref<SysRole[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailUser = ref<SysUser | null>(null)

const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editId = ref<number | null>(null)

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  mobile: '',
  email: '',
  roleId: '' as number | '',
  status: 'ACTIVE' as 'ACTIVE' | 'DISABLED',
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    {
      validator: (_rule, value, callback) => {
        if (isEdit.value) {
          callback()
          return
        }
        if (!value || String(value).length < 6) {
          callback(new Error('请输入至少 6 位初始密码'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

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

function getRoleName(roleId?: number | null): string {
  if (roleId == null) return '—'
  const role = roleList.value.find((r) => r.id === roleId)
  return role?.roleName ?? '—'
}

function normalizeRoleId(roleId: unknown): number | '' {
  if (roleId === null || roleId === undefined || roleId === '') return ''
  const n = Number(roleId)
  return Number.isFinite(n) ? n : ''
}

function fillFormFromUser(user: Partial<SysUser>) {
  Object.assign(form, {
    username: user.username ?? '',
    password: '',
    nickname: user.nickname ?? '',
    mobile: user.phone ?? user.mobile ?? '',
    email: user.email ?? '',
    roleId: normalizeRoleId(user.roleId),
    status: (user.status ?? 'ACTIVE') as 'ACTIVE' | 'DISABLED',
  })
}

async function openUserDetail(row: SysUser) {
  detailVisible.value = true
  detailLoading.value = true
  detailUser.value = null
  try {
    const full = await getUserById(row.id)
    detailUser.value = { ...row, ...full, roleId: full.roleId ?? row.roleId ?? null }
  } catch {
    ElMessage.error('加载用户详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    username: '',
    password: '',
    nickname: '',
    mobile: '',
    email: '',
    roleId: '',
    status: 'ACTIVE' as const,
  })
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  resetForm()
  formVisible.value = true
}

function openEdit(row: SysUser) {
  isEdit.value = true
  editId.value = row.id
  formVisible.value = true
  void (async () => {
    try {
      const full = await getUserById(row.id)
      fillFormFromUser({ ...row, ...full })
    } catch {
      ElMessage.error('加载用户信息失败')
      formVisible.value = false
    }
  })()
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const roleId = normalizeRoleId(form.roleId)
      if (roleId === '') {
        ElMessage.warning('请选择角色')
        submitting.value = false
        return
      }
      const data: Record<string, unknown> = {
        nickname: form.nickname || undefined,
        phone: form.mobile || undefined,
        email: form.email || undefined,
        roleId,
        status: form.status,
      }
      if (isEdit.value && editId.value !== null) {
        await updateUser(editId.value, data)
        ElMessage.success('用户已更新')
      } else {
        data.username = form.username
        data.password = form.password
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

onMounted(() => {
  loadUsers()
  loadRoles()
})
</script>
