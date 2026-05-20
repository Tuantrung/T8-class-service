import { useMutation, useQueryClient } from '@tanstack/react-query'
import { commentsApi } from '../api/endpoints/comments'
import type { CommentDto } from '../api/types'

export function useSaveComment(sessionId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({
      studentId,
      body,
      commentId,
    }: {
      studentId: string
      body: string
      commentId?: string
    }): Promise<CommentDto> => {
      if (commentId) {
        return commentsApi.update(commentId, body)
      }
      // Try to create; if 409, fetch existing and update
      try {
        return await commentsApi.create(sessionId, studentId, body)
      } catch (err: unknown) {
        const axiosErr = err as { response?: { status?: number } }
        if (axiosErr?.response?.status === 409) {
          // Comment already exists, fetch its ID and update
          const existing = await commentsApi.getBySession(sessionId, studentId)
          if (existing && existing.length > 0) {
            return commentsApi.update(existing[0].id, body)
          }
        }
        throw err
      }
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['session', sessionId] })
    },
  })
}
