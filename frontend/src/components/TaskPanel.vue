<template>  <!-- 模板区域：定义任务管理面板的HTML结构 -->
  <div>  <!-- 根容器元素 -->
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">  <!-- 顶部工具栏容器：弹性布局、两端对齐、垂直居中、底部外边距16px -->
      <div style="display:flex;align-items:center;gap:12px">  <!-- 左侧区域：弹性布局、垂直居中、子元素间距12px -->
        <h2>任务管理</h2>  <!-- 页面标题"任务管理" -->
        <el-button v-if="(isLeader || isAdmin) && selectedIds.length > 0"
                   type="danger" @click="handleBatchDelete" :loading="batchDeleting">  <!-- 批量删除按钮：负责人或管理员可见、危险色调、点击调用批量删除、操作中显示loading -->
          批量删除（{{ selectedIds.length }}）  <!-- 按钮文字：显示已选中的任务数量 -->
        </el-button>  <!-- 批量删除按钮结束 -->
      </div>  <!-- 左侧区域结束 -->
      <div style="display:flex;gap:12px">  <!-- 右侧区域：弹性布局、子元素间距12px -->
        <el-select v-model="filterProjectId" placeholder="筛选项目" clearable style="width:200px" @change="fetchTasks">  <!-- 项目筛选下拉框：双向绑定筛选项目ID、可清除、宽度200px、切换时重新获取任务 -->
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />  <!-- 循环渲染项目选项：key为项目ID、标签显示项目名称、值为项目ID -->
        </el-select>  <!-- 项目筛选下拉框结束 -->
        <el-button v-if="isLeader || isAdmin" type="primary" @click="openDialog(null)">新建任务</el-button>  <!-- 新建任务按钮：仅负责人和管理员可见、主色调、点击打开空白新建弹窗 -->
      </div>  <!-- 右侧区域结束 -->
    </div>  <!-- 顶部工具栏容器结束 -->

    <el-table :data="tasks" border stripe v-loading="loading" row-key="id"
              @selection-change="handleSelectionChange">  <!-- 任务数据表格：绑定任务数组、带边框、斑马纹、加载中遮罩、行唯一键为id、多选变化事件 -->
      <el-table-column type="selection" width="45" />  <!-- 多选列，宽度45px -->
      <el-table-column prop="id" label="ID" width="60" />  <!-- ID列，绑定id属性，宽度60px -->
      <el-table-column prop="title" label="任务标题" />  <!-- 任务标题列，绑定title属性 -->
      <el-table-column prop="projectName" label="所属项目" width="120" />  <!-- 所属项目列，绑定projectName属性，宽度120px -->
      <el-table-column prop="description" label="任务说明" show-overflow-tooltip />  <!-- 任务说明列，绑定description属性，内容溢出时显示tooltip -->
      <el-table-column prop="assigneeName" label="负责人" width="100" />  <!-- 负责人列，绑定assigneeName属性，宽度100px -->
      <el-table-column prop="status" label="状态" width="100">  <!-- 状态列，绑定status属性，宽度100px -->
        <template #default="{ row }">  <!-- 自定义状态列内容，解构获取当前行数据 -->
          <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>  <!-- 状态标签：颜色根据statusType函数动态计算，显示当前行状态文字 -->
        </template>  <!-- 状态列模板结束 -->
      </el-table-column>  <!-- 状态列结束 -->
      <el-table-column prop="priority" label="优先级" width="80" />  <!-- 优先级列，绑定priority属性，宽度80px -->
      <el-table-column prop="dueDate" label="截止日期" width="110" />  <!-- 截止日期列，绑定dueDate属性，宽度110px -->
      <el-table-column label="操作" width="240">  <!-- 操作列，宽度240px -->
        <template #default="{ row }">  <!-- 自定义操作列内容，解构获取当前行数据 -->
          <el-button v-if="isLeader || isAdmin" size="small" @click="openDialog(row)">编辑</el-button>  <!-- 编辑按钮：仅负责人和管理员可见、小尺寸、点击打开编辑弹窗并传入当前行数据 -->
          <el-button v-if="isAdmin || isProjectOwner(row)" size="small" type="primary" @click="openAssignDialog(row)">改负责人</el-button>  <!-- 更改负责人按钮：管理员或项目负责人可见、小尺寸、主色调 -->
          <el-button v-if="(isAdmin || isProjectOwner(row)) && !row.githubPublished" size="small" type="success" @click="handlePublishTask(row)">发布到GitHub</el-button>  <!-- 发布到GitHub按钮：管理员或项目负责人可见且未发布时显示、小尺寸、成功色调 -->
          <el-button v-if="row.githubPublished" size="small" type="success" plain @click="openTaskCode(row)">  <!-- 查看代码按钮：已发布到GitHub时显示、小尺寸、成功色调镂空样式 -->
            {{ isProjectOwner(row) ? '查看代码' : '我的代码' }}  <!-- 按钮文字：项目负责人显示"查看代码"，普通组员显示"我的代码" -->
          </el-button>  <!-- 查看代码按钮结束 -->
          <el-button v-if="canUpdateStatus(row)" size="small" type="warning" @click="openStatusDialog(row)">更新状态</el-button>  <!-- 更新状态按钮：当前用户可更新时显示、小尺寸、警告色调 -->
          <el-popconfirm v-if="isLeader || isAdmin" title="确认删除？" @confirm="handleDelete(row.id)">  <!-- 删除确认气泡：仅负责人和管理员可见、确认文字"确认删除？"、确认后调用删除 -->
            <template #reference>  <!-- 触发气泡的参考元素插槽 -->
              <el-button size="small" type="danger">删除</el-button>  <!-- 删除按钮：小尺寸、危险色调 -->
            </template>  <!-- 参考元素模板结束 -->
          </el-popconfirm>  <!-- 删除确认气泡结束 -->
        </template>  <!-- 操作列模板结束 -->
      </el-table-column>  <!-- 操作列结束 -->
    </el-table>  <!-- 任务数据表格结束 -->

    <!-- 新建/编辑任务弹窗（负责人和管理员专用，包含全部字段） -->
    <el-dialog :title="form.id ? '编辑任务' : '新建任务'" v-model="dialogVisible" width="550px">  <!-- 弹窗：标题根据form.id动态切换、双向绑定显示状态、宽度550px -->
      <el-form :model="form" label-width="80px">  <!-- 任务表单：绑定form数据、标签宽度80px -->
        <el-form-item label="任务标题">  <!-- 表单项：标签"任务标题" -->
          <el-input v-model="form.title" />  <!-- 输入框双向绑定任务标题 -->
        </el-form-item>  <!-- 任务标题表单项结束 -->
        <el-form-item label="所属项目">  <!-- 表单项：标签"所属项目" -->
          <el-select v-model="form.projectId" style="width:100%">  <!-- 项目下拉选择框：双向绑定项目ID、宽度100% -->
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />  <!-- 循环渲染项目选项 -->
          </el-select>  <!-- 项目下拉框结束 -->
        </el-form-item>  <!-- 所属项目表单项结束 -->
        <el-form-item label="任务说明">  <!-- 表单项：标签"任务说明" -->
          <el-input v-model="form.description" type="textarea" :rows="3" />  <!-- 文本域输入框：双向绑定任务说明、3行高度 -->
        </el-form-item>  <!-- 任务说明表单项结束 -->
        <el-form-item label="负责人">  <!-- 表单项：标签"负责人" -->
          <el-select v-model="form.assigneeId" style="width:100%">  <!-- 负责人下拉选择框：双向绑定负责人ID、宽度100% -->
            <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />  <!-- 循环渲染用户选项：key为用户ID、标签显示真实姓名、值为用户ID -->
          </el-select>  <!-- 负责人下拉框结束 -->
        </el-form-item>  <!-- 负责人表单项结束 -->
        <el-form-item label="状态">  <!-- 表单项：标签"状态" -->
          <el-select v-model="form.status" style="width:100%">  <!-- 状态下拉选择框：双向绑定任务状态、宽度100% -->
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />  <!-- 循环渲染状态选项 -->
          </el-select>  <!-- 状态下拉框结束 -->
        </el-form-item>  <!-- 状态表单项结束 -->
        <el-form-item label="优先级">  <!-- 表单项：标签"优先级" -->
          <el-select v-model="form.priority" style="width:100%">  <!-- 优先级下拉选择框：双向绑定任务优先级、宽度100% -->
            <el-option v-for="p in priorities" :key="p" :label="p" :value="p" />  <!-- 循环渲染优先级选项 -->
          </el-select>  <!-- 优先级下拉框结束 -->
        </el-form-item>  <!-- 优先级表单项结束 -->
        <el-form-item label="截止日期">  <!-- 表单项：标签"截止日期" -->
          <el-date-picker v-model="form.dueDate" type="date" style="width:100%" value-format="YYYY-MM-DD" />  <!-- 日期选择器：双向绑定截止日期、日期类型、宽度100%、格式化输出YYYY-MM-DD -->
        </el-form-item>  <!-- 截止日期表单项结束 -->
      </el-form>  <!-- 任务表单结束 -->
      <template #footer>  <!-- 弹窗底部插槽 -->
        <el-button @click="dialogVisible = false">取消</el-button>  <!-- 取消按钮：点击关闭弹窗 -->
        <el-button type="primary" @click="handleSave">保存</el-button>  <!-- 保存按钮：主色调、点击调用保存方法 -->
      </template>  <!-- 底部插槽结束 -->
    </el-dialog>  <!-- 新建/编辑弹窗结束 -->

    <!-- 组员状态更新弹窗（普通组员更新自己任务的状态） -->
    <el-dialog title="更新任务状态" v-model="statusDialogVisible" width="400px">  <!-- 状态更新弹窗：固定标题"更新任务状态"、宽度400px -->
      <el-form :model="statusForm" label-width="80px">  <!-- 状态表单：绑定statusForm数据、标签宽度80px -->
        <el-form-item label="任务标题">  <!-- 表单项：标签"任务标题" -->
          <el-input :model-value="statusForm.title" disabled />  <!-- 只读输入框：单向显示任务标题，不可编辑 -->
        </el-form-item>  <!-- 任务标题表单项结束 -->
        <el-form-item label="当前状态">  <!-- 表单项：标签"当前状态" -->
          <el-tag :type="statusType(statusForm.oldStatus)">{{ statusForm.oldStatus }}</el-tag>  <!-- 状态标签显示当前旧状态，颜色根据statusType动态计算 -->
        </el-form-item>  <!-- 当前状态表单项结束 -->
        <el-form-item label="新状态">  <!-- 表单项：标签"新状态" -->
          <el-select v-model="statusForm.status" style="width:100%">  <!-- 新状态下拉选择框：双向绑定、宽度100% -->
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />  <!-- 循环渲染状态选项 -->
          </el-select>  <!-- 新状态下拉框结束 -->
        </el-form-item>  <!-- 新状态表单项结束 -->
      </el-form>  <!-- 状态表单结束 -->
      <template #footer>  <!-- 弹窗底部插槽 -->
        <el-button @click="statusDialogVisible = false">取消</el-button>  <!-- 取消按钮：点击关闭状态弹窗 -->
        <el-button type="primary" @click="handleStatusUpdate">确认更新</el-button>  <!-- 确认更新按钮：主色调、点击更新任务状态 -->
      </template>  <!-- 底部插槽结束 -->
    </el-dialog>  <!-- 状态更新弹窗结束 -->

    <!-- 管理员更改任务负责人弹窗 -->
    <el-dialog title="更改负责人" v-model="assignDialogVisible" width="400px">  <!-- 更改负责人弹窗：固定标题"更改负责人"、宽度400px -->
      <el-form :model="assignForm" label-width="80px">  <!-- 负责人表单：绑定assignForm数据、标签宽度80px -->
        <el-form-item label="任务">  <!-- 表单项：标签"任务" -->
          <el-input :model-value="assignForm.title" disabled />  <!-- 只读输入框：单向显示任务标题，不可编辑 -->
        </el-form-item>  <!-- 任务表单项结束 -->
        <el-form-item label="当前负责人">  <!-- 表单项：标签"当前负责人" -->
          <el-input :model-value="assignForm.oldAssignee" disabled />  <!-- 只读输入框：单向显示当前负责人姓名，不可编辑 -->
        </el-form-item>  <!-- 当前负责人表单项结束 -->
        <el-form-item label="新负责人">  <!-- 表单项：标签"新负责人" -->
          <el-select v-model="assignForm.newAssigneeId" style="width:100%">  <!-- 新负责人下拉选择框：双向绑定新负责人ID、宽度100% -->
            <el-option v-for="u in users" :key="u.id" :label="u.realName + ' (' + u.role + ')'"
                       :value="u.id" :disabled="u.role === '管理员'" />  <!-- 循环渲染用户选项：标签显示"真实姓名 (角色)"，选项值为用户ID，管理员角色禁用 -->
          </el-select>  <!-- 新负责人下拉框结束 -->
        </el-form-item>  <!-- 新负责人表单项结束 -->
      </el-form>  <!-- 负责人表单结束 -->
      <template #footer>  <!-- 弹窗底部插槽 -->
        <el-button @click="assignDialogVisible = false">取消</el-button>  <!-- 取消按钮：点击关闭负责人弹窗 -->
        <el-button type="primary" @click="handleAssignSave" :loading="assignSaving">确认更改</el-button>  <!-- 确认更改按钮：主色调、点击保存、保存中显示loading -->
      </template>  <!-- 底部插槽结束 -->
    </el-dialog>  <!-- 更改负责人弹窗结束 -->
  </div>  <!-- 根容器结束 -->
