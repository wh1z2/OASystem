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
        <span>共 {{ rejectedList.length }} 项已拒绝</span>
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

      <div v-for="item in rejectedList" :key="item.id"
           class="card hover:shadow-md transition-shadow cursor-pointer"
           @click="goToDetail(item.id)">
        <div class="flex items-start gap-4">
          <div class="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 bg-danger-50">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6 text-danger-500">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>

          <div class="flex-1 min-w-0">
            <div class="flex items-start justify-between gap-4">
              <div>
                <h3 class="font-semibold text-gray-900">{{ item.title }}</h3>
                <p class="text-sm text-gray-500 mt-1">{{ item.content.substring(0, 80) }}...</p>
              </div>
              <span class="badge badge-danger flex-shrink-0">已拒绝</span>
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
      </div>

      <div v-if="!loading && !error && rejectedList.length === 0" class="card text-center py-12">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-16 h-16 text-gray-300 mx-auto mb-4">
          <path stroke-linecap="round" stroke-linejoin="round" d="M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5m6 4.125l2.25 2.25m0 0l2.25 2.25M12 13.875l2.25-2.25M12 13.875l-2.25 2.25M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z" />
        </svg>
        <p class="text-gray-500 text-lg">暂无已拒绝记录</p>
        <p class="text-gray-400 text-sm mt-1">被拒绝的申请将显示在这里</p>
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

const rejectedList = computed(() => approvalStore.rejectedApprovals)

onMounted(async () => {
  loading.value = true
  error.value = null
  const result = await approvalStore.fetchApprovals({ status: 3 })
  loading.value = false
  if (!result.success) {
    error.value = result.message || '加载失败，请检查权限'
  }
})

function goBack() {
  router.push('/')
}

function goToDetail(id) {
  router.push(`/approval/detail/${id}`)
}
</script>
