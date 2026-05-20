# Project: Tutoring Class Management Service (class-service)

## Development Workflow
This project uses a multi-agent SDLC pipeline via Claude Code.
- Run `/build-product` to execute the full pipeline from idea to deployment
- Run `/process-feedback` to handle user feedback and iterate
- Run `/maintenance-check` for periodic health checks

## Agent Pipeline
Specialized subagents (defined in ~/.claude/agents/):
- product-strategist → Discovery & stakeholder validation
- spec-writer → Requirements, user stories, API contracts
- ux-designer → Wireframes, interaction flows, accessibility specs
- architect → Technical architecture & decision records
- devops → GitHub setup, CI/CD, deployment
- backend-dev → Server-side implementation
- frontend-dev → Client-side implementation
- test-engineer → Integration, E2E, accessibility, performance tests
- reviewer → Code review against specs
- security-auditor → Security vulnerability audit
- feedback-triage → Post-launch feedback processing

## Project Overview
A web-based tutoring class management tool for Vietnamese tutoring centers.
Replaces manual Excel workflows with class/student/session/billing management.

**Tech Stack:**
- Backend: Spring Boot 3.3 (Java 21), PostgreSQL 15, Flyway, JWT (JJWT), iText 7, Apache POI
- Frontend: React 18, TypeScript, Vite, shadcn/ui + Tailwind CSS, TanStack Query, Zustand, React Hook Form + Zod
- Deployment: Docker + Docker Compose (multi-stage build)

**Key architectural patterns:**
- Multi-tenant: shared schema with `tenant_id` discriminator on every table + Hibernate `@Filter`
- Auth: stateless JWT carrying `tenantId` + `role` claims; token stored in Zustand (not localStorage)
- `TenantContextFilter` extracts `tenantId` from JWT into a ThreadLocal before every request
- Every repo query is automatically scoped with `WHERE tenant_id = ?`

## Branch Strategy
- master: production (main integration branch for this project)
- develop: integration branch (PRs merge here first)
- feature/*: implementation branches (one per parallel track)
- hotfix/*: urgent bug fixes (branch from master, merge to master + develop)

## Git Conventions
- Conventional commits: `feat:`, `fix:`, `test:`, `refactor:`, `chore:`, `docs:`
- PRs require: passing CI, no lint warnings, no type errors
- Squash merge to develop, merge commit to master

## Documentation
All project docs live in `docs/` and are the source of truth:
- `validated-idea.md` — product requirements, personas, MVP scope
- `specifications.md` — user stories and functional specs
- `ux-design.md` — wireframes and UX flows
- `architecture.md` — system architecture, DB schema, component layout
- `tech-decisions.md` — ADRs (ADR-001 through ADR-010)
- `api-contract.md` — REST API contract (request/response shapes)
- `implementation-plan.md` — phased implementation plan
- `progress.md` — current build pipeline status

## Code Standards

### Backend (Java / Spring Boot)
- Package root: `com.classservice`
- All business entities extend `TenantEntity` (`@MappedSuperclass` with `tenantId UUID`)
- Role-based access: `ADMIN | TEACHER` enforced via `@PreAuthorize` at service layer
- Return `ApiResponse<T>` envelope: `{ data, error, message }` on all endpoints
- DTOs are Java `record` types in `dto/` subpackage within each domain package
- No `System.out.println` — use SLF4J logger
- Tests: JUnit 5 + Spring Boot test slices + Testcontainers (real DB, no mocks for DB layer)

### Frontend (TypeScript / React)
- TypeScript strict mode — no `any` types
- All API types defined in `frontend/src/api/types.ts`
- Server state via TanStack Query hooks in `frontend/src/hooks/`
- Client auth state via Zustand in `frontend/src/store/authStore.ts`
- Forms: React Hook Form + Zod schemas in `frontend/src/lib/validations/`
- No `console.log` in production code
- Tests: Vitest + React Testing Library, co-located (`*.test.ts` next to source)

## Deployment
- Docker multi-stage build: `eclipse-temurin:21-jdk-alpine` (build) → `eclipse-temurin:21-jre-alpine` (runtime)
- Maven installed via `apk add --no-cache maven` in the build stage (no Maven wrapper in repo)
- Runtime env vars injected via `.env` file (NOT committed to git): `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRY_HOURS`, `CORS_ALLOWED_ORIGINS`
- Start: `docker compose up -d`
- App runs on port `8080`; PostgreSQL on port `5433` (host)
- GitHub remote: `https://github.com/Tuantrung/T8-class-service.git`

## Domain Model Summary
- `Tenant` — tutoring center
- `AppUser` — ADMIN or TEACHER, scoped to a tenant
- `TutoringClass` — has `teacher_id FK → app_user`, `status: ACTIVE|ARCHIVED`
- `Student` — enrolled via `class_student` join table
- `Session` — scheduled meeting of a class (can be `cancelled_by_teacher`)
- `Attendance` — per student per session: `PRESENT | ABSENT | LATE`
- `Comment` — teacher note per student per session
- `Grade` — exam score per student per class
- `Bill` — monthly billing record: `DRAFT | ISSUED | PAID`

## Known Gaps (Backlog)
- `ClassDto` only exposes `teacherId` UUID — needs `teacherName + teacherEmail` embedded
- No role filter on `GET /api/auth/users` — needs `?role=TEACHER` query param
- `POST /api/classes` not restricted to ADMIN role in code
- `ClassService.createClass` does not validate that assignee has `role == TEACHER`
- Zalo bill sending deferred to backlog (V1 = PDF/screen bill only)
- JWT token revocation (denylist) deferred to post-MVP
- Refresh token (httpOnly cookie) deferred to post-MVP
