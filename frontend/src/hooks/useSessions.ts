import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { sessionsApi } from '../api/endpoints/sessions'
import type { CreateSessionRequest, UpdateSessionRequest } from '../api/types'

export function useSessions(classId: string, params?: { from?: string; to?: string; page?: number }) {
  return useQuery({
    queryKey: ['sessions', classId, params],
    queryFn: () => sessionsApi.listByClass(classId, params),
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
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: ['sessions', vars.classId] })
    },
  })
}

export function useUpdateSession() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateSessionRequest }) =>
      sessionsApi.update(id, data),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: ['session', vars.id] })
    },
  })
}

export function useDeleteSession() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => sessionsApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sessions'] }),
  })
}
