<template>
  <!-- ==================== 登录/注册页面 ==================== -->
  <div v-if="!currentUser" class="login-container">
    <!-- 像素风格动态背景 -->
    <canvas ref="pixelCanvas" class="pixel-canvas"></canvas>
    <!-- 背景大字标题 -->
    <div class="hero-title">FlowSync</div>
    <div class="hero-subtitle">小组任务协同管理系统</div>

    <!-- 翻转卡片 -->
    <div class="wrapper">
      <!-- 登录/注册 切换滑块 -->
      <div class="switch" @click="isRegisterMode = !isRegisterMode">
        <div class="slider" :class="{ checked: isRegisterMode }"></div>
        <div class="card-side" :class="{ flipped: isRegisterMode }"></div>
      </div>

      <!-- 3D 翻转卡片 -->
      <div class="flip-card__inner" :class="{ flipped: isRegisterMode }">
        <!-- 正面：登录 -->
        <div class="flip-card__front">
          <div class="flip-title">登 录</div>
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" class="flip-card__form"
                   @keyup.enter="handleLogin">
            <el-input v-model="loginForm.username" placeholder="用户名" size="large" class="flip-input" />
            <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large"
                      show-password class="flip-input" />
            <el-button type="primary" size="large" class="flip-btn" @click="handleLogin" :loading="submitting">
              登 录
            </el-button>
          </el-form>
          <div class="flip-hint">
            没有账号？<el-button link type="primary" @click="switchToRegister">立即注册</el-button>
          </div>
        </div>

        <!-- 背面：注册 -->
        <div class="flip-card__back">
          <div class="flip-title">注 册</div>
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" class="flip-card__form"
                   @keyup.enter="handleRegister">
            <el-input v-model="registerForm.username" placeholder="用户名" size="large" class="flip-input" />
            <el-input v-model="registerForm.realName" placeholder="真实姓名" size="large" class="flip-input" />
            <el-select v-model="registerForm.role" placeholder="选择角色" size="large"
                       class="flip-input" :popper-options="{ strategy: 'fixed' }">
              <el-option label="组员（直接加入团队）" value="组员" />
              <el-option label="项目负责人（需邀请码）" value="负责人" />
            </el-select>
            <el-input v-if="registerForm.role === '负责人'" v-model="registerForm.inviteCode"
                      placeholder="请输入邀请码" size="large" class="flip-input" />
            <el-input v-model="registerForm.password" type="password" placeholder="密码（至少4位）" size="large"
                      show-password class="flip-input" />
            <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" size="large"
                      show-password class="flip-input" />
            <el-button type="success" size="large" class="flip-btn" @click="handleRegister" :loading="submitting">
              注 册
            </el-button>
          </el-form>
          <div class="flip-hint">
            <span class="flip-hint-tip">注册为项目负责人需要管理员提供的邀请码</span>
            <el-button link type="primary" @click="switchToLogin">返回登录</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- ==================== 主界面 ==================== -->
  <div v-else class="main-layout">
    <!-- 像素风格背景（无鼠标交互） -->
    <canvas ref="mainCanvas" class="main-canvas"></canvas>

    <!-- 顶部悬浮导航栏 -->
    <div class="top-nav">
      <div class="nav-left">
        <div class="nav-avatar" :style="{ background: !sidebarAvatarImg ? sidebarAvatarColor : 'transparent' }"
             @click="showProfile" title="个人信息">
          <img v-if="sidebarAvatarImg" :src="sidebarAvatarImg" style="width:100%;height:100%;border-radius:50%;object-fit:cover" />
          <span v-else>{{ sidebarAvatarText }}</span>
        </div>
        <span class="nav-brand">FlowSync</span>
      </div>

      <div class="nav-center">
        <span class="nav-item" :class="{ active: activeMenu === 'dashboard' }" @click="handleMenuSelect('dashboard')">
          <el-icon><DataAnalysis /></el-icon> 控制台
        </span>
        <span v-if="isLeader || isAdmin" class="nav-item" :class="{ active: activeMenu === 'ai-plan' }" @click="handleMenuSelect('ai-plan')">
          <el-icon><MagicStick /></el-icon> AI 任务拆解
        </span>
        <span class="nav-item" :class="{ active: activeMenu === 'task-logs' }" @click="handleMenuSelect('task-logs')">
          <el-icon><Timer /></el-icon> 进度更新
        </span>
        <span v-if="isLeader || isAdmin" class="nav-item" :class="{ active: activeMenu === 'github' }" @click="handleMenuSelect('github')">
          <el-icon><Connection /></el-icon> GitHub 仓库
        </span>
      </div>

      <div class="nav-right">
        <span class="nav-user">{{ currentUser.realName }}</span>
        <el-tag :type="isLeader ? 'danger' : 'info'" size="small">{{ currentUser.role }}</el-tag>
        <el-popconfirm title="确认退出登录？" @confirm="handleLogout">
          <template #reference>
            <el-button text size="small" class="nav-logout">退出</el-button>
          </template>
        </el-popconfirm>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="content">
      <component :is="currentPanel" :current-user="currentUser" @navigate="handleMenuSelect" />
    </div>

    <!-- 个人信息抽屉（点击头像展开） -->
    <el-drawer v-model="profileVisible" title="个人信息" direction="ltr" size="420px">
      <ProfilePanel :current-user="currentUser" />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
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
import { avatarImage, avatarColor as storeAvatarColor } from '../store/avatarStore'

