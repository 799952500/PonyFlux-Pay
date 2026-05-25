<template>
  <el-drawer
    :model-value="visible"
    :title="`分配权限 - ${role?.roleName ?? ''}`"
    size="880px"
    destroy-on-close
    @close="emit('update:visible', false)"
  >
    <div v-if="loading" class="p-4">
      <el-skeleton animated :rows="8" />
    </div>
    <div v-else class="perm-drawer">
      <div class="perm-toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索菜单名 / 权限码"
          clearable
          prefix-icon="Search"
          class="perm-search"
        />
        <el-switch v-model="showCheckedOnly" active-text="仅已勾选" />
        <el-button size="small" @click="selectAllCurrent">全选当前</el-button>
        <el-button size="small" @click="invertCurrent">反选当前</el-button>
        <el-button size="small" @click="resetSelection">重置</el-button>
      </div>

      <div class="perm-body">
        <aside class="perm-nav">
          <el-tree
            ref="menuTreeRef"
            :data="menuOnlyTree"
            node-key="id"
            :props="{ label: 'menuName', children: 'children' }"
            highlight-current
            :expand-on-click-node="false"
            :filter-node-method="filterMenuNode"
            default-expand-all
            @node-click="onMenuNodeClick"
          >
            <template #default="{ data }">
              <div class="perm-nav-node">
                <el-checkbox
                  :model-value="checkedMenuIds.has(data.id)"
                  @change="(v: boolean) => toggleMenu(data, v)"
                  @click.stop
                />
                <span class="perm-nav-label">{{ data.menuName }}</span>
                <el-tag v-if="buttonTotal(data) > 0" size="small" :type="badgeType(data)">
                  {{ buttonChecked(data) }}/{{ buttonTotal(data) }}
                </el-tag>
              </div>
            </template>
          </el-tree>
        </aside>

        <section class="perm-panel">
          <div v-if="!activeMenu" class="perm-empty">请从左侧选择菜单</div>
          <template v-else>
            <div class="perm-breadcrumb">
              <span class="font-medium">{{ activeMenu.menuName }}</span>
              <span class="text-xs text-slate-500 ml-2">{{ activeMenu.path || '无路由' }}</span>
            </div>
            <div v-if="filteredButtons.length === 0" class="perm-empty text-sm">
              该菜单下暂无按钮权限
            </div>
            <div v-else class="perm-grid">
              <label
                v-for="btn in filteredButtons"
                :key="btn.id"
                class="perm-card"
                :class="{ 'perm-card--highlight': isKeywordHit(btn) }"
              >
                <el-checkbox
                  :model-value="checkedPermIds.has(btn.id)"
                  @change="(v: boolean) => toggleButton(btn.id, v)"
                />
                <div class="perm-meta">
                  <span class="perm-name">{{ btn.menuName }}</span>
                  <code class="perm-code">{{ btn.permCode }}</code>
                  <span v-if="btn.apiPattern" class="perm-api">{{ btn.apiPattern }}</span>
                </div>
              </label>
            </div>
          </template>
        </section>
      </div>
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" class="btn-primary" :loading="saving" @click="handleSave">保存权限</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { ElTree } from 'element-plus'
import { assignRoleMenus, getMenuTree, getRoleMenus } from '@/api/admin'
import type { SysMenu, SysRole } from '@/types'

const props = defineProps<{
  visible: boolean
  role: SysRole | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved'): void
}>()

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const showCheckedOnly = ref(false)
const menuTree = ref<SysMenu[]>([])
const activeMenuId = ref<number | null>(null)
const checkedMenuIds = ref<Set<number>>(new Set())
const checkedPermIds = ref<Set<number>>(new Set())
const initialMenuIds = ref<Set<number>>(new Set())
const initialPermIds = ref<Set<number>>(new Set())
const menuTreeRef = ref<InstanceType<typeof ElTree>>()

