import { useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { useClass } from '../../hooks/useClasses'
import { PageSpinner } from '../../components/ui/Spinner'
import { StatusBadge } from '../../components/ui/Badge'
import { formatCurrency } from '../../lib/utils'
import ClassStudentsTab from './tabs/ClassStudentsTab'
import ClassSessionsTab from './tabs/ClassSessionsTab'
import ClassGradesTab from './tabs/ClassGradesTab'
import ClassBillingTab from './tabs/ClassBillingTab'

type Tab = 'students' | 'sessions' | 'grades' | 'billing'

const tabs: { id: Tab; label: string }[] = [
  { id: 'students', label: 'Học sinh' },
  { id: 'sessions', label: 'Buổi học' },
  { id: 'grades', label: 'Điểm số' },
  { id: 'billing', label: 'Học phí' },
]

export default function ClassDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<Tab>('students')

  const { data: cls, isLoading, isError } = useClass(id!)

  if (isLoading) return <PageSpinner />

  if (isError || !cls) {
    return (
      <div className="p-6 bg-red-50 rounded-lg text-red-700 text-sm">
        Không tìm thấy lớp học.{' '}
        <button onClick={() => navigate('/classes')} className="underline">
          Quay lại
        </button>
      </div>
    )
  }

  return (
    <div>
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 mb-4" aria-label="Breadcrumb">
        <Link to="/classes" className="hover:text-gray-700">
          Lớp học
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900 font-medium">{cls.name}</span>
      </nav>

      {/* Header */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <h1 className="text-2xl font-bold text-gray-900">{cls.name}</h1>
              <StatusBadge status={cls.status} />
            </div>
            <p className="text-gray-500 text-sm">{cls.subject ?? 'Chưa có môn học'}</p>
          </div>
          <div className="text-right">
            <p className="text-xs text-gray-500">Học phí/buổi</p>
            <p className="text-xl font-bold text-blue-600">{formatCurrency(cls.ratePerSession)}</p>
          </div>
        </div>
        {cls.teacher && (
          <p className="text-sm text-gray-600 mt-2">
            Giáo viên: <span className="font-medium">{cls.teacher.fullName}</span>
          </p>
        )}
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200 mb-6" role="tablist" aria-label="Tabs lớp học">
        <div className="flex gap-1 -mb-px">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              role="tab"
              aria-selected={activeTab === tab.id}
              aria-controls={`tab-panel-${tab.id}`}
              id={`tab-${tab.id}`}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.id
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Tab Content */}
      <div
        id={`tab-panel-${activeTab}`}
        role="tabpanel"
        aria-labelledby={`tab-${activeTab}`}
      >
        {activeTab === 'students' && <ClassStudentsTab classId={id!} />}
        {activeTab === 'sessions' && <ClassSessionsTab classId={id!} />}
        {activeTab === 'grades' && <ClassGradesTab classId={id!} />}
        {activeTab === 'billing' && <ClassBillingTab classId={id!} />}
      </div>
    </div>
  )
}
