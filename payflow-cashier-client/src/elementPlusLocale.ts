import { ref } from 'vue'
import zhCn from 'element-plus/dist/locale/zh-cn.js'
import zhTw from 'element-plus/dist/locale/zh-tw.mjs'
import en from 'element-plus/dist/locale/en.js'
import type { DisplayLocaleCode } from '@/composables/useDisplayLocale'

export const elementPlusLocale = ref(zhCn)

export function setElementPlusLocale(code: DisplayLocaleCode) {
  if (code === 'en-US') {
    elementPlusLocale.value = en
  } else if (code === 'zh-TW') {
    elementPlusLocale.value = zhTw
  } else {
    elementPlusLocale.value = zhCn
  }
}
