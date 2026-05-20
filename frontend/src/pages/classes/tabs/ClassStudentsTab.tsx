import { useState, useRef, type ChangeEvent } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  useStudentsByClass,
  useRemoveStudentFromClass,
  useCreateStudent,
  useImportStudents,
} from '../../../hooks/useStudents'
import { useEnrollStudents } from '../../../hooks/useClasses'
import { useToast } from '../../../components/ui/Toast'
import { Modal } from '../../../components/ui/Modal'
import { PageSpinner } from '../../../components/ui/Spinner'
import { studentsApi } from '../../../api/endpoints/students'
import { downloadBlob } from '../../../lib/utils'
import type { ImportResult } from '../../../api/types'

const studentSchema = z.object({
  fullName: z.string().min(1, 'Họ tên không được để trống'),
  parentPhone: z.string().optional(),
  phone: z.string().optional(),
  notes: z.string().optional(),
  schoolName: z.string().optional(),
})

type StudentForm = z.infer<typeof studentSchema>

interface Props {
  classId: string
}

export default function ClassStudentsTab({ classId }: Props) {
  const [addModalOpen, setAddModalOpen] = useState(false)
  const [importResult, setImportResult] = useState<ImportResult | null>(null)
  const [importModalOpen, setImportModalOpen] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const { showToast } = useToast()

  const { data: students, isLoading, isError } = useStudentsByClass(classId)
  const removeStudent = useRemoveStudentFromClass(classId)
  const createStudent = useCreateStudent()
  const enrollStudents = useEnrollStudents(classId)
  const importStudents = useImportStudents(classId)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<StudentForm>({ resolver: zodResolver(studentSchema) })

  const onAddStudent = async (data: StudentForm) => {
    try {
      const newStudent = await createStudent.mutateAsync(data)
      await enrollStudents.mutateAsync([newStudent.id])
      showToast('Thêm học sinh thành công', 'success')
      setAddModalOpen(false)
      reset()
    } catch {
      showToast('Có lỗi xảy ra khi thêm học sinh', 'error')
    }
  }

  const handleRemove = async (studentId: string, name: string) => {
    if (!confirm(`Xóa học sinh "${name}" khỏi lớp?`)) return
    try {
      await removeStudent.mutateAsync(studentId)
      showToast('Đã xóa học sinh khỏi lớp', 'success')
    } catch {
      showToast('Không thể xóa học sinh', 'error')
    }
  }

  const handleImportClick = () => {
    fileInputRef.current?.click()
  }

  const handleFileChange = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    try {
      const result = await importStudents.mutateAsync(file)
      setImportResult(result)
      setImportModalOpen(true)
      if (result.imported > 0) {
        showToast(`Đã nhập ${result.imported} học sinh`, 'success')
      }
    } catch {
      showToast('Lỗi nhập file, vui lòng kiểm tra định dạng', 'error')
    }
    // Reset input
    e.target.value = ''
  }

  const handleDownloadTemplate = async () => {
    try {
      const blob = await studentsApi.downloadTemplate()
      downloadBlob(blob, 'student-import-template.xlsx')
    } catch {
      showToast('Không thể tải template', 'error')
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

  const studentList = students ?? []

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
        <p className="text-sm text-gray-500">{studentList.length} học sinh</p>
        <div className="flex gap-2 flex-wrap">
          <button
            onClick={handleDownloadTemplate}
            className="text-sm text-blue-600 hover:underline"
          >
            Tải template
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".xlsx"
            className="hidden"
            onChange={handleFileChange}
            aria-label="Import Excel"
          />
          <button
            onClick={handleImportClick}
            disabled={importStudents.isPending}
            className="border border-gray-300 text-gray-700 px-3 py-1.5 rounded-md text-sm hover:bg-gray-50 disabled:opacity-50"
          >
            {importStudents.isPending ? 'Đang nhập...' : 'Import Excel'}
          </button>
          <button
            onClick={() => {
              reset()
              setAddModalOpen(true)
            }}
            className="bg-blue-600 text-white px-3 py-1.5 rounded-md text-sm hover:bg-blue-700"
          >
            + Thêm học sinh
          </button>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-x-auto">
        <table className="w-full text-sm" aria-label="Danh sách học sinh">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">
                Họ tên học sinh
              </th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">
                Trường
              </th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">
                SĐT học sinh
              </th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">
                SĐT phụ huynh
              </th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">
                Ghi chú
              </th>
              <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">
                Thao tác
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {studentList.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-10 text-center text-gray-400">
                  Chưa có học sinh nào trong lớp
                </td>
              </tr>
            ) : (
              studentList.map((s) => (
                <tr key={s.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">{s.fullName}</td>
                  <td className="px-4 py-3 text-gray-600">{s.schoolName ?? '-'}</td>
                  <td className="px-4 py-3 text-gray-600">{s.phone ?? '-'}</td>
                  <td className="px-4 py-3 text-gray-600">{s.parentPhone ?? '-'}</td>
                  <td className="px-4 py-3 text-gray-600 max-w-xs truncate">{s.notes ?? '-'}</td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => handleRemove(s.id, s.fullName)}
                      className="text-red-600 hover:text-red-800 text-xs font-medium"
                      aria-label={`Xóa ${s.fullName} khỏi lớp`}
                    >
                      Xóa khỏi lớp
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Add Student Modal */}
      <Modal open={addModalOpen} onClose={() => setAddModalOpen(false)} title="Thêm học sinh vào lớp">
        <form onSubmit={handleSubmit(onAddStudent)} className="space-y-4">
          <div>
            <label htmlFor="s-fullName" className="block text-sm font-medium text-gray-700 mb-1">
              Họ tên <span className="text-red-500">*</span>
            </label>
            <input
              id="s-fullName"
              {...register('fullName')}
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.fullName && (
              <p className="text-red-500 text-xs mt-1">{errors.fullName.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="s-phone" className="block text-sm font-medium text-gray-700 mb-1">
              SĐT học sinh
            </label>
            <input
              id="s-phone"
              {...register('phone')}
              type="tel"
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label htmlFor="s-parentPhone" className="block text-sm font-medium text-gray-700 mb-1">
              SĐT phụ huynh
            </label>
            <input
              id="s-parentPhone"
              {...register('parentPhone')}
              type="tel"
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label htmlFor="s-schoolName" className="block text-sm font-medium text-gray-700 mb-1">
              Tên trường
            </label>
            <input
              id="s-schoolName"
              {...register('schoolName')}
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label htmlFor="s-notes" className="block text-sm font-medium text-gray-700 mb-1">
              Ghi chú
            </label>
            <textarea
              id="s-notes"
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

      {/* Import Result Modal */}
      <Modal
        open={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        title="Kết quả nhập Excel"
      >
        {importResult && (
          <div className="space-y-3">
            <div className="flex gap-4">
              <div className="bg-green-50 rounded-lg p-3 flex-1 text-center">
                <p className="text-2xl font-bold text-green-600">{importResult.imported}</p>
                <p className="text-xs text-green-600">Đã nhập</p>
              </div>
              <div className="bg-gray-50 rounded-lg p-3 flex-1 text-center">
                <p className="text-2xl font-bold text-gray-600">{importResult.skipped}</p>
                <p className="text-xs text-gray-600">Bỏ qua</p>
              </div>
              <div className="bg-red-50 rounded-lg p-3 flex-1 text-center">
                <p className="text-2xl font-bold text-red-600">{importResult.errors.length}</p>
                <p className="text-xs text-red-600">Lỗi</p>
              </div>
            </div>

            {importResult.errors.length > 0 && (
              <div>
                <p className="text-sm font-medium text-gray-700 mb-2">Chi tiết lỗi:</p>
                <div className="bg-red-50 rounded-md p-3 max-h-40 overflow-y-auto">
                  {importResult.errors.map((err, i) => (
                    <p key={i} className="text-xs text-red-600">
                      Dòng {err.row}: {err.message}
                    </p>
                  ))}
                </div>
              </div>
            )}

            <div className="flex justify-end">
              <button
                onClick={() => setImportModalOpen(false)}
                className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm"
              >
                Đóng
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
