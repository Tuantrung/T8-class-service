# Implementation Plan — Tutoring Class Management Service

## Parallel Track Map

```
Track 1 (feature/db-foundation)    DB schema, Flyway migrations, JPA entities, tenant setup, auth
         │
         ├──────────────────────────────────────────────────────────────┐
         ▼                                                              │
Track 2 (feature/api-core)         Classes, Students, Sessions,        │
         │                         Attendance, Comments, Grades APIs    │
         │                                                              │
         ▼                                                              │
Track 3 (feature/billing)          Bill engine, PDF export, ZIP export  │
                                                                        │
         ┌──────────────────────────────────────────────────────────────┘
         │  (API contract doc is the handshake — no code dependency)
         ▼
Track 4 (feature/frontend-foundation)  React app scaffold, routing, auth UI, layout
         │
         ▼
Track 5 (feature/frontend-core)    Class management, student roster, Excel import,
         │                         session recording, attendance grid, comments, grades
         │
         ▼
Track 6 (feature/frontend-billing) Bill list, bill detail, PDF download, bulk export
```

### Dependency graph

```
Track 1 ──► Track 2 ──► Track 3
Track 1 ──► Track 4 (parallel — both start from day 1)
Track 4 ──► Track 5 (needs frontend shell)
Track 2 ──► Track 5 (needs real API endpoints)
Track 3 ──► Track 6 (needs billing API)
Track 5 ──► Track 6 (needs core UI patterns)
```

**Track 4 can start the same day as Track 1** — it only depends on the API contract document, not on working backend code.

---

## Track 1 — Backend Foundation

**Owner type:** Backend developer
**Branch:** `feature/db-foundation`
**Complexity:** M
**Depends on:** Nothing (starts immediately)

### Purpose
Establish the entire data layer: database migrations, JPA entities, tenant context infrastructure, Spring Security config, and JWT auth. Every other backend track depends on this being merged first.

### Tasks (in order)

**1.1 — Project dependencies (pom.xml)**
Add to `pom.xml`:
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `postgresql` driver
- `flyway-core` + `flyway-database-postgresql`
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.12.x)
- `itext7-core` (7.2.x)
- `poi-ooxml` (5.3.x)
- `lombok`
- `spring-boot-starter-test` + `testcontainers` (test scope)

Files: `pom.xml`

**1.2 — Application properties**
Files:
- `src/main/resources/application.yml` — datasource, JPA settings, JWT config, file upload limits
- `src/main/resources/application-dev.yml` — dev DB URL, logging level DEBUG
- `src/main/resources/application-prod.yml` — prod profile overrides

Key properties:
```yaml
spring.datasource.url: ${DB_URL}
spring.jpa.hibernate.ddl-auto: validate
spring.flyway.enabled: true
app.jwt.secret: ${JWT_SECRET}
app.jwt.expiry-hours: ${JWT_EXPIRY_HOURS:8}
app.pdf.export-dir: ${PDF_EXPORT_DIR:/tmp/pdf-exports}
```

**1.3 — Flyway migrations**
Files under `src/main/resources/db/migration/`:
- `V1__create_tenant_and_users.sql` — `tenant`, `app_user` tables
- `V2__create_classes_and_students.sql` — `class`, `student`, `class_student` tables
- `V3__create_sessions_and_attendance.sql` — `session`, `attendance` tables
- `V4__create_comments_and_grades.sql` — `comment`, `grade` tables
- `V5__create_bills.sql` — `bill` table
- `V6__seed_dev_data.sql` — dev seed data (demo tenant, admin user, sample classes/students)

Schema mirrors exactly the tables in `architecture.md §4`.

**1.4 — Common infrastructure**
Files:
- `src/main/java/com/classservice/common/TenantEntity.java` — `@MappedSuperclass` with `tenantId UUID` field + Hibernate `@FilterDef` + `@Filter`
- `src/main/java/com/classservice/common/TenantContext.java` — `ThreadLocal<UUID>` with `get()`, `set()`, `clear()` statics
- `src/main/java/com/classservice/common/ApiResponse.java` — `{ T data, String message }`
- `src/main/java/com/classservice/common/PageResponse.java` — pagination wrapper
- `src/main/java/com/classservice/common/GlobalExceptionHandler.java` — `@ControllerAdvice` mapping all exceptions to HTTP codes + `ApiError` shape
- `src/main/java/com/classservice/common/TenantAwareRepositoryImpl.java` — custom JPA repository base class that enables the Hibernate `tenantFilter` for every `EntityManager` call

