<template>
  <div>
    <h2 style="margin-bottom:16px">文件修改记录</h2>
    <div style="margin-bottom:16px;display:flex;gap:12px;align-items:center">
      <el-select v-model="selectedProjectId" placeholder="选择项目" style="width:300px" @change="loadHistory">
        <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
    </div>

    <el-card v-if="!commits.length && selectedProjectId" style="text-align:center;padding:40px;color:#909399" v-loading="loading">
      <template v-if="!loading">该项目暂无提交记录或未绑定仓库</template>
    </el-card>

    <el-card v-if="commits.length > 0">
      <el-timeline>
        <el-timeline-item v-for="c in commits" :key="c.sha"
                          :timestamp="c.date" placement="top"
                          :color="c.flowSyncUser ? '#409EFF' : '#909399'">
          <div style="display:flex;justify-content:space-between;align-items:center">
            <div>
              <div style="font-weight:bold;margin-bottom:4px">{{ c.message }}</div>
              <div style="font-size:12px;color:#909399">
                SHA: <code>{{ c.sha }}</code>
                <template v-if="c.githubLogin">
                  | GitHub: <strong>{{ c.githubLogin }}</strong>
                </template>
                <template v-if="c.flowSyncUser">
                  | FlowSync: <el-tag size="small" type="success">{{ c.flowSyncUser }}</el-tag>
                </template>
                <template v-if="c.author && !c.githubLogin">
                  | 作者: {{ c.author }}
                </template>
              </div>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getProjects, githubProjectCommits } from '../api'

defineProps({ currentUser: Object })

const projects = ref([])
const selectedProjectId = ref(null)
const commits = ref([])
const loading = ref(false)

onMounted(async () => {
  const res = await getProjects()
  if (res.success) projects.value = res.data || []
})

async function loadHistory() {
  if (!selectedProjectId.value) return
  loading.value = true
  try {
    const res = await githubProjectCommits(selectedProjectId.value)
    if (res.success) commits.value = res.data || []
    else commits.value = []
  } finally { loading.value = false }
}
</script>
