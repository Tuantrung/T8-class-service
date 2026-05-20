# Technical Decisions — Tutoring Class Management Service

Architecture Decision Records for every major technical choice in this project.

---

## ADR-001: Frontend Framework

**Decision:** React 18 with TypeScript, built with Vite.

**Alternatives considered:**
- Vue 3 + Vite
- Next.js (React SSR)
- Thymeleaf (server-side templates, no separate frontend)
- HTMX + Spring MVC templates

**Rationale:**
React has the largest talent pool and component ecosystem. TypeScript provides compile-time safety on the API contract shared between frontend and backend developers. Vite gives sub-second HMR, critical for development velocity. A pure SPA is sufficient given the desktop-first, authenticated-only nature of this app — SSR (Next.js) adds complexity with no SEO benefit.

**Trade-offs:**
- Requires a separate build step and more complex deployment vs. server-rendered templates.
- HTMX would have been simpler but limits interactivity for the attendance grid and Excel import flow.

---

## ADR-002: Database

**Decision:** PostgreSQL 15.

**Alternatives considered:**
- SQLite
- MySQL 8
- H2 (embedded, tests only)

**Rationale:**
The project must be multi-tenant from day 1 for a future SaaS pivot. PostgreSQL's `UUID` primary keys, `gen_random_uuid()`, row-level security (available as a future hardening layer), `TIMESTAMPTZ` correctness, and JSONB extension support all matter for a growing SaaS. SQLite would be simpler for a single-user app but cannot handle concurrent writes across multiple centers without locking. MySQL is a reasonable alternative but PostgreSQL has better standards compliance and better support in Spring/Hibernate.

**Trade-offs:**
- Requires a running Postgres instance in dev (Docker Compose mitigates this).
- Heavier than SQLite for initial setup.

---

## ADR-003: ORM / Data Access

**Decision:** Spring Data JPA (Hibernate 6) with Flyway for migrations.

**Alternatives considered:**
- jOOQ (type-safe SQL DSL)
- MyBatis
- Plain JDBC / Spring JdbcTemplate
- Liquibase (instead of Flyway)

**Rationale:**
Spring Data JPA is already part of the Spring Boot scaffold and reduces CRUD boilerplate to near zero via repository interfaces. Hibernate 6's `@Filter` is the cleanest mechanism for transparent tenant scoping — every query automatically includes the tenant discriminator without developer effort. Flyway is SQL-first (unlike Liquibase XML/YAML), making migrations easy to review in code review and easy to reason about.

**Trade-offs:**
- Hibernate's N+1 problem requires careful use of `@EntityGraph` or `JOIN FETCH` in performance-sensitive queries (attendance list, bill calculation).
- jOOQ would give compile-time SQL safety but requires significant additional setup and a code-generation step.

---

## ADR-004: Authentication Mechanism

**Decision:** JWT (JSON Web Token) with JJWT library, access token in memory (Zustand state), optional httpOnly refresh token cookie.

**Alternatives considered:**
- Spring Session + Redis (server-side sessions)
- Spring Session + database-backed sessions
- OAuth2 / OpenID Connect (Keycloak, Auth0)
- Cookie-only sessions (Spring Security default)

**Rationale:**
JWT is stateless — the server carries no session state, which simplifies horizontal scaling. The `tenantId` and `role` claims in the JWT allow `TenantContextFilter` to scope every request without a database lookup on every call. JJWT is the de-facto Java JWT library with active maintenance.

Token stored in Zustand memory (not localStorage) avoids XSS token theft. An httpOnly refresh token cookie can be added post-MVP for silent re-auth.

OAuth2/Keycloak is over-engineered for a first version of a single-product app.

**Trade-offs:**
- Stateless JWT means token revocation requires a denylist (Redis/DB lookup) — deferred to post-MVP. In MVP, tokens expire after 8 hours.
- Memory-only access token means users must re-login after a hard page refresh. Acceptable for MVP; refresh token cookie addresses this in v2.

---

## ADR-005: Multi-Tenancy Pattern

**Decision:** Shared schema with `tenant_id` discriminator column on every business entity table, enforced via Hibernate `@Filter` activated per-request.

**Alternatives considered:**
- Schema-per-tenant (each center gets its own PostgreSQL schema)
- Database-per-tenant (each center gets its own Postgres database)
- Row-Level Security (PostgreSQL RLS) at the DB layer

**Rationale:**
For an MVP with expected < 50 tenants, shared schema is by far the simplest to operate. Schema-per-tenant and database-per-tenant require dynamic datasource routing, separate Flyway migration runs per tenant, and operational complexity multiplied by tenant count. PostgreSQL RLS is a strong option for future hardening but requires more DB-level expertise and makes local development harder. Hibernate `@Filter` places tenant enforcement in application code where it is visible, testable, and auditable.

