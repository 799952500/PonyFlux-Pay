<template>
  <div class="notifications-page">
    <div class="content-card mb-4">
      <h2 class="page-title">{{ t('notifications.title') }}</h2>
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
import request from '@/api/request'

const { t } = useI18n()
const pendingRefunds = ref(0)

onMounted(async () => {
  try {
    const data = await request.get('/admin/notifications/summary') as { pendingRefunds?: number }
    pendingRefunds.value = data.pendingRefunds ?? 0
  } catch {
    pendingRefunds.value = 0
  }
})
</script>

<style scoped>
.notifications-page {
  max-width: 720px;
}

.content-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid rgba(99, 102, 241, 0.08);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  padding: 24px;
}

.page-title {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.stat-line {
  margin: 0 0 8px;
  font-size: 14px;
  color: #334155;
}

.stat-line strong {
  font-size: 20px;
  color: #047857;
}

.muted {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}
</style>
