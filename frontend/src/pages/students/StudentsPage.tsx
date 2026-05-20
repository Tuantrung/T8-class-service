import { useState, useCallback, useRef, type ChangeEvent } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useStudents, useCreateStudent } from '../../hooks/useStudents'
import { useToast } from '../../components/ui/Toast'
import { Modal } from '../../components/ui/Modal'
import { PageSpinner } from '../../components/ui/Spinner'

const studentSchema = z.object({
  fullName: z.string().min(1, 'Họ tên không được để trống'),
  phone: z.string().optional(),
  parentPhone: z.string().optional(),
  notes: z.string().optional(),
})

type StudentForm = z.infer<typeof studentSchema>

export default function StudentsPage() {
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [addModalOpen, setAddModalOpen] = useState(false)
  const { showToast } = useToast()
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const { data, isLoading, isError } = useStudents({ search: debouncedSearch })
  const createStudent = useCreateStudent()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<StudentForm>({ resolver: zodResolver(studentSchema) })

  const handleSearchChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value
    setSearch(val)
    if (debounceTimer.current) clearTimeout(debounceTimer.current)
    debounceTimer.current = setTimeout(() => setDebouncedSearch(val), 300)
  }, [])

  const onSubmit = async (data: StudentForm) => {
    try {
      await createStudent.mutateAsync(data)
      showToast('Thêm học sinh thành công', 'success')
      setAddModalOpen(false)
      reset()
    } catch {
      showToast('Có lỗi khi thêm học sinh', 'error')
    }
  }

  if (isLoading) return <PageSpinner />

  if (isError) {
    return (
      <div className="p-4 bg-red-50 rounded-lg text-red-700 text-sm">
        Không thể tải danh sách học sinh.
      </div>
    )
  }

  const students = data?.content ?? []

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Học sinh</h1>
        <button
          onClick={() => {
            reset()
            setAddModalOpen(true)
          }}
          className="inline-flex items-center bg-blue-600 text-white px-4 py-2 rounded-md text-sm font-medium hover:bg-blue-700 transition-colors"
        >
          + Thêm học sinh
        </button>
      </div>

      <div className="mb-4">
        <label htmlFor="student-search" className="sr-only">
          Tìm kiếm học sinh
        </label>
        <input
          id="student-search"
          type="search"
          value={search}
          onChange={handleSearchChange}
          placeholder="Tìm theo tên hoặc số điện thoại..."
          className="w-full max-w-sm border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-x-auto">
        <table className="w-full text-sm" aria-label="Danh sách học sinh">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Họ tên</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">SĐT học sinh</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">SĐT phụ huynh</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Ghi chú</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {students.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-10 text-center text-gray-400">
                  {search ? 'Không tìm thấy học sinh phù hợp' : 'Chưa có học sinh nào'}
                </td>
              </tr>
            ) : (
              students.map((s) => (
                <tr key={s.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">
                    <Link to={`/students/${s.id}`} className="text-blue-600 hover:underline">
                      {s.fullName}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{s.phone ?? '-'}</td>
                  <td className="px-4 py-3 text-gray-600">{s.parentPhone ?? '-'}</td>
                  <td className="px-4 py-3 text-gray-600 max-w-xs truncate">{s.notes ?? '-'}</td>
                  <td className="px-4 py-3">
                    <Link
                      to={`/students/${s.id}`}
                      className="text-blue-600 hover:underline text-xs font-medium"
                    >
                      Xem chi tiết
                    </Link>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <p className="mt-3 text-sm text-gray-500 text-right">
          {data.totalElements} học sinh
        </p>
      )}

      <Modal
        open={addModalOpen}
        onClose={() => setAddModalOpen(false)}
        title="Thêm học sinh mới"
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label htmlFor="new-fullName" className="block text-sm font-medium text-gray-700 mb-1">
              Họ tên <span className="text-red-500">*</span>
            </label>
            <input
              id="new-fullName"
              {...register('fullName')}
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.fullName && (
              <p className="text-red-500 text-xs mt-1">{errors.fullName.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="new-phone" className="block text-sm font-medium text-gray-700 mb-1">
              SĐT học sinh
            </label>
            <input
              id="new-phone"
              {...register('phone')}
              type="tel"
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label htmlFor="new-parentPhone" className="block text-sm font-medium text-gray-700 mb-1">
              SĐT phụ huynh
            </label>
            <input
              id="new-parentPhone"
              {...register('parentPhone')}
              type="tel"
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label htmlFor="new-notes" className="block text-sm font-medium text-gray-700 mb-1">
              Ghi chú
            </label>
            <textarea
              id="new-notes"
              {...register('notes')}
              rows={2}
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
            />
          </div>

          <div className="flex gap-2 justify-end pt-2">
            <button
              type="button"
              onClick={() => setAddModalOpen(false)}
              className="px-4 py-2 border border-gray-300 text-gray-700 rounded-md text-sm hover:bg-gray-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm hover:bg-blue-700 disabled:opacity-50"
            >
              {isSubmitting ? 'Đang thêm...' : 'Thêm học sinh'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
