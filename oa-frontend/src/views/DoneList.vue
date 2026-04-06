<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-gray-900">已办事项</h2>
      <div class="flex items-center gap-4">
        <select v-model="filterStatus" class="input w-32">
          <option value="">全部状态</option>
          <option value="approved">已通过</option>
          <option value="rejected">已拒绝</option>
        </select>
      </div>
    </div>

    <div class="space-y-4">
      <div v-for="item in filteredApprovals" :key="item.id" 
           class="card hover:shadow-md transition-shadow cursor-pointer"
           @click="goToDetail(item.id)">
        <div class="flex items-start gap-4">
          <div :class="[
            'w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0',
            item.status === 'approved' ? 'bg-success-50' : 'bg-danger-50'
          ]">
            <svg v-if="item.status === 'approved'" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6 text-success-500">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6 text-danger-500">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
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
                item.status === 'approved' ? 'badge-success' : 'badge-danger'
              ]">
                {{ getStatusLabel(item.status) }}
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
                <span>{{ item.history?.length > 0 ? item.history[item.history.length - 1]?.time : item.createTime }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="filteredApprovals.length === 0" class="card text-center py-12">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-16 h-16 text-gray-300 mx-auto mb-4">
          <path stroke-linecap="round" stroke-linejoin="round" d="M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5m6 4.125l2.25 2.25m0 0l2.25 2.25M12 13.875l2.25-2.25M12 13.875l-2.25 2.25M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z" />
        </svg>
        <p class="text-gray-500 text-lg">暂无已办事项</p>
        <p class="text-gray-400 text-sm mt-1">处理过的审批将显示在这里</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useApprovalStore } from '@/stores/approval'

const router = useRouter()
const approvalStore = useApprovalStore()

const filterStatus = ref('')

const filteredApprovals = computed(() => {
  // 已办列表从后端获取
  if (!filterStatus.value) return approvalStore.approvals
  return approvalStore.approvals.filter(item => item.status === filterStatus.value)
})

// 页面加载时获取已办列表
onMounted(() => {
  approvalStore.fetchDoneList()
})

function getStatusLabel(status) {
  const labels = {
    approved: '已通过',
    returned: '已打回',
    draft: '草稿',
    processing: '审批中',
    revoked: '已撤销'
  }
  return labels[status] || status
}

function goToDetail(id) {
  router.push(`/approval/detail/${id}`)
}
</script>
