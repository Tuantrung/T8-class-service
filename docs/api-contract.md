# API Contract — Tutoring Class Management Service

**Base path:** `/api`
**Content-Type:** `application/json` (all requests and responses)
**Auth header:** `Authorization: Bearer <accessToken>` (required on all endpoints except `/auth/login` and `/auth/register-tenant`)

---

## Common Types

```typescript
// Every success response is wrapped in this envelope
interface ApiResponse<T> {
  data: T;
  message: string;
}

// Every error response uses this shape
interface ApiError {
  error: string;         // machine-readable error code e.g. "ENTITY_NOT_FOUND"
  message: string;       // human-readable description
  timestamp: string;     // ISO-8601
  fieldErrors?: {        // only present for validation errors (400)
    field: string;
    message: string;
  }[];
}

// Paginated list
interface PageResponse<T> {
  data: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// Attendance status
type AttendanceStatus = "PRESENT" | "ABSENT" | "LATE";

// User roles
type UserRole = "ADMIN" | "TEACHER";

// Bill status
type BillStatus = "DRAFT" | "ISSUED" | "PAID";

// Class status
type ClassStatus = "ACTIVE" | "ARCHIVED";
```

---

## /auth

### POST /api/auth/register-tenant
Register a new tutoring center (tenant) with its first admin user. No auth required.

**Request:**
```typescript
interface RegisterTenantRequest {
  tenantName: string;      // tutoring center name, required
  adminEmail: string;      // required, must be valid email
  adminPassword: string;   // required, min 8 chars
  adminFullName: string;   // required
}
```

**Response 201:**
```typescript
interface RegisterTenantResponse {
  tenantId: string;        // UUID
  userId: string;          // UUID of created admin
  email: string;
  fullName: string;
  role: "ADMIN";
}
```

**Errors:** 400 (validation), 409 (email already registered)

---

### POST /api/auth/login
Authenticate and receive a JWT access token. No auth required.

**Request:**
```typescript
interface LoginRequest {
  email: string;
  password: string;
}
```
Note: `tenantId` is derived from the JWT of a previously-issued token or passed as a query param `?tenantId=<uuid>` for the initial login. In MVP, tenantId is included in the request body.

```typescript
interface LoginRequest {
  email: string;
  password: string;
  tenantId: string;        // UUID — which center to log in to
}
```

**Response 200:**
```typescript
interface LoginResponse {
  accessToken: string;     // JWT, 8h expiry
  tokenType: "Bearer";
  expiresIn: number;       // seconds (28800)
  user: {
    id: string;
    email: string;
    fullName: string;
    role: UserRole;
    tenantId: string;
  };
}
```

**Errors:** 400 (validation), 401 (bad credentials), 404 (tenant not found)

---

### GET /api/auth/me
Get current authenticated user profile.

**Auth:** Required

**Response 200:**
```typescript
interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  tenantId: string;
  tenantName: string;
}
```

**Errors:** 401

---

### POST /api/auth/change-password
Change current user's password.

**Auth:** Required

**Request:**
```typescript
interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;     // min 8 chars
}
```

**Response 200:**
```typescript
{ message: "Password changed successfully" }
```

**Errors:** 400, 401 (wrong current password)

---

### POST /api/auth/users
Create a new user within the current tenant. ADMIN only.

**Auth:** Required (ADMIN role)

**Request:**
```typescript
interface CreateUserRequest {
  email: string;
  password: string;        // min 8 chars
  fullName: string;
  role: UserRole;
}
```

**Response 201:**
```typescript
interface UserDto {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  createdAt: string;       // ISO-8601
}
```

**Errors:** 400, 403 (not ADMIN), 409 (email conflict within tenant)

---

### GET /api/auth/users
List all users in the current tenant. ADMIN only.

**Auth:** Required (ADMIN role)

**Query params:** `page` (default 0), `size` (default 20)

**Response 200:** `PageResponse<UserDto>`

---

## /classes

### GET /api/classes
List all classes for the current tenant.

**Auth:** Required

**Query params:**
- `page` (default 0), `size` (default 20)
- `status` — filter by ClassStatus (optional)
- `teacherId` — filter by teacher UUID (optional)
- `q` — search by class name (optional)

