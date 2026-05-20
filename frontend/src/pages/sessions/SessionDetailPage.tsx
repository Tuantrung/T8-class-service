import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useSession, useUpdateSession } from '../../hooks/useSessions'
import { useSaveAttendance } from '../../hooks/useAttendance'
import { useSaveComment } from '../../hooks/useComments'
import { useToast } from '../../components/ui/Toast'
import { PageSpinner } from '../../components/ui/Spinner'
import { formatDate } from '../../lib/utils'
import type { AttendanceStatus } from '../../api/types'

const sessionSchema = z.object({
  sessionDate: z.string().min(1, 'Ngày học không được để trống'),
  startTime: z.string().optional(),
  endTime: z.string().optional(),
  topic: z.string().optional(),
  cancelledByTeacher: z.boolean(),
})

type SessionForm = z.infer<typeof sessionSchema>

interface AttendanceRow {
  studentId: string
  studentName: string
  status: AttendanceStatus
  comment: string
  commentId?: string
}

export default function SessionDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { showToast } = useToast()

  const { data: session, isLoading, isError } = useSession(id!)
  const updateSession = useUpdateSession()
  const saveAttendance = useSaveAttendance()
  const saveComment = useSaveComment(id!)

  const [rows, setRows] = useState<AttendanceRow[]>([])
  const [saving, setSaving] = useState(false)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<SessionForm>({ resolver: zodResolver(sessionSchema) })

  // Initialise rows from session data
  useEffect(() => {
    if (!session) return

    reset({
      sessionDate: session.sessionDate,
      startTime: session.startTime ?? '',
      endTime: session.endTime ?? '',
      topic: session.topic ?? '',
      cancelledByTeacher: session.cancelledByTeacher,
    })

    const attendanceMap: Record<string, AttendanceStatus> = {}
    for (const a of session.attendance ?? []) {
      attendanceMap[a.studentId] = a.status
    }

    const commentMap: Record<string, { body: string; id?: string }> = {}
    for (const c of session.comments ?? []) {
      commentMap[c.studentId] = { body: c.body }
    }

    // Build rows from attendance list; if empty, derive from comments
    const studentSet = new Map<string, string>()
    for (const a of session.attendance ?? []) {
      studentSet.set(a.studentId, a.studentName)
    }

    const newRows: AttendanceRow[] = Array.from(studentSet.entries()).map(
      ([studentId, studentName]) => ({
        studentId,
        studentName,
        status: attendanceMap[studentId] ?? 'PRESENT',
        comment: commentMap[studentId]?.body ?? '',
        // commentId is not available in session detail; useSaveComment handles upsert
        commentId: undefined,
      })
    )

    setRows(newRows)
  }, [session, reset])

  const updateRow = (
    studentId: string,
    field: 'status' | 'comment',
    value: AttendanceStatus | string
  ) => {
    setRows((prev) =>
      prev.map((r) =>
        r.studentId === studentId ? { ...r, [field]: value } : r
      )
    )
  }

  const onSaveInfo = async (data: SessionForm) => {
    try {
      await updateSession.mutateAsync({ id: id!, data })
      showToast('Đã cập nhật thông tin buổi học', 'success')
    } catch {
      showToast('Có lỗi khi lưu thông tin', 'error')
    }
  }

  const onSaveAll = async () => {
    if (!session) return
    setSaving(true)
    try {
      // Save attendance
      await saveAttendance.mutateAsync({
        sessionId: id!,
        records: rows.map((r) => ({ studentId: r.studentId, status: r.status })),
      })

      // Save comments (only non-empty)
      await Promise.all(
        rows
          .filter((r) => r.comment.trim())
          .map((r) =>
            saveComment.mutateAsync({
              studentId: r.studentId,
              body: r.comment,
              commentId: r.commentId,
            })
          )
      )

      showToast('Đã lưu điểm danh và nhận xét', 'success')
    } catch {
      showToast('Có lỗi khi lưu, vui lòng thử lại', 'error')
    } finally {
      setSaving(false)
    }
  }

  if (isLoading) return <PageSpinner />

  if (isError || !session) {
    return (
      <div className="p-6 bg-red-50 rounded-lg text-red-700 text-sm">
        Không tìm thấy buổi học.{' '}
        <button onClick={() => navigate(-1)} className="underline">
          Quay lại
        </button>
      </div>
    )
  }

  const attendanceLabels: Record<AttendanceStatus, string> = {
    PRESENT: 'Có mặt',
    ABSENT: 'Vắng mặt',
    LATE: 'Đi muộn',
  }

  return (
    <div>
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 mb-4" aria-label="Breadcrumb">
        <Link to={`/classes/${session.classId}`} className="hover:text-gray-700">
          {session.className ?? 'Lớp học'}
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900 font-medium">
          Buổi học {formatDate(session.sessionDate)}
        </span>
      </nav>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* LEFT: Session Info */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Thông tin buổi học</h2>

          <form onSubmit={handleSubmit(onSaveInfo)} className="space-y-4">
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

            <div className="flex items-center gap-3 p-3 bg-red-50 border border-red-200 rounded-md">
              <input
                id="sess-cancelled"
                {...register('cancelledByTeacher')}
                type="checkbox"
                className="h-4 w-4 text-red-600 border-gray-300 rounded"
              />
              <div>
                <label htmlFor="sess-cancelled" className="text-sm font-medium text-red-700">
                  Huỷ buổi học
                </label>
                <p className="text-xs text-red-500 mt-0.5">
                  Buổi huỷ sẽ không được tính vào học phí
                </p>
              </div>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full bg-gray-800 text-white py-2 rounded-md text-sm font-medium hover:bg-gray-900 disabled:opacity-50"
            >
              {isSubmitting ? 'Đang lưu...' : 'Lưu thông tin'}
            </button>
          </form>
        </div>

        {/* RIGHT: Attendance + Comments */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Điểm danh & Nhận xét</h2>
            <button
              onClick={onSaveAll}
              disabled={saving || rows.length === 0}
              className="bg-blue-600 text-white px-4 py-1.5 rounded-md text-sm hover:bg-blue-700 disabled:opacity-50"
            >
              {saving ? 'Đang lưu...' : 'Lưu tất cả'}
            </button>
          </div>

          {rows.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-8">
              Chưa có học sinh nào trong buổi học này
            </p>
          ) : (
            <div className="space-y-3 max-h-[520px] overflow-y-auto pr-1">
              {rows.map((row) => (
                <div
                  key={row.studentId}
                  className="border border-gray-200 rounded-lg p-3"
                >
                  <div className="flex items-center justify-between mb-2">
                    <p className="text-sm font-medium text-gray-900">{row.studentName}</p>
                    <div role="group" aria-label={`Trạng thái ${row.studentName}`} className="flex gap-1">
                      {(['PRESENT', 'ABSENT', 'LATE'] as AttendanceStatus[]).map((status) => (
                        <button
                          key={status}
                          type="button"
                          onClick={() => updateRow(row.studentId, 'status', status)}
                          aria-pressed={row.status === status}
                          className={`px-2 py-0.5 rounded text-xs font-medium transition-colors ${
                            row.status === status
                              ? status === 'PRESENT'
                                ? 'bg-green-600 text-white'
                                : status === 'ABSENT'
                                ? 'bg-red-600 text-white'
                                : 'bg-yellow-500 text-white'
                              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                          }`}
                        >
                          {attendanceLabels[status]}
                        </button>
                      ))}
                    </div>
                  </div>
                  <textarea
                    value={row.comment}
                    onChange={(e) => updateRow(row.studentId, 'comment', e.target.value)}
                    placeholder="Nhận xét học sinh..."
                    rows={2}
                    aria-label={`Nhận xét ${row.studentName}`}
                    className="w-full border border-gray-200 rounded px-2 py-1.5 text-xs resize-none focus:outline-none focus:ring-1 focus:ring-blue-400 text-gray-700"
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