**1.5 — Tenant entity + repository**
Files:
- `src/main/java/com/classservice/tenant/Tenant.java` — JPA entity (no tenant filter — Tenant table is not scoped)
- `src/main/java/com/classservice/tenant/TenantRepository.java`

**1.6 — User entity + repository**
Files:
- `src/main/java/com/classservice/auth/AppUser.java` — JPA entity extending `TenantEntity`
- `src/main/java/com/classservice/auth/UserRepository.java`
- `src/main/java/com/classservice/auth/UserRole.java` — enum `ADMIN | TEACHER`

**1.7 — JWT utility**
Files:
- `src/main/java/com/classservice/auth/JwtUtil.java` — `generateToken(userId, tenantId, role)`, `parseToken(token)`, `validateToken(token)`

**1.8 — Security configuration**
Files:
- `src/main/java/com/classservice/config/SecurityConfig.java` — disables CSRF, sets stateless session, registers `JwtAuthFilter` and `TenantContextFilter`
- `src/main/java/com/classservice/config/JwtAuthFilter.java` — `OncePerRequestFilter`: extracts Bearer token → validates → sets `SecurityContextHolder`
- `src/main/java/com/classservice/config/TenantContextFilter.java` — `OncePerRequestFilter` (runs after JWT): reads `tenantId` from JWT claim → `TenantContext.set()` → `finally { TenantContext.clear() }`
- `src/main/java/com/classservice/config/CorsConfig.java` — reads `CORS_ALLOWED_ORIGINS` from env

**1.9 — Auth service + controller**
Files:
- `src/main/java/com/classservice/auth/AuthService.java` — `registerTenant()`, `login()`, `changePassword()`, `createUser()`, `listUsers()`
- `src/main/java/com/classservice/auth/AuthController.java` — REST endpoints per API contract `/api/auth/*`
- `src/main/java/com/classservice/auth/dto/` — request/response DTOs (LoginRequest, LoginResponse, RegisterTenantRequest, CreateUserRequest, UserDto, UserProfile, ChangePasswordRequest)

**1.10 — Unit + integration tests**
Files:
- `src/test/java/com/classservice/auth/AuthControllerTest.java` — `@SpringBootTest` with Testcontainers Postgres
- `src/test/java/com/classservice/config/TenantIsolationTest.java` — critical: verifies that user from tenant A cannot access data from tenant B

### Acceptance criteria
- `mvn test` passes with Testcontainers
- `POST /api/auth/register-tenant` creates tenant + admin user
- `POST /api/auth/login` returns a JWT with `tenantId`, `userId`, `role` claims
- `GET /api/auth/me` with valid JWT returns user profile
- Requests without JWT return 401
- Requests with JWT for tenant A cannot retrieve data seeded for tenant B (TenantIsolationTest passes)

---

## Track 2 — Backend Core APIs

**Owner type:** Backend developer
**Branch:** `feature/api-core`
**Complexity:** L
**Depends on:** Track 1 merged to `develop`

### Purpose
Implement all CRUD APIs for classes, students, sessions, attendance, comments, and grades.

### Tasks (in order)

**2.1 — Class module**
Files:
- `src/main/java/com/classservice/classes/Class.java` — JPA entity extending `TenantEntity`
- `src/main/java/com/classservice/classes/ClassRepository.java` — `findAllByTenantId`, `findByIdAndTenantId`
- `src/main/java/com/classservice/classes/ClassService.java` — CRUD + enrollment logic
- `src/main/java/com/classservice/classes/ClassController.java` — endpoints per API contract
- `src/main/java/com/classservice/classes/dto/` — ClassDto, CreateClassRequest, UpdateClassRequest, EnrollStudentsRequest, EnrollResult
- `src/main/java/com/classservice/classes/ClassStudent.java` — join entity for `class_student`

