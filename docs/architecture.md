# Architecture — Tutoring Class Management Service

## 1. System Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         BROWSER (Desktop-first)                      │
│                                                                       │
│   React 18 SPA  ──── Axios ────►  REST API (JSON)                   │
│   (Vite + TS)                                                         │
└──────────────────────────────────────┬──────────────────────────────┘
                                       │ HTTPS / HTTP (dev)
                                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Spring Boot 3.x  (Java 21)                        │
│                                                                       │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────────────┐ │
│  │  Security    │   │  Controllers │   │  Tenant Filter           │ │
│  │  (JWT)       │◄──│  (REST)      │◄──│  (resolves tenantId      │ │
│  └──────────────┘   └──────┬───────┘   │   from JWT claim)        │ │
│                             │           └──────────────────────────┘ │
│                             ▼                                         │
│                    ┌──────────────────┐                              │
│                    │   Service Layer  │                              │
│                    │  (business logic │                              │
│                    │   + tenant scope)│                              │
│                    └────────┬─────────┘                              │
│                             │                                         │
│                             ▼                                         │
│                    ┌──────────────────┐                              │
│                    │  Repository Layer│                              │
│                    │  (Spring Data JPA│                              │
│                    │   + tenant WHERE)│                              │
│                    └────────┬─────────┘                              │
│                             │                                         │
│          ┌──────────────────┼───────────────────────────────┐        │
│          │                  │                               │        │
│          ▼                  ▼                               ▼        │
│   ┌────────────┐  ┌──────────────────┐          ┌──────────────┐   │
│   │ PostgreSQL │  │  iText 7 (PDF)   │          │  Apache POI  │   │
│   │ (primary   │  │  generation      │          │  (Excel      │   │
│   │  datastore)│  └──────────────────┘          │  import)     │   │
│   └────────────┘                                └──────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

**Data flow summary:**
1. Browser sends JWT in `Authorization: Bearer <token>` header on every request.
2. `TenantContextFilter` extracts `tenantId` claim from the JWT and stores it in a thread-local `TenantContext`.
3. Every repository method appends `AND tenant_id = :tenantId` (via Spring Data JPA `@Query` or Hibernate filter).
4. Service layer executes business logic; billing engine runs pure Java calculation.
5. PDF export uses iText 7; Excel import uses Apache POI — both are in-process, no external services.

---

## 2. Tech Stack

| Layer | Technology | Version | Rationale |
|---|---|---|---|
| Runtime | Java | 21 (LTS) | Already scaffolded; virtual threads available |
| Framework | Spring Boot | 3.3.x | Already scaffolded; mature ecosystem |
| Build | Maven | 3.9.x | Already in pom.xml |
| Database | PostgreSQL | 15+ | JSONB if needed; reliable; multi-tenant row-level isolation |
| ORM | Spring Data JPA / Hibernate | 6.x (via Boot) | Standard; reduces boilerplate |
| Migrations | Flyway | 10.x | SQL-first; deterministic ordering |
| Auth | JWT (JJWT library) | 0.12.x | Stateless; carries tenantId claim |
| Password hashing | BCrypt | (Spring Security built-in) | Industry standard |
| PDF generation | iText 7 Community | 7.2.x | Java-native; no external process |
| Excel import | Apache POI | 5.3.x | De-facto Java Excel library |
| Frontend framework | React | 18.x | Widely adopted; large component ecosystem |
| Frontend build | Vite | 5.x | Fast HMR; minimal config |
| Frontend language | TypeScript | 5.x | Type safety on API contract |
| UI components | shadcn/ui + Tailwind CSS | latest | Copy-in components; no runtime CSS-in-JS overhead |
| HTTP client | Axios | 1.x | Interceptors for JWT injection |
| Routing | React Router | 6.x | Client-side SPA routing |
| State management | TanStack Query (React Query) | 5.x | Server-state cache; avoids Redux complexity |
| Form handling | React Hook Form + Zod | latest | Schema-validated forms with TS inference |
| Testing (backend) | JUnit 5 + Testcontainers | Spring Boot test slice | Real DB in tests |
| Testing (frontend) | Vitest + React Testing Library | latest | Co-located with Vite |
| Deployment | Docker + Docker Compose | — | Single-machine MVP; portable |

