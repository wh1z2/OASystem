<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="relative">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
          </svg>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索审批标题..."
            class="input pl-10 w-64"
          />
        </div>
        <select v-model="filterType" class="input w-40">
          <option value="">全部类型</option>
          <option value="leave">请假申请</option>
          <option value="expense">报销申请</option>
          <option value="purchase">采购申请</option>
          <option value="overtime">加班申请</option>
          <option value="travel">出差申请</option>
        </select>
        <select v-model="filterPriority" class="input w-32">
          <option value="">全部优先级</option>
          <option value="high">紧急</option>
          <option value="normal">普通</option>
          <option value="low">低</option>
        </select>
      </div>
      <router-link v-if="canCreateApproval" to="/approval/create" class="btn btn-primary flex items-center gap-2">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
        </svg>
        发起审批
      </router-link>
    </div>

    <div class="card overflow-hidden">
      <table class="w-full">
        <thead class="bg-gray-50 border-b border-gray-200">
          <tr>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">审批标题</th>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">类型</th>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">申请人</th>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">部门</th>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">优先级</th>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">状态</th>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">提交时间</th>
            <th class="text-left px-6 py-4 text-sm font-medium text-gray-500">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200">
          <tr v-for="item in filteredApprovals" :key="item.id" class="hover:bg-gray-50 transition-colors">
            <td class="px-6 py-4">
              <span class="font-medium text-gray-900">{{ item.title }}</span>
            </td>
            <td class="px-6 py-4">
              <span class="text-gray-600">{{ getTypeLabel(item.type) }}</span>
            </td>
            <td class="px-6 py-4">
              <span class="text-gray-600">{{ item.applicant }}</span>
            </td>
            <td class="px-6 py-4">
              <span class="text-gray-600">{{ item.department }}</span>
            </td>
            <td class="px-6 py-4">
              <span :class="[
                'badge',
                item.priority === 'high' ? 'badge-danger' : item.priority === 'normal' ? 'badge-primary' : 'bg-gray-100 text-gray-600'
              ]">
                {{ getPriorityLabel(item.priority) }}
              </span>
            </td>
            <td class="px-6 py-4">
              <span :class="[
                'badge',
                item.status === 'processing' ? 'badge-warning' :
                  item.status === 'approved' ? 'badge-success' :
                    item.status === 'draft' ? 'bg-gray-100 text-gray-600' :
                      item.status === 'returned' ? 'badge-danger' : 'bg-gray-100 text-gray-600'
              ]">
                {{ getStatusLabel(item.status) }}
              </span>
            </td>
            <td class="px-6 py-4">
              <span class="text-gray-500 text-sm">{{ item.createTime }}</span>
            </td>
            <td class="px-6 py-4">
              <div class="flex items-center gap-2">
                <button @click="viewDetail(item.id)" class="text-primary-600 hover:text-primary-700 text-sm font-medium cursor-pointer">
                  查看
                </button>
                <button v-if="item.status === 'draft' && canEdit(item)" @click="handleEdit(item.id)" class="text-primary-600 hover:text-primary-700 text-sm font-medium cursor-pointer">
                  编辑
                </button>
                <button v-if="item.status === 'processing' && canExecuteApproval" @click="showApproveModal(item)" class="text-success-600 hover:text-success-700 text-sm font-medium cursor-pointer">
                  审批
                </button>
                <button v-if="(item.status === 'approved' || item.status === 'returned') && canReedit(item)" @click="handleReedit(item)" class="text-warning-600 hover:text-warning-700 text-sm font-medium cursor-pointer">
                  重新编辑
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredApprovals.length === 0">
            <td colspan="8" class="px-6 py-12 text-center text-gray-500">
              暂无审批数据
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="showModal = false">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">审批操作</h3>
        <p class="text-gray-600 mb-4">{{ currentItem?.title }}</p>
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-1">审批意见</label>
          <textarea v-model="approveComment" class="input h-24 resize-none" placeholder="请输入审批意见..."></textarea>
        </div>
        <div class="flex gap-3">
          <button @click="handleApprove('approved')" class="btn btn-success flex-1">
            通过
          </button>
          <button @click="handleApprove('rejected')" class="btn btn-danger flex-1">
            拒绝
          </button>
          <button @click="showModal = false" class="btn btn-secondary">
            取消
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useApprovalStore } from '@/stores/approval'
import { useAuthStore } from '@/stores/auth'
import { hasPermission, hasAnyPermission, hasApprovalPermission, hasApprovalExecutePermission } from '@/utils/permission'

const router = useRouter()
const approvalStore = useApprovalStore()
const authStore = useAuthStore()

const canCreateApproval = computed(() => authStore.checkPermission('apply'))
const canExecuteApproval = computed(() => hasApprovalExecutePermission(authStore.permissions))

const searchQuery = ref('')
const filterType = ref('')
const filterPriority = ref('')
const showModal = ref(false)
const currentItem = ref(null)
const approveComment = ref('')

const filteredApprovals = computed(() => {
  return approvalStore.approvals.filter(item => {
    const matchSearch = item.title.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchType = !filterType.value || item.type === filterType.value
    const matchPriority = !filterPriority.value || item.priority === filterPriority.value
    return matchSearch && matchType && matchPriority
  })
})

function getTypeLabel(type) {
  const labels = {
    leave: '请假申请',
    expense: '报销申请',
    purchase: '采购申请',
    overtime: '加班申请',
    travel: '出差申请'
  }
  return labels[type] || type
}

function getPriorityLabel(priority) {
  const labels = {
    high: '紧急',
    normal: '普通',
    low: '低'
  }
  return labels[priority] || priority
}

function getStatusLabel(status) {
  const labels = {
    draft: '草稿',
    processing: '审批中',
    approved: '已通过',
    returned: '已打回',
    revoked: '已撤销'
  }
  return labels[status] || status
}

function viewDetail(id) {
  router.push(`/approval/detail/${id}`)
}

function handleEdit(id) {
  router.push(`/approval/edit/${id}`)
}

function canEdit(item) {
  return item.applicantId === authStore.currentUser?.id || authStore.checkPermission('all')
}

function canReedit(item) {
  return item.applicantId === authStore.currentUser?.id || authStore.checkPermission('all')
}

async function handleReedit(item) {
  const result = await approvalStore.reeditApproval(item.id)
  if (result.success) {
    await approvalStore.fetchApprovals()
  } else {
    alert('重新编辑失败：' + result.message)
  }
}

function showApproveModal(item) {
  currentItem.value = item
  approveComment.value = ''
  showModal.value = true
}

// 页面加载时获取数据
onMounted(() => {
  approvalStore.fetchApprovals()
})

async function handleApprove(action) {
  if (!currentItem.value) return

  let result
  if (action === 'approved') {
    result = await approvalStore.approveApproval(currentItem.value.id, approveComment.value)
  } else {
    result = await approvalStore.rejectApproval(currentItem.value.id, approveComment.value)
  }

  if (result.success) {
    // 刷新列表
    await approvalStore.fetchApprovals()
  }

  showModal.value = false
  currentItem.value = null
  approveComment.value = ''
}
</script>