**2.2 — Student module**
Files:
- `src/main/java/com/classservice/students/Student.java` — JPA entity
- `src/main/java/com/classservice/students/StudentRepository.java`
- `src/main/java/com/classservice/students/StudentService.java`
- `src/main/java/com/classservice/students/StudentController.java`
- `src/main/java/com/classservice/students/ExcelImportService.java` — Apache POI parsing, returns `ImportResult`
- `src/main/java/com/classservice/students/dto/` — StudentDto, CreateStudentRequest, ImportResult, ImportError
- `src/main/resources/templates/student-import-template.xlsx` — bundled template file

**2.3 — Session module**
Files:
- `src/main/java/com/classservice/sessions/Session.java` — JPA entity
- `src/main/java/com/classservice/sessions/SessionRepository.java` — `findByClassIdAndSessionDateBetween`
- `src/main/java/com/classservice/sessions/SessionService.java`
- `src/main/java/com/classservice/sessions/SessionController.java`
- `src/main/java/com/classservice/sessions/dto/` — SessionDto, SessionDetailDto, CreateSessionRequest, UpdateSessionRequest

**2.4 — Attendance module**
Files:
- `src/main/java/com/classservice/attendance/Attendance.java` — JPA entity; field: `AttendanceStatus` enum
- `src/main/java/com/classservice/attendance/AttendanceStatus.java` — `PRESENT | ABSENT | LATE`
- `src/main/java/com/classservice/attendance/AttendanceRepository.java` — `findBySessionId`, `deleteBySessionIdAndStudentId`
- `src/main/java/com/classservice/attendance/AttendanceService.java` — `saveAll()` upsert for bulk endpoint
- `src/main/java/com/classservice/attendance/AttendanceController.java`
- `src/main/java/com/classservice/attendance/dto/` — AttendanceDto, BulkAttendanceRequest, UpdateAttendanceRequest

**2.5 — Comment module**
Files:
- `src/main/java/com/classservice/comments/Comment.java` — JPA entity
- `src/main/java/com/classservice/comments/CommentRepository.java` — `findBySessionId`, `findBySessionIdAndStudentId`
- `src/main/java/com/classservice/comments/CommentService.java`
- `src/main/java/com/classservice/comments/CommentController.java`
- `src/main/java/com/classservice/comments/dto/` — CommentDto, CreateCommentRequest, UpdateCommentRequest

**2.6 — Grade module**
Files:
- `src/main/java/com/classservice/grades/Grade.java` — JPA entity
- `src/main/java/com/classservice/grades/GradeRepository.java` — `findByClassIdAndStudentId`
- `src/main/java/com/classservice/grades/GradeService.java`
- `src/main/java/com/classservice/grades/GradeController.java`
- `src/main/java/com/classservice/grades/dto/` — GradeDto, CreateGradeRequest, UpdateGradeRequest

**2.7 — Integration tests**
Files:
- `src/test/java/com/classservice/classes/ClassControllerTest.java`
- `src/test/java/com/classservice/students/StudentControllerTest.java`
- `src/test/java/com/classservice/students/ExcelImportServiceTest.java` — test with a sample xlsx fixture
- `src/test/java/com/classservice/sessions/SessionControllerTest.java`
- `src/test/java/com/classservice/attendance/AttendanceBulkTest.java`
- `src/test/java/com/classservice/grades/GradeControllerTest.java`
- `src/test/resources/fixtures/valid-students.xlsx` — test Excel file

### Acceptance criteria
- All CRUD endpoints return correct status codes
- Tenant isolation: all queries automatically scoped to `TenantContext.get()`
- `POST /api/students/import` with valid xlsx creates students; with invalid xlsx returns row-level errors
- Bulk attendance upsert: calling twice for same session updates not duplicates
- `GET /api/sessions/{id}` returns full attendance + comments nested in response

---

## Track 3 — Backend Billing

**Owner type:** Backend developer
**Branch:** `feature/billing`
**Complexity:** M
**Depends on:** Track 2 merged to `develop`

### Purpose
Billing calculation engine, bill generation, status management, and PDF/ZIP export.

### Tasks (in order)

**3.1 — Bill entity + repository**
Files:
- `src/main/java/com/classservice/billing/Bill.java` — JPA entity
- `src/main/java/com/classservice/billing/BillStatus.java` — `DRAFT | ISSUED | PAID`
- `src/main/java/com/classservice/billing/BillRepository.java` — `findByTenantIdAndBillingMonth`, `findByStudentIdAndClassIdAndBillingMonth`

