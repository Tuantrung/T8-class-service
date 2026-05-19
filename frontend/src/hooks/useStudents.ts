// TODO Track 5: Implement full TanStack Query hooks for students
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { studentsApi } from '../api/endpoints/students'
import type { CreateStudentRequest } from '../api/types'

export function useStudents(page = 0, size = 20) {
  return useQuery({
    queryKey: ['students', { page, size }],
    queryFn: () => studentsApi.list(page, size),
  })
}

export function useStudent(id: string) {
  return useQuery({
    queryKey: ['student', id],
    queryFn: () => studentsApi.get(id),
    enabled: !!id,
  })
}

export function useCreateStudent() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateStudentRequest) => studentsApi.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['students'] }),
  })
}

export function useImportStudents() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => studentsApi.importExcel(file),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['students'] }),
  })
}
