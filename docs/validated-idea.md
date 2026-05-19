# Validated Idea: Class Management & Monthly Billing Tool for Tutoring Centers

**Document Status:** Validated — Ready for Spec & Design
**Date:** 2026-05-19
**Author:** Product Strategy Review

---

## Problem Statement

Teachers at tutoring centers spend untracked manual effort in Excel to manage class rosters, record attendance, and calculate monthly tuition — then must manually compose and send individual billing messages to parents via Zalo. This creates error-prone billing, lost session records, and no audit trail.

---

## Target Users

### Persona 1 — "The Classroom Teacher" (Primary)
- Works at a single tutoring center
- Manages up to 20 classes, each with up to 35 students
- Runs ~2 sessions per week per class
- Currently uses Excel for rosters and manual arithmetic for billing
- Sends monthly tuition bills to parents via Zalo
- Pain: calculating fees manually is slow and prone to mistakes; tracking attendance across classes is fragmented

### Persona 2 — "The Center Admin / Owner" (Secondary, V1-lite)
- Oversees multiple teachers at one center
- Needs visibility into which classes are active and whether bills have been sent
- Does not necessarily manage classes directly in V1

### Persona 3 — "The Parent" (Recipient, not a system user in V1)
- Receives a Zalo message with their child's monthly bill
- Does not log into the system in V1

---

## Core Value Proposition

A purpose-built web tool that replaces Excel for tutoring centers: track classes, rosters, and attendance in one place, then generate and dispatch monthly bills to parents via Zalo with one action — eliminating manual calculation errors and the copy-paste billing workflow.

**Why not just keep using Excel?**
Excel requires manual formula maintenance, has no built-in Zalo integration, offers no per-session attendance history, and cannot enforce business rules (e.g., prorate missed sessions). This tool removes all of those gaps.

---

## MVP Feature List (P0 / P1 / P2)

### P0 — Must-Have for Launch (Core loop cannot function without these)

| # | Feature | Description |
|---|---------|-------------|
| 1 | Teacher account & login | Each teacher has their own login; data is scoped to that teacher |
| 2 | Class management | Create, edit, deactivate a class (name, schedule, tuition rate per session or per month) |
| 3 | Student roster per class | Add/remove students from a class; store student name and parent Zalo contact |
| 4 | Session / attendance tracking | Mark each session as held; mark per-student attendance (present / absent) |
| 5 | Monthly bill calculation | Auto-calculate tuition per student based on sessions attended in the billing month |
| 6 | Bill preview | Teacher reviews the calculated bill before sending |
| 7 | Bill export / summary view | Generate a printable bill summary per student per month (screen view + PDF download) |
| 8 | Billing history | View past months' bills per student — date generated, amount, sessions attended |

### P1 — High Value, Ship in First Iteration After P0 Stabilizes

| # | Feature | Description |
|---|---------|-------------|
| 9 | Bulk bill send | Send all bills for a class in one action instead of one-by-one |
| 10 | Bill send status | Track whether a Zalo message was sent successfully per student per month |
| 11 | Class dashboard | Summary view: active classes, students per class, sessions this month, bills sent vs. pending |
| 12 | Multi-teacher support under one center | Center admin account can see all teachers' classes (read-only) |

### P2 — Nice to Have, Defer to Next Cycle

| # | Feature | Description |
|---|---------|-------------|
| 13 | Custom bill message template | Teacher can edit the Zalo message format |
| 14 | Prorated / makeup session rules | Handle policy for excused absences or makeup classes affecting billing |
| 15 | Export to PDF/Excel | Download billing records for accounting |
| 16 | Student payment tracking | Record whether the parent has paid the bill (cash/transfer) |
| 17 | Notification reminders | Auto-remind teacher to send bills at end of month |

---

## User Roles & Permissions

| Role | Capabilities |
|------|-------------|
| Teacher | Full CRUD on their own classes, students, sessions, bills. Cannot see other teachers' data. |
| Center Admin | Read-only view of all classes and billing status across all teachers. Cannot edit teacher data in V1. |
| Parent | No system access in V1. Receives Zalo message only. |

Note: Role management UI (assigning roles, inviting teachers) is a V1 feature only at the minimal level — a center admin manually creates teacher accounts. No self-registration flow in V1.

---

## Key Assumptions

