<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">任务管理</h2>
      <div style="display:flex;gap:12px;align-items:center">
        <el-select v-model="filterProjectId" placeholder="按项目筛选" clearable @change="load">
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-button type="primary" @click="openDialog()" v-if="isLeader">新建任务</el-button>
      </div>
    </div>
    <el-table :data="tasks" stripe style="width:100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="任务标题" min-width="160" />
      <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
      <el-table-column prop="projectId" label="项目ID" width="80" />
      <el-table-column prop="assigneeId" label="负责人ID" width="90" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}"><el-tag :type="row.status==='已完成'?'success':row.status==='进行中'?'warning':'info'">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="70" />
      <el-table-column prop="dueDate" label="截止日期" width="100" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{row}">
          <el-button size="small" @click="openDialog(row)" v-if="isLeader || row.assigneeId === currentUser.id">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)" v-if="isLeader">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="所属项目"><el-select v-model="form.projectId" style="width:100%" :disabled="!isLeader"><el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" /></el-select></el-form-item>
        <el-form-item label="任务标题"><el-input v-model="form.title" :disabled="!isLeader" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="2" :disabled="!isLeader" /></el-form-item>
        <el-form-item label="负责人"><el-select v-model="form.assigneeId" style="width:100%" :disabled="!isLeader"><el-option v-for="u in members" :key="u.id" :label="u.realName" :value="u.id" /></el-select></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="未开始" value="未开始" /><el-option label="进行中" value="进行中" /><el-option label="已完成" value="已完成" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级"><el-select v-model="form.priority" style="width:100%" :disabled="!isLeader"><el-option label="低" value="低" /><el-option label="中" value="中" /><el-option label="高" value="高" /></el-select></el-form-item>
        <el-form-item label="截止日期"><el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" :disabled="!isLeader" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { taskApi, projectApi, userApi } from "../api/index";
import { ElMessage, ElMessageBox } from "element-plus";

const userStr = sessionStorage.getItem("currentUser");
const currentUser = userStr ? JSON.parse(userStr) : {};
const isLeader = computed(() => currentUser?.role === "负责人");

const tasks = ref([]);
const projects = ref([]);
const members = ref([]);
const filterProjectId = ref(null);
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = ref({});
const dialogTitle = computed(() => {
  if (!isEdit.value) return "新建任务";
  return isLeader.value ? "编辑任务" : "更新任务状态";
});

async function load() {
  const res = await taskApi.list(filterProjectId.value);
  tasks.value = res.data;
}
async function loadProjects() { const res = await projectApi.list(); projects.value = res.data; }
async function loadMembers() { const res = await userApi.list(); members.value = res.data; }

function openDialog(row) {
  if (row) {
    isEdit.value = true;
    form.value = { ...row };
  } else {
    isEdit.value = false;
    form.value = { projectId: filterProjectId.value, title: "", description: "", assigneeId: null, status: "未开始", priority: "中", dueDate: null, creatorId: currentUser.id };
  }
  dialogVisible.value = true;
}

async function handleSave() {
  if (!isEdit.value) form.value.creatorId = currentUser.id;
  await taskApi.save(form.value);
  ElMessage.success("保存成功");
  dialogVisible.value = false;
  load();
}

async function handleDelete(id) {
  await ElMessageBox.confirm("确认删除？");
  await taskApi.delete(id);
  ElMessage.success("删除成功");
  load();
}

onMounted(() => { load(); loadProjects(); loadMembers(); });
</script>
