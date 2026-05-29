<template>
  <div class="page-table-shell">
    <div class="content-card">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-semibold">{{ t('notifications.title') }}</h2>
        <div class="flex gap-2">
          <el-button
            type="primary"
            :disabled="selectedIds.length === 0"
            @click="handleBatchRead"
          >
            {{ t('notifications.batchRead') }} ({{ selectedIds.length }})
          </el-button>
          <el-button @click="handleMarkAllRead">
            {{ t('notifications.markAllRead') }}
          </el-button>
        </div>
      </div>

      <div class="flex items-center gap-4 mb-4">
        <el-radio-group v-model="readFilter" @change="onFilterChange">
          <el-radio-button value="all">
            {{ t('notifications.tabs.all') }}
          </el-radio-button>
          <el-radio-button value="false">
            {{ t('notifications.tabs.unread') }}
            <el-badge v-if="unreadCount > 0" :value="unreadCount" class="ml-1" />
          </el-radio-button>
          <el-radio-button value="true">
            {{ t('notifications.tabs.read') }}
          </el-radio-button>
        </el-radio-group>

        <el-select
          v-model="typeFilter"
          :placeholder="t('notifications.filterType')"
          clearable
          class="w-48"
          @change="onFilterChange"
        >
          <el-option
            v-for="bt in bizTypes"
            :key="bt.value"
            :label="bt.label"
            :value="bt.value"
          />
        </el-select>
      </div>

      <div v-loading="loading">
        <div v-if="items.length === 0 && !loading" class="py-12">
          <el-empty :description="t('notifications.empty')" />
        </div>

        <div v-else class="notification-list">
          <div
            v-for="item in items"
            :key="item.id"
            class="notification-row"
            :class="{ unread: item.readStatus === 0 }"
          >
            <el-checkbox
              :model-value="selectedIds.includes(item.id)"
              @change="toggleSelect(item.id)"
              class="mr-3"
            />
            <div
              class="notification-row-body"
              @click="handleItemClick(item)"
            >
              <div class="flex items-center gap-2">
                <el-tag size="small" :type="tagType(item.bizType)" effect="plain">
                  {{ bizTypeLabel(item.bizType) }}
                </el-tag>
                <span class="notification-row-title">{{ item.title }}</span>
              </div>
              <div class="notification-row-meta">
                <span v-if="item.summary" class="text-gray-500 text-sm truncate max-w-[400px]">{{ item.summary }}</span>
                <span class="text-gray-400 text-xs ml-auto flex-shrink-0">{{ relativeTime(item.createdAt) }}</span>
              </div>
            </div>
            <el-tag v-if="item.readStatus === 0" size="small" type="danger" effect="dark" class="ml-2 flex-shrink-0">
              {{ t('notifications.tabs.unread') }}
            </el-tag>
          </div>
        </div>
      </div>

      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchList"
          @size-change="fetchList"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  getNotifications,
  getUnreadCount,
  markNotificationRead,
  markAllNotificationsRead,
  markBatchNotificationsRead,
} from '@/api/admin'
import type { NotificationItem } from '@/api/admin'
import { useNotificationStore } from '@/composables/useNotification'
import dayjs from 'dayjs'
import relativeTimePlugin from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTimePlugin)
dayjs.locale('zh-cn')

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const notifStore = useNotificationStore()

const loading = ref(false)
const items = ref<NotificationItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const readFilter = ref((route.query.read as string) || 'all')
const typeFilter = ref((route.query.type as string) || '')
const selectedIds = ref<(string | number)[]>([])
const unreadCount = computed(() => notifStore.unreadCount)

const bizTypes = [
  { value: 'REFUND_APPROVAL', label: t('notifications.types.refundApproval') },
  { value: 'CHURN_OVERDUE', label: t('notifications.types.churnOverdue') },
  { value: 'EXPORT_COMPLETED', label: t('notifications.types.exportCompleted') },
  { value: 'EXPORT_FAILED', label: t('notifications.types.exportFailed') },
  { value: 'RECON_DIFF', label: t('notifications.types.reconDiff') },
  { value: 'WEBHOOK_FAILURE', label: t('notifications.types.webhookFailure') },
]

const tagTypeMap: Record<string, string> = {
  REFUND_APPROVAL: 'warning',
  CHURN_OVERDUE: 'danger',
  EXPORT_COMPLETED: 'success',
  EXPORT_FAILED: 'danger',
  RECON_DIFF: 'info',
  WEBHOOK_FAILURE: 'danger',
}

function tagType(bizType: string) {
  return (tagTypeMap[bizType] || '') as any
}

function bizTypeLabel(bizType: string) {
  const found = bizTypes.find(b => b.value === bizType)
  return found?.label || bizType
}

function relativeTime(dateStr: string) {
  return dayjs(dateStr).fromNow()
}

async function fetchList() {
  loading.value = true
  selectedIds.value = []
  try {
    const params: Record<string, any> = { page: page.value, size: pageSize.value }
    if (readFilter.value !== 'all') params.read = readFilter.value
    if (typeFilter.value) params.type = typeFilter.value
    const result = await getNotifications(params)
    items.value = result.list
    total.value = result.total
  } catch (e: any) {
    ElMessage.error(e?.message || '加载通知失败')
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  page.value = 1
  router.replace({
    query: {
      ...route.query,
      read: readFilter.value !== 'all' ? readFilter.value : undefined,
      type: typeFilter.value || undefined,
    },
  })
  fetchList()
}

function toggleSelect(id: string | number) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

async function handleItemClick(item: NotificationItem) {
  if (item.readStatus === 0) {
    try {
      await markNotificationRead(item.id)
      item.readStatus = 1
      notifStore.decrementUnread(1)
    } catch { /* 静默 */ }
  }
  if (item.link) {
    router.push(item.link)
  }
}

async function handleBatchRead() {
  try {
    const { affected } = await markBatchNotificationsRead(selectedIds.value)
    notifStore.decrementUnread(affected)
    selectedIds.value = []
    fetchList()
    ElMessage.success(`已标记 ${affected} 条为已读`)
  } catch (e: any) {
    ElMessage.error(e?.message || '批量标记失败')
  }
}

async function handleMarkAllRead() {
  try {
    const { affected } = await markAllNotificationsRead()
    notifStore.resetUnread()
    fetchList()
    ElMessage.success(`已标记 ${affected} 条为已读`)
  } catch (e: any) {
    ElMessage.error(e?.message || '标记失败')
  }
}

onMounted(() => {
  fetchList()
  notifStore.fetchUnreadCount()
})
</script>

<style scoped>
.notification-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  background: var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}
.notification-row {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-bg-color);
  cursor: pointer;
  transition: background 0.15s;
}
.notification-row:hover {
  background: var(--el-fill-color-light);
}
.notification-row.unread {
  background: var(--el-color-primary-light-9);
}
.notification-row.unread:hover {
  background: var(--el-color-primary-light-8);
}
.notification-row-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.notification-row-title {
  font-size: 14px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notification-row-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