const buttonMap = computed(() => {
  const map = new Map<number, SysMenu[]>()
  function walk(nodes: SysMenu[], parentMenuId: number | null) {
    for (const n of nodes) {
      if (n.menuType === 'BUTTON' && parentMenuId != null) {
        const list = map.get(parentMenuId) ?? []
        list.push(n)
        map.set(parentMenuId, list)
      }
      if (n.children?.length) {
        const nextParent = n.menuType === 'MENU' ? n.id : parentMenuId
        walk(n.children, nextParent)
      }
    }
  }
  walk(menuTree.value, null)
  return map
})

const menuOnlyTree = computed(() => stripButtons(menuTree.value))

const activeMenu = computed(() => findMenuById(menuOnlyTree.value, activeMenuId.value))

const currentButtons = computed(() => {
  if (!activeMenuId.value) {
    return []
  }
  return buttonMap.value.get(activeMenuId.value) ?? []
})

const filteredButtons = computed(() => {
  let list = currentButtons.value
  if (showCheckedOnly.value) {
    list = list.filter((b) => checkedPermIds.value.has(b.id))
  }
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) {
    return list
  }
  return list.filter(
    (b) =>
      b.menuName.toLowerCase().includes(kw) ||
      (b.permCode?.toLowerCase().includes(kw) ?? false),
  )
})

watch(
  () => [props.visible, props.role?.id] as const,
  ([vis]) => {
    if (vis && props.role) {
      loadData(props.role)
    }
  },
)

watch(keyword, (kw) => {
  menuTreeRef.value?.filter(kw.trim())
})

function stripButtons(nodes: SysMenu[]): SysMenu[] {
  return nodes
    .filter((n) => n.menuType === 'MENU')
    .map((n) => ({
      ...n,
      children: n.children?.length ? stripButtons(n.children) : undefined,
    }))
}

function findMenuById(nodes: SysMenu[], id: number | null): SysMenu | null {
  if (id == null) {
    return null
  }
  for (const n of nodes) {
    if (n.id === id) {
      return n
    }
    if (n.children?.length) {
      const found = findMenuById(n.children, id)
      if (found) {
        return found
      }
    }
  }
  return null
}

function collectButtons(nodes: SysMenu[]): SysMenu[] {
  const buttons: SysMenu[] = []
  function walk(list: SysMenu[]) {
    for (const n of list) {
      if (n.menuType === 'BUTTON') {
        buttons.push(n)
      }
      if (n.children?.length) {
        walk(n.children)
      }
    }
  }
  walk(nodes)
  return buttons
}

function collectMenus(nodes: SysMenu[]): SysMenu[] {
  const menus: SysMenu[] = []
  function walk(list: SysMenu[]) {
    for (const n of list) {
      if (n.menuType === 'MENU') {
        menus.push(n)
      }
      if (n.children?.length) {
        walk(n.children)
      }
    }
  }
  walk(nodes)
  return menus
}

function buttonTotal(menu: SysMenu): number {
  return buttonMap.value.get(menu.id)?.length ?? 0
}

function buttonChecked(menu: SysMenu): number {
  return (buttonMap.value.get(menu.id) ?? []).filter((b) => checkedPermIds.value.has(b.id)).length
}

function badgeType(menu: SysMenu): 'success' | 'warning' | 'info' {
  const total = buttonTotal(menu)
  const checked = buttonChecked(menu)
  if (total === 0) {
    return 'info'
  }
  if (checked === total) {
    return 'success'
  }
  if (checked > 0) {
    return 'warning'
  }
  return 'info'
}

function filterMenuNode(value: string, data: SysMenu) {
  if (!value) {
    return true
  }
  const kw = value.toLowerCase()
  if (data.menuName.toLowerCase().includes(kw)) {
    return true
  }
  const buttons = buttonMap.value.get(data.id) ?? []
  return buttons.some(
    (b) =>
      b.menuName.toLowerCase().includes(kw) ||
      (b.permCode?.toLowerCase().includes(kw) ?? false),
  )
}

function isKeywordHit(btn: SysMenu): boolean {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) {
    return false
  }
  return (
    btn.menuName.toLowerCase().includes(kw) ||
    (btn.permCode?.toLowerCase().includes(kw) ?? false)
  )
}