**3.2 — Bill calculation engine**
Files:
- `src/main/java/com/classservice/billing/BillService.java`

Key method: `generateBills(String classId, YearMonth month)`:
1. Load all sessions for `classId` in `month`.
2. For each enrolled student:
   - Count `sessionsTotal` = sessions in month where `cancelledByTeacher = false`.
   - Count `sessionsAttended` = attendance records for student with status `PRESENT` or `LATE` in those sessions.
   - `totalAmount = sessionsAttended * class.ratePerSession`.
3. Upsert `Bill` rows — only overwrite if current status is `DRAFT`. Throw `BillAlreadyIssuedException` if attempting to regenerate an `ISSUED` or `PAID` bill.

**3.3 — PDF generation service**
Files:
- `src/main/java/com/classservice/billing/PdfExportService.java` — iText 7 implementation

`generateBillPdf(BillDetailDto bill)` returns `byte[]`:
- Header: tenant name, bill period, generated date
- Student info table
- Session table: date | topic | attended | cancelled | counted
- Summary: sessions attended × rate = total
- Footer: center contact info (from tenant record)

**3.4 — Bill controller**
Files:
- `src/main/java/com/classservice/billing/BillController.java` — all endpoints per API contract
- `src/main/java/com/classservice/billing/dto/` — BillDto, BillDetailDto, GenerateBillsRequest, GenerateBillsResult, UpdateBillStatusRequest

ZIP export (`GET /api/bills/export`):
- Streams a `ZipOutputStream` containing one PDF per bill, named `{studentName}-{YYYY-MM}.pdf`.
- Uses `StreamingResponseBody` to avoid buffering entire ZIP in memory.

**3.5 — Tests**
Files:
- `src/test/java/com/classservice/billing/BillCalculationTest.java` — pure unit test of calculation logic (no DB)
  - Test case: session cancelled by teacher → not counted
  - Test case: student absent → attended = 0, amount = 0
  - Test case: ISSUED bill not overwritten on re-generate
- `src/test/java/com/classservice/billing/BillControllerTest.java` — integration test
- `src/test/java/com/classservice/billing/PdfExportServiceTest.java` — verify PDF is non-empty byte array

### Acceptance criteria
- `POST /api/bills/generate` produces correct `totalAmount` for all edge cases in unit tests
- Cancelled-by-teacher sessions produce `$0` contribution to bill
- `GET /api/bills/{id}/pdf` returns a valid PDF binary
- `GET /api/bills/export?month=2024-03` returns a valid ZIP
- ISSUED/PAID bills cannot be overwritten by generate endpoint (409 response)

---

## Track 4 — Frontend Foundation

**Owner type:** Frontend developer
**Branch:** `feature/frontend-foundation`
**Complexity:** M
**Depends on:** API contract document only (can start immediately, parallel with Track 1)

### Purpose
React app scaffold: project setup, routing, auth pages, layout shell, shared API client, and the Zustand auth store. All subsequent frontend tracks build on top of this.

### Tasks (in order)

**4.1 — Project scaffold**
Initialize inside `src/main/frontend/` (or a separate `frontend/` root directory — choose `frontend/` for cleaner separation):
- `frontend/package.json` — dependencies: react, react-dom, react-router-dom, axios, @tanstack/react-query, zustand, react-hook-form, zod, @hookform/resolvers, tailwindcss, shadcn/ui init
- `frontend/vite.config.ts` — proxy `/api` to `http://localhost:8080` in dev
- `frontend/tsconfig.json`
- `frontend/tailwind.config.ts`
- `frontend/postcss.config.js`
- `frontend/index.html`
- `frontend/.env.example` — `VITE_API_BASE_URL=`

**4.2 — shadcn/ui setup**
Files:
- `frontend/components.json` — shadcn config
- `frontend/src/lib/utils.ts` — `cn()` helper
- Add components via `npx shadcn-ui@latest add button input label card table badge toast dialog`
- Generated under `frontend/src/components/ui/`

**4.3 — API client**
Files:
- `frontend/src/api/client.ts` — Axios instance:
  - `baseURL` from `import.meta.env.VITE_API_BASE_URL`
  - Request interceptor: inject `Authorization: Bearer <token>` from Zustand store
  - Response interceptor: on 401 → clear auth state → `navigate('/login')`
