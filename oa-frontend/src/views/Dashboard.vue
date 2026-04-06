<template>
  <div class="space-y-6">
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <div class="card">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 bg-primary-100 rounded-xl flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6 text-primary-600">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-500">待办事项</p>
            <p class="text-2xl font-bold text-gray-900">{{ stats.pending }}</p>
          </div>
        </div>
      </div>
      
      <div class="card">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 bg-success-50 rounded-xl flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6 text-success-500">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-500">已通过</p>
            <p class="text-2xl font-bold text-gray-900">{{ stats.approved }}</p>
          </div>
        </div>
      </div>
      
      <div class="card">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 bg-danger-50 rounded-xl flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6 text-danger-500">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-500">已拒绝</p>
            <p class="text-2xl font-bold text-gray-900">{{ stats.rejected }}</p>
          </div>
        </div>
      </div>
      
      <div class="card">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 bg-warning-50 rounded-xl flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6 text-warning-500">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-500">本月申请</p>
            <p class="text-2xl font-bold text-gray-900">{{ stats.total }}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2">
        <div class="card">
          <div class="flex items-center justify-between mb-6">
            <h3 class="text-lg font-semibold text-gray-900">待办事项</h3>
            <router-link to="/todo" class="text-sm text-primary-600 hover:text-primary-700 font-medium">
              查看全部
            </router-link>
          </div>
          
          <div class="space-y-4">
            <div v-for="item in pendingList" :key="item.id" 
                 class="flex items-center gap-4 p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer"
                 @click="goToDetail(item.id)">
              <div :class="[
                'w-10 h-10 rounded-lg flex items-center justify-center',
                item.priority === 'high' ? 'bg-danger-50' : item.priority === 'normal' ? 'bg-primary-50' : 'bg-gray-100'
              ]">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5"
                     :class="item.priority === 'high' ? 'text-danger-500' : item.priority === 'normal' ? 'text-primary-600' : 'text-gray-500'">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <p class="font-medium text-gray-900 truncate">{{ item.title }}</p>
                <p class="text-sm text-gray-500">{{ item.applicant }} · {{ item.department }}</p>
              </div>
              <div class="text-right">
                <span :class="[
                  'badge',
                  item.priority === 'high' ? 'badge-danger' : item.priority === 'normal' ? 'badge-primary' : 'bg-gray-100 text-gray-600'
                ]">
                  {{ getPriorityLabel(item.priority) }}
                </span>
                <p class="text-xs text-gray-400 mt-1">{{ item.createTime }}</p>
              </div>
            </div>
            
            <div v-if="pendingList.length === 0" class="text-center py-8 text-gray-500">
              暂无待办事项
            </div>
          </div>
        </div>
      </div>

      <div class="space-y-6">
        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">快捷操作</h3>
          <div class="grid grid-cols-2 gap-3">
            <router-link to="/approval/create" 
                        class="p-4 bg-primary-50 rounded-lg text-center hover:bg-primary-100 transition-colors cursor-pointer">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6 text-primary-600 mx-auto mb-2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
              <span class="text-sm font-medium text-primary-600">发起申请</span>
            </router-link>
            <router-link to="/todo" 
                        class="p-4 bg-warning-50 rounded-lg text-center hover:bg-warning-100 transition-colors cursor-pointer">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6 text-warning-500 mx-auto mb-2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span class="text-sm font-medium text-warning-600">待办审批</span>
            </router-link>
            <router-link to="/done" 
                        class="p-4 bg-success-50 rounded-lg text-center hover:bg-success-100 transition-colors cursor-pointer">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6 text-success-500 mx-auto mb-2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span class="text-sm font-medium text-success-600">已办事项</span>
            </router-link>
            <router-link to="/form-designer" 
                        class="p-4 bg-gray-100 rounded-lg text-center hover:bg-gray-200 transition-colors cursor-pointer">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6 text-gray-600 mx-auto mb-2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281z" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <span class="text-sm font-medium text-gray-600">表单设计</span>
            </router-link>
          </div>
        </div>

        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">审批类型分布</h3>
          <div class="space-y-3">
            <div v-for="type in approvalTypes" :key="type.name" class="flex items-center gap-3">
              <div class="w-3 h-3 rounded-full" :style="{ backgroundColor: type.color }"></div>
              <span class="flex-1 text-sm text-gray-600">{{ type.label }}</span>
              <span class="text-sm font-medium text-gray-900">{{ type.count }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useApprovalStore } from '@/stores/approval'

const router = useRouter()
const approvalStore = useApprovalStore()

// 页面加载时获取待办列表
onMounted(() => {
  approvalStore.fetchTodoList()
})

const stats = computed(() => ({
  pending: approvalStore.pendingApprovals.length,
  approved: approvalStore.approvedApprovals.length,
  rejected: approvalStore.rejectedApprovals.length,
  total: approvalStore.approvals.length
}))

const pendingList = computed(() => approvalStore.pendingApprovals.slice(0, 5))

const approvalTypes = computed(() => {
  const types = {}
  approvalStore.approvals.forEach(a => {
    types[a.type] = (types[a.type] || 0) + 1
  })
  
  const typeLabels = {
    leave: { label: '请假申请', color: '#3b82f6' },
    expense: { label: '报销申请', color: '#22c55e' },
    purchase: { label: '采购申请', color: '#f59e0b' },
    overtime: { label: '加班申请', color: '#8b5cf6' },
    travel: { label: '出差申请', color: '#ec4899' }
  }
  
  return Object.entries(types).map(([key, count]) => ({
    name: key,
    label: typeLabels[key]?.label || key,
    color: typeLabels[key]?.color || '#6b7280',
    count
  }))
})

function getPriorityLabel(priority) {
  const labels = {
    high: '紧急',
    normal: '普通',
    low: '低'
  }
  return labels[priority] || priority
}

function goToDetail(id) {
  router.push(`/approval/detail/${id}`)
}
</script>
