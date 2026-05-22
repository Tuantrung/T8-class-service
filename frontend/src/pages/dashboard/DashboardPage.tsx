import { useQuery } from '@tanstack/react-query'
import { classesApi } from '../../api/endpoints/classes'
import { studentsApi } from '../../api/endpoints/students'
import { billsApi } from '../../api/endpoints/bills'
import { useAuthStore } from '../../store/authStore'
import { PageSpinner } from '../../components/ui/Spinner'
import { Link } from 'react-router-dom'

function getCurrentMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user)
  const isAdmin = user?.role === 'ADMIN'
  const currentMonth = getCurrentMonth()

  const { data: classesData, isLoading: loadingClasses } = useQuery({
    queryKey: ['classes', { status: 'ACTIVE', page: 0, size: 1 }],
    queryFn: () => classesApi.list({ status: 'ACTIVE', page: 0, size: 1 }),
  })

  const { data: studentsData, isLoading: loadingStudents } = useQuery({
    queryKey: ['students', { page: 0, size: 1 }],
    queryFn: () => studentsApi.list({ page: 0, size: 1 }),
  })

  const { data: billsData, isLoading: loadingBills } = useQuery({
    queryKey: ['bills', { month: currentMonth, status: 'DRAFT', page: 0, size: 1 }],
    queryFn: () => billsApi.list({ month: currentMonth, status: 'DRAFT', page: 0, size: 1 }),
    enabled: isAdmin,
  })

  const isLoading = loadingClasses || loadingStudents || (isAdmin && loadingBills)

  if (isLoading) return <PageSpinner />

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">
          Xin chào, {user?.fullName ?? user?.email}
        </h1>
        <p className="text-sm text-gray-500 mt-1">
          Tháng {currentMonth.split('-')[1]}/{currentMonth.split('-')[0]}
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatCard
          label="Lớp đang hoạt động"
          value={classesData?.totalElements ?? 0}
          href="/classes"
          color="blue"
        />
        <StatCard
          label="Tổng học sinh"
          value={studentsData?.totalElements ?? 0}
          href="/students"
          color="green"
        />
        {isAdmin && (
          <StatCard
            label="Học phí chờ xử lý"
            value={billsData?.totalElements ?? 0}
            href="/billing"
            color="yellow"
          />
        )}
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5">
        <h2 className="text-base font-semibold text-gray-900 mb-3">Truy cập nhanh</h2>
        <div className="flex flex-wrap gap-3">
          <Link
            to="/classes"
            className="px-4 py-2 bg-blue-50 text-blue-700 rounded-md text-sm font-medium hover:bg-blue-100 transition-colors"
          >
            Quản lý lớp học
          </Link>
          <Link
            to="/students"
            className="px-4 py-2 bg-green-50 text-green-700 rounded-md text-sm font-medium hover:bg-green-100 transition-colors"
          >
            Danh sách học sinh
          </Link>
          {isAdmin && (
            <Link
              to="/billing"
              className="px-4 py-2 bg-yellow-50 text-yellow-700 rounded-md text-sm font-medium hover:bg-yellow-100 transition-colors"
            >
              Quản lý học phí
            </Link>
          )}
        </div>
      </div>
    </div>
  )
}

interface StatCardProps {
  label: string
  value: number | string
  href?: string
  color?: 'blue' | 'green' | 'yellow' | 'gray'
}

const colorMap: Record<NonNullable<StatCardProps['color']>, string> = {
  blue: 'text-blue-600',
  green: 'text-green-600',
  yellow: 'text-yellow-600',
  gray: 'text-gray-700',
}

function StatCard({ label, value, href, color = 'gray' }: StatCardProps) {
  const content = (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5">
      <p className="text-sm text-gray-500">{label}</p>
      <p className={`text-3xl font-bold mt-1 ${colorMap[color]}`}>{value}</p>
    </div>
  )

  if (href) {
    return (
      <Link to={href} className="block hover:shadow-md transition-shadow rounded-lg">
        {content}
      </Link>
    )
  }
  return content
}
