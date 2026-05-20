<script setup lang="ts">
import type { SysMenu } from '@/types'
import MenuTree from './AdminSidebarMenu.vue'

defineProps<{
  nodes: SysMenu[]
}>()
</script>

<template>
  <template v-for="node in nodes" :key="node.id">
    <el-sub-menu v-if="node.children && node.children.length > 0" :index="'sub-' + node.id">
      <template #title>
        <span v-if="node.icon" class="menu-icon menu-icon--text">{{ node.icon }}</span>
        <span v-else class="menu-leaf-dot menu-leaf-dot--group" />
        <span class="menu-text">{{ node.menuName }}</span>
      </template>
      <MenuTree :nodes="node.children" />
    </el-sub-menu>
    <el-menu-item v-else-if="node.path" :index="node.path">
      <span v-if="node.icon" class="menu-icon menu-icon--text">{{ node.icon }}</span>
      <span v-else class="menu-leaf-dot" />
      <span class="menu-text">{{ node.menuName }}</span>
    </el-menu-item>
  </template>
</template>

<style scoped>
.menu-icon--text {
  font-size: 16px;
  width: 20px;
  text-align: center;
  flex-shrink: 0;
}

.menu-leaf-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(148, 163, 184, 0.6);
  flex-shrink: 0;
}

.menu-leaf-dot--group {
  background: rgba(148, 163, 184, 0.45);
}

.menu-text {
  font-size: 14px;
  font-weight: 500;
}
</style>
