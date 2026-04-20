import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import registerElementPlusIcons from '@element-plus/icons-vue/global'
import router from './router'
import App from './App.vue'
import './style.css'

const app = createApp(App)
/* 使 el-button 的 icon="Search" 等字符串能解析为图标组件，避免占位异常导致文字靠右 */
registerElementPlusIcons(app, { prefix: '' })
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
