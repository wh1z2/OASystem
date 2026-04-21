<template>
  <div class="space-y-6">
    <!-- 页面标题 -->
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-gray-900">默认审批人配置</h2>
      <button @click="openCreateModal" class="btn btn-primary flex items-center gap-2">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
        </svg>
        新建规则
      </button>
    </div>

    <!-- 搜索和筛选 -->
    <div class="card">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[200px]">
          <div class="relative">
            <input v-model="query.keyword" type="text" class="input pl-10" placeholder="搜索规则名称..." @keyup.enter="handleSearch" />
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
            </svg>
          </div>
        </div>
        <select v-model="query.strategyType" class="input w-40" @change="handleSearch">
          <option value="">全部策略</option>
          <option value="1">按部门角色</option>
          <option value="3">固定人员</option>
        </select>
        <select v-model="query.status" class="input w-32" @change="handleSearch">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">禁用</option>
        </select>
      </div>
    </div>

    <!-- 规则列表 -->
    <div class="card overflow-hidden">
      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-200">
            <th class="text-left py-3 px-4 text-sm font-medium text-gray-700">规则名称</th>
            <th class="text-left py-3 px-4 text-sm font-medium text-gray-700">策略类型</th>
            <th class="text-left py-3 px-4 text-sm font-medium text-gray-700">匹配条件</th>
            <th class="text-left py-3 px-4 text-sm font-medium text-gray-700">审批人</th>
            <th
              class="text-left py-3 px-4 text-sm font-medium text-gray-700 cursor-pointer select-none hover:text-primary-600"
              @click="handleSort('priority')"
            >
              优先级
              <span v-if="sortField === 'priority'" class="ml-1 text-xs">{{ sortDirection === 'asc' ? '↑' : '↓' }}</span>
            </th>
            <th class="text-left py-3 px-4 text-sm font-medium text-gray-700">状态</th>
            <th class="text-right py-3 px-4 text-sm font-medium text-gray-700">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="rule in rules" :key="rule.id" class="border-b border-gray-100 hover:bg-gray-50">
            <td class="py-3 px-4">
              <div class="font-medium text-gray-900">{{ rule.name }}</div>
              <div class="text-xs text-gray-500">{{ rule.description }}</div>
            </td>
            <td class="py-3 px-4">
              <span class="badge badge-primary">{{ rule.strategyTypeName }}</span>
            </td>
            <td class="py-3 px-4">
              <div class="text-sm text-gray-600 max-w-[200px] truncate">{{ formatMatchConditions(rule.matchConditions) }}</div>
            </td>
            <td class="py-3 px-4">
              <span class="text-sm text-gray-700">{{ rule.approverTypeName }}</span>
              <div class="text-xs text-gray-500">{{ rule.approverValue }}</div>
            </td>
            <td class="py-3 px-4 text-sm text-gray-700">{{ rule.priority }}</td>
            <td class="py-3 px-4">
              <span :class="rule.status === 1 ? 'badge badge-success' : 'badge badge-danger'">
                {{ rule.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="py-3 px-4 text-right">
              <div class="flex items-center justify-end gap-2">
                <button @click="openEditModal(rule)" class="text-primary-600 hover:text-primary-700 text-sm font-medium cursor-pointer">
                  编辑
                </button>
                <button @click="handleDelete(rule)" class="text-danger-500 hover:text-danger-600 text-sm font-medium cursor-pointer">
                  删除
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="rules.length === 0">
            <td colspan="7" class="py-8 text-center text-gray-500">
              暂无审批规则，点击「新建规则」创建
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新建/编辑弹窗 -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-lg shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto">
        <div class="p-6 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-gray-900">{{ isEditMode ? '编辑规则' : '新建规则' }}</h3>
        </div>
        <div class="p-6 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">规则名称 <span class="text-danger-500">*</span></label>
            <input v-model="form.name" type="text" class="input" placeholder="如：技术部请假审批规则" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">策略类型 <span class="text-danger-500">*</span></label>
            <select v-model="form.strategyType" class="input">
              <option value="">请选择策略类型</option>
              <option value="1">按部门角色</option>
              <option value="3">固定人员</option>
            </select>
          </div>

          <!-- 策略1：按部门角色的匹配条件 -->
          <div v-if="form.strategyType === '1' || form.strategyType === 1">
            <label class="block text-sm font-medium text-gray-700 mb-1">匹配条件</label>
            <div class="space-y-2">
              <div>
                <label class="text-xs text-gray-500">适用部门</label>
                <select v-model="form.matchDeptId" class="input">
                  <option value="">全部部门</option>
                  <option v-for="dept in departments" :key="dept.id" :value="dept.id">{{ dept.name }}</option>
                </select>
              </div>
              <div>
                <label class="text-xs text-gray-500">适用审批类型</label>
                <div class="flex flex-wrap gap-2 mt-1">
                  <label v-for="type in approvalTypes" :key="type.value" class="flex items-center gap-1 text-sm cursor-pointer">
                    <input type="checkbox" :value="type.value" v-model="form.matchTypes" />
                    <span>{{ type.label }}</span>
                  </label>
                </div>
              </div>
            </div>
          </div>

          <!-- 策略3：固定人员的匹配条件 -->
          <div v-if="form.strategyType === '3' || form.strategyType === 3">
            <label class="block text-sm font-medium text-gray-700 mb-1">适用审批类型</label>
            <div class="flex flex-wrap gap-2">
              <label v-for="type in approvalTypes" :key="type.value" class="flex items-center gap-1 text-sm cursor-pointer">
                <input type="checkbox" :value="type.value" v-model="form.matchTypes" />
                <span>{{ type.label }}</span>
              </label>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">审批人类型 <span class="text-danger-500">*</span></label>
            <select v-model="form.approverType" class="input">
              <option value="">请选择审批人类型</option>
              <option value="1">指定用户</option>
              <option value="2">指定角色</option>
            </select>
          </div>

          <div v-if="form.approverType === '1' || form.approverType === 1">
            <label class="block text-sm font-medium text-gray-700 mb-1">指定审批人 <span class="text-danger-500">*</span></label>
            <select v-model="form.approverUserId" class="input">
              <option value="">请选择用户</option>
              <option v-for="user in users" :key="user.id" :value="user.id">{{ user.name }} ({{ user.username }})</option>
            </select>
          </div>

          <div v-if="form.approverType === '2' || form.approverType === 2">
            <label class="block text-sm font-medium text-gray-700 mb-1">指定角色 <span class="text-danger-500">*</span></label>
            <select v-model="form.approverRoleId" class="input">
              <option value="">请选择角色</option>
              <option v-for="role in roles" :key="role.id" :value="role.id">{{ role.label }}</option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">优先级</label>
            <input v-model.number="form.priority" type="number" class="input" placeholder="数字越小优先级越高，默认100" />
            <p class="text-xs text-gray-500 mt-1">数字越小优先级越高</p>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">状态</label>
            <div class="flex gap-4">
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" v-model="form.status" :value="1" />
                <span class="text-sm">启用</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" v-model="form.status" :value="0" />
                <span class="text-sm">禁用</span>
              </label>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">规则描述</label>
            <textarea v-model="form.description" class="input h-20 resize-none" placeholder="请输入规则描述..."></textarea>
          </div>
        </div>
        <div class="p-6 border-t border-gray-200 flex gap-3">
          <button @click="handleSubmit" class="btn btn-primary flex-1">{{ isEditMode ? '保存修改' : '创建规则' }}</button>
          <button @click="closeModal" class="btn btn-secondary">取消</button>
        </div>
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

    <!-- 删除确认弹窗 -->
    <ConfirmDialog
      :visible="showDeleteConfirm"
      title="删除确认"
      :message="`确定要删除规则「${deleteRuleTarget?.name || ''}」吗？`"
      @confirm="handleDeleteConfirm"
      @cancel="handleDeleteCancel"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { approverRuleApi } from '@/api/approverRule'
import { useUserStore } from '@/stores/user'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const userStore = useUserStore()

const rules = ref([])
const loading = ref(false)
const showModal = ref(false)
const isEditMode = ref(false)
const editingId = ref(null)

// 轻量提示弹窗状态
const showAlert = ref(false)
const alertTitle = ref('提示')
const alertMessage = ref('')

// 删除确认弹窗状态
const showDeleteConfirm = ref(false)
const deleteRuleTarget = ref(null)

function showAlertDialog(title, message) {
  alertTitle.value = title
  alertMessage.value = message
  showAlert.value = true
}

function handleAlertConfirm() {
  showAlert.value = false
}

const query = reactive({
  keyword: '',
  strategyType: '',
  status: ''
})

// 排序状态
const sortField = ref('')
const sortDirection = ref('') // 'asc' | 'desc'

const form = reactive({
  name: '',
  strategyType: '',
  matchDeptId: '',
  matchTypes: [],
  approverType: '',
  approverUserId: '',
  approverRoleId: '',
  priority: 100,
  status: 1,
  description: ''
})

const approvalTypes = [
  { value: 1, label: '请假' },
  { value: 2, label: '报销' },
  { value: 3, label: '采购' },
  { value: 4, label: '加班' },
  { value: 5, label: '出差' }
]

const departments = [
  { id: 1, name: '技术部' },
  { id: 2, name: '财务部' },
  { id: 3, name: '人事部' },
  { id: 4, name: '系统管理部' }
]

const users = computed(() => userStore.users)
const roles = computed(() => userStore.roles)

function resetForm() {
  form.name = ''
  form.strategyType = ''
  form.matchDeptId = ''
  form.matchTypes = []
  form.approverType = ''
  form.approverUserId = ''
  form.approverRoleId = ''
  form.priority = 100
  form.status = 1
  form.description = ''
}

function openCreateModal() {
  resetForm()
  isEditMode.value = false
  editingId.value = null
  showModal.value = true
}

function openEditModal(rule) {
  resetForm()
  isEditMode.value = true
  editingId.value = rule.id
  form.name = rule.name
  form.strategyType = String(rule.strategyType)
  form.approverType = String(rule.approverType)
  form.priority = rule.priority
  form.status = rule.status
  form.description = rule.description || ''

  // 解析匹配条件
  if (rule.matchConditions) {
    if (rule.matchConditions.deptIds && rule.matchConditions.deptIds.length > 0) {
      form.matchDeptId = String(rule.matchConditions.deptIds[0])
    }
    if (rule.matchConditions.types) {
      form.matchTypes = rule.matchConditions.types.map(t => String(t))
    }
  }

  // 解析审批人值
  if (rule.approverValue) {
    try {
      const values = JSON.parse(rule.approverValue)
      if (Array.isArray(values) && values.length > 0) {
        if (rule.approverType === 1) {
          form.approverUserId = String(values[0])
        } else if (rule.approverType === 2) {
          form.approverRoleId = String(values[0])
        }
      }
    } catch (e) {
      // ignore
    }
  }

  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

function buildMatchConditions() {
  const conditions = {}
  if (form.strategyType === '1' || form.strategyType === 1) {
    if (form.matchDeptId) {
      conditions.deptIds = [parseInt(form.matchDeptId)]
    }
  }
  if (form.matchTypes.length > 0) {
    conditions.types = form.matchTypes.map(t => parseInt(t))
  }
  return conditions
}

function buildApproverValue() {
  if (form.approverType === '1' || form.approverType === 1) {
    if (form.approverUserId) {
      return JSON.stringify([parseInt(form.approverUserId)])
    }
  } else if (form.approverType === '2' || form.approverType === 2) {
    if (form.approverRoleId) {
      return JSON.stringify([parseInt(form.approverRoleId)])
    }
  }
  return ''
}

async function handleSubmit() {
  if (!form.name) {
    showAlertDialog('表单校验', '请输入规则名称')
    return
  }
  if (!form.strategyType) {
    showAlertDialog('表单校验', '请选择策略类型')
    return
  }
  if (!form.approverType) {
    showAlertDialog('表单校验', '请选择审批人类型')
    return
  }

  const approverValue = buildApproverValue()
  if (!approverValue) {
    showAlertDialog('表单校验', '请指定审批人')
    return
  }

  const payload = {
    name: form.name,
    strategyType: parseInt(form.strategyType),
    matchConditions: buildMatchConditions(),
    approverType: parseInt(form.approverType),
    approverValue: approverValue,
    priority: form.priority,
    status: form.status,
    description: form.description,
    scopeType: 1
  }

  try {
    if (isEditMode.value) {
      await approverRuleApi.update(editingId.value, payload)
    } else {
      await approverRuleApi.create(payload)
    }
    closeModal()
    fetchRules()
  } catch (error) {
    showAlertDialog('操作失败', error.message || '操作失败')
  }
}

function handleDelete(rule) {
  deleteRuleTarget.value = rule
  showDeleteConfirm.value = true
}

function handleDeleteCancel() {
  showDeleteConfirm.value = false
  deleteRuleTarget.value = null
}

async function handleDeleteConfirm() {
  showDeleteConfirm.value = false
  if (!deleteRuleTarget.value) return
  try {
    await approverRuleApi.delete(deleteRuleTarget.value.id)
    fetchRules()
  } catch (error) {
    showAlertDialog('删除失败', error.message || '删除失败')
  } finally {
    deleteRuleTarget.value = null
  }
}

function formatMatchConditions(conditions) {
  if (!conditions) return '无'
  const parts = []
  if (conditions.deptIds) {
    const deptNames = conditions.deptIds.map(id => {
      const dept = departments.find(d => d.id === id)
      return dept ? dept.name : id
    })
    parts.push('部门：' + deptNames.join(','))
  }
  if (conditions.types) {
    const typeNames = conditions.types.map(t => {
      const type = approvalTypes.find(at => at.value === t)
      return type ? type.label : t
    })
    parts.push('类型：' + typeNames.join(','))
  }
  return parts.join('；') || '全部'
}

async function fetchRules() {
  loading.value = true
  try {
    const params = {}
    if (query.keyword) params.keyword = query.keyword
    if (query.strategyType) params.strategyType = query.strategyType
    if (query.status) params.status = query.status
    if (sortField.value) {
      params.orderBy = sortField.value
      params.orderDirection = sortDirection.value
    }
    const result = await approverRuleApi.getList(params)
    rules.value = result.records || []
  } catch (error) {
    console.error('获取规则列表失败:', error)
    // 降级到本地 mock 数据（如果后端未启动）
    rules.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchRules()
}

function handleSort(field) {
  if (sortField.value === field) {
    // 切换方向
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    // 新字段，默认升序
    sortField.value = field
    sortDirection.value = 'asc'
  }
  fetchRules()
}

onMounted(() => {
  fetchRules()
  userStore.fetchUsers()
})
</script>