**Trade-offs:**
- A bug in tenant filtering could expose cross-tenant data — mitigated by integration tests that verify scoping and by the Hibernate filter being on by default.
- Large tenants on shared tables require good indexing strategy (all indexes include `tenant_id` as prefix column).
- Schema-per-tenant would give stronger isolation; can be migrated to later if a large enterprise customer requires it.

---

## ADR-006: PDF Generation Library

**Decision:** iText 7 Community Edition.

**Alternatives considered:**
- Apache PDFBox
- Flying Saucer (XHTML to PDF via CSS)
- OpenPDF (fork of iText 5)
- JasperReports
- Headless Chrome / Puppeteer (external process)

**Rationale:**
iText 7 Community is the most capable pure-Java PDF library with active maintenance. Bill PDFs are relatively simple (table layout, text, logo) — iText 7's layout API handles this without needing an external process. JasperReports is heavyweight. Flying Saucer requires well-formed XHTML and is less maintained. PDFBox is better suited to reading/editing PDFs than generating them programmatically.

**Trade-offs:**
- iText 7 Community is AGPL-licensed; for commercial SaaS use, a commercial license may be required. OpenPDF (LGPL) is the fallback if licensing becomes a concern.
- iText 7 API is more verbose than template-based approaches (Flying Saucer, Puppeteer). Accept this for the MVP; a Thymeleaf → HTML → PDF pipeline is a reasonable later upgrade.

---

## ADR-007: Excel Import Library

**Decision:** Apache POI 5.x for `.xlsx` parsing.

**Alternatives considered:**
- EasyExcel (Alibaba)
- FastExcel
- Plain CSV import instead of Excel

**Rationale:**
Apache POI is the Java standard for Office formats with near-universal adoption. The import use case is simple (single sheet, handful of columns), so POI's SXSSF streaming API is not needed — the standard `XSSFWorkbook` is sufficient for files of a few hundred rows. EasyExcel is a good alternative but adds a less-common dependency; for a simple use case, POI is preferred.

**Trade-offs:**
- POI adds ~5 MB to the JAR. Acceptable.
- For very large imports (10,000+ rows), SXSSF streaming should replace `XSSFWorkbook`. Out of scope for MVP.

---

## ADR-008: API Style

**Decision:** RESTful JSON API with a consistent envelope: `{ data: T, error: string, message: string }`.

**Alternatives considered:**
- GraphQL
- tRPC (TypeScript end-to-end)
- gRPC
- HATEOAS REST

**Rationale:**
REST is the most widely understood API style, works naturally with Spring MVC, and requires no additional runtime on the frontend. GraphQL would reduce over-fetching but adds significant complexity (schema definition, resolver setup, N+1 tooling) for a small surface area. tRPC is excellent but requires the entire stack to be TypeScript — incompatible with the Java backend. HATEOAS adds response complexity with no benefit for a controlled SPA client.

**Trade-offs:**
- REST requires the frontend to know URL structure. Well-defined API contract document (`api-contract.md`) mitigates this.
- No automatic type safety between backend and frontend (unlike tRPC). Mitigated by manually maintaining TypeScript interfaces in `api-contract.md` as the shared contract.

---

## ADR-009: UI Component Library

**Decision:** shadcn/ui components + Tailwind CSS.

**Alternatives considered:**
- Ant Design (antd)
- Material UI (MUI)
- Chakra UI
- Bootstrap + React Bootstrap

**Rationale:**
shadcn/ui copies components directly into the project (no runtime component library dependency), built on Radix UI primitives for accessibility, and styled with Tailwind CSS utility classes. This gives full control over markup and style without fighting a third-party theme system. Ant Design and MUI are full design systems that are hard to customize and add significant bundle weight. Tailwind avoids custom CSS files, keeping styles co-located with components.

**Trade-offs:**
- shadcn/ui components must be manually updated when the library releases changes (no `npm update`). Acceptable for MVP scope.
- Tailwind class verbosity in JSX can reduce readability; use `cn()` utility and component extraction to manage this.

---

## ADR-010: State Management (Frontend)

**Decision:** TanStack Query (React Query) for server state + Zustand for client-only auth state.

**Alternatives considered:**
- Redux Toolkit + RTK Query
- Zustand for everything
- React Context + useReducer
- SWR

**Rationale:**
The vast majority of client state in this app is server state (data fetched from the API). TanStack Query handles caching, background refetching, loading/error states, and mutations with minimal boilerplate. Zustand is used only for the auth token and user profile (client-side state that does not come from an API call). This avoids setting up Redux for what amounts to one slice.

**Trade-offs:**
- Two state libraries instead of one. The separation of concerns (server vs. client state) justifies this.
- TanStack Query v5 has a significantly different API from v4; developers must use v5 docs.
