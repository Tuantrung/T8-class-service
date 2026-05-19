import { useAuthStore } from '../../store/authStore'
import { useNavigate } from 'react-router-dom'

export default function TopBar() {
  const { user, clearAuth } = useAuthStore()
  const navigate = useNavigate()

  const handleLogout = () => {
    clearAuth()
    navigate('/login')
  }

  return (
    <header className="h-14 bg-white border-b border-gray-200 flex items-center justify-between px-6 shadow-sm">
      <div />
      <div className="flex items-center gap-4">
        <span className="text-sm text-gray-600">
          {user?.fullName ?? user?.email}
        </span>
        <span className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded">
          {user?.role}
        </span>
        <button
          onClick={handleLogout}
          className="text-sm text-red-600 hover:text-red-800 font-medium"
        >
          Logout
        </button>
      </div>
    </header>
  )
}
