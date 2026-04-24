import { onMounted, onUnmounted } from 'vue'

const ACTIVITY_EVENTS = ['mousedown', 'keydown', 'touchstart', 'scroll']

/**
 * 用户活动追踪 composable
 * 监听用户操作事件，更新 lastActivity 时间戳到 localStorage
 */
export function useActivityTracker() {
  function updateActivity() {
    localStorage.setItem('lastActivity', Date.now().toString())
  }

  onMounted(() => {
    updateActivity()
    ACTIVITY_EVENTS.forEach(event => {
      document.addEventListener(event, updateActivity, { passive: true })
    })
  })

  onUnmounted(() => {
    ACTIVITY_EVENTS.forEach(event => {
      document.removeEventListener(event, updateActivity)
    })
  })
}