function onMenuNodeClick(data: SysMenu) {
  activeMenuId.value = data.id
}

function toggleMenu(menu: SysMenu, checked: boolean) {
  if (checked) {
    checkedMenuIds.value.add(menu.id)
  } else {
    checkedMenuIds.value.delete(menu.id)
    for (const btn of buttonMap.value.get(menu.id) ?? []) {
      checkedPermIds.value.delete(btn.id)
    }
  }
  checkedMenuIds.value = new Set(checkedMenuIds.value)
  checkedPermIds.value = new Set(checkedPermIds.value)
}

function toggleButton(id: number, checked: boolean) {
  if (checked) {
    checkedPermIds.value.add(id)
  } else {
    checkedPermIds.value.delete(id)
  }
  checkedPermIds.value = new Set(checkedPermIds.value)
}

function selectAllCurrent() {
  for (const btn of currentButtons.value) {
    checkedPermIds.value.add(btn.id)
  }
  checkedPermIds.value = new Set(checkedPermIds.value)
}

function invertCurrent() {
  for (const btn of currentButtons.value) {
    if (checkedPermIds.value.has(btn.id)) {
      checkedPermIds.value.delete(btn.id)
    } else {
      checkedPermIds.value.add(btn.id)
    }
  }
  checkedPermIds.value = new Set(checkedPermIds.value)
}

function resetSelection() {
  checkedMenuIds.value = new Set(initialMenuIds.value)
  checkedPermIds.value = new Set(initialPermIds.value)
}

async function loadData(role: SysRole) {
  loading.value = true
  keyword.value = ''
  showCheckedOnly.value = false
  try {
    const [tree, roleMenus] = await Promise.all([getMenuTree(), getRoleMenus(role.id)])
    menuTree.value = Array.isArray(tree) ? tree : []
    const assigned = Array.isArray(roleMenus) ? roleMenus : []
    const menus = collectMenus(assigned)
    const buttons = collectButtons(assigned)
    checkedMenuIds.value = new Set(menus.map((m) => m.id))
    checkedPermIds.value = new Set(buttons.map((b) => b.id))
    initialMenuIds.value = new Set(checkedMenuIds.value)
    initialPermIds.value = new Set(checkedPermIds.value)
    const first = menuOnlyTree.value[0]
    activeMenuId.value = first?.id ?? null
  } catch {
    ElMessage.error('加载权限数据失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!props.role) {
    return
  }
  saving.value = true
  try {
    const allIds = [...checkedMenuIds.value, ...checkedPermIds.value]
    await assignRoleMenus(props.role.id, allIds)
    ElMessage.success('权限已保存')
    emit('saved')
    emit('update:visible', false)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '保存权限失败'
    ElMessage.error(msg)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.perm-drawer {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 140px);
  min-height: 480px;
}

.perm-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.perm-search {
  width: 240px;
}

.perm-body {
  display: flex;
  flex: 1;
  min-height: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}

.perm-nav {
  width: 320px;
  flex-shrink: 0;
  border-right: 1px solid var(--el-border-color-lighter);
  overflow: auto;
  padding: 8px;
}

.perm-nav-node {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding-right: 4px;
}

.perm-nav-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.perm-panel {
  flex: 1;
  overflow: auto;
  padding: 12px 16px;
}

.perm-breadcrumb {
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.perm-empty {
  color: var(--el-text-color-secondary);
  padding: 48px 16px;
  text-align: center;
}

.perm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 10px;
}

.perm-card {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 10px 12px;
  min-height: 72px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s;
}

.perm-card:hover {
  border-color: var(--el-color-primary);
}

.perm-card--highlight {
  border-color: var(--el-color-warning);
  background: var(--el-color-warning-light-9);
}

.perm-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.perm-name {
  font-size: 13px;
  font-weight: 500;
}

.perm-code {
  font-size: 11px;
  color: var(--el-color-primary);
  background: var(--el-fill-color-light);
  padding: 1px 6px;
  border-radius: 4px;
  width: fit-content;
}

.perm-api {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  word-break: break-all;
}
</style>