</template>  <!-- 模板区域结束 -->

<script setup>  // Vue3 Composition API 语法糖写法
import { ref, computed, onMounted } from 'vue'  // 从Vue导入响应式API：ref、computed、生命周期钩子onMounted
import { getTasks, saveTask, updateTaskStatus, deleteTask, batchDeleteTasks, getProjects, getUsers, githubPublishTask } from '../api'  // 从api模块导入任务相关接口：获取任务列表、保存任务、更新状态、删除任务、批量删除、获取项目列表、获取用户列表、发布任务到GitHub
import { ElMessage, ElMessageBox } from 'element-plus'  // 从Element Plus导入消息提示组件和确认弹窗组件

const props = defineProps({ currentUser: Object })  // 定义组件props，接收一个currentUser对象
const isLeader = computed(() => props.currentUser?.role === '负责人')  // 计算属性：判断当前用户是否为项目负责人
const isAdmin = computed(() => props.currentUser?.role === '管理员')  // 计算属性：判断当前用户是否为管理员

const tasks = ref([])  // 任务列表响应式数组，初始为空数组
const projects = ref([])  // 项目列表响应式数组，用于下拉选择，初始为空数组
const users = ref([])  // 用户列表响应式数组，用于负责人选择，初始为空数组
const loading = ref(false)  // 表格数据加载中的loading状态，初始为false
const batchDeleting = ref(false)  // 批量删除操作的loading状态，初始为false
const selectedIds = ref([])  // 表格多选选中的任务ID数组，初始为空数组
const filterProjectId = ref(null)  // 项目筛选条件，null表示不筛选显示全部
const dialogVisible = ref(false)  // 新建/编辑任务弹窗的显示/隐藏状态，初始隐藏
const statusDialogVisible = ref(false)  // 状态更新弹窗的显示/隐藏状态，初始隐藏
const assignDialogVisible = ref(false)  // 更改负责人弹窗的显示/隐藏状态，初始隐藏
const assignSaving = ref(false)  // 负责人更改保存操作的loading状态，初始为false
const assignForm = ref({})  // 更改负责人表单数据，初始为空对象
const form = ref({})  // 新建/编辑任务表单数据，初始为空对象
const statusForm = ref({})  // 状态更新表单数据，初始为空对象
const statuses = ['未开始', '进行中', '已完成']  // 任务状态常量数组：未开始、进行中、已完成
const priorities = ['低', '中', '高']  // 任务优先级常量数组：低、中、高

