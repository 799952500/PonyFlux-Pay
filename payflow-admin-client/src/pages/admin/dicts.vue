<template>
  <div>
    <div class="bg-white rounded-xl p-5 card-shadow mb-4">
      <p class="text-sm text-slate-600 m-0">
        枚举字典来自接口 <code class="text-xs bg-slate-100 px-1 rounded">GET /api/v1/admin/dicts</code>，供筛选选项与前后端对齐。
      </p>
    </div>
    <div v-loading="loading" class="bg-white rounded-xl card-shadow p-5">
      <el-tabs v-if="loaded" v-model="activeTab">
        <el-tab-pane v-for="(val, key) in raw" :key="key" :label="String(key)" :name="String(key)">
          <pre class="dict-pre text-xs text-slate-700 overflow-auto max-h-[480px] m-0">{{ format(val) }}</pre>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDicts } from '@/api/admin'

const loading = ref(false)
const loaded = ref(false)
const raw = ref<Record<string, unknown>>({})
const activeTab = ref('refundStatus')

function format(val: unknown): string {
  try {
    return JSON.stringify(val, null, 2)
  } catch {
    return String(val)
  }
}

onMounted(async () => {
  loading.value = true
  try {
    raw.value = await getDicts()
    const keys = Object.keys(raw.value)
    if (keys.length) activeTab.value = keys[0]
    loaded.value = true
  } catch {
    ElMessage.error('加载字典失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.card-shadow {
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(99, 102, 241, 0.08);
}

.dict-pre {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  background: #f8fafc;
  padding: 12px;
  border-radius: 8px;
}
</style>