---

## 3. Component Architecture

### Backend Layers

```
com.classservice
├── config/
│   ├── SecurityConfig          — JWT filter chain, CORS, CSRF disabled for API
│   ├── TenantContextFilter     — extracts tenantId from JWT, sets ThreadLocal
│   └── JpaConfig               — Hibernate tenant filter activation
│
├── auth/
│   ├── AuthController          — POST /api/auth/login, /register, /refresh
│   ├── AuthService             — validates credentials, issues JWT
│   ├── JwtUtil                 — sign / parse / validate JWT (JJWT)
│   └── UserRepository          — JPA repo for app_user table
│
├── tenant/
│   ├── Tenant                  — JPA entity for tenant (tutoring center)
│   ├── TenantRepository
│   └── TenantContext           — ThreadLocal holder (cleared in finally block)
│
├── classes/
│   ├── ClassController         — CRUD + roster endpoints
│   ├── ClassService
│   └── ClassRepository
│
├── students/
│   ├── StudentController       — CRUD + Excel import
│   ├── StudentService
│   ├── StudentRepository
│   └── ExcelImportService      — Apache POI parsing logic
│
├── sessions/
│   ├── SessionController       — session CRUD
│   ├── SessionService
│   └── SessionRepository
│
├── attendance/
│   ├── AttendanceController    — mark/update attendance per session
│   ├── AttendanceService
│   └── AttendanceRepository
│
├── comments/
│   ├── CommentController       — per-student comment on a session
│   ├── CommentService
│   └── CommentRepository
│
├── grades/
│   ├── GradeController         — exam grade entry
│   ├── GradeService
│   └── GradeRepository
│
├── billing/
│   ├── BillController          — generate + list + export bills
│   ├── BillService             — calculation engine
│   ├── BillRepository
│   └── PdfExportService        — iText 7 PDF generation
│
└── common/
    ├── TenantEntity            — @MappedSuperclass with tenantId field
    ├── GlobalExceptionHandler  — @ControllerAdvice maps exceptions to HTTP codes
    ├── ApiResponse<T>          — standard envelope {data, error, message}
    └── PageResponse<T>         — pagination wrapper
```

### Frontend Layers

```
src/
├── main.tsx                    — React root, QueryClientProvider, RouterProvider
├── router.tsx                  — React Router route definitions + auth guard
│
├── api/
│   ├── client.ts               — Axios instance with JWT interceptor
│   └── endpoints/              — one file per resource (auth, classes, students…)
│
├── hooks/                      — TanStack Query hooks per resource
│
├── store/
│   └── authStore.ts            — Zustand slice: user, token, tenantId
│
├── pages/
│   ├── LoginPage
│   ├── DashboardPage
│   ├── ClassesPage / ClassDetailPage
│   ├── StudentsPage / StudentDetailPage
│   ├── SessionsPage / SessionDetailPage (attendance + comments)
│   ├── GradesPage
│   └── BillingPage / BillDetailPage
│
├── components/
│   ├── layout/                 — AppShell, Sidebar, TopBar
│   ├── tables/                 — DataTable (reusable, sortable/paginated)
│   ├── forms/                  — ClassForm, StudentForm, GradeForm…
│   └── ui/                     — shadcn/ui primitives (Button, Dialog, etc.)
│
└── lib/
    ├── utils.ts                — cn() helper, formatters
    └── validations/            — Zod schemas matching API contract
```

---

## 4. Database Schema

### Multi-tenancy approach

**Strategy: Shared schema, tenant_id discriminator column on every table.**

Every business entity table has a `tenant_id UUID NOT NULL` column. All queries are scoped with `WHERE tenant_id = ?`. A Hibernate `@Filter` is activated per-request by `TenantContextFilter` so developers cannot accidentally leak cross-tenant data. Indexes on `(tenant_id, id)` keep queries fast.

