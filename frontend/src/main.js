import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

// 屏蔽 ResizeObserver 无害警告
window.addEventListener('error', e => {
  if (e.message.includes('ResizeObserver')) {
    e.stopImmediatePropagation()
  }
})

const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.mount('#app')
