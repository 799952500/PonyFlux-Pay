<template>
  <div class="content-card">
    <h2 class="text-lg font-semibold mb-4">渠道路由健康度</h2>
    <el-form :inline="true" @submit.prevent="load">
      <el-form-item label="账户编码">
        <el-input v-model="accountCode" placeholder="如 acc_wx_01" clearable style="width:220px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>
    <pre class="text-sm bg-slate-900 text-slate-100 p-4 rounded overflow-auto">{{ json }}</pre>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getChannelRoutingHealth } from '@/api/admin'

const accountCode = ref('')
const json = ref('（输入账户编码后查询）')

async function load() {
  if (!accountCode.value.trim()) return
  try {
    const d = await getChannelRoutingHealth(accountCode.value.trim())
    json.value = JSON.stringify(d, null, 2)
  } catch {
    json.value = '加载失败'
  }
}
</script>
