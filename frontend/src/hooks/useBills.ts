import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { billsApi } from '../api/endpoints/bills'
import type { BillStatus, GenerateBillsRequest } from '../api/types'

export function useBills(params?: {
  classId?: string
  month?: string
  status?: string
  page?: number
  size?: number
}) {
  return useQuery({
    queryKey: ['bills', params],
    queryFn: () => billsApi.list(params),
    enabled: !!params?.month,
  })
}

export function useBillDetail(id: string) {
  return useQuery({
    queryKey: ['bill', id],
    queryFn: () => billsApi.get(id),
    enabled: !!id,
  })
}

export function useGenerateBills() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: GenerateBillsRequest) => billsApi.generate(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['bills'] }),
  })
}

export function useUpdateBillStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: BillStatus }) =>
      billsApi.updateStatus(id, status),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: ['bills'] })
      qc.invalidateQueries({ queryKey: ['bill', vars.id] })
    },
  })
}
