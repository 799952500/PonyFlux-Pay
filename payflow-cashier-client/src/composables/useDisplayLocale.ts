import { nextTick } from 'vue'
import { i18n, LOCALE_STORAGE_KEY, resolveInitialLocale } from '@/i18n'
import { setElementPlusLocale } from '@/elementPlusLocale'

export type DisplayLocaleCode = 'zh-CN' | 'zh-TW' | 'en-US'

const SUPPORTED: DisplayLocaleCode[] = ['zh-CN', 'zh-TW', 'en-US']

export function normalizeDisplayLocale(raw: string | null | undefined): DisplayLocaleCode {
  if (!raw) return 'zh-CN'
  const trimmed = raw.trim()
  if (SUPPORTED.includes(trimmed as DisplayLocaleCode)) {
    return trimmed as DisplayLocaleCode
  }
  const lower = trimmed.toLowerCase()
  if (lower === 'zh-tw' || lower === 'zh_hant') return 'zh-TW'
  if (lower.startsWith('en')) return 'en-US'
  return 'zh-CN'
}

export function htmlLangFor(locale: DisplayLocaleCode): string {
  if (locale === 'en-US') return 'en'
  if (locale === 'zh-TW') return 'zh-TW'
  return 'zh-CN'
}

/**
 * 应用展示语言：vue-i18n、Element Plus、document.lang。
 * @param persist 是否写入 localStorage（门户页 true，订单页 false）
 */
export async function applyDisplayLocale(
  raw: string | null | undefined,
  options?: { persist?: boolean }
) {
  const locale = normalizeDisplayLocale(raw)
  if (i18n.global.locale.value !== locale) {
    i18n.global.locale.value = locale
  }
  if (options?.persist) {
    try {
      localStorage.setItem(LOCALE_STORAGE_KEY, locale)
    } catch {
      // localStorage 不可用时仅影响持久化
    }
  }
  setElementPlusLocale(locale)
  document.documentElement.lang = htmlLangFor(locale)
  await nextTick()
  return locale
}

/**
 * 门户页：浏览器语言启发式判定。
 */
export function detectBrowserLocale(): DisplayLocaleCode {
  if (typeof navigator === 'undefined') {
    return 'zh-CN'
  }
  const saved = resolveInitialLocale()
  if (saved && SUPPORTED.includes(saved as DisplayLocaleCode)) {
    return saved as DisplayLocaleCode
  }
  const langs = [...(navigator.languages ?? []), navigator.language].filter(Boolean)
  for (const lang of langs) {
    const lower = lang.toLowerCase()
    if (lower.includes('tw') || lower.includes('hk') || lower.includes('hant')) {
      return 'zh-TW'
    }
    if (lower.startsWith('en')) {
      return 'en-US'
    }
    if (lower.startsWith('zh')) {
      return 'zh-CN'
    }
  }
  return 'zh-CN'
}

export function isPortalRoute(path: string): boolean {
  return (
    path.startsWith('/login') ||
    path.startsWith('/register') ||
    path.startsWith('/onboarding')
  )
}
