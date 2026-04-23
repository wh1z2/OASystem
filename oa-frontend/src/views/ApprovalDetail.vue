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

          <!-- 动态表单数据 -->
          <div v-if="hasFormData" class="border-t border-gray-200 pt-6 mt-6">
            <h4 class="font-medium text-gray-900 mb-3">申请详情</h4>
            <DynamicForm :fields="templateFields" v-model="parsedFormData" :readonly="true" />
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
          <div v-if="approval.status === 'processing' && canExecuteApproval" class="space-y-4">
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
            <button v-if="canRevoke" @click="handleRevoke" class="btn btn-secondary w-full">
              撤销申请
            </button>
          </div>
          <div v-else-if="approval.status === 'processing' && !canExecuteApproval && canRevoke" class="space-y-4">
            <div class="text-center py-4">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-12 h-12 text-gray-300 mx-auto mb-2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9.75 9.75l4.5 4.5m0-4.5l-4.5 4.5M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p class="text-gray-500 mb-4">您可撤销该申请</p>
              <button @click="handleRevoke" class="btn btn-danger w-full">
                撤销申请
              </button>
            </div>
          </div>
          <div v-else-if="approval.status === 'processing' && !canExecuteApproval" class="text-center py-4">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-12 h-12 text-gray-300 mx-auto mb-2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
            </svg>
            <p class="text-gray-500">您没有该工单的审批权限</p>
          </div>
          <div v-else-if="approval.status === 'draft' && canSubmit" class="space-y-4">
            <div class="text-center py-4">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-12 h-12 text-gray-300 mx-auto mb-2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
              </svg>
              <p class="text-gray-500 mb-4">草稿待提交</p>
              <button @click="handleSubmit" class="btn btn-primary w-full">
                提交审批
              </button>
              <button @click="handleEdit" class="btn btn-secondary w-full mt-2">
                编辑内容
              </button>
            </div>
          </div>
          <div v-else-if="approval.status === 'draft' && !canSubmit" class="text-center py-4">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-12 h-12 text-gray-300 mx-auto mb-2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
              </svg>
            <p class="text-gray-500">草稿待提交</p>
          </div>
          <div v-else-if="(approval.status === 'approved' || approval.status === 'returned') && canReedit" class="text-center py-4">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-12 h-12 text-gray-300 mx-auto mb-2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
            </svg>
            <p class="text-gray-500 mb-4">{{ approval.status === 'approved' ? '该审批已通过' : '该审批已被打回' }}</p>
            <button @click="handleReedit" class="btn btn-secondary w-full">
              重新编辑
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
                {{
                  approval.status === 'processing' ? '待审批' :
                    approval.status === 'draft' ? '待提交' : '已处理'
                }}
              </p>
            </div>
          </div>
          <p v-else-if="approval.status === 'draft'" class="text-gray-500 text-sm">
            未指定审批人
          </p>
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
              <span class="text-gray-900">{{ approvalHistory.length > 0 ? approvalHistory[approvalHistory.length - 1]?.time : '-' }}</span>
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
    <ConfirmDialog
      :visible="showRevokeConfirm"
      title="撤销申请"
      message="确定要撤销该申请吗？撤销后该工单将变为草稿状态。"
      @confirm="handleRevokeConfirm"
      @cancel="showRevokeConfirm = false"
    />

    <!-- 轻量提示弹窗 -->
    <ConfirmDialog
      :visible="showAlert"
      :title="alertTitle"
      :message="alertMessage"
      :show-cancel="false"
      @confirm="handleAlertConfirm"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useApprovalStore } from '@/stores/approval'
import { useAuthStore } from '@/stores/auth'
import { hasApprovalExecutePermission } from '@/utils/permission'
import { formTemplateApi } from '@/api/formTemplate'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import DynamicForm from '@/components/DynamicForm.vue'

const route = useRoute()
const router = useRouter()
const approvalStore = useApprovalStore()
const authStore = useAuthStore()

const comment = ref('')
const loading = ref(false)
const showRevokeConfirm = ref(false)
const templateFields = ref([])
const templates = ref([])

