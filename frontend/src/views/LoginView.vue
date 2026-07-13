<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>FlowSync</h1>
        <p class="subtitle">小组任务协同管理系统</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" @keyup.enter="login">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width:100%" @click="login">登 录</el-button>
        </el-form-item>
      </el-form>
      <div class="login-hint">
        <p>测试账号：leader / 123456</p>
        <p>成员账号：member1 / 123456</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { authApi } from "../api/index";
import { ElMessage } from "element-plus";

const router = useRouter();
const formRef = ref(null);
const loading = ref(false);
const form = reactive({ username: "leader", password: "123456" });
const rules = { username: [{ required: true, message: "请输入用户名" }], password: [{ required: true, message: "请输入密码" }] };

async function login() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  loading.value = true;
  try {
    const res = await authApi.login(form);
    sessionStorage.setItem("currentUser", JSON.stringify(res.data.user));
    sessionStorage.setItem("token", res.data.token);
    ElMessage.success("登录成功");
    router.push("/");
  } catch {
    // error already handled by interceptor
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px; padding: 40px; background: #fff; border-radius: 8px; box-shadow: 0 8px 32px rgba(0,0,0,0.15);
}
.login-header { text-align: center; margin-bottom: 30px; }
.login-header h1 { font-size: 28px; color: #303133; margin: 0 0 6px; }
.subtitle { font-size: 14px; color: #909399; margin: 0; }
.login-hint { margin-top: 20px; padding-top: 16px; border-top: 1px solid #ebeef5; text-align: center; }
.login-hint p { font-size: 12px; color: #c0c4cc; margin: 4px 0; }
</style>
