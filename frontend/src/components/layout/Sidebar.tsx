import { NavLink } from 'react-router-dom'
import { cn } from '../../lib/utils'

const navItems = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/classes', label: 'Lớp học' },
  { to: '/students', label: 'Học sinh' },
  { to: '/billing', label: 'Học phí' },
]

export default function Sidebar() {
  return (
    <aside className="w-56 bg-white shadow-sm border-r border-gray-200 flex flex-col" aria-label="Điều hướng chính">
      <div className="p-4 border-b border-gray-200">
        <h1 className="text-lg font-bold text-gray-900">ClassService</h1>
        <p className="text-xs text-gray-400 mt-0.5">Quản lý lớp học</p>
      </div>
      <nav className="flex-1 p-3 space-y-1" aria-label="Menu">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) =>
              cn(
                'block px-3 py-2 rounded-md text-sm font-medium transition-colors',
                isActive
                  ? 'bg-blue-50 text-blue-700'
                  : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
              )
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
