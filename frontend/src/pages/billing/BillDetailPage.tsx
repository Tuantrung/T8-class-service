import { useParams, useNavigate, Link } from 'react-router-dom'
import { useBillDetail, useUpdateBillStatus } from '../../hooks/useBills'
import { useToast } from '../../components/ui/Toast'
import { PageSpinner } from '../../components/ui/Spinner'
import { StatusBadge } from '../../components/ui/Badge'
import { formatCurrency, formatDate, downloadBlob } from '../../lib/utils'
import { billsApi } from '../../api/endpoints/bills'
import type { BillStatus } from '../../api/types'

const statusLabels: Record<BillStatus, string> = {
  DRAFT: 'Nháp',
  ISSUED: 'Đã gửi',
  PAID: 'Đã thanh toán',
}

export default function BillDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { showToast } = useToast()

  const { data: bill, isLoading, isError } = useBillDetail(id!)
  const updateStatus = useUpdateBillStatus()

  if (isLoading) return <PageSpinner />

  if (isError || !bill) {
    return (
      <div className="p-6 bg-red-50 rounded-lg text-red-700 text-sm">
        Không tìm thấy hoá đơn.{' '}
        <button onClick={() => navigate('/billing')} className="underline">
          Quay lại
        </button>
      </div>
    )
  }

  const handleDownloadPdf = async () => {
    try {
      const blob = await billsApi.downloadPdf(bill.id)
      const studentName = bill.studentName ?? bill.studentId
      downloadBlob(blob, `hoc-phi-${studentName}-${bill.billingMonth}.pdf`)
    } catch {
      showToast('Không thể tải PDF', 'error')
    }
  }

  const handleStatusChange = async (status: BillStatus) => {
    try {
      await updateStatus.mutateAsync({ id: bill.id, status })
      showToast('Đã cập nhật trạng thái', 'success')
    } catch {
      showToast('Không thể cập nhật trạng thái', 'error')
    }
  }

  const sessions = bill.sessions ?? []

  return (
    <div className="max-w-3xl">
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 mb-4" aria-label="Breadcrumb">
        <Link to="/billing" className="hover:text-gray-700">
          Học phí
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900 font-medium">
          Chi tiết hoá đơn
        </span>
      </nav>

      {/* Header */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5 mb-4">
        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
          <div>
            <h1 className="text-xl font-bold text-gray-900">
              {bill.studentName ?? bill.studentId}
            </h1>
            <p className="text-sm text-gray-500 mt-1">
              {bill.className ?? bill.classId} &bull; Tháng {bill.billingMonth}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <StatusBadge status={bill.status} />
            <button
              onClick={handleDownloadPdf}
              className="inline-flex items-center border border-gray-300 text-gray-700 px-3 py-1.5 rounded-md text-sm hover:bg-gray-50 transition-colors"
            >
              Tải PDF
            </button>
          </div>
        </div>

        {/* Summary */}
        <div className="mt-4 pt-4 border-t border-gray-100">
          <div className="flex flex-wrap gap-6">
            <div>
              <p className="text-xs text-gray-500">Buổi có mặt</p>
              <p className="text-lg font-semibold text-gray-900">
                {bill.sessionsAttended} buổi
              </p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Học phí/buổi</p>
              <p className="text-lg font-semibold text-gray-900">
                {formatCurrency(bill.ratePerSession)}
              </p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Tổng học phí</p>
              <p className="text-xl font-bold text-blue-600">
                {formatCurrency(bill.totalAmount)}
              </p>
            </div>
          </div>
          <p className="text-xs text-gray-400 mt-2">
            {bill.sessionsAttended} buổi x {formatCurrency(bill.ratePerSession)}/buổi
            = {formatCurrency(bill.totalAmount)}
          </p>
        </div>
      </div>

      {/* Status Change */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 mb-4">
        <div className="flex items-center gap-3">
          <p className="text-sm font-medium text-gray-700">Cập nhật trạng thái:</p>
          <div className="flex gap-2">
            {(['DRAFT', 'ISSUED', 'PAID'] as BillStatus[]).map((s) => (
              <button
                key={s}
                onClick={() => handleStatusChange(s)}
                disabled={bill.status === s || updateStatus.isPending}
                className={`px-3 py-1.5 rounded text-xs font-medium transition-colors disabled:opacity-50 ${
                  bill.status === s
                    ? 'bg-blue-600 text-white'
                    : 'border border-gray-300 text-gray-600 hover:bg-gray-50'
                }`}
              >
                {statusLabels[s]}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Session Breakdown */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-x-auto">
        <div className="px-4 py-3 border-b border-gray-200">
          <h2 className="text-sm font-semibold text-gray-700">Chi tiết từng buổi học</h2>
        </div>
        <table className="w-full text-sm" aria-label="Chi tiết buổi học">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Ngày</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Chủ đề</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Có mặt</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Huỷ bởi GV</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Tính tiền</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {sessions.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                  Không có dữ liệu buổi học
                </td>
              </tr>
            ) : (
              sessions.map((s) => (
                <tr
                  key={s.sessionId}
                  className={`hover:bg-gray-50 ${s.countedInBill ? '' : 'opacity-50'}`}
                >
                  <td className="px-4 py-3 text-gray-900">{formatDate(s.sessionDate)}</td>
                  <td className="px-4 py-3 text-gray-600">{s.topic ?? '-'}</td>
                  <td className="px-4 py-3">
                    {s.attendanceStatus === 'PRESENT' ? (
                      <span className="text-green-600">Có mặt</span>
                    ) : s.attendanceStatus === 'LATE' ? (
                      <span className="text-yellow-600">Đi muộn</span>
                    ) : s.attendanceStatus === 'ABSENT' ? (
                      <span className="text-red-600">Vắng</span>
                    ) : (
                      <span className="text-gray-400">-</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    {s.cancelledByTeacher ? (
                      <span className="text-red-500">Có</span>
                    ) : (
                      <span className="text-gray-400">Không</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    {s.countedInBill ? (
                      <span className="text-green-600 font-medium">
                        {formatCurrency(bill.ratePerSession)}
                      </span>
                    ) : (
                      <span className="text-gray-400">Không tính</span>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
          {sessions.length > 0 && (
            <tfoot className="border-t-2 border-gray-200">
              <tr className="bg-gray-50">
                <td colSpan={4} className="px-4 py-3 text-right font-semibold text-gray-700">
                  Tổng cộng:
                </td>
                <td className="px-4 py-3 font-bold text-blue-600">
                  {formatCurrency(bill.totalAmount)}
                </td>
              </tr>
            </tfoot>
          )}
        </table>
      </div>
    </div>
  )
}
