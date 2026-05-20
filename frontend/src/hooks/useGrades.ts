import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { gradesApi } from '../api/endpoints/grades'
import type { CreateGradeRequest } from '../api/types'

export function useGrades(classId: string, studentId?: string) {
  return useQuery({
    queryKey: ['grades', classId, studentId],
    queryFn: () => gradesApi.list(classId, { studentId }),
    enabled: !!classId,
  })
}

export function useCreateGrade() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateGradeRequest) => gradesApi.create(data),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: ['grades', vars.classId] })
    },
  })
}

export function useUpdateGrade(classId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<CreateGradeRequest> }) =>
      gradesApi.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['grades', classId] })
    },
  })
}

export function useDeleteGrade(classId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => gradesApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['grades', classId] })
    },
  })
}
