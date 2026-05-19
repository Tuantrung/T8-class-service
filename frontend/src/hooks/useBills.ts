// TODO Track 6: Implement billing hooks
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { billsApi } from '../api/endpoints/bills'
import type { GenerateBillsRequest } from '../api/types'

export function useBills(page = 0, size = 20) {
  return useQuery({
    queryKey: ['bills', { page, size }],
    queryFn: () => billsApi.list(page, size),
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
    mutationFn: ({ id, status }: { id: string; status: string }) => billsApi.updateStatus(id, status),
    onSuccess: (_, vars) => qc.invalidateQueries({ queryKey: ['bill', vars.id] }),
  })
}
