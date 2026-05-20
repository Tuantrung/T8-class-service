import { useState, type KeyboardEvent } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useGrades, useCreateGrade, useUpdateGrade } from '../../../hooks/useGrades'
import { useStudentsByClass } from '../../../hooks/useStudents'
import { useToast } from '../../../components/ui/Toast'
import { Modal } from '../../../components/ui/Modal'
import { PageSpinner } from '../../../components/ui/Spinner'
import type { GradeDto } from '../../../api/types'

const examSchema = z.object({
  examName: z.string().min(1, 'Tên bài kiểm tra không được để trống'),
  examDate: z.string().optional(),
})

type ExamForm = z.infer<typeof examSchema>

interface Props {
  classId: string
}

interface EditingCell {
  gradeId: string
  studentId: string
  examName: string
  currentScore: number | undefined
}

export default function ClassGradesTab({ classId }: Props) {
  const [examModalOpen, setExamModalOpen] = useState(false)
  const [editingCell, setEditingCell] = useState<EditingCell | null>(null)
  const [editValue, setEditValue] = useState('')
  const { showToast } = useToast()

  const { data: gradesPage, isLoading: loadingGrades } = useGrades(classId)
  const { data: students, isLoading: loadingStudents } = useStudentsByClass(classId)
  const createGrade = useCreateGrade()
  const updateGrade = useUpdateGrade(classId)

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<ExamForm>({
    resolver: zodResolver(examSchema),
  })

  const grades = gradesPage?.content ?? []
  const studentList = students ?? []

  // Derive unique exam names (columns)
  const examNames = [...new Set(grades.map((g) => g.examName))].sort()

  // Map: studentId -> examName -> grade
  const gradeMap: Record<string, Record<string, GradeDto>> = {}
  for (const g of grades) {
    if (!gradeMap[g.studentId]) gradeMap[g.studentId] = {}
    gradeMap[g.studentId][g.examName] = g
  }

  const onAddExam = async (data: ExamForm) => {
    // Create a grade entry for each student with this exam name (score null initially)
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
      await updateGrade.mutateAsync({
        id: editingCell.gradeId,
        data: { score },
      })
      setEditingCell(null)
    } catch {
      showToast('Không thể lưu điểm', 'error')
    }
  }

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' || e.key === 'Tab') {
      e.preventDefault()
      commitEdit()
    }
    if (e.key === 'Escape') {
      setEditingCell(null)
    }
  }

  if (loadingGrades || loadingStudents) return <PageSpinner />

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <p className="text-sm text-gray-500">
          {studentList.length} học sinh &bull; {examNames.length} bài kiểm tra
        </p>
        <button
          onClick={() => {
            reset()
            setExamModalOpen(true)
          }}
          className="bg-blue-600 text-white px-3 py-1.5 rounded-md text-sm hover:bg-blue-700"
        >
          + Thêm bài kiểm tra
        </button>
      </div>

      {examNames.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 p-10 text-center text-gray-400">
          Chưa có bài kiểm tra nào. Nhấn "Thêm bài kiểm tra" để bắt đầu.
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-x-auto">
          <table className="w-full text-sm" aria-label="Bảng điểm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600 sticky left-0 bg-gray-50">
                  Tên học sinh
                </th>
                {examNames.map((exam) => (
                  <th key={exam} scope="col" className="px-4 py-3 text-center font-medium text-gray-600 min-w-24">
                    {exam}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {studentList.map((student) => (
                <tr key={student.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900 sticky left-0 bg-white">
                    {student.fullName}
                  </td>
                  {examNames.map((exam) => {
                    const grade = gradeMap[student.id]?.[exam]
                    const isEditing =
                      editingCell?.gradeId === grade?.id &&
                      editingCell?.studentId === student.id

                    if (!grade) {
                      return (
                        <td key={exam} className="px-4 py-3 text-center text-gray-400">
                          -
                        </td>
                      )
                    }

                    return (
                      <td key={exam} className="px-4 py-3 text-center">
                        {isEditing ? (
                          <input
                            type="number"
                            value={editValue}
                            onChange={(e) => setEditValue(e.target.value)}
                            onBlur={commitEdit}
                            onKeyDown={handleKeyDown}
                            autoFocus
                            className="w-16 border border-blue-400 rounded px-1 py-0.5 text-center text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                            aria-label={`Điểm ${exam} của ${student.fullName}`}
                          />
                        ) : (
                          <button
                            type="button"
                            className="px-2 py-1 rounded hover:bg-blue-50 text-gray-700 cursor-pointer w-full text-center"
                            aria-label={`Sửa điểm ${exam} của ${student.fullName}`}
                            onClick={() =>
                              startEdit({
                                gradeId: grade.id,
                                studentId: student.id,
                                examName: exam,
                                currentScore: grade.score,
                              })
                            }
                          >
                            {grade.score != null ? grade.score : '-'}
                          </button>
                        )}
                      </td>
                    )
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <p className="text-xs text-gray-400 mt-2">
        Nhấp vào ô điểm để chỉnh sửa. Nhấn Enter hoặc Tab để lưu.
      </p>

      <Modal
        open={examModalOpen}
        onClose={() => setExamModalOpen(false)}
        title="Thêm bài kiểm tra"
      >
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
    </div>
  )
}
