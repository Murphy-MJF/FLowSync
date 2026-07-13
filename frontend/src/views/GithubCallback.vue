<template>
  <div style="text-align:center;padding:60px 20px">
    <h2 v-if="loading">正在完成 GitHub 授权...</h2>
    <h2 v-else-if="error" style="color:#F56C6C">{{ error }}</h2>
    <h2 v-else style="color:#67C23A">授权成功！窗口即将关闭</h2>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { githubCallback } from '../api'

const loading = ref(true)
const error = ref('')

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  const code = params.get('code')
  if (!code) {
    error.value = '未收到授权码，请重试'
    loading.value = false
    return
  }
  try {
    const res = await githubCallback(code)
    if (res.success) {
      loading.value = false
      setTimeout(() => window.close(), 1500)
    } else {
      error.value = res.message || '授权失败'
      loading.value = false
    }
  } catch (e) {
    error.value = '网络错误，请重试'
    loading.value = false
  }
})
</script>
