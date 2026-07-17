<!-- 模板区域开始 -->
<template>
  <!-- 根容器元素 -->
  <div>
    <!-- 顶部操作栏：flex布局，两端对齐，垂直居中，下方留白16px -->
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <!-- 左侧区域：flex布局，垂直居中，子元素间距12px -->
      <div style="display:flex;align-items:center;gap:12px">
        <!-- 页面标题："项目管理" -->
        <h2>项目管理</h2>
        <!-- 仅在"负责人"或"管理员"身份且已勾选项目时显示批量删除按钮，危险样式，点击触发批量删除，加载中状态绑定 -->
        <el-button v-if="(isLeader || isAdmin) && selectedIds.length > 0"
                   type="danger" @click="handleBatchDelete" :loading="batchDeleting">
          <!-- 按钮文字显示已选项目数量 -->
          批量删除（{{ selectedIds.length }}）
        </el-button>
      </div>
      <!-- 右侧新建项目按钮：仅负责人或管理员可见，主色调样式，点击打开新建弹窗 -->
      <el-button v-if="isLeader || isAdmin" type="primary" @click="openDialog(null)">新建项目</el-button>
    </div>

    <!-- 项目表格：数据源projects，带边框和斑马纹，加载状态绑定，多选变化时触发handleSelectionChange -->
    <el-table :data="projects" border stripe v-loading="loading"
              @selection-change="handleSelectionChange">
      <!-- 多选列，宽度45px -->
      <el-table-column type="selection" width="45" />
      <!-- ID列，字段id，宽度60px -->
      <el-table-column prop="id" label="ID" width="60" />
      <!-- 项目名称列，字段name -->
      <el-table-column prop="name" label="项目名称" />
      <!-- 项目说明列，字段description，溢出时显示tooltip -->
      <el-table-column prop="description" label="项目说明" show-overflow-tooltip />
      <!-- 状态列，字段status，宽度100px -->
      <el-table-column prop="status" label="状态" width="100">
        <!-- 自定义单元格模板，解构获取当前行数据row -->
        <template #default="{ row }">
          <!-- 状态标签，根据状态值动态设置标签颜色类型 -->
          <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <!-- 优先级列，字段priority，宽度80px -->
      <el-table-column prop="priority" label="优先级" width="80" />
      <!-- 负责人列，字段ownerName，宽度100px -->
      <el-table-column prop="ownerName" label="负责人" width="100" />
      <!-- 开始日期列，字段startDate，宽度110px -->
      <el-table-column prop="startDate" label="开始" width="110" />
      <!-- 结束日期列，字段endDate，宽度110px -->
      <el-table-column prop="endDate" label="结束" width="110" />
      <!-- GitHub仓库列，宽度130px -->
      <el-table-column label="GitHub" width="130">
        <!-- 自定义单元格模板，解构获取当前行数据row -->
        <template #default="{ row }">
          <!-- 若该项目的仓库状态已存在，则显示仓库信息 -->
          <template v-if="repoStatus[row.id]">
            <!-- 仓库名标签：已归档显示info灰色，否则success绿色，右侧留白4px -->
            <el-tag size="small" :type="archived[row.id] ? 'info' : 'success'" style="margin-right:4px">{{ repoStatus[row.id].repoName }}</el-tag>
            <!-- 项目已完成且未归档时显示"归档"按钮 -->
            <el-button v-if="row.status === '已完成' && !archived[row.id]" size="small" @click="handleArchive(row)">归档</el-button>
            <!-- 若已归档则显示"已归档"标签 -->
            <el-tag v-if="archived[row.id]" size="small" type="info">已归档</el-tag>
          </template>
          <!-- 仓库状态不存在时的处理 -->
          <template v-else>
            <!-- 负责人或管理员显示下拉菜单，选中项触发handleGithubAction -->
            <el-dropdown v-if="isLeader || isAdmin" @command="(cmd) => handleGithubAction(row, cmd)">
              <!-- 下拉菜单触发器按钮 -->
              <el-button size="small">仓库 ▾</el-button>
              <!-- 下拉菜单面板模板 -->
              <template #dropdown>
                <!-- 下拉菜单列表 -->
                <el-dropdown-menu>
                  <!-- 菜单项：新建仓库，command值为"create" -->
                  <el-dropdown-item command="create">新建仓库</el-dropdown-item>
                  <!-- 菜单项：绑定已有仓库，command值为"bind" -->
                  <el-dropdown-item command="bind">绑定已有</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <!-- 非负责人/管理员显示灰色横线占位 -->
            <span v-else style="color:#c0c4cc">—</span>
          </template>
        </template>
      </el-table-column>
      <!-- 操作列：仅负责人或管理员可见，宽度220px -->
      <el-table-column v-if="isLeader || isAdmin" label="操作" width="220">
        <!-- 自定义单元格模板，解构获取当前行数据row -->
        <template #default="{ row }">
          <!-- "编辑"按钮，点击打开编辑弹窗并传入当前行数据 -->
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <!-- "改负责人"按钮：仅管理员可见，主色调样式 -->
          <el-button v-if="isAdmin" size="small" type="primary" @click="openOwnerDialog(row)">改负责人</el-button>
          <!-- 删除确认气泡：管理员或非"已完成"状态显示，确认后调用handleDelete -->
          <el-popconfirm v-if="isAdmin || row.status !== '已完成'" title="确认删除此项目？" @confirm="handleDelete(row.id)">
            <!-- 触发器插槽 -->
            <template #reference>
              <!-- 危险样式的"删除"按钮 -->
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑弹窗：标题根据form.id动态切换，双向绑定显示状态，宽度500px -->
    <el-dialog :title="form.id ? '编辑项目' : '新建项目'" v-model="dialogVisible" width="500px">
      <!-- 表单：数据绑定form，标签宽度80px -->
      <el-form :model="form" label-width="80px">
        <!-- 表单项：项目名称 -->
        <el-form-item label="项目名称">
          <!-- 输入框，双向绑定form.name -->
          <el-input v-model="form.name" />
        </el-form-item>
        <!-- 表单项：项目说明 -->
        <el-form-item label="项目说明">
          <!-- 文本域输入框，双向绑定form.description，3行高度 -->
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <!-- 表单项：项目状态 -->
        <el-form-item label="项目状态">
          <!-- 下拉选择器，双向绑定form.status，宽度100% -->
          <el-select v-model="form.status" style="width:100%">
            <!-- 遍历statuses数组生成选项，key/label/value均使用s -->
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <!-- 表单项：优先级 -->
        <el-form-item label="优先级">
          <!-- 下拉选择器，双向绑定form.priority，宽度100% -->
          <el-select v-model="form.priority" style="width:100%">
            <!-- 遍历priorities数组生成选项，key/label/value均使用p -->
            <el-option v-for="p in priorities" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <!-- 表单项：开始日期 -->
        <el-form-item label="开始日期">
          <!-- 日期选择器，类型date，双向绑定form.startDate，格式YYYY-MM-DD -->
          <el-date-picker v-model="form.startDate" type="date" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <!-- 表单项：结束日期 -->
        <el-form-item label="结束日期">
          <!-- 日期选择器，类型date，双向绑定form.endDate，格式YYYY-MM-DD -->
          <el-date-picker v-model="form.endDate" type="date" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <!-- 弹窗底部插槽 -->
      <template #footer>
        <!-- "取消"按钮，点击关闭弹窗 -->
        <el-button @click="dialogVisible = false">取消</el-button>
        <!-- "保存"按钮，主色调，点击触发handleSave -->
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 管理员更改项目负责人弹窗：宽度400px -->
    <el-dialog title="更改项目负责人" v-model="ownerDialogVisible" width="400px">
      <!-- 表单：数据绑定ownerForm，标签宽度80px -->
      <el-form :model="ownerForm" label-width="80px">
        <!-- 表单项：项目名称（只读显示） -->
        <el-form-item label="项目">
          <!-- 输入框，单向绑定显示项目名，禁用编辑 -->
          <el-input :model-value="ownerForm.name" disabled />
        </el-form-item>
        <!-- 表单项：当前负责人（只读显示） -->
        <el-form-item label="当前负责人">
          <!-- 输入框，单向绑定显示当前负责人，禁用编辑 -->
          <el-input :model-value="ownerForm.oldOwner" disabled />
        </el-form-item>
        <!-- 表单项：新负责人选择 -->
        <el-form-item label="新负责人">
          <!-- 下拉选择器，双向绑定新负责人ID，宽度100% -->
          <el-select v-model="ownerForm.newOwnerId" style="width:100%">
            <!-- 遍历可选负责人列表，key为u.id，选项标签显示"姓名 (角色)"，值为用户ID -->
            <el-option v-for="u in ownerCandidates" :key="u.id"
                       :label="u.realName + ' (' + u.role + ')'" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <!-- 弹窗底部插槽 -->
      <template #footer>
        <!-- "取消"按钮，点击关闭弹窗 -->
        <el-button @click="ownerDialogVisible = false">取消</el-button>
        <!-- "确认更改"按钮，主色调，带加载状态 -->
        <el-button type="primary" @click="handleOwnerSave" :loading="ownerSaving">确认更改</el-button>
      </template>
    </el-dialog>

    <!-- 绑定仓库弹窗（选择列表）：宽度550px -->
    <el-dialog title="绑定 GitHub 仓库" v-model="showBindDialog" width="550px">
      <!-- 仓库列表表格：小尺寸，最大高度350px，带加载状态，高亮当前行，点击行触发selectBindRepo -->
      <el-table :data="bindRepos" border size="small" max-height="350" v-loading="bindRepoLoading"
                highlight-current-row @row-click="selectBindRepo">
        <!-- 仓库全名列，字段full_name -->
        <el-table-column prop="full_name" label="仓库" />
        <!-- 描述列，字段description，溢出显示tooltip -->
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <!-- 语言列，字段language，宽度80px -->
        <el-table-column prop="language" label="语言" width="80" />
      </el-table>
      <!-- 已选择仓库提示区，上边距12px，蓝色文字 -->
      <div v-if="selectedBindRepo" style="margin-top:12px;color:#409EFF">
        <!-- 显示已选仓库的全名 -->
        已选择：{{ selectedBindRepo.full_name }}
      </div>
      <!-- 弹窗底部插槽 -->
      <template #footer>
        <!-- "取消"按钮，点击关闭弹窗 -->
        <el-button @click="showBindDialog = false">取消</el-button>
        <!-- "确认绑定"按钮，主色调，带加载状态，未选择时禁用 -->
        <el-button type="primary" @click="confirmBindRepo" :loading="bindRepoLoading" :disabled="!selectedBindRepo">确认绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<!-- 脚本区域开始（使用setup语法糖） -->
