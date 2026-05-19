// TODO Track 5: Implement full student list with search, Excel import
import { useQuery } from '@tanstack/react-query'
import { studentsApi } from '../../api/endpoints/students'
import { Link } from 'react-router-dom'

export default function StudentsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['students'],
    queryFn: () => studentsApi.list(),
  })

  if (isLoading) return <p className="text-gray-500">Loading...</p>

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Students</h1>
        <div className="flex gap-2">
          <button className="border border-gray-300 text-gray-700 px-4 py-2 rounded-md text-sm hover:bg-gray-50">
            Import Excel
          </button>
          <button className="bg-blue-600 text-white px-4 py-2 rounded-md text-sm hover:bg-blue-700">
            Add Student
          </button>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Name</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Phone</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Parent Phone</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {data?.content.map((s) => (
              <tr key={s.id} className="hover:bg-gray-50">
                <td className="px-4 py-3 font-medium text-gray-900">{s.fullName}</td>
                <td className="px-4 py-3 text-gray-600">{s.phone ?? '-'}</td>
                <td className="px-4 py-3 text-gray-600">{s.parentPhone ?? '-'}</td>
                <td className="px-4 py-3">
                  <Link to={`/students/${s.id}`} className="text-blue-600 hover:underline text-xs">
                    View
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
