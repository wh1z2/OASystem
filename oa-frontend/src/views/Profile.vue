<template>
  <div class="space-y-6">
    <h2 class="text-xl font-semibold text-gray-900">个人中心</h2>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2">
        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-6">基本信息</h3>
          
          <form @submit.prevent="handleSave" class="space-y-6">
            <div class="flex items-center gap-6">
              <img :src="authStore.currentUser?.avatar" :alt="authStore.currentUser?.name" class="w-24 h-24 rounded-full" />
              <div>
                <button type="button" class="btn btn-secondary text-sm">
                  更换头像
                </button>
                <p class="text-xs text-gray-500 mt-2">支持 JPG、PNG 格式，最大 2MB</p>
              </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">姓名</label>
                <input v-model="form.name" type="text" class="input" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">用户名</label>
                <input v-model="form.username" type="text" class="input" disabled />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">邮箱</label>
                <input v-model="form.email" type="email" class="input" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">手机号</label>
                <input v-model="form.phone" type="tel" class="input" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">部门</label>
                <input v-model="form.department" type="text" class="input" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">角色</label>
                <input :value="getRoleLabel(form.role)" type="text" class="input" disabled />
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">个人简介</label>
              <textarea v-model="form.bio" class="input h-24 resize-none" placeholder="请输入个人简介..."></textarea>
            </div>

            <div class="flex gap-4">
              <button type="submit" class="btn btn-primary">
                保存修改
              </button>
              <button type="button" @click="resetForm" class="btn btn-secondary">
                重置
              </button>
            </div>
          </form>
        </div>

        <div class="card mt-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-6">修改密码</h3>
          
          <form @submit.prevent="handleChangePassword" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">当前密码</label>
              <input v-model="passwordForm.currentPassword" type="password" class="input" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">新密码</label>
              <input v-model="passwordForm.newPassword" type="password" class="input" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">确认新密码</label>
              <input v-model="passwordForm.confirmPassword" type="password" class="input" />
            </div>
            <button type="submit" class="btn btn-primary">
              修改密码
            </button>
          </form>
        </div>
      </div>

      <div class="space-y-6">
        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">账户统计</h3>
          <div class="space-y-4">
            <div class="flex items-center justify-between">
              <span class="text-gray-600">已提交申请</span>
              <span class="font-semibold text-gray-900">{{ stats.submitted }}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-gray-600">已通过</span>
              <span class="font-semibold text-success-600">{{ stats.approved }}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-gray-600">已拒绝</span>
              <span class="font-semibold text-danger-600">{{ stats.rejected }}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-gray-600">待审批</span>
              <span class="font-semibold text-warning-600">{{ stats.pending }}</span>
            </div>
          </div>
        </div>

        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">快捷操作</h3>
          <div class="space-y-3">
            <router-link to="/approval/create" class="block p-3 bg-primary-50 rounded-lg hover:bg-primary-100 transition-colors cursor-pointer">
              <p class="font-medium text-primary-600">发起审批</p>
              <p class="text-xs text-primary-500">提交新的审批申请</p>
            </router-link>
            <router-link to="/todo" class="block p-3 bg-warning-50 rounded-lg hover:bg-warning-100 transition-colors cursor-pointer">
              <p class="font-medium text-warning-600">待办事项</p>
              <p class="text-xs text-warning-500">查看待处理的审批</p>
            </router-link>
            <router-link to="/done" class="block p-3 bg-success-50 rounded-lg hover:bg-success-100 transition-colors cursor-pointer">
              <p class="font-medium text-success-600">已办事项</p>
              <p class="text-xs text-success-500">查看已处理的审批</p>
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useApprovalStore } from '@/stores/approval'

const authStore = useAuthStore()
const approvalStore = useApprovalStore()

const form = ref({
  name: '',
  username: '',
  email: '',
  phone: '',
  department: '',
  role: '',
  bio: ''
})

const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const stats = computed(() => {
  const myApprovals = approvalStore.approvals.filter(a => a.applicantId === authStore.currentUser?.id)
  return {
    submitted: myApprovals.length,
    approved: myApprovals.filter(a => a.status === 'approved').length,
    rejected: myApprovals.filter(a => a.status === 'rejected').length,
    pending: myApprovals.filter(a => a.status === 'pending').length
  }
})

function getRoleLabel(role) {
  const labels = {
    admin: '系统管理员',
    manager: '部门经理',
    employee: '普通员工'
  }
  return labels[role] || role
}

function handleSave() {
  alert('保存成功！')
}

function resetForm() {
  initForm()
}

function handleChangePassword() {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }
  alert('密码修改成功！')
  passwordForm.value = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
}

function initForm() {
  if (authStore.currentUser) {
    form.value = {
      name: authStore.currentUser.name,
      username: authStore.currentUser.username,
      email: authStore.currentUser.email,
      phone: authStore.currentUser.phone || '',
      department: authStore.currentUser.department,
      role: authStore.currentUser.role,
      bio: ''
    }
  }
}

onMounted(() => {
  initForm()
})
</script>