const currentUser = ref(null)
const activeMenu = ref('dashboard')
const profileVisible = ref(false)
function showProfile() { profileVisible.value = true }
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

const sidebarAvatarText = computed(() => (currentUser.value?.realName || 'U').charAt(0).toUpperCase())
const sidebarAvatarImg = computed(() => avatarImage.value || currentUser.value?.avatar || '')
const sidebarAvatarColor = computed(() => {
  const sc = storeAvatarColor.value
  if (sc) return sc
  const h = (s) => { let h = 0; for (let i = 0; i < (s||'').length; i++) h = ((h << 5) - h) + s.charCodeAt(i); return Math.abs(h) }
  const colors = ['#409EFF','#67C23A','#E6A23C','#F56C6C','#00d4ff','#8b5cf6']
  return colors[h(currentUser.value?.realName) % colors.length]
})

// 菜单 → 面板映射
const panelMap = {
  'dashboard': DashboardPanel,
  'projects': ProjectPanel,
  'ai-plan': AiTaskPlanPanel,
  'tasks': TaskPanel,
  'task-logs': TaskLogPanel,
  'summaries': SummaryPanel,
  'github': GitHubPanel,
  'admin': AdminPanel
}

const currentPanel = computed(() => panelMap[activeMenu.value] || DashboardPanel)

// 像素风格背景动画（登录页，含鼠标交互）
const pixelCanvas = ref(null)

