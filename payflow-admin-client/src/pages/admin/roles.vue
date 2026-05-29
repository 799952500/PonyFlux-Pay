<template>
  <div class="page-table-shell">
    <div class="content-card">
      <TableToolbar title="角色列表" :total="roleList.length">
        <template #actions>
          <el-button v-permission="'role:create'" type="primary" class="btn-primary" icon="Plus" @click="openCreate">新增角色</el-button>
        </template>
      </TableToolbar>

      <el-table table-layout="auto" v-loading="loading" :data="roleList" stripe size="small" class="data-table">
        <el-table-column label="角色编码" prop="roleCode" min-width="140">
          <template #default="{ row }">
            <span class="cell-mono pf-link">{{ row.roleCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="角色名称" prop="roleName" min-width="160">
          <template #default="{ row }">
            <span class="font-medium">{{ row.roleName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="描述" prop="description" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description ?? '—' }}</template>
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
            <span class="cell-datetime">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220" class-name="col-actions" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'role:edit'" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'role:assign_menu'" link type="success" size="small" @click="openPermission(row)">分配权限</el-button>
            <el-button v-permission="'role:delete'" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="请输入角色编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="角色描述（可选）" />
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

    <RolePermissionDrawer
      v-model:visible="permVisible"
      :role="currentRole"
      @saved="loadRoles"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getRoles, createRole, updateRole, deleteRole } from '@/api/admin'
import { confirmDeleteWithGuard } from '@/composables/useResourceDeleteGuard'
import TableToolbar from '@/components/admin/TableToolbar.vue'
import RolePermissionDrawer from '@/components/admin/RolePermissionDrawer.vue'
import {
  ENABLE_STATUS_LABEL,
  ENABLE_STATUS_TAG,
  formatDateTime,
  labelOf,
  tagTypeOf,
} from '@/utils/format'
import type { SysRole } from '@/types'

const loading = ref(false)
const roleList = ref<SysRole[]>([])

const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editId = ref<number | null>(null)

const form = reactive({
  roleCode: '',
  roleName: '',
  description: '',
  status: 'ACTIVE' as 'ACTIVE' | 'DISABLED',
})

const formRules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const permVisible = ref(false)
const currentRole = ref<SysRole | null>(null)

async function loadRoles() {
  loading.value = true
  try {
    const resp = await getRoles()
    roleList.value = Array.isArray(resp) ? resp : (resp as { list?: SysRole[] })?.list ?? []
  } catch {
    ElMessage.error('加载角色列表失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, { roleCode: '', roleName: '', description: '', status: 'ACTIVE' as const })
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  resetForm()
  formVisible.value = true
}

function openEdit(row: SysRole) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    roleCode: row.roleCode,
    roleName: row.roleName,
    description: row.description ?? '',
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
      const data: Partial<SysRole> = {
        roleCode: form.roleCode,
        roleName: form.roleName,
        description: form.description || undefined,
        status: form.status,
      }
      if (isEdit.value && editId.value !== null) {
        await updateRole(editId.value, data)
        ElMessage.success('角色已更新')
      } else {
        await createRole(data)
        ElMessage.success('角色已创建')
      }
      formVisible.value = false
      loadRoles()
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : '操作失败'
      ElMessage.error(msg)
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row: SysRole) {
  await confirmDeleteWithGuard({
    resourceType: 'SYS_ROLE',
    resourceId: row.id,
    displayName: row.roleName,
    deleteFn: () => deleteRole(row.id),
    onSuccess: loadRoles,
  })
}

function openPermission(role: SysRole) {
  currentRole.value = role
  permVisible.value = true
}

onMounted(() => {
  loadRoles()
})
</script>
