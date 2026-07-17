<template>  <!-- 模板部分：定义总结管理面板的 HTML 结构 -->
  <div>  <!-- 根容器元素 -->
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">  <!-- 头部容器：弹性布局两端对齐，垂直居中，下边距 16px -->
      <h2>总结管理</h2>  <!-- 页面标题"总结管理" -->
      <el-button type="primary" @click="openDialog">新增总结</el-button>  <!-- 主色调按钮，点击打开新增总结弹窗 -->
    </div>  <!-- 头部容器结束 -->

    <el-table :data="summaries" border stripe v-loading="loading">  <!-- Element Plus 表格，数据源为 summaries，带边框和斑马纹，加载状态 -->
      <el-table-column prop="id" label="ID" width="60" />  <!-- 表格列：ID，宽度 60px -->
      <el-table-column prop="projectName" label="所属项目" width="120" />  <!-- 表格列：所属项目，宽度 120px -->
      <el-table-column prop="taskTitle" label="关联任务" width="120" />  <!-- 表格列：关联任务，宽度 120px -->
      <el-table-column prop="summaryType" label="总结类型" width="100">  <!-- 表格列：总结类型，宽度 100px -->
        <template #default="{ row }">  <!-- 自定义列内容插槽，解构获取行数据 -->
          <el-tag :type="row.summaryType === '最终总结' ? 'success' : 'warning'">{{ row.summaryType }}</el-tag>  <!-- 标签组件：最终总结为成功色，否则警告色 -->
        </template>  <!-- 插槽结束 -->
      </el-table-column>  <!-- 总结类型列结束 -->
      <el-table-column prop="content" label="内容" show-overflow-tooltip />  <!-- 表格列：内容，溢出时显示 tooltip -->
      <el-table-column prop="creatorName" label="创建人" width="100" />  <!-- 表格列：创建人，宽度 100px -->
      <el-table-column prop="createTime" label="时间" width="160" />  <!-- 表格列：时间，宽度 160px -->
    </el-table>  <!-- 表格组件结束 -->

    <!-- 新增总结弹窗 -->
    <el-dialog title="新增总结" v-model="dialogVisible" width="550px">  <!-- Element Plus 对话框，标题"新增总结"，绑定显示状态，宽度 550px -->
      <el-form :model="form" label-width="80px">  <!-- Element Plus 表单，绑定 form 数据，标签宽度 80px -->
        <el-form-item label="所属项目">  <!-- 表单项：所属项目 -->
          <el-select v-model="form.projectId" style="width:100%">  <!-- 下拉选择框，绑定 form.projectId，宽度 100% -->
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />  <!-- 循环渲染项目选项，键为项目 ID -->
          </el-select>  <!-- 下拉框结束 -->
        </el-form-item>  <!-- 表单项结束 -->
        <el-form-item label="关联任务">  <!-- 表单项：关联任务 -->
          <el-select v-model="form.taskId" style="width:100%" clearable placeholder="可选">  <!-- 下拉选择框，可清除，占位文字"可选" -->
            <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />  <!-- 循环渲染任务选项，键为任务 ID -->
          </el-select>  <!-- 下拉框结束 -->
        </el-form-item>  <!-- 表单项结束 -->
        <el-form-item label="总结类型">  <!-- 表单项：总结类型 -->
          <el-select v-model="form.summaryType" style="width:100%">  <!-- 下拉选择框，绑定 form.summaryType -->
            <el-option v-for="s in summaryTypes" :key="s" :label="s" :value="s" />  <!-- 循环渲染总结类型选项 -->
          </el-select>  <!-- 下拉框结束 -->
        </el-form-item>  <!-- 表单项结束 -->
        <el-form-item label="总结内容">  <!-- 表单项：总结内容 -->
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请撰写总结内容" />  <!-- 文本域输入框，6 行高度，占位提示文字 -->
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
import { getSummaries, saveSummary, getProjects, getTasks } from '../api'  // 从 API 模块导入总结列表、保存总结、项目列表、任务列表接口函数
import { ElMessage } from 'element-plus'  // 从 Element Plus 导入 ElMessage 消息提示组件

defineProps({ currentUser: Object })  // 定义组件 props：接收 currentUser 对象类型属性

const summaries = ref([])  // 总结列表响应式引用，初始为空数组
const projects = ref([])  // 项目列表响应式引用，初始为空数组
const tasks = ref([])  // 任务列表响应式引用，初始为空数组
const loading = ref(false)  // 加载状态，初始为 false
const dialogVisible = ref(false)  // 对话框显示状态，初始为 false（隐藏）
const form = ref({ projectId: null, taskId: null, summaryType: '阶段总结', content: '' })  // 表单数据响应式对象，默认总结类型为"阶段总结"
const summaryTypes = ['阶段总结', '最终总结']  // 总结类型选项数组

onMounted(async () => {  // 组件挂载后执行的生命周期钩子（异步）
  loading.value = true  // 设置加载状态为 true
  try {  // 尝试执行以下代码
    const [sRes, pRes, tRes] = await Promise.all([getSummaries(), getProjects(), getTasks()])  // 并行请求总结列表、项目列表、任务列表三个接口
    if (sRes.success) summaries.value = sRes.data || []  // 如果总结请求成功则更新列表，否则为空数组
    if (pRes.success) projects.value = pRes.data || []  // 如果项目请求成功则更新列表，否则为空数组
    if (tRes.success) tasks.value = tRes.data || []  // 如果任务请求成功则更新列表，否则为空数组
  } finally { loading.value = false }  // 无论成功失败，最终关闭加载状态
})  // onMounted 钩子结束

async function fetchSummaries() {  // 异步函数：重新获取总结列表
  const res = await getSummaries()  // 调用 API 获取总结列表
  if (res.success) summaries.value = res.data || []  // 请求成功则更新列表，否则为空数组
}  // fetchSummaries 函数结束

function openDialog() {  // 函数：打开新增总结对话框
  form.value = { projectId: null, taskId: null, summaryType: '阶段总结', content: '' }  // 重置表单数据为默认值
  dialogVisible.value = true  // 设置对话框为可见
}  // openDialog 函数结束

async function handleSave() {  // 异步函数：处理保存总结操作
  const res = await saveSummary(form.value)  // 调用 API 保存当前表单数据
  if (res.success) {  // 如果保存成功
    ElMessage.success('总结已保存')  // 弹出成功提示消息
    dialogVisible.value = false  // 关闭对话框
    fetchSummaries()  // 重新获取总结列表以刷新表格
  }  // 成功判断结束
}  // handleSave 函数结束
</script>  // 脚本结束