<script setup>
// 从Vue导入ref、reactive、computed、onMounted
import { ref, reactive, computed, onMounted } from 'vue'
// 从API模块导入所有需要的接口方法
import { getProjects, saveProject, deleteProject, batchDeleteProjects, getUsers, githubCreateRepo, githubBindRepo, githubOwnerRepositories, githubProjectStatus, archiveProject } from '../api'
// 从Element Plus导入消息提示和消息弹框
import { ElMessage, ElMessageBox } from 'element-plus'

// 定义组件props：currentUser，类型为Object
const props = defineProps({ currentUser: Object })
// 计算属性：根据currentUser的role判断是否为"负责人"
const isLeader = computed(() => props.currentUser?.role === '负责人')
// 计算属性：根据currentUser的role判断是否为"管理员"
const isAdmin = computed(() => props.currentUser?.role === '管理员')

// 响应式引用：项目列表，初始为空数组
const projects = ref([])
// 响应式引用：表格加载状态，初始false
const loading = ref(false)
// 响应式引用：批量删除加载状态，初始false
const batchDeleting = ref(false)
// 响应式引用：已选项目ID数组，初始为空数组
const selectedIds = ref([])
// 响应式引用：新建/编辑弹窗显示状态，初始false
const dialogVisible = ref(false)
// 响应式引用：更改负责人弹窗显示状态，初始false
const ownerDialogVisible = ref(false)
// 响应式引用：更改负责人保存中的加载状态，初始false
const ownerSaving = ref(false)
// 响应式引用：更改负责人表单数据，初始为空对象
const ownerForm = ref({})
// 响应式引用：可选的新负责人列表，初始为空数组
const ownerCandidates = ref([])
// 响应式引用：项目仓库状态映射表（key为项目ID），初始为空对象
const repoStatus = ref({})
// 响应式对象：项目归档状态映射表（key为项目ID）
const archived = reactive({})
// 以下为绑定弹窗相关状态
// 响应式引用：绑定仓库弹窗显示状态，初始false
const showBindDialog = ref(false)
// 响应式引用：可绑定的仓库列表，初始为空数组
const bindRepos = ref([])
// 响应式引用：当前选中的绑定仓库，初始null
const selectedBindRepo = ref(null)
// 响应式引用：绑定仓库加载状态，初始false
const bindRepoLoading = ref(false)
// 响应式引用：当前要绑定的项目ID，初始null
const bindProjectId = ref(null)

