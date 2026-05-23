<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
onMounted(() => {
  router.replace({ path: '/admin/profile', query: { tab: 'appearance' } })
})
</script>

<template>
  <div class="page-table-shell" />
</template>
