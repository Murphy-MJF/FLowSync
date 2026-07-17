<template>  <!-- 模板区域：定义组件的HTML结构 -->
  <div>  <!-- 根容器元素 -->
    <h2 style="margin-bottom:16px">个人信息</h2>  <!-- 页面标题"个人信息"，底部外边距16px -->
    <el-card style="max-width:600px">  <!-- Element Plus卡片组件，最大宽度限制600px -->
      <template #header>基本信息</template>  <!-- 卡片头部插槽，显示"基本信息"文字 -->
      <el-descriptions :column="1" border>  <!-- 描述列表组件，单列布局，带边框样式 -->
        <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>  <!-- 描述项：显示当前登录用户的用户名 -->
        <el-descriptions-item label="真实姓名">{{ currentUser.realName }}</el-descriptions-item>  <!-- 描述项：显示当前登录用户的真实姓名 -->
        <el-descriptions-item label="角色">  <!-- 描述项：显示用户角色 -->
          <el-tag :type="currentUser.role === '负责人' ? 'danger' : 'info'">{{ currentUser.role }}</el-tag>  <!-- 角色标签：负责人显示红色(danger)，其他角色显示灰色(info) -->
        </el-descriptions-item>
        <el-descriptions-item label="电话">  <!-- 描述项：显示用户电话 -->
          <span style="margin-right:12px">{{ currentUser.phone || '未填写' }}</span>  <!-- 显示电话号码，为空时显示"未填写"，右侧留12px间距 -->
          <el-button size="small" @click="openDialog('phone')">修改</el-button>  <!-- 小尺寸修改按钮，点击触发打开电话修改弹窗 -->
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">  <!-- 描述项：显示用户邮箱 -->
          <span style="margin-right:12px">{{ currentUser.email || '未填写' }}</span>  <!-- 显示邮箱地址，为空时显示"未填写"，右侧留12px间距 -->
          <el-button size="small" @click="openDialog('email')">修改</el-button>  <!-- 小尺寸修改按钮，点击触发打开邮箱修改弹窗 -->
        </el-descriptions-item>
        <el-descriptions-item label="密码">  <!-- 描述项：显示密码（脱敏） -->
          <span style="margin-right:12px">••••••</span>  <!-- 密码以圆点遮蔽显示，右侧留12px间距 -->
          <el-button size="small" @click="openDialog('password')">修改</el-button>  <!-- 小尺寸修改按钮，点击触发打开密码修改弹窗 -->
        </el-descriptions-item>
      </el-descriptions>  <!-- 描述列表结束 -->
    </el-card>  <!-- 个人信息卡片结束 -->

    <!-- 修改弹窗：用于修改电话、邮箱、密码 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="400px" @close="resetDialog">  <!-- 弹窗组件：标题动态计算、双向绑定显示状态、宽度400px、关闭时重置表单 -->

      <!-- 修改电话表单 -->
      <el-form v-if="dialogType === 'phone'" :model="phoneForm" label-width="80px">  <!-- 当弹窗类型为phone时显示，绑定电话表单数据，标签宽度80px -->
        <el-form-item label="新电话">  <!-- 表单项，标签文字"新电话" -->
          <el-input v-model="phoneForm.value" placeholder="请输入联系电话" />  <!-- 输入框双向绑定电话值，占位提示"请输入联系电话" -->
        </el-form-item>
        <el-form-item>  <!-- 表单按钮区域 -->
          <el-button type="primary" @click="handleSavePhone" :loading="saving">保存</el-button>  <!-- 主色调保存按钮，点击保存电话，保存中显示loading动效 -->
          <el-button @click="dialogVisible = false">取消</el-button>  <!-- 取消按钮，点击关闭弹窗 -->
        </el-form-item>
      </el-form>  <!-- 修改电话表单结束 -->

      <!-- 修改邮箱表单 -->
      <el-form v-if="dialogType === 'email'" :model="emailForm" label-width="80px">  <!-- 当弹窗类型为email时显示，绑定邮箱表单数据，标签宽度80px -->
        <el-form-item label="新邮箱">  <!-- 表单项，标签文字"新邮箱" -->
          <el-input v-model="emailForm.value" placeholder="请输入电子邮箱" />  <!-- 输入框双向绑定邮箱值，占位提示"请输入电子邮箱" -->
        </el-form-item>
        <el-form-item>  <!-- 表单按钮区域 -->
          <el-button type="primary" @click="handleSaveEmail" :loading="saving">保存</el-button>  <!-- 主色调保存按钮，点击保存邮箱，保存中显示loading动效 -->
          <el-button @click="dialogVisible = false">取消</el-button>  <!-- 取消按钮，点击关闭弹窗 -->
        </el-form-item>
      </el-form>  <!-- 修改邮箱表单结束 -->

      <!-- 修改密码表单 -->
      <el-form v-if="dialogType === 'password'" :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px">  <!-- 当弹窗类型为password时显示，绑定密码表单数据和校验规则，ref用于表单验证，标签宽度100px -->
        <el-form-item label="原密码" prop="oldPassword">  <!-- 表单项标签"原密码"，prop指向校验规则中的oldPassword字段 -->
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />  <!-- 密码输入框，可点击切换密码显示/隐藏 -->
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">  <!-- 表单项标签"新密码"，prop指向校验规则中的newPassword字段 -->
          <el-input v-model="pwdForm.newPassword" type="password" show-password />  <!-- 密码输入框，可点击切换密码显示/隐藏 -->
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">  <!-- 表单项标签"确认新密码"，prop指向校验规则中的confirmPassword字段 -->
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />  <!-- 密码输入框，可点击切换密码显示/隐藏 -->
        </el-form-item>
        <el-form-item>  <!-- 表单按钮区域 -->
          <el-button type="primary" @click="handleSavePwd" :loading="saving">保存</el-button>  <!-- 主色调保存按钮，点击保存密码，保存中显示loading动效 -->
          <el-button @click="dialogVisible = false">取消</el-button>  <!-- 取消按钮，点击关闭弹窗 -->
        </el-form-item>
      </el-form>  <!-- 修改密码表单结束 -->
    </el-dialog>  <!-- 修改信息弹窗结束 -->

    <!-- GitHub 连接卡片 -->
    <el-card style="max-width:600px;margin-top:20px">  <!-- GitHub连接卡片，最大宽度600px，顶部外边距20px -->
      <template #header>  <!-- 卡片头部插槽 -->
        <div style="display:flex;justify-content:space-between;align-items:center">  <!-- 弹性布局容器：水平排列、两端对齐、垂直居中 -->
          <span>GitHub 连接</span>  <!-- 卡片标题"GitHub 连接" -->
          <el-tag :type="ghConnected ? 'success' : 'info'">{{ ghConnected ? '已连接' : '未连接' }}</el-tag>  <!-- 连接状态标签：已连接为绿色(success)，未连接为灰色(info) -->
        </div>
      </template>
      <div v-if="ghConnected">  <!-- 已连接GitHub时显示的内容 -->
        <p>已连接账号：<strong>{{ ghLogin }}</strong></p>  <!-- 显示已连接的GitHub账号名，用户名加粗 -->
        <el-button size="small" type="danger" @click="handleGithubRevoke" :loading="ghLoading">解除绑定</el-button>  <!-- 解除绑定按钮，危险色调，操作中显示loading -->
      </div>
      <div v-else>  <!-- 未连接GitHub时显示的内容 -->
        <p style="color:#909399;margin-bottom:12px">连接 GitHub 后可绑定仓库并查看代码状态</p>  <!-- 引导提示文字，灰色字体，底部外边距12px -->
        <el-button size="small" type="primary" @click="handleGithubConnect" :loading="ghLoading">连接 GitHub</el-button>  <!-- 连接按钮，主色调，操作中显示loading -->
      </div>
    </el-card>  <!-- GitHub连接卡片结束 -->
  </div>  <!-- 根容器结束 -->
