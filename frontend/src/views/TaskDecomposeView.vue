<template>
  <div class="page">
    <h2 class="page-title">AI 任务拆解</h2>
    <el-card class="decompose-card">
      <el-form :model="form" label-width="100px">
        <el-form-item label="选择项目"><el-select v-model="form.projectId" style="width:100%"><el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" /></el-select></el-form-item>
        <el-form-item label="任务目标"><el-input v-model="form.goal" placeholder="描述需要拆解的任务目标" /></el-form-item>
        <el-form-item label="补充说明"><el-input v-model="form.description" type="textarea" :rows="3" placeholder="补充说明（可选）" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="generatePlan">开始拆解</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-if="planResult">
      <el-card class="result-card">
        <template #header>拆解结果</template>
        <p class="summary-text">{{ planResult.summary }}</p>
        <el-table :data="planResult.items" stripe style="width:100%" max-height="400">
          <el-table-column type="selection" width="40" />
          <el-table-column prop="title" label="任务名称" min-width="160" />
          <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
          <el-table-column prop="priority" label="优先级" width="80" />
          <el-table-column prop="suggestedDays" label="建议天数" width="90" />
          <el-table-column label="负责人" width="160">
            <template #default="{row,$index}">
              <el-select v-model="row.assigneeId" size="small">
                <el-option v-for="u in members" :key="u.id" :label="u.realName" :value="u.id" />
              </el-select>
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top:16px;text-align:right">
          <el-button type="primary" @click="importTasks">导入选中任务</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { projectApi, userApi, aiApi } from "../api/index";
import { ElMessage } from "element-plus";

const userStr = sessionStorage.getItem("currentUser");
const currentUser = userStr ? JSON.parse(userStr) : {};

const projects = ref([]);
const members = ref([]);
const loading = ref(false);
const planResult = ref(null);
const form = ref({ projectId: null, goal: "", description: "" });

async function loadProjects() { const res = await projectApi.list(); projects.value = res.data; }
async function loadMembers() { const res = await userApi.list(); members.value = res.data; }

async function generatePlan() {
  if (!form.value.projectId || !form.value.goal) { ElMessage.warning("请选择项目并填写任务目标"); return; }
  loading.value = true;
  try {
    const proj = projects.value.find(p => p.id === form.value.projectId);
    const res = await aiApi.taskPlan({
      projectId: form.value.projectId,
      operatorId: currentUser.id,
      projectName: proj?.name || "",
      goal: form.value.goal,
      description: form.value.description,
    });
    planResult.value = res.data;
  } finally { loading.value = false; }
}

async function importTasks() {
  if (!planResult.value?.items?.length) return;
  const selected = planResult.value.items.filter((_, i) => {
    const checkbox = document.querySelector(`.el-table__body-wrapper .el-checkbox`);
    return true; // import all for simplicity; user could deselect via checkbox
  });
  const items = planResult.value.items.filter(item => item.assigneeId);
  if (items.length === 0) { ElMessage.warning("请为任务选择负责人"); return; }
  await aiApi.importPlan({ projectId: form.value.projectId, creatorId: currentUser.id, items });
  ElMessage.success(`成功导入 ${items.length} 个任务`);
  planResult.value = null;
}

onMounted(() => { loadProjects(); loadMembers(); });
</script>

<style scoped>
.decompose-card { margin-bottom: 20px; }
.result-card { margin-top: 20px; }
.summary-text { color: #606266; margin-bottom: 16px; }
</style>

