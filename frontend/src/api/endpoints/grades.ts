import apiClient from '../client'
import type { ApiResponse, CreateGradeRequest, GradeDto, GradeImportResult, PageResponse } from '../types'

export const gradesApi = {
  list: (classId: string, params?: { studentId?: string; page?: number; size?: number }) =>
    apiClient
      .get<PageResponse<GradeDto>>('/api/grades', {
        params: { classId, page: 0, size: 500, ...params },
      })
      .then((r) => r.data),

  create: (data: CreateGradeRequest) =>
    apiClient.post<ApiResponse<GradeDto>>('/api/grades', data).then((r) => r.data.data),

  update: (id: string, data: Partial<CreateGradeRequest>) =>
    apiClient.put<ApiResponse<GradeDto>>(`/api/grades/${id}`, data).then((r) => r.data.data),

  delete: (id: string) => apiClient.delete(`/api/grades/${id}`),

  importGrades: (classId: string, examName: string, examDate: string | undefined, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return apiClient
      .post<ApiResponse<GradeImportResult>>('/api/grades/import', form, {
        params: { classId, examName, ...(examDate ? { examDate } : {}) },
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data.data)
  },

  downloadTemplate: (classId: string) =>
    apiClient
      .get<Blob>('/api/grades/import/template', {
        params: { classId },
        responseType: 'blob',
      })
      .then((r) => r.data),
}
