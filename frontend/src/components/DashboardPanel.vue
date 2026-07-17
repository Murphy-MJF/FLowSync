<template>  <!-- 模板部分：定义控制台面板的 HTML 结构 -->
  <div>  <!-- 根容器元素 -->
    <h2 style="margin-bottom:20px">控制台</h2>  <!-- 页面标题"控制台"，下边距 20px -->
    <el-row :gutter="20">  <!-- Element Plus 栅格行，列间距 20px -->
      <el-col :span="6" v-for="item in stats" :key="item.label">  <!-- 每个统计卡片占 6/24 列宽，循环渲染 stats 数组 -->
        <el-card shadow="hover">  <!-- Element Plus 卡片组件，悬停时显示阴影 -->
          <div style="text-align:center">  <!-- 卡片内容居中容器 -->
            <div style="font-size:14px;color:#909399">{{ item.label }}</div>  <!-- 统计项标签，字号 14px，灰色文字 -->
            <div style="font-size:36px;font-weight:bold;color:#409EFF;margin-top:8px">{{ item.value }}</div>  <!-- 统计项数值，大号蓝色加粗字体 -->
          </div>  <!-- 居中容器结束 -->
        </el-card>  <!-- 卡片组件结束 -->
      </el-col>  <!-- 栅格列结束 -->
    </el-row>  <!-- 栅格行结束 -->

    <!-- 上传排队状态区域，仅当队列有数据时显示 -->
    <el-card v-if="queueItems.length > 0" style="margin-top:20px">  <!-- 卡片上边距 20px -->
      <template #header>  <!-- 卡片头部插槽 -->
        <span>文件上传队列</span>  <!-- 卡片标题"文件上传队列" -->
      </template>  <!-- 头部插槽结束 -->
      <!-- 循环渲染队列中的每个上传项，使用文件路径作为唯一键 -->
      <div v-for="item in queueItems" :key="item.path"
           style="display:flex;align-items:center;justify-content:space-between;padding:10px;margin-bottom:8px;border-radius:4px"
           :style="{ background: item.ready ? '#f0f9eb' : '#fdf6ec' }">
        <div>  <!-- 队列项信息容器 -->
          <div style="font-weight:bold">{{ item.owner }}/{{ item.repo }} — {{ item.path }}</div>  <!-- 显示仓库所有者/仓库名和文件路径，加粗 -->
          <div style="font-size:12px;color:#909399;margin-top:4px">  <!-- 状态信息行，小号灰色字体 -->
            <template v-if="item.ready">  <!-- 条件渲染：如果该项已就绪 -->
              <span style="color:#67C23A">✅ 轮到您了！请检查远端最新版本后确认上传</span>  <!-- 绿色提示文字，提醒用户轮到上传 -->
            </template>  <!-- 就绪条件结束 -->
            <template v-else>  <!-- 条件渲染：否则（未就绪） -->
              ⏳ {{ item.currentHolder || '未知' }} 正在上传，等待中...  <!-- 显示当前持有者名称，无则显示"未知"，等待提示 -->
            </template>  <!-- 未就绪条件结束 -->
          </div>  <!-- 状态行容器结束 -->
        </div>  <!-- 队列项信息容器结束 -->
        <el-button v-if="item.ready" type="primary" size="small" @click="jumpToUpload(item)">  <!-- 仅就绪时显示主色调小按钮，点击跳转到上传页面 -->
          去确认上传  <!-- 按钮文本 -->
        </el-button>  <!-- 按钮结束 -->
      </div>  <!-- 队列循环结束 -->
    </el-card>  <!-- 上传队列卡片结束 -->

    <!-- 文件修改记录区域 -->
    <el-card style="margin-top:20px">  <!-- 文件修改记录卡片，上边距 20px -->
      <template #header>  <!-- 卡片头部插槽 -->
        <div style="display:flex;justify-content:space-between;align-items:center">  <!-- 头部弹性布局，两端对齐，垂直居中 -->
          <span>文件修改记录</span>  <!-- 卡片标题"文件修改记录" -->
          <div style="display:flex;gap:8px">  <!-- 筛选控件容器，弹性布局，间距 8px -->
            <el-select v-model="selectedProject" placeholder="选择项目" size="small" style="width:200px" @change="loadCommits" clearable>  <!-- 项目选择下拉框，绑定 selectedProject，改变时加载提交记录，可清除 -->
              <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />  <!-- 循环渲染项目选项，键为项目 ID -->
            </el-select>  <!-- 项目下拉框结束 -->
            <el-select v-model="filterUser" placeholder="筛选成员" size="small" style="width:150px" @change="loadCommits" clearable>  <!-- 成员筛选下拉框，绑定 filterUser，可清除 -->
              <el-option v-for="u in uniqueUsers" :key="u" :label="u" :value="u" />  <!-- 循环渲染不重复的成员选项 -->
            </el-select>  <!-- 成员下拉框结束 -->
          </div>  <!-- 筛选控件容器结束 -->
        </div>  <!-- 头部弹性容器结束 -->
      </template>  <!-- 头部插槽结束 -->
      <div v-if="!selectedProject" style="text-align:center;padding:20px;color:#909399">选择项目查看 Git 提交历史</div>  <!-- 未选择项目时显示提示文字 -->
      <div v-else-if="commits.length === 0" style="text-align:center;padding:20px;color:#909399" v-loading="commitLoading">暂无提交记录或未绑定仓库</div>  <!-- 已选项目但无提交时显示空状态提示，带加载动画 -->
      <el-timeline v-else>  <!-- Element Plus 时间线组件，有提交数据时渲染 -->
        <!-- 循环渲染每个提交为时间线项，键为 SHA，显示日期，顶部放置 -->
        <el-timeline-item v-for="c in commits" :key="c.sha" :timestamp="c.date" placement="top"
                          :color="c.flowSyncUser ? '#409EFF' : '#909399'">
          <div>{{ c.message }}</div>  <!-- 显示提交信息 -->
          <div style="font-size:12px;color:#909399;margin-top:4px">  <!-- 提交详情行，小号灰色字体 -->
            SHA: <code>{{ c.sha }}</code>  <!-- 显示提交 SHA 值，使用代码字体 -->
            <template v-if="c.githubLogin"> | <strong>{{ c.githubLogin }}</strong></template>  <!-- 如有 GitHub 登录名则加粗显示 -->
            <template v-if="c.flowSyncUser"> | <el-tag size="small" type="success">{{ c.flowSyncUser }}</el-tag></template>  <!-- 如有 flowSyncUser 则以成功标签显示 -->
            <template v-if="c.author && !c.githubLogin"> | {{ c.author }}</template>  <!-- 无 GitHub 登录名但有作者时显示作者名 -->
          </div>  <!-- 详情行结束 -->
        </el-timeline-item>  <!-- 时间线项结束 -->
      </el-timeline>  <!-- 时间线组件结束 -->
    </el-card>  <!-- 文件修改记录卡片结束 -->
  </div>  <!-- 根容器结束 -->
