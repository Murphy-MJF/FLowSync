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

    <!-- 上传排队状态，仅当队列有数据时显示 -->
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

    <!-- 文件修改记录 -->
    <el-card style="margin-top:20px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>文件修改记录</span>
          <div style="display:flex;gap:8px">
            <el-select v-model="selectedProject" placeholder="选择项目" size="small" style="width:200px" @change="loadCommits" clearable>
              <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
            <el-select v-model="filterUser" placeholder="筛选成员" size="small" style="width:150px" @change="loadCommits" clearable>
              <el-option v-for="u in uniqueUsers" :key="u" :label="u" :value="u" />
            </el-select>
          </div>
        </div>
      </template>
      <div v-if="!selectedProject" style="text-align:center;padding:20px;color:#909399">选择项目查看 Git 提交历史</div>
      <div v-else-if="commits.length === 0" style="text-align:center;padding:20px;color:#909399" v-loading="commitLoading">暂无提交记录或未绑定仓库</div>
      <el-timeline v-else>
        <el-timeline-item v-for="c in commits" :key="c.sha" :timestamp="c.date" placement="top"
                          :color="c.flowSyncUser ? '#409EFF' : '#909399'">
          <div>{{ c.message }}</div>
          <div style="font-size:12px;color:#909399;margin-top:4px">
            SHA: <code>{{ c.sha }}</code>
            <template v-if="c.githubLogin"> | <strong>{{ c.githubLogin }}</strong></template>
            <template v-if="c.flowSyncUser"> | <el-tag size="small" type="success">{{ c.flowSyncUser }}</el-tag></template>
            <template v-if="c.author && !c.githubLogin"> | {{ c.author }}</template>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOverview, getProjects, githubProjectCommits } from '../api'
import { pendingUploads } from '../store/uploadQueue'

const stats = ref([
  { label: '系统用户', value: 0 },
  { label: '项目总数', value: 0 },
  { label: '任务总数', value: 0 },
  { label: '总结总数', value: 0 }
])

const queueItems = pendingUploads
const projects = ref([])
const selectedProject = ref(null)
const commits = ref([])
const allCommits = ref([])
const filterUser = ref('')
const commitLoading = ref(false)

// 从所有提交记录中提取不重复的用户列表
const uniqueUsers = computed(() => {
  const names = new Set()
  for (const c of allCommits.value) {
    if (c.flowSyncUser) names.add(c.flowSyncUser)
  }
  return [...names].sort()
})

onMounted(async () => {
  try {
    const res = await getOverview()
    if (res.success) {
      stats.value[0].value = res.data.userCount || 0
      stats.value[1].value = res.data.projectCount || 0
      stats.value[2].value = res.data.taskCount || 0
      stats.value[3].value = res.data.summaryCount || 0
    }
    const pRes = await getProjects()
    if (pRes.success) projects.value = pRes.data || []
  } catch (e) { /* ignore */ }
})

// 加载选中项目的 Git 提交记录
async function loadCommits() {
  if (!selectedProject.value) { allCommits.value = []; commits.value = []; return }
  commitLoading.value = true
  try {
    const res = await githubProjectCommits(selectedProject.value)
    allCommits.value = res.success ? (res.data || []) : []
    applyFilter()
  } finally { commitLoading.value = false }
}

// 根据筛选条件过滤提交记录
function applyFilter() {
  commits.value = filterUser.value
    ? allCommits.value.filter(c => c.flowSyncUser === filterUser.value)
    : allCommits.value
}

// 跳转到上传确认页面
function jumpToUpload(item) {
  window.dispatchEvent(new CustomEvent('nav-github-tree', { detail: item }))
}
</script>
