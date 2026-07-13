<template>
  <div class="home-layout">
    <div class="sidebar">
      <div class="logo-area">
        <h2 class="logo-title">FlowSync</h2>
        <p class="logo-sub">{{ currentUser?.realName }}</p>
      </div>
      <el-menu :default-active="route.path" router class="sidebar-menu" background-color="#304156" text-color="#bfcbd9" active-text-color="#409eff">
        <div class="menu-group-label">工作台</div>
        <el-menu-item index="/overview"><el-icon><DataAnalysis /></el-icon><span>总览</span></el-menu-item>
        <div class="menu-group-label">业务管理</div>
        <el-menu-item index="/projects"><el-icon><FolderOpened /></el-icon><span>项目管理</span></el-menu-item>
        <el-menu-item index="/task-decompose" v-if="isLeader"><el-icon><MagicStick /></el-icon><span>任务拆解</span></el-menu-item>
        <el-menu-item index="/tasks"><el-icon><List /></el-icon><span>任务管理</span></el-menu-item>
        <el-menu-item index="/progress"><el-icon><TrendCharts /></el-icon><span>进度跟踪</span></el-menu-item>
        <el-menu-item index="/summaries"><el-icon><Document /></el-icon><span>总结中心</span></el-menu-item>
        <div class="menu-group-label">系统信息</div>
        <el-menu-item index="/members"><el-icon><UserFilled /></el-icon><span>成员列表</span></el-menu-item>
        <el-menu-item index="/profile"><el-icon><Setting /></el-icon><span>个人信息</span></el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <el-button text style="color:#bfcbd9;width:100%;justify-content:flex-start;" @click="logout">
          <el-icon><SwitchButton /></el-icon> 退出登录
        </el-button>
      </div>
    </div>
    <div class="main-area">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();
const userStr = sessionStorage.getItem("currentUser");
const currentUser = userStr ? JSON.parse(userStr) : {};
const isLeader = computed(() => currentUser?.role === "负责人");

function logout() {
  sessionStorage.removeItem("currentUser");
  sessionStorage.removeItem("token");
  router.push("/login");
}
</script>

<style scoped>
.home-layout { display: flex; height: 100vh; }
.sidebar { width: 220px; background: #304156; display: flex; flex-direction: column; flex-shrink: 0; }
.logo-area { padding: 20px; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.1); }
.logo-title { color: #fff; font-size: 20px; margin: 0 0 4px; }
.logo-sub { color: #bfcbd9; font-size: 12px; margin: 0; }
.sidebar-menu { border-right: none; flex: 1; }
.menu-group-label { padding: 12px 20px 4px; font-size: 12px; color: #5a6b7c; }
.sidebar-footer { border-top: 1px solid rgba(255,255,255,0.1); padding: 8px; }
.main-area { flex: 1; padding: 20px; background: #f5f7fa; overflow-y: auto; }
</style>
