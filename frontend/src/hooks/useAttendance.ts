// TODO Track 5: Implement attendance hooks
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { attendanceApi } from '../api/endpoints/attendance'
import type { BulkAttendanceRequest } from '../api/types'

export function useAttendance(sessionId: string) {
  return useQuery({
    queryKey: ['attendance', sessionId],
    queryFn: () => attendanceApi.getBySession(sessionId),
    enabled: !!sessionId,
  })
}

export function useSaveAttendance(sessionId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: BulkAttendanceRequest) => attendanceApi.saveBulk(sessionId, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['attendance', sessionId] }),
  })
}
