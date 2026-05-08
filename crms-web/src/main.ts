import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import 'nprogress/nprogress.css'

import App from './App.vue'
import router from './router'
import { setupPermissionDirective } from '@/utils/permission'
import './assets/styles/index.scss'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
setupPermissionDirective(app)

// 全局注册 element-plus icons，让侧边栏 / 菜单 / 表单中
// `<component :is="'Odometer'" />` 这种按名渲染能解析得到。
for (const [name, comp] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, comp)
}

app.mount('#app')
