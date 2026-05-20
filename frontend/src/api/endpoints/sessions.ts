import apiClient from '../client'
import type {
  ApiResponse,
  CreateSessionRequest,
  PageResponse,
  SessionDetailDto,
  SessionDto,
  UpdateSessionRequest,
} from '../types'

export const sessionsApi = {
  listByClass: (classId: string, params?: { from?: string; to?: string; page?: number; size?: number }) =>
    apiClient
      .get<PageResponse<SessionDto>>('/api/sessions', {
        params: { classId, page: 0, size: 30, ...params },
      })
      .then((r) => r.data),

  get: (id: string) =>
    apiClient
      .get<ApiResponse<SessionDetailDto>>(`/api/sessions/${id}`)
      .then((r) => r.data.data),

  create: (data: CreateSessionRequest) =>
    apiClient.post<ApiResponse<SessionDto>>('/api/sessions', data).then((r) => r.data.data),

  update: (id: string, data: UpdateSessionRequest) =>
    apiClient.put<ApiResponse<SessionDto>>(`/api/sessions/${id}`, data).then((r) => r.data.data),

  delete: (id: string) => apiClient.delete(`/api/sessions/${id}`),
}
