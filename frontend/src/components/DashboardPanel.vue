<template>
  <div>
    <h2 style="margin-bottom:20px">控制台</h2>
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in stats" :key="item.label">
        <el-card shadow="hover">
          <div style="text-align:center">
            <div style="font-size:14px;color:#909399">{{ item.label }}</div>
            <div style="font-size:36px;font-weight:bold;color:#409EFF;margin-top:8px">{{ item.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 上传排队状态 -->
    <el-card v-if="queueItems.length > 0" style="margin-top:20px">
      <template #header>
        <span>文件上传队列</span>
      </template>
      <div v-for="item in queueItems" :key="item.path"
           style="display:flex;align-items:center;justify-content:space-between;padding:10px;margin-bottom:8px;border-radius:4px"
           :style="{ background: item.ready ? '#f0f9eb' : '#fdf6ec' }">
        <div>
          <div style="font-weight:bold">{{ item.owner }}/{{ item.repo }} — {{ item.path }}</div>
          <div style="font-size:12px;color:#909399;margin-top:4px">
            <template v-if="item.ready">
              <span style="color:#67C23A">✅ 轮到您了！请检查远端最新版本后确认上传</span>
            </template>
            <template v-else>
              ⏳ {{ item.currentHolder || '未知' }} 正在上传，等待中...
            </template>
          </div>
        </div>
        <el-button v-if="item.ready" type="primary" size="small" @click="jumpToUpload(item)">
          去确认上传
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOverview } from '../api'
import { pendingUploads } from '../store/uploadQueue'

const stats = ref([
  { label: '系统用户', value: 0 },
  { label: '项目总数', value: 0 },
  { label: '任务总数', value: 0 },
  { label: '总结总数', value: 0 }
])

const queueItems = pendingUploads

onMounted(async () => {
  try {
    const res = await getOverview()
    if (res.success) {
      stats.value[0].value = res.data.userCount || 0
      stats.value[1].value = res.data.projectCount || 0
      stats.value[2].value = res.data.taskCount || 0
      stats.value[3].value = res.data.summaryCount || 0
    }
  } catch (e) { /* ignore */ }
})

function jumpToUpload(item) {
  window.dispatchEvent(new CustomEvent('nav-github-tree', { detail: item }))
}
</script>