</template>  <!-- 模板结束 -->

<script setup>  // 脚本部分：使用 Vue 3 <script setup> 语法糖
import { ref, computed, onMounted } from 'vue'  // 从 Vue 中导入 ref（响应式引用）、computed（计算属性）、onMounted（生命周期钩子）
import { getOverview, getProjects, githubProjectCommits } from '../api'  // 从 API 模块导入概览、项目列表、GitHub 提交记录接口函数
import { pendingUploads } from '../store/uploadQueue'  // 从上传队列 store 导入待上传列表响应式数据

const stats = ref([  // 定义响应式统计数组，初始值均为 0
  { label: '系统用户', value: 0 },  // 统计项：系统用户数
  { label: '项目总数', value: 0 },  // 统计项：项目总数
  { label: '任务总数', value: 0 },  // 统计项：任务总数
  { label: '总结总数', value: 0 }  // 统计项：总结总数
])  // stats 数组定义结束

const queueItems = pendingUploads  // 队列数据直接引用 store 中的待上传列表
const projects = ref([])  // 项目列表响应式引用，初始为空数组
const selectedProject = ref(null)  // 当前选中的项目 ID，初始为 null
const commits = ref([])  // 当前显示的提交记录列表（经过筛选），初始为空数组
const allCommits = ref([])  // 所有提交记录列表（未筛选），初始为空数组
const filterUser = ref('')  // 成员筛选关键词，初始为空字符串
const commitLoading = ref(false)  // 提交记录加载状态，初始为 false

const uniqueUsers = computed(() => {  // 计算属性：从所有提交中提取不重复的 flowSyncUser 列表
  const names = new Set()  // 使用 Set 数据结构去重
  for (const c of allCommits.value) {  // 遍历所有提交记录
    if (c.flowSyncUser) names.add(c.flowSyncUser)  // 如果提交有 flowSyncUser 则添加到 Set 中
  }  // 循环结束
  return [...names].sort()  // 将 Set 转为数组并排序后返回
})  // 计算属性结束

onMounted(async () => {  // 组件挂载后执行的生命周期钩子（异步）
  try {  // 尝试执行以下代码
    const res = await getOverview()  // 调用 API 获取系统概览数据
    if (res.success) {  // 如果请求成功
      stats.value[0].value = res.data.userCount || 0  // 更新"系统用户"统计值
      stats.value[1].value = res.data.projectCount || 0  // 更新"项目总数"统计值
      stats.value[2].value = res.data.taskCount || 0  // 更新"任务总数"统计值
      stats.value[3].value = res.data.summaryCount || 0  // 更新"总结总数"统计值
    }  // 成功判断结束
    const pRes = await getProjects()  // 调用 API 获取项目列表
    if (pRes.success) projects.value = pRes.data || []  // 请求成功则更新项目列表，否则为空数组
  } catch (e) { /* ignore */ }  // 捕获异常并忽略（静默处理）
})  // onMounted 钩子结束

async function loadCommits() {  // 异步函数：加载选中项目的 Git 提交记录
  if (!selectedProject.value) { allCommits.value = []; commits.value = []; return }  // 未选项目时清空所有提交数据并返回
  commitLoading.value = true  // 设置加载状态为 true
  try {  // 尝试执行以下代码
    const res = await githubProjectCommits(selectedProject.value)  // 调用 API 获取指定项目的 GitHub 提交记录
    allCommits.value = res.success ? (res.data || []) : []  // 请求成功则保存全部提交，否则空数组
    applyFilter()  // 应用当前用户筛选条件
  } finally { commitLoading.value = false }  // 无论成功失败，最终关闭加载状态
}  // loadCommits 函数结束

function applyFilter() {  // 函数：根据 filterUser 筛选提交记录
  commits.value = filterUser.value  // 如果填了筛选用户
    ? allCommits.value.filter(c => c.flowSyncUser === filterUser.value)  // 则过滤出该用户的提交
    : allCommits.value  // 否则显示全部提交
}  // applyFilter 函数结束

function jumpToUpload(item) {  // 函数：跳转到上传确认页面
  window.dispatchEvent(new CustomEvent('nav-github-tree', { detail: item }))  // 派发自定义事件 nav-github-tree，携带上传项数据
}  // jumpToUpload 函数结束
</script>  // 脚本结束