async function loadTemplates() {
  try {
    const data = await formTemplateApi.getAll()
    templates.value = data || []
  } catch (error) {
    console.error('加载表单模板列表失败:', error)
  }
}

function getTypeLabel(typeCode) {
  const template = templates.value.find(t => t.code === typeCode)
  return template?.name || typeCode
}

// 轻量提示弹窗状态
const showAlert = ref(false)
const alertTitle = ref('提示')
const alertMessage = ref('')

function showAlertDialog(title, message) {
  alertTitle.value = title
  alertMessage.value = message
  showAlert.value = true
}

function handleAlertConfirm() {
  showAlert.value = false
}

const approval = computed(() => approvalStore.currentApproval)
const approvalHistory = computed(() => approvalStore.approvalHistory)

const parsedFormData = computed(() => {
  const fd = approval.value?.formData
  if (!fd) return {}
  if (typeof fd === 'string') {
    try {
      return JSON.parse(fd)
    } catch (e) {
      return {}
    }
  }
  return fd
})

const hasFormData = computed(() => {
  const fd = parsedFormData.value
  return Object.keys(fd).length > 0
})
const canExecuteApproval = computed(() => hasApprovalExecutePermission(authStore.permissions))
const canSubmit = computed(() => authStore.checkPermission('apply'))
const canRevoke = computed(() =>
  approval.value?.status === 'processing' &&
  approval.value?.applicantId === authStore.currentUser?.id
)
const canReedit = computed(() =>
  (approval.value?.status === 'approved' || approval.value?.status === 'returned') &&
  approval.value?.applicantId === authStore.currentUser?.id
)

async function loadFormTemplate() {
  const code = approval.value?.type
  if (!code) {
    templateFields.value = []
    return
  }
  try {
    const template = await formTemplateApi.getByCode(code)
    if (template) {
      let fields = []
      if (template.fieldsConfig) {
        try {
          fields = typeof template.fieldsConfig === 'string'
            ? JSON.parse(template.fieldsConfig)
            : template.fieldsConfig
        } catch (e) {
          console.error('解析表单模板字段失败:', e)
        }
      }
      templateFields.value = fields
    } else {
      templateFields.value = []
    }
  } catch (error) {
    console.error('加载表单模板失败:', error)
    templateFields.value = []
  }
}

// 页面加载时获取详情和历史
onMounted(async () => {
  await loadTemplates()
  const id = route.params.id
  loading.value = true
  await approvalStore.fetchApprovalById(id)
  await approvalStore.fetchApprovalHistory(id)
  await loadFormTemplate()
  loading.value = false
})

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

function handleRevoke() {
  if (!approval.value) return
  showRevokeConfirm.value = true
}

async function handleRevokeConfirm() {
  showRevokeConfirm.value = false
  if (!approval.value) return

  const result = await approvalStore.revokeApproval(approval.value.id)

  if (result.success) {
    router.push('/approval')
  } else {
    showAlertDialog('撤销失败', '撤销失败：' + result.message)
  }
}

async function handleSubmit() {
  if (!approval.value) return

  const result = await approvalStore.submitApproval(approval.value.id)

  if (result.success) {
    // 提交成功后刷新页面数据
    await approvalStore.fetchApprovalById(approval.value.id)
    await approvalStore.fetchApprovalHistory(approval.value.id)
  } else {
    showAlertDialog('提交失败', '提交失败：' + result.message)
  }
}

function handleEdit() {
  if (!approval.value) return
  router.push(`/approval/edit/${approval.value.id}`)
}

async function handleReedit() {
  if (!approval.value) return

  const result = await approvalStore.reeditApproval(approval.value.id)

  if (result.success) {
    // 重新编辑成功后刷新页面数据
    await approvalStore.fetchApprovalById(approval.value.id)
    await approvalStore.fetchApprovalHistory(approval.value.id)
  } else {
    showAlertDialog('重新编辑失败', '重新编辑失败：' + result.message)
  }
}
</script>
