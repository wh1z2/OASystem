<template>
  <div class="space-y-6">
    <div class="flex items-center gap-4">
      <router-link to="/approval" class="text-gray-400 hover:text-gray-600 cursor-pointer">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
          <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
        </svg>
      </router-link>
      <h2 class="text-xl font-semibold text-gray-900">{{ isEditMode ? '编辑审批' : '发起审批' }}</h2>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2">
        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-6">填写申请信息</h3>
          
          <form @submit.prevent="handleSubmit" class="space-y-6">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">审批类型 <span class="text-danger-500">*</span></label>
              <select v-if="!isEditMode" v-model="form.type" class="input" required>
                <option value="">请选择审批类型</option>
                <option v-for="template in templates" :key="template.id" :value="template.code">
                  {{ template.name }}
                </option>
              </select>
              <p v-else class="input bg-gray-50 text-gray-600">{{ getTypeLabel(form.type) }}</p>
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

            <div v-if="templateLoading" class="py-4 text-center text-gray-500">
              加载表单模板...
            </div>
            <div v-else-if="templateFields.length > 0">
              <label class="block text-sm font-medium text-gray-700 mb-3">申请详情</label>
              <DynamicForm :fields="templateFields" v-model="dynamicFormData" />
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
                {{ isEditMode ? '保存修改' : '提交申请' }}
              </button>
              <button v-if="!isEditMode" type="button" @click="handleSaveDraft" class="btn btn-secondary flex-1">
                保存草稿
              </button>
              <router-link to="/approval" class="btn btn-secondary">
                取消
              </router-link>
            </div>
          </form>
        </div>
      </div>

      <!-- 轻量提示弹窗 -->
      <ConfirmDialog
        :visible="showAlert"
        :title="alertTitle"
        :message="alertMessage"
        :show-cancel="false"
        @confirm="handleAlertConfirm"
      />

      <div class="space-y-6">
        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">审批流程</h3>
          <div class="bg-primary-50 rounded-lg p-3 mb-4">
            <p class="text-sm text-primary-700 flex items-start gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5 flex-shrink-0 mt-0.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
              </svg>
              审批人将由系统根据预设规则自动分配，您无需手动选择。
            </p>
          </div>
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
                <p class="text-sm text-gray-500">
                  <span v-if="resolverLoading">正在解析审批人...</span>
                  <span v-else-if="!form.type">请选择审批类型后查看</span>
                  <span v-else-if="resolverResult?.success">由 {{ previewApproverName }} 审核</span>
                  <span v-else-if="resolverResult">{{ resolverResult.message }}</span>
                  <span v-else>部门经理审核</span>
                </p>
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useApprovalStore } from '@/stores/approval'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import { approverRuleApi } from '@/api/approverRule'
import { formTemplateApi } from '@/api/formTemplate'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import DynamicForm from '@/components/DynamicForm.vue'

const router = useRouter()
const route = useRoute()
const approvalStore = useApprovalStore()
const authStore = useAuthStore()
const userStore = useUserStore()

const resolverResult = ref(null)
const resolverLoading = ref(false)
const templateFields = ref([])
const templateLoading = ref(false)
const dynamicFormData = ref({})
const templates = ref([])

// 轻量提示弹窗状态
const showAlert = ref(false)
const alertTitle = ref('提示')
const alertMessage = ref('')
const alertOnConfirm = ref(null)

function showAlertDialog(title, message, onConfirm = null) {
  alertTitle.value = title
  alertMessage.value = message
  alertOnConfirm.value = onConfirm
  showAlert.value = true
}

function handleAlertConfirm() {
  showAlert.value = false
  if (alertOnConfirm.value) {
    alertOnConfirm.value()
    alertOnConfirm.value = null
  }
}

const previewApproverName = computed(() => {
  if (!resolverResult.value?.success) return ''
  const approver = userStore.getUserById(resolverResult.value.approverId)
  return approver?.name || `用户#${resolverResult.value.approverId}`
})

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

const isEditMode = computed(() => !!route.params.id)
const approvalId = computed(() => route.params.id)

// 优先级映射 (前端字符串 -> 后端数值)
const priorityMap = {
  'low': 0,
  'normal': 1,
  'high': 2
}