- `frontend/src/api/endpoints/auth.ts` — `login()`, `registerTenant()`, `getMe()`, `changePassword()`, `createUser()`, `listUsers()`
- `frontend/src/api/endpoints/classes.ts` — all class API calls
- `frontend/src/api/endpoints/students.ts` — all student API calls (including import)
- `frontend/src/api/endpoints/sessions.ts`
- `frontend/src/api/endpoints/attendance.ts`
- `frontend/src/api/endpoints/comments.ts`
- `frontend/src/api/endpoints/grades.ts`
- `frontend/src/api/endpoints/bills.ts`
- `frontend/src/api/types.ts` — all TypeScript interfaces from `api-contract.md` (single source of truth)

**4.4 — Auth store**
Files:
- `frontend/src/store/authStore.ts` — Zustand store:
  ```typescript
  interface AuthState {
    token: string | null;
    user: UserProfile | null;
    setAuth: (token: string, user: UserProfile) => void;
    clearAuth: () => void;
  }
  ```
  Note: token stored in memory only (not localStorage).

**4.5 — Router**
Files:
- `frontend/src/router.tsx` — `createBrowserRouter` with:
  - Public routes: `/login`, `/register`
  - Protected routes (wrapped in `AuthGuard`): `/`, `/classes`, `/classes/:id`, `/students`, `/students/:id`, `/sessions/:id`, `/grades`, `/billing`
- `frontend/src/components/layout/AuthGuard.tsx` — checks `authStore.token`; redirects to `/login` if null

**4.6 — Layout shell**
Files:
- `frontend/src/components/layout/AppShell.tsx` — outer layout: `Sidebar` + `TopBar` + `<Outlet />`
- `frontend/src/components/layout/Sidebar.tsx` — nav links: Dashboard, Classes, Students, Billing
- `frontend/src/components/layout/TopBar.tsx` — tenant name, user name, logout button

**4.7 — Auth pages**
Files:
- `frontend/src/pages/LoginPage.tsx` — email + password + tenantId form; calls `auth.login()`; on success calls `setAuth()` and navigates to `/`
- `frontend/src/pages/RegisterPage.tsx` — register new tenant form
- `frontend/src/pages/DashboardPage.tsx` — minimal placeholder (counts: X classes, Y students)

**4.8 — Shared components**
Files:
- `frontend/src/components/tables/DataTable.tsx` — generic sortable/paginated table wrapping shadcn `Table`
- `frontend/src/components/common/LoadingSpinner.tsx`
- `frontend/src/components/common/ErrorMessage.tsx`
- `frontend/src/components/common/ConfirmDialog.tsx` — reusable delete confirmation dialog
- `frontend/src/components/common/ToastProvider.tsx` — Sonner or shadcn toast

**4.9 — Entry point**
Files:
- `frontend/src/main.tsx` — `ReactDOM.createRoot` with `QueryClientProvider`, `RouterProvider`, `ToastProvider`

### Acceptance criteria
- `npm run dev` starts the dev server; proxy to backend works
- Unauthenticated navigation to `/classes` redirects to `/login`
- Login form submits, stores token, redirects to `/`
- Logout clears token, redirects to `/login`
- AppShell renders with sidebar and topbar
- `npm run build` produces static files with no TypeScript errors

---

## Track 5 — Frontend Core

**Owner type:** Frontend developer
**Branch:** `feature/frontend-core`
**Complexity:** L
**Depends on:** Track 4 merged + Track 2 merged (backend APIs available)

### Purpose
All the day-to-day operational screens: class management, student roster, Excel import, session recording, attendance grid, student comments per session, grade management.

### Tasks (in order)

**5.1 — TanStack Query hooks**
Files:
- `frontend/src/hooks/useClasses.ts` — `useClasses()`, `useClass(id)`, `useCreateClass()`, `useUpdateClass()`, `useDeleteClass()`
- `frontend/src/hooks/useStudents.ts` — CRUD + `useImportStudents()`
- `frontend/src/hooks/useSessions.ts` — CRUD
- `frontend/src/hooks/useAttendance.ts` — `useAttendance(sessionId)`, `useSaveAttendance()`
- `frontend/src/hooks/useComments.ts`
- `frontend/src/hooks/useGrades.ts`

