<template>
  <el-config-provider :locale="elementPlusLocale">
    <router-view v-slot="{ Component }">
      <transition name="page-zoom" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { installGlowPulseDelegation } from '@/utils/glowPulse'
import { elementPlusLocale } from '@/elementPlusLocale'

onMounted(() => {
  installGlowPulseDelegation()
})
</script>
