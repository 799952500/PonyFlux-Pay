<template>
  <div class="content-card">
    <h2 class="text-lg font-semibold mb-4">支付漏斗（占位）</h2>
    <pre class="text-sm bg-slate-900 text-slate-100 p-4 rounded overflow-auto">{{ json }}</pre>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getInsightsFunnel } from '@/api/admin'

const json = ref('')

onMounted(async () => {
  try {
    const d = await getInsightsFunnel()
    json.value = JSON.stringify(d, null, 2)
  } catch {
    json.value = '加载失败'
  }
})
</script>
