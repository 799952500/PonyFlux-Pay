<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { installGlowPulseDelegation } from '@/utils/glowPulse'

onMounted(() => {
  installGlowPulseDelegation()
})
</script>
