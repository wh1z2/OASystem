<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useActivityTracker } from '@/composables/useActivityTracker'

const authStore = useAuthStore()

useActivityTracker()

onMounted(() => {
  authStore.initAuth()
})
</script>