onMounted(async () => {  // 组件挂载完成后执行的异步生命周期钩子
  loading.value = true  // 开启表格加载loading状态
  try {  // 开始try代码块
    const [taskRes, projRes, userRes] = await Promise.all([getTasks(), getProjects(), getUsers()])  // 并发请求三个接口：获取任务列表、项目列表、用户列表，解构赋值到对应变量
    if (taskRes.success) tasks.value = taskRes.data || []  // 如果任务接口成功，更新任务列表，无数据则设为空数组
    if (projRes.success) projects.value = projRes.data || []  // 如果项目接口成功，更新项目列表，无数据则设为空数组
    if (userRes.success) users.value = userRes.data || []  // 如果用户接口成功，更新用户列表，无数据则设为空数组
  } finally {  // finally块
    loading.value = false  // 无论成功或失败，最终关闭表格loading状态
  }  // try-finally结束
})  // onMounted钩子结束

async function fetchTasks() {  // 根据筛选条件重新获取任务列表的异步函数
  loading.value = true  // 开启表格加载loading状态
  try {  // 开始try代码块
    const res = await getTasks(filterProjectId.value || undefined)  // 调用获取任务接口，传入筛选项目ID（无则传undefined表示获取全部）
    if (res.success) tasks.value = res.data || []  // 如果接口成功，更新任务列表，无数据则设为空数组
  } finally { loading.value = false }  // finally块：无论成功或失败，最终关闭表格loading状态
}  // fetchTasks函数结束

