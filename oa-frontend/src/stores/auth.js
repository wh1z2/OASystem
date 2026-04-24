import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '@/api/config.js'
import { hasPermission, hasAnyPermission, hasRole } from '@/utils/permission.js'
import { showAuthExpiredDialog, clearAuthStorage } from '@/utils/authDialog.js'

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

      // 后端返回: { token, tokenType, expiresIn, refreshToken, refreshExpiresAt, user }
      token.value = data.token
      user.value = data.user

      localStorage.setItem('token', data.token)
      localStorage.setItem('refreshToken', data.refreshToken)
      localStorage.setItem('user', JSON.stringify(data.user))
      localStorage.setItem('tokenExpiresAt', String(Date.now() + (data.expiresIn || 1800000)))
      localStorage.setItem('lastActivity', String(Date.now()))

      return { success: true }
    } catch (error) {
      return { success: false, message: error.message || '用户名或密码错误' }
    }
  }

  // 登出
  async function logout() {
    try {
      // 调用后端登出接口，撤销 refresh token
      await apiClient.post('/auth/logout')
    } catch (error) {
      // 忽略后端错误，确保前端状态一定被清除
    }
    user.value = null
    token.value = null
    clearAuthStorage()
  }

  // 获取当前用户信息 - 调用后端接口
  async function fetchCurrentUser() {
    try {
      const data = await apiClient.get('/auth/info')
      // 合并新旧数据，防止后端返回字段不全导致已有数据丢失
      user.value = { ...user.value, ...data }
      localStorage.setItem('user', JSON.stringify(user.value))
      return user.value
    } catch (error) {
      // Token 无效，清除登录状态并弹窗提示
      user.value = null
      token.value = null
      clearAuthStorage()
      showAuthExpiredDialog()
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
