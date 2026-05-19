// TODO Track 5: Implement full TanStack Query hooks for sessions
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { sessionsApi } from '../api/endpoints/sessions'
import type { CreateSessionRequest } from '../api/types'

export function useSessions(classId: string) {
  return useQuery({
    queryKey: ['sessions', classId],
    queryFn: () => sessionsApi.listByClass(classId),
    enabled: !!classId,
  })
}

export function useSession(id: string) {
  return useQuery({
    queryKey: ['session', id],
    queryFn: () => sessionsApi.get(id),
    enabled: !!id,
  })
}

export function useCreateSession() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateSessionRequest) => sessionsApi.create(data),
    onSuccess: (_, vars) => qc.invalidateQueries({ queryKey: ['sessions', vars.classId] }),
  })
}
