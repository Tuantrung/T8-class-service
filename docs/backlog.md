# Backlog

## Known Gaps (carried from CLAUDE.md at project init)

- `ClassDto` only exposes `teacherId` UUID — needs `teacherName + teacherEmail` embedded
- No role filter on `GET /api/auth/users` — needs `?role=TEACHER` query param
- `POST /api/classes` not restricted to ADMIN role in code
- `ClassService.createClass` does not validate that assignee has `role == TEACHER`
- Zalo bill sending deferred to backlog (V1 = PDF/screen bill only)
- JWT token revocation (denylist) deferred to post-MVP
- Refresh token (httpOnly cookie) deferred to post-MVP

---

## Bugs

### BUG-001 — Import Excel không enroll học sinh vào class_student ✅ Fixed 2026-05-20
- `ClassController.importStudents()` nhận `classId` trên URL nhưng truyền vào `studentService.importStudents(file)` không có classId. Students được tạo trong bảng `student` nhưng không có dòng nào ghi vào `class_student` → không hiển thị trên UI.
- **Fix:** Thêm `ExcelImportService.importStudents(file, classId)` overload tạo student + ghi `ClassStudent` enrollment trong cùng transaction. `ClassController` gọi method mới này.
- **Branch:** `hotfix/import-students-not-enrolled`

### BUG-002 — Template Excel thiếu cột school_name ✅ Fixed 2026-05-20
- `generateTemplate()` được viết trước khi `school_name` được thêm vào `Student`. Template chỉ có 4 cột (`Full Name | Phone | Parent Phone | Notes`), thiếu cột thứ 5.
- **Fix:** Thêm `"School Name"` vào header template, thêm `getCellValue(row, 4)` vào cả 2 import parsers, update `isBlankRow` check từ 4 lên 5 cột.
- **Branch:** `hotfix/import-students-not-enrolled`

---

## Feature Requests

### FEAT-001: Add "School Name" field to Add Student screen + show already-enrolled students ✅ Done 2026-05-20

- **Requested:** 2026-05-20
- **Value:** Medium
- **Effort:** S
- **Description:** On the "Add Student to Class" screen, teachers need two improvements: (1) a "school name" free-text field on the student creation/import form so the school affiliation is captured at enrolment time, and (2) a visible list of students already added to the class so duplicates are avoided without trial-and-error. The `Student` entity exists in the backend; `school_name` is a new nullable column. The already-enrolled list is a UI concern — the data is available via the existing class-student join table.
- **User quote:** "Add 'school name' field + show already-added/imported students"
- **Aligns with V1 scope?** Yes — student enrolment is core MVP; this is a data-field addition and a UX completeness fix on an existing screen.

---

### FEAT-002: Session screen — student attendance, per-student comments, and session progress field ✅ Done 2026-05-20

- **Requested:** 2026-05-20
- **Value:** High
- **Effort:** M
- **Description:** The Session detail screen currently has no in-app workflow for the teacher. Three capabilities are requested: (1) display the student roster for the session, (2) allow marking each student's attendance (PRESENT / ABSENT / LATE — maps to the existing `Attendance` entity), (3) allow entering a per-student comment (maps to the existing `Comment` entity), and (4) a "session progress" free-text field describing the teaching content covered (new `description` or `progress_notes` column on the `Session` entity). Backend entities `Attendance` and `Comment` already exist; the session progress field requires a schema migration and a DTO update.
- **User quote:** "Add student list, allow attendance marking, allow per-student comments, add 'session progress' text field (description of teaching content)"
- **Aligns with V1 scope?** Yes — attendance and comments are named domain entities in the MVP; the session progress field is a small in-scope extension of the Session entity.

---

### FEAT-003: Grades screen — grade table with test detail view, inline editing, and Excel import ✅ Done 2026-05-20

- **Requested:** 2026-05-20
- **Value:** High
- **Effort:** L
- **Description:** The Grades screen needs a structured table showing (index, test name, test date, details button). Clicking "Details" opens a student-grade table for that test. Teachers must be able to add or edit grades inline within that detail view. Additionally, teachers need to bulk-import grades from an Excel file (`.xlsx`), which maps to the existing Apache POI dependency in the stack. The `Grade` entity already exists. New work: test-name and test-date fields on `Grade` (or a new `GradeSheet` aggregate), inline edit UI, and an import endpoint (`POST /api/grades/import`).
- **User quote:** "Show table with (index, test name, test date, details button); Details shows student-grade table; Teacher can add/edit grades inline or import Excel file"
- **Aligns with V1 scope?** Yes for the table view and inline editing. The Excel import is a medium-complexity addition; Apache POI is already in the stack so it is within V1 technical scope. Mark import sub-feature for a dedicated implementation ticket.

---

### FEAT-004: Hide Billing screen entirely from Teacher role (Admin-only) ✅ Done 2026-05-20

- **Requested:** 2026-05-20
- **Value:** High
- **Effort:** S
- **Description:** The Billing screen must not be visible or accessible to users with the TEACHER role. This is a two-part change: (1) backend — add `@PreAuthorize("hasRole('ADMIN')")` on all `/api/bills/**` endpoints (data-integrity guard), and (2) frontend — conditionally render the Billing nav item and route only when the authenticated user's role is ADMIN (read from Zustand `authStore`). Note: the CLAUDE.md known gaps already flag that `POST /api/classes` lacks ADMIN restriction — this item is the billing equivalent and follows the same remediation pattern. These two items should be fixed together in the same PR to close the role-enforcement gap class-wide.
- **User quote:** "Hide entirely from Teacher role (Admin-only view)"
- **Aligns with V1 scope?** Yes — role-based access control is a stated V1 requirement. This is a missing enforcement, not a new feature. **Related known gap:** `POST /api/classes` not restricted to ADMIN (same root cause — `@PreAuthorize` absent on write endpoints).
