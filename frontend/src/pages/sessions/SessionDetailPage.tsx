// TODO Track 5: Implement session detail with attendance + comments grid
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { sessionsApi } from '../../api/endpoints/sessions'

export default function SessionDetailPage() {
  const { id } = useParams<{ id: string }>()

  const { data: session, isLoading } = useQuery({
    queryKey: ['session', id],
    queryFn: () => sessionsApi.get(id!),
    enabled: !!id,
  })

  if (isLoading) return <p className="text-gray-500">Loading...</p>
  if (!session) return <p className="text-red-500">Session not found</p>

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-2">Session: {session.sessionDate}</h1>
      <p className="text-gray-500 mb-6">{session.topic ?? 'No topic'}</p>
      {session.cancelledByTeacher && (
        <div className="mb-4 p-3 bg-yellow-50 text-yellow-700 rounded text-sm">
          This session was cancelled by teacher
        </div>
      )}
      <p className="text-gray-400 text-sm">TODO: Add attendance grid and comments panel in Track 5.</p>
    </div>
  )
}
