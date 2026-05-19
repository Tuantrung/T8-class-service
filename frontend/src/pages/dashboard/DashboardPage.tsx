import { useQuery } from '@tanstack/react-query'
import { classesApi } from '../../api/endpoints/classes'
import { studentsApi } from '../../api/endpoints/students'
import { useAuthStore } from '../../store/authStore'

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user)

  const { data: classes } = useQuery({
    queryKey: ['classes', { page: 0, size: 1 }],
    queryFn: () => classesApi.list(0, 1),
  })

  const { data: students } = useQuery({
    queryKey: ['students', { page: 0, size: 1 }],
    queryFn: () => studentsApi.list(0, 1),
  })

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">
        Welcome, {user?.fullName ?? user?.email}
      </h1>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <StatCard label="Total Classes" value={classes?.totalElements ?? '-'} />
        <StatCard label="Total Students" value={students?.totalElements ?? '-'} />
      </div>
    </div>
  )
}

function StatCard({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5">
      <p className="text-sm text-gray-500">{label}</p>
      <p className="text-3xl font-bold text-gray-900 mt-1">{value}</p>
    </div>
  )
}
