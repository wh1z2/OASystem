import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '@/api/config.js'

export const useUserStore = defineStore('user', () => {
  const users = ref([])
  const roles = ref([])

  const departments = ref([
    { id: 1, name: '技术部' },
    { id: 2, name: '财务部' },
    { id: 3, name: '人事部' },
    { id: 4, name: '系统管理部' }
  ])

  const activeUsers = computed(() => users.value.filter(u => u.status === 'active'))
  const userCount = computed(() => users.value.length)

  function getUserById(id) {
    return users.value.find(u => u.id === parseInt(id))
  }

  function getRoleIdByName(roleName) {
    const role = roles.value.find(r => r.name === roleName)
    return role ? role.id : null
  }

  function getDeptNameById(deptId) {
    const dept = departments.value.find(d => d.id === deptId)
    return dept ? dept.name : ''
  }

  // 从后端获取用户列表
  async function fetchUsers() {
    try {
      const result = await apiClient.get('/users', { params: { current: 1, size: 1000 } })
      if (result && result.records) {
        users.value = result.records.map(u => ({
          id: u.id,
          username: u.username,
          name: u.name,
          email: u.email,
          phone: u.phone,
          avatar: u.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${u.username}`,
          role: u.roleName,
          deptId: u.deptId,
          department: u.department,
          status: u.status === 1 ? 'active' : 'inactive'
        }))
      }
    } catch (error) {
      console.warn('获取用户列表失败，使用本地数据:', error.message)
    }
  }

  // 创建用户
  async function addUser(user) {
    try {
      const roleId = getRoleIdByName(user.role)
      await apiClient.post('/users', {
        username: user.username,
        password: user.password,
        name: user.name,
        email: user.email,
        phone: user.phone,
        deptId: user.deptId || null,
        roleId: roleId,
        status: 1
      })
      await fetchUsers()
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 更新用户
  async function updateUser(id, data) {
    try {
      const roleId = getRoleIdByName(data.role)
      await apiClient.post(`/users/${id}/update`, {
        name: data.name,
        email: data.email,
        phone: data.phone,
        deptId: data.deptId || null,
        roleId: roleId
      })
      await fetchUsers()
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 删除用户
  async function deleteUser(id) {
    try {
      await apiClient.post(`/users/${id}/delete`)
      await fetchUsers()
      return true
    } catch (error) {
      console.error('删除用户失败:', error.message)
      return false
    }
  }

  // 从后端获取角色列表
  async function fetchRoles() {
    try {
      const result = await apiClient.get('/roles/all')
      if (result) {
        roles.value = result.map(r => ({
          id: r.id,
          name: r.name,
          label: r.label,
          description: r.description,
          permissions: r.permissions ? JSON.parse(r.permissions) : []
        }))
      }
    } catch (error) {
      console.warn('获取角色列表失败，使用本地数据:', error.message)
    }
  }

  function getRoleById(id) {
    return roles.value.find(r => r.id === parseInt(id))
  }

  // 创建角色
  async function addRole(role) {
    try {
      await apiClient.post('/roles', {
        name: role.name,
        label: role.label,
        description: role.description,
        permissions: role.permissions || []
      })
      await fetchRoles()
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 更新角色
  async function updateRole(id, data) {
    try {
      await apiClient.post(`/roles/${id}/update`, {
        label: data.label,
        description: data.description,
        permissions: data.permissions || []
      })
      await fetchRoles()
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 删除角色
  async function deleteRole(id) {
    try {
      await apiClient.post(`/roles/${id}/delete`)
      await fetchRoles()
      return true
    } catch (error) {
      console.error('删除角色失败:', error.message)
      return false
    }
  }

  // 更新个人信息
  async function updateProfile(profileData) {
    try {
      await apiClient.post('/users/profile', profileData)
      const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
      localStorage.setItem('user', JSON.stringify({
        ...currentUser,
        ...profileData
      }))
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 修改密码
  async function changePassword(passwordData) {
    try {
      await apiClient.post('/users/password', passwordData)
      return { success: true }
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || '修改密码失败'
      }
    }
  }

  return {
    users,
    roles,
    departments,
    activeUsers,
    userCount,
    getUserById,
    addUser,
    updateUser,
    deleteUser,
    getRoleById,
    getDeptNameById,
    addRole,
    updateRole,
    deleteRole,
    fetchUsers,
    fetchRoles,
    updateProfile,
    changePassword
  }
})
