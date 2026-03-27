<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-gray-900">表单设计器</h2>
      <div class="flex items-center gap-3">
        <button @click="previewForm" class="btn btn-secondary flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          预览
        </button>
        <button @click="saveForm" class="btn btn-primary flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          保存表单
        </button>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
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
              <p class="text-gray-500">拖拽或点击左侧组件添加到表单</p>
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
                    <textarea v-model="field.options" class="input text-sm h-20 resize-none" placeholder="选项1&#10;选项2&#10;选项3"></textarea>
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
            <label class="block text-sm font-medium text-gray-700 mb-1">表单名称</label>
            <input v-model="formSettings.name" type="text" class="input" placeholder="请输入表单名称" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">表单描述</label>
            <textarea v-model="formSettings.description" class="input h-20 resize-none" placeholder="请输入表单描述"></textarea>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">审批流程</label>
            <select v-model="formSettings.approvalFlow" class="input">
              <option value="">请选择审批流程</option>
              <option value="simple">简单审批</option>
              <option value="department">部门审批</option>
              <option value="multi">多级审批</option>
            </select>
          </div>
        </div>
      </div>
    </div>

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
          <div v-for="field in formFields" :key="field.id">
            <label class="block text-sm font-medium text-gray-700 mb-1">
              {{ field.label }}
              <span v-if="field.required" class="text-danger-500">*</span>
            </label>
            
            <input v-if="field.type === 'text'" type="text" :placeholder="field.placeholder" class="input" />
            <input v-else-if="field.type === 'number'" type="number" :placeholder="field.placeholder" class="input" />
            <input v-else-if="field.type === 'email'" type="email" :placeholder="field.placeholder" class="input" />
            <input v-else-if="field.type === 'date'" type="date" class="input" />
            <textarea v-else-if="field.type === 'textarea'" :placeholder="field.placeholder" class="input h-24 resize-none"></textarea>
            <select v-else-if="field.type === 'select'" class="input">
              <option value="">请选择</option>
              <option v-for="(option, index) in field.options.split('\n')" :key="index" :value="option">{{ option }}</option>
            </select>
          </div>
          
          <button class="btn btn-primary w-full mt-6">提交表单</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

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
  description: '',
  approvalFlow: ''
})
const showPreview = ref(false)

function addComponent(component) {
  formFields.value.push({
    id: Date.now(),
    type: component.type,
    label: component.label,
    name: `field_${Date.now()}`,
    placeholder: '',
    required: false,
    options: ''
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

function saveForm() {
  alert('表单保存成功！')
}
</script>
