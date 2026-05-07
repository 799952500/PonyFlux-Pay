<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
onMounted(() => {
  router.replace('/admin/reconcile/tasks')
})
</script>

<template>
  <div class="p-8 text-slate-500 text-sm">正在跳转…</div>
</template>
