import { ref } from 'vue'

// 全局上传排队状态（跨组件共享）
export const pendingUploads = ref([])

export function addToQueue(item) {
  if (!pendingUploads.value.find(x => x.path === item.path && x.repo === item.repo)) {
    pendingUploads.value.push(item)
    item._timer = setInterval(async () => {
      const { fileLockStatus } = await import('../api')
      const s = await fileLockStatus({
        owner: item.owner, repo: item.repo, branch: item.branch, path: item.path
      })
      if (s.success && !s.data.locked) {
        item.ready = true
        item.queuePosition = 0
        clearInterval(item._timer)
      } else if (s.success) {
        item.currentHolder = s.data.currentUserName
        item.queuePosition = item._timer ? '等待中...' : s.data.queueLength
      }
    }, 3000)
  }
}

export function removeFromQueue(item) {
  if (item._timer) clearInterval(item._timer)
  const idx = pendingUploads.value.indexOf(item)
  if (idx >= 0) pendingUploads.value.splice(idx, 1)
}

export function clearQueue() {
  for (const item of pendingUploads.value) {
    if (item._timer) clearInterval(item._timer)
  }
  pendingUploads.value = []
}
