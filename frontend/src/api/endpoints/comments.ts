import apiClient from '../client'
import type { ApiResponse, CommentDto } from '../types'

export const commentsApi = {
  getBySession: (sessionId: string, studentId?: string) =>
    apiClient
      .get<ApiResponse<CommentDto[]>>('/api/comments', {
        params: { sessionId, studentId },
      })
      .then((r) => r.data.data),

  create: (sessionId: string, studentId: string, body: string) =>
    apiClient
      .post<ApiResponse<CommentDto>>('/api/comments', { sessionId, studentId, body })
      .then((r) => r.data.data),

  update: (commentId: string, body: string) =>
    apiClient
      .put<ApiResponse<CommentDto>>(`/api/comments/${commentId}`, { body })
      .then((r) => r.data.data),

  delete: (commentId: string) => apiClient.delete(`/api/comments/${commentId}`),
}
