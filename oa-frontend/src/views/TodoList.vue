<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-gray-900">待办事项</h2>
      <div class="flex items-center gap-2 text-sm text-gray-500">
        <span>共 {{ pendingApprovals.length }} 项待处理</span>
      </div>
    </div>

    <div class="space-y-4">
      <div v-for="item in pendingApprovals" :key="item.id" 
           class="card hover:shadow-md transition-shadow cursor-pointer"
           @click="goToDetail(item.id)">
        <div class="flex items-start gap-4">
          <div :class="[
            'w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0',
            item.priority === 'high' ? 'bg-danger-50' : item.priority === 'normal' ? 'bg-primary-50' : 'bg-gray-100'
          ]">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6"
                 :class="item.priority === 'high' ? 'text-danger-500' : item.priority === 'normal' ? 'text-primary-600' : 'text-gray-500'">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
            </svg>
          </div>
          
          <div class="flex-1 min-w-0">
            <div class="flex items-start justify-between gap-4">
              <div>
                <h3 class="font-semibold text-gray-900">{{ item.title }}</h3>
                <p class="text-sm text-gray-500 mt-1">{{ item.content.substring(0, 80) }}...</p>
              </div>
              <span :class="[
                'badge flex-shrink-0',
                item.priority === 'high' ? 'badge-danger' : item.priority === 'normal' ? 'badge-primary' : 'bg-gray-100 text-gray-600'
              ]">
                {{ getPriorityLabel(item.priority) }}
              </span>
            </div>
            
            <div class="flex items-center gap-6 mt-4 text-sm">
              <div class="flex items-center gap-2 text-gray-500">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
                </svg>
                <span>{{ item.applicant }}</span>
              </div>
              <div class="flex items-center gap-2 text-gray-500">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 21h19.5m-18-18v18m10.5-18v18m6-13.5V21M6.75 6.75h.75m-.75 3h.75m-.75 3h.75m3-6h.75m-.75 3h.75m-.75 3h.75M6.75 21v-3.375c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21M3 3h12m-.75 4.5H21m-3.75 3.75h.008v.008h-.008v-.008zm0 3h.008v.008h-.008v-.008zm0 3h.008v.008h-.008v-.008z" />
                </svg>
                <span>{{ item.department }}</span>
              </div>
              <div class="flex items-center gap-2 text-gray-500">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>{{ item.createTime }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="flex items-center justify-end gap-3 mt-4 pt-4 border-t border-gray-100">
          <button @click.stop="quickReject(item.id)" class="btn btn-secondary text-sm">
            拒绝
          </button>
          <button @click.stop="quickApprove(item.id)" class="btn btn-primary text-sm">
            快速审批
          </button>
        </div>
      </div>
      
      <div v-if="pendingApprovals.length === 0" class="card text-center py-12">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-16 h-16 text-gray-300 mx-auto mb-4">
          <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <p class="text-gray-500 text-lg">暂无待办事项</p>
        <p class="text-gray-400 text-sm mt-1">所有审批已处理完成</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useApprovalStore } from '@/stores/approval'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const approvalStore = useApprovalStore()
const authStore = useAuthStore()

const pendingApprovals = computed(() => approvalStore.pendingApprovals)

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

function quickApprove(id) {
  approvalStore.approveApproval(id, '快速审批通过', authStore.currentUser?.name)
}

function quickReject(id) {
  approvalStore.rejectApproval(id, '快速审批拒绝', authStore.currentUser?.name)
}
</script>
