import apiClient from './config'

export const formTemplateApi = {
  getList: (params) => apiClient.get('/form-templates', { params }),
  getAll: () => apiClient.get('/form-templates/all'),
  getById: (id) => apiClient.get(`/form-templates/${id}`),
  getByCode: (code) => apiClient.get(`/form-templates/code/${code}`),
  create: (data) => apiClient.post('/form-templates', data),
  update: (id, data) => apiClient.post(`/form-templates/${id}/update`, data),
  delete: (id) => apiClient.post(`/form-templates/${id}/delete`)
}