Each hook uses `useQuery` / `useMutation` from TanStack Query with appropriate `queryKey` patterns for cache invalidation.

**5.2 — Class management pages**
Files:
- `frontend/src/pages/ClassesPage.tsx` — paginated list, search bar, "New Class" button; `DataTable` with columns: Name, Subject, Teacher, Rate, Status, Actions
- `frontend/src/pages/ClassDetailPage.tsx` — tabs: Overview | Students | Sessions | Grades
- `frontend/src/components/forms/ClassForm.tsx` — React Hook Form + Zod schema; teacher dropdown (fetched from `/api/auth/users`)

**5.3 — Student roster pages**
Files:
- `frontend/src/pages/StudentsPage.tsx` — paginated list, search, "Add Student" + "Import Excel" buttons
- `frontend/src/pages/StudentDetailPage.tsx` — student info, enrolled classes, recent grades
- `frontend/src/components/forms/StudentForm.tsx`
- `frontend/src/components/students/ExcelImportDialog.tsx` — file upload (drag-and-drop), upload progress, import result table showing per-row errors; "Download Template" link calls `/api/students/import-template`

**5.4 — Session recording page**
Files:
- `frontend/src/pages/SessionDetailPage.tsx` — two panels side-by-side:
  - Left: session metadata form (date, topic, cancelled toggle)
  - Right: attendance + comments grid
- `frontend/src/components/sessions/AttendanceGrid.tsx` — one row per enrolled student; status selector (PRESENT / ABSENT / LATE); comment text area per student; "Save All" button calls bulk attendance + comment endpoints
- `frontend/src/components/sessions/SessionForm.tsx`

**5.5 — Sessions list (inside ClassDetailPage)**
Files:
- `frontend/src/components/classes/SessionsList.tsx` — table of sessions for a class; "New Session" button; link to `SessionDetailPage`

**5.6 — Grade management**
Files:
- `frontend/src/pages/GradesPage.tsx` — class selector → student selector → exam list with scores; inline edit
- `frontend/src/components/grades/GradeTable.tsx` — editable grid of exam scores per student
- `frontend/src/components/forms/GradeForm.tsx` — add exam dialog

**5.7 — Zod validation schemas**
Files:
- `frontend/src/lib/validations/classSchema.ts`
- `frontend/src/lib/validations/studentSchema.ts`
- `frontend/src/lib/validations/sessionSchema.ts`
- `frontend/src/lib/validations/gradeSchema.ts`

### Acceptance criteria
- Class list loads from API, search filters work, CRUD operations complete with toast feedback
- Student import: upload xlsx, see import result with error rows highlighted
- Session detail page: attendance grid saves bulk attendance in one click; comments save inline
- Grade table: add/edit/delete exams for a student in a class
- All forms show field-level validation errors before submitting

---

## Track 6 — Frontend Billing

**Owner type:** Frontend developer
**Branch:** `feature/frontend-billing`
**Complexity:** M
**Depends on:** Track 5 merged + Track 3 merged (billing API available)

### Purpose
Monthly bill generation, bill list, bill detail view with line items, PDF download, and bulk ZIP export.

### Tasks (in order)

**6.1 — Billing hooks**
Files:
- `frontend/src/hooks/useBills.ts` — `useBills(filters)`, `useGenerateBills()`, `useUpdateBillStatus()`, `useBillDetail(id)`

**6.2 — Billing list page**
Files:
- `frontend/src/pages/BillingPage.tsx` — month picker + class filter + status filter; "Generate Bills" button (opens `GenerateBillsDialog`); `DataTable` of bills with columns: Student, Class, Month, Attended, Total, Status, Actions (View PDF, Mark Paid)
- `frontend/src/components/billing/GenerateBillsDialog.tsx` — class selector + month picker; on confirm calls `POST /api/bills/generate`; shows result count

**6.3 — Bill detail page**
Files:
- `frontend/src/pages/BillDetailPage.tsx` — student info header; session table with per-session attendance + cancellation indicators; summary footer: `X sessions attended × rate = total`; "Download PDF" button + status badge + "Mark as Issued/Paid" dropdown

