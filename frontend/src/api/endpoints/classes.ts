import apiClient from '../client'
import type {
  ApiResponse,
  ClassDto,
  CreateClassRequest,
  UpdateClassRequest,
  PageResponse,
  StudentDto,
} from '../types'

export const classesApi = {
  list: (params?: { page?: number; size?: number; q?: string; status?: string }) =>
    apiClient
      .get<PageResponse<ClassDto>>('/api/classes', {
        params: { page: 0, size: 20, ...params },
      })
      .then((r) => r.data),

  get: (id: string) =>
    apiClient.get<ApiResponse<ClassDto>>(`/api/classes/${id}`).then((r) => r.data.data),

  create: (data: CreateClassRequest) =>
    apiClient.post<ApiResponse<ClassDto>>('/api/classes', data).then((r) => r.data.data),

  update: (id: string, data: UpdateClassRequest) =>
    apiClient.put<ApiResponse<ClassDto>>(`/api/classes/${id}`, data).then((r) => r.data.data),

  delete: (id: string) => apiClient.delete(`/api/classes/${id}`),

  getStudents: (classId: string) =>
    apiClient
      .get<ApiResponse<StudentDto[]>>(`/api/classes/${classId}/students`)
      .then((r) => r.data.data),

  enrollStudents: (classId: string, studentIds: string[]) =>
    apiClient
      .post(`/api/classes/${classId}/students`, { studentIds })
      .then((r) => r.data),

  removeStudent: (classId: string, studentId: string) =>
    apiClient.delete(`/api/classes/${classId}/students/${studentId}`),
}
