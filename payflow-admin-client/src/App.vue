<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { installGlowPulseDelegation } from '@/utils/glowPulse'
import { useThemeStore } from '@/stores/theme'
import { useTableDensityStore } from '@/stores/tableDensity'

onMounted(() => {
  useThemeStore().init()
  useTableDensityStore().init()
  installGlowPulseDelegation()
})
</script>
