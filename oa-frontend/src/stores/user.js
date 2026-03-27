import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const users = ref([
    { id: 1, username: 'admin', name: '系统管理员', email: 'admin@company.com', role: 'admin', department: '信息技术部', status: 'active', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', phone: '13800138001' },
    { id: 2, username: 'manager', name: '张经理', email: 'zhang@company.com', role: 'manager', department: '人力资源部', status: 'active', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=manager', phone: '13800138002' },
    { id: 3, username: 'user', name: '李员工', email: 'li@company.com', role: 'employee', department: '市场部', status: 'active', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=user', phone: '13800138003' },
    { id: 4, username: 'sales', name: '王销售', email: 'wang@company.com', role: 'employee', department: '销售部', status: 'active', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=sales', phone: '13800138004' },
    { id: 5, username: 'admin2', name: '赵行政', email: 'zhao@company.com', role: 'employee', department: '行政部', status: 'active', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin2', phone: '13800138005' }
  ])

  const roles = ref([
    { id: 1, name: 'admin', label: '系统管理员', permissions: ['all'], description: '拥有系统所有权限' },
    { id: 2, name: 'manager', label: '部门经理', permissions: ['approval', 'user_view', 'report'], description: '审批权限、查看用户、查看报表' },
    { id: 3, name: 'employee', label: '普通员工', permissions: ['apply', 'personal'], description: '提交申请、个人信息管理' }
  ])

  const activeUsers = computed(() => users.value.filter(u => u.status === 'active'))
  const userCount = computed(() => users.value.length)

  function getUserById(id) {
    return users.value.find(u => u.id === parseInt(id))
  }

  function addUser(user) {
    const newUser = {
      id: users.value.length + 1,
      ...user,
      status: 'active',
      avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${user.username}`
    }
    users.value.push(newUser)
    return newUser
  }

  function updateUser(id, data) {
    const index = users.value.findIndex(u => u.id === parseInt(id))
    if (index !== -1) {
      users.value[index] = { ...users.value[index], ...data }
      return users.value[index]
    }
    return null
  }

  function deleteUser(id) {
    const index = users.value.findIndex(u => u.id === parseInt(id))
    if (index !== -1) {
      users.value.splice(index, 1)
      return true
    }
    return false
  }

  function getRoleById(id) {
    return roles.value.find(r => r.id === parseInt(id))
  }

  function addRole(role) {
    const newRole = {
      id: roles.value.length + 1,
      ...role
    }
    roles.value.push(newRole)
    return newRole
  }

  function updateRole(id, data) {
    const index = roles.value.findIndex(r => r.id === parseInt(id))
    if (index !== -1) {
      roles.value[index] = { ...roles.value[index], ...data }
      return roles.value[index]
    }
    return null
  }

  function deleteRole(id) {
    const index = roles.value.findIndex(r => r.id === parseInt(id))
    if (index !== -1) {
      roles.value.splice(index, 1)
      return true
    }
    return false
  }

  return {
    users,
    roles,
    activeUsers,
    userCount,
    getUserById,
    addUser,
    updateUser,
    deleteUser,
    getRoleById,
    addRole,
    updateRole,
    deleteRole
  }
})
