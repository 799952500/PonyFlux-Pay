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
        <span class="menu-icon">{{ node.icon || '•' }}</span>
        <span class="menu-text">{{ node.menuName }}</span>
      </template>
      <MenuTree :nodes="node.children" />
    </el-sub-menu>
    <el-menu-item v-else-if="node.path" :index="node.path">
      <span v-if="node.icon" class="menu-icon">{{ node.icon }}</span>
      <span class="menu-text">{{ node.menuName }}</span>
    </el-menu-item>
  </template>
</template>
