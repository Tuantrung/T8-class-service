import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useSessions, useCreateSession } from '../../../hooks/useSessions'
import { useToast } from '../../../components/ui/Toast'
import { Modal } from '../../../components/ui/Modal'
import { PageSpinner } from '../../../components/ui/Spinner'
import { formatDate } from '../../../lib/utils'

const sessionSchema = z.object({
  sessionDate: z.string().min(1, 'Ngày học không được để trống'),
  startTime: z.string().optional(),
  endTime: z.string().optional(),
  topic: z.string().optional(),
  cancelledByTeacher: z.boolean().optional(),
})

type SessionForm = z.infer<typeof sessionSchema>

interface Props {
  classId: string
}

export default function ClassSessionsTab({ classId }: Props) {
  const [modalOpen, setModalOpen] = useState(false)
  const { showToast } = useToast()

  const { data, isLoading, isError } = useSessions(classId)
  const createSession = useCreateSession()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<SessionForm>({
    resolver: zodResolver(sessionSchema),
    defaultValues: { cancelledByTeacher: false },
  })

  const onSubmit = async (data: SessionForm) => {
    try {
      await createSession.mutateAsync({
        classId,
        ...data,
        cancelledByTeacher: data.cancelledByTeacher ?? false,
      })
      showToast('Tạo buổi học thành công', 'success')
      setModalOpen(false)
      reset()
    } catch {
      showToast('Có lỗi xảy ra khi tạo buổi học', 'error')
    }
  }

  if (isLoading) return <PageSpinner />

  if (isError) {
    return (
      <div className="p-4 bg-red-50 rounded-lg text-red-700 text-sm">
        Không thể tải danh sách buổi học.
      </div>
    )
  }

  const sessions = data?.content ?? []

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <p className="text-sm text-gray-500">{sessions.length} buổi học</p>
        <button
          onClick={() => {
            reset({ cancelledByTeacher: false })
            setModalOpen(true)
          }}
          className="bg-blue-600 text-white px-3 py-1.5 rounded-md text-sm hover:bg-blue-700"
        >
          + Thêm buổi học
        </button>
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-x-auto">
        <table className="w-full text-sm" aria-label="Danh sách buổi học">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Ngày học</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Thời gian</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Chủ đề</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Điểm danh</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Trạng thái</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {sessions.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-10 text-center text-gray-400">
                  Chưa có buổi học nào
                </td>
              </tr>
            ) : (
              sessions.map((s) => (
                <tr key={s.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">{formatDate(s.sessionDate)}</td>
                  <td className="px-4 py-3 text-gray-600">
                    {s.startTime && s.endTime
                      ? `${s.startTime} - ${s.endTime}`
                      : s.startTime ?? '-'}
                  </td>
                  <td className="px-4 py-3 text-gray-600 max-w-xs truncate">{s.topic ?? '-'}</td>
                  <td className="px-4 py-3 text-gray-600">
                    {s.attendanceCount != null && s.totalStudents != null
                      ? `${s.attendanceCount}/${s.totalStudents}`
                      : '-'}
                  </td>
                  <td className="px-4 py-3">
                    {s.cancelledByTeacher ? (
                      <span className="px-2 py-0.5 rounded text-xs font-medium bg-red-100 text-red-700">
                        Đã huỷ
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-700">
                        Diễn ra
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <Link
                      to={`/sessions/${s.id}`}
                      className="text-blue-600 hover:underline text-xs font-medium"
                    >
                      Chi tiết
                    </Link>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Thêm buổi học mới"
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label htmlFor="sess-date" className="block text-sm font-medium text-gray-700 mb-1">
              Ngày học <span className="text-red-500">*</span>
            </label>
            <input
              id="sess-date"
              {...register('sessionDate')}
              type="date"
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.sessionDate && (
              <p className="text-red-500 text-xs mt-1">{errors.sessionDate.message}</p>
            )}
          </div>

          <div className="flex gap-3">
            <div className="flex-1">
              <label htmlFor="sess-start" className="block text-sm font-medium text-gray-700 mb-1">
                Giờ bắt đầu
              </label>
              <input
                id="sess-start"
                {...register('startTime')}
                type="time"
                className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div className="flex-1">
              <label htmlFor="sess-end" className="block text-sm font-medium text-gray-700 mb-1">
                Giờ kết thúc
              </label>
              <input
                id="sess-end"
                {...register('endTime')}
                type="time"
                className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div>
            <label htmlFor="sess-topic" className="block text-sm font-medium text-gray-700 mb-1">
              Chủ đề buổi học
            </label>
            <input
              id="sess-topic"
              {...register('topic')}
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex items-center gap-2">
            <input
              id="sess-cancelled"
              {...register('cancelledByTeacher')}
              type="checkbox"
              className="h-4 w-4 text-red-600 border-gray-300 rounded"
            />
            <label htmlFor="sess-cancelled" className="text-sm text-gray-700">
              Huỷ buổi học này (ảnh hưởng đến tính học phí)
            </label>
          </div>

          <div className="flex gap-2 justify-end pt-2">
            <button
              type="button"
              onClick={() => setModalOpen(false)}
              className="px-4 py-2 border border-gray-300 text-gray-700 rounded-md text-sm hover:bg-gray-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm hover:bg-blue-700 disabled:opacity-50"
            >
              {isSubmitting ? 'Đang tạo...' : 'Tạo buổi học'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
