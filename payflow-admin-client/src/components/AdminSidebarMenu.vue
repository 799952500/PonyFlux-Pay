<script setup lang="ts">
import type { SysMenu } from '@/types'
import { resolveMenuIcon } from '@/utils/menuIcon'
import MenuTree from './AdminSidebarMenu.vue'

defineProps<{
  nodes: SysMenu[]
}>()
</script>

<template>
  <template v-for="node in nodes" :key="node.id">
    <el-sub-menu v-if="node.children && node.children.length > 0" :index="'sub-' + node.id">
      <template #title>
        <el-icon class="menu-icon menu-icon--group">
          <component :is="resolveMenuIcon(node)" />
        </el-icon>
        <span class="menu-text">{{ node.menuName }}</span>
      </template>
      <MenuTree :nodes="node.children" />
    </el-sub-menu>
    <el-menu-item v-else-if="node.path" :index="node.path">
      <el-icon class="menu-icon menu-icon--leaf">
        <component :is="resolveMenuIcon(node)" />
      </el-icon>
      <span class="menu-text">{{ node.menuName }}</span>
    </el-menu-item>
  </template>
</template>

<style scoped>
.menu-icon {
  font-size: 18px;
  flex-shrink: 0;
  color: var(--pf-sidebar-text-muted);
  transition: color 0.2s ease;
}

.menu-icon--group {
  font-size: 19px;
}

.menu-icon--leaf {
  font-size: 16px;
}

.menu-text {
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.02em;
}
</style>