// 优先级反向映射 (后端数值 -> 前端字符串)
const reversePriorityMap = {
  0: 'low',
  1: 'normal',
  2: 'high'
}

async function loadTemplates() {
  try {
    const data = await formTemplateApi.getAll()
    templates.value = (data || []).filter(t => t.status === 1)
  } catch (error) {
    console.error('加载表单模板列表失败:', error)
  }
}

function getTypeLabel(typeCode) {
  const template = templates.value.find(t => t.code === typeCode)
  return template?.name || typeCode
}

async function fetchTemplate() {
  const code = form.value.type
  if (!code) {
    templateFields.value = []
    dynamicFormData.value = {}
    return
  }
  templateLoading.value = true
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
  } finally {
    templateLoading.value = false
  }
}

const fetchPreview = async () => {
  const currentUserId = authStore.currentUser?.id
  const type = form.value.type
  if (!currentUserId || !type) {
    resolverResult.value = null
    return
  }
  resolverLoading.value = true
  try {
    const result = await approverRuleApi.preview({
      applicantId: currentUserId,
      type: type
    })
    resolverResult.value = result
  } catch (error) {
    console.error('获取审批人预览失败:', error)
    resolverResult.value = null
  } finally {
    resolverLoading.value = false
  }
}

watch(() => form.value.type, () => {
  fetchPreview()
  fetchTemplate()
})

// 编辑模式下加载已有数据
onMounted(async () => {
  await loadTemplates()
  await userStore.fetchUsers()
  if (isEditMode.value) {
    const result = await approvalStore.fetchApprovalById(approvalId.value)
    if (result.success) {
      const data = result.data
      form.value.type = data.type || ''
      form.value.title = data.title
      form.value.priority = reversePriorityMap[data.priority] || data.priority
      form.value.content = data.content

      // 解析 formData 回填动态字段
      let fd = data.formData
      if (typeof fd === 'string' && fd) {
        try { fd = JSON.parse(fd) } catch (e) { fd = {} }
      }
      if (fd && typeof fd === 'object') {
        dynamicFormData.value = { ...fd }
      }

      await fetchPreview()
      await fetchTemplate()
    } else {
      showAlertDialog('加载失败', '加载审批数据失败：' + result.message, () => {
        router.push('/approval')
      })
    }
  }
})

async function handleSubmit() {
  try {
    // 构建符合后端 API 格式的请求数据
    const approvalData = {
      title: form.value.title,
      priority: priorityMap[form.value.priority],
      content: form.value.content,
      formData: { ...dynamicFormData.value }
    }

    if (isEditMode.value) {
      const result = await approvalStore.updateApproval(approvalId.value, approvalData)
      if (result.success) {
        router.push(`/approval/detail/${approvalId.value}`)
      } else {
        showAlertDialog('保存失败', '保存失败：' + result.message)
      }
    } else {
      approvalData.type = form.value.type
      const result = await approvalStore.createApproval(approvalData)
      if (result.success) {
        // 创建成功后自动提交审批
        const newId = result.data
        const submitResult = await approvalStore.submitApproval(newId)
        if (submitResult.success) {
          router.push(`/approval/detail/${newId}`)
        } else {
          showAlertDialog('提交失败', '提交审批失败：' + submitResult.message)
        }
      } else {
        showAlertDialog('创建失败', '创建失败：' + result.message)
      }
    }
  } catch (error) {
    console.error(isEditMode.value ? '保存审批失败:' : '创建审批失败:', error)
    showAlertDialog('操作失败', isEditMode.value ? '保存失败，请稍后重试' : '创建失败，请稍后重试')
  }
}

async function handleSaveDraft() {
  if (isEditMode.value) {
    // 编辑模式下保存草稿即更新内容
    await handleSubmit()
    return
  }

  try {
    const approvalData = {
      title: form.value.title || '无标题草稿',
      priority: priorityMap[form.value.priority],
      content: form.value.content,
      type: form.value.type,
      formData: { ...dynamicFormData.value }
    }
    const result = await approvalStore.createApproval(approvalData)
    if (result.success) {
      router.push('/approval')
    } else {
      showAlertDialog('保存失败', '保存草稿失败：' + result.message)
    }
  } catch (error) {
    console.error('保存草稿失败:', error)
    showAlertDialog('操作失败', '保存草稿失败，请稍后重试')
  }
}
</script>
