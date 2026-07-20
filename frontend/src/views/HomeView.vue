<template>
  <!-- ==================== 登录/注册页面 ==================== -->
  <div v-if="!currentUser" class="login-container">
    <!-- 像素风格动态背景 -->
    <canvas ref="pixelCanvas" class="pixel-canvas"></canvas>
    <!-- 背景大字标题 -->
    <div class="hero-title">FlowSync</div>
    <div class="hero-subtitle">小组任务协同管理系统</div>

    <div class="login-card">
      <h1 v-if="!isRegisterMode" class="login-heading">登 录</h1>
      <h1 v-else class="login-heading">注 册</h1>

      <!-- 登录表单 -->
      <el-form v-if="!isRegisterMode" :model="loginForm" :rules="loginRules" ref="loginFormRef" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" @click="handleLogin" :loading="submitting">
            登 录
          </el-button>
        </el-form-item>
        <div class="login-hint">
          <!--<p>预置账号：admin/leader / member1 / member2，密码均为 123456</p> -->
          <p>
            没有账号？<el-button link type="primary" @click="switchToRegister">立即注册</el-button>
          </p>
        </div>
      </el-form>

      <!-- 注册表单 -->
      <el-form v-else :model="registerForm" :rules="registerRules" ref="registerFormRef" @keyup.enter="handleRegister">
        <el-form-item prop="username">
          <el-input v-model="registerForm.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item prop="realName">
          <el-input v-model="registerForm.realName" placeholder="真实姓名" size="large" />
        </el-form-item>
        <el-form-item prop="role">
          <el-select v-model="registerForm.role" placeholder="选择角色" size="large" style="width:100%">
            <el-option label="组员（直接加入团队）" value="组员" />
            <el-option label="项目负责人（需邀请码）" value="负责人" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="registerForm.role === '负责人'" prop="inviteCode">
          <el-input v-model="registerForm.inviteCode" placeholder="请输入邀请码" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="密码（至少4位）" size="large" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="success" size="large" style="width:100%" @click="handleRegister" :loading="submitting">
            注 册
          </el-button>
        </el-form-item>
        <div class="login-hint">
          <p style="color:#909399;font-size:12px">注册为项目负责人需要管理员提供的邀请码</p>
          <p>
            已有账号？<el-button link type="primary" @click="switchToLogin">返回登录</el-button>
          </p>
        </div>
      </el-form>
    </div>
  </div>

  <!-- ==================== 主界面 ==================== -->
  <div v-else class="main-layout">
    <!-- 侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h2>FlowSync</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        @select="handleMenuSelect"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <!-- 控制台 -->
        <el-menu-item index="dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>控制台</span>
        </el-menu-item>

        <!-- 业务功能 -->
        <el-menu-item index="projects">
          <el-icon><Folder /></el-icon>
          <span>项目管理</span>
        </el-menu-item>
        <el-menu-item v-if="isLeader || isAdmin" index="ai-plan">
          <el-icon><MagicStick /></el-icon>
          <span>AI 任务拆解</span>
        </el-menu-item>
        <el-menu-item index="tasks">
          <el-icon><List /></el-icon>
          <span>任务管理</span>
        </el-menu-item>
        <el-menu-item index="task-logs">
          <el-icon><Timer /></el-icon>
          <span>进度更新</span>
        </el-menu-item>
        <el-menu-item index="summaries">
          <el-icon><Document /></el-icon>
          <span>总结管理</span>
        </el-menu-item>

        <!-- 成员管理（管理员可见完整功能） -->
        <el-menu-item index="admin">
          <el-icon><UserFilled /></el-icon>
          <span>成员管理</span>
        </el-menu-item>

        <!-- GitHub（仅负责人和管理员可见） -->
        <el-menu-item v-if="isLeader || isAdmin" index="github">
          <el-icon><Connection /></el-icon>
          <span>GitHub 仓库</span>
        </el-menu-item>

        <!-- 系统信息 -->
        <el-menu-item index="profile">
          <el-icon><Setting /></el-icon>
          <span>个人信息</span>
        </el-menu-item>
      </el-menu>

      <!-- 底部用户信息 -->
      <div class="sidebar-footer">
        <span>{{ currentUser.realName }}</span>
        <el-tag :type="isLeader ? 'danger' : 'info'" size="small">{{ currentUser.role }}</el-tag>
        <el-popconfirm title="确认退出登录？" @confirm="handleLogout">
  <template #reference>
    <el-button text size="small" style="color:#bfcbd9;margin-left:8px">退出</el-button>
  </template>