// 选中绑定仓库：将点击的行数据赋值给selectedBindRepo
function selectBindRepo(row) { selectedBindRepo.value = row }

// 确认绑定仓库的异步方法
async function confirmBindRepo() {
  // 若未选中仓库则直接返回
  if (!selectedBindRepo.value) return
  // 开启绑定加载状态
  bindRepoLoading.value = true
  // 开启try块
  try {
    // 从仓库全名中分割出owner（所有者）和repo（仓库名）
    const [owner, repo] = selectedBindRepo.value.full_name.split('/')
    // 调用API绑定仓库到指定项目
    const res = await githubBindRepo(bindProjectId.value, owner, repo)
    // 若绑定成功
    if (res.success) {
      // 弹出成功提示"已绑定"
      ElMessage.success('已绑定')
      // 更新项目仓库状态映射表
      repoStatus.value[bindProjectId.value] = { owner, repoName: repo }
      // 关闭绑定弹窗
      showBindDialog.value = false
    }
  } finally { bindRepoLoading.value = false } // finally块：关闭绑定加载状态
}
// 响应式引用：新建/编辑表单数据，初始为空对象
const form = ref({})
// 项目状态选项数组
const statuses = ['未开始', '进行中', '已完成']
// 优先级选项数组
const priorities = ['低', '中', '高']

