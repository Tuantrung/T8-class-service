// TODO Track 5: Implement full student detail with enrolled classes, grades
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { studentsApi } from '../../api/endpoints/students'

export default function StudentDetailPage() {
  const { id } = useParams<{ id: string }>()

  const { data: student, isLoading } = useQuery({
    queryKey: ['student', id],
    queryFn: () => studentsApi.get(id!),
    enabled: !!id,
  })

  if (isLoading) return <p className="text-gray-500">Loading...</p>
  if (!student) return <p className="text-red-500">Student not found</p>

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-2">{student.fullName}</h1>
      <p className="text-gray-500 mb-6">{student.phone ?? 'No phone'}</p>
      <p className="text-gray-400 text-sm">TODO: Add enrolled classes, recent grades in Track 5.</p>
    </div>
  )
}
