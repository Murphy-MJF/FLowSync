<template>
  <div>
    <h2 style="margin-bottom:16px">GitHub 仓库</h2>

    <!-- 选择项目 -->
    <div style="margin-bottom:16px;display:flex;gap:12px;align-items:center">
      <el-select v-model="selectedProjectId" placeholder="选择项目" style="width:280px" @change="loadRepo">
        <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
    </div>

    <!-- 授权管理 -->
    <el-card style="margin-bottom:16px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>仓库授权管理</span>
          <el-button size="small" type="primary" @click="openAuthDialog">添加授权</el-button>
        </div>
      </template>
      <el-table :data="authorizedRepos" border size="small" empty-text="暂无授权仓库，点击「添加授权」">
        <el-table-column prop="repoFullName" label="仓库" />
        <el-table-column label="属性" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="success">可读写</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-popconfirm title="取消授权后该仓库将不可在绑定列表中显示" @confirm="handleDeauthorize(row)">
              <template #reference>
                <el-button size="small" type="danger">取消授权</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加授权弹窗 -->
    <el-dialog title="添加仓库授权" v-model="showAuthDialog" width="600px">
      <el-table :data="allRepos" border size="small" max-height="350" v-loading="repoListLoading"
                highlight-current-row @row-click="selectAuthRepo">
        <el-table-column prop="full_name" label="仓库" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="language" label="语言" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="isAuthorized(row.full_name)" size="small" type="success">已授权</el-tag>
            <el-tag v-else size="small" type="info">未授权</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="selectedAuthRepo" style="margin-top:12px;color:#409EFF">
        已选择：{{ selectedAuthRepo.full_name }}
      </div>
      <template #footer>
        <el-button @click="showAuthDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAuthorize" :loading="authSaving" :disabled="!selectedAuthRepo || isAuthorized(selectedAuthRepo.full_name)">
          确认授权
        </el-button>
      </template>
    </el-dialog>

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
              <el-table-column label="操作" width="90">
                <template #default="{ row }">
                  <el-button size="small" @click="openTreeDialog(row.name)">查看源码</el-button>
                </template>
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

    <!-- 新建文件弹窗 -->
    <el-dialog title="新建文件" v-model="newFileVisible" width="500px">
      <el-form :model="newFileForm" label-width="80px">
        <el-form-item label="文件路径">
          <el-input v-model="newFileForm.path" placeholder="例如：src/utils.js" />
        </el-form-item>
        <el-form-item label="文件内容">
          <el-input v-model="newFileForm.content" type="textarea" :rows="10" placeholder="输入文件内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newFileVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateFile" :loading="createFileSaving">创建并上传</el-button>
      </template>
    </el-dialog>

    <!-- 新建文件夹弹窗 -->
    <el-dialog title="新建文件夹" v-model="newFolderVisible" width="400px">
      <el-form :model="newFolderForm" label-width="80px">
        <el-form-item label="文件夹路径">
          <el-input v-model="newFolderForm.path" placeholder="例如：src/components" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newFolderVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateFolder" :loading="createFileSaving">创建</el-button>
      </template>
    </el-dialog>

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

    <!-- 文件树 + 源码查看弹窗 -->
    <el-dialog :title="'源码浏览 — ' + selectedBranch" v-model="treeVisible" width="1000px" top="2vh">
      <div style="margin-bottom:8px;display:flex;gap:6px">
        <el-button size="small" @click="openNewFileDialog">新建文件</el-button>
        <el-button size="small" @click="openNewFolderDialog">新建文件夹</el-button>
      </div>
      <div style="display:flex;height:600px">
        <!-- 文件树 -->
        <div style="width:320px;overflow-x:auto;overflow-y:auto;border-right:1px solid #ebeef5;padding-right:8px;min-width:200px;resize:horizontal">
          <el-tree :data="fileTree" :props="{ label: 'name', children: 'children' }"
                   @node-click="handleFileClick" highlight-current node-key="path"
                   :filter-node-method="filterNode" style="font-size:13px;white-space:nowrap" default-expand-all />
        </div>
        <!-- 源码 -->
        <div style="flex:1;overflow-y:auto;overflow-x:hidden;padding-left:12px;word-break:break-all">
          <div v-if="!selectedFile" style="color:#909399;text-align:center;margin-top:100px">
            点击左侧文件查看源码
          </div>
          <!-- 上传排队提示 -->
          <div v-if="uploadQueue" style="text-align:center;padding:20px;margin:8px 0;background:#fdf6ec;border-radius:4px">
            <p style="color:#E6A23C">⏳ <strong>{{ uploadQueue.currentHolder }}</strong> 正在上传，您排在第 <strong>{{ uploadQueue.queuePosition }}</strong> 位</p>
            <p style="color:#909399;font-size:12px">每 3 秒自动检查，轮到您时将拉取远端最新版本供差分对比</p>
          </div>
          <!-- 上传就绪 — 差分对比 -->
          <div v-if="uploadReady" style="padding:12px;margin:8px 0;background:#f0f9eb;border-radius:4px">
            <p style="color:#67C23A;margin-bottom:8px">✅ 轮到您了！请检查远端最新版本与您的修改：</p>
            <div v-if="diffContent" style="max-height:200px;overflow:auto;margin-bottom:8px">
              <p style="font-size:12px;color:#909399;margin-bottom:4px">远端最新版本（仓库）：</p>
              <pre style="background:#1e1e1e;color:#d4d4d4;padding:8px;border-radius:4px;font-size:12px;line-height:1.4">{{ diffContent }}</pre>
            </div>
            <div style="display:flex;gap:8px">
              <el-button size="small" type="primary" @click="handleConfirmUpload">确认上传</el-button>
              <el-button size="small" @click="cancelUpload">放弃上传</el-button>
            </div>
          </div>
          <div v-else-if="selectedFile">
            <div style="margin-bottom:8px;display:flex;justify-content:space-between;align-items:center">
              <div style="display:flex;align-items:center;gap:12px">
                <strong>{{ selectedFile.path }}</strong>
                <el-tag size="small">{{ selectedFile.size }} bytes</el-tag>
                <el-tag v-if="isEditing" type="warning" size="small">编辑中</el-tag>
              </div>
              <div style="display:flex;gap:6px">
                <template v-if="!isEditing && selectedFile.isText">
                  <el-button size="small" @click="startEdit">编辑</el-button>
                </template>
                <template v-if="isEditing">
                  <el-button size="small" type="primary" @click="saveEdit">应用修改</el-button>
                  <el-button size="small" type="success" @click="handleUpload">上传到仓库</el-button>
                  <el-button size="small" @click="cancelEdit">取消</el-button>
                </template>
              </div>
            </div>
            <pre v-if="selectedFile.isText && !isEditing" style="background:#1e1e1e;color:#d4d4d4;padding:12px;border-radius:4px;overflow:auto;max-height:520px;font-size:13px;line-height:1.5">{{ selectedFile.content }}</pre>
            <el-input v-if="selectedFile.isText && isEditing" type="textarea" v-model="editContent"
                      style="max-height:520px" :rows="20" resize="vertical"
                      :input-style="{ fontFamily:'Consolas,monospace', fontSize:'13px', lineHeight:'1.5', background:'#1e1e1e', color:'#d4d4d4' }" />
            <div v-if="!selectedFile.isText" style="color:#E6A23C;padding:40px;text-align:center">二进制文件，无法预览</div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getProjects, githubProjectStatus, githubRepositories, githubBindRepo, githubUnbindRepo,
         githubBranches, githubCommits, githubIssues, githubPulls, githubTree, githubContents,
         fileLockAcquire, fileLockRelease, fileLockStatus, githubUploadFile,
         githubAuthorizedRepos, githubAuthorizeRepo, githubDeauthorizeRepo } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addToQueue, removeFromQueue } from '../store/uploadQueue'

