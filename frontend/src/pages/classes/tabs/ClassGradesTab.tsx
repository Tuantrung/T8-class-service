import { useState, useRef, type ChangeEvent, type KeyboardEvent } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  useGrades,
  useCreateGrade,
  useUpdateGrade,
  useImportGrades,
  useDownloadGradeTemplate,
} from '../../../hooks/useGrades'
import { useStudentsByClass } from '../../../hooks/useStudents'
import { useToast } from '../../../components/ui/Toast'
import { Modal } from '../../../components/ui/Modal'
import { PageSpinner } from '../../../components/ui/Spinner'
import type { GradeDto, GradeImportResult } from '../../../api/types'
import { formatDate } from '../../../lib/utils'

const examSchema = z.object({
  examName: z.string().min(1, 'Tên bài kiểm tra không được để trống'),
  examDate: z.string().optional(),
})

type ExamForm = z.infer<typeof examSchema>

interface Exam {
  examName: string
  examDate?: string
  grades: GradeDto[]
}

interface EditingCell {
  gradeId: string
  currentScore: number | undefined
}

interface Props {
  classId: string
}

export default function ClassGradesTab({ classId }: Props) {
  const [examModalOpen, setExamModalOpen] = useState(false)
  const [detailExam, setDetailExam] = useState<Exam | null>(null)
  const [editingCell, setEditingCell] = useState<EditingCell | null>(null)
  const [editValue, setEditValue] = useState('')
  const [importResult, setImportResult] = useState<GradeImportResult | null>(null)
  const [importResultOpen, setImportResultOpen] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const { showToast } = useToast()

  const { data: gradesPage, isLoading: loadingGrades } = useGrades(classId)
  const { data: students, isLoading: loadingStudents } = useStudentsByClass(classId)
  const createGrade = useCreateGrade()
  const updateGrade = useUpdateGrade(classId)
  const importGrades = useImportGrades(classId)
  const downloadTemplate = useDownloadGradeTemplate(classId)

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<ExamForm>({
    resolver: zodResolver(examSchema),
  })

  const grades = gradesPage?.content ?? []
  const studentList = students ?? []

  // Build student name lookup
  const studentNameMap: Record<string, string> = {}
  for (const s of studentList) {
    studentNameMap[s.id] = s.fullName
  }

  // Group grades into unique exams
  const examMap = new Map<string, Exam>()
  for (const g of grades) {
    const key = `${g.examName}||${g.examDate ?? ''}`
    if (!examMap.has(key)) {
      examMap.set(key, { examName: g.examName, examDate: g.examDate, grades: [] })
    }
    examMap.get(key)!.grades.push(g)
  }
  const exams = Array.from(examMap.values()).sort((a, b) => {
    if (a.examDate && b.examDate) return a.examDate.localeCompare(b.examDate)
    return a.examName.localeCompare(b.examName)
  })

  const onAddExam = async (data: ExamForm) => {
    if (studentList.length === 0) {
      showToast('Lớp chưa có học sinh nào', 'error')
      return
    }
    try {
      await Promise.all(
        studentList.map((s) =>
          createGrade.mutateAsync({
            classId,
            studentId: s.id,
            examName: data.examName,
            examDate: data.examDate,
          })
        )
      )
      showToast(`Đã thêm bài kiểm tra "${data.examName}"`, 'success')
      setExamModalOpen(false)
      reset()
    } catch {
      showToast('Có lỗi khi thêm bài kiểm tra', 'error')
    }
  }

  const startEdit = (cell: EditingCell) => {
    setEditingCell(cell)
    setEditValue(cell.currentScore != null ? String(cell.currentScore) : '')
  }

  const commitEdit = async () => {
    if (!editingCell) return
    const score = editValue === '' ? undefined : parseFloat(editValue)
    try {
      await updateGrade.mutateAsync({ id: editingCell.gradeId, data: { score } })
      setEditingCell(null)
      // Refresh detail view if open
      if (detailExam) {
        setDetailExam((prev) =>
          prev
            ? {
                ...prev,
                grades: prev.grades.map((g) =>
                  g.id === editingCell.gradeId ? { ...g, score } : g
                ),
              }
            : null
        )
      }
    } catch {
      showToast('Không thể lưu điểm', 'error')
    }
  }

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' || e.key === 'Tab') {
      e.preventDefault()
      commitEdit()
    }
    if (e.key === 'Escape') setEditingCell(null)
  }

  const handleImportClick = () => {
    fileInputRef.current?.click()
  }

  const handleFileChange = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file || !detailExam) return
    try {
      const result = await importGrades.mutateAsync({
        examName: detailExam.examName,
        examDate: detailExam.examDate,
        file,
      })
      setImportResult(result)
      setImportResultOpen(true)
      if (result.imported + result.updated > 0) {
        showToast(`Đã nhập ${result.imported} mới, cập nhật ${result.updated}`, 'success')
      }
    } catch {
      showToast('Lỗi nhập file, vui lòng kiểm tra định dạng', 'error')
    }
    e.target.value = ''
  }

  const handleDownloadTemplate = () => {
    downloadTemplate.mutate()
  }

  if (loadingGrades || loadingStudents) return <PageSpinner />

  return (
    <div>
      {/* Header actions */}
      <div className="flex items-center justify-between mb-4">
        <p className="text-sm text-gray-500">
          {studentList.length} học sinh &bull; {exams.length} bài kiểm tra
        </p>
        <button
          onClick={() => { reset(); setExamModalOpen(true) }}
          className="bg-blue-600 text-white px-3 py-1.5 rounded-md text-sm hover:bg-blue-700"
        >
          + Thêm bài kiểm tra
        </button>
      </div>

      {/* Exam list */}
      {exams.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 p-10 text-center text-gray-400">
          Chưa có bài kiểm tra nào. Nhấn "Thêm bài kiểm tra" để bắt đầu.
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
          <table className="w-full text-sm" aria-label="Danh sách bài kiểm tra">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600 w-12">STT</th>
                <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Tên bài kiểm tra</th>
                <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Ngày kiểm tra</th>
                <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600 w-24">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {exams.map((exam, idx) => (
                <tr key={`${exam.examName}||${exam.examDate}`} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-500">{idx + 1}</td>
                  <td className="px-4 py-3 font-medium text-gray-900">{exam.examName}</td>
                  <td className="px-4 py-3 text-gray-600">
                    {exam.examDate ? formatDate(exam.examDate) : '—'}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => { setEditingCell(null); setDetailExam(exam) }}
                      className="text-blue-600 hover:text-blue-800 text-xs font-medium"
                    >
                      Chi tiết
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Add exam modal */}
      <Modal open={examModalOpen} onClose={() => setExamModalOpen(false)} title="Thêm bài kiểm tra">
        <form onSubmit={handleSubmit(onAddExam)} className="space-y-4">
          <div>
            <label htmlFor="exam-name" className="block text-sm font-medium text-gray-700 mb-1">
              Tên bài kiểm tra <span className="text-red-500">*</span>
            </label>
            <input
              id="exam-name"
              {...register('examName')}
              placeholder="Vd: Kiểm tra giữa kỳ"
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.examName && (
              <p className="text-red-500 text-xs mt-1">{errors.examName.message}</p>
            )}
          </div>
          <div>
            <label htmlFor="exam-date" className="block text-sm font-medium text-gray-700 mb-1">
              Ngày kiểm tra
            </label>
            <input
              id="exam-date"
              {...register('examDate')}
              type="date"
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div className="flex gap-2 justify-end pt-2">
            <button
              type="button"
              onClick={() => setExamModalOpen(false)}
              className="px-4 py-2 border border-gray-300 text-gray-700 rounded-md text-sm hover:bg-gray-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm hover:bg-blue-700 disabled:opacity-50"
            >
              {isSubmitting ? 'Đang thêm...' : 'Thêm bài kiểm tra'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Exam detail modal */}
      <Modal
        open={!!detailExam}
        onClose={() => { setDetailExam(null); setEditingCell(null) }}
        title={detailExam ? `Chi tiết: ${detailExam.examName}` : ''}
      >
        {detailExam && (
          <div>
            {detailExam.examDate && (
              <p className="text-sm text-gray-500 mb-3">
                Ngày kiểm tra: {formatDate(detailExam.examDate)}
              </p>
            )}

            {/* Import actions */}
            <div className="flex gap-2 mb-3">
              <button
                onClick={handleDownloadTemplate}
                disabled={downloadTemplate.isPending}
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
                aria-label="Import Excel điểm"
              />
              <button
                onClick={handleImportClick}
                disabled={importGrades.isPending}
                className="border border-gray-300 text-gray-700 px-3 py-1 rounded-md text-sm hover:bg-gray-50 disabled:opacity-50"
              >
                {importGrades.isPending ? 'Đang nhập...' : 'Import Excel'}
              </button>
            </div>

            <table className="w-full text-sm" aria-label={`Điểm ${detailExam.examName}`}>
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-3 py-2 text-left font-medium text-gray-600 w-10">STT</th>
                  <th className="px-3 py-2 text-left font-medium text-gray-600">Học sinh</th>
                  <th className="px-3 py-2 text-center font-medium text-gray-600 w-28">Điểm</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {detailExam.grades.length === 0 ? (
                  <tr>
                    <td colSpan={3} className="px-3 py-6 text-center text-gray-400">
                      Chưa có dữ liệu điểm
                    </td>
                  </tr>
                ) : (
                  detailExam.grades.map((g, idx) => {
                    const isEditing = editingCell?.gradeId === g.id
                    return (
                      <tr key={g.id} className="hover:bg-gray-50">
                        <td className="px-3 py-2 text-gray-500">{idx + 1}</td>
                        <td className="px-3 py-2 font-medium text-gray-900">
                          {studentNameMap[g.studentId] ?? g.studentId}
                        </td>
                        <td className="px-3 py-2 text-center">
                          {isEditing ? (
                            <input
                              type="number"
                              value={editValue}
                              onChange={(e) => setEditValue(e.target.value)}
                              onBlur={commitEdit}
                              onKeyDown={handleKeyDown}
                              autoFocus
                              className="w-20 border border-blue-400 rounded px-1 py-0.5 text-center text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                              aria-label={`Điểm ${studentNameMap[g.studentId]}`}
                            />
                          ) : (
                            <button
                              type="button"
                              onClick={() => startEdit({ gradeId: g.id, currentScore: g.score })}
                              className="px-2 py-1 rounded hover:bg-blue-50 text-gray-700 w-full text-center"
                              aria-label={`Sửa điểm ${studentNameMap[g.studentId]}`}
                            >
                              {g.score != null ? g.score : '—'}
                            </button>
                          )}
                        </td>
                      </tr>
                    )
                  })
                )}
              </tbody>
            </table>
            <p className="text-xs text-gray-400 mt-2">Nhấp vào điểm để chỉnh sửa. Enter để lưu.</p>
          </div>
        )}
      </Modal>

      {/* Import result modal */}
      <Modal
        open={importResultOpen}
        onClose={() => setImportResultOpen(false)}
        title="Kết quả import điểm"
      >
        {importResult && (
          <div className="space-y-3">
            <div className="flex gap-3">
              <div className="bg-green-50 rounded-lg p-3 flex-1 text-center">
                <p className="text-xl font-bold text-green-600">{importResult.imported}</p>
                <p className="text-xs text-green-600">Thêm mới</p>
              </div>
              <div className="bg-blue-50 rounded-lg p-3 flex-1 text-center">
                <p className="text-xl font-bold text-blue-600">{importResult.updated}</p>
                <p className="text-xs text-blue-600">Cập nhật</p>
              </div>
              <div className="bg-gray-50 rounded-lg p-3 flex-1 text-center">
                <p className="text-xl font-bold text-gray-600">{importResult.skipped}</p>
                <p className="text-xs text-gray-600">Bỏ qua</p>
              </div>
            </div>
            {importResult.errors.length > 0 && (
              <div>
                <p className="text-sm font-medium text-gray-700 mb-1">Lỗi:</p>
                <div className="bg-red-50 rounded-md p-3 max-h-36 overflow-y-auto">
                  {importResult.errors.map((err, i) => (
                    <p key={i} className="text-xs text-red-600">
                      Dòng {err.rowNumber}: {err.message}
                    </p>
                  ))}
                </div>
              </div>
            )}
            <div className="flex justify-end">
              <button
                onClick={() => setImportResultOpen(false)}
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
