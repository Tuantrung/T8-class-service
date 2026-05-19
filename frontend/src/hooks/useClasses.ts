// TODO Track 5: Implement full TanStack Query hooks for classes
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { classesApi } from '../api/endpoints/classes'
import type { CreateClassRequest } from '../api/types'

export function useClasses(page = 0, size = 20) {
  return useQuery({
    queryKey: ['classes', { page, size }],
    queryFn: () => classesApi.list(page, size),
  })
}

export function useClass(id: string) {
  return useQuery({
    queryKey: ['class', id],
    queryFn: () => classesApi.get(id),
    enabled: !!id,
  })
}

export function useCreateClass() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateClassRequest) => classesApi.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['classes'] }),
  })
}

export function useDeleteClass() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => classesApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['classes'] }),
  })
}
