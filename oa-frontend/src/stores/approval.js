import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useApprovalStore = defineStore('approval', () => {
  const approvals = ref([
    {
      id: 1,
      title: '请假申请 - 年假3天',
      type: 'leave',
      applicant: '李员工',
      applicantId: 3,
      department: '市场部',
      status: 'pending',
      priority: 'normal',
      createTime: '2024-01-15 09:30:00',
      content: '申请2024年1月20日至1月22日年假，共3天，用于家庭事务处理。',
      currentApprover: '张经理',
      history: [
        { approver: '李员工', action: 'submit', time: '2024-01-15 09:30:00', comment: '提交申请' }
      ]
    },
    {
      id: 2,
      title: '报销申请 - 差旅费用',
      type: 'expense',
      applicant: '王销售',
      applicantId: 4,
      department: '销售部',
      status: 'pending',
      priority: 'high',
      createTime: '2024-01-15 10:15:00',
      content: '出差北京客户拜访，交通费1200元，住宿费800元，餐饮费300元，合计2300元。',
      currentApprover: '张经理',
      history: [
        { approver: '王销售', action: 'submit', time: '2024-01-15 10:15:00', comment: '提交申请' }
      ]
    },
    {
      id: 3,
      title: '采购申请 - 办公设备',
      type: 'purchase',
      applicant: '赵行政',
      applicantId: 5,
      department: '行政部',
      status: 'approved',
      priority: 'normal',
      createTime: '2024-01-14 14:00:00',
      content: '采购笔记本电脑2台，打印机1台，预算合计25000元。',
      currentApprover: null,
      history: [
        { approver: '赵行政', action: 'submit', time: '2024-01-14 14:00:00', comment: '提交申请' },
        { approver: '张经理', action: 'approve', time: '2024-01-14 16:30:00', comment: '同意采购' },
        { approver: '系统管理员', action: 'approve', time: '2024-01-15 09:00:00', comment: '审批通过' }
      ]
    },
    {
      id: 4,
      title: '加班申请 - 项目上线',
      type: 'overtime',
      applicant: '李员工',
      applicantId: 3,
      department: '市场部',
      status: 'rejected',
      priority: 'low',
      createTime: '2024-01-13 11:00:00',
      content: '申请1月18日、19日加班，配合项目上线工作。',
      currentApprover: null,
      history: [
        { approver: '李员工', action: 'submit', time: '2024-01-13 11:00:00', comment: '提交申请' },
        { approver: '张经理', action: 'reject', time: '2024-01-13 15:00:00', comment: '项目已延期，暂不需要加班' }
      ]
    },
    {
      id: 5,
      title: '出差申请 - 上海客户拜访',
      type: 'travel',
      applicant: '王销售',
      applicantId: 4,
      department: '销售部',
      status: 'pending',
      priority: 'high',
      createTime: '2024-01-15 08:00:00',
      content: '计划1月22日至1月24日出差上海，拜访重要客户，预计费用5000元。',
      currentApprover: '张经理',
      history: [
        { approver: '王销售', action: 'submit', time: '2024-01-15 08:00:00', comment: '提交申请' }
      ]
    }
  ])

  const pendingApprovals = computed(() =>
    approvals.value.filter(a => a.status === 'pending')
  )

  const approvedApprovals = computed(() =>
    approvals.value.filter(a => a.status === 'approved')
  )

  const rejectedApprovals = computed(() =>
    approvals.value.filter(a => a.status === 'rejected')
  )

  function getApprovalById(id) {
    return approvals.value.find(a => a.id === parseInt(id))
  }

  function addApproval(approval) {
    const newApproval = {
      id: approvals.value.length + 1,
      ...approval,
      status: 'pending',
      createTime: new Date().toLocaleString('zh-CN'),
      history: [
        {
          approver: approval.applicant,
          action: 'submit',
          time: new Date().toLocaleString('zh-CN'),
          comment: '提交申请'
        }
      ]
    }
    approvals.value.unshift(newApproval)
    return newApproval
  }

  function approveApproval(id, comment, approver) {
    const approval = approvals.value.find(a => a.id === id)
    if (approval) {
      approval.status = 'approved'
      approval.currentApprover = null
      approval.history.push({
        approver,
        action: 'approve',
        time: new Date().toLocaleString('zh-CN'),
        comment
      })
    }
  }

  function rejectApproval(id, comment, approver) {
    const approval = approvals.value.find(a => a.id === id)
    if (approval) {
      approval.status = 'rejected'
      approval.currentApprover = null
      approval.history.push({
        approver,
        action: 'reject',
        time: new Date().toLocaleString('zh-CN'),
        comment
      })
    }
  }

  return {
    approvals,
    pendingApprovals,
    approvedApprovals,
    rejectedApprovals,
    getApprovalById,
    addApproval,
    approveApproval,
    rejectApproval
  }
})