**Response 200:** `PageResponse<ClassDto>`

```typescript
interface ClassDto {
  id: string;
  name: string;
  subject: string | null;
  teacher: {
    id: string;
    fullName: string;
    email: string;
  };
  ratePerSession: number;
  status: ClassStatus;
  studentCount: number;
  createdAt: string;
}
```

---

### POST /api/classes
Create a new class.

**Auth:** Required (ADMIN or TEACHER)

**Request:**
```typescript
interface CreateClassRequest {
  name: string;            // required
  subject?: string;
  teacherId: string;       // UUID, required — must belong to same tenant
  ratePerSession: number;  // required, >= 0
}
```

**Response 201:** `ClassDto`

**Errors:** 400, 404 (teacher not found)

---

### GET /api/classes/{classId}
Get class detail.

**Auth:** Required

**Response 200:** `ClassDto`

**Errors:** 404

---

### PUT /api/classes/{classId}
Update a class.

**Auth:** Required (ADMIN, or TEACHER who owns the class)

**Request:**
```typescript
interface UpdateClassRequest {
  name: string;
  subject?: string;
  teacherId: string;
  ratePerSession: number;
  status: ClassStatus;
}
```

**Response 200:** `ClassDto`

**Errors:** 400, 403, 404

---

### DELETE /api/classes/{classId}
Archive (soft-delete) a class. Sets status to ARCHIVED.

**Auth:** Required (ADMIN only)

**Response 204:** No body

**Errors:** 403, 404

---

### GET /api/classes/{classId}/students
List students enrolled in a class.

**Auth:** Required

**Response 200:** `ApiResponse<StudentDto[]>`

```typescript
interface StudentDto {
  id: string;
  fullName: string;
  phone: string | null;
  parentPhone: string | null;
  notes: string | null;
  schoolName: string | null;  // added V6 migration
  createdAt: string;
}
```

---

### POST /api/classes/{classId}/students
Enroll one or more students in a class.

**Auth:** Required (ADMIN or TEACHER who owns the class)

**Request:**
```typescript
interface EnrollStudentsRequest {
  studentIds: string[];    // array of student UUIDs
}
```

**Response 200:**
```typescript
interface EnrollResult {
  enrolled: string[];      // IDs successfully enrolled
  alreadyEnrolled: string[];
  notFound: string[];
}
```

**Errors:** 400, 404

---

### DELETE /api/classes/{classId}/students/{studentId}
Remove a student from a class (unenroll).

**Auth:** Required (ADMIN or TEACHER who owns the class)

**Response 204:** No body

**Errors:** 404

---

## /students

### GET /api/students
List all students for the current tenant.

**Auth:** Required

**Query params:** `page` (default 0), `size` (default 20), `q` (name/phone search)

**Response 200:** `PageResponse<StudentDto>`

---

### POST /api/students
Create a student.

**Auth:** Required

**Request:**
```typescript
interface CreateStudentRequest {
  fullName: string;        // required
  phone?: string;
  parentPhone?: string;
  notes?: string;
  schoolName?: string;     // added V6 migration
}
```

**Response 201:** `StudentDto`

**Errors:** 400

---

### GET /api/students/{studentId}
Get student detail.

**Auth:** Required

**Response 200:** `StudentDto`

**Errors:** 404

---

### PUT /api/students/{studentId}
Update a student.

**Auth:** Required

**Request:** Same fields as `CreateStudentRequest` (all optional except fullName)

**Response 200:** `StudentDto`

**Errors:** 400, 404

---

### DELETE /api/students/{studentId}
Delete a student. Only allowed if student has no sessions/bills.

**Auth:** Required (ADMIN only)

**Response 204:** No body

**Errors:** 403, 404, 409 (student has billing history)

---

### POST /api/students/import
Import students from an Excel file (.xlsx).

**Auth:** Required

**Request:** `multipart/form-data`
- `file`: the `.xlsx` file (required)

**Response 200:**
```typescript
interface ImportResult {
  imported: number;        // count of students created
  skipped: number;         // blank rows skipped
  errors: {
    row: number;
    message: string;
  }[];
}
```

**Errors:** 400 (not xlsx), 422 (file parse errors — errors array populated)

---

### GET /api/students/import-template
Download the Excel import template.

**Auth:** Required

