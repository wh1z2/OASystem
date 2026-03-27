<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-gray-900">角色权限管理</h2>
      <button @click="showAddModal = true" class="btn btn-primary flex items-center gap-2">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
        </svg>
        添加角色
      </button>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="role in roles" :key="role.id" class="card hover:shadow-md transition-shadow">
        <div class="flex items-start justify-between mb-4">
          <div class="w-12 h-12 bg-primary-100 rounded-xl flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6 text-primary-600">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z" />
            </svg>
          </div>
          <div class="flex items-center gap-2">
            <button @click="editRole(role)" class="text-gray-400 hover:text-gray-600 cursor-pointer">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
              </svg>
            </button>
            <button v-if="role.name !== 'admin'" @click="deleteRole(role.id)" class="text-gray-400 hover:text-danger-500 cursor-pointer">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
              </svg>
            </button>
          </div>
        </div>
        
        <h3 class="text-lg font-semibold text-gray-900 mb-1">{{ role.label }}</h3>
        <p class="text-sm text-gray-500 mb-4">{{ role.description }}</p>
        
        <div class="space-y-2">
          <p class="text-xs font-medium text-gray-400 uppercase tracking-wider">权限列表</p>
          <div class="flex flex-wrap gap-2">
            <span v-for="permission in role.permissions" :key="permission" 
                  class="badge badge-primary text-xs">
              {{ getPermissionLabel(permission) }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showAddModal || showEditModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeModal">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-6">{{ showAddModal ? '添加角色' : '编辑角色' }}</h3>
        
        <form @submit.prevent="handleSubmit" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">角色名称 <span class="text-danger-500">*</span></label>
            <input v-model="roleForm.name" type="text" class="input" placeholder="如: manager" required :disabled="showEditModal" />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">角色标签 <span class="text-danger-500">*</span></label>
            <input v-model="roleForm.label" type="text" class="input" placeholder="如: 部门经理" required />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">角色描述</label>
            <textarea v-model="roleForm.description" class="input h-20 resize-none" placeholder="请输入角色描述"></textarea>
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">权限设置</label>
            <div class="space-y-2">
              <label v-for="permission in allPermissions" :key="permission.value" class="flex items-center gap-2 cursor-pointer">
                <input v-model="roleForm.permissions" type="checkbox" :value="permission.value" class="text-primary-600" />
                <span class="text-sm text-gray-600">{{ permission.label }}</span>
              </label>
            </div>
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
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const showAddModal = ref(false)
const showEditModal = ref(false)
const editingRoleId = ref(null)
const roleForm = ref({
  name: '',
  label: '',
  description: '',
  permissions: []
})

const allPermissions = [
  { value: 'all', label: '全部权限' },
  { value: 'approval', label: '审批权限' },
  { value: 'apply', label: '申请权限' },
  { value: 'user_view', label: '查看用户' },
  { value: 'user_manage', label: '用户管理' },
  { value: 'role_manage', label: '角色管理' },
  { value: 'report', label: '报表查看' },
  { value: 'personal', label: '个人信息管理' }
]

const roles = computed(() => userStore.roles)

function getPermissionLabel(permission) {
  const perm = allPermissions.find(p => p.value === permission)
  return perm ? perm.label : permission
}

function editRole(role) {
  editingRoleId.value = role.id
  roleForm.value = {
    name: role.name,
    label: role.label,
    description: role.description,
    permissions: [...role.permissions]
  }
  showEditModal.value = true
}

function deleteRole(id) {
  if (confirm('确定要删除该角色吗？')) {
    userStore.deleteRole(id)
  }
}

function handleSubmit() {
  if (showAddModal.value) {
    userStore.addRole(roleForm.value)
  } else {
    userStore.updateRole(editingRoleId.value, roleForm.value)
  }
  closeModal()
}

function closeModal() {
  showAddModal.value = false
  showEditModal.value = false
  editingRoleId.value = null
  roleForm.value = {
    name: '',
    label: '',
    description: '',
    permissions: []
  }
}
</script>
