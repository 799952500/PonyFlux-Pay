<template>
  <el-popover
    :visible="visible"
    placement="bottom-end"
    :width="380"
    trigger="click"
    @update:visible="visible = $event"
  >
    <template #reference>
      <el-badge
        :value="store.unreadCount > 999 ? '999+' : store.unreadCount"
        class="cursor-pointer"
        :hidden="store.unreadCount === 0"
      >
        <el-button circle class="topbar-notify-btn" @click="onBellClick">
          <el-icon><Bell /></el-icon>
        </el-button>
      </el-badge>
    </template>

    <div class="notification-popover">
      <div class="popover-header">
        <span class="font-semibold">{{ t('notifications.title') }}</span>
        <el-button link type="primary" size="small" @click="handleMarkAllRead" :disabled="items.length === 0">
          {{ t('notifications.markAllRead') }}
        </el-button>
      </div>

      <el-scrollbar max-height="360px">
        <div v-if="loading" class="popover-empty">
          <el-icon class="is-loading"><Loading /></el-icon>
        </div>
        <div v-else-if="items.length === 0" class="popover-empty">
          <el-empty :image-size="64" :description="t('notifications.emptyUnread')" />
        </div>
        <div
          v-else
          v-for="item in items"
          :key="item.id"
          class="notification-item"
          @click="handleItemClick(item)"
        >
          <div class="item-icon">
            <el-icon :size="18">
              <component :is="iconMap[item.bizType] || Notification" />
            </el-icon>
          </div>
          <div class="item-content">
            <div class="item-title">{{ item.title }}</div>
            <div class="item-time">{{ relativeTime(item.createdAt) }}</div>
          </div>
        </div>
      </el-scrollbar>

      <div class="popover-footer">
        <el-button link type="primary" @click="goToList">{{ t('notifications.viewAll') }}</el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, markRaw, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Bell, Warning, Clock, Download, CloseBold, DataAnalysis, Connection, Notification, Loading } from '@element-plus/icons-vue'
import { getNotifications, markNotificationRead, markAllNotificationsRead } from '@/api/admin'
import type { NotificationItem } from '@/api/admin'
import { useNotificationStore } from '@/composables/useNotification'
import dayjs from 'dayjs'
import relativeTimePlugin from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import 'dayjs/locale/en'

dayjs.extend(relativeTimePlugin)

const { t, locale } = useI18n()
const router = useRouter()
const store = useNotificationStore()

const visible = ref(false)
const loading = ref(false)
const items = ref<NotificationItem[]>([])

const iconMap: Record<string, any> = {
  REFUND_APPROVAL: markRaw(Warning),
  CHURN_OVERDUE: markRaw(Clock),
  EXPORT_COMPLETED: markRaw(Download),
  EXPORT_FAILED: markRaw(CloseBold),
  RECON_DIFF: markRaw(DataAnalysis),
  WEBHOOK_FAILURE: markRaw(Connection),
  SYSTEM_ANNOUNCEMENT: markRaw(Notification),
}

function relativeTime(dateStr: string) {
  const loc = locale.value === 'en-US' ? 'en' : 'zh-cn'
  return dayjs(dateStr).locale(loc).fromNow()
}

watch(locale, (loc) => {
  dayjs.locale(loc === 'en-US' ? 'en' : 'zh-cn')
})

async function fetchItems() {
  loading.value = true
  try {
    const result = await getNotifications({ read: 'false', size: 10 })
    items.value = result.list
  } catch {
    items.value = []
    ElMessage.warning(t('notifications.loadFailed'))
  } finally {
    loading.value = false
  }
}

function onBellClick() {
  if (!visible.value) {
    fetchItems()
  }
}

async function handleItemClick(item: NotificationItem) {
  try {
    await markNotificationRead(item.id)
    store.decrementUnread(1)
    items.value = items.value.filter(i => i.id !== item.id)
  } catch { /* 静默 */ }
  visible.value = false
  if (item.link) {
    router.push(item.link)
  }
}

async function handleMarkAllRead() {
  try {
    await markAllNotificationsRead()
    store.resetUnread()
    items.value = []
  } catch { /* 静默 */ }
}

function goToList() {
  visible.value = false
  router.push('/admin/notifications')
}
</script>

<style scoped>
.notification-popover {
  margin: -12px;
}
.popover-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.popover-footer {
  display: flex;
  justify-content: center;
  padding: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.popover-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 120px;
}
.notification-item {
  display: flex;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
}
.notification-item:hover {
  background: var(--el-fill-color-light);
}
.item-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.item-content {
  flex: 1;
  min-width: 0;
}
.item-title {
  font-size: 13px;
  line-height: 1.4;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}
</style>
