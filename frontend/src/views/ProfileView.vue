<template>
  <div class="page">
    <h2 class="page-title">个人信息</h2>
    <el-row :gutter="40">
      <el-col :span="12">
        <el-card>
          <template #header>个人资料</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
            <el-descriptions-item label="真实姓名">{{ user.realName }}</el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-tag :type="user.role==="负责人"?"primary":"success"">{{ user.role }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="电话">{{ user.phone || "-" }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ user.email || "-" }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ user.createTime }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>修改密码</template>
          <el-form :model="pwdForm" label-width="90px" style="max-width:350px">
            <el-form-item label="旧密码"><el-input v-model="pwdForm.oldPassword" type="password" /></el-form-item>
            <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" /></el-form-item>
            <el-form-item>
              <el-button type="primary" @click="updatePwd">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { userApi } from "../api/index";
import { ElMessage } from "element-plus";

const userStr = sessionStorage.getItem("currentUser");
const user = userStr ? JSON.parse(userStr) : {};

const pwdForm = ref({ oldPassword: "", newPassword: "" });

async function updatePwd() {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    ElMessage.warning("请填写完整");
    return;
  }
  try {
    await userApi.updatePassword({
      userId: user.id,
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword,
    });
    ElMessage.success("密码修改成功");
    pwdForm.value = { oldPassword: "", newPassword: "" };
  } catch {
    /* handled by interceptor */
  }
}
</script>
