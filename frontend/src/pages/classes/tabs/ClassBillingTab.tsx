import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useBills, useGenerateBills } from '../../../hooks/useBills'
import { useToast } from '../../../components/ui/Toast'
import { PageSpinner } from '../../../components/ui/Spinner'
import { StatusBadge } from '../../../components/ui/Badge'
import { formatCurrency } from '../../../lib/utils'

function getCurrentMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

interface Props {
  classId: string
}

export default function ClassBillingTab({ classId }: Props) {
  const [month, setMonth] = useState(getCurrentMonth())
  const { showToast } = useToast()

  const { data, isLoading } = useBills({ classId, month })
  const generateBills = useGenerateBills()

  const handleGenerate = async () => {
    if (!month) return
    try {
      const result = await generateBills.mutateAsync({ classId, month })
      showToast(`Đã tạo ${result.generated} hoá đơn cho tháng ${month}`, 'success')
    } catch {
      showToast('Có lỗi khi tạo học phí', 'error')
    }
  }

  if (isLoading) return <PageSpinner />

  const bills = data?.content ?? []

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
        <div className="flex items-center gap-2">
          <label htmlFor="bill-month" className="text-sm text-gray-600">
            Tháng:
          </label>
          <input
            id="bill-month"
            type="month"
            value={month}
            onChange={(e) => setMonth(e.target.value)}
            className="border border-gray-300 rounded-md px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <button
          onClick={handleGenerate}
          disabled={generateBills.isPending || !month}
          className="bg-blue-600 text-white px-3 py-1.5 rounded-md text-sm hover:bg-blue-700 disabled:opacity-50"
        >
          {generateBills.isPending ? 'Đang tạo...' : 'Tạo học phí tháng này'}
        </button>
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-x-auto">
        <table className="w-full text-sm" aria-label="Danh sách học phí">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Học sinh</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Tháng</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Số buổi</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Học phí</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Trạng thái</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {bills.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-10 text-center text-gray-400">
                  Chưa có học phí. Nhấn "Tạo học phí" để tạo.
                </td>
              </tr>
            ) : (
              bills.map((bill) => (
                <tr key={bill.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">
                    {bill.studentName ?? bill.studentId}
                  </td>
                  <td className="px-4 py-3 text-gray-600">{bill.billingMonth}</td>
                  <td className="px-4 py-3 text-gray-600">
                    {bill.sessionsAttended}/{bill.sessionsTotal}
                  </td>
                  <td className="px-4 py-3 font-medium">{formatCurrency(bill.totalAmount)}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={bill.status} />
                  </td>
                  <td className="px-4 py-3">
                    <Link
                      to={`/billing/${bill.id}`}
                      className="text-blue-600 hover:underline text-xs font-medium"
                    >
                      Xem
                    </Link>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