defineProps({ currentUser: Object })

const projects = ref([])
const selectedProjectId = ref(null)
const repo = ref(null)
const showBindDialog = ref(false)
const ghRepos = ref([])
const selectedRepo = ref(null)
const repoLoading = ref(false)
const binding = ref(false)

// 授权管理
const authorizedRepos = ref([])
const allRepos = ref([])
const selectedAuthRepo = ref(null)
const showAuthDialog = ref(false)
const authSaving = ref(false)
const repoListLoading = ref(false)

// 新建文件/文件夹
const newFileVisible = ref(false)
const newFolderVisible = ref(false)
const createFileSaving = ref(false)
const newFileForm = ref({ path: '', content: '' })
const newFolderForm = ref({ path: '' })

// file tree
const treeVisible = ref(false)
const selectedBranch = ref('')
const fileTree = ref([])
const selectedFile = ref(null)
const isEditing = ref(false)
const editContent = ref('')

const activeTab = ref('branches')
const branches = ref([])
const commits = ref([])
const issues = ref([])
const pulls = ref([])
const tabLoading = ref(false)

onMounted(async () => {
  const res = await getProjects()
  if (res.success) projects.value = res.data || []
  loadAuthorizedRepos()
  // 如果从任务管理跳转过来，自动打开对应分支
  const branchInfo = sessionStorage.getItem('githubOpenBranch')
  if (branchInfo) {
    sessionStorage.removeItem('githubOpenBranch')
    try {
      const info = JSON.parse(branchInfo)
      selectedProjectId.value = info.projectId
      // 等待仓库信息加载
      setTimeout(async () => {
        await loadRepo()
        if (repo.value) {
          openTreeDialog(info.branchName || 'main')
        }
      }, 500)
    } catch {}
  }
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

async function handleUnbind() {
  try {
    await ElMessageBox.confirm('确认解绑仓库？', '提示', { type: 'warning' })
  } catch { return }
  const res = await githubUnbindRepo(selectedProjectId.value)
  if (res.success) {
    ElMessage.success('已解绑')
    repo.value = null
  }
}

// ---- 文件树 ----
function buildTree(treeData) {
  const root = {}
  for (const item of (treeData.tree || [])) {
    const parts = item.path.split('/')
    let current = root
    for (let i = 0; i < parts.length; i++) {
      if (!current[parts[i]]) current[parts[i]] = {}
      current = current[parts[i]]
    }
    current._path = item.path
    current._type = item.type
    current._size = item.size || 0
  }
  const toArray = (obj, prefix) => {
    const result = []
    for (const key of Object.keys(obj)) {
      if (key.startsWith('_')) continue
      const node = { name: key, path: obj[key]._path, type: obj[key]._type, size: obj[key]._size }
      const children = toArray(obj[key], obj[key]._path)
      if (children.length > 0) node.children = children
      else node.isLeaf = true
      result.push(node)
    }
    return result.sort((a, b) => {
      if (a.type === 'tree' && b.type !== 'tree') return -1
      if (a.type !== 'tree' && b.type === 'tree') return 1
      return a.name.localeCompare(b.name)
    })
  }
  return toArray(root, '')
}

async function openTreeDialog(branch) {
  selectedBranch.value = branch
  selectedFile.value = null
  treeVisible.value = true
  try {
    const res = await githubTree(repo.value.owner, repo.value.repoName, branch)
    if (res.success) fileTree.value = buildTree(res.data)
  } catch { fileTree.value = [] }
}

async function handleFileClick(node) {
  if (node.type === 'tree') return
  const isBinary = /\.(png|jpg|jpeg|gif|ico|pdf|zip|gz|tar|exe|dll|so|class|jar|war|mp3|mp4|avi|mov|woff|ttf|eot|otf)$/i.test(node.name)
  if (isBinary) {
    selectedFile.value = { path: node.path, size: node.size, isText: false }
    return
  }
  try {
    const res = await githubContents(repo.value.owner, repo.value.repoName, node.path, selectedBranch.value)
    if (res.success) {
      const content = res.data.content ? decodeBase64(res.data.content) : ''
      selectedFile.value = { path: node.path, size: res.data.size || node.size, isText: true, content, _sha: res.data.sha }
    }
  } catch {
    selectedFile.value = { path: node.path, size: node.size, isText: false }
  }
}

function decodeBase64(base64) {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return new TextDecoder('utf-8').decode(bytes)
}

const lockStatus = ref(null)  // 锁状态提示
let lockPollTimer = null

const uploadQueue = ref(null)   // 上传排队状态
const diffContent = ref('')      // 远端最新版本（用于差分对比）
const uploadReady = ref(false)   // 轮到我了，等待确认

function startEdit() {
  editContent.value = selectedFile.value.content
  isEditing.value = true
}

async function saveEdit() {
  selectedFile.value.content = editContent.value
  selectedFile.value.size = new Blob([editContent.value]).size
  isEditing.value = false
}

function cancelEdit() {
  isEditing.value = false
}

// ---- 上传 GitHub（含排队机制） ----
async function handleUpload() {
  const r = repo.value
  const filePath = selectedFile.value.path
  const res = await fileLockAcquire({
    owner: r.owner, repo: r.repoName, branch: selectedBranch.value, path: filePath
  })
  if (res.success && res.data.acquired) {
    // 直接上传
    uploadReady.value = true
    uploadQueue.value = null
    ElMessage.success('上传通道就绪，请确认上传')
  } else if (res.success) {
    // 进入排队 → 加入全局队列
    const queueItem = {
      owner: r.owner, repo: r.repoName, branch: selectedBranch.value,
      path: filePath, ready: false, currentHolder: res.data.currentHolder
    }
    uploadQueue.value = res.data
    uploadReady.value = false
    currentQueueItem.value = queueItem
    addToQueue(queueItem)
    lockPollTimer = setInterval(async () => {
      const s = await fileLockStatus({
        owner: r.owner, repo: r.repoName, branch: selectedBranch.value, path: filePath
      })
      if (s.success && !s.data.locked) {
        clearInterval(lockPollTimer)
        // 拉取远端最新版本做差分对比
        const cRes = await githubContents(r.owner, r.repoName, filePath, selectedBranch.value)
        if (cRes.success && cRes.data.content) {
          diffContent.value = decodeBase64(cRes.data.content)
        }
        uploadReady.value = true
        uploadQueue.value = null
        ElMessage.success('轮到您了！请检查远端变更后确认上传')
      }
    }, 3000)
  }
}

const currentQueueItem = ref(null)

async function handleConfirmUpload() {
  if (!selectedFile.value || !repo.value) {
    ElMessage.error('文件信息已丢失，请重新选择文件')
    uploadReady.value = false
    return
  }
  const r = repo.value
  const filePath = selectedFile.value.path
  const fileSha = selectedFile.value._sha
  try {
    const body = {
      path: filePath,
      content: btoa(unescape(encodeURIComponent(editContent.value))),
      sha: fileSha || null,
      branch: selectedBranch.value,
      message: '[FlowSync] Update ' + filePath
    }
    const res = await githubUploadFile(r.owner, r.repoName, body)
    if (res.success) {
      ElMessage.success('文件已上传到 GitHub')
    }
  } catch (e) {
    ElMessage.error('上传失败')
  }
  // 释放锁 + 清理状态 + 退出编辑
  await fileLockRelease({
    owner: repo.value.owner, repo: repo.value.repoName,
    branch: selectedBranch.value, path: filePath
  })
  if (currentQueueItem.value) removeFromQueue(currentQueueItem.value)
  if (lockPollTimer) { clearInterval(lockPollTimer); lockPollTimer = null }
  uploadReady.value = false
  uploadQueue.value = null
  diffContent.value = ''
  isEditing.value = false
  currentQueueItem.value = null
  // 刷新树回到文件预览
  openTreeDialog(selectedBranch.value)
}

function cancelUpload() {
  if (lockPollTimer) { clearInterval(lockPollTimer); lockPollTimer = null }
  if (currentQueueItem.value) removeFromQueue(currentQueueItem.value)
  fileLockRelease({
    owner: repo.value.owner, repo: repo.value.repoName,
    branch: selectedBranch.value, path: selectedFile.value.path
  })
  uploadReady.value = false
  uploadQueue.value = null
  diffContent.value = ''
}

// ---- 授权管理 ----
async function loadAuthorizedRepos() {
  const res = await githubAuthorizedRepos()
  if (res.success) authorizedRepos.value = res.data || []
}

function isAuthorized(fullName) {
  return authorizedRepos.value.some(r => r.repoFullName === fullName)
}

function selectAuthRepo(row) {
  if (!isAuthorized(row.full_name)) selectedAuthRepo.value = row
}

async function openAuthDialog() {
  showAuthDialog.value = true
  selectedAuthRepo.value = null
  repoListLoading.value = true
  try {
    const res = await githubRepositories()
    if (res.success) allRepos.value = res.data || []
  } finally { repoListLoading.value = false }
}

async function handleAuthorize() {
  if (!selectedAuthRepo.value) return
  authSaving.value = true
  try {
    const [owner, repo] = selectedAuthRepo.value.full_name.split('/')
    const res = await githubAuthorizeRepo({ owner, repo })
    if (res.success) {
      ElMessage.success('已授权')
      showAuthDialog.value = false
      loadAuthorizedRepos()
    }
  } finally { authSaving.value = false }
}

async function handleDeauthorize(row) {
  const res = await githubDeauthorizeRepo({ fullName: row.repoFullName })
  if (res.success) {
    ElMessage.success('已取消授权')
    loadAuthorizedRepos()
  }
}

// ---- 新建文件/文件夹 ----
function openNewFileDialog() {
  newFileForm.value = { path: '', content: '' }
  newFileVisible.value = true
}

function openNewFolderDialog() {
  newFolderForm.value = { path: '' }
  newFolderVisible.value = true
}

async function handleCreateFile() {
  if (!newFileForm.value.path) return
  createFileSaving.value = true
  try {
    const r = repo.value
    const body = {
      path: newFileForm.value.path,
      content: btoa(unescape(encodeURIComponent(newFileForm.value.content))),
      sha: null,
      branch: selectedBranch.value,
      message: '[FlowSync] Create ' + newFileForm.value.path
    }
    const res = await githubUploadFile(r.owner, r.repoName, body)
    if (res.success) {
      ElMessage.success('文件已创建')
      newFileVisible.value = false
      openTreeDialog(selectedBranch.value)
    }
  } finally { createFileSaving.value = false }
}

async function handleCreateFolder() {
  if (!newFolderForm.value.path) return
  createFileSaving.value = true
  try {
    const r = repo.value
    const body = {
      path: newFolderForm.value.path + '/.gitkeep',
      content: btoa(''),
      sha: null,
      branch: selectedBranch.value,
      message: '[FlowSync] Create folder ' + newFolderForm.value.path
    }
    const res = await githubUploadFile(r.owner, r.repoName, body)
    if (res.success) {
      ElMessage.success('文件夹已创建')
      newFolderVisible.value = false
      openTreeDialog(selectedBranch.value)
    }
  } finally { createFileSaving.value = false }
}

function filterNode(value, data) {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}
</script>
