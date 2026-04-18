import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '@/api/config.js'
import { hasPermission, hasAnyPermission, hasRole } from '@/utils/permission.js'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || null)

  const isAuthenticated = computed(() => !!token.value)
  const currentUser = computed(() => user.value)
  const permissions = computed(() => user.value?.permissions || [])

  /**
   * 检查当前用户是否拥有指定权限
   * @param {string} permission
   * @returns {boolean}
   */
  function checkPermission(permission) {
    return hasPermission(permissions.value, permission)
  }

  /**
   * 检查当前用户是否拥有任意一个指定权限
   * @param {string[]} requiredPermissions
   * @returns {boolean}
   */
  function checkAnyPermission(requiredPermissions) {
    return hasAnyPermission(permissions.value, requiredPermissions)
  }

  /**
   * 检查当前用户是否拥有指定角色
   * @param {string|string[]} roles
   * @returns {boolean}
   */
  function checkRole(roles) {
    return hasRole(currentUser.value?.role, roles)
  }

  // 登录 - 调用后端接口
  async function login(credentials) {
    try {
      const data = await apiClient.post('/auth/login', {
        username: credentials.username,
        password: credentials.password
      })

      // 后端返回: { token, tokenType, expiresIn, user }
      token.value = data.token
      user.value = data.user

      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))

      return { success: true }
    } catch (error) {
      return { success: false, message: error.message || '用户名或密码错误' }
    }
  }

  // 登出
  function logout() {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  // 获取当前用户信息 - 调用后端接口
  async function fetchCurrentUser() {
    try {
      const data = await apiClient.get('/auth/info')
      user.value = data
      localStorage.setItem('user', JSON.stringify(data))
      return data
    } catch (error) {
      // Token 无效，清除登录状态
      logout()
      return null
    }
  }

  // 初始化认证状态
  async function initAuth() {
    const savedToken = localStorage.getItem('token')
    const savedUser = localStorage.getItem('user')

    if (savedToken && savedUser) {
      token.value = savedToken
      user.value = JSON.parse(savedUser)
      // 验证 token 是否有效
      await fetchCurrentUser()
    }
  }

  return {
    user,
    token,
    isAuthenticated,
    currentUser,
    permissions,
    checkPermission,
    checkAnyPermission,
    checkRole,
    login,
    logout,
    initAuth,
    fetchCurrentUser
  }
})