1. **Zalo integration is feasible for the target users.** We are betting that either Zalo OA API access is obtainable, or a deep-link/manual copy-paste fallback is acceptable for V1 launch.
2. **Flat monthly billing per class is the dominant model.** Teachers charge a fixed rate per session attended; complex pricing tiers are edge cases deferred to V2.
3. **One teacher = one center in V1.** We are not building multi-center hierarchy for V1. A teacher's data is isolated to their account.
4. **35 students per class is a hard ceiling for V1.** The stated maximum is 35; we build to that constraint and do not over-engineer for scale.
5. **Parents have Zalo and their contact is known to the teacher.** The teacher is responsible for entering correct Zalo contacts; we do not manage contact verification.
6. **Web browser on desktop is the primary interface for teachers.** Mobile web is a bonus, not a requirement in V1.
7. **There is no payment collection in V1.** The tool tracks billing sent, not whether the parent has paid.

---

## Risks & Mitigations

### Technical Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Zalo integration | N/A | Deferred | Zalo bill sending moved to backlog. V1 generates bill on-screen and as PDF only. |

### Adoption Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Teachers find data entry slower than Excel initially | High | Medium | Prioritize fast roster import; reduce friction on session marking UI |
| Single center only; value is thin until commercialized | Low | Low | Acceptable for internal tool phase; revisit at SaaS pivot |

### Business Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| SaaS pivot requires multi-tenancy rebuild | Medium | High | Design data model with tenant isolation from day one even if UI for multi-tenant is not exposed in V1 |
| No monetization pressure means scope creep goes unchecked | Medium | Medium | Lock V1 scope to this document; any new feature request gets a V2 label |

---

## Success Metrics

These are the quantifiable outcomes that define whether V1 is working.

| Metric | Target at 30 days post-launch |
|--------|-------------------------------|
| Active teachers using the tool weekly | All teachers at the center (minimum 2) |
| Sessions logged per week per teacher | Matches real class schedule (>= 2 per class) |
| Time to send monthly bills for one class | Under 5 minutes (vs. estimated 30+ minutes in Excel) |
| Billing errors reported by parents | 0 incorrect bill amounts sent |
| Bills sent via system vs. manual Zalo | >= 80% of monthly bills sent through the tool |

---

## Out of Scope for V1

The following are explicitly deferred. Raising them as V1 requirements will be pushed back.

| Feature | Rationale for Deferral |
|---------|------------------------|
| Mobile native app (iOS/Android) | Web first; build audience before investing in native |
| Offline mode | No stated requirement; adds significant complexity |
| Payment collection / tracking | Billing sent != paid; payment tracking is a separate workflow |
| Student self-service portal | Parents are recipients only; no portal in V1 |
| Multi-center / franchise hierarchy | Only one center in scope; architecture deferred |
| Advanced data security / compliance (PDPA, etc.) | Noted as backlog by stakeholder; must be addressed before SaaS launch |
| Automated reminders / scheduling | V2 after core billing loop is stable |
| Reporting & analytics dashboard | V2; not needed for internal tool phase |
| Integration with payment gateways (MoMo, VNPay) | V2+ only |
| Custom roles / permissions editor | Hard-coded roles sufficient for V1 |
| Self-registration / onboarding flow | Admin manually creates accounts in V1 |

---

## Tech Preferences & Notes

These are not decisions — they are context notes for the engineering team.

- **Platform:** Web application, desktop-first. Mobile-responsive is a bonus.
- **Backend language hint:** Project scaffolded as a Spring (Java) service based on repository structure.
- **Deployment:** No stated preference; assumed cloud-hosted (not on-premise).
- **Data isolation note:** Even though V1 serves one center, the data model must be built with tenant/center isolation so that a SaaS pivot does not require a schema rewrite.
- **Zalo integration:** Plan for two modes — (a) full API integration if Zalo OA is approved, (b) generated message text for manual copy-paste as fallback. Do not block V1 delivery on Zalo API approval.
- **Data protection:** Explicitly deferred to backlog by stakeholder. Must be revisited before any public SaaS launch. At minimum, passwords must be hashed and student/parent data must not be exposed across teacher accounts.

---

## Resolved Decisions (2026-05-19)

1. **Zalo OA account:** Center does NOT yet have a Zalo Official Account. Zalo bill sending is **deferred to backlog**. V1 bill feature = generate bill summary (PDF/screen view) only. No Zalo integration in MVP.
2. **Billing calculation rule:** Fee is calculated **per session attended**. Students only pay for sessions they were present. Absences are deducted.
3. **Teacher-cancelled sessions:** If a teacher marks a session as cancelled, **no students are charged** for that session. Cancelled sessions are excluded from monthly billing calculation entirely.
