<template>  <!-- 模板部分：定义进度更新面板的 HTML 结构 -->
  <div>  <!-- 根容器元素 -->
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">  <!-- 头部容器：弹性布局两端对齐，垂直居中，下边距 16px -->
      <h2>进度更新</h2>  <!-- 页面标题"进度更新" -->
      <div style="display:flex;gap:12px">  <!-- 右侧控件容器，弹性布局，间距 12px -->
        <el-select v-model="filterTaskId" placeholder="筛选任务" clearable style="width:220px" @change="fetchLogs">  <!-- 任务筛选下拉框，绑定 filterTaskId，可清除，改变时重新获取日志 -->
          <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />  <!-- 循环渲染任务选项，键为任务 ID -->
        </el-select>  <!-- 筛选下拉框结束 -->
        <el-button type="primary" @click="openDialog">新增进度</el-button>  <!-- 主色调按钮，点击打开新增进度弹窗 -->
      </div>  <!-- 右侧控件容器结束 -->
    </div>  <!-- 头部容器结束 -->

    <el-table :data="logs" border stripe v-loading="loading">  <!-- Element Plus 表格，数据源为 logs，带边框和斑马纹，加载状态 -->
      <el-table-column prop="id" label="ID" width="60" />  <!-- 表格列：ID，宽度 60px -->
      <el-table-column prop="taskTitle" label="任务" width="150" show-overflow-tooltip />  <!-- 表格列：任务，宽度 150px，溢出显示 tooltip -->
      <el-table-column prop="progressPercent" label="进度" width="180">  <!-- 表格列：进度，宽度 180px -->
        <template #default="{ row }">  <!-- 自定义列内容插槽，解构获取行数据 -->
          <div style="display:flex;align-items:center;gap:8px">  <!-- 进度行容器：弹性布局，垂直居中，间距 8px -->
            <el-progress :percentage="row.progressPercent" :stroke-width="10" style="flex:1" />  <!-- Element Plus 进度条组件，百分比绑定行数据，线条宽度 10px，弹性占满 -->
            <span style="font-size:12px;width:40px">{{ row.progressPercent }}%</span>  <!-- 百分比文字显示，固定宽度 40px -->
          </div>  <!-- 进度行容器结束 -->
        </template>  <!-- 插槽结束 -->
      </el-table-column>  <!-- 进度列结束 -->
      <el-table-column prop="content" label="说明内容" show-overflow-tooltip />  <!-- 表格列：说明内容，溢出显示 tooltip -->
      <el-table-column prop="operatorName" label="记录人" width="100" />  <!-- 表格列：记录人，宽度 100px -->
      <el-table-column prop="createTime" label="时间" width="160" />  <!-- 表格列：时间，宽度 160px -->
    </el-table>  <!-- 表格组件结束 -->

    <!-- 新增进度弹窗 -->
    <el-dialog title="新增进度记录" v-model="dialogVisible" width="500px">  <!-- Element Plus 对话框，标题"新增进度记录"，绑定显示状态，宽度 500px -->
      <el-form :model="form" label-width="80px">  <!-- Element Plus 表单，绑定 form 数据，标签宽度 80px -->
        <el-form-item label="选择任务">  <!-- 表单项：选择任务 -->
          <el-select v-model="form.taskId" style="width:100%" filterable>  <!-- 下拉选择框，绑定 form.taskId，支持搜索过滤 -->
            <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />  <!-- 循环渲染任务选项，键为任务 ID -->
          </el-select>  <!-- 下拉框结束 -->
        </el-form-item>  <!-- 表单项结束 -->
        <el-form-item label="进度百分比">  <!-- 表单项：进度百分比 -->
          <el-slider v-model="form.progressPercent" :min="0" :max="100" show-input />  <!-- Element Plus 滑块组件，范围 0-100，显示输入框 -->
        </el-form-item>  <!-- 表单项结束 -->
        <el-form-item label="说明内容">  <!-- 表单项：说明内容 -->
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请描述本次进度更新内容" />  <!-- 文本域输入框，4 行高度，占位提示文字 -->
        </el-form-item>  <!-- 表单项结束 -->
      </el-form>  <!-- 表单结束 -->
      <template #footer>  <!-- 对话框底部插槽 -->
        <el-button @click="dialogVisible = false">取消</el-button>  <!-- 取消按钮，点击关闭对话框 -->
        <el-button type="primary" @click="handleSave">保存</el-button>  <!-- 主色调保存按钮，点击执行保存逻辑 -->
      </template>  <!-- 底部插槽结束 -->
    </el-dialog>  <!-- 对话框组件结束 -->
  </div>  <!-- 根容器结束 -->