This is the simplest approach for MVP and scales to hundreds of tenants without schema-per-tenant operational overhead.

### Tables

```sql
-- Tenants (tutoring centers)
CREATE TABLE tenant (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Application users (teachers / admins)
CREATE TABLE app_user (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenant(id),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL,  -- ADMIN | TEACHER
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, email)
);
CREATE INDEX idx_app_user_tenant ON app_user(tenant_id);

-- Classes
CREATE TABLE class (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenant(id),
    name          VARCHAR(255) NOT NULL,
    subject       VARCHAR(255),
    teacher_id    UUID NOT NULL REFERENCES app_user(id),
    rate_per_session NUMERIC(12,2) NOT NULL DEFAULT 0,
    status        VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | ARCHIVED
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_class_tenant ON class(tenant_id);
CREATE INDEX idx_class_teacher ON class(teacher_id);

-- Students
CREATE TABLE student (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenant(id),
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(50),
    parent_phone  VARCHAR(50),
    notes         TEXT,
    school_name   VARCHAR(255),          -- added V6 migration
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_student_tenant ON student(tenant_id);

-- Class-Student enrollment (many-to-many)
CREATE TABLE class_student (
    class_id    UUID NOT NULL REFERENCES class(id),
    student_id  UUID NOT NULL REFERENCES student(id),
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (class_id, student_id)
);
CREATE INDEX idx_class_student_student ON class_student(student_id);

-- Sessions (a scheduled meeting of a class)
CREATE TABLE session (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL REFERENCES tenant(id),
    class_id             UUID NOT NULL REFERENCES class(id),
    session_date         DATE NOT NULL,
    start_time           TIME,
    end_time             TIME,
    topic                TEXT,
    progress_notes       TEXT,           -- teaching content description, added V6 migration
    cancelled_by_teacher BOOLEAN NOT NULL DEFAULT false,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_session_tenant ON session(tenant_id);
CREATE INDEX idx_session_class ON session(class_id);
CREATE INDEX idx_session_date ON session(class_id, session_date);

-- Attendance
CREATE TABLE attendance (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenant(id),
    session_id  UUID NOT NULL REFERENCES session(id),
    student_id  UUID NOT NULL REFERENCES student(id),
    status      VARCHAR(50) NOT NULL DEFAULT 'PRESENT',  -- PRESENT | ABSENT | LATE
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (session_id, student_id)
);
CREATE INDEX idx_attendance_tenant ON attendance(tenant_id);
CREATE INDEX idx_attendance_session ON attendance(session_id);
CREATE INDEX idx_attendance_student ON attendance(student_id);

-- Comments (per-student per-session teacher notes)
CREATE TABLE comment (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenant(id),
    session_id  UUID NOT NULL REFERENCES session(id),
    student_id  UUID NOT NULL REFERENCES student(id),
    author_id   UUID NOT NULL REFERENCES app_user(id),
    body        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (session_id, student_id)  -- one comment per student per session
);
CREATE INDEX idx_comment_tenant ON comment(tenant_id);
CREATE INDEX idx_comment_session ON comment(session_id);

-- Grades (per-student per-exam)
CREATE TABLE grade (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenant(id),
    class_id    UUID NOT NULL REFERENCES class(id),
    student_id  UUID NOT NULL REFERENCES student(id),
    exam_name   VARCHAR(255) NOT NULL,
    exam_date   DATE,
    score       NUMERIC(6,2),
    max_score   NUMERIC(6,2),
    notes       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (class_id, student_id, exam_name)
);
CREATE INDEX idx_grade_tenant ON grade(tenant_id);
CREATE INDEX idx_grade_class_student ON grade(class_id, student_id);

-- Bills (monthly billing record)
CREATE TABLE bill (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenant(id),
    student_id      UUID NOT NULL REFERENCES student(id),
    class_id        UUID NOT NULL REFERENCES class(id),
    billing_month   DATE NOT NULL,  -- stored as first day of month: 2024-03-01
    sessions_total  INTEGER NOT NULL DEFAULT 0,
    sessions_attended INT NOT NULL DEFAULT 0,
    rate_per_session NUMERIC(12,2) NOT NULL,
    total_amount    NUMERIC(12,2) NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'DRAFT',  -- DRAFT | ISSUED | PAID
    pdf_path        VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, student_id, class_id, billing_month)
);
CREATE INDEX idx_bill_tenant ON bill(tenant_id);
CREATE INDEX idx_bill_student ON bill(student_id);
CREATE INDEX idx_bill_month ON bill(tenant_id, billing_month);
```

