/**
 * 权限判断工具函数
 * 统一封装前端权限校验逻辑
 */

/**
 * 检查是否拥有指定权限
 * 拥有 'all' 权限视为拥有所有权限
 * @param {string[]} permissions - 当前用户权限列表
 * @param {string} permission - 需要检查的权限
 * @returns {boolean}
 */
export function hasPermission(permissions, permission) {
  if (!permissions || !Array.isArray(permissions)) return false
  if (permissions.includes('all')) return true
  return permissions.includes(permission)
}

/**
 * 检查是否拥有任意一个指定权限
 * 拥有 'all' 权限视为拥有所有权限
 * @param {string[]} permissions - 当前用户权限列表
 * @param {string[]} requiredPermissions - 需要检查的权限列表
 * @returns {boolean}
 */
export function hasAnyPermission(permissions, requiredPermissions) {
  if (!permissions || !Array.isArray(permissions)) return false
  if (permissions.includes('all')) return true
  if (!requiredPermissions || !Array.isArray(requiredPermissions)) return false
  return requiredPermissions.some(p => permissions.includes(p))
}

/**
 * 检查是否拥有所有指定权限
 * 拥有 'all' 权限视为拥有所有权限
 * @param {string[]} permissions - 当前用户权限列表
 * @param {string[]} requiredPermissions - 需要检查的权限列表
 * @returns {boolean}
 */
export function hasAllPermissions(permissions, requiredPermissions) {
  if (!permissions || !Array.isArray(permissions)) return false
  if (permissions.includes('all')) return true
  if (!requiredPermissions || !Array.isArray(requiredPermissions)) return false
  return requiredPermissions.every(p => permissions.includes(p))
}

/**
 * 检查是否拥有审批相关权限
 * @param {string[]} permissions - 当前用户权限列表
 * @returns {boolean}
 */
export function hasApprovalPermission(permissions) {
  return hasAnyPermission(permissions, [
    'approval',
    'approval:execute',
    'approval:execute:all',
    'approval:execute:dept',
    'approval:view:all'
  ])
}

/**
 * 检查是否拥有审批操作权限
 * @param {string[]} permissions - 当前用户权限列表
 * @returns {boolean}
 */
export function hasApprovalExecutePermission(permissions) {
  return hasAnyPermission(permissions, [
    'approval:execute',
    'approval:execute:all',
    'approval:execute:dept'
  ])
}

/**
 * 检查是否拥有指定角色
 * @param {string} currentRole - 当前用户角色
 * @param {string|string[]} roles - 需要的角色
 * @returns {boolean}
 */
export function hasRole(currentRole, roles) {
  if (!currentRole) return false
  const roleList = Array.isArray(roles) ? roles : [roles]
  return roleList.includes(currentRole)
}
