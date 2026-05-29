import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.js'
import en from 'element-plus/dist/locale/en.js'

import App from './App.vue'
import router from './router'
import './styles/main.css'
import { installPfSurface } from '@/composables/usePfSurface'
import { i18n, resolveInitialLocale } from './i18n'

const savedLocale = resolveInitialLocale()

installPfSurface()

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)
app.use(ElementPlus, { locale: savedLocale === 'en-US' ? en : zhCn })

app.mount('#app')