// 生命周期钩子：组件挂载后调用fetchProjects获取项目列表
onMounted(fetchProjects)

// 获取项目列表的异步方法
async function fetchProjects() {
  // 开启表格加载状态
  loading.value = true
  // 开启try块
  try {
    // 调用API获取所有项目
    const res = await getProjects()
    // 若请求成功
    if (res.success) {
      // 将返回数据赋值给projects（若数据为空则用空数组）
      projects.value = res.data || []
      // 加载各项目的GitHub仓库状态
      loadRepoStatuses()
    }
  } finally { loading.value = false } // finally块：关闭表格加载状态
}

// 加载所有项目GitHub仓库状态的异步方法
async function loadRepoStatuses() {
  // 遍历projects列表中的每个项目p
  for (const p of projects.value) {
    // 开启try块
    try {
      // 调用API获取项目p的GitHub仓库状态
      const r = await githubProjectStatus(p.id)
      // 若成功且有数据，存入repoStatus映射表
      if (r.success && r.data) repoStatus.value[p.id] = r.data
    } catch {} // 静默捕获异常（单个项目加载失败不影响其他）
  }
}

// 处理GitHub操作的异步方法：row为当前行数据，cmd为操作命令
async function handleGithubAction(row, cmd) {
  // 若操作为"create"（新建仓库）
  if (cmd === 'create') {
    // 开启try块
    try {
      // 弹出输入框：标题"新建 GitHub 仓库"，提示"仓库名称"，默认输入值为项目名称小写化并将空格替换为连字符
      const { value } = await ElMessageBox.prompt('仓库名称', '新建 GitHub 仓库', {
        inputValue: row.name.toLowerCase().replace(/\s+/g, '-')
      })
      // 若用户输入了仓库名
      if (value) {
        // 调用API创建GitHub仓库（公开仓库）
        const res = await githubCreateRepo(row.id, { name: value, description: row.description || '', private: false })
        // 若创建成功
        if (res.success) {
          // 弹出成功提示"仓库已创建并绑定"
          ElMessage.success('仓库已创建并绑定')
          // 更新该项目在repoStatus中的仓库信息
          repoStatus.value[row.id] = res.data
        }
      }
    } catch {} // 静默捕获异常（用户取消等）
  } else if (cmd === 'bind') { // 若操作为"bind"（绑定已有仓库）
    // 记录当前项目ID到bindProjectId
    bindProjectId.value = row.id
    // 清空可绑定仓库列表
    bindRepos.value = []
    // 清空已选中的仓库
    selectedBindRepo.value = null
    // 开启绑定仓库加载状态
    bindRepoLoading.value = true
    // 显示绑定仓库弹窗
    showBindDialog.value = true
    // 开启try块
    try {
      // 调用API获取该项目所有者可绑定的仓库列表
      const reposRes = await githubOwnerRepositories(row.id)
      // 若成功则赋值给bindRepos（空数据则用空数组）
      if (reposRes.success) bindRepos.value = reposRes.data || []
    } finally { bindRepoLoading.value = false } // finally块：关闭绑定仓库加载状态
  }
}

// 打开新建/编辑弹窗的方法
function openDialog(row) {
  // 若传入row则拷贝行数据用于编辑，否则初始化空表单（默认状态"未开始"、优先级"中"）
  form.value = row ? { ...row } : { name: '', description: '', status: '未开始', priority: '中', startDate: '', endDate: '' }
  // 显示弹窗
  dialogVisible.value = true
}

