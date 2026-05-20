import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { studentsApi } from '../api/endpoints/students'
import { classesApi } from '../api/endpoints/classes'
import type { CreateStudentRequest } from '../api/types'

export function useStudents(params?: { page?: number; size?: number; search?: string }) {
  return useQuery({
    queryKey: ['students', params],
    queryFn: () => studentsApi.list({ page: params?.page, size: params?.size, q: params?.search }),
  })
}

export function useStudent(id: string) {
  return useQuery({
    queryKey: ['student', id],
    queryFn: () => studentsApi.get(id),
    enabled: !!id,
  })
}

export function useStudentsByClass(classId: string) {
  return useQuery({
    queryKey: ['class-students', classId],
    queryFn: () => classesApi.getStudents(classId),
    enabled: !!classId,
  })
}

export function useCreateStudent() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateStudentRequest) => studentsApi.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['students'] }),
  })
}

export function useUpdateStudent() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<CreateStudentRequest> }) =>
      studentsApi.update(id, data),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: ['students'] })
      qc.invalidateQueries({ queryKey: ['student', vars.id] })
    },
  })
}

export function useImportStudents(classId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => studentsApi.importExcel(classId, file),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['students'] })
      qc.invalidateQueries({ queryKey: ['class-students', classId] })
    },
  })
}

export function useRemoveStudentFromClass(classId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (studentId: string) => classesApi.removeStudent(classId, studentId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['class-students', classId] }),
  })
}

export function useDeleteStudent() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => studentsApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['students'] }),
  })
}
