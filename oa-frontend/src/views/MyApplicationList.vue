<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <button @click="goBack" class="btn btn-secondary flex items-center gap-2">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
          <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
        </svg>
        返回工作台
      </button>
      <div class="flex items-center gap-2 text-sm text-gray-500">
        <span>共 {{ myList.length }} 项申请</span>
      </div>
    </div>

    <div class="space-y-4">
      <div v-if="loading" class="card text-center py-12">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-10 h-10 text-gray-300 mx-auto mb-4 animate-spin">
          <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182" />
        </svg>
        <p class="text-gray-500">加载中...</p>
      </div>

      <div v-else-if="error" class="card text-center py-12">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-16 h-16 text-danger-300 mx-auto mb-4">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
        </svg>
        <p class="text-danger-500 text-lg">加载失败</p>
        <p class="text-gray-400 text-sm mt-1">{{ error }}</p>
      </div>

      <div v-for="item in myList" :key="item.id"
           class="card hover:shadow-md transition-shadow cursor-pointer"
           @click="goToDetail(item.id)">
        <div class="flex items-start gap-4">
          <div :class="[
            'w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0',
            item.status === 'approved' ? 'bg-success-50' :
              item.status === 'returned' ? 'bg-danger-50' :
                item.status === 'processing' ? 'bg-primary-50' :
                  item.status === 'revoked' ? 'bg-gray-100' : 'bg-gray-100'
          ]">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6"
                 :class="item.status === 'approved' ? 'text-success-500' :
                   item.status === 'returned' ? 'text-danger-500' :
                     item.status === 'processing' ? 'text-primary-600' : 'text-gray-500'">
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
                item.status === 'approved' ? 'badge-success' :
                  item.status === 'returned' ? 'badge-danger' :
                    item.status === 'processing' ? 'badge-primary' :
                      item.status === 'revoked' ? 'bg-gray-100 text-gray-600' : 'bg-gray-100 text-gray-600'
              ]">
                {{ getStatusLabel(item.status) }}
              </span>
            </div>

            <div class="flex items-center gap-6 mt-4 text-sm">
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
              <div v-if="item.currentApprover" class="flex items-center gap-2 text-gray-500">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
                </svg>
                <span>当前审批人：{{ item.currentApprover }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!loading && !error && myList.length === 0" class="card text-center py-12">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-16 h-16 text-gray-300 mx-auto mb-4">
          <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
        </svg>
        <p class="text-gray-500 text-lg">暂无我的申请</p>
        <p class="text-gray-400 text-sm mt-1">您发起的申请将显示在这里</p>
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

const loading = ref(false)
const error = ref(null)

const myList = computed(() => approvalStore.myApprovals)

onMounted(async () => {
  loading.value = true
  error.value = null
  const result = await approvalStore.fetchMyApprovals()
  loading.value = false
  if (!result.success) {
    error.value = result.message || '加载失败，请检查权限'
  }
})

function getStatusLabel(status) {
  const labels = {
    draft: '草稿',
    processing: '审批中',
    approved: '已通过',
    returned: '已拒绝',
    revoked: '已撤销'
  }
  return labels[status] || status
}

function goBack() {
  router.push('/')
}

function goToDetail(id) {
  router.push(`/approval/detail/${id}`)
}
</script>
