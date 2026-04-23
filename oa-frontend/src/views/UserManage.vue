<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-gray-900">用户管理</h2>
      <button v-if="canManageUsers" @click="showAddModal = true" class="btn btn-primary flex items-center gap-2">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
        </svg>
        添加用户
      </button>
    </div>

    <div class="card">
      <div class="flex items-center gap-4 mb-6">
        <div class="relative flex-1 min-w-[240px]">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none">
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
          </svg>
          <input v-model="searchQuery" type="text" placeholder="搜索用户名、姓名、邮箱..." class="input w-full pl-10" />
        </div>
        <select v-model="filterRole" class="input w-40">
          <option value="">全部角色</option>
          <option value="admin">系统管理员</option>
          <option value="manager">部门经理</option>
          <option value="employee">普通员工</option>
        </select>
      </div>

      <table class="w-full">
        <thead class="bg-gray-50 border-b border-gray-200">
          <tr>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">用户</th>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">用户名</th>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">邮箱</th>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">部门</th>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">角色</th>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">状态</th>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200">
          <tr v-for="user in filteredUsers" :key="user.id" class="hover:bg-gray-50 transition-colors">
            <td class="px-6 py-4">
              <div class="flex items-center gap-3">
                <img :src="user.avatar" :alt="user.name" class="w-10 h-10 rounded-full" />
                <div>
                  <p class="font-medium text-gray-900">{{ user.name }}</p>
                  <p class="text-sm text-gray-500">{{ user.phone }}</p>
                </div>
              </div>
            </td>
            <td class="px-6 py-4">
              <span class="text-gray-600">{{ user.username }}</span>
            </td>
            <td class="px-6 py-4">
              <span class="text-gray-600">{{ user.email }}</span>
            </td>
            <td class="px-6 py-4">
              <span class="text-gray-600">{{ user.department }}</span>
            </td>
            <td class="px-6 py-4">
              <span class="badge badge-primary">{{ getRoleLabel(user.role) }}</span>
            </td>
            <td class="px-6 py-4">
              <span :class="['badge', user.status === 'active' ? 'badge-success' : 'badge-danger']">
                {{ user.status === 'active' ? '正常' : '禁用' }}
              </span>
            </td>
            <td class="px-6 py-4">
              <div class="flex items-center gap-2">
                <button v-if="canManageUsers" @click="editUser(user)" class="text-primary-600 hover:text-primary-700 text-sm font-medium cursor-pointer">
                  编辑
                </button>
                <button v-if="canManageUsers" @click="deleteUser(user.id)" class="text-danger-600 hover:text-danger-700 text-sm font-medium cursor-pointer">
                  删除
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredUsers.length === 0">
            <td colspan="7" class="px-6 py-12 text-center text-gray-500">
              暂无用户数据
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <ConfirmDialog
      :visible="showDeleteConfirm"
      title="删除确认"
      message="确定要删除该用户吗？"
      @confirm="handleDeleteConfirm"
      @cancel="showDeleteConfirm = false"
    />

    <div v-if="showAddModal || showEditModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeModal">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-6">{{ showAddModal ? '添加用户' : '编辑用户' }}</h3>
        
        <form @submit.prevent="handleSubmit" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">姓名 <span class="text-danger-500">*</span></label>
            <input v-model="userForm.name" type="text" class="input" required />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">用户名 <span class="text-danger-500">*</span></label>
            <input v-model="userForm.username" type="text" class="input" required :disabled="showEditModal" />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">邮箱 <span class="text-danger-500">*</span></label>
            <input v-model="userForm.email" type="email" class="input" required />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">手机号</label>
            <input v-model="userForm.phone" type="tel" class="input" />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">部门</label>
            <select v-model="userForm.deptId" class="input w-full">
              <option value="">请选择部门</option>
              <option v-for="dept in userStore.departments" :key="dept.id" :value="dept.id">{{ dept.name }}</option>
            </select>
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">角色 <span class="text-danger-500">*</span></label>
            <select v-model="userForm.role" class="input" required>
              <option value="">请选择角色</option>
              <option value="admin">系统管理员</option>
              <option value="manager">部门经理</option>
              <option value="employee">普通员工</option>
            </select>
          </div>
          
          <div v-if="showAddModal">
            <label class="block text-sm font-medium text-gray-700 mb-1">密码 <span class="text-danger-500">*</span></label>
            <input v-model="userForm.password" type="password" class="input" required />
          </div>
          
          <div class="flex gap-3 pt-4">
            <button type="submit" class="btn btn-primary flex-1">
              {{ showAddModal ? '添加' : '保存' }}
            </button>
            <button type="button" @click="closeModal" class="btn btn-secondary">
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useAuthStore } from '@/stores/auth'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const userStore = useUserStore()
const authStore = useAuthStore()

const canManageUsers = computed(() => authStore.checkPermission('user_manage') || authStore.checkPermission('all'))

const searchQuery = ref('')
const filterRole = ref('')
const showAddModal = ref(false)
const showEditModal = ref(false)
const showDeleteConfirm = ref(false)
const deletingUserId = ref(null)
const editingUserId = ref(null)
const userForm = ref({
  name: '',
  username: '',
  email: '',
  phone: '',
  deptId: '',
  role: '',
  password: ''
})

const filteredUsers = computed(() => {
  return userStore.users.filter(user => {
    const matchSearch = user.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
                       user.username.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
                       user.email.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchRole = !filterRole.value || user.role === filterRole.value
    return matchSearch && matchRole
  })
})

function getRoleLabel(role) {
  const labels = {
    admin: '系统管理员',
    manager: '部门经理',
    employee: '普通员工'
  }
  return labels[role] || role
}

function editUser(user) {
  editingUserId.value = user.id
  userForm.value = {
    name: user.name,
    username: user.username,
    email: user.email,
    phone: user.phone,
    deptId: user.deptId || '',
    role: user.role,
    password: ''
  }
  showEditModal.value = true
}

function deleteUser(id) {
  deletingUserId.value = id
  showDeleteConfirm.value = true
}

function handleDeleteConfirm() {
  if (deletingUserId.value !== null) {
    userStore.deleteUser(deletingUserId.value)
  }
  showDeleteConfirm.value = false
  deletingUserId.value = null
}

function handleSubmit() {
  if (showAddModal.value) {
    userStore.addUser(userForm.value)
  } else {
    userStore.updateUser(editingUserId.value, userForm.value)
  }
  closeModal()
}

function closeModal() {
  showAddModal.value = false
  showEditModal.value = false
  editingUserId.value = null
  userForm.value = {
    name: '',
    username: '',
    email: '',
    phone: '',
    deptId: '',
    role: '',
    password: ''
  }
}

onMounted(() => {
  userStore.fetchUsers()
  userStore.fetchRoles()
})
</script>
