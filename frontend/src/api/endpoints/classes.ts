import apiClient from '../client'
import type { ApiResponse, ClassDto, CreateClassRequest, PageResponse } from '../types'

export const classesApi = {
  list: (page = 0, size = 20) =>
    apiClient.get<PageResponse<ClassDto>>('/api/classes', { params: { page, size } }).then((r) => r.data),

  get: (id: string) =>
    apiClient.get<ApiResponse<ClassDto>>(`/api/classes/${id}`).then((r) => r.data.data),

  create: (data: CreateClassRequest) =>
    apiClient.post<ApiResponse<ClassDto>>('/api/classes', data).then((r) => r.data.data),

  update: (id: string, data: Partial<CreateClassRequest> & { status?: string }) =>
    apiClient.put<ApiResponse<ClassDto>>(`/api/classes/${id}`, data).then((r) => r.data.data),

  delete: (id: string) =>
    apiClient.delete(`/api/classes/${id}`),

  enrollStudents: (classId: string, studentIds: string[]) =>
    apiClient.post(`/api/classes/${classId}/students`, { studentIds }),
}
