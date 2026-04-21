import apiClient from './config'

export const approverRuleApi = {
  getList: (params) => apiClient.get('/approver-rules', { params }),
  getById: (id) => apiClient.get(`/approver-rules/${id}`),
  create: (data) => apiClient.post('/approver-rules', data),
  update: (id, data) => apiClient.put(`/approver-rules/${id}`, data),
  delete: (id) => apiClient.delete(`/approver-rules/${id}`),
  preview: (data) => apiClient.post('/approver-rules/preview', data)
}
