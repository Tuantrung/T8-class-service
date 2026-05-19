# class-service — Tutoring Class Management

A multi-tenant web application for managing tutoring centers: classes, students, sessions, attendance, grades, and billing.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3 + Java 21 |
| Frontend | React 18 + TypeScript + Vite |
| Database | PostgreSQL 15 |
| Auth | JWT (JJWT 0.12.x) |
| Migrations | Flyway 10 |
| PDF export | iText 7 |
| Excel import | Apache POI 5.3 |
| UI | Tailwind CSS |
| State | TanStack Query + Zustand |
| Build | Maven 3.9 (backend) + npm (frontend) |
| Container | Docker + Docker Compose |

## Quick Start (Docker Compose)

```bash
# 1. Clone and enter the project
git clone <repo-url>
cd class-service

# 2. Copy environment file
cp .env.example .env
# Edit .env — set a strong JWT_SECRET in production!

# 3. Start postgres only (for local backend dev)
docker compose up -d postgres

# 4. Run backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 5. Run frontend (separate terminal)
cd frontend
cp .env.example .env
npm install
npm run dev
# → http://localhost:5173
```

Full stack with Docker:

```bash
docker compose up --build
# Backend: http://localhost:8080
# Frontend (after npm build): served by nginx
```

## Local Development Setup

### Prerequisites

- Java 21 (recommend: `sdk install java 21.0.4-tem`)
- Maven 3.9+
- Node.js 22+ and npm
- Docker + Docker Compose (for PostgreSQL)

### Backend

```bash
# Start PostgreSQL
docker compose up -d postgres

# Run tests (Testcontainers spins up its own Postgres — no local DB needed)
mvn test

# Run with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# → http://localhost:8080/api
```

### Frontend

```bash
cd frontend
npm install
npm run dev       # Vite dev server with proxy to localhost:8080
npm run build     # Production build
npm test          # Vitest
```

## Environment Variables

Copy `.env.example` to `.env`. Required in production:

| Variable | Description | Required in Prod |
|---|---|---|
| `DB_URL` | JDBC URL to PostgreSQL | Yes |
| `DB_USERNAME` | Database username | Yes |
| `DB_PASSWORD` | Database password | Yes |
| `JWT_SECRET` | JWT signing secret, min 32 chars | Yes |
| `JWT_EXPIRY_HOURS` | Token lifetime in hours (default: 8) | Yes |
| `SPRING_PROFILES_ACTIVE` | Spring profile: `dev` or `prod` | Yes |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed frontend origins | Yes |
| `PDF_EXPORT_DIR` | Directory for PDF file storage | Yes |

Frontend:

| Variable | Description |
|---|---|
| `VITE_API_BASE_URL` | Backend URL (empty = use Vite proxy in dev) |

## Project Structure

```
class-service/
├── src/
│   ├── main/
│   │   ├── java/com/classservice/
│   │   │   ├── auth/          # JWT, users, login/register
│   │   │   ├── config/        # Security, CORS, filters
│   │   │   ├── common/        # TenantEntity, TenantContext, ApiResponse
│   │   │   ├── tenant/        # Tenant entity + repository
│   │   │   ├── classes/       # TutoringClass CRUD + enrollment
│   │   │   ├── students/      # Student CRUD + Excel import
│   │   │   ├── sessions/      # Session CRUD
│   │   │   ├── attendance/    # Bulk attendance upsert
│   │   │   ├── comments/      # Per-student session comments
│   │   │   ├── grades/        # Exam grade management
│   │   │   └── billing/       # Bill engine, PDF export
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/   # Flyway SQL migrations V1–V6
│   └── test/
│       └── java/com/classservice/
│           └── billing/BillCalculationTest.java
├── frontend/
│   └── src/
│       ├── api/               # Axios client + typed API endpoints
│       ├── store/             # Zustand auth store
│       ├── hooks/             # TanStack Query hooks
│       ├── pages/             # Route pages
│       ├── components/        # Layout, UI, forms
│       └── lib/               # utils, Zod validation schemas
├── .github/
│   ├── workflows/ci.yml       # GitHub Actions CI pipeline
│   └── CONTRIBUTING.md        # Branch strategy and PR guidelines
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── .env.example
```

## Database Migrations

Flyway migrations run automatically on startup from `src/main/resources/db/migration/`:

| File | Creates |
|---|---|
| V1 | `tenant`, `app_user` |
| V2 | `class`, `student`, `class_student` |
| V3 | `session`, `attendance` |
| V4 | `comment`, `grade` |
| V5 | `bill` |
| V6 | Seed data (dev profile only) |

All tables include a `tenant_id` column for multi-tenant row-level isolation.

## API Overview

Base path: `/api`

| Resource | Endpoints |
|---|---|
| Auth | `POST /auth/register-tenant`, `POST /auth/login`, `GET /auth/me` |
| Classes | `GET/POST /classes`, `GET/PUT/DELETE /classes/{id}`, `POST /classes/{id}/students` |
| Students | `GET/POST /students`, `GET/PUT/DELETE /students/{id}`, `POST /students/import` |
| Sessions | `GET/POST /sessions`, `GET/PUT/DELETE /sessions/{id}` |
| Attendance | `GET/POST /sessions/{id}/attendance` (bulk upsert) |
| Comments | `GET /sessions/{id}/comments`, `PUT /sessions/{id}/comments/{studentId}` |
| Grades | `GET/POST /grades`, `PUT/DELETE /grades/{id}` |
| Billing | `GET /bills`, `POST /bills/generate`, `GET /bills/{id}/pdf`, `GET /bills/export` |

## Authentication

1. Register a tenant: `POST /api/auth/register-tenant`
2. Login: `POST /api/auth/login` — returns `{ accessToken, user }`
3. Include the token in every subsequent request: `Authorization: Bearer <accessToken>`

Token carries `userId`, `tenantId`, and `role` claims. All data access is automatically scoped to the tenant.

## CI/CD

GitHub Actions workflow at `.github/workflows/ci.yml`:

- Triggers on every push and every PR to `main` or `develop`
- `backend-test`: `mvn verify` with Testcontainers (pulls its own Postgres)
- `backend-build`: packages fat JAR
- `frontend-test`: `vitest run`
- `frontend-build`: TypeScript type check + `vite build`

See `.github/CONTRIBUTING.md` for the full branch strategy and PR process.