</template>  <!-- 模板区域结束 -->

<script setup>  // Vue3 Composition API 语法糖写法
import { ref, reactive, computed, onMounted } from 'vue'  // 从Vue中导入响应式API：ref、reactive、computed、生命周期钩子onMounted
import { updatePassword, updateProfile, githubStatus, githubConnect, githubRevoke } from '../api'  // 从api模块导入：更新密码、更新资料、GitHub状态、连接GitHub、解绑GitHub等接口方法
import { ElMessage, ElMessageBox } from 'element-plus'  // 从Element Plus导入消息提示组件和确认弹窗组件

const props = defineProps({ currentUser: Object })  // 定义组件props，接收一个currentUser对象

const dialogVisible = ref(false)  // 控制修改信息弹窗显示/隐藏的响应式状态，默认为隐藏
const dialogType = ref('')  // 当前弹窗类型：'phone'手机、'email'邮箱 或 'password'密码，默认为空字符串
const saving = ref(false)  // 保存操作的loading加载状态，默认为false
const pwdFormRef = ref(null)  // 密码表单组件的模板引用，用于触发表单校验

const phoneForm = reactive({ value: '' })  // 电话表单响应式数据对象，value字段初始为空字符串
const emailForm = reactive({ value: '' })  // 邮箱表单响应式数据对象，value字段初始为空字符串
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })  // 密码表单响应式数据对象，包含原密码、新密码、确认密码三个字段

