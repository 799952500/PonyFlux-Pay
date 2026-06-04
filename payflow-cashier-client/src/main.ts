import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import './styles/main.css'
import { installPfSurface } from '@/composables/usePfSurface'
import { i18n } from './i18n'
import { applyDisplayLocale, detectBrowserLocale, isPortalRoute } from '@/composables/useDisplayLocale'

installPfSurface()

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)
app.use(ElementPlus)

const initialPath = typeof window !== 'undefined' ? window.location.pathname : '/'
if (isPortalRoute(initialPath)) {
  void applyDisplayLocale(detectBrowserLocale(), { persist: false })
} else {
  void applyDisplayLocale('zh-CN', { persist: false })
}

app.mount('#app')