### Migration strategy

Flyway classpath migration files under `src/main/resources/db/migration/`:
- `V1__create_tenant_and_users.sql`
- `V2__create_classes_and_students.sql`
- `V3__create_sessions_and_attendance.sql`
- `V4__create_comments_and_grades.sql`
- `V5__create_bills.sql`
- `V6__add_progress_notes_and_school_name.sql` — `ALTER TABLE session ADD COLUMN progress_notes TEXT` + `ALTER TABLE student ADD COLUMN school_name VARCHAR(255)`

---

## 5. Authentication Flow

```
POST /api/auth/login
  { email, password, tenantId }
        │
        ▼
  AuthService.login()
   ├─ load AppUser by (tenantId, email)
   ├─ BCrypt.verify(password, passwordHash)
   └─ JwtUtil.generate(userId, tenantId, role, exp=8h)
        │
        ▼
  Response: { accessToken, refreshToken, user: {id, name, role} }
        │
        ▼ (stored in memory / Zustand — NOT localStorage to avoid XSS)
  Subsequent requests:
  Authorization: Bearer <accessToken>
        │
        ▼
  TenantContextFilter
   ├─ parse JWT → extract tenantId, userId, role
   ├─ TenantContext.set(tenantId)   ← ThreadLocal
   └─ SecurityContext.set(user principal)
        │
        ▼
  Every repository query:  WHERE tenant_id = TenantContext.get()
        │
        ▼
  Finally block: TenantContext.clear()  ← prevent leakage on thread reuse
```

### Role-based access control

| Role | Capabilities |
|---|---|
| ADMIN | All operations within their tenant; user management; billing (exclusive) |
| TEACHER | Read/write own classes, sessions, attendance, comments, grades; read students. **No access to `/api/bills/**`** |

Spring `@PreAuthorize` annotations enforce role checks at controller layer. `BillController` carries a class-level `@PreAuthorize("hasRole('ADMIN')")`. Frontend hides billing nav link and class billing tab based on `user.role` from Zustand `authStore`; the `AdminGuard` route wrapper also prevents direct URL access.

### Token storage

Access token: stored in memory (Zustand/React state). Survives page navigation but not hard refresh — user re-logs in after refresh. This avoids XSS from localStorage.

Refresh token (optional in MVP): httpOnly cookie for silent re-auth. Can be deferred to post-MVP.

---

## 6. Multi-Tenancy Details

**Pattern: Shared schema with discriminator column**

1. Every entity extends `TenantEntity` (`@MappedSuperclass`) which adds `tenantId UUID`.
2. `TenantContextFilter` (servlet filter, `@Order(1)`) runs before every request:
   - Extracts `tenantId` from validated JWT.
   - Writes to `TenantContext` (static `ThreadLocal<UUID>`).
3. A Hibernate `@Filter` named `tenantFilter` is defined on each entity:
   ```java
   @FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
   @Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
   ```
4. `TenantAwareRepositoryImpl` (custom base class) enables the filter for every `EntityManager` operation.
5. Filter is cleared in a `finally` block to prevent ThreadLocal leakage.

**Tenant onboarding:** `POST /api/auth/register-tenant` creates a `Tenant` row and the first `ADMIN` user atomically in a single transaction.

---

## 7. Error Handling Strategy

