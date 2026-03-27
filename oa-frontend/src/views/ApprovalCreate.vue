<template>
  <div class="space-y-6">
    <div class="flex items-center gap-4">
      <router-link to="/approval" class="text-gray-400 hover:text-gray-600 cursor-pointer">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
          <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
        </svg>
      </router-link>
      <h2 class="text-xl font-semibold text-gray-900">发起审批</h2>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2">
        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-6">填写申请信息</h3>
          
          <form @submit.prevent="handleSubmit" class="space-y-6">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">审批类型 <span class="text-danger-500">*</span></label>
              <select v-model="form.type" class="input" required>
                <option value="">请选择审批类型</option>
                <option value="leave">请假申请</option>
                <option value="expense">报销申请</option>
                <option value="purchase">采购申请</option>
                <option value="overtime">加班申请</option>
                <option value="travel">出差申请</option>
              </select>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">审批标题 <span class="text-danger-500">*</span></label>
              <input v-model="form.title" type="text" class="input" placeholder="请输入审批标题" required />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">优先级</label>
              <div class="flex gap-4">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input type="radio" v-model="form.priority" value="low" class="text-primary-600" />
                  <span class="text-sm text-gray-600">低</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input type="radio" v-model="form.priority" value="normal" class="text-primary-600" />
                  <span class="text-sm text-gray-600">普通</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input type="radio" v-model="form.priority" value="high" class="text-primary-600" />
                  <span class="text-sm text-gray-600">紧急</span>
                </label>
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">申请内容 <span class="text-danger-500">*</span></label>
              <textarea v-model="form.content" class="input h-32 resize-none" placeholder="请详细描述申请内容..." required></textarea>
            </div>

            <div v-if="form.type === 'leave'">
              <label class="block text-sm font-medium text-gray-700 mb-1">请假日期</label>
              <div class="grid grid-cols-2 gap-4">
                <input v-model="form.startDate" type="date" class="input" />
                <input v-model="form.endDate" type="date" class="input" />
              </div>
            </div>

            <div v-if="form.type === 'expense'">
              <label class="block text-sm font-medium text-gray-700 mb-1">报销金额</label>
              <input v-model="form.amount" type="number" class="input" placeholder="请输入金额" />
            </div>

            <div v-if="form.type === 'travel'">
              <label class="block text-sm font-medium text-gray-700 mb-1">出差地点</label>
              <input v-model="form.destination" type="text" class="input" placeholder="请输入出差目的地" />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">附件</label>
              <div class="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:border-primary-400 transition-colors cursor-pointer">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-8 h-8 text-gray-400 mx-auto mb-2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 16.5V9.75m0 0l3 3m-3-3l-3 3M6.75 19.5a4.5 4.5 0 01-1.41-8.775 5.25 5.25 0 0110.233-2.33 3 3 0 013.758 3.848A3.752 3.752 0 0118 19.5H6.75z" />
                </svg>
                <p class="text-sm text-gray-500">点击或拖拽上传附件</p>
                <p class="text-xs text-gray-400 mt-1">支持 PDF、Word、Excel、图片等格式</p>
              </div>
            </div>

            <div class="flex gap-4 pt-4">
              <button type="submit" class="btn btn-primary flex-1">
                提交申请
              </button>
              <button type="button" @click="handleSaveDraft" class="btn btn-secondary flex-1">
                保存草稿
              </button>
              <router-link to="/approval" class="btn btn-secondary">
                取消
              </router-link>
            </div>
          </form>
        </div>
      </div>

      <div class="space-y-6">
        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">审批流程</h3>
          <div class="space-y-4">
            <div class="flex items-start gap-3">
              <div class="w-8 h-8 bg-success-50 rounded-full flex items-center justify-center flex-shrink-0">
                <span class="text-sm font-medium text-success-600">1</span>
              </div>
              <div>
                <p class="font-medium text-gray-900">提交申请</p>
                <p class="text-sm text-gray-500">填写申请表单并提交</p>
              </div>
            </div>
            <div class="w-px h-6 bg-gray-200 ml-4"></div>
            <div class="flex items-start gap-3">
              <div class="w-8 h-8 bg-primary-50 rounded-full flex items-center justify-center flex-shrink-0">
                <span class="text-sm font-medium text-primary-600">2</span>
              </div>
              <div>
                <p class="font-medium text-gray-900">部门审批</p>
                <p class="text-sm text-gray-500">部门经理审核</p>
              </div>
            </div>
            <div class="w-px h-6 bg-gray-200 ml-4"></div>
            <div class="flex items-start gap-3">
              <div class="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center flex-shrink-0">
                <span class="text-sm font-medium text-gray-600">3</span>
              </div>
              <div>
                <p class="font-medium text-gray-900">审批完成</p>
                <p class="text-sm text-gray-500">通知审批结果</p>
              </div>
            </div>
          </div>
        </div>

        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">温馨提示</h3>
          <ul class="space-y-2 text-sm text-gray-600">
            <li class="flex items-start gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4 text-primary-600 mt-0.5 flex-shrink-0">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              请如实填写申请内容
            </li>
            <li class="flex items-start gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4 text-primary-600 mt-0.5 flex-shrink-0">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              紧急事项请选择紧急优先级
            </li>
            <li class="flex items-start gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4 text-primary-600 mt-0.5 flex-shrink-0">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              相关证明材料请作为附件上传
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useApprovalStore } from '@/stores/approval'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const approvalStore = useApprovalStore()
const authStore = useAuthStore()

const form = ref({
  type: '',
  title: '',
  priority: 'normal',
  content: '',
  startDate: '',
  endDate: '',
  amount: '',
  destination: ''
})

function handleSubmit() {
  const approval = approvalStore.addApproval({
    ...form.value,
    applicant: authStore.currentUser?.name || '未知用户',
    applicantId: authStore.currentUser?.id,
    department: authStore.currentUser?.department || '未知部门',
    currentApprover: '张经理'
  })
  
  router.push(`/approval/detail/${approval.id}`)
}

function handleSaveDraft() {
  alert('草稿已保存')
}
</script>
