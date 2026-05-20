import { useParams, useNavigate, Link } from 'react-router-dom'
import { useStudent } from '../../hooks/useStudents'
import { PageSpinner } from '../../components/ui/Spinner'

export default function StudentDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const { data: student, isLoading, isError } = useStudent(id!)

  if (isLoading) return <PageSpinner />

  if (isError || !student) {
    return (
      <div className="p-6 bg-red-50 rounded-lg text-red-700 text-sm">
        Không tìm thấy học sinh.{' '}
        <button onClick={() => navigate('/students')} className="underline">
          Quay lại
        </button>
      </div>
    )
  }

  return (
    <div>
      <nav className="text-sm text-gray-500 mb-4" aria-label="Breadcrumb">
        <Link to="/students" className="hover:text-gray-700">
          Học sinh
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900 font-medium">{student.fullName}</span>
      </nav>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <h1 className="text-2xl font-bold text-gray-900 mb-4">{student.fullName}</h1>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <InfoItem label="SĐT học sinh" value={student.phone ?? '-'} />
          <InfoItem label="SĐT phụ huynh" value={student.parentPhone ?? '-'} />
          {student.notes && (
            <div className="sm:col-span-2">
              <InfoItem label="Ghi chú" value={student.notes} />
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-gray-50 rounded-lg p-4">
      <p className="text-xs text-gray-500 mb-1">{label}</p>
      <p className="text-sm font-medium text-gray-900">{value}</p>
    </div>
  )
}
