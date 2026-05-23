import { useThemeStore, type ThemeKey, isThemeKey } from '@/stores/theme'
import { useTableDensityStore, type TableDensityKey, isTableDensityKey } from '@/stores/tableDensity'
import { useAdminStore } from '@/stores/admin'
import { updateUiPreferences } from '@/api/auth'
import type { AdminUiPreferences } from '@/types'

let persistTimer: ReturnType<typeof setTimeout> | null = null
let skipNextPersist = false

/**
 * 将服务端下发的 UI 偏好应用到本地（主题、表格密度、侧栏），并写入 localStorage 缓存。
 */
export function applyUiPreferencesFromServer(prefs?: AdminUiPreferences | null) {
  if (!prefs) return
  skipNextPersist = true
  const themeStore = useThemeStore()
  const densityStore = useTableDensityStore()

  if (isThemeKey(prefs.themeKey)) {
    themeStore.themeKey = prefs.themeKey
    localStorage.setItem('adminTheme', prefs.themeKey)
    themeStore.apply()
  }
  if (isTableDensityKey(prefs.tableDensity)) {
    densityStore.densityKey = prefs.tableDensity
    if (densityStore.storageAvailable) {
      localStorage.setItem('adminTableDensity', prefs.tableDensity)
    }
    densityStore.apply()
  }
  if (typeof prefs.sidebarCollapsed === 'boolean') {
    themeStore.sidebarCollapsed = prefs.sidebarCollapsed
    localStorage.setItem('adminSidebarCollapsed', String(prefs.sidebarCollapsed))
  }
  setTimeout(() => {
    skipNextPersist = false
  }, 0)
}

/**
 * 防抖后将当前本地 UI 状态同步到数据库（需已登录）。
 */
export function schedulePersistUiPreferences() {
  if (skipNextPersist || !useAdminStore().isLoggedIn()) return
  if (persistTimer) clearTimeout(persistTimer)
  persistTimer = setTimeout(async () => {
    const themeStore = useThemeStore()
    const densityStore = useTableDensityStore()
    try {
      const data = await updateUiPreferences({
        themeKey: themeStore.themeKey as ThemeKey,
        tableDensity: densityStore.densityKey as TableDensityKey,
        sidebarCollapsed: themeStore.sidebarCollapsed,
      })
      applyUiPreferencesFromServer(data)
    } catch {
      /* 静默：保留本地设置，下次登录 profile 可再同步 */
    }
  }, 400)
}
