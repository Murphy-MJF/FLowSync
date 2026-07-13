<template>
  <div class="page">
    <h2 class="page-title">总览</h2>
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in stats" :key="item.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { overviewApi } from "../api/index";

const stats = ref([
  { label: "用户数", value: 0 }, { label: "项目数", value: 0 },
  { label: "任务数", value: 0 }, { label: "总结数", value: 0 },
]);

onMounted(async () => {
  const res = await overviewApi.get();
  const d = res.data;
  stats.value[0].value = d.userCount;
  stats.value[1].value = d.projectCount;
  stats.value[2].value = d.taskCount;
  stats.value[3].value = d.summaryCount;
});
</script>

<style scoped>
.stat-card { text-align: center; margin-bottom: 20px; }
.stat-value { font-size: 36px; font-weight: 700; color: #409eff; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
</style>
