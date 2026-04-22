<template>
  <div class="space-y-6">
    <!-- 列表模式 -->
    <div v-if="mode === 'list'">
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-semibold text-gray-900">表单模板管理</h2>
        <button @click="createNew" class="btn btn-primary flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          新建表单
        </button>
      </div>

      <div class="card mt-6">
        <div v-if="loading" class="py-12 text-center text-gray-500">加载中...</div>
        <div v-else-if="templates.length === 0" class="py-12 text-center text-gray-500">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-12 h-12 text-gray-300 mx-auto mb-3">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
          </svg>
          <p>暂无表单模板</p>
        </div>
        <div v-else class="divide-y divide-gray-100">
          <div v-for="template in templates" :key="template.id" class="py-4 flex items-center justify-between">
            <div>
              <p class="font-medium text-gray-900">{{ template.name }}</p>
              <p class="text-sm text-gray-500">编码: {{ template.code }} | {{ template.description || '无描述' }}</p>
            </div>
            <div class="flex items-center gap-2">
              <button @click="editTemplate(template)" class="btn btn-secondary text-sm">编辑</button>
              <button @click="confirmDelete(template)" class="btn btn-danger text-sm">删除</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 设计模式 -->
    <div v-else>
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <button @click="backToList" class="text-gray-400 hover:text-gray-600 cursor-pointer">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
              <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
            </svg>
          </button>
          <h2 class="text-xl font-semibold text-gray-900">{{ isEditing ? '编辑表单' : '新建表单' }}</h2>
        </div>
        <div class="flex items-center gap-3">
          <button @click="previewForm" class="btn btn-secondary flex items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            预览
          </button>
          <button @click="saveForm" :disabled="saving" class="btn btn-primary flex items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {{ saving ? '保存中...' : '保存表单' }}
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-4 gap-6 mt-6">
        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">表单组件</h3>
          <div class="space-y-2">
            <div v-for="component in formComponents" :key="component.type"
                 @click="addComponent(component)"
                 class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer">
              <div class="w-8 h-8 bg-primary-100 rounded-lg flex items-center justify-center">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4 text-primary-600">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                </svg>
              </div>
              <span class="text-sm font-medium text-gray-700">{{ component.label }}</span>
            </div>
          </div>
        </div>

        <div class="lg:col-span-2">
          <div class="card min-h-[600px]">
            <div class="flex items-center justify-between mb-6">
              <h3 class="text-lg font-semibold text-gray-900">表单设计</h3>
              <div class="flex items-center gap-2 text-sm text-gray-500">
                <span>{{ formFields.length }} 个组件</span>
              </div>
            </div>

            <div class="space-y-4">
              <div v-if="formFields.length === 0" class="border-2 border-dashed border-gray-200 rounded-lg p-12 text-center">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-12 h-12 text-gray-300 mx-auto mb-3">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                </svg>
                <p class="text-gray-500">点击左侧组件添加到表单</p>
              </div>

              <div v-for="(field, index) in formFields" :key="field.id"
                   class="relative group">
                <div class="border border-gray-200 rounded-lg p-4 hover:border-primary-300 transition-colors">
                  <div class="flex items-start justify-between mb-3">
                    <div class="flex-1">
                      <input v-model="field.label" type="text" class="text-sm font-medium text-gray-900 bg-transparent border-b border-transparent hover:border-gray-300 focus:border-primary-500 focus:outline-none w-full" placeholder="字段名称" />
                    </div>
                    <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button @click="moveField(index, -1)" class="p-1 hover:bg-gray-100 rounded cursor-pointer" :disabled="index === 0">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4 text-gray-500">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 15.75l7.5-7.5 7.5 7.5" />
                        </svg>
                      </button>
                      <button @click="moveField(index, 1)" class="p-1 hover:bg-gray-100 rounded cursor-pointer" :disabled="index === formFields.length - 1">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4 text-gray-500">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                        </svg>
                      </button>
                      <button @click="removeField(index)" class="p-1 hover:bg-danger-50 rounded cursor-pointer">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4 text-danger-500">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                    </div>
                  </div>

                  <div class="space-y-3">
                    <div>
                      <label class="block text-xs text-gray-500 mb-1">字段标识</label>
                      <input v-model="field.name" type="text" class="input text-sm py-1.5" placeholder="field_name" />
                    </div>

                    <div v-if="field.type === 'select' || field.type === 'radio' || field.type === 'checkbox'">
                      <label class="block text-xs text-gray-500 mb-1">选项 (每行一个)</label>
                      <textarea v-model="field.optionsText" class="input text-sm h-20 resize-none" placeholder="选项1&#10;选项2&#10;选项3"></textarea>
                    </div>

                    <div>
                      <label class="block text-xs text-gray-500 mb-1">占位提示</label>
                      <input v-model="field.placeholder" type="text" class="input text-sm py-1.5" placeholder="请输入..." />
                    </div>

                    <div class="flex items-center gap-4">
                      <label class="flex items-center gap-2 cursor-pointer">
                        <input v-model="field.required" type="checkbox" class="text-primary-600" />
                        <span class="text-sm text-gray-600">必填</span>
                      </label>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="card">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">表单设置</h3>
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">表单名称 <span class="text-danger-500">*</span></label>
              <input v-model="formSettings.name" type="text" class="input" placeholder="请输入表单名称" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">表单编码 <span class="text-danger-500">*</span></label>
              <input v-model="formSettings.code" type="text" class="input" :disabled="isEditing" placeholder="如 LEAVE_FORM" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">表单描述</label>
              <textarea v-model="formSettings.description" class="input h-20 resize-none" placeholder="请输入表单描述"></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">状态</label>
              <select v-model="formSettings.status" class="input">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 预览弹窗 -->
    <div v-if="showPreview" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="showPreview = false">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[80vh] overflow-y-auto p-6">
        <div class="flex items-center justify-between mb-6">
          <h3 class="text-lg font-semibold text-gray-900">表单预览</h3>
          <button @click="showPreview = false" class="text-gray-400 hover:text-gray-600 cursor-pointer">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="space-y-4">
          <DynamicForm :fields="previewFields" v-model="previewFormData" />
          <button class="btn btn-primary w-full mt-6">提交表单</button>
        </div>
      </div>
    </div>

    <!-- 确认弹窗 -->
    <ConfirmDialog
      :visible="showAlert"
      :title="alertTitle"
      :message="alertMessage"
      :show-cancel="alertShowCancel"
      @confirm="handleAlertConfirm"
      @cancel="showAlert = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { formTemplateApi } from '@/api/formTemplate'
