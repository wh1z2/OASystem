<template>
  <div class="min-h-screen bg-gray-50 flex">
    <aside class="w-64 bg-white border-r border-gray-200 fixed h-full">
      <div class="p-6 border-b border-gray-200">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-primary-600 rounded-lg flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6 text-white">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25zM6.75 12h.008v.008H6.75V12zm0 3h.008v.008H6.75V15zm0 3h.008v.008H6.75V18z" />
            </svg>
          </div>
          <div>
            <h1 class="font-bold text-gray-900">OA审批系统</h1>
            <p class="text-xs text-gray-500">Approval System</p>
          </div>
        </div>
      </div>
      
      <nav class="p-4">
        <div class="mb-2">
          <span class="text-xs font-medium text-gray-400 uppercase tracking-wider">工作台</span>
        </div>
        <router-link to="/" class="sidebar-item" :class="{ active: $route.path === '/' }">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 12l8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25" />
          </svg>
          <span>工作台</span>
        </router-link>
        
        <router-link v-if="showTodoMenu" to="/todo" class="sidebar-item" :class="{ active: $route.path === '/todo' }">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>待办事项</span>
          <span v-if="pendingCount > 0" class="ml-auto bg-danger-500 text-white text-xs px-2 py-0.5 rounded-full">{{ pendingCount }}</span>
        </router-link>

        <router-link v-if="showDoneMenu" to="/done" class="sidebar-item" :class="{ active: $route.path === '/done' }">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>已办事项</span>
        </router-link>

        <div v-if="showApprovalSection" class="mb-2 mt-6">
          <span class="text-xs font-medium text-gray-400 uppercase tracking-wider">审批管理</span>
        </div>
        <router-link v-if="showApprovalMenu" to="/approval" class="sidebar-item" :class="{ active: $route.path.startsWith('/approval') }">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
          </svg>
          <span>审批流程</span>
        </router-link>
        <router-link v-if="showFormDesignerMenu" to="/form-designer" class="sidebar-item" :class="{ active: $route.path === '/form-designer' }">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <span>表单设计器</span>
        </router-link>

        <div v-if="showSystemSection" class="mb-2 mt-6">
          <span class="text-xs font-medium text-gray-400 uppercase tracking-wider">系统管理</span>
        </div>
        <router-link v-if="showUserMenuItem" to="/users" class="sidebar-item" :class="{ active: $route.path === '/users' }">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.38 9.38 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" />
          </svg>
          <span>用户管理</span>
        </router-link>
        <router-link v-if="showRoleMenu" to="/roles" class="sidebar-item" :class="{ active: $route.path === '/roles' }">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z" />
          </svg>
          <span>角色权限</span>
        </router-link>
        <router-link v-if="showApproverRuleMenu" to="/approver-rules" class="sidebar-item" :class="{ active: $route.path === '/approver-rules' }">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
          </svg>
          <span>审批规则配置</span>
        </router-link>
      </nav>
    </aside>

    <div class="flex-1 ml-64">
      <header class="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div class="flex items-center justify-between px-6 py-4">
          <div class="flex items-center gap-4">
            <h2 class="text-lg font-semibold text-gray-900">{{ pageTitle }}</h2>
          </div>
          
          <div class="flex items-center gap-4">
            <button class="relative p-2 text-gray-400 hover:text-gray-600 transition-colors cursor-pointer">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
                <path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
              </svg>
              <span class="absolute top-1 right-1 w-2 h-2 bg-danger-500 rounded-full"></span>
            </button>
            
            <div class="relative" ref="userMenuRef">
              <button @click="showUserMenu = !showUserMenu" class="flex items-center gap-3 cursor-pointer hover:bg-gray-50 rounded-lg p-2 transition-colors">
                <img :src="authStore.currentUser?.avatar" :alt="authStore.currentUser?.name" class="w-8 h-8 rounded-full">
                <div class="text-left">
                  <p class="text-sm font-medium text-gray-900">{{ authStore.currentUser?.name }}</p>
                  <p class="text-xs text-gray-500">{{ getRoleLabel(authStore.currentUser?.role) }}</p>
                </div>
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4 text-gray-400">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                </svg>
              </button>
              
              <div v-if="showUserMenu" class="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1">
                <router-link to="/profile" class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50" @click="showUserMenu = false">
                  个人中心
                </router-link>
                <button @click="handleLogout" class="w-full text-left px-4 py-2 text-sm text-danger-600 hover:bg-gray-50 cursor-pointer">
                  退出登录
                </button>
              </div>
            </div>
          </div>
        </div>
      </header>

      <main class="p-6">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useApprovalStore } from '@/stores/approval'
import { hasPermission, hasAnyPermission, hasApprovalPermission } from '@/utils/permission'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const approvalStore = useApprovalStore()

const showUserMenu = ref(false)
const userMenuRef = ref(null)

// 使用后端返回的各列表总数（通过 fetchXxxList 获取）
const pendingCount = computed(() => approvalStore.pendingCount)
const doneCount = computed(() => approvalStore.doneCount)
const myApprovalCount = computed(() => approvalStore.myApprovalCount)

// 菜单权限控制
const showTodoMenu = computed(() => hasApprovalPermission(authStore.permissions))
const showDoneMenu = computed(() => hasApprovalPermission(authStore.permissions))
const showApprovalMenu = computed(() => hasApprovalPermission(authStore.permissions) || authStore.checkPermission('apply'))
const showFormDesignerMenu = computed(() => authStore.checkPermission('form_design'))
const showUserMenuItem = computed(() => authStore.checkAnyPermission(['user_view', 'user_manage']))
const showRoleMenu = computed(() => authStore.checkPermission('role_manage') || authStore.checkPermission('all'))
const showApproverRuleMenu = computed(() => authStore.checkPermission('role_manage') || authStore.checkPermission('all'))

const showApprovalSection = computed(() => showApprovalMenu.value || showFormDesignerMenu.value)
const showSystemSection = computed(() => showUserMenuItem.value || showRoleMenu.value || showApproverRuleMenu.value)

const pageTitle = computed(() => {
  const titles = {
    '/': '工作台',
    '/todo': '待办事项',
    '/done': '已办事项',
    '/approval': '审批流程',
    '/approval/create': '发起审批',
    '/form-designer': '表单设计器',
    '/users': '用户管理',
    '/roles': '角色权限',
    '/approver-rules': '审批规则配置',
    '/profile': '个人中心'
  }
  return titles[route.path] || 'OA审批系统'
})

function getRoleLabel(role) {
  const labels = {
    admin: '系统管理员',
    manager: '部门经理',
    employee: '普通员工'
  }
  return labels[role] || role
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function handleClickOutside(event) {
  if (userMenuRef.value && !userMenuRef.value.contains(event.target)) {
    showUserMenu.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  // 获取各列表总数，确保侧边栏徽章计数正确
  approvalStore.fetchTodoList()
  approvalStore.fetchDoneList()
  approvalStore.fetchMyApprovals()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>