function canUpdateStatus(row) {  // 判断当前用户是否可以更新某任务的状态，参数row为任务行数据
  return row.assigneeId === props.currentUser?.id && row.status !== '已完成'  // 只有任务是分配给当前用户的且状态不是"已完成"时，才允许更新状态
}  // canUpdateStatus函数结束

function isProjectOwner(row) {  // 判断当前用户是否为某任务所属项目的负责人，参数row为任务行数据
  const p = projects.value.find(x => x.id === row.projectId)  // 在项目列表中查找与任务项目ID匹配的项目
  return p && p.ownerId === props.currentUser?.id  // 如果找到项目且项目负责人ID等于当前用户ID，返回true
}  // isProjectOwner函数结束

function openDialog(row) {  // 打开新建/编辑任务弹窗，参数row为任务行数据（null表示新建）
  form.value = row ? { ...row } : {  // 如果传入row则复制行数据用于编辑，否则创建默认空表单用于新建
    title: '', description: '', projectId: null, assigneeId: null,  // 新建默认值：标题空、说明空、项目null、负责人null
    status: '未开始', priority: '中', dueDate: ''  // 新建默认值：状态"未开始"、优先级"中"、截止日期空
  }  // 默认值对象结束
  dialogVisible.value = true  // 显示新建/编辑弹窗
}  // openDialog函数结束

