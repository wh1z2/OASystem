import axios from 'axios'

// 创建 axios 实例
const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 添加 token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
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
    if (code !== 200) {
      // 业务逻辑错误
      return Promise.reject(new Error(message || '请求失败'))
    }
    return data
  },
  (error) => {
    // 网络或服务器错误
    if (error.response) {
      // 服务器返回了错误响应
      const { status, data } = error.response
      switch (status) {
        case 400:
          error.message = data?.message || '请求参数错误'
          break
        case 401:
          error.message = '登录已过期，请重新登录'
          // 清除本地认证信息
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          // 跳转到登录页
          window.location.href = '/login'
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
      // 请求发送但没有收到响应
      error.message = '网络连接失败，请检查网络设置'
    } else {
      // 请求配置错误
      error.message = '请求配置错误'
    }
    return Promise.reject(error)
  }
)

export default apiClient