function initPixelBackground() {
  const canvas = pixelCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  let w, h, cols, rows, drops

  function resize() {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
    w = canvas.width; h = canvas.height
    const size = 14
    cols = Math.floor(w / size) + 1
    rows = Math.floor(h / size) + 1
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

// 主界面像素背景（纯下落，无鼠标交互）
const mainCanvas = ref(null)

function initMainBackground() {
  const canvas = mainCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  let w, h, cols, rows, drops

  function resize() {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
    w = canvas.width; h = canvas.height
    const size = 14
    cols = Math.floor(w / size) + 1
    rows = Math.floor(h / size) + 1
    drops = new Array(cols).fill(0).map(() => Math.random() * -rows)
  }
  resize()
  window.addEventListener('resize', resize)

  const bluePalette = ['#409EFF','#337ecc','#5cadff','#1a6dd4','#79bbff','#2b8af0']
  const size = 14

  function draw() {
    ctx.fillStyle = 'rgba(8, 15, 30, 0.05)'
    ctx.fillRect(0, 0, w, h)

    for (let c = 0; c < cols; c++) {
      const speed = 0.06 + Math.random() * 0.18
      drops[c] += speed
      const r = Math.floor(drops[c])
      if (drops[c] > rows + 10) drops[c] = -3

      if (r >= 0 && r < rows) {
        const px = c * size, py = r * size
        ctx.fillStyle = bluePalette[Math.floor(Math.random() * bluePalette.length)]
        ctx.globalAlpha = 0.1
        ctx.fillRect(px, py, size, size)

        // 拖尾
        if (r > 0) {
          ctx.globalAlpha = 0.025
          ctx.fillRect(c * size, (r - 1) * size, size, size)
        }
        ctx.globalAlpha = 1
      }
    }

    // 随机闪烁
    for (let i = 0; i < 8; i++) {
      const x = Math.floor(Math.random() * cols) * size
      const y = Math.floor(Math.random() * rows) * size
      ctx.fillStyle = bluePalette[Math.floor(Math.random() * bluePalette.length)]
      ctx.globalAlpha = 0.1 + Math.random() * 0.15
      ctx.fillRect(x, y, size, size)
    }
    ctx.globalAlpha = 1
    requestAnimationFrame(draw)
  }
  draw()
}

onMounted(() => {
  const stored = sessionStorage.getItem('currentUser')
  const token = sessionStorage.getItem('token')
  if (stored && token) {
    currentUser.value = JSON.parse(stored)
  }
  if (currentUser.value) {
    initMainBackground()
  } else {
    initPixelBackground()
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

// 登录/退出时切换背景
watch(currentUser, async (val) => {
  await nextTick()
  if (val) { initMainBackground() }
  else { initPixelBackground() }
})

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

/* 覆盖 Element Plus CSS 变量（从根源暗色化，组件内部不再引用白色变量） */
:root {
  --el-bg-color-overlay: rgba(16, 26, 48, 0.96);
  --el-bg-color: #0a0f1e;
  --el-bg-color-page: #0a0f1e;
  --el-fill-color-blank: rgba(20, 32, 56, 0.55);
  --el-fill-color: rgba(20, 32, 56, 0.3);
  --el-fill-color-light: rgba(64, 158, 255, 0.1);
  --el-fill-color-lighter: rgba(64, 158, 255, 0.05);
  --el-color-white: #0d162a;
  --el-color-black: #c8d8f0;
  --el-border-color: rgba(64, 158, 255, 0.15);
  --el-border-color-light: rgba(64, 158, 255, 0.1);
  --el-border-color-lighter: rgba(64, 158, 255, 0.06);
  --el-border-color-dark: rgba(64, 158, 255, 0.3);
  --el-text-color-primary: #d0e0ff;
  --el-text-color-regular: #c8d8f0;
  --el-text-color-secondary: #8899b4;
  --el-text-color-placeholder: #8899b4;
  --el-text-color-disabled: #556677;
  --el-disabled-bg-color: rgba(20, 32, 56, 0.3);
  --el-disabled-border-color: rgba(64, 158, 255, 0.08);
  --el-disabled-text-color: #556677;
  --el-mask-color: rgba(5, 8, 16, 0.7);
  --el-box-shadow: 0 2px 16px rgba(0,0,0,0.25);
  --el-box-shadow-light: 0 2px 8px rgba(0,0,0,0.15);
}
</style>

<style scoped>
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
/* ======== 翻转卡片容器 ======== */
.wrapper {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  --input-focus: #409EFF;
  --font-color: #c8d8f0;
  --font-color-sub: #8899b4;
  --bg-color: rgba(13, 22, 42, 0.85);
  --main-color: rgba(64, 158, 255, 0.45);
}

/* ======== 切换滑块 ======== */
.switch {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 30px;
  width: 50px;
  height: 20px;
  margin-bottom: 18px;
}
.card-side::before {
  position: absolute;
  content: '登 录';
  left: -50px;
  top: 0;
  width: 60px;
  text-decoration: underline;
  color: var(--font-color);
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 2px;
  text-shadow: 0 0 10px rgba(64, 158, 255, 0.3);
}
.card-side::after {
  position: absolute;
  content: '注 册';
  left: 65px;
  top: 0;
  width: 60px;
  text-decoration: none;
  color: var(--font-color);
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 2px;
}
.slider {
  box-sizing: border-box;
  border-radius: 12px;
  border: 2px solid var(--main-color);
  box-shadow: 0 0 12px rgba(64, 158, 255, 0.2);
  position: absolute;
  cursor: pointer;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: rgba(20, 32, 56, 0.7);
  transition: 0.3s;
}
.slider:before {
  box-sizing: border-box;
  position: absolute;
  content: "";
  height: 20px;
  width: 20px;
  border: 2px solid var(--main-color);
  border-radius: 10px;
  left: -2px;
  bottom: -2px;
  background-color: rgba(64, 158, 255, 0.25);
  box-shadow: 0 0 8px rgba(64, 158, 255, 0.3);
  transition: 0.3s;
}
.slider.checked {
  background-color: rgba(64, 158, 255, 0.2);
  border-color: #409EFF;
  box-shadow: 0 0 18px rgba(64, 158, 255, 0.35);
}
.slider.checked:before {
  transform: translateX(30px);
  background-color: #409EFF;
  box-shadow: 0 0 14px rgba(64, 158, 255, 0.5);
}
.card-side.flipped:before {
  text-decoration: none;
}
.card-side.flipped:after {
  text-decoration: underline;
}

/* ======== 3D 翻转卡片 ======== */
.flip-card__inner {
  width: 380px;
  min-height: 480px;
  position: relative;
  background-color: transparent;
  perspective: 1000px;
  text-align: center;
  transition: transform 0.7s cubic-bezier(0.4, 0, 0.2, 1);
  transform-style: preserve-3d;
}
.flip-card__inner.flipped {
  transform: rotateY(180deg);
}
.flip-card__inner.flipped .flip-card__front {
  box-shadow: none;
}
.flip-card__front, .flip-card__back {
  padding: 28px 24px;
  position: absolute;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
  background: rgba(10, 18, 36, 0.88);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  gap: 16px;
  border-radius: 16px;
  border: 2px solid rgba(64, 158, 255, 0.35);
  box-shadow: 0 0 30px rgba(64, 158, 255, 0.2), inset 0 0 30px rgba(64, 158, 255, 0.05);
}
.flip-card__back {
  transform: rotateY(180deg);
}
.flip-card__form {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  width: 100%;
}
.flip-title {
  font-size: 22px;
  font-weight: 600;
  text-align: center;
  color: #79bbff;
  text-shadow: 0 0 12px rgba(64, 158, 255, 0.4);
  font-family: 'Courier New', monospace;
  letter-spacing: 4px;
}
/* 输入框 / 选择器 / 按钮 暗色适配 */
.flip-card__form :deep(.el-input__wrapper) {
  background: rgba(20, 32, 56, 0.7) !important;
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.2) inset !important;
  border-radius: 8px !important;
}
.flip-card__form :deep(.el-input__inner) {
  color: #c8d8f0 !important;
}
.flip-card__form :deep(.el-input__inner::placeholder) {
  color: #8899b4 !important;
}
.flip-card__form :deep(.el-select .el-input__wrapper) {
  background: rgba(20, 32, 56, 0.7) !important;
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.2) inset !important;
}
.flip-card__form :deep(.el-select .el-input__inner) {
  color: #c8d8f0 !important;
}
/* 输入框 hover/focus 态 */
.flip-card__form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.4) inset !important;
}
.flip-card__form :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #409EFF inset !important;
}
/* 密码可见切换图标 */
.flip-card__form :deep(.el-input__suffix .el-icon) {
  color: #8899b4;
}
/* 按钮 */
.flip-btn {
  width: 200px;
  margin-top: 4px;
  border-radius: 8px !important;
}
.flip-hint {
  text-align: center;
  color: rgba(121, 187, 255, 0.5);
  font-size: 12px;
  line-height: 1.8;
}
.flip-hint-tip {
  display: block;
  color: #8899b4;
  font-size: 11px;
  margin-bottom: 2px;
}
.hero-title {
  position: absolute;
  top: 5%;
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
  top: calc(7% + 100px);
  left: 50%;
  transform: translateX(-50%);
  font-size: 18px;
  font-weight: 300;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 8px;
  pointer-events: none;
  user-select: none;
}
/* ======== 主界面 ======== */
.main-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #0a0f1e;
  position: relative;
  overflow: hidden;
}
.main-canvas {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  z-index: 0;
}

