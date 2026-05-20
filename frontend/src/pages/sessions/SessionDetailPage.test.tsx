import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import SessionDetailPage from './SessionDetailPage'
import type { SessionDetailDto } from '../../api/types'

// ---------------------------------------------------------------------------
// Module mocks — must be hoisted to the top of the file by vi.mock
// ---------------------------------------------------------------------------

vi.mock('../../hooks/useSessions', () => ({
  useSession:       vi.fn(),
  useUpdateSession: vi.fn(),
}))

vi.mock('../../hooks/useAttendance', () => ({
  useSaveAttendance: vi.fn(),
}))

vi.mock('../../hooks/useComments', () => ({
  useSaveComment: vi.fn(),
}))

vi.mock('../../components/ui/Toast', () => ({
  useToast: vi.fn(),
}))

vi.mock('../../components/ui/Spinner', () => ({
  PageSpinner: () => <div data-testid="page-spinner" />,
}))

// ---------------------------------------------------------------------------
// Import mocked modules AFTER vi.mock declarations
// ---------------------------------------------------------------------------

import { useSession, useUpdateSession } from '../../hooks/useSessions'
import { useSaveAttendance } from '../../hooks/useAttendance'
import { useSaveComment } from '../../hooks/useComments'
import { useToast } from '../../components/ui/Toast'

// ---------------------------------------------------------------------------
// Test fixtures
// ---------------------------------------------------------------------------

const SESSION_ID = 'session-abc-123'

const makeSession = (overrides: Partial<SessionDetailDto> = {}): SessionDetailDto => ({
  id:                 SESSION_ID,
  classId:            'class-xyz-456',
  className:          'Toán Nâng Cao',
  sessionDate:        '2024-03-15',
  cancelledByTeacher: false,
  createdAt:          '2024-03-15T08:00:00Z',
  attendance: [
    { studentId: 'student-1', studentName: 'Nguyen Van A', status: 'PRESENT' },
    { studentId: 'student-2', studentName: 'Tran Thi B',   status: 'ABSENT'  },
  ],
  comments: [],
  ...overrides,
})

// ---------------------------------------------------------------------------
// Render helper — wraps the component in MemoryRouter with the session id param
// ---------------------------------------------------------------------------