**6.4 — PDF download + ZIP export**
Files:
- `frontend/src/components/billing/PdfDownloadButton.tsx` — calls `GET /api/bills/{id}/pdf`, triggers browser download via Blob URL
- `frontend/src/components/billing/BulkExportButton.tsx` — calls `GET /api/bills/export?month=...`, triggers ZIP download; shows loading spinner during stream

**6.5 — Bill status badge**
Files:
- `frontend/src/components/billing/BillStatusBadge.tsx` — colored badge: DRAFT (grey), ISSUED (blue), PAID (green)

### Acceptance criteria
- Month/class filter combination loads correct bills list
- "Generate Bills" for a class+month shows updated bill totals matching backend calculation
- Bill detail page shows per-session breakdown matching the PDF
- "Download PDF" triggers browser file download of a valid PDF
- "Bulk Export" downloads a ZIP with one PDF per bill
- Status transitions: DRAFT → ISSUED → PAID; backward transitions blocked by UI and API

---

## Merge Order

```
develop
  ├── Track 1 merges first (unblocks 2, 4)
  ├── Track 4 merges (can be same day as Track 1 or before)
  ├── Track 2 merges (unblocks 3, 5)
  ├── Track 3 merges (unblocks 6)
  ├── Track 5 merges (unblocks 6)
  └── Track 6 merges last
```

Integration testing should be run after each merge to `develop`.

---

## File Ownership Matrix

Every source file belongs to exactly one track. No overlap.

| Directory / File pattern | Track |
|---|---|
| `pom.xml` | 1 |
| `src/main/resources/application*.yml` | 1 |
| `src/main/resources/db/migration/V*.sql` | 1 |
| `src/main/java/com/classservice/common/` | 1 |
| `src/main/java/com/classservice/config/` | 1 |
| `src/main/java/com/classservice/tenant/` | 1 |
| `src/main/java/com/classservice/auth/` | 1 |
| `src/main/java/com/classservice/classes/` | 2 |
| `src/main/java/com/classservice/students/` | 2 |
| `src/main/java/com/classservice/sessions/` | 2 |
| `src/main/java/com/classservice/attendance/` | 2 |
| `src/main/java/com/classservice/comments/` | 2 |
| `src/main/java/com/classservice/grades/` | 2 |
| `src/main/java/com/classservice/billing/` | 3 |
| `frontend/vite.config.ts`, `package.json`, `tsconfig.json` | 4 |
| `frontend/src/main.tsx`, `router.tsx` | 4 |
| `frontend/src/api/` | 4 |
| `frontend/src/store/` | 4 |
| `frontend/src/components/layout/` | 4 |
| `frontend/src/components/ui/` | 4 |
| `frontend/src/components/common/` | 4 |
| `frontend/src/components/tables/DataTable.tsx` | 4 |
| `frontend/src/pages/LoginPage.tsx`, `RegisterPage.tsx`, `DashboardPage.tsx` | 4 |
| `frontend/src/lib/utils.ts` | 4 |
| `frontend/src/hooks/useClasses.ts`, `useStudents.ts`, etc. | 5 |
| `frontend/src/pages/ClassesPage.tsx`, `ClassDetailPage.tsx` | 5 |
| `frontend/src/pages/StudentsPage.tsx`, `StudentDetailPage.tsx` | 5 |
| `frontend/src/pages/SessionDetailPage.tsx` | 5 |
| `frontend/src/pages/GradesPage.tsx` | 5 |
| `frontend/src/components/forms/` | 5 |
| `frontend/src/components/classes/`, `students/`, `sessions/`, `grades/` | 5 |
| `frontend/src/lib/validations/` | 5 |
| `frontend/src/hooks/useBills.ts` | 6 |
| `frontend/src/pages/BillingPage.tsx`, `BillDetailPage.tsx` | 6 |
| `frontend/src/components/billing/` | 6 |

---

## Estimated Timeline (single team, tracks serialized where dependent)

| Week | Activity |
|---|---|
| 1 | Track 1 + Track 4 (parallel) |
| 2 | Track 2 + Track 4 finalize |
| 3 | Track 3 + Track 5 (parallel) |
| 4 | Track 5 finalize + Track 6 |
| 5 | Integration testing, bug fixes, deployment setup |

With two developers (one backend, one frontend), Tracks 1+4 and 2+5 can proceed in parallel, compressing the timeline to approximately 3 weeks of active development.
