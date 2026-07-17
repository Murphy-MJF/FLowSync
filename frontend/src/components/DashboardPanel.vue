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

    <!-- 待审核文件（仅项目负责人和管理员可见） -->
    <el-card style="margin-top:20px" v-if="isReviewer && pendingReviews.length > 0">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>待审核文件（{{ pendingReviews.length }}）</span>
          <el-button size="small" @click="loadPendingReviews">刷新</el-button>
        </div>
      </template>
      <el-table :data="pendingReviews" border size="small" v-loading="prLoading">
        <el-table-column prop="submitterName" label="提交人" width="100" />
        <el-table-column prop="filePath" label="文件路径" show-overflow-tooltip />
        <el-table-column prop="branch" label="分支" width="120" />
        <el-table-column prop="createTime" label="提交时间" width="160" />
        <el-table-column label="操作" width="210">
          <template #default="{ row }">
            <el-button size="small" @click="viewCacheContent(row)">查看</el-button>
            <el-button size="small" type="success" @click="handleApproveCache(row)">批准</el-button>
            <el-button size="small" type="danger" @click="handleRejectCache(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 文件内容预览弹窗 -->
    <el-dialog :title="'审核文件 — ' + previewFile.path" v-model="previewVisible" width="800px" top="2vh">
      <div style="font-size:12px;color:#909399;margin-bottom:8px">
        提交人：{{ previewFile.submitterName }} | 分支：{{ previewFile.branch }} | 时间：{{ previewFile.createTime }}
      </div>
      <pre style="background:#1e1e1e;color:#d4d4d4;padding:12px;border-radius:4px;overflow:auto;max-height:500px;font-size:13px;line-height:1.5">{{ previewFile.decoded }}</pre>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button type="success" @click="previewVisible = false; handleApproveCache(previewFile)">批准</el-button>
        <el-button type="danger" @click="previewVisible = false; handleRejectCache(previewFile)">拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOverview, getProjects, githubProjectCommits, getFileCache, approveFileCache, rejectFileCache } from '../api'
import { pendingUploads } from '../store/uploadQueue'
import { ElMessage } from 'element-plus'

const stats = ref([
  { label: '系统用户', value: 0 },
  { label: '项目总数', value: 0 },
  { label: '任务总数', value: 0 },
  { label: '总结总数', value: 0 }
])

const props = defineProps({ currentUser: Object })
const isReviewer = computed(() => props.currentUser?.role === '负责人' || props.currentUser?.role === '管理员')
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

onMounted(loadInitialData)

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
// 待审核文件
const pendingReviews = ref([])
const prLoading = ref(false)
const previewVisible = ref(false)
const previewFile = ref({})

async function loadPendingReviews() {
  prLoading.value = true
  try {
    const all = []
    for (const p of projects.value) {
      const res = await getFileCache(p.id)
      if (res.success) {
        for (const item of (res.data || [])) {
          if (item.status === 'pending') all.push({ ...item, projectName: p.name })
        }
      }
    }
    pendingReviews.value = all
  } finally { prLoading.value = false }
}

async function handleApproveCache(row) {
  const res = await approveFileCache(row.id)
  if (res.success) {
    ElMessage.success('已批准并上传到 GitHub')
    loadPendingReviews()
  }
}

function viewCacheContent(row) {
  try {
    previewFile.value = {
      ...row,
      decoded: decodeURIComponent(escape(atob(row.content)))
    }
  } catch {
    previewFile.value = { ...row, decoded: '[解码失败]' }
  }
  previewVisible.value = true
}

async function handleRejectCache(row) {
  const res = await rejectFileCache(row.id)
  if (res.success) {
    ElMessage.success('已拒绝')
    loadPendingReviews()
  }
}

function jumpToUpload(item) {
  window.dispatchEvent(new CustomEvent('nav-github-tree', { detail: item }))
}

// 加载时也获取待审核列表
async function loadInitialData() {
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
    loadPendingReviews()
  } catch (e) { /* ignore */ }
}
</script>
