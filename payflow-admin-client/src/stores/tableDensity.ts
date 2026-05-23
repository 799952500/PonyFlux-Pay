import { ref } from 'vue'
import { defineStore } from 'pinia'

export type TableDensityKey = 'standard' | 'compact'

const DENSITY_STORAGE_KEY = 'adminTableDensity'

const VALID_DENSITIES: TableDensityKey[] = ['standard', 'compact']

export interface TableDensityPreset {
  key: TableDensityKey
  label: string
  description: string
}

export const TABLE_DENSITY_PRESETS: TableDensityPreset[] = [
  { key: 'standard', label: '标准', description: '舒适行高，适合日常浏览' },
  { key: 'compact', label: '紧凑', description: '收敛行高，适合宽表与密集列表' },
]

export function isTableDensityKey(value: string | null | undefined): value is TableDensityKey {
  return value != null && VALID_DENSITIES.includes(value as TableDensityKey)
}

function canUseLocalStorage(): boolean {
  try {
    const key = '__pf_ls_test__'
    localStorage.setItem(key, '1')
    localStorage.removeItem(key)
    return true
  } catch {
    return false
  }
}

export const useTableDensityStore = defineStore('tableDensity', () => {
  const saved = canUseLocalStorage() ? localStorage.getItem(DENSITY_STORAGE_KEY) : null
  const densityKey = ref<TableDensityKey>(isTableDensityKey(saved) ? saved : 'standard')
  const storageAvailable = ref(canUseLocalStorage())
  const storageHintShown = ref(false)

  function apply() {
    document.documentElement.setAttribute('data-table-density', densityKey.value)
  }

  function setDensity(key: TableDensityKey) {
    densityKey.value = key
    if (storageAvailable.value) {
      localStorage.setItem(DENSITY_STORAGE_KEY, key)
    } else if (!storageHintShown.value) {
      storageHintShown.value = true
    }
    apply()
    import('@/composables/useAppearancePreferences').then((m) => m.schedulePersistUiPreferences())
  }

  function init() {
    storageAvailable.value = canUseLocalStorage()
    if (storageAvailable.value) {
      const stored = localStorage.getItem(DENSITY_STORAGE_KEY)
      if (isTableDensityKey(stored)) {
        densityKey.value = stored
      }
    }
    apply()
  }

  return {
    densityKey,
    storageAvailable,
    storageHintShown,
    setDensity,
    init,
    apply,
  }
})
