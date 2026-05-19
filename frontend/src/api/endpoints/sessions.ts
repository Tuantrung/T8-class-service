import apiClient from '../client'
import type { ApiResponse, CreateSessionRequest, SessionDto } from '../types'

export const sessionsApi = {
  listByClass: (classId: string) =>
    apiClient.get<ApiResponse<SessionDto[]>>('/api/sessions', { params: { classId } }).then((r) => r.data.data),

  get: (id: string) =>
    apiClient.get<ApiResponse<SessionDto>>(`/api/sessions/${id}`).then((r) => r.data.data),

  create: (data: CreateSessionRequest) =>
    apiClient.post<ApiResponse<SessionDto>>('/api/sessions', data).then((r) => r.data.data),

  update: (id: string, data: Partial<CreateSessionRequest> & { cancelledByTeacher?: boolean }) =>
    apiClient.put<ApiResponse<SessionDto>>(`/api/sessions/${id}`, data).then((r) => r.data.data),

  delete: (id: string) =>
    apiClient.delete(`/api/sessions/${id}`),
}
