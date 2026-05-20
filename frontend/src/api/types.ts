// TypeScript interfaces matching the API contract.
// This is the single source of truth for all API shapes.

export interface ApiResponse<T> {
  data: T
  message?: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

// Auth
export type UserRole = 'ADMIN' | 'TEACHER'

export interface UserDto {
  id: string
  tenantId: string
  email: string
  fullName: string
  role: UserRole
}

export interface LoginRequest {
  email: string
  password: string
  tenantId: string
}

export interface LoginResponse {
  accessToken: string
  user: UserDto
}

export interface RegisterTenantRequest {
  centerName: string
  adminEmail: string
  adminPassword: string
  adminFullName: string
}

// Classes
export type ClassStatus = 'ACTIVE' | 'ARCHIVED'

export interface ClassDto {
  id: string
  tenantId: string
  name: string
  subject?: string
  teacherId: string
  teacher?: {
    id: string
    fullName: string
    email: string
  }
  ratePerSession: number
  status: ClassStatus
  studentCount?: number
  createdAt: string
}

export interface CreateClassRequest {
  name: string
  subject?: string
  teacherId: string
  ratePerSession: number
}

export interface UpdateClassRequest {
  name: string
  subject?: string
  teacherId: string
  ratePerSession: number
  status: ClassStatus
}

// Students
export interface StudentDto {
  id: string
  tenantId?: string
  fullName: string
  phone?: string
  parentPhone?: string
  notes?: string
  createdAt: string
}

export interface CreateStudentRequest {
  fullName: string
  phone?: string
  parentPhone?: string
  notes?: string
}

export interface ImportError {
  row: number
  message: string
}

export interface ImportResult {
  imported: number
  skipped: number
  errors: ImportError[]
}

// Sessions
export interface SessionDto {
  id: string
  tenantId?: string
  classId: string
  className?: string
  sessionDate: string
  startTime?: string
  endTime?: string
  topic?: string
  cancelledByTeacher: boolean
  attendanceCount?: number
  totalStudents?: number
  createdAt: string
}

export interface SessionDetailDto extends SessionDto {
  attendance: {
    studentId: string
    studentName: string
    status: AttendanceStatus
  }[]
  comments: {
    studentId: string
    studentName: string
    body: string
    authorId: string
    authorName: string
    updatedAt: string
  }[]
}

export interface CreateSessionRequest {
  classId: string
  sessionDate: string
  startTime?: string
  endTime?: string
  topic?: string
  cancelledByTeacher?: boolean
}

export interface UpdateSessionRequest {
  sessionDate: string
  startTime?: string
  endTime?: string
  topic?: string
  cancelledByTeacher: boolean
}

// Attendance
export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE'

export interface AttendanceDto {
  id: string
  sessionId: string
  studentId: string
  studentName?: string
  status: AttendanceStatus
}

export interface AttendanceRecord {
  studentId: string
  status: AttendanceStatus
}

export interface BulkAttendanceRequest {
  sessionId: string
  records: AttendanceRecord[]
}

// Comments
export interface CommentDto {
  id: string
  sessionId: string
  studentId: string
  studentName?: string
  authorId: string
  authorName?: string
  body: string
  createdAt: string
  updatedAt: string
}

// Grades
export interface GradeDto {
  id: string
  classId: string
  studentId: string
  studentName?: string
  examName: string
  examDate?: string
  score?: number
  maxScore?: number
  percentage?: number
  notes?: string
  createdAt: string
}

export interface CreateGradeRequest {
  classId: string
  studentId: string
  examName: string
  examDate?: string
  score?: number
  maxScore?: number
  notes?: string
}

// Billing
export type BillStatus = 'DRAFT' | 'ISSUED' | 'PAID'

export interface BillDto {
  id: string
  tenantId?: string
  studentId: string
  studentName?: string
  classId: string
  className?: string
  billingMonth: string
  sessionsTotal: number
  sessionsAttended: number
  ratePerSession: number
  totalAmount: number
  status: BillStatus
  createdAt: string
  updatedAt: string
}

export interface BillDetailDto extends BillDto {
  sessions: {
    sessionId: string
    sessionDate: string
    topic?: string
    cancelledByTeacher: boolean
    attendanceStatus: AttendanceStatus | null
    countedInBill: boolean
  }[]
}

export interface GenerateBillsRequest {
  classId: string
  month: string // YYYY-MM
}

export interface GenerateBillsResult {
  generated: number
  month: string
  classId: string
  bills: BillDto[]
}
