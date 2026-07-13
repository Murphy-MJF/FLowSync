<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">进度跟踪</h2>
      <el-button type="primary" @click="dialogVisible=true">新增进度记录</el-button>
    </div>
    <el-table :data="logs" stripe style="width:100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="taskId" label="任务ID" width="80" />
      <el-table-column prop="progressPercent" label="进度" width="100">
        <template #default="{row}">
          <el-progress :percentage="row.progressPercent" :status="row.progressPercent>=100?'success':undefined" />
        </template>
      </el-table-column>
      <el-table-column prop="content" label="说明" min-width="300" show-overflow-tooltip />
      <el-table-column prop="operatorId" label="记录人ID" width="100" />
      <el-table-column prop="createTime" label="记录时间" width="160" />
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增进度记录" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="关联任务">
          <el-select v-model="form.taskId" style="width:100%">
            <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="进度百分比">
          <el-slider v-model="form.progressPercent" :min="0" :max="100" show-input style="width:100%" />
        </el-form-item>
        <el-form-item label="说明文字"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { taskLogApi, taskApi } from "../api/index";
import { ElMessage } from "element-plus";

const userStr = sessionStorage.getItem("currentUser");
const currentUser = userStr ? JSON.parse(userStr) : {};

const logs = ref([]);
const tasks = ref([]);
const dialogVisible = ref(false);
const form = ref({ taskId: null, progressPercent: 0, content: "", operatorId: currentUser.id });

async function load() { const res = await taskLogApi.list(); logs.value = res.data; }
async function loadTasks() { const res = await taskApi.list(); tasks.value = res.data; }
async function handleSave() {
  form.value.operatorId = currentUser.id;
  await taskLogApi.save(form.value);
  ElMessage.success("保存成功");
  dialogVisible.value = false;
  form.value = { taskId: null, progressPercent: 0, content: "", operatorId: currentUser.id };
  load();
}
onMounted(() => { load(); loadTasks(); });
</script>