import DynamicForm from '@/components/DynamicForm.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const mode = ref('list') // 'list' | 'design'
const loading = ref(false)
const saving = ref(false)
const templates = ref([])
const currentTemplateId = ref(null)

const isEditing = computed(() => !!currentTemplateId.value)

const formComponents = [
  { type: 'text', label: '单行文本' },
  { type: 'textarea', label: '多行文本' },
  { type: 'number', label: '数字输入' },
  { type: 'email', label: '邮箱输入' },
  { type: 'date', label: '日期选择' },
  { type: 'select', label: '下拉选择' },
  { type: 'radio', label: '单选框' },
  { type: 'checkbox', label: '多选框' }
]

const formFields = ref([])
const formSettings = ref({
  name: '',
  code: '',
  description: '',
  status: 1
})
const showPreview = ref(false)
const previewFormData = ref({})

const previewFields = computed(() => {
  return formFields.value.map(f => ({
    ...f,
    options: f.optionsText
  }))
})

// 弹窗状态
const showAlert = ref(false)
const alertTitle = ref('提示')
const alertMessage = ref('')
const alertShowCancel = ref(false)
const alertOnConfirm = ref(null)

function showAlertDialog(title, message, { showCancel = false, onConfirm = null } = {}) {
  alertTitle.value = title
  alertMessage.value = message
  alertShowCancel.value = showCancel
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

async function loadTemplates() {
  loading.value = true
  try {
    const res = await formTemplateApi.getAll()
    if (res.data?.code === 200) {
      templates.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载表单模板失败:', error)
  } finally {
    loading.value = false
  }
}

function createNew() {
  currentTemplateId.value = null
  formFields.value = []
  formSettings.value = { name: '', code: '', description: '', status: 1 }
  mode.value = 'design'
}

async function editTemplate(template) {
  currentTemplateId.value = template.id
  formSettings.value = {
    name: template.name,
    code: template.code,
    description: template.description || '',
    status: template.status ?? 1
  }

  // 解析 fieldsConfig
  let fields = []
  if (template.fieldsConfig) {
    try {
      fields = typeof template.fieldsConfig === 'string'
        ? JSON.parse(template.fieldsConfig)
        : template.fieldsConfig
    } catch (e) {
      console.error('解析字段配置失败:', e)
    }
  }

  formFields.value = fields.map(f => ({
    ...f,
    optionsText: parseOptionsToText(f.options)
  }))
  mode.value = 'design'
}

function parseOptionsToText(options) {
  if (!options) return ''
  if (Array.isArray(options)) {
    return options.map(opt => {
      if (typeof opt === 'string') return opt
      return opt.label || opt.value || ''
    }).join('\n')
  }
  if (typeof options === 'string') return options
  return ''
}

function backToList() {
  mode.value = 'list'
  currentTemplateId.value = null
  loadTemplates()
}

function addComponent(component) {
  formFields.value.push({
    id: 'field_' + Date.now(),
    type: component.type,
    label: component.label,
    name: `field_${Date.now()}`,
    placeholder: '',
    required: false,
    optionsText: ''
  })
}

function removeField(index) {
  formFields.value.splice(index, 1)
}

function moveField(index, direction) {
  const newIndex = index + direction
  if (newIndex >= 0 && newIndex < formFields.value.length) {
    const temp = formFields.value[index]
    formFields.value[index] = formFields.value[newIndex]
    formFields.value[newIndex] = temp
  }
}

function previewForm() {
  showPreview.value = true
}

function buildSaveData() {
  const fieldsConfig = formFields.value.map(f => {
    const field = {
      id: f.id,
      type: f.type,
      label: f.label,
      name: f.name,
      placeholder: f.placeholder,
      required: !!f.required
    }
    if (f.type === 'select' || f.type === 'radio' || f.type === 'checkbox') {
      field.options = f.optionsText.split('\n').filter(Boolean).map(s => ({ value: s, label: s }))
    }
    return field
  })

  return {
    name: formSettings.value.name,
    code: formSettings.value.code,
    description: formSettings.value.description,
    fieldsConfig,
    status: formSettings.value.status
  }
}

async function saveForm() {
  if (!formSettings.value.name.trim()) {
    showAlertDialog('验证失败', '请输入表单名称')
    return
  }
  if (!formSettings.value.code.trim()) {
    showAlertDialog('验证失败', '请输入表单编码')
    return
  }

  saving.value = true
  try {
    const data = buildSaveData()
    if (isEditing.value) {
      // 更新时去掉 code（不可修改）
      const updateData = { ...data }
      delete updateData.code
      const res = await formTemplateApi.update(currentTemplateId.value, updateData)
      if (res.data?.code === 200) {
        showAlertDialog('保存成功', '表单模板已更新', {
          onConfirm: () => backToList()
        })
      } else {
        showAlertDialog('保存失败', res.data?.message || '更新失败')
      }
    } else {
      const res = await formTemplateApi.create(data)
      if (res.data?.code === 200) {
        showAlertDialog('保存成功', '表单模板已创建', {
          onConfirm: () => backToList()
        })
      } else {
        showAlertDialog('保存失败', res.data?.message || '创建失败')
      }
    }
  } catch (error) {
    console.error('保存表单模板失败:', error)
    showAlertDialog('保存失败', error.response?.data?.message || '网络错误，请稍后重试')
  } finally {
    saving.value = false
  }
}

function confirmDelete(template) {
  showAlertDialog('确认删除', `确定要删除表单模板 "${template.name}" 吗？此操作不可恢复。`, {
    showCancel: true,
    onConfirm: () => doDelete(template.id)
  })
}

async function doDelete(id) {
  try {
    const res = await formTemplateApi.delete(id)
    if (res.data?.code === 200) {
      loadTemplates()
    } else {
      showAlertDialog('删除失败', res.data?.message || '删除失败')
    }
  } catch (error) {
    console.error('删除表单模板失败:', error)
    showAlertDialog('删除失败', error.response?.data?.message || '网络错误')
  }
}

onMounted(() => {
  loadTemplates()
})
</script>
