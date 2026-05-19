import apiClient from '../client'
import type { ApiResponse, AttendanceDto, BulkAttendanceRequest } from '../types'

export const attendanceApi = {
  getBySession: (sessionId: string) =>
    apiClient
      .get<ApiResponse<AttendanceDto[]>>(`/api/sessions/${sessionId}/attendance`)
      .then((r) => r.data.data),

  saveBulk: (sessionId: string, data: BulkAttendanceRequest) =>
    apiClient
      .post<ApiResponse<AttendanceDto[]>>(`/api/sessions/${sessionId}/attendance`, data)
      .then((r) => r.data.data),
}
