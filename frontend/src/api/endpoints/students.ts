import apiClient from '../client'
import type { ApiResponse, CreateStudentRequest, ImportResult, PageResponse, StudentDto } from '../types'

export const studentsApi = {
  list: (page = 0, size = 20) =>
    apiClient.get<PageResponse<StudentDto>>('/api/students', { params: { page, size } }).then((r) => r.data),

  get: (id: string) =>
    apiClient.get<ApiResponse<StudentDto>>(`/api/students/${id}`).then((r) => r.data.data),

  create: (data: CreateStudentRequest) =>
    apiClient.post<ApiResponse<StudentDto>>('/api/students', data).then((r) => r.data.data),

  update: (id: string, data: Partial<CreateStudentRequest>) =>
    apiClient.put<ApiResponse<StudentDto>>(`/api/students/${id}`, data).then((r) => r.data.data),

  delete: (id: string) =>
    apiClient.delete(`/api/students/${id}`),

  importExcel: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return apiClient
      .post<ApiResponse<ImportResult>>('/api/students/import', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data.data)
  },

  downloadTemplate: () =>
    apiClient.get('/api/students/import-template', { responseType: 'blob' }).then((r) => r.data),
}
