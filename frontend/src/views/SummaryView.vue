<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">总结中心</h2>
      <el-button type="primary" @click="dialogVisible=true">新增总结</el-button>
    </div>
    <el-table :data="summaries" stripe style="width:100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="summaryType" label="类型" width="100">
        <template #default="{row}"><el-tag :type="row.summaryType==='最终总结'?'success':'warning'">{{ row.summaryType }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="projectId" label="项目ID" width="80" />
      <el-table-column prop="taskId" label="任务ID" width="80" />
      <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
      <el-table-column prop="createdBy" label="创建人ID" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="160" />
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增总结" width="550px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="所属项目">
          <el-select v-model="form.projectId" style="width:100%">
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联任务">
          <el-select v-model="form.taskId" style="width:100%" clearable>
            <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="总结类型">
          <el-radio-group v-model="form.summaryType">
            <el-radio value="阶段总结">阶段总结</el-radio>
            <el-radio value="最终总结">最终总结</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="总结内容"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
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
import { summaryApi, projectApi, taskApi } from "../api/index";
import { ElMessage } from "element-plus";

const userStr = sessionStorage.getItem("currentUser");
const currentUser = userStr ? JSON.parse(userStr) : {};

const summaries = ref([]);
const projects = ref([]);
const tasks = ref([]);
const dialogVisible = ref(false);
const form = ref({ projectId: null, taskId: null, summaryType: "阶段总结", content: "", createdBy: currentUser.id });

async function load() { const res = await summaryApi.list(); summaries.value = res.data; }
async function loadProjects() { const res = await projectApi.list(); projects.value = res.data; }
async function loadTasks() { const res = await taskApi.list(); tasks.value = res.data; }
async function handleSave() {
  form.value.createdBy = currentUser.id;
  await summaryApi.save(form.value);
  ElMessage.success("保存成功");
  dialogVisible.value = false;
  form.value = { projectId: null, taskId: null, summaryType: "阶段总结", content: "", createdBy: currentUser.id };
  load();
}
onMounted(() => { load(); loadProjects(); loadTasks(); });
</script>
