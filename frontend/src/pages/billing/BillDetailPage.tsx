// TODO Track 6: Implement bill detail with session breakdown, PDF download, status management
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { billsApi } from '../../api/endpoints/bills'
import { formatCurrency, downloadBlob } from '../../lib/utils'

export default function BillDetailPage() {
  const { id } = useParams<{ id: string }>()

  const { data: bill, isLoading } = useQuery({
    queryKey: ['bill', id],
    queryFn: () => billsApi.get(id!),
    enabled: !!id,
  })

  if (isLoading) return <p className="text-gray-500">Loading...</p>
  if (!bill) return <p className="text-red-500">Bill not found</p>

  const handleDownloadPdf = async () => {
    const blob = await billsApi.downloadPdf(bill.id)
    downloadBlob(blob, `bill-${bill.billingMonth}-${bill.studentId}.pdf`)
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Bill Detail</h1>
        <button
          onClick={handleDownloadPdf}
          className="bg-white border border-gray-300 text-gray-700 px-4 py-2 rounded-md text-sm hover:bg-gray-50"
        >
          Download PDF
        </button>
      </div>

      <div className="grid grid-cols-2 gap-4 mb-6">
        <InfoItem label="Billing Month" value={bill.billingMonth} />
        <InfoItem label="Status" value={bill.status} />
        <InfoItem label="Sessions Attended" value={`${bill.sessionsAttended} / ${bill.sessionsTotal}`} />
        <InfoItem label="Total Amount" value={formatCurrency(bill.totalAmount)} />
      </div>

      <p className="text-gray-400 text-sm">
        TODO: Add per-session breakdown table and status change controls in Track 6.
      </p>
    </div>
  )
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-4">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="text-sm font-medium text-gray-900 mt-1">{value}</p>
    </div>
  )
}