async function handleSave() {  // 处理任务保存（新建或编辑）的异步函数
  const res = await saveTask(form.value)  // 调用保存任务接口，传入表单数据
  if (res.success) {  // 如果保存成功
    ElMessage.success('保存成功')  // 弹出成功提示消息
    dialogVisible.value = false  // 关闭弹窗
    fetchTasks()  // 重新获取任务列表，刷新表格数据
  }  // 成功判断结束
}  // handleSave函数结束

function openStatusDialog(row) {  // 打开任务状态更新弹窗，参数row为任务行数据
  statusForm.value = { id: row.id, title: row.title, oldStatus: row.status, status: row.status }  // 初始化状态表单：保存任务ID、标题、旧状态、新状态（默认与旧状态相同）
  statusDialogVisible.value = true  // 显示状态更新弹窗
}  // openStatusDialog函数结束

async function handleStatusUpdate() {  // 处理任务状态更新的异步函数
  const res = await updateTaskStatus(statusForm.value.id, statusForm.value.status)  // 调用更新任务状态接口，传入任务ID和新状态
  if (res.success) {  // 如果更新成功
    ElMessage.success('状态更新成功')  // 弹出成功提示消息
    statusDialogVisible.value = false  // 关闭状态弹窗
    fetchTasks()  // 重新获取任务列表，刷新表格数据
  }  // 成功判断结束
}  // handleStatusUpdate函数结束

function handleSelectionChange(rows) {  // 处理表格多选变化的函数，参数rows为当前选中的行数据数组
  selectedIds.value = rows.map(r => r.id)  // 将选中行映射为ID数组，更新selectedIds响应式状态
}  // handleSelectionChange函数结束