</template>  <!-- 模板结束 -->

<script setup>  // 脚本部分：使用 Vue 3 <script setup> 语法糖
import { ref, onMounted } from 'vue'  // 从 Vue 中导入 ref（响应式引用）、onMounted（生命周期钩子）
import { getTaskLogs, saveTaskLog, getTasks } from '../api'  // 从 API 模块导入任务日志列表、保存日志、任务列表接口函数
import { ElMessage } from 'element-plus'  // 从 Element Plus 导入 ElMessage 消息提示组件

defineProps({ currentUser: Object })  // 定义组件 props：接收 currentUser 对象类型属性

const logs = ref([])  // 进度日志列表响应式引用，初始为空数组
const tasks = ref([])  // 任务列表响应式引用，初始为空数组
const loading = ref(false)  // 加载状态，初始为 false
const filterTaskId = ref(null)  // 筛选任务 ID，初始为 null（不筛选）
const dialogVisible = ref(false)  // 对话框显示状态，初始为 false（隐藏）
const form = ref({ taskId: null, progressPercent: 0, content: '' })  // 表单数据响应式对象，默认进度为 0%

onMounted(async () => {  // 组件挂载后执行的生命周期钩子（异步）
  loading.value = true  // 设置加载状态为 true
  try {  // 尝试执行以下代码
    const [logRes, taskRes] = await Promise.all([getTaskLogs(), getTasks()])  // 并行请求任务日志列表和任务列表两个接口
    if (logRes.success) logs.value = logRes.data || []  // 如果日志请求成功则更新列表，否则为空数组
    if (taskRes.success) tasks.value = taskRes.data || []  // 如果任务请求成功则更新列表，否则为空数组
  } finally { loading.value = false }  // 无论成功失败，最终关闭加载状态
})  // onMounted 钩子结束

async function fetchLogs() {  // 异步函数：重新获取任务日志列表（支持筛选）
  loading.value = true  // 设置加载状态为 true
  try {  // 尝试执行以下代码
    const res = await getTaskLogs(filterTaskId.value || undefined)  // 调用 API 获取日志，传入筛选任务 ID（无则为 undefined）
    if (res.success) logs.value = res.data || []  // 请求成功则更新列表，否则为空数组
  } finally { loading.value = false }  // 无论成功失败，最终关闭加载状态
}  // fetchLogs 函数结束

function openDialog() {  // 函数：打开新增进度对话框
  form.value = { taskId: null, progressPercent: 0, content: '' }  // 重置表单数据为默认值
  dialogVisible.value = true  // 设置对话框为可见
}  // openDialog 函数结束

async function handleSave() {  // 异步函数：处理保存进度记录操作
  const res = await saveTaskLog(form.value)  // 调用 API 保存当前表单数据
  if (res.success) {  // 如果保存成功
    ElMessage.success('进度记录已保存')  // 弹出成功提示消息
    dialogVisible.value = false  // 关闭对话框
    fetchLogs()  // 重新获取日志列表以刷新表格
  }  // 成功判断结束
}  // handleSave 函数结束
</script>  // 脚本结束