const dialogTitle = computed(() => {  // 计算属性：根据弹窗类型动态返回弹窗标题文本
  return dialogType.value === 'phone' ? '修改电话' :  // 类型为phone返回"修改电话"
         dialogType.value === 'email' ? '修改邮箱' : '修改密码'  // 类型为email返回"修改邮箱"，否则返回"修改密码"
})  // 计算属性结束

const pwdRules = {  // 密码表单的校验规则配置对象
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],  // 原密码字段：必填校验，失焦触发，提示"请输入原密码"
  newPassword: [  // 新密码字段校验规则数组
    { required: true, message: '请输入新密码', trigger: 'blur' },  // 第一条规则：必填校验，失焦触发
    { min: 4, message: '密码至少 4 位', trigger: 'blur' }  // 第二条规则：最小长度4位，失焦触发
  ],  // 新密码规则结束
  confirmPassword: [  // 确认密码字段校验规则数组
    { required: true, message: '请确认新密码', trigger: 'blur' },  // 第一条规则：必填校验，失焦触发
    { validator: (rule, value, callback) => {  // 第二条规则：自定义校验器函数
        if (value !== pwdForm.newPassword) callback(new Error('两次密码不一致'))  // 如果与新密码不一致，回调错误信息"两次密码不一致"
        else callback()  // 密码一致则调用callback()表示校验通过
      }, trigger: 'blur' }  // 失焦时触发自定义校验
  ]  // 确认密码规则结束
}  // 密码校验规则对象结束

function openDialog(type) {  // 打开修改信息弹窗的函数，参数type为弹窗类型字符串
  dialogType.value = type  // 设置当前弹窗类型
  if (type === 'phone') phoneForm.value = props.currentUser.phone || ''  // 如果是电话弹窗，预填当前用户电话，无则填空字符串
  if (type === 'email') emailForm.value = props.currentUser.email || ''  // 如果是邮箱弹窗，预填当前用户邮箱，无则填空字符串
  dialogVisible.value = true  // 设置弹窗为显示状态
}  // openDialog函数结束

// GitHub 相关状态和逻辑
const ghConnected = ref(false)  // GitHub是否已连接的响应式状态，默认为未连接
const ghLogin = ref('')  // GitHub登录用户名的响应式状态，默认为空字符串
const ghLoading = ref(false)  // GitHub相关操作的loading加载状态，默认为false

onMounted(async () => {  // 组件挂载完成后执行的异步生命周期钩子
  const res = await githubStatus()  // 调用接口获取当前GitHub连接状态
  if (res.success && res.data) {  // 如果接口返回成功且有数据
    ghConnected.value = res.data.connected  // 更新连接状态为接口返回的值
    ghLogin.value = res.data.login || ''  // 更新GitHub用户名，无则设为空字符串
  }  // 条件判断结束
})  // onMounted钩子结束

async function handleGithubConnect() {  // 处理GitHub连接操作的异步函数
  ghLoading.value = true  // 开启loading加载状态
  try {  // 开始try代码块
    const res = await githubConnect(window.location.origin)  // 调用GitHub连接接口，传入当前页面域名作为OAuth回调地址
    if (res.success) {  // 如果接口调用成功
      const win = window.open(res.data.url, 'github-oauth', 'width=800,height=700')  // 打开GitHub OAuth授权弹窗，指定宽高800x700
      // 监听OAuth回调页面通过postMessage发来的授权成功消息（自动模式）
      const onMessage = (e) => {  // 定义消息事件处理回调函数
        if (e.data?.type === 'github-connected') {  // 如果收到的消息类型为github-connected
          window.removeEventListener('message', onMessage)  // 移除消息事件监听，避免重复处理
          refreshGithubStatus()  // 刷新GitHub连接状态
        }  // 消息类型判断结束
      }  // onMessage回调函数结束
      window.addEventListener('message', onMessage)  // 注册message事件监听，接收OAuth回调页面消息
      // 兜底方案：如果postMessage未触发，通过轮询检测窗口关闭来刷新状态
      const checkInterval = setInterval(async () => {  // 设置定时器，每秒执行一次检查
        if (win.closed) {  // 如果OAuth授权窗口已关闭
          clearInterval(checkInterval)  // 清除定时器，停止轮询
          window.removeEventListener('message', onMessage)  // 移除消息事件监听
          refreshGithubStatus()  // 刷新GitHub连接状态（兜底逻辑）
        }  // 窗口关闭判断结束
      }, 1000)  // 轮询间隔为1000毫秒（1秒）
    }  // 接口成功判断结束
  } finally { ghLoading.value = false }  // finally块：无论成功或失败，最终关闭loading状态
}  // handleGithubConnect函数结束