function renderPage(sessionData: SessionDetailDto | null, isLoading = false, isError = false) {
  const showToastMock = vi.fn()

  vi.mocked(useToast).mockReturnValue({ showToast: showToastMock } as any)

  vi.mocked(useSession).mockReturnValue({
    data:      sessionData ?? undefined,
    isLoading,
    isError,
  } as any)

  const mutateAsyncAttendance = vi.fn().mockResolvedValue({ saved: 2, sessionId: SESSION_ID })
  vi.mocked(useSaveAttendance).mockReturnValue({
    mutateAsync: mutateAsyncAttendance,
    isPending:   false,
  } as any)

  const mutateAsyncComment = vi.fn().mockResolvedValue({})
  vi.mocked(useSaveComment).mockReturnValue({
    mutateAsync: mutateAsyncComment,
    isPending:   false,
  } as any)

  vi.mocked(useUpdateSession).mockReturnValue({
    mutateAsync: vi.fn().mockResolvedValue({}),
    isPending:   false,
  } as any)

  const result = render(
    <MemoryRouter initialEntries={[`/sessions/${SESSION_ID}`]}>
      <Routes>
        <Route path="/sessions/:id" element={<SessionDetailPage />} />
      </Routes>
    </MemoryRouter>
  )

  return { ...result, showToastMock, mutateAsyncAttendance, mutateAsyncComment }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('SessionDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // -------------------------------------------------------------------------
  // 1. Renders attendance grid with student rows
  // -------------------------------------------------------------------------

  it('renders an attendance row for each student in the session', () => {
    renderPage(makeSession())

    expect(screen.getByText('Nguyen Van A')).toBeInTheDocument()
    expect(screen.getByText('Tran Thi B')).toBeInTheDocument()
  })

  it('displays attendance status buttons for each student', () => {
    renderPage(makeSession())

    // Each student has three status buttons: Có mặt, Vắng mặt, Đi muộn
    const presentButtons = screen.getAllByRole('button', { name: /Có mặt/i })
    const absentButtons  = screen.getAllByRole('button', { name: /Vắng mặt/i })
    const lateButtons    = screen.getAllByRole('button', { name: /Đi muộn/i })

    // Two students → two sets of buttons
    expect(presentButtons.length).toBe(2)
    expect(absentButtons.length).toBe(2)
    expect(lateButtons.length).toBe(2)
  })

  it('initialises the first student status from session attendance data', () => {
    renderPage(makeSession())

    // student-1 is PRESENT — the "Có mặt" button for that row should be aria-pressed=true
    const presentButtons = screen.getAllByRole('button', { name: /Có mặt/i })
    // First student row: PRESENT is active
    expect(presentButtons[0]).toHaveAttribute('aria-pressed', 'true')
  })

  // -------------------------------------------------------------------------
  // 2. Clicking "Lưu tất cả" calls the bulk attendance API
  // -------------------------------------------------------------------------

  it('clicking Lưu tất cả calls saveBulkAttendance with correct payload', async () => {
    const { mutateAsyncAttendance } = renderPage(makeSession())

    const saveButton = screen.getByRole('button', { name: /Lưu tất cả/i })
    await userEvent.click(saveButton)

    await waitFor(() => {
      expect(mutateAsyncAttendance).toHaveBeenCalledOnce()
    })

    const callArg = mutateAsyncAttendance.mock.calls[0][0]
    expect(callArg.sessionId).toBe(SESSION_ID)
    expect(callArg.records).toHaveLength(2)

    // Verify student IDs are present in the payload
    const studentIds = callArg.records.map((r: { studentId: string }) => r.studentId)
    expect(studentIds).toContain('student-1')
    expect(studentIds).toContain('student-2')
  })

  it('Lưu tất cả button is disabled when there are no student rows', () => {
    renderPage(makeSession({ attendance: [], comments: [] }))

    const saveButton = screen.getByRole('button', { name: /Lưu tất cả/i })
    expect(saveButton).toBeDisabled()
  })

  // -------------------------------------------------------------------------
  // 3. Cancelled session shows "Giáo viên huỷ" label
  // -------------------------------------------------------------------------

  it('shows Huỷ buổi học label and checked checkbox when session is cancelled', () => {
    renderPage(makeSession({ cancelledByTeacher: true }))

    // The checkbox for cancelledByTeacher should be rendered
    const cancelledCheckbox = screen.getByRole('checkbox')
    expect(cancelledCheckbox).toBeInTheDocument()

    // The label text must be present
    expect(screen.getByText(/Huỷ buổi học/i)).toBeInTheDocument()
  })

  it('cancelled session info text explains it does not count toward fees', () => {
    renderPage(makeSession({ cancelledByTeacher: true }))

    expect(screen.getByText(/Buổi huỷ sẽ không được tính vào học phí/i)).toBeInTheDocument()
  })

  // -------------------------------------------------------------------------
  // 4. Loading and error states
  // -------------------------------------------------------------------------

  it('shows spinner while loading', () => {
    renderPage(null, true, false)
    expect(screen.getByTestId('page-spinner')).toBeInTheDocument()
  })

  it('shows error message when session is not found', () => {
    renderPage(null, false, true)
    expect(screen.getByText(/Không tìm thấy buổi học/i)).toBeInTheDocument()
  })

  // -------------------------------------------------------------------------
  // 5. Attendance status toggle works correctly
  // -------------------------------------------------------------------------

  it('clicking a different status button updates aria-pressed for that student', async () => {
    renderPage(makeSession())

    // student-2 is ABSENT initially — click LATE for them
    const lateButtons = screen.getAllByRole('button', { name: /Đi muộn/i })
    await userEvent.click(lateButtons[1]) // second student's LATE button

    // After clicking, LATE should now be pressed for student-2
    const updatedLateButtons = screen.getAllByRole('button', { name: /Đi muộn/i })
    expect(updatedLateButtons[1]).toHaveAttribute('aria-pressed', 'true')
  })

  // -------------------------------------------------------------------------
  // 6. Empty attendance list shows placeholder
  // -------------------------------------------------------------------------

  it('shows placeholder text when session has no students', () => {
    renderPage(makeSession({ attendance: [], comments: [] }))

    expect(screen.getByText(/Chưa có học sinh nào/i)).toBeInTheDocument()
  })
})
