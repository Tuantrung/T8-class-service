import { useState } from 'react'
import { useClasses } from '../../hooks/useClasses'
import { PageSpinner } from '../../components/ui/Spinner'
import ClassGradesTab from '../classes/tabs/ClassGradesTab'

export default function GradesPage() {
  const [selectedClassId, setSelectedClassId] = useState('')

  const { data: classesData, isLoading } = useClasses({ size: 100 })
  const classes = classesData?.content ?? []

  if (isLoading) return <PageSpinner />

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Điểm số</h1>

      <div className="mb-6">
        <label htmlFor="grade-class" className="block text-sm font-medium text-gray-700 mb-1">
          Chọn lớp học
        </label>
        <select
          id="grade-class"
          value={selectedClassId}
          onChange={(e) => setSelectedClassId(e.target.value)}
          className="w-full max-w-sm border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">-- Chọn lớp --</option>
          {classes.map((cls) => (
            <option key={cls.id} value={cls.id}>
              {cls.name} {cls.subject ? `(${cls.subject})` : ''}
            </option>
          ))}
        </select>
      </div>

      {selectedClassId ? (
        <ClassGradesTab classId={selectedClassId} />
      ) : (
        <div className="bg-white rounded-lg border border-gray-200 p-10 text-center text-gray-400">
          Vui lòng chọn lớp học để xem điểm số
        </div>
      )}
    </div>
  )
}
