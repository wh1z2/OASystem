<template>
  <div class="space-y-6">
    <div class="flex items-center gap-4">
      <router-link to="/approval" class="text-gray-400 hover:text-gray-600 cursor-pointer">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
          <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
        </svg>
      </router-link>
      <h2 class="text-xl font-semibold text-gray-900">审批详情</h2>
    </div>

    <div v-if="approval" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2 space-y-6">
        <div class="card">
          <div class="flex items-start justify-between mb-6">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ approval.title }}</h3>
              <p class="text-sm text-gray-500 mt-1">{{ approval.createTime }}</p>
            </div>
            <span :class="[
              'badge',
              approval.status === 'processing' ? 'badge-warning' :
                approval.status === 'approved' ? 'badge-success' :
                  approval.status === 'returned' ? 'badge-danger' :
                    approval.status === 'draft' ? 'bg-gray-100 text-gray-600' : 'bg-gray-100 text-gray-600'
            ]">
              {{ getStatusLabel(approval.status) }}
            </span>
          </div>

          <div class="grid grid-cols-2 gap-4 mb-6">
            <div>
              <p class="text-sm text-gray-500">申请人</p>
              <p class="font-medium text-gray-900">{{ approval.applicant }}</p>
            </div>
            <div>
              <p class="text-sm text-gray-500">所属部门</p>
              <p class="font-medium text-gray-900">{{ approval.department }}</p>
            </div>
            <div>
              <p class="text-sm text-gray-500">审批类型</p>
              <p class="font-medium text-gray-900">{{ getTypeLabel(approval.type) }}</p>
            </div>
            <div>
              <p class="text-sm text-gray-500">优先级</p>
              <span :class="[
                'badge',
                approval.priority === 'high' ? 'badge-danger' : approval.priority === 'normal' ? 'badge-primary' : 'bg-gray-100 text-gray-600'
              ]">
                {{ getPriorityLabel(approval.priority) }}
              </span>
            </div>
          </div>

          <div class="border-t border-gray-200 pt-6">
            <h4 class="font-medium text-gray-900 mb-3">申请内容</h4>
            <p class="text-gray-600 leading-relaxed">{{ approval.content }}</p>
          </div>
        </div>

        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-6">审批历史</h3>
          <div class="space-y-4">
            <div v-for="(record, index) in approvalHistory" :key="index"
                 class="flex items-start gap-4">
              <div class="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center flex-shrink-0">
                <span class="text-sm font-medium text-primary-600">{{ record.approver.charAt(0) }}</span>
              </div>
              <div class="flex-1">
                <div class="flex items-center gap-2">
                  <span class="font-medium text-gray-900">{{ record.approver }}</span>
                  <span :class="[
                    'badge text-xs',
                    record.actionName === '提交' ? 'badge-primary' :
                      record.actionName === '审批同意' ? 'badge-success' : 'badge-danger'
                  ]">
                    {{ record.actionName || getActionLabel(record.action) }}
                  </span>
                </div>
                <p class="text-sm text-gray-500 mt-1">{{ record.comment }}</p>
                <p class="text-xs text-gray-400 mt-1">{{ record.time }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="space-y-6">
        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">审批操作</h3>
          <div v-if="approval.status === 'processing'" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">审批意见</label>
              <textarea v-model="comment" class="input h-24 resize-none" placeholder="请输入审批意见..."></textarea>
            </div>
            <button @click="handleApprove('approved')" class="btn btn-success w-full">
              通过审批
            </button>
            <button @click="handleApprove('rejected')" class="btn btn-danger w-full">
              拒绝审批
            </button>
          </div>
          <div v-else class="text-center py-4">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-12 h-12 text-gray-300 mx-auto mb-2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z" />
            </svg>
            <p class="text-gray-500">该审批已处理完成</p>
          </div>
        </div>

        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">
            {{ approval.status === 'processing' ? '当前审批人' : '审批人' }}
          </h3>
          <div v-if="approval.currentApprover" class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center">
              <span class="text-sm font-medium text-primary-600">{{ approval.currentApprover.charAt(0) }}</span>
            </div>
            <div>
              <p class="font-medium text-gray-900">{{ approval.currentApprover }}</p>
              <p class="text-sm text-gray-500">
                {{ approval.status === 'processing' ? '待审批' : '已处理' }}
              </p>
            </div>
          </div>
          <p v-else class="text-gray-500 text-sm">
            {{ approval.status === 'processing' ? '无待审批人' : '审批流程已结束' }}
          </p>
        </div>

        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">操作记录</h3>
          <div class="space-y-3 text-sm">
            <div class="flex justify-between">
              <span class="text-gray-500">创建时间</span>
              <span class="text-gray-900">{{ approval.createTime }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-500">最后更新</span>
              <span class="text-gray-900">{{ approval.history?.length > 0 ? approval.history[approval.history.length - 1]?.time : '-' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="card text-center py-12">
      <p class="text-gray-500">审批不存在</p>
      <router-link to="/approval" class="text-primary-600 hover:text-primary-700 mt-2 inline-block">
        返回列表
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useApprovalStore } from '@/stores/approval'

const route = useRoute()
const router = useRouter()
const approvalStore = useApprovalStore()

const comment = ref('')
const loading = ref(false)

const approval = computed(() => approvalStore.currentApproval)
const approvalHistory = computed(() => approvalStore.approvalHistory)

// 页面加载时获取详情和历史
onMounted(async () => {
  const id = route.params.id
  loading.value = true
  await approvalStore.fetchApprovalById(id)
  await approvalStore.fetchApprovalHistory(id)
  loading.value = false
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

function getActionLabel(action) {
  const labels = {
    SUBMIT: '提交',
    APPROVE: '通过',
    REJECT: '拒绝',
    REEDIT: '重新编辑',
    REVOKE: '撤销'
  }
  return labels[action] || action
}

async function handleApprove(action) {
  if (!approval.value) return

  let result
  if (action === 'approved') {
    result = await approvalStore.approveApproval(approval.value.id, comment.value)
  } else {
    result = await approvalStore.rejectApproval(approval.value.id, comment.value)
  }

  if (result.success) {
    router.push('/approval')
  }
}
</script>
