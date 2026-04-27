<template>
  <div>
    <!-- 顶部操作栏 -->
    <div class="bg-white rounded-xl p-5 card-shadow mb-4">
      <div class="flex items-center justify-between">
        <h3 class="text-base font-semibold text-gray-700 m-0">角色列表</h3>
        <el-button type="primary" class="btn-primary" icon="Plus" @click="openCreate">新增角色</el-button>
      </div>
    </div>

    <!-- 表格区 -->
    <div class="bg-white rounded-xl card-shadow">
      <el-table v-loading="loading" :data="roleList" stripe size="small">
        <el-table-column label="角色编码" prop="roleCode" min-width="140">
          <template #default="{ row }">
            <span class="text-xs font-mono font-medium text-primary">{{ row.roleCode }}</span>
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
            <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'danger'" effect="plain">
              {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" size="small" @click="openPermission(row)">分配权限</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 分配权限弹窗 -->
    <el-dialog v-model="permVisible" :title="`分配权限 - ${currentRole?.roleName || ''}`" width="480px" destroy-on-close>
      <div v-if="permLoading" class="p-4"><el-skeleton animated :rows="5" /></div>
      <div v-else>
        <el-tree
          ref="menuTreeRef"
          :data="menuTree"
          :props="{ label: 'menuName', children: 'children' }"
          node-key="id"
          show-checkbox
          default-expand-all
          :default-checked-keys="checkedMenuIds"
        />
      </div>
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" :loading="permSaving" @click="handleSavePermission">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import type { ElTree } from 'element-plus'
import { getRoles, createRole, updateRole, deleteRole, getRoleMenus, assignRoleMenus, getMenuTree } from '@/api/admin'
import type { SysRole, SysMenu } from '@/types'

const loading = ref(false)
const roleList = ref<SysRole[]>([])

// 表单
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

// 权限分配
const permVisible = ref(false)
const permLoading = ref(false)
const permSaving = ref(false)
const currentRole = ref<SysRole | null>(null)
const menuTree = ref<SysMenu[]>([])
const checkedMenuIds = ref<number[]>([])
const menuTreeRef = ref<InstanceType<typeof ElTree>>()

async function loadRoles() {
  loading.value = true
  try {
    const resp = await getRoles()
    roleList.value = Array.isArray(resp) ? resp : (resp as any)?.list ?? []
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
    } catch (e: any) {
      ElMessage.error(e?.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row: SysRole) {
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.roleName}」吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteRole(row.id)
    ElMessage.success('角色已删除')
    loadRoles()
  } catch (e: any) {
    if (e === 'cancel' || e?.toString?.().includes('cancel')) return
    ElMessage.error(e?.message || '删除角色失败')
  }
}

// ========== 分配权限 ==========
function collectLeafIds(menus: SysMenu[]): number[] {
  const ids: number[] = []
  for (const m of menus) {
    if (m.children && m.children.length > 0) {
      ids.push(...collectLeafIds(m.children))
    } else {
      ids.push(m.id)
    }
  }
  return ids
}

async function openPermission(role: SysRole) {
  currentRole.value = role
  permVisible.value = true
  permLoading.value = true
  try {
    const [tree, roleMenuList] = await Promise.all([getMenuTree(), getRoleMenus(role.id)])
    menuTree.value = Array.isArray(tree) ? tree : []
    const menus = Array.isArray(roleMenuList) ? roleMenuList : []
    // el-tree 的 default-checked-keys 只应包含叶子节点
    checkedMenuIds.value = collectLeafIds(menus)
  } catch {
    ElMessage.error('加载菜单数据失败')
  } finally {
    permLoading.value = false
  }
}

async function handleSavePermission() {
  if (!currentRole.value || !menuTreeRef.value) return
  permSaving.value = true
  try {
    const checkedKeys = menuTreeRef.value.getCheckedKeys(false) as number[]
    const halfCheckedKeys = menuTreeRef.value.getHalfCheckedKeys() as number[]
    const allKeys = [...checkedKeys, ...halfCheckedKeys]
    await assignRoleMenus(currentRole.value.id, allKeys)
    ElMessage.success('权限已保存')
    permVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '保存权限失败')
  } finally {
    permSaving.value = false
  }
}

onMounted(() => {
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
