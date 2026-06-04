import { createI18n } from 'vue-i18n'
import { zhCN, zhTW, enUS } from './locales'

export const LOCALE_STORAGE_KEY = 'payflow-cashier-locale'

export function resolveInitialLocale(): string {
  return localStorage.getItem(LOCALE_STORAGE_KEY) || 'zh-CN'
}

export const i18n = createI18n({
  legacy: false,
  locale: resolveInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'zh-TW': zhTW,
    'en-US': enUS,
  },
})