</el-popconfirm>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="content">
      <component :is="currentPanel" :current-user="currentUser" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { login as apiLogin, register as apiRegister } from '../api'

// 面板组件
import DashboardPanel from '../components/DashboardPanel.vue'
import ProjectPanel from '../components/ProjectPanel.vue'
import AiTaskPlanPanel from '../components/AiTaskPlanPanel.vue'
import TaskPanel from '../components/TaskPanel.vue'
import TaskLogPanel from '../components/TaskLogPanel.vue'
import SummaryPanel from '../components/SummaryPanel.vue'
import AdminPanel from '../components/AdminPanel.vue'
import GitHubPanel from '../components/GitHubPanel.vue'
import ProfilePanel from '../components/ProfilePanel.vue'

const currentUser = ref(null)
const activeMenu = ref('dashboard')
const submitting = ref(false)
const isRegisterMode = ref(false)
const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', realName: '', role: '组员', inviteCode: '', password: '', confirmPassword: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const validateConfirmPwd = (rule, value, callback) => {
  if (value !== registerForm.value.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, message: '密码至少 4 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}

const isLeader = computed(() => currentUser.value?.role === '负责人')
const isAdmin = computed(() => currentUser.value?.role === '管理员')

// 菜单 → 面板映射
const panelMap = {
  'dashboard': DashboardPanel,
  'projects': ProjectPanel,
  'ai-plan': AiTaskPlanPanel,
  'tasks': TaskPanel,
  'task-logs': TaskLogPanel,
  'summaries': SummaryPanel,
  'github': GitHubPanel,
  'admin': AdminPanel,
  'profile': ProfilePanel
}

const currentPanel = computed(() => panelMap[activeMenu.value] || DashboardPanel)

// 像素风格背景动画
const pixelCanvas = ref(null)

function initPixelBackground() {
  const canvas = pixelCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  let w, h, cols, rows, grid, drops

  function resize() {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
    w = canvas.width; h = canvas.height
    const size = 14
    cols = Math.floor(w / size) + 1
    rows = Math.floor(h / size) + 1
    grid = new Array(cols).fill(null).map(() => new Array(rows).fill(0))
    drops = new Array(cols).fill(0).map(() => Math.random() * -rows)
  }
  resize()
  window.addEventListener('resize', resize)

  let mx = w / 2, my = h / 2
  canvas.addEventListener('mousemove', e => { mx = e.clientX; my = e.clientY })

  // 蓝色系像素风
  const bluePalette = ['#409EFF','#337ecc','#5cadff','#1a6dd4','#79bbff','#2b8af0']
  const size = 14

  function draw() {
    ctx.fillStyle = 'rgba(8, 15, 30, 0.06)'
    ctx.fillRect(0, 0, w, h)

    for (let c = 0; c < cols; c++) {
      const speed = 0.08 + Math.random() * 0.2
      drops[c] += speed
      const r = Math.floor(drops[c])
      if (drops[c] > rows + 10) drops[c] = -3

      if (r >= 0 && r < rows) {
        let px = c * size, py = r * size
        const dist = Math.hypot(px - mx, py - my)

        // 鼠标避让
        if (dist < 120 && dist > 0) {
          const angle = Math.atan2(py - my, px - mx)
          const push = (120 - dist) * 0.4
          px += Math.cos(angle) * push
          py += Math.sin(angle) * push
        }

        ctx.fillStyle = bluePalette[Math.floor(Math.random() * bluePalette.length)]
        ctx.globalAlpha = 0.12

        // 鼠标附近变亮
        if (dist < 100) {
          ctx.globalAlpha = 0.12 + (1 - dist / 100) * 0.45
        }
        ctx.fillRect(px, py, size, size)

        // 拖尾
        if (r > 0) {
          let tx = c * size, ty = (r - 1) * size
          const tdist = Math.hypot(tx - mx, ty - my)
          if (tdist < 120 && tdist > 0) {
            const a = Math.atan2(ty - my, tx - mx)
            tx += Math.cos(a) * (120 - tdist) * 0.3
            ty += Math.sin(a) * (120 - tdist) * 0.3
          }
          ctx.globalAlpha = 0.03
          ctx.fillRect(tx, ty, size, size)
        }
        ctx.globalAlpha = 1
      }
    }

    // 随机闪烁
    for (let i = 0; i < 12; i++) {
      const x = Math.floor(Math.random() * cols) * size
      const y = Math.floor(Math.random() * rows) * size
      ctx.fillStyle = bluePalette[Math.floor(Math.random() * bluePalette.length)]
      ctx.globalAlpha = 0.15 + Math.random() * 0.2
      ctx.fillRect(x, y, size, size)
    }
    ctx.globalAlpha = 1
    requestAnimationFrame(draw)
  }
  draw()
}

onMounted(() => {
  initPixelBackground()
  const stored = sessionStorage.getItem('currentUser')
  const token = sessionStorage.getItem('token')
  if (stored && token) {
    currentUser.value = JSON.parse(stored)
  }
  // 监听任务代码跳转事件
  window.addEventListener('nav-github-branch', (e) => {
    sessionStorage.setItem('githubOpenBranch', JSON.stringify(e.detail))
    activeMenu.value = 'github'
  })
})

function switchToRegister() {
  isRegisterMode.value = true
}

function switchToLogin() {
  isRegisterMode.value = false
}

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await apiLogin(loginForm.value)
    if (res.success) {
      const { token, user } = res.data
      sessionStorage.setItem('token', token)
      sessionStorage.setItem('currentUser', JSON.stringify(user))
      currentUser.value = user
      ElMessage.success(`欢迎，${user.realName}`)
    }
  } finally {
    submitting.value = false
  }
}

