import { useMutation, useQueryClient } from '@tanstack/react-query'
import { attendanceApi } from '../api/endpoints/attendance'
import type { BulkAttendanceRequest } from '../api/types'

export function useSaveAttendance() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: BulkAttendanceRequest) => attendanceApi.saveBulk(data),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: ['session', vars.sessionId] })
    },
  })
}
