import { createBrowserRouter, Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import AppShell from './components/layout/AppShell'
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import DashboardPage from './pages/dashboard/DashboardPage'
import ClassesPage from './pages/classes/ClassesPage'
import ClassDetailPage from './pages/classes/ClassDetailPage'
import StudentsPage from './pages/students/StudentsPage'
import StudentDetailPage from './pages/students/StudentDetailPage'
import SessionDetailPage from './pages/sessions/SessionDetailPage'
import GradesPage from './pages/grades/GradesPage'
import BillingPage from './pages/billing/BillingPage'
import BillDetailPage from './pages/billing/BillDetailPage'

function AuthGuard() {
  const token = useAuthStore((s) => s.token)
  if (!token) return <Navigate to="/login" replace />
  return (
    <AppShell>
      <Outlet />
    </AppShell>
  )
}

function AdminGuard() {
  const role = useAuthStore((s) => s.user?.role)
  if (role !== 'ADMIN') return <Navigate to="/" replace />
  return <Outlet />
}

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/register',
    element: <RegisterPage />,
  },
  {
    path: '/',
    element: <AuthGuard />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'classes', element: <ClassesPage /> },
      { path: 'classes/:id', element: <ClassDetailPage /> },
      { path: 'students', element: <StudentsPage /> },
      { path: 'students/:id', element: <StudentDetailPage /> },
      { path: 'sessions/:id', element: <SessionDetailPage /> },
      { path: 'grades', element: <GradesPage /> },
      {
        element: <AdminGuard />,
        children: [
          { path: 'billing', element: <BillingPage /> },
          { path: 'billing/:id', element: <BillDetailPage /> },
        ],
      },
    ],
  },
])