async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await apiRegister({
      username: registerForm.value.username,
      password: registerForm.value.password,
      realName: registerForm.value.realName,
      role: registerForm.value.role,
      inviteCode: registerForm.value.inviteCode
    })
    if (res.success) {
      ElMessage.success('注册成功，请登录')
      switchToLogin()
      loginForm.value.username = registerForm.value.username
      // 清空注册表单
      registerForm.value = { username: '', realName: '', password: '', confirmPassword: '' }
    }
  } finally {
    submitting.value = false
  }
}

function handleMenuSelect(index) {
  activeMenu.value = index
}

function handleLogout() {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('currentUser')
  sessionStorage.removeItem('publishedTasks')
  currentUser.value = null
  activeMenu.value = 'dashboard'
}
</script>

<style>
/* 全局重置 */
* { margin: 0; padding: 0; box-sizing: border-box; }

/* ======== 登录页 ======== */
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #0a0f1e;
  position: relative;
  overflow: hidden;
}
.pixel-canvas {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  z-index: 0;
}
.login-card {
  position: relative;
  z-index: 2;
  width: 400px;
  padding: 40px;
  background: rgba(10, 18, 36, 0.88);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 2px solid rgba(64, 158, 255, 0.35);
  box-shadow: 0 0 30px rgba(64, 158, 255, 0.2), inset 0 0 30px rgba(64, 158, 255, 0.05);
  image-rendering: pixelated;
}
/* 背景大字标题 */
.hero-title {
  position: absolute;
  top: 10%;
  left: 50%;
  transform: translateX(-50%);
  font-size: 72px;
  font-weight: 900;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 12px;
  pointer-events: none;
  user-select: none;
  text-shadow: 0 0 80px rgba(255,255,255,0.1);
}
.hero-subtitle {
  position: absolute;
  top: calc(12% + 100px);
  left: 50%;
  transform: translateX(-50%);
  font-size: 18px;
  font-weight: 300;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 8px;
  pointer-events: none;
  user-select: none;
}
.login-heading {
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  color: #79bbff;
  margin-bottom: 24px;
  text-shadow: 0 0 12px rgba(64, 158, 255, 0.4);
  font-family: 'Courier New', monospace;
}
.login-hint {
  text-align: center;
  color: rgba(121, 187, 255, 0.5);
  font-size: 12px;
  line-height: 1.8;
}

/* ======== 主界面 ======== */
.main-layout {
  display: flex;
  height: 100vh;
}

/* 侧边栏 */
.sidebar {
  width: 220px;
  background: linear-gradient(180deg, #1e2a3a 0%, #263445 100%);
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 16px rgba(0,0,0,0.12);
}
.sidebar-header {
  padding: 20px;
  text-align: center;
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.sidebar-header h2 {
  font-size: 20px;
}
.sidebar .el-menu {
  flex: 1;
  border-right: none;
}
.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255,255,255,0.1);
  display: flex;
  align-items: center;
  color: #bfcbd9;
  font-size: 13px;
  gap: 6px;
  flex-wrap: wrap;
}

/* 内容区 */
.content {
  flex: 1;
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  overflow-y: auto;
}
</style>
