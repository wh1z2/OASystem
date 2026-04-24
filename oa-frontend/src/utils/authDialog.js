import { createApp, h } from 'vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

let dialogInstance = null

/**
 * 显示登录过期弹窗
 * 使用系统标准 ConfirmDialog 组件，用户点击确定后跳转到登录页
 */
export function showAuthExpiredDialog() {
  if (dialogInstance) return

  const div = document.createElement('div')
  document.body.appendChild(div)

  const app = createApp({
    render() {
      return h(ConfirmDialog, {
        visible: true,
        title: '提示',
        message: '您的登录已过期，请重新登录',
        showCancel: false,
        onConfirm: () => {
          app.unmount()
          div.remove()
          dialogInstance = null
          window.location.href = '/login'
        },
        onClose: () => {
          app.unmount()
          div.remove()
          dialogInstance = null
          window.location.href = '/login'
        }
      })
    }
  })

  dialogInstance = app
  app.mount(div)
}

/**
 * 清除登录相关 localStorage
 */
export function clearAuthStorage() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  localStorage.removeItem('tokenExpiresAt')
  localStorage.removeItem('lastActivity')
}
