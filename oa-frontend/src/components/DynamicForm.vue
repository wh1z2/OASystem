<template>
  <div class="space-y-4">
    <div v-for="field in fields" :key="field.id || field.name">
      <label class="block text-sm font-medium text-gray-700 mb-1">
        {{ field.label }}
        <span v-if="field.required && !readonly" class="text-danger-500">*</span>
      </label>

      <!-- 只读模式 -->
      <div v-if="readonly" class="text-gray-900">
        <span v-if="field.type === 'checkbox'">{{ formatCheckboxValue(formData[field.name]) }}</span>
        <span v-else-if="field.type === 'select' || field.type === 'radio'">{{ formatSelectValue(formData[field.name], field.options) }}</span>
        <span v-else>{{ formData[field.name] ?? '-' }}</span>
      </div>

      <!-- 编辑模式 -->
      <template v-else>
        <!-- 单行文本 -->
        <input
          v-if="field.type === 'text'"
          v-model="formData[field.name]"
          type="text"
          :placeholder="field.placeholder || '请输入'"
          class="input"
          :required="field.required"
        />

        <!-- 多行文本 -->
        <textarea
          v-else-if="field.type === 'textarea'"
          v-model="formData[field.name]"
          :placeholder="field.placeholder || '请输入'"
          class="input h-24 resize-none"
          :required="field.required"
        ></textarea>

        <!-- 数字输入 -->
        <input
          v-else-if="field.type === 'number'"
          v-model="formData[field.name]"
          type="number"
          :placeholder="field.placeholder || '请输入数字'"
          class="input"
          :required="field.required"
        />

        <!-- 邮箱输入 -->
        <input
          v-else-if="field.type === 'email'"
          v-model="formData[field.name]"
          type="email"
          :placeholder="field.placeholder || '请输入邮箱'"
          class="input"
          :required="field.required"
        />

        <!-- 日期选择 -->
        <input
          v-else-if="field.type === 'date'"
          v-model="formData[field.name]"
          type="date"
          class="input"
          :required="field.required"
        />

        <!-- 下拉选择 -->
        <select
          v-else-if="field.type === 'select'"
          v-model="formData[field.name]"
          class="input"
          :required="field.required"
        >
          <option value="">请选择</option>
          <option v-for="opt in parseOptions(field.options)" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>

        <!-- 单选框 -->
        <div v-else-if="field.type === 'radio'" class="flex flex-wrap gap-4">
          <label
            v-for="opt in parseOptions(field.options)"
            :key="opt.value"
            class="flex items-center gap-2 cursor-pointer"
          >
            <input
              v-model="formData[field.name]"
              type="radio"
              :value="opt.value"
              class="text-primary-600"
              :required="field.required"
            />
            <span class="text-sm text-gray-600">{{ opt.label }}</span>
          </label>
        </div>

        <!-- 多选框 -->
        <div v-else-if="field.type === 'checkbox'" class="flex flex-wrap gap-4">
          <label
            v-for="opt in parseOptions(field.options)"
            :key="opt.value"
            class="flex items-center gap-2 cursor-pointer"
          >
            <input
              v-model="formData[field.name]"
              type="checkbox"
              :value="opt.value"
              class="text-primary-600"
            />
            <span class="text-sm text-gray-600">{{ opt.label }}</span>
          </label>
        </div>

        <!-- 未知类型回退 -->
        <input
          v-else
          v-model="formData[field.name]"
          type="text"
          :placeholder="field.placeholder || '请输入'"
          class="input"
          :required="field.required"
        />
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  fields: {
    type: Array,
    default: () => []
  },
  modelValue: {
    type: Object,
    default: () => ({})
  },
  readonly: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const formData = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

function parseOptions(options) {
  if (!options) return []
  if (Array.isArray(options)) {
    return options.map(opt => {
      if (typeof opt === 'string') return { value: opt, label: opt }
      return opt
    })
  }
  if (typeof options === 'string') {
    return options.split('\n').filter(Boolean).map(s => ({ value: s, label: s }))
  }
  return []
}

function formatSelectValue(value, options) {
  if (value == null || value === '') return '-'
  const opts = parseOptions(options)
  const found = opts.find(o => o.value === value)
  return found ? found.label : value
}

function formatCheckboxValue(value) {
  if (!value || (Array.isArray(value) && value.length === 0)) return '-'
  if (Array.isArray(value)) return value.join(', ')
  return value
}
</script>