async function handleBatchDelete() {  // 处理批量删除任务的异步函数
  try {  // 开始try代码块
    await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 个任务？`, '批量删除', { type: 'warning' })  // 弹出警告确认对话框，显示选中的任务数量
  } catch { return }  // 用户取消操作时捕获异常并直接返回
  batchDeleting.value = true  // 开启批量删除loading状态
  try {  // 开始try代码块
    const res = await batchDeleteTasks(selectedIds.value)  // 调用批量删除接口，传入选中的任务ID数组
    if (res.success) {  // 如果删除成功
      ElMessage.success(res.message)  // 弹出成功提示消息（使用接口返回的消息文本）
      selectedIds.value = []  // 清空已选中的ID列表
      fetchTasks()  // 重新获取任务列表，刷新表格数据
    }  // 成功判断结束
  } finally { batchDeleting.value = false }  // finally块：无论成功或失败，最终关闭批量删除loading状态
}  // handleBatchDelete函数结束

function openAssignDialog(row) {  // 打开更改负责人弹窗，参数row为任务行数据
  assignForm.value = {  // 初始化负责人表单数据
    id: row.id,  // 任务ID
    title: row.title,  // 任务标题（只读显示）
    oldAssignee: row.assigneeName,  // 当前负责人姓名（只读显示）
    newAssigneeId: row.assigneeId  // 新负责人ID，默认与当前相同
  }  // assignForm对象结束
  assignDialogVisible.value = true  // 显示更改负责人弹窗
}  // openAssignDialog函数结束

async function handleAssignSave() {  // 处理保存负责人更改的异步函数
  assignSaving.value = true  // 开启保存loading状态
  try {  // 开始try代码块
    const task = tasks.value.find(t => t.id === assignForm.value.id)  // 在任务列表中查找匹配ID的任务对象
    if (task) {  // 如果找到对应任务
      task.assigneeId = assignForm.value.newAssigneeId  // 更新任务的负责人ID为新选择的值
      const res = await saveTask(task)  // 调用保存任务接口，更新任务数据
      if (res.success) {  // 如果保存成功
        ElMessage.success('负责人已更改')  // 弹出成功提示消息
        assignDialogVisible.value = false  // 关闭更改负责人弹窗
        fetchTasks()  // 重新获取任务列表，刷新表格数据
      }  // 成功判断结束
    }  // 任务查找判断结束
  } finally { assignSaving.value = false }  // finally块：无论成功或失败，最终关闭保存loading状态
}  // handleAssignSave函数结束

async function handlePublishTask(row) {  // 处理发布任务到GitHub的异步函数，参数row为任务行数据
  try {  // 开始try代码块
    const res = await githubPublishTask(row.id)  // 调用发布任务到GitHub接口，传入任务ID
    if (res.success) {  // 如果发布成功
      row.githubPublished = true  // 设置当前行数据的githubPublished标记为true
      row._branchName = res.data.branchName  // 保存返回的Git分支名到行数据（用于后续查看代码）
      ElMessage.success(`已发布：Issue #${res.data.issueNumber} + 分支 ${res.data.branchName}`)  // 弹出成功提示，显示Issue编号和分支名
    }  // 成功判断结束
  } catch {}  // 捕获异常但不做处理（静默失败）
}  // handlePublishTask函数结束

function openTaskCode(row) {  // 打开任务代码视图的函数，参数row为任务行数据
  // 分支名与后端 GitHubRepositoryController.publishTask 保持一致
  const slug = row.title.replace(/[^a-zA-Z0-9一-龥]/g, '-').replace(/-+/g, '-').replace(/^-|-$/g, '').toLowerCase()  // 将任务标题处理为URL安全的slug：非字母数字中文替换为短横线、合并连续短横线、去除首尾短横线、转小写
  const branchName = 'task/' + row.id + '-' + slug  // 拼接Git分支名：格式为 task/任务ID-标题slug
  window.dispatchEvent(new CustomEvent('nav-github-branch', {  // 派发自定义导航事件，事件名为nav-github-branch
    detail: { projectId: row.projectId, branchName, taskTitle: row.title }  // 事件详情：传递项目ID、分支名、任务标题
  }))  // dispatchEvent调用结束
}  // openTaskCode函数结束

async function handleDelete(id) {  // 处理单个任务删除的异步函数，参数id为要删除的任务ID
  await deleteTask(id)  // 调用删除任务接口，传入任务ID
  ElMessage.success('删除成功')  // 弹出成功提示消息
  fetchTasks()  // 重新获取任务列表，刷新表格数据
}  // handleDelete函数结束

function statusType(status) {  // 根据任务状态返回Element Plus标签类型的函数，参数status为状态字符串
  return status === '已完成' ? 'success' : status === '进行中' ? 'warning' : 'info'  // 已完成返回success(绿色)、进行中返回warning(橙色)、其他返回info(灰色)
}  // statusType函数结束
</script>  <!-- script区域结束（此组件无style部分） -->
