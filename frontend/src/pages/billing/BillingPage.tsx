// TODO Track 6: Implement billing page with month/class filter and generate bills action
import { useQuery } from '@tanstack/react-query'
import { billsApi } from '../../api/endpoints/bills'
import { Link } from 'react-router-dom'
import { formatCurrency } from '../../lib/utils'

export default function BillingPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['bills'],
    queryFn: () => billsApi.list(),
  })

  if (isLoading) return <p className="text-gray-500">Loading...</p>

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Billing</h1>
        <button className="bg-blue-600 text-white px-4 py-2 rounded-md text-sm hover:bg-blue-700">
          Generate Bills
        </button>
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Student</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Month</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Attended</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Total</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Status</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {data?.content.map((bill) => (
              <tr key={bill.id} className="hover:bg-gray-50">
                <td className="px-4 py-3 text-gray-900">{bill.studentId}</td>
                <td className="px-4 py-3 text-gray-600">{bill.billingMonth}</td>
                <td className="px-4 py-3 text-gray-600">{bill.sessionsAttended}/{bill.sessionsTotal}</td>
                <td className="px-4 py-3 font-medium">{formatCurrency(bill.totalAmount)}</td>
                <td className="px-4 py-3">
                  <span className="px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-600">
                    {bill.status}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <Link to={`/billing/${bill.id}`} className="text-blue-600 hover:underline text-xs">
                    View
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