// 保存项目（新建或编辑）的异步方法
async function handleSave() {
  // 调用API保存项目表单数据
  const res = await saveProject(form.value)
  // 若保存成功
  if (res.success) {
    // 弹出成功提示"保存成功"
    ElMessage.success('保存成功')
    // 关闭弹窗
    dialogVisible.value = false
    // 重新获取项目列表以刷新表格
    fetchProjects()
  }
}

// 表格多选变化的事件处理
function handleSelectionChange(rows) {
  // 将选中的行数组映射为ID数组并赋值给selectedIds
  selectedIds.value = rows.map(r => r.id)
}

// 批量删除项目的异步方法
async function handleBatchDelete() {
  // 开启try块
  try {
    // 弹出警告确认框，提示删除数量且不可恢复
    await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 个项目？此操作不可恢复。`, '批量删除', { type: 'warning' })
  } catch { return } // 用户取消则直接返回
  // 开启批量删除加载状态
  batchDeleting.value = true
  // 开启try块
  try {
    // 调用API批量删除选中的项目
    const res = await batchDeleteProjects(selectedIds.value)
    // 若删除成功
    if (res.success) {
      // 弹出成功提示（使用接口返回的消息）
      ElMessage.success(res.message)
      // 清空已选项目ID列表
      selectedIds.value = []
      // 重新获取项目列表以刷新表格
      fetchProjects()
    }
  } finally { batchDeleting.value = false } // finally块：关闭批量删除加载状态
}

// 打开更改负责人弹窗的异步方法
async function openOwnerDialog(row) {
  // 调用API获取所有用户列表
  const res = await getUsers()
  // 若获取成功
  if (res.success) {
    // 过滤掉管理员和当前项目负责人，作为备选新负责人
    ownerCandidates.value = (res.data || []).filter(u => u.role !== '管理员' && u.id !== row.ownerId)
  }
  // 初始化负责人表单：项目ID、名称、当前负责人、新负责人ID（空）
  ownerForm.value = { id: row.id, name: row.name, oldOwner: row.ownerName, newOwnerId: null }
  // 显示更改负责人弹窗
  ownerDialogVisible.value = true
}

// 保存更改负责人的异步方法
async function handleOwnerSave() {
  // 若未选择新负责人则弹出警告并返回
  if (!ownerForm.value.newOwnerId) { ElMessage.warning('请选择新负责人'); return }
  // 开启保存加载状态
  ownerSaving.value = true
  // 开启try块
  try {
    // 在项目列表中查找当前项目
    const p = projects.value.find(x => x.id === ownerForm.value.id)
    // 若找到项目
    if (p) {
      // 将项目的ownerId更新为新负责人ID
      p.ownerId = ownerForm.value.newOwnerId
      // 调用API保存项目变更
      const res = await saveProject(p)
      // 若保存成功
      if (res.success) {
        // 弹出成功提示"负责人已更改"
        ElMessage.success('负责人已更改')
        // 关闭弹窗
        ownerDialogVisible.value = false
        // 重新获取项目列表以刷新表格
        fetchProjects()
      }
    }
  } finally { ownerSaving.value = false } // finally块：关闭保存加载状态
}

// 归档GitHub仓库的异步方法
async function handleArchive(row) {
  // 开启try块
  try {
    // 弹出警告确认框，提示归档后仓库只读
    await ElMessageBox.confirm('归档后仓库变为只读，确认归档？', '归档仓库', { type: 'warning' })
  } catch { return } // 用户取消则直接返回
  // 调用API归档项目仓库
  const res = await archiveProject(row.id)
  // 若归档成功
  if (res.success) {
    // 将该项目的归档状态标记为true
    archived[row.id] = true
    // 弹出成功提示"仓库已归档"
    ElMessage.success('仓库已归档')
  }
}

// 删除单个项目的异步方法
async function handleDelete(id) {
  // 调用API删除指定ID的项目
  const res = await deleteProject(id)
  // 若删除成功
  if (res.success) {
    // 弹出成功提示"删除成功"
    ElMessage.success('删除成功')
    // 重新获取项目列表以刷新表格
    fetchProjects()
  }
}

// 根据状态值返回Element Plus标签类型的方法：已完成→success(绿)，进行中→warning(橙)，其他→info(灰)
function statusType(status) {
  return status === '已完成' ? 'success' : status === '进行中' ? 'warning' : 'info'
}
</script>