### Backend

`GlobalExceptionHandler` (`@ControllerAdvice`) maps exceptions to HTTP status codes:

| Exception | HTTP Status | Notes |
|---|---|---|
| `EntityNotFoundException` | 404 | Resource not found |
| `TenantMismatchException` | 403 | Cross-tenant access attempt |
| `DuplicateResourceException` | 409 | Unique constraint violation |
| `ValidationException` | 400 | `@Valid` failures |
| `BadCredentialsException` | 401 | Wrong password |
| `JwtExpiredException` | 401 | Token expired |
| `AccessDeniedException` | 403 | Insufficient role |
| `ExcelParseException` | 422 | Malformed import file |
| `RuntimeException` (catch-all) | 500 | Logged + generic message |

All errors return:
```json
{
  "error": "ENTITY_NOT_FOUND",
  "message": "Class with id abc123 not found",
  "timestamp": "2024-03-15T10:00:00Z"
}
```

### Frontend

- Axios response interceptor catches 401 → clears auth state → redirects to `/login`.
- TanStack Query `onError` callbacks display toast notifications.
- React Error Boundaries wrap each page for render-time crashes.
- Form errors displayed inline via React Hook Form field-level errors.

---

## 8. Deployment Architecture

### MVP (single VPS or local Docker Compose)

```
┌──────────────────────────────────────────────┐
│  Docker Compose                              │
│                                              │
│  ┌────────────────┐   ┌──────────────────┐  │
│  │  nginx:alpine  │   │  postgres:15     │  │
│  │  port 80/443   │   │  volume: pgdata  │  │
│  │  serves SPA    │   └──────────────────┘  │
│  │  proxies /api  │            ▲             │
│  └───────┬────────┘            │             │
│          │                     │             │
│          ▼                     │             │
│  ┌────────────────┐            │             │
│  │  Spring Boot   │────────────┘             │
│  │  JAR (Java 21) │                          │
│  │  port 8080     │                          │
│  └────────────────┘                          │
│                                              │
│  Volumes: pgdata, pdf-exports                │
└──────────────────────────────────────────────┘
```

**Build pipeline:**
1. `mvn clean package -DskipTests` → fat JAR.
2. `npm run build` in frontend → static files copied to Spring Boot `src/main/resources/static/` (or served by nginx).
3. Docker image built from `Dockerfile`.

### Environment variables

| Variable | Dev default | Required in Prod |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/classservice` | Yes |
| `DB_USERNAME` | `classservice` | Yes |
| `DB_PASSWORD` | `classservice` | Yes |
| `JWT_SECRET` | `dev-secret-32-chars-minimum` | Yes (strong random) |
| `JWT_EXPIRY_HOURS` | `8` | Yes |
| `PDF_EXPORT_DIR` | `/tmp/pdf-exports` | Yes |
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | frontend URL |

### Frontend build-time env

| Variable | Purpose |
|---|---|
| `VITE_API_BASE_URL` | Backend URL (empty = same origin via nginx proxy) |

---

## 9. PDF Export Design

Bills are exported as PDFs using iText 7 Community:

- `PdfExportService.exportBill(Bill bill, List<Session> sessions)` generates a PDF in memory (ByteArrayOutputStream).
- Controller streams response: `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="bill-{month}-{studentName}.pdf"`.
- PDF includes: center name, student name, class name, month, session list (date + attended/absent), rate, total.
- No file is persisted to disk in MVP (generated on-demand).

---

## 10. Excel Import Design

Student roster import via Apache POI:

- `ExcelImportService.parseStudents(InputStream, tenantId)` reads `.xlsx` files.
- Expected columns: `Full Name`, `Phone`, `Parent Phone`, `Notes` (header row in row 1).
- Validation: non-empty name required; skips blank rows; collects all row errors and returns them in the response so the user sees all problems at once.
- Returns `ImportResult { List<Student> imported, List<ImportError> errors }`.
- A template `.xlsx` download endpoint is provided at `GET /api/students/import-template`.
