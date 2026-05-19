// TODO Track 5: Implement full class detail with tabs: Overview | Students | Sessions | Grades
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { classesApi } from '../../api/endpoints/classes'

export default function ClassDetailPage() {
  const { id } = useParams<{ id: string }>()

  const { data: cls, isLoading } = useQuery({
    queryKey: ['class', id],
    queryFn: () => classesApi.get(id!),
    enabled: !!id,
  })

  if (isLoading) return <p className="text-gray-500">Loading...</p>
  if (!cls) return <p className="text-red-500">Class not found</p>

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-2">{cls.name}</h1>
      <p className="text-gray-500 mb-6">{cls.subject ?? 'No subject'}</p>

      <div className="grid grid-cols-2 gap-4 mb-6">
        <InfoItem label="Status" value={cls.status} />
        <InfoItem label="Rate / Session" value={`${cls.ratePerSession.toLocaleString()} VND`} />
      </div>

      <p className="text-gray-400 text-sm">
        TODO: Add Students, Sessions, and Grades tabs in Track 5.
      </p>
    </div>
  )
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-4">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="text-sm font-medium text-gray-900 mt-1">{value}</p>
    </div>
  )
}
