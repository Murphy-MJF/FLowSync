<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">项目管理</h2>
      <el-button type="primary" @click="openDialog()" v-if="isLeader">新建项目</el-button>
    </div>
    <el-table :data="projects" stripe style="width:100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="项目名称" min-width="140" />
      <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
      <el-table-column prop="ownerId" label="负责人ID" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}"><el-tag :type="statusType(row.status)">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="80" />
      <el-table-column prop="startDate" label="开始" width="100" />
      <el-table-column prop="endDate" label="结束" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="140" fixed="right" v-if="isLeader">
        <template #default="{row}">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑项目' : '新建项目'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status"><el-option label="未开始" value="未开始" /><el-option label="进行中" value="进行中" /><el-option label="已完成" value="已完成" /></el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority"><el-option label="低" value="低" /><el-option label="中" value="中" /><el-option label="高" value="高" /></el-select>
        </el-form-item>
        <el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="结束日期"><el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { projectApi } from "../api/index";
import { ElMessage, ElMessageBox } from "element-plus";

const userStr = sessionStorage.getItem("currentUser");
const currentUser = userStr ? JSON.parse(userStr) : {};
const isLeader = computed(() => currentUser?.role === "负责人");

const projects = ref([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = ref({});

function statusType(s) { return s === "已完成" ? "success" : s === "进行中" ? "warning" : "info"; }

async function load() { const res = await projectApi.list(); projects.value = res.data; }
function openDialog(row) {
  if (row) { isEdit.value = true; form.value = { ...row }; }
  else { isEdit.value = false; form.value = { name: "", description: "", status: "未开始", priority: "中", ownerId: currentUser.id, startDate: null, endDate: null }; }
  dialogVisible.value = true;
}
async function handleSave() {
  await projectApi.save(form.value);
  ElMessage.success(isEdit.value ? "编辑成功" : "创建成功");
  dialogVisible.value = false;
  load();
}
async function handleDelete(id) {
  await ElMessageBox.confirm("确认删除？");
  await projectApi.delete(id);
  ElMessage.success("删除成功");
  load();
}
onMounted(load);
</script>
