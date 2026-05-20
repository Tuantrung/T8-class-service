import apiClient from '../client'
import type { ApiResponse, AttendanceDto, BulkAttendanceRequest } from '../types'

export const attendanceApi = {
  getBySession: (sessionId: string) =>
    apiClient
      .get<ApiResponse<AttendanceDto[]>>('/api/attendance', { params: { sessionId } })
      .then((r) => r.data.data),

  saveBulk: (data: BulkAttendanceRequest) =>
    apiClient
      .post<ApiResponse<{ saved: number; sessionId: string }>>('/api/attendance/bulk', data)
      .then((r) => r.data.data),
}
