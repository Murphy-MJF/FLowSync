<template>
  <div>
    <h2 style="margin-bottom:16px">GitHub 仓库</h2>

    <!-- 选择项目 -->
    <div style="margin-bottom:16px;display:flex;gap:12px;align-items:center">
      <el-select v-model="selectedProjectId" placeholder="选择项目" style="width:280px" @change="loadRepo">
        <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
    </div>

    <!-- 仓库信息 -->
    <el-card v-if="!repo" style="margin-bottom:16px">
      <p style="color:#909399">未绑定 GitHub 仓库。选择一个项目后，可为其绑定仓库。</p>
      <el-button v-if="selectedProjectId" type="primary" size="small" style="margin-top:8px" @click="openBindDialog">绑定仓库</el-button>
    </el-card>

    <template v-if="repo">
      <el-card style="margin-bottom:16px">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>仓库信息</span>
            <el-button size="small" type="danger" @click="handleUnbind">解绑</el-button>
          </div>
        </template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="Owner">{{ repo.owner }}</el-descriptions-item>
          <el-descriptions-item label="仓库">{{ repo.repoName }}</el-descriptions-item>
          <el-descriptions-item label="默认分支">{{ repo.defaultBranch }}</el-descriptions-item>
          <el-descriptions-item label="链接">
            <a :href="repo.repoUrl" target="_blank">{{ repo.repoUrl }}</a>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Tabs: Branches / Commits / Issues / PRs -->
      <el-card>
        <el-tabs v-model="activeTab" @tab-change="loadTabData">
          <el-tab-pane label="分支" name="branches">
            <el-table :data="branches" border size="small" v-loading="tabLoading">
              <el-table-column prop="name" label="分支名" />
              <el-table-column label="最后提交" show-overflow-tooltip>
                <template #default="{ row }">{{ (row.commit && row.commit.sha || '').substring(0,7) }} - {{ row.commit && row.commit.commit ? row.commit.commit.message : '' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="提交" name="commits">
            <el-table :data="commits" border size="small" v-loading="tabLoading" max-height="400">
              <el-table-column label="SHA" width="80">
                <template #default="{ row }">{{ (row.sha || '').substring(0,7) }}</template>
              </el-table-column>
              <el-table-column label="提交信息" show-overflow-tooltip>
                <template #default="{ row }">{{ row.commit ? row.commit.message : '' }}</template>
              </el-table-column>
              <el-table-column label="作者" width="120">
                <template #default="{ row }">{{ row.commit && row.commit.author ? row.commit.author.name : '' }}</template>
              </el-table-column>
              <el-table-column label="时间" width="170">
                <template #default="{ row }">{{ row.commit && row.commit.author ? row.commit.author.date : '' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="Issues" name="issues">
            <el-table :data="issues" border size="small" v-loading="tabLoading">
              <el-table-column prop="number" label="#" width="60" />
              <el-table-column prop="title" label="标题" show-overflow-tooltip />
              <el-table-column prop="state" label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.state==='open'?'success':'info'" size="small">{{ row.state }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="创建者" width="100">
                <template #default="{ row }">{{ row.user ? row.user.login : '' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="Pull Requests" name="pulls">
            <el-table :data="pulls" border size="small" v-loading="tabLoading">
              <el-table-column prop="number" label="#" width="60" />
              <el-table-column prop="title" label="标题" show-overflow-tooltip />
              <el-table-column prop="state" label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.state==='open'?'success':'info'" size="small">{{ row.state }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="作者" width="100">
                <template #default="{ row }">{{ row.user ? row.user.login : '' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>

    <!-- 绑定仓库弹窗 -->
    <el-dialog title="绑定 GitHub 仓库" v-model="showBindDialog" width="500px">
      <el-table :data="ghRepos" border size="small" max-height="350" v-loading="repoLoading"
                highlight-current-row @row-click="selectRepo">
        <el-table-column prop="full_name" label="仓库" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="language" label="语言" width="80" />
      </el-table>
      <div v-if="selectedRepo" style="margin-top:12px;color:#409EFF">
        已选择：{{ selectedRepo.full_name }}
      </div>
      <template #footer>
        <el-button @click="showBindDialog = false">取消</el-button>
        <el-button type="primary" @click="handleBindRepo" :loading="binding" :disabled="!selectedRepo">确认绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getProjects, githubProjectStatus, githubRepositories, githubBindRepo,
         githubBranches, githubCommits, githubIssues, githubPulls } from '../api'
import { ElMessage } from 'element-plus'

defineProps({ currentUser: Object })

const projects = ref([])
const selectedProjectId = ref(null)
const repo = ref(null)
const showBindDialog = ref(false)
const ghRepos = ref([])
const selectedRepo = ref(null)
const repoLoading = ref(false)
const binding = ref(false)

const activeTab = ref('branches')
const branches = ref([])
const commits = ref([])
const issues = ref([])
const pulls = ref([])
const tabLoading = ref(false)

onMounted(async () => {
  const res = await getProjects()
  if (res.success) projects.value = res.data || []
})

async function loadRepo() {
  repo.value = null
  if (!selectedProjectId.value) return
  const res = await githubProjectStatus(selectedProjectId.value)
  if (res.success) {
    repo.value = res.data
    loadTabData()
  }
}

async function loadTabData() {
  if (!repo.value) return
  tabLoading.value = true
  const { owner, repoName, defaultBranch } = repo.value
  try {
    switch (activeTab.value) {
      case 'branches': {
        const r = await githubBranches(owner, repoName)
        if (r.success) branches.value = r.data || []
        break
      }
      case 'commits': {
        const r = await githubCommits(owner, repoName, defaultBranch)
        if (r.success) commits.value = r.data || []
        break
      }
      case 'issues': {
        const r = await githubIssues(owner, repoName)
        if (r.success) issues.value = r.data || []
        break
      }
      case 'pulls': {
        const r = await githubPulls(owner, repoName)
        if (r.success) pulls.value = r.data || []
        break
      }
    }
  } finally { tabLoading.value = false }
}

function selectRepo(row) { selectedRepo.value = row }

async function openBindDialog() {
  showBindDialog.value = true
  repoLoading.value = true
  try {
    const res = await githubRepositories()
    if (res.success) ghRepos.value = res.data || []
  } finally { repoLoading.value = false }
}

async function handleBindRepo() {
  binding.value = true
  try {
    const [owner, name] = selectedRepo.value.full_name.split('/')
    const res = await githubBindRepo(selectedProjectId.value, owner, name)
    if (res.success) {
      ElMessage.success('仓库已绑定')
      showBindDialog.value = false
      loadRepo()
    }
  } finally { binding.value = false }
}
</script>
