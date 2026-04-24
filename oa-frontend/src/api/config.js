import axios from 'axios'
import { showAuthExpiredDialog, clearAuthStorage } from '@/utils/authDialog.js'

// 创建 axios 实例
const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 独立的 axios 实例用于刷新 token，避免拦截器递归调用
const refreshClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

let isRefreshing = false
let refreshSubscribers = []

function onTokenRefreshed() {
  refreshSubscribers.forEach(cb => cb())
  refreshSubscribers = []
}

function addRefreshSubscriber(cb) {
  refreshSubscribers.push(cb)
}

/**
 * 处理 Token 刷新
 * 使用独立的 refreshClient 发起请求，避免当前 apiClient 的拦截器递归
 */
async function handleTokenRefresh() {
  if (isRefreshing) {
    return new Promise(resolve => {
      addRefreshSubscriber(() => resolve())
    })
  }

  isRefreshing = true
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) {
    isRefreshing = false
    return Promise.reject(new Error('no refresh token'))
  }

  try {
    const res = await refreshClient.post('/auth/refresh', { refreshToken })
    const result = res.data
    if (result.code !== 200) {
      throw new Error(result.message || '刷新失败')
    }
    const data = result.data
    localStorage.setItem('token', data.token)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('tokenExpiresAt', String(Date.now() + (data.expiresIn || 1800000)))
    onTokenRefreshed()
    isRefreshing = false
  } catch (err) {
    isRefreshing = false
    refreshSubscribers = []
    throw err
  }
}

// 请求拦截器 - 添加 token + 预检即将过期的 token
apiClient.interceptors.request.use(
  async (config) => {
    const token = localStorage.getItem('token')
    const expiresAt = Number(localStorage.getItem('tokenExpiresAt') || 0)
    const lastActivity = Number(localStorage.getItem('lastActivity') || 0)
    const now = Date.now()

    // Token 将在 5 分钟内过期，且用户 30 分钟内有活动，尝试静默刷新
    if (token && expiresAt > 0 && expiresAt - now < 5 * 60 * 1000) {
      if (now - lastActivity < 30 * 60 * 1000) {
        try {
          await handleTokenRefresh()
        } catch (e) {
          // 静默刷新失败不阻断请求，让后端返回 401 后再统一弹窗
        }
      }
    }

    const currentToken = localStorage.getItem('token')
    if (currentToken) {
      config.headers.Authorization = `Bearer ${currentToken}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 统一处理错误
apiClient.interceptors.response.use(
  (response) => {
    // 后端返回格式: { code, message, data, timestamp }
    const { code, message, data } = response.data
    if (code === 401) {
      // 后端返回业务码 401（HTTP 200），属于认证过期
      return Promise.reject({
        isAuthError: true,
        message: message || '登录已过期，请重新登录',
        response: response,
        config: response.config
      })
    }
    if (code !== 200) {
      return Promise.reject(new Error(message || '请求失败'))
    }
    return data
  },
  (error) => {
    // 处理认证类错误（isAuthError、HTTP 401、或 body.code=401）
    if (error.isAuthError || error.response?.status === 401 || error.response?.data?.code === 401) {
      const originalConfig = error.config || error.response?.config
      if (!originalConfig) {
        clearAuthStorage()
        showAuthExpiredDialog()
        return Promise.reject(error)
      }

      return handleTokenRefresh().then(() => {
        // 刷新成功，携带新 token 重试原请求
        originalConfig.headers.Authorization = `Bearer ${localStorage.getItem('token')}`
        return apiClient.request(originalConfig)
      }).catch(() => {
        // 刷新失败，弹窗提示并清理状态
        clearAuthStorage()
        showAuthExpiredDialog()
        return Promise.reject(error)
      })
    }

    // 网络或服务器错误
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 400:
          error.message = data?.message || '请求参数错误'
          break
        case 403:
          error.message = '没有权限执行此操作'
          break
        case 404:
          error.message = '请求的资源不存在'
          break
        case 500:
          error.message = '服务器内部错误，请稍后重试'
          break
        default:
          error.message = data?.message || `请求失败(${status})`
      }
    } else if (error.request) {
      error.message = '网络连接失败，请检查网络设置'
    } else {
      error.message = '请求配置错误'
    }
    return Promise.reject(error)
  }
)

export default apiClient
