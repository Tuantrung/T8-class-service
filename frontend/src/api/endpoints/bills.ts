import apiClient from '../client'
import type { ApiResponse, BillDto, GenerateBillsRequest, GenerateBillsResult, PageResponse } from '../types'

export const billsApi = {
  list: (page = 0, size = 20) =>
    apiClient.get<PageResponse<BillDto>>('/api/bills', { params: { page, size } }).then((r) => r.data),

  get: (id: string) =>
    apiClient.get<ApiResponse<BillDto>>(`/api/bills/${id}`).then((r) => r.data.data),

  generate: (data: GenerateBillsRequest) =>
    apiClient
      .post<ApiResponse<GenerateBillsResult>>('/api/bills/generate', data)
      .then((r) => r.data.data),

  updateStatus: (id: string, status: string) =>
    apiClient.put<ApiResponse<BillDto>>(`/api/bills/${id}/status`, { status }).then((r) => r.data.data),

  downloadPdf: (id: string) =>
    apiClient.get(`/api/bills/${id}/pdf`, { responseType: 'blob' }).then((r) => r.data),

  exportZip: (month: string) =>
    apiClient.get('/api/bills/export', { params: { month }, responseType: 'blob' }).then((r) => r.data),
}
