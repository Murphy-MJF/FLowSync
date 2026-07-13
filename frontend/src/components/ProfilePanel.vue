<template>
  <div>
    <h2 style="margin-bottom:16px">个人信息</h2>
    <el-card style="max-width:600px">
      <template #header>基本信息</template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ currentUser.realName }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="currentUser.role === '负责人' ? 'danger' : 'info'">{{ currentUser.role }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="电话">
          <span style="margin-right:12px">{{ currentUser.phone || '未填写' }}</span>
          <el-button size="small" @click="openDialog('phone')">修改</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">
          <span style="margin-right:12px">{{ currentUser.email || '未填写' }}</span>
          <el-button size="small" @click="openDialog('email')">修改</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="密码">
          <span style="margin-right:12px">••••••</span>
          <el-button size="small" @click="openDialog('password')">修改</el-button>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 修改弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="400px" @close="resetDialog">
      <!-- 修改电话 -->
      <el-form v-if="dialogType === 'phone'" :model="phoneForm" label-width="80px">
        <el-form-item label="新电话">
          <el-input v-model="phoneForm.value" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSavePhone" :loading="saving">保存</el-button>
          <el-button @click="dialogVisible = false">取消</el-button>
        </el-form-item>
      </el-form>

      <!-- 修改邮箱 -->
      <el-form v-if="dialogType === 'email'" :model="emailForm" label-width="80px">
        <el-form-item label="新邮箱">
          <el-input v-model="emailForm.value" placeholder="请输入电子邮箱" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSaveEmail" :loading="saving">保存</el-button>
          <el-button @click="dialogVisible = false">取消</el-button>
        </el-form-item>
      </el-form>

      <!-- 修改密码 -->
      <el-form v-if="dialogType === 'password'" :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSavePwd" :loading="saving">保存</el-button>
          <el-button @click="dialogVisible = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- GitHub 连接 -->
    <el-card style="max-width:600px;margin-top:20px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>GitHub 连接</span>
          <el-tag :type="ghConnected ? 'success' : 'info'">{{ ghConnected ? '已连接' : '未连接' }}</el-tag>
        </div>
      </template>
      <div v-if="ghConnected">
        <p>已连接账号：<strong>{{ ghLogin }}</strong></p>
        <el-button size="small" type="danger" @click="handleGithubRevoke" :loading="ghLoading">解除绑定</el-button>
      </div>
      <div v-else>
        <p style="color:#909399;margin-bottom:12px">连接 GitHub 后可绑定仓库并查看代码状态</p>
        <el-button size="small" type="primary" @click="handleGithubConnect" :loading="ghLoading">连接 GitHub</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { updatePassword, updateProfile, githubStatus, githubConnect, githubCallback, githubRevoke } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({ currentUser: Object })

const dialogVisible = ref(false)
const dialogType = ref('')
const saving = ref(false)
const pwdFormRef = ref(null)

const phoneForm = reactive({ value: '' })
const emailForm = reactive({ value: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const dialogTitle = computed(() => {
  return dialogType.value === 'phone' ? '修改电话' :
         dialogType.value === 'email' ? '修改邮箱' : '修改密码'
})

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 4, message: '密码至少 4 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次密码不一致'))
        else callback()
      }, trigger: 'blur' }
  ]
}

function openDialog(type) {
  dialogType.value = type
  if (type === 'phone') phoneForm.value = props.currentUser.phone || ''
  if (type === 'email') emailForm.value = props.currentUser.email || ''
  dialogVisible.value = true
}

// GitHub
const ghConnected = ref(false)
const ghLogin = ref('')
const ghLoading = ref(false)

onMounted(async () => {
  const res = await githubStatus()
  if (res.success && res.data) {
    ghConnected.value = res.data.connected
    ghLogin.value = res.data.login || ''
  }
})

async function handleGithubConnect() {
  ghLoading.value = true
  try {
    const res = await githubConnect(window.location.origin)
    if (res.success) {
      // 弹出 GitHub 授权窗口
      const win = window.open(res.data.url, 'github-oauth', 'width=800,height=700')
      const checkInterval = setInterval(async () => {
        if (win.closed) {
          clearInterval(checkInterval)
          // 窗口关闭后，提示用户输入 code（简化方案）
          try {
            const { value } = await ElMessageBox.prompt('请粘贴 GitHub 返回的授权码（URL中 ?code= 后面的部分）', '输入授权码')
            if (value) {
              const cbRes = await githubCallback(value.trim())
              if (cbRes.success) {
                ghConnected.value = true
                ghLogin.value = cbRes.data.login
                ElMessage.success('GitHub 已连接')
              }
            }
          } catch { /* cancelled */ }
        }
      }, 500)
    }
  } finally { ghLoading.value = false }
}

async function handleGithubRevoke() {
  try {
    await ElMessageBox.confirm('确认解除 GitHub 绑定？', '提示', { type: 'warning' })
  } catch { return }
  ghLoading.value = true
  try {
    const res = await githubRevoke()
    if (res.success) {
      ghConnected.value = false
      ghLogin.value = ''
      ElMessage.success('已解除绑定')
    }
  } finally { ghLoading.value = false }
}

function resetDialog() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
}

function syncSessionUser(updates) {
  const stored = JSON.parse(sessionStorage.getItem('currentUser'))
  Object.assign(stored, updates)
  sessionStorage.setItem('currentUser', JSON.stringify(stored))
}

async function handleSavePhone() {
  saving.value = true
  try {
    const res = await updateProfile({ phone: phoneForm.value, email: props.currentUser.email })
    if (res.success) {
      syncSessionUser({ phone: phoneForm.value })
      ElMessage.success('电话修改成功')
      dialogVisible.value = false
    }
  } finally { saving.value = false }
}

async function handleSaveEmail() {
  saving.value = true
  try {
    const res = await updateProfile({ phone: props.currentUser.phone, email: emailForm.value })
    if (res.success) {
      syncSessionUser({ email: emailForm.value })
      ElMessage.success('邮箱修改成功')
      dialogVisible.value = false
    }
  } finally { saving.value = false }
}

async function handleSavePwd() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const res = await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    if (res.success) {
      ElMessage.success('密码修改成功')
      dialogVisible.value = false
    }
  } finally { saving.value = false }
}
</script>