/* ======== 顶部悬浮导航栏 ======== */
.top-nav {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
  margin: 12px 20px 0 20px;
  background: rgba(10, 18, 36, 0.82);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(64, 158, 255, 0.25);
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3), 0 0 20px rgba(64, 158, 255, 0.1);
  flex-shrink: 0;
}
.nav-left { display: flex; align-items: center; gap: 10px; }
.nav-avatar {
  width: 34px; height: 34px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 15px; font-weight: bold;
  flex-shrink: 0; cursor: pointer;
  transition: transform 0.2s;
}
.nav-avatar:hover { transform: scale(1.1); }
.nav-brand {
  font-size: 18px; font-weight: 700;
  color: #e0edff; letter-spacing: 3px;
  text-shadow: 0 0 16px rgba(64, 158, 255, 0.35);
}
.nav-center { display: flex; align-items: center; gap: 4px; }
.nav-item {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 8px 16px; border-radius: 10px;
  font-size: 14px; color: #8899b4;
  cursor: pointer; transition: all 0.25s;
  white-space: nowrap; user-select: none;
}
.nav-item:hover { color: #c8ddf8; background: rgba(64, 158, 255, 0.12); }
.nav-item.active {
  color: #409EFF; background: rgba(64, 158, 255, 0.18);
  font-weight: 600; text-shadow: 0 0 10px rgba(64, 158, 255, 0.3);
}
.nav-right { display: flex; align-items: center; gap: 10px; }
.nav-user { font-size: 13px; color: #bfcbd9; }
.nav-logout { color: #8899b4 !important; }
.nav-logout:hover { color: #F56C6C !important; }

/* ======== 内容区 ======== */
.content {
  position: relative; z-index: 1;
  flex: 1; padding: 20px 24px;
  overflow-y: auto; color: #c8d8f0;
}
.content h2 { color: #d0e0ff; }
.content h3 { color: #c8d8f0; }

/* 卡片毛玻璃 */
.content :deep(.el-card) {
  background: rgba(13, 22, 42, 0.78) !important;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(64, 158, 255, 0.18) !important;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.25) !important;
  color: #c8d8f0;
}
.content :deep(.el-card:hover) {
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.15) !important;
}
.content :deep(.el-card__header) {
  color: #d0e0ff;
  border-bottom-color: rgba(64, 158, 255, 0.12) !important;
}

/* 表格 */
.content :deep(.el-table) {
  background: rgba(13, 22, 42, 0.6) !important;
  color: #c8d8f0;
}
.content :deep(.el-table__header-wrapper) { background: rgba(20, 32, 56, 0.85); }
.content :deep(.el-table__body-wrapper) { background: rgba(13, 22, 42, 0.6); }
.content :deep(.el-table th.el-table__cell) {
  background-color: rgba(20, 32, 56, 0.85) !important;
  color: #99b8e0 !important;
  border-bottom-color: rgba(64, 158, 255, 0.18) !important;
}
.content :deep(.el-table td.el-table__cell) {
  background-color: transparent !important;
  color: #c8d8f0 !important;
  border-bottom-color: rgba(64, 158, 255, 0.08) !important;
}
.content :deep(.el-table tr.el-table__row:hover > td.el-table__cell) {
  background-color: rgba(64, 158, 255, 0.08) !important;
}
.content :deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background-color: rgba(20, 32, 56, 0.35) !important;
}
.content :deep(.el-table--border td.el-table__cell),
.content :deep(.el-table--border th.el-table__cell) {
  border-right-color: rgba(64, 158, 255, 0.1) !important;
}
.content :deep(.el-table--border) {
  border-color: rgba(64, 158, 255, 0.15) !important;
}
.content :deep(.el-table__empty-text) { color: #8899b4; }
.content :deep(.el-table .el-loading-mask) { background-color: rgba(10, 15, 30, 0.6); }

/* 输入框 / 选择器 */
.content :deep(.el-input__wrapper) {
  background: rgba(20, 32, 56, 0.6);
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.15) inset;
}
.content :deep(.el-input__inner) { color: #c8d8f0; }
.content :deep(.el-select .el-input__inner) { color: #c8d8f0; }

/* 描述列表 */
.content :deep(.el-descriptions__label) { color: #99b8e0; }
.content :deep(.el-descriptions__content) { color: #c8d8f0; }

/* 时间线 */
.content :deep(.el-timeline-item__timestamp) { color: #8899b4; }

/* 弹窗 */
.content :deep(.el-dialog) {
  background: rgba(16, 26, 48, 0.95);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(64, 158, 255, 0.2);
  border-radius: 12px;
}
.content :deep(.el-dialog__title) { color: #d0e0ff; }
.content :deep(.el-dialog__body) { color: #c8d8f0; }

/* 抽屉 (个人信息) — rendered in-place, scoped OK */
:deep(.el-drawer) {
  background: rgba(14, 22, 42, 0.95) !important;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}
:deep(.el-drawer__title) { color: #d0e0ff; }

/* 分页 */
.content :deep(.el-pagination .btn-prev),
.content :deep(.el-pagination .btn-next),
.content :deep(.el-pager li) {
  background: rgba(20, 32, 56, 0.5) !important;
  color: #99b8e0 !important;
}
.content :deep(.el-pager li.is-active) {
  background: rgba(64, 158, 255, 0.3) !important;
  color: #fff !important;
}

/* 标签页 */
.content :deep(.el-tabs__item) { color: #8899b4; }
.content :deep(.el-tabs__item.is-active) { color: #409EFF; }
.content :deep(.el-tabs__nav-wrap::after) { background-color: rgba(64, 158, 255, 0.1); }

/* Tag 标签 */
.content :deep(.el-tag--info) {
  background-color: rgba(144,147,153,0.15);
  border-color: rgba(144,147,153,0.3);
  color: #b0b8c4;
}

/* 表单标签 */
.content :deep(.el-form-item__label) { color: #99b8e0; }
.content :deep(.el-checkbox__label) { color: #c8d8f0; }

/* 级联/树选择器 */
.content :deep(.el-tree) { background: transparent; color: #c8d8f0; }
.content :deep(.el-tree-node__content:hover) { background-color: rgba(64,158,255,0.08); }
</style>

<style>
/* === 全局暗色覆盖：所有 Element Plus 弹出层 & 输入组件 === */

/* — 基础 Popper 容器 — */
.el-popper {
  background: rgba(16, 26, 48, 0.96) !important;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(64, 158, 255, 0.2) !important;
  color: #c8d8f0 !important;
}

/* — 下拉选择器 — */
.el-select-dropdown {
  background: rgba(16, 26, 48, 0.96) !important;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(64, 158, 255, 0.2) !important;
}
.el-select-dropdown__wrap {
  background: transparent !important;
}
.el-select-dropdown__list {
  background: transparent !important;
}
.el-select-dropdown__item {
  color: #c8d8f0 !important;
  background: transparent !important;
}
.el-select-dropdown__item:hover {
  background: rgba(64, 158, 255, 0.12) !important;
}
.el-select-dropdown__item.is-selected {
  color: #409EFF !important;
  background: rgba(64, 158, 255, 0.15) !important;
  font-weight: 600;
}
.el-select-dropdown__item.is-hovering {
  background: rgba(64, 158, 255, 0.1) !important;
}
.el-select-dropdown__empty {
  color: #8899b4 !important;
}

/* — 选择器专用外壳（Element Plus 2.4+ 新增 el-select__wrapper） — */
.el-select__wrapper {
  background: rgba(20, 32, 56, 0.55) !important;
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.15) inset !important;
}
.el-select__wrapper:hover {
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.35) inset !important;
}
.el-select.is-focus .el-select__wrapper {
  box-shadow: 0 0 0 1px #409EFF inset !important;
}
.el-select__placeholder {
  color: #8899b4 !important;
}
.el-select__placeholder.is-transparent {
  color: #8899b4 !important;
}
.el-select__caret {
  color: #8899b4 !important;
}

/* — 输入框全局暗色 — */
.el-input__wrapper {
  background: rgba(20, 32, 56, 0.55) !important;
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.15) inset !important;
}
.el-input__wrapper:hover {
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.35) inset !important;
}
.el-input.is-focus .el-input__wrapper,
.el-input.is-focus .el-input__wrapper:hover {
  box-shadow: 0 0 0 1px #409EFF inset !important;
}
.el-input__inner {
  color: #c8d8f0 !important;
  background: transparent !important;
}
.el-input__inner::placeholder {
  color: #8899b4 !important;
}
.el-input__suffix .el-icon {
  color: #8899b4;
}
/* textarea */
.el-textarea__inner {
  background: rgba(20, 32, 56, 0.55) !important;
  color: #c8d8f0 !important;
  border-color: rgba(64, 158, 255, 0.15) !important;
}
.el-textarea__inner::placeholder {
  color: #8899b4 !important;
}
.el-textarea__inner:focus {
  border-color: #409EFF !important;
}

/* — 多选 tag 标签 — */
.el-select .el-tag {
  background-color: rgba(64, 158, 255, 0.15) !important;
  border-color: rgba(64, 158, 255, 0.3) !important;
  color: #c8d8f0 !important;
}
.el-select .el-tag .el-tag__close {
  color: #8899b4 !important;
}
.el-select .el-tag .el-tag__close:hover {
  background-color: rgba(245, 108, 108, 0.3) !important;
  color: #F56C6C !important;
}

/* — Popconfirm 气泡 — */
.el-popconfirm {
  background: rgba(20, 32, 56, 0.95) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(64, 158, 255, 0.2) !important;
}
.el-popconfirm__title {
  color: #c8d8f0 !important;
}
.el-popconfirm__action .el-button--text {
  color: #8899b4 !important;
}
.el-popper__arrow::before {
  background: rgba(20, 32, 56, 0.95) !important;
  border: 1px solid rgba(64, 158, 255, 0.2) !important;
}

/* — 消息提示 — */
.el-message {
  background: rgba(16, 26, 48, 0.95) !important;
  backdrop-filter: blur(14px);
  border: 1px solid rgba(64, 158, 255, 0.2) !important;
}
.el-message__content { color: #c8d8f0 !important; }

/* — 消息弹窗 (MessageBox) — */
.el-message-box {
  background: rgba(16, 26, 48, 0.96) !important;
  backdrop-filter: blur(14px);
  border: 1px solid rgba(64, 158, 255, 0.2) !important;
}
.el-message-box__title { color: #d0e0ff !important; }
.el-message-box__message { color: #c8d8f0 !important; }
.el-message-box__input .el-input__inner { color: #c8d8f0 !important; }

/* — 级联选择器 — */
.el-cascader-node {
  color: #c8d8f0 !important;
  background: transparent !important;
}
.el-cascader-node:not(.is-disabled):hover {
  background: rgba(64, 158, 255, 0.1) !important;
}
.el-cascader-node.is-active {
  color: #409EFF !important;
  background: rgba(64, 158, 255, 0.12) !important;
}
.el-cascader__dropdown {
  background: rgba(16, 26, 48, 0.96) !important;
  border: 1px solid rgba(64, 158, 255, 0.2) !important;
}

/* — 日期选择器 — */
.el-picker-panel {
  background: rgba(16, 26, 48, 0.96) !important;
  border: 1px solid rgba(64, 158, 255, 0.2) !important;
  color: #c8d8f0 !important;
}
.el-date-picker__header-label { color: #c8d8f0 !important; }
.el-date-table th { color: #8899b4 !important; }
.el-date-table td { color: #c8d8f0 !important; }
.el-date-table td.available:hover { color: #409EFF !important; }
.el-date-table td.current:not(.disabled) span { background-color: #409EFF !important; }
.el-date-table td.next-month, .el-date-table td.prev-month { color: #556677 !important; }
.el-picker-panel__icon-btn { color: #8899b4 !important; }
.el-picker-panel__icon-btn:hover { color: #409EFF !important; }

/* — 下拉菜单 — */
.el-dropdown-menu {
  background: rgba(16, 26, 48, 0.96) !important;
  border: 1px solid rgba(64, 158, 255, 0.2) !important;
}
.el-dropdown-menu__item {
  color: #c8d8f0 !important;
  background: transparent !important;
}
.el-dropdown-menu__item:hover {
  background: rgba(64, 158, 255, 0.12) !important;
  color: #409EFF !important;
}

/* — 自动补全 — */
.el-autocomplete-suggestion {
  background: rgba(16, 26, 48, 0.96) !important;
  border: 1px solid rgba(64, 158, 255, 0.2) !important;
}
.el-autocomplete-suggestion li {
  color: #c8d8f0 !important;
}
.el-autocomplete-suggestion li:hover {
  background: rgba(64, 158, 255, 0.12) !important;
}

/* — 穿梭框 — */
.el-transfer-panel {
  background: rgba(16, 26, 48, 0.8) !important;
  border-color: rgba(64, 158, 255, 0.2) !important;
}
</style>