**Response 200:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- `Content-Disposition: attachment; filename="student-import-template.xlsx"`

---

## /sessions

### GET /api/sessions
List sessions. Filter by class.

**Auth:** Required

**Query params:**
- `classId` — required (filter sessions for a specific class)
- `from` — ISO date string (optional, e.g. `2024-03-01`)
- `to` — ISO date string (optional)
- `page` (default 0), `size` (default 30)

**Response 200:** `PageResponse<SessionDto>`

```typescript
interface SessionDto {
  id: string;
  classId: string;
  className: string;
  sessionDate: string;      // ISO date "YYYY-MM-DD"
  startTime: string | null; // "HH:mm"
  endTime: string | null;
  topic: string | null;
  progressNotes: string | null; // teaching content description — added V6 migration
  cancelledByTeacher: boolean;
  attendanceCount: number;  // number of PRESENT students
  totalStudents: number;
  createdAt: string;
}
```

---

### POST /api/sessions
Create a session for a class.

**Auth:** Required (ADMIN or TEACHER who owns the class)

**Request:**
```typescript
interface CreateSessionRequest {
  classId: string;         // UUID, required
  sessionDate: string;     // ISO date "YYYY-MM-DD", required
  startTime?: string;      // "HH:mm"
  endTime?: string;
  topic?: string;
  cancelledByTeacher?: boolean; // default false
}
```

**Response 201:** `SessionDto`

**Errors:** 400, 404 (class not found)

---

### GET /api/sessions/{sessionId}
Get session detail including attendance and comments.

**Auth:** Required

**Response 200:**
```typescript
interface SessionDetailDto extends SessionDto {
  attendance: {
    studentId: string;
    studentName: string;
    status: AttendanceStatus;
  }[];
  comments: {
    studentId: string;
    studentName: string;
    body: string;
    authorId: string;
    authorName: string;
    updatedAt: string;
  }[];
}
```

---

### PUT /api/sessions/{sessionId}
Update session metadata.

**Auth:** Required (ADMIN or TEACHER who owns the class)

**Request:**
```typescript
interface UpdateSessionRequest {
  sessionDate: string;
  startTime?: string;
  endTime?: string;
  topic?: string;
  progressNotes?: string;  // teaching content description — added V6 migration
  cancelledByTeacher: boolean;
}
```

**Response 200:** `SessionDto`

**Errors:** 400, 404

---

### DELETE /api/sessions/{sessionId}
Delete a session. Only if session has no attendance records.

**Auth:** Required (ADMIN only)

**Response 204:** No body

**Errors:** 403, 404, 409 (has attendance)

---

## /attendance

### GET /api/attendance
Get attendance records for a session.

**Auth:** Required

**Query params:** `sessionId` — required

**Response 200:**
```typescript
interface AttendanceDto {
  id: string;
  sessionId: string;
  studentId: string;
  studentName: string;
  status: AttendanceStatus;
}
```
`ApiResponse<AttendanceDto[]>`

---

### POST /api/attendance/bulk
Save attendance for an entire session (creates or updates all records).

**Auth:** Required (ADMIN or TEACHER who owns the class)

**Request:**
```typescript
interface BulkAttendanceRequest {
  sessionId: string;
  records: {
    studentId: string;
    status: AttendanceStatus;
  }[];
}
```

**Response 200:**
```typescript
interface BulkAttendanceResponse {
  saved: number;
  sessionId: string;
}
```

**Errors:** 400, 404

---

### PATCH /api/attendance/{attendanceId}
Update a single attendance record.

**Auth:** Required

**Request:**
```typescript
interface UpdateAttendanceRequest {
  status: AttendanceStatus;
}
```

**Response 200:** `AttendanceDto`

**Errors:** 400, 404

---

## /comments

### GET /api/comments
Get comments for a session, optionally filtered by student.

**Auth:** Required

**Query params:** `sessionId` — required, `studentId` — optional

**Response 200:**
```typescript
interface CommentDto {
  id: string;
  sessionId: string;
  studentId: string;
  studentName: string;
  body: string;
  authorId: string;
  authorName: string;
  createdAt: string;
  updatedAt: string;
}
```
`ApiResponse<CommentDto[]>`

---

### POST /api/comments
Create a comment for a student on a session.

