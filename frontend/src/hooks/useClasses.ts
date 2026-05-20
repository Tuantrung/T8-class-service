import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { classesApi } from '../api/endpoints/classes'
import type { CreateClassRequest, UpdateClassRequest } from '../api/types'

export function useClasses(params?: { search?: string; page?: number; size?: number }) {
  return useQuery({
    queryKey: ['classes', params],
    queryFn: () => classesApi.list({ q: params?.search, page: params?.page, size: params?.size }),
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

export function useUpdateClass() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateClassRequest }) =>
      classesApi.update(id, data),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: ['classes'] })
      qc.invalidateQueries({ queryKey: ['class', vars.id] })
    },
  })
}

export function useDeactivateClass() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => classesApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['classes'] }),
  })
}

export function useClassStudents(classId: string) {
  return useQuery({
    queryKey: ['class-students', classId],
    queryFn: () => classesApi.getStudents(classId),
    enabled: !!classId,
  })
}

export function useEnrollStudents(classId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (studentIds: string[]) => classesApi.enrollStudents(classId, studentIds),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['class-students', classId] }),
  })
}

export function useRemoveStudentFromClass(classId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (studentId: string) => classesApi.removeStudent(classId, studentId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['class-students', classId] }),
  })
}
