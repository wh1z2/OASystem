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

// 类型映射 (后端数值 -> 前端字符串)
const typeMap = {
  1: 'leave',
  2: 'expense',
  3: 'purchase',
  4: 'overtime',
  5: 'travel'
}

// 优先级映射
const priorityMap = {
  0: 'low',
  1: 'normal',
  2: 'high'
}

export const useApprovalStore = defineStore('approval', () => {
  // 审批列表
  const approvals = ref([])
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

  // 计算属性 - 待办列表 (状态为 processing)
  const pendingApprovals = computed(() =>
    approvals.value.filter(a => a.status === 'processing')
  )

  // 计算属性 - 已通过列表
  const approvedApprovals = computed(() =>
    approvals.value.filter(a => a.status === 'approved')
  )

  // 计算属性 - 已打回列表
  const rejectedApprovals = computed(() =>
    approvals.value.filter(a => a.status === 'returned')
  )

  // 转换后端数据为前端格式
  function transformApproval(item) {
    return {
      id: item.id,
      title: item.title,
      type: typeMap[item.type] || item.type,
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

  // 更新审批工单
  async function updateApproval(id, approvalData) {
    try {
      await apiClient.put(`/approvals/${id}`, approvalData)
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

  // 重新编辑
  async function reeditApproval(id) {
    try {
      await apiClient.post(`/approvals/${id}/reedit`)
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
      approvals.value = records.map(transformApproval)
      pagination.value = { current, size, total }
      return { success: true, data: approvals.value }
    } catch (error) {
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
      approvals.value = records.map(transformApproval)
      pagination.value = { current, size, total }
      return { success: true, data: approvals.value }
    } catch (error) {
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
      approvals.value = records.map(transformApproval)
      pagination.value = { current, size, total }
      return { success: true, data: approvals.value }
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

  return {
    approvals,
    currentApproval,
    approvalHistory,
    pagination,
    pendingApprovals,
    approvedApprovals,
    rejectedApprovals,
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
    fetchApprovalHistory
  }
})
