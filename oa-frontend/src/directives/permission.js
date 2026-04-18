import { hasPermission, hasAnyPermission, hasRole } from '@/utils/permission.js'
import { useAuthStore } from '@/stores/auth.js'

/**
 * v-permission 自定义指令
 * 根据当前用户权限控制元素显示/隐藏
 *
 * 用法:
 * v-permission="'user_manage'"              // 需要 user_manage 权限
 * v-permission="['user_manage', 'all']"     // 需要任意一个权限
 * v-permission:role="'admin'"               // 需要 admin 角色
 * v-permission:role="['admin', 'manager']"  // 需要任意一个角色
 */
function checkPermission(el, binding) {
  const authStore = useAuthStore()
  const permissions = authStore.permissions
  const userRole = authStore.currentUser?.role

  const { arg, value } = binding

  let hasAuth = false

  if (arg === 'role') {
    // 角色校验
    const roles = Array.isArray(value) ? value : [value]
    hasAuth = hasRole(userRole, roles)
  } else {
    // 权限校验
    if (Array.isArray(value)) {
      hasAuth = hasAnyPermission(permissions, value)
    } else if (typeof value === 'string') {
      hasAuth = hasPermission(permissions, value)
    }
  }

  if (!hasAuth && el.parentNode) {
    el.parentNode.removeChild(el)
  }
}

export const permissionDirective = {
  mounted(el, binding) {
    checkPermission(el, binding)
  },
  updated(el, binding) {
    // 如果权限发生变化，这里可以重新校验
    // 但由于我们已经从 DOM 中移除了元素，updated 通常不会触发
    // 实际场景中权限变化需要重新加载页面或组件
    checkPermission(el, binding)
  }
}

export default permissionDirective
