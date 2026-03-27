import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

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
        component: () => import('@/views/TodoList.vue')
      },
      {
        path: 'done',
        name: 'DoneList',
        component: () => import('@/views/DoneList.vue')
      },
      {
        path: 'approval',
        name: 'ApprovalManage',
        component: () => import('@/views/ApprovalManage.vue')
      },
      {
        path: 'approval/create',
        name: 'ApprovalCreate',
        component: () => import('@/views/ApprovalCreate.vue')
      },
      {
        path: 'approval/detail/:id',
        name: 'ApprovalDetail',
        component: () => import('@/views/ApprovalDetail.vue')
      },
      {
        path: 'form-designer',
        name: 'FormDesigner',
        component: () => import('@/views/FormDesigner.vue')
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('@/views/UserManage.vue')
      },
      {
        path: 'roles',
        name: 'RoleManage',
        component: () => import('@/views/RoleManage.vue')
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

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
  } else if (to.path === '/login' && authStore.isAuthenticated) {
    next('/')
  } else {
    next()
  }
})

export default router
