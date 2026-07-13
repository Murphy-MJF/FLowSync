<template>
  <div class="page">
    <h2 class="page-title">成员列表</h2>
    <el-table :data="members" stripe style="width:100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" width="100" />
      <el-table-column prop="realName" label="真实姓名" width="100" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{row}"><el-tag :type="row.role==='负责人'?'primary':'success'">{{ row.role }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="phone" label="电话" width="140" />
      <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="160" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { userApi } from "../api/index";
const members = ref([]);
onMounted(async () => { const res = await userApi.list(); members.value = res.data; });
</script>
