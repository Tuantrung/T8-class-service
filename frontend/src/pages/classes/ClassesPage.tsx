import { useState, useCallback, useRef, type ChangeEvent } from 'react'
import { Link } from 'react-router-dom'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useClasses, useCreateClass, useUpdateClass, useDeactivateClass } from '../../hooks/useClasses'
import { useToast } from '../../components/ui/Toast'
import { Modal } from '../../components/ui/Modal'
import { PageSpinner } from '../../components/ui/Spinner'
import { StatusBadge } from '../../components/ui/Badge'
import { formatCurrency } from '../../lib/utils'
import type { ClassDto } from '../../api/types'

const classSchema = z.object({
  name: z.string().min(1, 'Tên lớp không được để trống'),
  subject: z.string().optional(),
  teacherId: z.string().min(1, 'Giáo viên không được để trống'),
  ratePerSession: z.coerce.number().min(0, 'Học phí phải >= 0'),
})

type ClassForm = z.infer<typeof classSchema>

export default function ClassesPage() {
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editingClass, setEditingClass] = useState<ClassDto | null>(null)
  const { showToast } = useToast()
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const { data, isLoading, isError } = useClasses({ search: debouncedSearch })
  const createClass = useCreateClass()
  const updateClass = useUpdateClass()
  const deactivateClass = useDeactivateClass()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ClassForm>({ resolver: zodResolver(classSchema) as Resolver<ClassForm> })

  const handleSearchChange = useCallback(
    (e: ChangeEvent<HTMLInputElement>) => {
      const val = e.target.value
      setSearch(val)
      if (debounceTimer.current) clearTimeout(debounceTimer.current)
      debounceTimer.current = setTimeout(() => setDebouncedSearch(val), 300)
    },
    []
  )

  const openCreate = () => {
    setEditingClass(null)
    reset({ name: '', subject: '', teacherId: '', ratePerSession: 0 })
    setModalOpen(true)
  }

  const openEdit = (cls: ClassDto) => {
    setEditingClass(cls)
    reset({
      name: cls.name,
      subject: cls.subject ?? '',
      teacherId: cls.teacher?.id ?? cls.teacherId,
      ratePerSession: cls.ratePerSession,
    })
    setModalOpen(true)
  }

  const onSubmit = async (data: ClassForm) => {
    try {
      if (editingClass) {
        await updateClass.mutateAsync({
          id: editingClass.id,
          data: { ...data, status: editingClass.status },
        })
        showToast('Cập nhật lớp học thành công', 'success')
      } else {
        await createClass.mutateAsync(data)
        showToast('Tạo lớp học thành công', 'success')
      }
      setModalOpen(false)
      reset()
    } catch {
      showToast('Có lỗi xảy ra, vui lòng thử lại', 'error')
    }
  }

  const handleDeactivate = async (cls: ClassDto) => {
    if (!confirm(`Bạn có chắc chắn muốn lưu trữ lớp "${cls.name}"?`)) return
    try {
      await deactivateClass.mutateAsync(cls.id)
      showToast('Đã lưu trữ lớp học', 'success')
    } catch {
      showToast('Không thể lưu trữ lớp học', 'error')
    }
  }

  if (isLoading) return <PageSpinner />

  if (isError) {
    return (
      <div className="p-6 bg-red-50 rounded-lg text-red-700 text-sm">
        Không thể tải danh sách lớp học. Vui lòng thử lại.
      </div>
    )
  }

  const classes = data?.content ?? []

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Lớp học</h1>
        <button
          onClick={openCreate}
          className="inline-flex items-center bg-blue-600 text-white px-4 py-2 rounded-md text-sm font-medium hover:bg-blue-700 transition-colors"
        >
          + Thêm lớp
        </button>
      </div>

      <div className="mb-4">
        <label htmlFor="class-search" className="sr-only">
          Tìm kiếm lớp học
        </label>
        <input
          id="class-search"
          type="search"
          value={search}
          onChange={handleSearchChange}
          placeholder="Tìm kiếm theo tên lớp..."
          className="w-full max-w-sm border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-x-auto">
        <table className="w-full text-sm" aria-label="Danh sách lớp học">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Tên lớp</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Môn học</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Giáo viên</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Học phí/buổi</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Trạng thái</th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {classes.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-10 text-center text-gray-400">
                  {search ? 'Không tìm thấy lớp học phù hợp' : 'Chưa có lớp học nào'}
                </td>
              </tr>
            ) : (
              classes.map((cls) => (
                <tr key={cls.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">
                    <Link to={`/classes/${cls.id}`} className="text-blue-600 hover:underline">
                      {cls.name}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{cls.subject ?? '-'}</td>
                  <td className="px-4 py-3 text-gray-600">
                    {cls.teacher?.fullName ?? '-'}
                  </td>
                  <td className="px-4 py-3 text-gray-600">{formatCurrency(cls.ratePerSession)}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={cls.status} />
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <button
                        onClick={() => openEdit(cls)}
                        className="text-blue-600 hover:text-blue-800 text-xs font-medium"
                        aria-label={`Chỉnh sửa lớp ${cls.name}`}
                      >
                        Sửa
                      </button>
                      {cls.status === 'ACTIVE' && (
                        <button
                          onClick={() => handleDeactivate(cls)}
                          className="text-red-600 hover:text-red-800 text-xs font-medium"
                          aria-label={`Lưu trữ lớp ${cls.name}`}
                        >
                          Lưu trữ
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <p className="mt-3 text-sm text-gray-500 text-right">
          Trang 1/{data.totalPages} &bull; {data.totalElements} lớp
        </p>
      )}

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editingClass ? 'Chỉnh sửa lớp học' : 'Thêm lớp học mới'}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label htmlFor="cls-name" className="block text-sm font-medium text-gray-700 mb-1">
              Tên lớp <span className="text-red-500">*</span>
            </label>
            <input
              id="cls-name"
              {...register('name')}
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.name && <p className="text-red-500 text-xs mt-1">{errors.name.message}</p>}
          </div>

          <div>
            <label htmlFor="cls-subject" className="block text-sm font-medium text-gray-700 mb-1">
              Môn học
            </label>
            <input
              id="cls-subject"
              {...register('subject')}
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label htmlFor="cls-teacher" className="block text-sm font-medium text-gray-700 mb-1">
              ID Giáo viên <span className="text-red-500">*</span>
            </label>
            <input
              id="cls-teacher"
              {...register('teacherId')}
              placeholder="UUID giáo viên"
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.teacherId && (
              <p className="text-red-500 text-xs mt-1">{errors.teacherId.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="cls-rate" className="block text-sm font-medium text-gray-700 mb-1">
              Học phí/buổi (VND) <span className="text-red-500">*</span>
            </label>
            <input
              id="cls-rate"
              {...register('ratePerSession')}
              type="number"
              min={0}
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.ratePerSession && (
              <p className="text-red-500 text-xs mt-1">{errors.ratePerSession.message}</p>
            )}
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
              {isSubmitting ? 'Đang lưu...' : editingClass ? 'Cập nhật' : 'Tạo lớp'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
