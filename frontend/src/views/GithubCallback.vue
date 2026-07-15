<!--
  GitHub OAuth 回调页面
  用途：接收 GitHub 重定向返回的 ?code=xxx，自动调用后端交换 token 并关闭窗口。
  状态：当前已启用自动处理。如需回退到手动输入 code 模式，参考下方注释。

  回退步骤（手动输入 code 模式）：
  1. 删除本页面
  2. 在 router/index.js 中移除 /github-callback 路由
  3. 在 ProfilePanel.vue 的 handleGithubConnect 中取消注释手动输入代码块
  4. 重新 npm run serve
-->
<template>
  <div style="text-align:center;padding:60px 20px">
    <h2 v-if="status === 'loading'">正在完成 GitHub 授权...</h2>
    <h2 v-else-if="status === 'ok'" style="color:#67C23A">授权成功！窗口即将关闭</h2>
    <h2 v-else style="color:#F56C6C">{{ errorMsg }}</h2>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { githubCallback } from '../api'

const status = ref('loading')
const errorMsg = ref('')

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  const code = params.get('code')
  const state = params.get('state')
  const error = params.get('error')

  if (error) { status.value = 'error'; errorMsg.value = '授权被拒绝：' + error; return }
  if (!code) { status.value = 'error'; errorMsg.value = '未收到授权码'; return }

  try {
    const res = await githubCallback(code, state)
    if (res.success) {
      status.value = 'ok'
      sessionStorage.setItem('githubConnected', '1')
      setTimeout(() => {
        if (window.opener) window.opener.postMessage({ type: 'github-connected' }, window.location.origin)
        window.close()
      }, 1000)
    } else {
      status.value = 'error'; errorMsg.value = res.message || '授权失败'
    }
  } catch (e) {
    status.value = 'error'; errorMsg.value = '网络错误'
  }

  // ===== 手动输入模式（回退时取消注释下面代码，并注释上面自动交换代码） =====
  // 步骤同本文件顶部注释
})
</script>
