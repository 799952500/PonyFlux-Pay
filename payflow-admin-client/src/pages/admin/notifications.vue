<template>
  <div class="page-table-shell">
    <div class="content-card">
      <p class="stat-line">
        {{ t('notifications.pendingRefunds') }}：
        <strong>{{ pendingRefunds }}</strong>
      </p>
      <p class="muted">{{ t('notifications.emptyAnnouncements') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const { t } = useI18n()
const pendingRefunds = ref(0)

onMounted(async () => {
  try {
    const data = await request.get('/admin/notifications/summary') as { pendingRefunds?: number }
    pendingRefunds.value = data.pendingRefunds ?? 0
  } catch (e: any) {
    pendingRefunds.value = 0
    ElMessage.error(e?.message || '加载通知摘要失败')
  }
})
</script>