async function refreshGithubStatus() {  // 刷新GitHub连接状态的异步函数
  const res = await githubStatus()  // 调用接口获取GitHub连接状态
  if (res.success && res.data) {  // 如果接口返回成功且有数据
    ghConnected.value = res.data.connected  // 更新连接状态
    ghLogin.value = res.data.login || ''  // 更新GitHub用户名，无则设为空字符串
    if (ghConnected.value) ElMessage.success('GitHub 已连接')  // 如果已连接，弹出成功提示消息
  }  // 条件判断结束
}  // refreshGithubStatus函数结束

async function handleGithubRevoke() {  // 处理解除GitHub绑定的异步函数
  try {  // 开始try代码块
    await ElMessageBox.confirm('确认解除 GitHub 绑定？', '提示', { type: 'warning' })  // 弹出警告确认对话框，标题"提示"，类型为warning
  } catch { return }  // 用户取消操作时捕获异常并直接返回，不执行后续解绑逻辑
  ghLoading.value = true  // 开启loading加载状态
  try {  // 开始try代码块
    const res = await githubRevoke()  // 调用解绑GitHub的接口
    if (res.success) {  // 如果解绑成功
      ghConnected.value = false  // 将连接状态设为未连接
      ghLogin.value = ''  // 清空GitHub用户名
      ElMessage.success('已解除绑定')  // 弹出成功提示消息
    }  // 成功判断结束
  } finally { ghLoading.value = false }  // finally块：无论成功或失败，最终关闭loading状态
}  // handleGithubRevoke函数结束

function resetDialog() {  // 弹窗关闭时重置密码表单数据的函数
  pwdForm.oldPassword = ''  // 清空原密码输入框
  pwdForm.newPassword = ''  // 清空新密码输入框
  pwdForm.confirmPassword = ''  // 清空确认密码输入框
}  // resetDialog函数结束

function syncSessionUser(updates) {  // 将用户信息更新同步到sessionStorage的函数，参数updates为要更新的字段键值对对象
  const stored = JSON.parse(sessionStorage.getItem('currentUser'))  // 从sessionStorage读取currentUser的JSON字符串并解析为对象
  Object.assign(stored, updates)  // 将更新字段合并到已存储的用户对象上
  sessionStorage.setItem('currentUser', JSON.stringify(stored))  // 将合并后的对象序列化为JSON字符串重新存入sessionStorage
}  // syncSessionUser函数结束

async function handleSavePhone() {  // 处理保存电话修改的异步函数
  saving.value = true  // 开启保存loading状态
  try {  // 开始try代码块
    const res = await updateProfile({ phone: phoneForm.value, email: props.currentUser.email })  // 调用更新资料接口，传入新电话和当前邮箱
    if (res.success) {  // 如果更新成功
      syncSessionUser({ phone: phoneForm.value })  // 同步新电话到sessionStorage
      ElMessage.success('电话修改成功')  // 弹出成功提示"电话修改成功"
      dialogVisible.value = false  // 关闭弹窗
    }  // 成功判断结束
  } finally { saving.value = false }  // finally块：无论成功或失败，最终关闭保存loading状态
}  // handleSavePhone函数结束

async function handleSaveEmail() {  // 处理保存邮箱修改的异步函数
  saving.value = true  // 开启保存loading状态
  try {  // 开始try代码块
    const res = await updateProfile({ phone: props.currentUser.phone, email: emailForm.value })  // 调用更新资料接口，传入当前电话和新邮箱
    if (res.success) {  // 如果更新成功
      syncSessionUser({ email: emailForm.value })  // 同步新邮箱到sessionStorage
      ElMessage.success('邮箱修改成功')  // 弹出成功提示"邮箱修改成功"
      dialogVisible.value = false  // 关闭弹窗
    }  // 成功判断结束
  } finally { saving.value = false }  // finally块：无论成功或失败，最终关闭保存loading状态
}  // handleSaveEmail函数结束

async function handleSavePwd() {  // 处理保存密码修改的异步函数
  const valid = await pwdFormRef.value.validate().catch(() => false)  // 执行密码表单校验，校验失败时捕获异常并返回false
  if (!valid) return  // 如果校验未通过，直接返回，不执行后续保存逻辑
  saving.value = true  // 开启保存loading状态
  try {  // 开始try代码块
    const res = await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })  // 调用修改密码接口，传入原密码和新密码
    if (res.success) {  // 如果修改成功
      ElMessage.success('密码修改成功')  // 弹出成功提示"密码修改成功"
      dialogVisible.value = false  // 关闭弹窗
    }  // 成功判断结束
  } finally { saving.value = false }  // finally块：无论成功或失败，最终关闭保存loading状态
}  // handleSavePwd函数结束
</script>  <!-- script区域结束（此组件无style部分） -->