**Auth:** Required

**Request:**
```typescript
interface CreateCommentRequest {
  sessionId: string;       // required
  studentId: string;       // required
  body: string;            // required, non-empty
}
```

**Response 201:** `CommentDto`

**Errors:** 400, 404, 409 (comment already exists for this student/session — use PUT)

---

### PUT /api/comments/{commentId}
Update a comment body.

**Auth:** Required (author or ADMIN)

**Request:**
```typescript
interface UpdateCommentRequest {
  body: string;            // required, non-empty
}
```

**Response 200:** `CommentDto`

**Errors:** 400, 403, 404

---

### DELETE /api/comments/{commentId}
Delete a comment.

**Auth:** Required (author or ADMIN)

**Response 204:** No body

**Errors:** 403, 404

---

## /grades

### GET /api/grades
List grades for a class, optionally filtered by student.

**Auth:** Required

**Query params:**
- `classId` — required
- `studentId` — optional
- `page` (default 0), `size` (default 20)

**Response 200:** `PageResponse<GradeDto>`

```typescript
interface GradeDto {
  id: string;
  classId: string;
  studentId: string;
  studentName: string;
  examName: string;
  examDate: string | null;  // ISO date
  score: number | null;
  maxScore: number | null;
  percentage: number | null; // computed: score / maxScore * 100
  notes: string | null;
  createdAt: string;
}
```

---

### POST /api/grades
Create a grade entry.

**Auth:** Required (ADMIN or TEACHER who owns the class)

**Request:**
```typescript
interface CreateGradeRequest {
  classId: string;         // required
  studentId: string;       // required
  examName: string;        // required
  examDate?: string;       // ISO date
  score?: number;          // >= 0
  maxScore?: number;       // > 0
  notes?: string;
}
```

**Response 201:** `GradeDto`

**Errors:** 400, 404, 409 (duplicate exam for same student in same class)

---

### PUT /api/grades/{gradeId}
Update a grade entry.

**Auth:** Required (ADMIN or TEACHER who owns the class)

**Request:**
```typescript
interface UpdateGradeRequest {
  examName: string;
  examDate?: string;
  score?: number;
  maxScore?: number;
  notes?: string;
}
```

**Response 200:** `GradeDto`

**Errors:** 400, 403, 404

---

### DELETE /api/grades/{gradeId}
Delete a grade entry.

**Auth:** Required (ADMIN or TEACHER who owns the class)

**Response 204:** No body

**Errors:** 403, 404

---

### POST /api/grades/import
Import grades from an Excel file for a specific exam. File columns: `Họ tên | Điểm`. Row 0 is the header and is skipped. Matches students by full name (case-insensitive) within the enrolled class. Creates a new grade if none exists for the (classId, studentId, examName) combo; otherwise updates the existing score.

**Auth:** Required (ADMIN or TEACHER who owns the class)

**Query params:**
- `classId` — required, UUID
- `examName` — required, string
- `examDate` — optional, ISO date `YYYY-MM-DD`

**Request:** `multipart/form-data`, field `file` (.xlsx)

**Response 200:**
```typescript
interface GradeImportResult {
  imported: number;  // new grade records created
  updated: number;   // existing grade records updated
  skipped: number;   // blank rows skipped
  errors: { rowNumber: number; message: string }[];
}
```

**Errors:** 400 (empty/unreadable file), 404 (class not found)

---

### GET /api/grades/import/template
Download an Excel template pre-filled with enrolled student names for grade import.

**Auth:** Required

**Query params:**
- `classId` — required, UUID

**Response 200:** `.xlsx` file — `Content-Disposition: attachment; filename="grade-template.xlsx"`

Columns: `Họ tên | Điểm`

---

## /bills

> **All `/api/bills/**` endpoints require `ADMIN` role.** Teachers do not have access to billing data (enforced via `@PreAuthorize("hasRole('ADMIN')")` on `BillController`).

### GET /api/bills
List bills for the current tenant.

**Auth:** Required (ADMIN only)

**Query params:**
- `month` — required, format `YYYY-MM` (e.g. `2024-03`)
- `classId` — optional
- `studentId` — optional
- `status` — optional (`DRAFT` | `ISSUED` | `PAID`)
- `page` (default 0), `size` (default 20)

