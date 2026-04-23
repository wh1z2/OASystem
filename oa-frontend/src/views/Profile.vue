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
              <div class="relative">
                <input v-model="passwordForm.currentPassword" :type="showPassword.current ? 'text' : 'password'" class="input pr-10" placeholder="请输入当前密码" />
                <button type="button" @click="togglePasswordVisibility('current')" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 focus:outline-none">
                  <svg v-if="showPassword.current" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                  </svg>
                </button>
              </div>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">新密码</label>
              <div class="relative">
                <input v-model="passwordForm.newPassword" :type="showPassword.new ? 'text' : 'password'" class="input pr-10" placeholder="请输入新密码" />
                <button type="button" @click="togglePasswordVisibility('new')" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 focus:outline-none">
                  <svg v-if="showPassword.new" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                  </svg>
                </button>
              </div>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">确认新密码</label>
              <div class="relative">
                <input v-model="passwordForm.confirmPassword" :type="showPassword.confirm ? 'text' : 'password'" class="input pr-10" placeholder="请再次输入新密码" />
                <button type="button" @click="togglePasswordVisibility('confirm')" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 focus:outline-none">
                  <svg v-if="showPassword.confirm" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                  </svg>
                </button>
              </div>
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
            <router-link v-if="canAccessApproval" to="/todo" class="block p-3 bg-warning-50 rounded-lg hover:bg-warning-100 transition-colors cursor-pointer">
              <p class="font-medium text-warning-600">待办事项</p>
              <p class="text-xs text-warning-500">查看待处理的审批</p>
            </router-link>
            <router-link v-if="canAccessApproval" to="/done" class="block p-3 bg-success-50 rounded-lg hover:bg-success-100 transition-colors cursor-pointer">
              <p class="font-medium text-success-600">已办事项</p>
              <p class="text-xs text-success-500">查看已处理的审批</p>
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <ConfirmDialog
      :visible="showAlert"
      :title="alertTitle"
      :message="alertMessage"
      :show-cancel="false"
      @confirm="showAlert = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useApprovalStore } from '@/stores/approval'
import { useUserStore } from '@/stores/user'
import { hasApprovalPermission } from '@/utils/permission'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const authStore = useAuthStore()
const approvalStore = useApprovalStore()
const userStore = useUserStore()

const canAccessApproval = computed(() => hasApprovalPermission(authStore.permissions))

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

// 密码可见性状态
const showPassword = ref({
  current: false,
  new: false,
  confirm: false
})

// 切换密码可见性
function togglePasswordVisibility(field) {
  showPassword.value[field] = !showPassword.value[field]
}

const showAlert = ref(false)
const alertTitle = ref('提示')
const alertMessage = ref('')

function showAlertDialog(title, message) {
  alertTitle.value = title
  alertMessage.value = message
  showAlert.value = true
}

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

async function handleSave() {
  const profileData = {
    name: form.value.name,
    phone: form.value.phone,
    email: form.value.email,
    department: form.value.department
  }

  const result = await userStore.updateProfile(profileData)
  if (result.success) {
    showAlertDialog('保存成功', '保存成功！')
    // 刷新用户信息
    await authStore.fetchCurrentUser()
  } else {
    showAlertDialog('保存失败', '保存失败：' + result.message)
  }
}

function resetForm() {
  initForm()
}

async function handleChangePassword() {
  // 前端验证
  if (!passwordForm.value.currentPassword) {
    showAlertDialog('验证失败', '请输入原密码')
    return
  }
  if (!passwordForm.value.newPassword) {
    showAlertDialog('验证失败', '请输入新密码')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    showAlertDialog('验证失败', '两次输入的新密码不一致')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    showAlertDialog('验证失败', '新密码长度不能少于6位')
    return
  }

  // 调用 API
  const result = await userStore.changePassword({
    oldPassword: passwordForm.value.currentPassword,
    newPassword: passwordForm.value.newPassword
  })

  if (result.success) {
    showAlertDialog('修改成功', '密码修改成功！')
    // 清空表单
    passwordForm.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  } else {
    showAlertDialog('修改失败', '密码修改失败：' + result.message)
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
