<template>
  <div class="min-h-screen bg-gradient-to-br from-primary-600 to-primary-800 flex items-center justify-center p-4">
    <div class="w-full max-w-md">
      <div class="bg-white rounded-2xl shadow-xl p-8">
        <div class="text-center mb-8">
          <div class="w-16 h-16 bg-primary-600 rounded-xl flex items-center justify-center mx-auto mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-8 h-8 text-white">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25zM6.75 12h.008v.008H6.75V12zm0 3h.008v.008H6.75V15zm0 3h.008v.008H6.75V18z" />
            </svg>
          </div>
          <h1 class="text-2xl font-bold text-gray-900">OA审批系统</h1>
          <p class="text-gray-500 mt-2">企业办公自动化平台</p>
        </div>

        <form @submit.prevent="handleLogin">
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">用户名</label>
              <input
                v-model="form.username"
                type="text"
                class="input"
                placeholder="请输入用户名"
                required
              />
            </div>
            
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">密码</label>
              <div class="relative">
                <input
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="input pr-10"
                  placeholder="请输入密码"
                  required
                />
                <button type="button" @click="togglePasswordVisibility" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 focus:outline-none">
                  <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                  </svg>
                </button>
              </div>
            </div>

            <div v-if="errorMessage" class="p-3 bg-danger-50 text-danger-600 text-sm rounded-lg">
              {{ errorMessage }}
            </div>

            <button
              type="submit"
              :disabled="loading"
              class="w-full btn btn-primary py-3 flex items-center justify-center gap-2"
            >
              <svg v-if="loading" class="animate-spin h-5 w-5" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span>{{ loading ? '登录中...' : '登 录' }}</span>
            </button>
          </div>
        </form>

        <div class="mt-6 pt-6 border-t border-gray-200">
          <p class="text-center text-sm text-gray-500 mb-3">测试账号</p>
          <div class="grid grid-cols-3 gap-2 text-xs">
            <button @click="fillTestAccount('admin')" class="p-2 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer">
              <p class="font-medium text-gray-900">管理员</p>
              <p class="text-gray-500">admin</p>
            </button>
            <button @click="fillTestAccount('manager')" class="p-2 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer">
              <p class="font-medium text-gray-900">经理</p>
              <p class="text-gray-500">manager</p>
            </button>
            <button @click="fillTestAccount('user')" class="p-2 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer">
              <p class="font-medium text-gray-900">员工</p>
              <p class="text-gray-500">user</p>
            </button>
          </div>
          <p class="text-center text-xs text-gray-400 mt-2">密码均为: 账号名 + 123</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = ref({
  username: '',
  password: ''
})
const loading = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)

function togglePasswordVisibility() {
  showPassword.value = !showPassword.value
}

function fillTestAccount(type) {
  const accounts = {
    admin: { username: 'admin', password: 'admin123' },
    manager: { username: 'manager', password: 'manager123' },
    user: { username: 'user', password: 'user123' }
  }
  form.value = { ...accounts[type] }
}

async function handleLogin() {
  loading.value = true
  errorMessage.value = ''

  const result = await authStore.login(form.value)

  if (result.success) {
    router.push('/')
  } else {
    errorMessage.value = result.message
  }

  loading.value = false
}
</script>
