<template>
  <div>
    <router-view />
    <Transition name="fade">
      <div v-if="showBackTop" class="back-to-top" @click="scrollToTop" title="返回顶部">
        <span class="top-arrow">▲</span>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const showBackTop = ref(false)

function onScroll() {
  showBackTop.value = window.scrollY > 300
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => window.addEventListener('scroll', onScroll))
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<style>
/* 返回顶部按钮 */
.back-to-top {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(64, 158, 255, 0.85);
  backdrop-filter: blur(8px);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1000;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.35);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.back-to-top:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 24px rgba(64, 158, 255, 0.5);
}
.back-to-top:active {
  transform: scale(0.9);
}
.top-arrow {
  font-size: 16px;
  line-height: 1;
}

/* 淡入淡出过渡 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: scale(0.6);
}

/* 全局按钮按压反馈 */
.el-button {
  transition: transform 0.15s cubic-bezier(0.4, 0, 0.2, 1) !important;
}
.el-button:active {
  transform: scale(0.95) !important;
}

/* 全局卡片悬停效果 */
.el-card {
  transition: box-shadow 0.3s ease, transform 0.3s ease;
}
.el-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1) !important;
}

/* 全局圆角统一 */
.el-card, .el-dialog, .el-table {
  border-radius: 12px !important;
  overflow: hidden;
}
.el-button {
  border-radius: 8px !important;
}
.el-tag {
  border-radius: 6px !important;
}
.el-input .el-input__wrapper, .el-select .el-input__wrapper {
  border-radius: 8px !important;
}
</style>
