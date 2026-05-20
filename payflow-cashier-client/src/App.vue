<template>
  <router-view v-slot="{ Component }">
    <transition name="page-zoom" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { installGlowPulseDelegation } from '@/utils/glowPulse'

onMounted(() => {
  installGlowPulseDelegation()
})
</script>
