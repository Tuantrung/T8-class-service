import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useBills, useGenerateBills, useUpdateBillStatus } from '../../hooks/useBills'
import { useClasses } from '../../hooks/useClasses'
import { useToast } from '../../components/ui/Toast'
import { Modal } from '../../components/ui/Modal'
import { PageSpinner } from '../../components/ui/Spinner'
import { StatusBadge } from '../../components/ui/Badge'
import { formatCurrency, downloadBlob } from '../../lib/utils'
import { billsApi } from '../../api/endpoints/bills'
import type { BillStatus } from '../../api/types'

function getCurrentMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

const generateSchema = z.object({
  classId: z.string().min(1, 'Vui lòng chọn lớp'),
  month: z.string().min(1, 'Vui lòng chọn tháng'),
})

type GenerateForm = z.infer<typeof generateSchema>

export default function BillingPage() {
  const [month, setMonth] = useState(getCurrentMonth())
  const [classId, setClassId] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [generateModalOpen, setGenerateModalOpen] = useState(false)
  const { showToast } = useToast()

  const { data, isLoading } = useBills({
    month: month || undefined,
    classId: classId || undefined,
    status: statusFilter || undefined,
  })

  const { data: classesData } = useClasses({ size: 100 })
  const classes = classesData?.content ?? []

  const generateBills = useGenerateBills()
  const updateStatus = useUpdateBillStatus()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<GenerateForm>({
    resolver: zodResolver(generateSchema),
    defaultValues: { month: getCurrentMonth() },
  })

  const onGenerate = async (data: GenerateForm) => {
    try {
      const result = await generateBills.mutateAsync(data)
      showToast(`Đã tạo ${result.generated} hoá đơn cho tháng ${data.month}`, 'success')
      setGenerateModalOpen(false)
      reset({ month: getCurrentMonth() })
    } catch {
      showToast('Có lỗi khi tạo học phí', 'error')
    }
  }

  const handleStatusChange = async (billId: string, status: BillStatus) => {
    try {
      await updateStatus.mutateAsync({ id: billId, status })
      showToast('Đã cập nhật trạng thái', 'success')
    } catch {
      showToast('Không thể cập nhật trạng thái', 'error')
    }
  }

  const handleDownloadPdf = async (billId: string, studentName: string, billingMonth: string) => {
    try {
      const blob = await billsApi.downloadPdf(billId)
      downloadBlob(blob, `hoc-phi-${studentName}-${billingMonth}.pdf`)
    } catch {
      showToast('Không thể tải PDF', 'error')
    }
  }

  if (isLoading) return <PageSpinner />

  const bills = data?.content ?? []

  const statusLabels: Record<BillStatus, string> = {
    DRAFT: 'Nháp',
    ISSUED: 'Đã gửi',
    PAID: 'Đã thanh toán',
  }

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Học phí</h1>
        <button
          onClick={() => {
            reset({ month: getCurrentMonth() })
            setGenerateModalOpen(true)
          }}
          className="inline-flex items-center bg-blue-600 text-white px-4 py-2 rounded-md text-sm font-medium hover:bg-blue-700 transition-colors"
        >
          + Tạo học phí
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3 mb-4">
        <div>
          <label htmlFor="bill-month" className="sr-only">Tháng</label>
          <input
            id="bill-month"
            type="month"
            value={month}
            onChange={(e) => setMonth(e.target.value)}
            className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label htmlFor="bill-class" className="sr-only">Lớp học</label>
          <select
            id="bill-class"
            value={classId}
            onChange={(e) => setClassId(e.target.value)}
            className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Tất cả lớp</option>
            {classes.map((cls) => (
              <option key={cls.id} value={cls.id}>
                {cls.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="bill-status" className="sr-only">Trạng thái</label>
          <select
            id="bill-status"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="DRAFT">Nháp</option>
            <option value="ISSUED">Đã gửi</option>
            <option value="PAID">Đã thanh toán</option>
          </select>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-x-auto">
        <table className="w-full text-sm" aria-label="Danh sách học phí">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Học sinh</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Lớp</th>
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
                <td colSpan={7} className="px-4 py-10 text-center text-gray-400">
                  {month
                    ? 'Không có học phí nào cho tháng này'
                    : 'Vui lòng chọn tháng để xem học phí'}
                </td>
              </tr>
            ) : (
              bills.map((bill) => (
                <tr key={bill.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">
                    {bill.studentName ?? bill.studentId}
                  </td>
                  <td className="px-4 py-3 text-gray-600">{bill.className ?? bill.classId}</td>
                  <td className="px-4 py-3 text-gray-600">{bill.billingMonth}</td>
                  <td className="px-4 py-3 text-gray-600">
                    {bill.sessionsAttended}/{bill.sessionsTotal}
                  </td>
                  <td className="px-4 py-3 font-medium">{formatCurrency(bill.totalAmount)}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={bill.status} />
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2 items-center">
                      <Link
                        to={`/billing/${bill.id}`}
                        className="text-blue-600 hover:underline text-xs font-medium"
                      >
                        Xem
                      </Link>
                      <button
                        onClick={() =>
                          handleDownloadPdf(
                            bill.id,
                            bill.studentName ?? bill.studentId,
                            bill.billingMonth
                          )
                        }
                        className="text-gray-600 hover:text-gray-800 text-xs font-medium"
                        aria-label="Tải PDF"
                      >
                        PDF
                      </button>
                      <select
                        value={bill.status}
                        onChange={(e) =>
                          handleStatusChange(bill.id, e.target.value as BillStatus)
                        }
                        className="text-xs border border-gray-200 rounded px-1 py-0.5 focus:outline-none focus:ring-1 focus:ring-blue-400"
                        aria-label={`Trạng thái hoá đơn ${bill.studentName}`}
                      >
                        {(['DRAFT', 'ISSUED', 'PAID'] as BillStatus[]).map((s) => (
                          <option key={s} value={s}>
                            {statusLabels[s]}
                          </option>
                        ))}
                      </select>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <p className="mt-3 text-sm text-gray-500 text-right">
          {data.totalElements} hoá đơn
        </p>
      )}

      {/* Generate Bills Modal */}
      <Modal
        open={generateModalOpen}
        onClose={() => setGenerateModalOpen(false)}
        title="Tạo học phí"
      >
        <form onSubmit={handleSubmit(onGenerate)} className="space-y-4">
          <div>
            <label htmlFor="gen-class" className="block text-sm font-medium text-gray-700 mb-1">
              Lớp học <span className="text-red-500">*</span>
            </label>
            <select
              id="gen-class"
              {...register('classId')}
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">-- Chọn lớp --</option>
              {classes.map((cls) => (
                <option key={cls.id} value={cls.id}>
                  {cls.name}
                </option>
              ))}
            </select>
            {errors.classId && (
              <p className="text-red-500 text-xs mt-1">{errors.classId.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="gen-month" className="block text-sm font-medium text-gray-700 mb-1">
              Tháng <span className="text-red-500">*</span>
            </label>
            <input
              id="gen-month"
              {...register('month')}
              type="month"
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.month && (
              <p className="text-red-500 text-xs mt-1">{errors.month.message}</p>
            )}
          </div>

          <p className="text-xs text-gray-500 bg-blue-50 p-3 rounded">
            Học phí sẽ được tính dựa trên số buổi có mặt (không tính buổi huỷ).
            Các hoá đơn đã gửi/đã thanh toán sẽ không bị ghi đè.
          </p>

          <div className="flex gap-2 justify-end pt-2">
            <button
              type="button"
              onClick={() => setGenerateModalOpen(false)}
              className="px-4 py-2 border border-gray-300 text-gray-700 rounded-md text-sm hover:bg-gray-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm hover:bg-blue-700 disabled:opacity-50"
            >
              {isSubmitting ? 'Đang tạo...' : 'Tạo học phí'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
