import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

// 屏蔽 ResizeObserver loop 无害警告
const _ResizeObserver = window.ResizeObserver
window.ResizeObserver = class extends _ResizeObserver {
  constructor(callback) {
    callback = (entries, observer) => {
      requestAnimationFrame(() => {
        try { callback(entries, observer) } catch (e) {
          if (!e.message.includes('ResizeObserver')) throw e
        }
      })
    }
    super(callback)
  }
}

const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.mount('#app')
