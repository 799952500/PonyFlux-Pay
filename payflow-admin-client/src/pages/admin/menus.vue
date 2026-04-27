<template>
  <div>
    <div class="bg-white rounded-xl card-shadow">
      <div class="flex items-center justify-between px-5 pt-4 pb-2">
        <span class="text-base font-semibold text-gray-700">菜单管理</span>
        <el-button type="primary" class="btn-primary" icon="Plus" @click="openAdd()">新增菜单</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="menuTreeData"
        row-key="id"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        stripe
        size="small"
        default-expand-all
      >
        <el-table-column label="菜单名称" prop="menuName" min-width="180">
          <template #default="{ row }"><span class="font-medium">{{ row.menuName }}</span></template>
        </el-table-column>
        <el-table-column label="编码" prop="menuCode" min-width="140">
          <template #default="{ row }"><span class="text-xs tabular-nums font-medium text-primary">{{ row.menuCode }}</span></template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.menuType === 'MENU' ? 'primary' : 'info'" effect="plain">
              {{ row.menuType === 'MENU' ? '菜单' : '按钮' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="路由路径" prop="path" min-width="160">
          <template #default="{ row }"><span class="text-sm text-gray-500">{{ row.path ?? '—' }}</span></template>
        </el-table-column>
        <el-table-column label="图标" prop="icon" width="90" align="center">
          <template #default="{ row }"><span>{{ row.icon ?? '—' }}</span></template>
        </el-table-column>
        <el-table-column label="排序" prop="sortOrder" width="80" align="center" />
        <el-table-column label="可见" width="90" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.visible" @change="handleToggleVisible(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openAdd(row.id)">添加子菜单</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" :disabled="!!row.children?.length" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editTitle" width="600px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="父菜单">
          <el-cascader
            v-model="editForm.parentIdPath"
            :options="cascaderOptions"
            :props="{ checkStrictly: true, value: 'id', label: 'menuName', children: 'children', emitPath: false }"
            placeholder="无（顶级菜单）"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单编码" prop="menuCode">
          <el-input v-model="editForm.menuCode" placeholder="请输入菜单编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="editForm.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="类型" prop="menuType">
          <el-select v-model="editForm.menuType" style="width: 100%">
            <el-option label="菜单" value="MENU" />
            <el-option label="按钮" value="BUTTON" />
          </el-select>
        </el-form-item>
        <el-form-item label="路由路径" v-if="editForm.menuType === 'MENU'">
          <el-input v-model="editForm.path" placeholder="如 /admin/dashboard" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="editForm.icon" placeholder="图标名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="editForm.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="可见">
          <el-switch v-model="editForm.visible" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" :loading="editSubmitting" @click="handleEditSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/admin'
import type { SysMenu } from '@/types'

const loading = ref(false)
const menuTreeData = ref<SysMenu[]>([])
const editVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref<FormInstance>()
const isEdit = ref(false)

const editTitle = computed(() => isEdit.value ? '编辑菜单' : '新增菜单')

const editForm = reactive({
  id: 0,
  parentIdPath: undefined as number | undefined,
  menuCode: '',
  menuName: '',
  menuType: 'MENU' as 'MENU' | 'BUTTON',
  path: '',
  icon: '',
  sortOrder: 0,
  visible: true,
})

const editRules: FormRules = {
  menuCode: [{ required: true, message: '请输入菜单编码', trigger: 'blur' }],
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

// 级联选择器数据源（复用菜单树）
const cascaderOptions = computed(() => menuTreeData.value)

async function loadMenuTree() {
  loading.value = true
  try {
    const resp = await getMenuTree()
    menuTreeData.value = Array.isArray(resp) ? resp : []
  } catch {
    ElMessage.error('加载菜单树失败')
  } finally {
    loading.value = false
  }
}

function openAdd(parentId?: number) {
  isEdit.value = false
  Object.assign(editForm, {
    id: 0,
    parentIdPath: parentId ?? undefined,
    menuCode: '',
    menuName: '',
    menuType: 'MENU',
    path: '',
    icon: '',
    sortOrder: 0,
    visible: true,
  })
  editVisible.value = true
}

function openEdit(row: SysMenu) {
  isEdit.value = true
  Object.assign(editForm, {
    id: row.id,
    parentIdPath: row.parentId ?? undefined,
    menuCode: row.menuCode,
    menuName: row.menuName,
    menuType: row.menuType,
    path: row.path ?? '',
    icon: row.icon ?? '',
    sortOrder: row.sortOrder,
    visible: row.visible,
  })
  editVisible.value = true
}

async function handleEditSubmit() {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editSubmitting.value = true
    try {
      const payload = {
        parentId: editForm.parentIdPath || undefined,
        menuCode: editForm.menuCode,
        menuName: editForm.menuName,
        menuType: editForm.menuType,
        path: editForm.path || undefined,
        icon: editForm.icon || undefined,
        sortOrder: editForm.sortOrder,
        visible: editForm.visible,
      }
      if (isEdit.value) {
        await updateMenu(editForm.id, payload)
        ElMessage.success('菜单已更新')
      } else {
        await createMenu(payload)
        ElMessage.success('菜单已创建')
      }
      editVisible.value = false
      loadMenuTree()
    } catch (e: any) {
      const msg = e?.response?.data?.message || (isEdit.value ? '更新失败' : '创建失败')
      ElMessage.error(msg)
    } finally {
      editSubmitting.value = false
    }
  })
}

async function handleToggleVisible(row: SysMenu) {
  try {
    await updateMenu(row.id, { visible: row.visible })
    ElMessage.success(row.visible ? '已显示' : '已隐藏')
  } catch {
    row.visible = !row.visible
    ElMessage.error('更新失败')
  }
}

async function handleDelete(row: SysMenu) {
  if (row.children?.length) {
    ElMessage.warning('该菜单存在子菜单，无法删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除菜单「${row.menuName}」？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteMenu(row.id)
    ElMessage.success('已删除')
    loadMenuTree()
  } catch (e: any) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      const msg = e?.response?.data?.message || '删除失败'
      ElMessage.error(msg)
    }
  }
}

onMounted(() => { loadMenuTree() })
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
