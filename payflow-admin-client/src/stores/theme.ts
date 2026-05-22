import { ref } from 'vue'
import { defineStore } from 'pinia'

export type ThemeKey = 'mint' | 'ocean' | 'violet' | 'dark'

const THEME_STORAGE_KEY = 'adminTheme'
const SIDEBAR_STORAGE_KEY = 'adminSidebarCollapsed'

const VALID_THEMES: ThemeKey[] = ['mint', 'ocean', 'violet', 'dark']

export interface ThemePreset {
  key: ThemeKey
  label: string
  color: string
}

export const THEME_PRESETS: ThemePreset[] = [
  { key: 'mint', label: '清新薄荷', color: '#14b8a6' },
  { key: 'ocean', label: '海蓝', color: '#0284c7' },
  { key: 'violet', label: '紫罗兰', color: '#7c3aed' },
  { key: 'dark', label: '暗夜', color: '#2dd4bf' },
]

function isThemeKey(value: string | null): value is ThemeKey {
  return value !== null && VALID_THEMES.includes(value as ThemeKey)
}

export const useThemeStore = defineStore('theme', () => {
  const savedTheme = localStorage.getItem(THEME_STORAGE_KEY)
  const savedCollapsed = localStorage.getItem(SIDEBAR_STORAGE_KEY)

  const themeKey = ref<ThemeKey>(isThemeKey(savedTheme) ? savedTheme : 'mint')
  const sidebarCollapsed = ref(savedCollapsed === 'true')

  function apply() {
    const root = document.documentElement
    root.setAttribute('data-theme', themeKey.value)
    root.style.colorScheme = themeKey.value === 'dark' ? 'dark' : 'light'
  }

  function setTheme(key: ThemeKey) {
    themeKey.value = key
    localStorage.setItem(THEME_STORAGE_KEY, key)
    apply()
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem(SIDEBAR_STORAGE_KEY, String(sidebarCollapsed.value))
  }

  function init() {
    const stored = localStorage.getItem(THEME_STORAGE_KEY)
    if (isThemeKey(stored)) {
      themeKey.value = stored
    }
    sidebarCollapsed.value = localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true'
    apply()
  }

  return {
    themeKey,
    sidebarCollapsed,
    setTheme,
    toggleSidebar,
    init,
    apply,
  }
})
