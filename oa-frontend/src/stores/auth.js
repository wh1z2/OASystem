import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || null)

  const isAuthenticated = computed(() => !!token.value)
  const currentUser = computed(() => user.value)

  function login(credentials) {
    const mockUsers = [
      { id: 1, username: 'admin', password: 'admin123', name: '系统管理员', role: 'admin', department: '信息技术部', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin' },
      { id: 2, username: 'manager', password: 'manager123', name: '张经理', role: 'manager', department: '人力资源部', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=manager' },
      { id: 3, username: 'user', password: 'user123', name: '李员工', role: 'employee', department: '市场部', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=user' }
    ]

    const foundUser = mockUsers.find(
      u => u.username === credentials.username && u.password === credentials.password
    )

    if (foundUser) {
      const { password: _, ...userWithoutPassword } = foundUser
      user.value = userWithoutPassword
      token.value = 'mock-jwt-token-' + Date.now()
      localStorage.setItem('token', token.value)
      localStorage.setItem('user', JSON.stringify(userWithoutPassword))
      return { success: true }
    }

    return { success: false, message: '用户名或密码错误' }
  }

  function logout() {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  function initAuth() {
    const savedUser = localStorage.getItem('user')
    if (savedUser && token.value) {
      user.value = JSON.parse(savedUser)
    }
  }

  return {
    user,
    token,
    isAuthenticated,
    currentUser,
    login,
    logout,
    initAuth
  }
})
