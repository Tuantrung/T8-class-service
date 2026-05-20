import apiClient from '../client'
import type { ApiResponse, CreateGradeRequest, GradeDto, PageResponse } from '../types'

export const gradesApi = {
  list: (classId: string, params?: { studentId?: string; page?: number; size?: number }) =>
    apiClient
      .get<PageResponse<GradeDto>>('/api/grades', {
        params: { classId, page: 0, size: 100, ...params },
      })
      .then((r) => r.data),

  create: (data: CreateGradeRequest) =>
    apiClient.post<ApiResponse<GradeDto>>('/api/grades', data).then((r) => r.data.data),

  update: (id: string, data: Partial<CreateGradeRequest>) =>
    apiClient.put<ApiResponse<GradeDto>>(`/api/grades/${id}`, data).then((r) => r.data.data),

  delete: (id: string) => apiClient.delete(`/api/grades/${id}`),
}
