import apiClient from '../client'
import type { ApiResponse, CommentDto } from '../types'

export const commentsApi = {
  getBySession: (sessionId: string) =>
    apiClient
      .get<ApiResponse<CommentDto[]>>(`/api/sessions/${sessionId}/comments`)
      .then((r) => r.data.data),

  upsert: (sessionId: string, studentId: string, body: string) =>
    apiClient
      .put<ApiResponse<CommentDto>>(`/api/sessions/${sessionId}/comments/${studentId}`, { body })
      .then((r) => r.data.data),

  delete: (sessionId: string, commentId: string) =>
    apiClient.delete(`/api/sessions/${sessionId}/comments/${commentId}`),
}
