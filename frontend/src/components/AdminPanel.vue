<template>
  <div>
    <h2 style="margin-bottom:16px">成员管理</h2>

    <!-- 管理员功能区 -->
    <template v-if="isAdmin">
      <!-- 邀请码 -->
      <el-card style="margin-bottom:20px">
        <template #header><span>邀请码管理</span></template>
        <div style="display:flex;align-items:center;gap:12px">
          <el-button type="primary" @click="handleGenerateCode" :loading="genLoading">生成邀请码</el-button>
          <template v-if="inviteCode">
            <el-tag type="success" size="large" style="font-size:18px;font-family:monospace;padding:8px 16px">{{ inviteCode }}</el-tag>
            <span style="color:#E6A23C;font-size:12px">2 分钟内有效</span>
          </template>
        </div>
      </el-card>
    </template>

    <!-- 成员列表（所有人可见） -->
    <el-card style="margin-bottom:20px">
      <template #header><span>成员列表</span></template>
      <el-table :data="users" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === '管理员' ? 'danger' : row.role === '负责人' ? 'warning' : 'info'" size="small">
              {{ row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="aiQuota" label="AI额度" width="80" />
        <el-table-column prop="createTime" label="注册时间" width="160" />
        <el-table-column v-if="isAdmin" label="操作" width="180">
          <template #default="{ row }">
            <template v-if="row.role !== '管理员'">
              <el-button v-if="row.role === '负责人'" size="small" type="warning"
                         @click="handleChangeRole(row, '组员')">降为组员</el-button>
              <el-button v-if="row.role === '组员'" size="small" type="success"
                         @click="handleChangeRole(row, '负责人')">升为负责人</el-button>
            </template>
            <span v-else style="color:#909399">—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 管理员：AI 额度审批 -->
    <el-card v-if="isAdmin" style="margin-bottom:20px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>AI 额度审批</span>
          <el-button size="small" @click="fetchQuotaRequests">刷新</el-button>
        </div>
      </template>
      <el-table :data="quotaRequests" border stripe v-loading="qrLoading" empty-text="暂无申请">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="userName" label="申请人" width="100" />
        <el-table-column prop="requestedAmount" label="申请次数" width="90" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'approved' ? 'success' : row.status === 'denied' ? 'danger' : 'warning'" size="small">
              {{ row.status === 'approved' ? '已批准' : row.status === 'denied' ? '已拒绝' : '待审批' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button size="small" type="success" @click="handleApprove(row, true)">批准</el-button>
              <el-button size="small" type="danger" @click="handleApprove(row, false)">拒绝</el-button>
            </template>
            <span v-else style="color:#909399">—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 转让弹窗 -->
    <el-dialog title="转让项目所有权" v-model="transferVisible" width="500px">
      <p style="margin-bottom:12px;color:#E6A23C">⚠️ 该用户拥有 {{ transferProjects.length }} 个项目，降级前需指定接手人：</p>
      <el-table :data="transferProjects" border size="small" max-height="200" style="margin-bottom:16px">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="项目名称" />
      </el-table>
      <el-form-item label="接手人" style="margin-top:8px">
        <el-select v-model="selectedNewOwner" placeholder="选择接手人" style="width:100%">
          <el-option v-for="u in transferCandidates" :key="u.id"
                     :label="u.realName + ' (' + u.role + ')'" :value="u.id"
                     :disabled="u.id === transferUserId" />
        </el-select>
      </el-form-item>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmTransfer" :disabled="!selectedNewOwner">确认转让并降级</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUsers, getAdminUsers, genInviteCode, changeUserRole, getTransferCandidates, getQuotaRequests, approveQuota } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({ currentUser: Object })
const isAdmin = computed(() => props.currentUser?.role === '管理员')

const users = ref([])
const inviteCode = ref('')
const loading = ref(false)
const genLoading = ref(false)

const transferVisible = ref(false)
const transferUserId = ref(null)
const transferProjects = ref([])
const transferCandidates = ref([])
const selectedNewOwner = ref(null)

const quotaRequests = ref([])
const qrLoading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const [uRes, cRes, qRes] = await Promise.all([
      isAdmin.value ? getAdminUsers() : getUsers(),
      isAdmin.value ? getTransferCandidates() : Promise.resolve({ success: false, data: [] }),
      isAdmin.value ? getQuotaRequests() : Promise.resolve({ success: false, data: [] })
    ])
    if (uRes.success) users.value = uRes.data || []
    if (cRes.success) transferCandidates.value = cRes.data || []
    if (qRes.success) quotaRequests.value = qRes.data || []
  } finally { loading.value = false }
})

async function handleGenerateCode() {
  genLoading.value = true
  try {
    const res = await genInviteCode()
    if (res.success) {
      inviteCode.value = res.data.code
      setTimeout(() => { inviteCode.value = '' }, 120000)
    }
  } finally { genLoading.value = false }
}

async function handleChangeRole(row, newRole) {
  try {
    const res = await changeUserRole({ userId: row.id, role: newRole })
    if (res.success) {
      if (res.data && res.data.needTransfer) {
        const cRes = await getTransferCandidates()
        if (cRes.success) transferCandidates.value = cRes.data || []
        transferUserId.value = row.id
        transferProjects.value = res.data.projects
        selectedNewOwner.value = null
        transferVisible.value = true
        return
      }
      ElMessage.success(res.message)
      const uRes = await getAdminUsers()
      if (uRes.success) users.value = uRes.data || []
      if (res.data && res.data.transferred > 0) {
        ElMessage.success(`已转让 ${res.data.transferred} 个项目`)
      }
    }
  } catch (e) { /* handled */ }
}

async function handleConfirmTransfer() {
  if (!selectedNewOwner.value) { ElMessage.warning('请选择接手人'); return }
  try {
    const res = await changeUserRole({
      userId: transferUserId.value, role: '组员', newOwnerId: selectedNewOwner.value
    })
    if (res.success) {
      ElMessage.success('降级成功，项目已转让')
      transferVisible.value = false
      const uRes = await getAdminUsers()
      if (uRes.success) users.value = uRes.data || []
    }
  } catch (e) { /* handled */ }
}

async function fetchQuotaRequests() {
  qrLoading.value = true
  try {
    const res = await getQuotaRequests()
    if (res.success) quotaRequests.value = res.data || []
  } finally { qrLoading.value = false }
}

async function handleApprove(row, approved) {
  let amount = row.requestedAmount
  if (approved) {
    try {
      const { value } = await ElMessageBox.prompt('批准次数', '批准额度', { inputValue: amount, inputType: 'number' })
      amount = parseInt(value)
    } catch { return }
  }
  try {
    const res = await approveQuota(row.id, approved, amount)
    if (res.success) {
      ElMessage.success(res.message)
      fetchQuotaRequests()
      const uRes = await getAdminUsers()
      if (uRes.success) users.value = uRes.data || []
    }
  } catch (e) { /* handled */ }
}
</script>
