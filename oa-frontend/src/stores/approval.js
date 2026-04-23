import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '@/api/config.js'

// 状态映射 (后端数值 -> 前端字符串)
const statusMap = {
  0: 'draft',
  1: 'processing',
  2: 'approved',
  3: 'returned',
  4: 'revoked'
}

// 优先级映射
const priorityMap = {
  0: 'low',
  1: 'normal',
  2: 'high'
}

export const useApprovalStore = defineStore('approval', () => {
  // 审批列表（ApprovalManage 专用）
  const approvals = ref([])
  // 待办列表（TodoList 专用）
  const todoApprovals = ref([])
  // 已办列表（DoneList 专用）
  const doneApprovals = ref([])
  // 我的申请列表（MyApprovals 专用）
  const myApprovals = ref([])
  // 当前审批详情
  const currentApproval = ref(null)
  // 审批历史
  const approvalHistory = ref([])
  // 分页信息
  const pagination = ref({
    current: 1,
    size: 10,
    total: 0
  })
  // 待办总数（独立存储，不受其他列表分页影响）
  const todoTotal = ref(0)
  const doneTotal = ref(0)
  const myTotal = ref(0)
  // 工作台统计数据（一次性返回）
  const dashboardStatistics = ref(null)

  // 计算属性 - 待办列表 (状态为 processing)
  const pendingApprovals = computed(() =>
    todoApprovals.value.filter(a => a.status === 'processing')
  )

  // 计算属性 - 已通过列表
  const approvedApprovals = computed(() =>
    approvals.value.filter(a => a.status === 'approved')
  )

  // 计算属性 - 已打回列表
  const rejectedApprovals = computed(() =>
    approvals.value.filter(a => a.status === 'returned')
  )

  // 计算属性 - 已办数量
  const doneCount = computed(() => doneTotal.value || 0)

  // 计算属性 - 我的申请数量
  const myApprovalCount = computed(() => myTotal.value || 0)

  // 转换后端数据为前端格式
  function transformApproval(item) {
    return {
      id: item.id,
      title: item.title,
      type: item.type,
      applicant: item.applicantName || item.applicant,
      applicantId: item.applicantId,
      department: item.deptName || item.department,
      status: statusMap[item.status] || item.status,
      priority: priorityMap[item.priority] || item.priority,
      createTime: item.createTime,
      content: item.content,
      currentApprover: item.currentApproverName || item.currentApprover,
      currentApproverId: item.currentApproverId,
      formData: item.formData,
      history: item.history || []
    }
  }

  // 获取审批列表
  async function fetchApprovals(params = {}) {
    try {
      const { records, total, current, size } = await apiClient.get('/approvals', {
        params: {
          current: params.current || 1,
          size: params.size || 10,
          title: params.title || undefined,
          type: params.type || undefined,
          status: params.status !== undefined ? params.status : undefined,
          applicantId: params.applicantId || undefined
        }
      })
      approvals.value = records.map(transformApproval)
      pagination.value = { current, size, total }
      return { success: true, data: approvals.value }
    } catch (error) {
      approvals.value = []
      pagination.value = { current: 1, size: 10, total: 0 }
      return { success: false, message: error.message }
    }
  }

  // 获取审批详情
  async function fetchApprovalById(id) {
    try {
      const data = await apiClient.get(`/approvals/${id}`)
      currentApproval.value = transformApproval(data)
      return { success: true, data: currentApproval.value }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 创建审批工单
  async function createApproval(approvalData) {
    try {
      const data = await apiClient.post('/approvals', approvalData)
      return { success: true, data }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 更新审批工单（仅草稿状态）
  async function updateApproval(id, approvalData) {
    try {
      await apiClient.post(`/approvals/${id}/update`, approvalData)
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 删除审批工单
  async function deleteApproval(id) {
    try {
      await apiClient.delete(`/approvals/${id}`)
      // 从列表中移除
      approvals.value = approvals.value.filter(a => a.id !== id)
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 提交审批
  async function submitApproval(id, nextApproverId) {
    try {
      await apiClient.post(`/approvals/${id}/submit`, {
        nextApproverId: nextApproverId
      })
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 审批通过
  async function approveApproval(id, comment) {
    try {
      await apiClient.post(`/approvals/${id}/approve`, {
        comment: comment
      })
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 审批拒绝
  async function rejectApproval(id, comment) {
    try {
      await apiClient.post(`/approvals/${id}/reject`, {
        comment: comment
      })
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 重新编辑（支持可选携带内容参数，实现一步到位更新）
  async function reeditApproval(id, approvalData) {
    try {
      await apiClient.post(`/approvals/${id}/reedit`, approvalData || {})
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 撤销申请
  async function revokeApproval(id) {
    try {
      await apiClient.post(`/approvals/${id}/revoke`)
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 获取待办列表
  async function fetchTodoList(params = {}) {
    try {
      const { records, total, current, size } = await apiClient.get('/approvals/todo', {
        params: {
          current: params.current || 1,
          size: params.size || 10
        }
      })
      todoApprovals.value = records.map(transformApproval)
      pagination.value = { current, size, total }
      // 独立存储待办总数，用于侧边栏徽章（不受其他分页影响）
      todoTotal.value = total
      return { success: true, data: todoApprovals.value }
    } catch (error) {
      todoApprovals.value = []
      pagination.value = { current: 1, size: 10, total: 0 }
      todoTotal.value = 0
      return { success: false, message: error.message }
    }
  }

  // 获取已办列表
  async function fetchDoneList(params = {}) {
    try {
      const { records, total, current, size } = await apiClient.get('/approvals/done', {
        params: {
          current: params.current || 1,
          size: params.size || 10
        }
      })
      doneApprovals.value = records.map(transformApproval)
      pagination.value = { current, size, total }
      // 独立存储已办总数，用于侧边栏徽章（不受其他分页影响）
      doneTotal.value = total
      return { success: true, data: doneApprovals.value }
    } catch (error) {
      doneApprovals.value = []
      pagination.value = { current: 1, size: 10, total: 0 }
      doneTotal.value = 0
      return { success: false, message: error.message }
    }
  }

  // 获取我的申请列表
  async function fetchMyApprovals(params = {}) {
    try {
      const { records, total, current, size } = await apiClient.get('/approvals/my', {
        params: {
          current: params.current || 1,
          size: params.size || 10
        }
      })
      myApprovals.value = records.map(transformApproval)
      pagination.value = { current, size, total }
      // 独立存储我的申请总数，用于侧边栏徽章（不受其他分页影响）
      myTotal.value = total
      return { success: true, data: myApprovals.value }
    } catch (error) {
      myApprovals.value = []
      pagination.value = { current: 1, size: 10, total: 0 }
      myTotal.value = 0
      return { success: false, message: error.message }
    }
  }

  // 获取工作台统计数据
  async function fetchDashboardStatistics() {
    try {
      const data = await apiClient.get('/approvals/statistics')
      dashboardStatistics.value = data
      return { success: true, data }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 获取审批历史
  async function fetchApprovalHistory(id) {
    try {
      const data = await apiClient.get(`/approvals/${id}/history`)
      approvalHistory.value = data.map(item => ({
        approver: item.approverName || item.approver,
        action: item.action,
        actionName: item.actionName,
        time: item.createTime,
        comment: item.comment,
        isProxy: item.isProxy,
        approvalType: item.approvalType
      }))
      return { success: true, data: approvalHistory.value }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 待办总数（从独立存储的 todoTotal 中获取，不受其他列表分页影响）
  const pendingCount = computed(() => todoTotal.value || 0)

  return {
    approvals,
    todoApprovals,
    doneApprovals,
    myApprovals,
    currentApproval,
    approvalHistory,
    pagination,
    todoTotal,
    doneTotal,
    myTotal,
    dashboardStatistics,
    pendingApprovals,
    approvedApprovals,
    rejectedApprovals,
    pendingCount,
    doneCount,
    myApprovalCount,
    fetchApprovals,
    fetchApprovalById,
    createApproval,
    updateApproval,
    deleteApproval,
    submitApproval,
    approveApproval,
    rejectApproval,
    reeditApproval,
    revokeApproval,
    fetchTodoList,
    fetchDoneList,
    fetchMyApprovals,
    fetchApprovalHistory,
    fetchDashboardStatistics
  }
})
