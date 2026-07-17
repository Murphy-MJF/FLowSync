<!-- 模板区域开始 -->
<template>
  <!-- 根容器元素 -->
  <div>
    <!-- 页面标题"成员管理"，下方留白16px -->
    <h2 style="margin-bottom:16px">成员管理</h2>

    <!-- 管理员功能区 -->
    <template v-if="isAdmin">
      <!-- 邀请码卡片 -->
      <el-card style="margin-bottom:20px">
        <!-- 卡片头部：显示"邀请码管理" -->
        <template #header><span>邀请码管理</span></template>
        <!-- 内容区flex布局，垂直居中，子元素间距12px -->
        <div style="display:flex;align-items:center;gap:12px">
          <!-- "生成邀请码"按钮，主色调，带加载状态 -->
          <el-button type="primary" @click="handleGenerateCode" :loading="genLoading">生成邀请码</el-button>
          <!-- 若存在邀请码则显示 -->
          <template v-if="inviteCode">
            <!-- 邀请码标签：success绿色，大尺寸，等宽字体 -->
            <el-tag type="success" size="large" style="font-size:18px;font-family:monospace;padding:8px 16px">{{ inviteCode }}</el-tag>
            <!-- 有效期限提示，橙色文字，小号字体 -->
            <span style="color:#E6A23C;font-size:12px">2 分钟内有效</span>
          </template>
        </div>
      </el-card>
    </template>

    <!-- 成员列表卡片（所有人可见） -->
    <el-card style="margin-bottom:20px">
      <!-- 卡片头部：显示"成员列表" -->
      <template #header><span>成员列表</span></template>
      <!-- 成员表格：数据源users，带边框和斑马纹，带加载状态 -->
      <el-table :data="users" border stripe v-loading="loading">
        <!-- ID列，字段id，宽度60px -->
        <el-table-column prop="id" label="ID" width="60" />
        <!-- 用户名列，字段username，宽度120px -->
        <el-table-column prop="username" label="用户名" width="120" />
        <!-- 姓名列，字段realName，宽度120px -->
        <el-table-column prop="realName" label="姓名" width="120" />
        <!-- 角色列，字段role，宽度100px -->
        <el-table-column prop="role" label="角色" width="100">
          <!-- 自定义单元格模板，解构获取当前行数据row -->
          <template #default="{ row }">
            <!-- 角色标签：管理员→danger红色，负责人→warning橙色，其他→info灰色 -->
            <el-tag :type="row.role === '管理员' ? 'danger' : row.role === '负责人' ? 'warning' : 'info'" size="small">
              {{ row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 电话列，字段phone，宽度130px -->
        <el-table-column prop="phone" label="电话" width="130" />
        <!-- 邮箱列，字段email -->
        <el-table-column prop="email" label="邮箱" />
        <!-- AI额度列，字段aiQuota，宽度80px -->
        <el-table-column prop="aiQuota" label="AI额度" width="80" />
        <!-- 注册时间列，字段createTime，宽度160px -->
        <el-table-column prop="createTime" label="注册时间" width="160" />
        <!-- 操作列：仅管理员可见，宽度180px -->
        <el-table-column v-if="isAdmin" label="操作" width="180">
          <!-- 自定义单元格模板，解构获取当前行数据row -->
          <template #default="{ row }">
            <!-- 仅当角色不是"管理员"时显示操作按钮 -->
            <template v-if="row.role !== '管理员'">
              <!-- 若角色为"负责人"，显示"降为组员"按钮 -->
              <el-button v-if="row.role === '负责人'" size="small" type="warning"
                         @click="handleChangeRole(row, '组员')">降为组员</el-button>
              <!-- 若角色为"组员"，显示"升为负责人"按钮 -->
              <el-button v-if="row.role === '组员'" size="small" type="success"
                         @click="handleChangeRole(row, '负责人')">升为负责人</el-button>
            </template>
            <!-- 若角色为"管理员"，显示灰色横线占位 -->
            <span v-else style="color:#909399">—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 管理员：AI额度审批卡片 -->
    <el-card v-if="isAdmin" style="margin-bottom:20px">
      <!-- 卡片头部：flex布局，两端对齐，垂直居中 -->
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <!-- 标题"AI 额度审批" -->
          <span>AI 额度审批</span>
          <!-- "刷新"按钮，小尺寸 -->
          <el-button size="small" @click="fetchQuotaRequests">刷新</el-button>
        </div>
      </template>
      <!-- 审批列表表格：数据源quotaRequests，带边框和斑马纹，加载状态，空数据提示 -->
      <el-table :data="quotaRequests" border stripe v-loading="qrLoading" empty-text="暂无申请">
        <!-- ID列，字段id，宽度60px -->
        <el-table-column prop="id" label="ID" width="60" />
        <!-- 申请人列，字段userName，宽度100px -->
        <el-table-column prop="userName" label="申请人" width="100" />
        <!-- 申请次数列，字段requestedAmount，宽度90px -->
        <el-table-column prop="requestedAmount" label="申请次数" width="90" />
        <!-- 状态列，字段status，宽度100px -->
        <el-table-column prop="status" label="状态" width="100">
          <!-- 自定义单元格模板，解构获取当前行数据row -->
          <template #default="{ row }">
            <!-- 状态标签：approved→success绿色，denied→danger红色，pending→warning橙色 -->
            <el-tag :type="row.status === 'approved' ? 'success' : row.status === 'denied' ? 'danger' : 'warning'" size="small">
              <!-- 状态文字映射：approved→已批准，denied→已拒绝，pending→待审批 -->
              {{ row.status === 'approved' ? '已批准' : row.status === 'denied' ? '已拒绝' : '待审批' }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 时间列，字段createTime，宽度160px -->
        <el-table-column prop="createTime" label="时间" width="160" />
        <!-- 操作列 -->
        <el-table-column label="操作">
          <!-- 自定义单元格模板，解构获取当前行数据row -->
          <template #default="{ row }">
            <!-- 仅当状态为'pending'（待审批）时显示操作按钮 -->
            <template v-if="row.status === 'pending'">
              <!-- "批准"按钮，success绿色 -->
              <el-button size="small" type="success" @click="handleApprove(row, true)">批准</el-button>
              <!-- "拒绝"按钮，danger红色 -->
              <el-button size="small" type="danger" @click="handleApprove(row, false)">拒绝</el-button>
            </template>
            <!-- 非待审批状态显示灰色横线占位 -->
            <span v-else style="color:#909399">—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 转让弹窗：标题"转让项目所有权"，宽度500px -->
    <el-dialog title="转让项目所有权" v-model="transferVisible" width="500px">
      <!-- 警告提示文字：橙色，底部留白12px -->
      <p style="margin-bottom:12px;color:#E6A23C">⚠️ 该用户拥有 {{ transferProjects.length }} 个项目，降级前需指定接手人：</p>
      <!-- 待转让项目表格：小尺寸，最大高度200px，底部留白16px -->
      <el-table :data="transferProjects" border size="small" max-height="200" style="margin-bottom:16px">
        <!-- ID列，字段id，宽度60px -->
        <el-table-column prop="id" label="ID" width="60" />
        <!-- 项目名称列，字段name -->
        <el-table-column prop="name" label="项目名称" />
      </el-table>
      <!-- 接手人选择表单项 -->
      <el-form-item label="接手人" style="margin-top:8px">
        <!-- 下拉选择接手人，宽度100% -->
        <el-select v-model="selectedNewOwner" placeholder="选择接手人" style="width:100%">
          <!-- 遍历可选接手人列表，key为u.id，选项标签为"姓名 (角色)"，值为用户ID，禁用当前被降级的用户自身 -->
          <el-option v-for="u in transferCandidates" :key="u.id"
                     :label="u.realName + ' (' + u.role + ')'" :value="u.id"
                     :disabled="u.id === transferUserId" />
        </el-select>
      </el-form-item>
      <!-- 弹窗底部插槽 -->
      <template #footer>
        <!-- "取消"按钮，点击关闭弹窗 -->
        <el-button @click="transferVisible = false">取消</el-button>
        <!-- "确认转让并降级"按钮，主色调，未选择接手人时禁用 -->
        <el-button type="primary" @click="handleConfirmTransfer" :disabled="!selectedNewOwner">确认转让并降级</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<!-- 脚本区域开始（使用setup语法糖） -->
<script setup>
// 从Vue导入ref、computed、onMounted
import { ref, computed, onMounted } from 'vue'
// 从API模块导入所需接口方法
import { getUsers, getAdminUsers, genInviteCode, changeUserRole, getTransferCandidates, getQuotaRequests, approveQuota } from '../api'
// 从Element Plus导入消息提示和消息弹框
import { ElMessage, ElMessageBox } from 'element-plus'

// 定义组件props：currentUser，类型为Object
const props = defineProps({ currentUser: Object })
// 计算属性：根据currentUser的role判断是否为"管理员"
const isAdmin = computed(() => props.currentUser?.role === '管理员')

// 响应式引用：用户列表，初始为空数组
const users = ref([])
// 响应式引用：邀请码字符串，初始为空
const inviteCode = ref('')
// 响应式引用：成员列表加载状态，初始false
const loading = ref(false)
// 响应式引用：生成邀请码加载状态，初始false
const genLoading = ref(false)

// 响应式引用：转让弹窗显示状态，初始false
const transferVisible = ref(false)
// 响应式引用：被降级的用户ID，初始null
const transferUserId = ref(null)
// 响应式引用：待转让的项目列表，初始为空数组
const transferProjects = ref([])
// 响应式引用：可选接手人列表，初始为空数组
const transferCandidates = ref([])
// 响应式引用：选中的接手人ID，初始null
const selectedNewOwner = ref(null)

// 响应式引用：AI额度审批列表，初始为空数组
const quotaRequests = ref([])
// 响应式引用：审批列表加载状态，初始false
const qrLoading = ref(false)

// 生命周期钩子：组件挂载后异步加载数据
onMounted(async () => {
  // 开启成员列表加载状态
  loading.value = true
  // 开启try块
  try {
    // 并行请求三个接口：用户列表、转让候选人和审批列表（管理员用管理员接口，否则用普通接口）
    const [uRes, cRes, qRes] = await Promise.all([
      isAdmin.value ? getAdminUsers() : getUsers(),
      isAdmin.value ? getTransferCandidates() : Promise.resolve({ success: false, data: [] }),
      isAdmin.value ? getQuotaRequests() : Promise.resolve({ success: false, data: [] })
    ])
    // 若用户列表请求成功则赋值
    if (uRes.success) users.value = uRes.data || []
    // 若转让候选人请求成功则赋值
    if (cRes.success) transferCandidates.value = cRes.data || []
    // 若审批列表请求成功则赋值
    if (qRes.success) quotaRequests.value = qRes.data || []
  } finally { loading.value = false } // finally块：关闭成员列表加载状态
})

// 生成邀请码的异步方法
async function handleGenerateCode() {
  // 开启生成加载状态
  genLoading.value = true
  // 开启try块
  try {
    // 调用API生成邀请码
    const res = await genInviteCode()
    // 若生成成功
    if (res.success) {
      // 将邀请码赋值显示
      inviteCode.value = res.data.code
      // 120秒后自动清空邀请码
      setTimeout(() => { inviteCode.value = '' }, 120000)
    }
  } finally { genLoading.value = false } // finally块：关闭生成加载状态
}

// 更改用户角色的异步方法
async function handleChangeRole(row, newRole) {
  // 开启try块
  try {
    // 调用API更改用户角色
    const res = await changeUserRole({ userId: row.id, role: newRole })
    // 若更改成功
    if (res.success) {
      // 若返回数据标记需要转让项目
      if (res.data && res.data.needTransfer) {
        // 重新获取可选接手人列表
        const cRes = await getTransferCandidates()
        // 若获取成功则赋值
        if (cRes.success) transferCandidates.value = cRes.data || []
        // 记录被降级的用户ID
        transferUserId.value = row.id
        // 记录待转让的项目列表
        transferProjects.value = res.data.projects
        // 清空已选的接手人
        selectedNewOwner.value = null
        // 显示转让弹窗
        transferVisible.value = true
        // 暂不完成降级，等待用户选择接手人
        return
      }
      // 无需转让，直接弹出成功提示
      ElMessage.success(res.message)
      // 重新获取管理员用户列表以刷新表格
      const uRes = await getAdminUsers()
      // 若获取成功则赋值
      if (uRes.success) users.value = uRes.data || []
      // 若有项目被转让，额外提示转让数量
      if (res.data && res.data.transferred > 0) {
        ElMessage.success(`已转让 ${res.data.transferred} 个项目`)
      }
    }
  } catch (e) { /* handled */ } // 静默捕获异常
}

// 确认转让并降级的异步方法
async function handleConfirmTransfer() {
  // 若未选择接手人则弹出警告并返回
  if (!selectedNewOwner.value) { ElMessage.warning('请选择接手人'); return }
  // 开启try块
  try {
    // 调用API更改角色为"组员"并指定新负责人
    const res = await changeUserRole({
      userId: transferUserId.value, role: '组员', newOwnerId: selectedNewOwner.value
    })
    // 若更改成功
    if (res.success) {
      // 弹出成功提示"降级成功，项目已转让"
      ElMessage.success('降级成功，项目已转让')
      // 关闭转让弹窗
      transferVisible.value = false
      // 重新获取管理员用户列表以刷新表格
      const uRes = await getAdminUsers()
      // 若获取成功则赋值
      if (uRes.success) users.value = uRes.data || []
    }
  } catch (e) { /* handled */ } // 静默捕获异常
}

// 刷新审批列表的异步方法
async function fetchQuotaRequests() {
  // 开启审批列表加载状态
  qrLoading.value = true
  // 开启try块
  try {
    // 调用API获取AI额度审批请求列表
    const res = await getQuotaRequests()
    // 若获取成功则赋值
    if (res.success) quotaRequests.value = res.data || []
  } finally { qrLoading.value = false } // finally块：关闭审批列表加载状态
}

// 处理审批（批准或拒绝）的异步方法
async function handleApprove(row, approved) {
  // 默认审批额度为申请额度
  let amount = row.requestedAmount
  // 若为批准操作，弹出输入框让管理员调整批准次数
  if (approved) {
    // 开启try块
    try {
      // 弹出数字输入框，默认值为申请次数
      const { value } = await ElMessageBox.prompt('批准次数', '批准额度', { inputValue: amount, inputType: 'number' })
      // 将输入值转为整数
      amount = parseInt(value)
    } catch { return } // 用户取消则直接返回
  }
  // 开启try块
  try {
    // 调用API审批额度申请
    const res = await approveQuota(row.id, approved, amount)
    // 若审批成功
    if (res.success) {
      // 弹出成功提示
      ElMessage.success(res.message)
      // 刷新审批列表
      fetchQuotaRequests()
      // 重新获取管理员用户列表以同步AI额度
      const uRes = await getAdminUsers()
      // 若获取成功则赋值
      if (uRes.success) users.value = uRes.data || []
    }
  } catch (e) { /* handled */ } // 静默捕获异常
}
</script>
