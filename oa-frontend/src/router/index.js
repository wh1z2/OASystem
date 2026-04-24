import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { hasPermission, hasAnyPermission, hasRole, hasApprovalPermission } from '@/utils/permission'
import { showAuthExpiredDialog, clearAuthStorage } from '@/utils/authDialog.js'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: 'todo',
        name: 'TodoList',
        meta: { permissionCheck: (perms) => hasApprovalPermission(perms) },
        component: () => import('@/views/TodoList.vue')
      },
      {
        path: 'done',
        name: 'DoneList',
        meta: { permissionCheck: (perms) => hasApprovalPermission(perms) },
        component: () => import('@/views/DoneList.vue')
      },
      {
        path: 'approval',
        name: 'ApprovalManage',
        meta: { permissionCheck: (perms) => hasApprovalPermission(perms) || hasPermission(perms, 'apply') },
        component: () => import('@/views/ApprovalManage.vue')
      },
      {
        path: 'approval/create',
        name: 'ApprovalCreate',
        meta: { permissionCheck: (perms) => hasPermission(perms, 'apply') },
        component: () => import('@/views/ApprovalCreate.vue')
      },
      {
        path: 'approval/detail/:id',
        name: 'ApprovalDetail',
        meta: { permissionCheck: (perms) => hasApprovalPermission(perms) || hasPermission(perms, 'apply') },
        component: () => import('@/views/ApprovalDetail.vue')
      },
      {
        path: 'approval/edit/:id',
        name: 'ApprovalEdit',
        meta: { permissionCheck: (perms) => hasPermission(perms, 'apply') },
        component: () => import('@/views/ApprovalCreate.vue')
      },
      {
        path: 'form-designer',
        name: 'FormDesigner',
        meta: { permissionCheck: (perms) => hasPermission(perms, 'form_design') },
        component: () => import('@/views/FormDesigner.vue')
      },
      {
        path: 'users',
        name: 'UserManage',
        meta: { permissionCheck: (perms) => hasAnyPermission(perms, ['user_view', 'user_manage']) },
        component: () => import('@/views/UserManage.vue')
      },
      {
        path: 'roles',
        name: 'RoleManage',
        meta: { permissionCheck: (perms) => hasPermission(perms, 'role_manage') || hasPermission(perms, 'all') },
        component: () => import('@/views/RoleManage.vue')
      },
      {
        path: 'approver-rules',
        name: 'ApproverRuleManage',
        meta: { permissionCheck: (perms) => hasPermission(perms, 'role_manage') || hasPermission(perms, 'all') },
        component: () => import('@/views/ApproverRuleManage.vue')
      },
      {
        path: 'approved',
        name: 'ApprovedList',
        meta: { permissionCheck: (perms) => hasApprovalPermission(perms) },
        component: () => import('@/views/ApprovedList.vue')
      },
      {
        path: 'rejected',
        name: 'RejectedList',
        meta: { permissionCheck: (perms) => hasApprovalPermission(perms) },
        component: () => import('@/views/RejectedList.vue')
      },
      {
        path: 'my-applications',
        name: 'MyApplicationList',
        component: () => import('@/views/MyApplicationList.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // Token 过期前置检查：如 token 已过期且无 refreshToken，直接清理并跳转
  if (to.meta.requiresAuth && authStore.isAuthenticated) {
    const tokenExpiresAt = Number(localStorage.getItem('tokenExpiresAt') || 0)
    const refreshToken = localStorage.getItem('refreshToken')
    if (tokenExpiresAt > 0 && Date.now() > tokenExpiresAt && !refreshToken) {
      clearAuthStorage()
      showAuthExpiredDialog()
      next('/login')
      return
    }
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
    return
  }

  if (to.path === '/login' && authStore.isAuthenticated) {
    next('/')
    return
  }

  // 权限校验
  if (to.meta.permissionCheck && typeof to.meta.permissionCheck === 'function') {
    if (!to.meta.permissionCheck(authStore.permissions)) {
      next('/')
      return
    }
  }

  next()
})

export default router
