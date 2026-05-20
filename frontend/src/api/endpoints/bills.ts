import apiClient from '../client'
import type {
  ApiResponse,
  BillDetailDto,
  BillDto,
  BillStatus,
  GenerateBillsRequest,
  GenerateBillsResult,
  PageResponse,
} from '../types'

export const billsApi = {
  list: (params?: {
    month?: string
    classId?: string
    studentId?: string
    status?: string
    page?: number
    size?: number
  }) =>
    apiClient
      .get<PageResponse<BillDto>>('/api/bills', { params: { page: 0, size: 20, ...params } })
      .then((r) => r.data),

  get: (id: string) =>
    apiClient.get<ApiResponse<BillDetailDto>>(`/api/bills/${id}`).then((r) => r.data.data),

  generate: (data: GenerateBillsRequest) =>
    apiClient
      .post<ApiResponse<GenerateBillsResult>>('/api/bills/generate', data)
      .then((r) => r.data.data),

  updateStatus: (id: string, status: BillStatus) =>
    apiClient
      .patch<ApiResponse<BillDto>>(`/api/bills/${id}/status`, { status })
      .then((r) => r.data.data),

  downloadPdf: (id: string) =>
    apiClient.get(`/api/bills/${id}/pdf`, { responseType: 'blob' }).then((r) => r.data as Blob),

  exportZip: (month: string, classId?: string) =>
    apiClient
      .get('/api/bills/export', { params: { month, classId }, responseType: 'blob' })
      .then((r) => r.data as Blob),
}
