import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import DashboardPage from './DashboardPage'
import type { UserDto, UserRole } from '../../api/types'

vi.mock('../../api/endpoints/classes', () => ({
  classesApi: { list: vi.fn().mockResolvedValue({ totalElements: 3 }) },
}))

vi.mock('../../api/endpoints/students', () => ({
  studentsApi: { list: vi.fn().mockResolvedValue({ totalElements: 42 }) },
}))

vi.mock('../../api/endpoints/bills', () => ({
  billsApi: { list: vi.fn().mockResolvedValue({ totalElements: 7 }) },
}))

vi.mock('../../store/authStore', () => ({
  useAuthStore: vi.fn(),
}))

vi.mock('../../components/ui/Spinner', () => ({
  PageSpinner: () => <div data-testid="page-spinner" />,
}))

import { useAuthStore } from '../../store/authStore'
import { billsApi } from '../../api/endpoints/bills'

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

function makeUser(role: UserRole): UserDto {
  return {
    id: 'user-1',
    tenantId: 'tenant-1',
    email: 'user@example.com',
    fullName: 'Test User',
    role,
  }
}

function renderAs(role: UserRole) {
  const user = makeUser(role)
  vi.mocked(useAuthStore).mockImplementation((selector: any) =>
    selector({ token: 'jwt', user, setAuth: vi.fn(), clearAuth: vi.fn() }),
  )

  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('DashboardPage role-based visibility', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('hides billing tile and quick-access link for TEACHER role', async () => {
    renderAs('TEACHER')

    await screen.findByText('Lớp đang hoạt động')

    expect(screen.queryByText('Học phí chờ xử lý')).not.toBeInTheDocument()
    expect(screen.queryByText('Quản lý học phí')).not.toBeInTheDocument()
    expect(billsApi.list).not.toHaveBeenCalled()
  })

  it('shows billing tile and quick-access link for ADMIN role', async () => {
    renderAs('ADMIN')

    await screen.findByText('Học phí chờ xử lý')
    expect(screen.getByText('Quản lý học phí')).toBeInTheDocument()
    expect(billsApi.list).toHaveBeenCalled()
  })
})