**Response 200:** `PageResponse<BillDto>`

```typescript
interface BillDto {
  id: string;
  studentId: string;
  studentName: string;
  classId: string;
  className: string;
  billingMonth: string;    // "YYYY-MM"
  sessionsTotal: number;   // all sessions in month for class
  sessionsAttended: number; // sessions attended by this student (non-cancelled)
  ratePerSession: number;
  totalAmount: number;     // sessionsAttended * ratePerSession
  status: BillStatus;
  createdAt: string;
  updatedAt: string;
}
```

---

### POST /api/bills/generate
Generate (or regenerate) bills for all students in a class for a given month.

**Auth:** Required (ADMIN only)

**Request:**
```typescript
interface GenerateBillsRequest {
  classId: string;         // required
  month: string;           // "YYYY-MM", required
}
```

**Response 200:**
```typescript
interface GenerateBillsResult {
  generated: number;       // bills created or updated
  month: string;
  classId: string;
  bills: BillDto[];
}
```

**Errors:** 400, 404

**Business rules applied by billing engine:**
- Only sessions where `cancelledByTeacher = false` count toward the bill.
- `sessionsAttended` = count of attendance records with status `PRESENT` or `LATE` in the month for this student/class.
- `totalAmount = sessionsAttended * ratePerSession` (rate snapshotted from class at generation time).
- Existing DRAFT bills for same (student, class, month) are overwritten. ISSUED/PAID bills are not overwritten.

---

### GET /api/bills/{billId}
Get a single bill with line-item session detail.

**Auth:** Required (ADMIN only)

**Response 200:**
```typescript
interface BillDetailDto extends BillDto {
  sessions: {
    sessionId: string;
    sessionDate: string;
    topic: string | null;
    cancelledByTeacher: boolean;
    attendanceStatus: AttendanceStatus | null;  // null if no record
    countedInBill: boolean;  // true if attended and not cancelled
  }[];
}
```

**Errors:** 404

---

### PATCH /api/bills/{billId}/status
Update the status of a bill (e.g. mark as ISSUED or PAID).

**Auth:** Required (ADMIN only)

**Request:**
```typescript
interface UpdateBillStatusRequest {
  status: BillStatus;
}
```

**Response 200:** `BillDto`

**Errors:** 400 (invalid status transition), 403, 404

---

### GET /api/bills/{billId}/pdf
Export a bill as a PDF.

**Auth:** Required (ADMIN only)

**Response 200:**
- `Content-Type: application/pdf`
- `Content-Disposition: attachment; filename="bill-{YYYY-MM}-{studentName}.pdf"`

Binary PDF stream.

**Errors:** 404

---

### GET /api/bills/export
Export all bills for a month as a ZIP of PDFs (one PDF per student per class).

**Auth:** Required (ADMIN only)

**Query params:** `month` — required `YYYY-MM`, `classId` — optional

**Response 200:**
- `Content-Type: application/zip`
- `Content-Disposition: attachment; filename="bills-{YYYY-MM}.zip"`

**Errors:** 400, 403

---

## HTTP Status Code Reference

| Code | Meaning |
|------|---------|
| 200 | OK |
| 201 | Created |
| 204 | No Content (successful delete) |
| 400 | Bad Request — validation failed |
| 401 | Unauthorized — missing or invalid/expired token |
| 403 | Forbidden — authenticated but insufficient permission |
| 404 | Not Found — resource does not exist in this tenant |
| 409 | Conflict — duplicate resource |
| 422 | Unprocessable Entity — semantically invalid (e.g. Excel parse errors) |
| 500 | Internal Server Error |

---

## Notes for Frontend Developers

1. All timestamps are ISO-8601 UTC strings.
2. All monetary amounts are plain `number` (JavaScript float). Display with locale formatting.
3. Pagination: `page` is 0-indexed. UI should display as 1-indexed to users.
4. The `tenantId` in the JWT claim identifies the tenant for every request — do not pass it in request bodies except for `/api/auth/login`.
5. On receiving a 401, clear the auth store and redirect to `/login`.
6. On receiving a 403, show a toast "You don't have permission for this action."
7. Axios base URL: set `VITE_API_BASE_URL` env var; empty string in production (same origin via nginx).
