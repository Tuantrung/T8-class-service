# Product Specification: Tutoring Class Management Web App

**Version:** 1.0  
**Date:** 2026-05-19  
**Status:** Draft — MVP Scope  
**Author:** Technical Product Manager  

---

## Table of Contents

1. [Functional Requirements](#1-functional-requirements)
2. [Non-Functional Requirements](#2-non-functional-requirements)
3. [User Stories](#3-user-stories)
4. [Data Model](#4-data-model)
5. [Business Rules](#5-business-rules)
6. [Excel Import Template Spec](#6-excel-import-template-spec)

---

## 1. Functional Requirements

### Feature 1 — Teacher Login (Multi-Teacher, One Center)

**FR-001** — The system shall allow a teacher to log in using their registered email address and password.  
**FR-002** — The system shall reject login attempts with an unrecognized email or an incorrect password, displaying a generic error message that does not reveal which field is wrong.  
**FR-003** — Each teacher account shall be associated with exactly one center. A teacher cannot access data belonging to a different center.  
**FR-004** — The system shall store passwords as bcrypt hashes (minimum cost factor 12). Plaintext passwords shall never be stored or logged.  
**FR-005** — The system shall issue a signed JWT (or equivalent session token) upon successful login. The token shall expire after 8 hours of inactivity.  
**FR-006** — The system shall provide a logout action that immediately invalidates the current session token.  
**FR-007** — A teacher shall only be able to view and modify data (classes, students, sessions, grades, bills) that belongs to their own account. Cross-teacher data access shall return HTTP 403.  
**FR-008** — An administrator account for the center shall exist to create and deactivate teacher accounts. The admin role is out of MVP scope for self-service; accounts are created manually or via a seeded setup endpoint.  

---

### Feature 2 — Class CRUD

**FR-009** — A logged-in teacher shall be able to create a new class by providing: class name, subject, grade level, schedule (days of week + time slot), start date, end date, and fee per session.  
**FR-010** — A logged-in teacher shall be able to view a list of all classes they own, showing at minimum: class name, subject, grade level, schedule summary, and active/archived status.  
**FR-011** — A logged-in teacher shall be able to view the full detail of a single class they own.  
**FR-012** — A logged-in teacher shall be able to update any editable field of a class they own.  
**FR-013** — A logged-in teacher shall be able to archive (soft-delete) a class. Archived classes shall be hidden from the default list view but accessible via a filter toggle.  
**FR-014** — Hard deletion of a class shall not be permitted if the class has any associated students, sessions, or billing records.  
**FR-015** — Class name must be unique within the same teacher's account.  
**FR-016** — Fee per session shall be a positive decimal value greater than zero.  

---

### Feature 3 — Student Roster per Class

**FR-017** — A logged-in teacher shall be able to add a student to a class manually by providing: full name, date of birth, parent/guardian name, contact phone number, and an optional note.  
**FR-018** — A student record is scoped to a class. The same real-world student attending two different classes shall have two separate student records.  
**FR-019** — A logged-in teacher shall be able to remove a student from a class. If the student has existing attendance, comment, or grade records, the removal shall be a soft-delete (the records are preserved but the student is marked inactive in the roster).  
**FR-020** — A logged-in teacher shall be able to upload an Excel file (.xlsx) to bulk-import students into a class. The system shall parse the file using the defined template (see Section 6).  
**FR-021** — On Excel import, the system shall validate each row. Rows that fail validation shall be skipped and reported back to the teacher in a summary response (row number, field, error reason). Valid rows shall be imported.  
**FR-022** — On Excel import, duplicate detection shall be based on the combination of full name + date of birth within the same class. Duplicate rows shall be skipped with a warning (not an error).  
**FR-023** — A class shall support a maximum of 50 active students (see BR-003).  
**FR-024** — The student roster shall be viewable as a paginated list, sorted by full name ascending by default.  

---

### Feature 4 — Session Attendance Tracking

**FR-025** — The system shall automatically generate session records for a class based on the class schedule (days of week, start date, end date) when the class is created. Sessions can also be added manually.  
**FR-026** — Each session record shall have: session date, session number (sequential), status (scheduled / completed / cancelled-by-teacher / cancelled-by-student-holiday), and an optional note.  
**FR-027** — A logged-in teacher shall be able to open a session and record attendance for each student in the class roster as either: Present, Absent (unexplained), or Absent (excused).  
**FR-028** — Attendance for a session can be saved incrementally (partial save is allowed) and updated until the session is marked completed.  
**FR-029** — Once a session is marked completed, its attendance records shall be locked. An edit requires the teacher to explicitly re-open the session, which generates an audit log entry.  
**FR-030** — A teacher shall be able to mark a session as cancelled-by-teacher. Cancelled sessions shall not count toward billing (see BR-002).  
**FR-031** — The attendance view shall show, per session, the count of present / absent students and the completion status.  

---

### Feature 5 — Student Comment / Feedback per Session

**FR-032** — During or after recording attendance for a session, a teacher shall be able to enter a free-text comment for each individual student.  
**FR-033** — Comments are optional. A session can be completed without any comments.  
**FR-034** — Comment length shall not exceed 1000 characters per student per session.  
**FR-035** — A teacher shall be able to view a chronological log of all comments for a specific student across all sessions in a class.  
**FR-036** — Comments shall be editable until the session is marked completed. After completion, comments are read-only unless the session is explicitly re-opened.  

---

### Feature 6 — Grade Entry per Exam / Test

**FR-037** — A logged-in teacher shall be able to create an exam/test record for a class by providing: exam name, exam date, maximum score, and an optional description.  
**FR-038** — After creating an exam, the teacher shall be able to enter a numeric score for each student in the class roster.  
**FR-039** — A score must be a non-negative number and shall not exceed the exam's maximum score.  
**FR-040** — Scores for a student in a class shall be viewable in a grade summary table: one row per student, one column per exam.  
**FR-041** — A teacher shall be able to edit a score after initial entry. Edits shall be permitted at any time (no locking on grades).  
**FR-042** — Students who were not present for an exam may have their score left blank (null). Blank scores are displayed as "—" in the grade table.  

---

### Feature 7 — Monthly Billing Calculation

**FR-043** — A teacher shall be able to generate a bill for a specific student for a specific month within a class.  
**FR-044** — The billing calculation shall apply the formula: **Bill Amount = Number of Billable Sessions Attended × Fee per Session** (see BR-001).  
**FR-045** — A billable session is one where: (a) the session status is completed (not cancelled-by-teacher), and (b) the student's attendance is Present.  
**FR-046** — Teacher-cancelled sessions (status = cancelled-by-teacher) shall be excluded from the billable count entirely, regardless of the student's recorded attendance (see BR-002).  
**FR-047** — The system shall display a bill breakdown showing: each session date, attendance status, whether it is billable, the per-session fee, and the running total.  
**FR-048** — A teacher shall be able to mark a bill as paid (status: unpaid / paid). The paid date shall be recorded.  
**FR-049** — Once a bill is marked paid, it shall be locked (read-only). Recalculation is not permitted after payment without explicit admin intervention (out of MVP).  
**FR-050** — A teacher shall be able to view a billing summary per class per month: list of students, sessions attended count, amount due, and payment status.  

---

### Feature 8 — Bill Export (On-Screen View + PDF Download)

**FR-051** — A teacher shall be able to view a formatted bill on-screen for any student-month combination. The view shall include: center name, teacher name, class name, student name, billing month, itemized session list, total amount due, and payment status.  
**FR-052** — A teacher shall be able to download the on-screen bill as a PDF file. The PDF shall be visually consistent with the on-screen view.  
**FR-053** — The PDF filename shall follow the pattern: `HoaDon_[StudentName]_[YYYY-MM].pdf` (example: `HoaDon_NguyenVanA_2026-04.pdf`).  
**FR-054** — PDF generation shall complete within 5 seconds for a single bill. If generation exceeds this threshold, the system shall return an error and allow retry.  
**FR-055** — The on-screen bill view and PDF shall be printable (print-friendly CSS or equivalent).  

---

## 2. Non-Functional Requirements

### 2.1 Performance

**NFR-001** — API response time for all list endpoints (classes, students, sessions, grades) shall be under 500 ms at p95 for data sets up to 50 students and 200 sessions.  
**NFR-002** — Excel import of up to 50 student rows shall complete processing within 3 seconds.  
**NFR-003** — PDF generation (FR-054) shall complete within 5 seconds.  
**NFR-004** — The frontend initial page load (first contentful paint) shall be under 3 seconds on a standard broadband connection.  

### 2.2 Security

**NFR-005** — All passwords shall be hashed using bcrypt with a minimum cost factor of 12. Plaintext passwords shall never appear in logs, responses, or the database.  
**NFR-006** — All API endpoints except `/auth/login` shall require a valid authentication token. Unauthenticated requests shall return HTTP 401.  
**NFR-007** — All data access shall be scoped to the authenticated teacher's account. Requests for resources belonging to another teacher shall return HTTP 403 (not 404, to avoid data enumeration where the ID itself is not sensitive, but see NFR-008).  
**NFR-008** — Resource IDs (class ID, student ID, etc.) shall use UUIDs (v4) to prevent sequential enumeration attacks.  
**NFR-009** — All HTTP communication shall be over HTTPS (TLS 1.2 minimum). HTTP traffic shall be redirected to HTTPS.  
**NFR-010** — The application shall implement CSRF protection for all state-changing operations.  
**NFR-011** — API rate limiting shall be enforced: 60 requests per minute per authenticated teacher; 10 login attempts per minute per IP address. Exceeding limits returns HTTP 429.  
**NFR-012** — Uploaded Excel files shall be validated for file type (MIME type must be `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`) and size (maximum 5 MB) before processing.  
**NFR-013** — No sensitive student data (names, phone numbers, dates of birth) shall appear in server-side logs.  

### 2.3 Data Isolation Between Teachers

**NFR-014** — Every database query for class, student, session, attendance, comment, grade, and bill data shall include a filter on the authenticated teacher's ID. This shall be enforced at the service layer, not only at the controller layer.  
**NFR-015** — A teacher cannot invite or share access to their classes with another teacher in MVP.  

### 2.4 Browser Support

**NFR-016** — The web application shall function correctly on the latest two major versions of: Google Chrome, Mozilla Firefox, Microsoft Edge, and Safari (macOS and iOS).  
**NFR-017** — The application shall be usable on mobile screen sizes (minimum 375px viewport width) with a responsive layout.  

### 2.5 Accessibility

**NFR-018** — The application shall meet WCAG 2.1 Level AA conformance for all core workflows (login, class list, attendance entry, bill view).  
**NFR-019** — All form inputs shall have visible, associated labels. Error messages shall be programmatically associated with their inputs.  

### 2.6 Availability and Data Integrity

**NFR-020** — The application shall use database transactions for any operation that writes to multiple tables atomically (e.g., bill calculation writing a Bill record and its line items).  
**NFR-021** — The system shall perform daily automated backups of the database. Backups shall be retained for 30 days.  

---

## 3. User Stories

### Feature 1 — Teacher Login

**US-101: Login with valid credentials**  
As a teacher, I want to log in with my email and password so that I can access my classes and student data securely.

Acceptance Criteria:  
- Given I am on the login page and I enter a registered email and correct password, when I submit the form, then I am redirected to my class dashboard and a session token is issued.  
- Given I enter a correct email but wrong password, when I submit, then I see the message "Email hoặc mật khẩu không đúng" and I remain on the login page.  
- Given I enter an email not registered in the system, when I submit, then I see the same generic error message (not a specific "email not found" message).  
- Given I have been idle for 8 hours, when I try to access a protected page, then I am redirected to the login page and shown "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại."

Complexity: S  
Dependencies: None  

---

**US-102: Logout**  
As a teacher, I want to log out so that my account is protected if I leave my computer unattended.

Acceptance Criteria:  
- Given I am logged in, when I click "Đăng xuất", then my session token is invalidated and I am redirected to the login page.  
- Given my session token is invalidated, when any request is made with the old token, then the system returns HTTP 401.

Complexity: S  
Dependencies: US-101  

---

**US-103: Data scoped to my account**  
As a teacher, I want to be certain that I only see my own classes and student data so that student privacy is maintained.

Acceptance Criteria:  
- Given I am logged in as Teacher A, when I navigate to the classes list, then I only see classes that belong to Teacher A.  
- Given I am logged in as Teacher A and I attempt to access a class ID that belongs to Teacher B, then I receive an "Không có quyền truy cập" error (HTTP 403).

Complexity: S (framework-level concern, tested in integration)  
Dependencies: US-101  

---

### Feature 2 — Class CRUD

**US-201: Create a class**  
As a teacher, I want to create a new class with a schedule and fee so that I can start tracking students and sessions.

Acceptance Criteria:  
- Given I am on the "Tạo lớp mới" form, when I fill in all required fields (name, subject, grade, schedule, start date, end date, fee per session) and submit, then a new class is created and I am redirected to the class detail page.  
- Given I submit the form with the class name left blank, then a validation error "Tên lớp là bắt buộc" is displayed and the class is not created.  
- Given I enter a fee per session of 0 or a negative number, then a validation error "Học phí mỗi buổi phải lớn hơn 0" is displayed.  
- Given I enter a class name that already exists among my classes, then a validation error "Tên lớp đã tồn tại" is displayed.

Complexity: M  
Dependencies: US-101  

---

**US-202: View class list**  
As a teacher, I want to see all my active classes in a list so that I can quickly navigate to any class.

Acceptance Criteria:  
- Given I have 3 active classes, when I navigate to the dashboard, then all 3 classes are displayed with name, subject, grade, and schedule summary.  
- Given I have archived 1 class, when I view the default list, then the archived class is not shown.  
- Given I toggle "Hiển thị lớp đã lưu trữ", then archived classes appear in the list with a visual indicator.

Complexity: S  
Dependencies: US-201  

---

**US-203: Edit a class**  
As a teacher, I want to update class details so that I can correct mistakes or reflect schedule changes.

Acceptance Criteria:  
- Given I open a class detail and edit the fee per session to a new valid value, when I save, then the updated fee is displayed and future bills use the new rate.  
- Given I change the end date to a date before the start date, then a validation error "Ngày kết thúc phải sau ngày bắt đầu" is displayed.

Complexity: S  
Dependencies: US-201  

---

**US-204: Archive a class**  
As a teacher, I want to archive a finished class so that it no longer clutters my active list but data is preserved.

Acceptance Criteria:  
- Given I archive a class, then it disappears from the default class list.  
- Given I archive a class that has students and sessions, then all associated data is retained and viewable after toggling the archived filter.

Complexity: S  
Dependencies: US-202  

---

### Feature 3 — Student Roster

**US-301: Add a student manually**  
As a teacher, I want to manually add a student to a class so that I can start tracking their attendance and grades.

Acceptance Criteria:  
- Given I am on the class roster page and click "Thêm học sinh", when I fill in full name and date of birth (minimum required fields) and save, then the student appears in the roster list.  
- Given I leave the full name blank, then a validation error "Họ tên là bắt buộc" is shown and the student is not saved.  
- Given the class already has 50 active students, when I try to add another, then I see the error "Lớp đã đạt giới hạn 50 học sinh."

Complexity: S  
Dependencies: US-201  

---

**US-302: Import students via Excel**  
As a teacher, I want to upload an Excel file to add multiple students at once so that I save time when setting up a new class.

Acceptance Criteria:  
- Given I download the Excel template, when I fill it in correctly and upload it, then all valid rows are imported and I see a success message with the count of students added.  
- Given a row is missing the required "Họ và tên" field, then that row is skipped and shown in an error summary: "Dòng 3: Họ và tên là bắt buộc."  
- Given a row has a duplicate (same name + date of birth already exists in the class), then it is skipped with a warning: "Dòng 5: Học sinh đã tồn tại trong lớp (bỏ qua)."  
- Given I upload a file that is not .xlsx format, then I see "Vui lòng chỉ tải lên file Excel (.xlsx)."  
- Given I upload a file larger than 5 MB, then I see "File vượt quá dung lượng cho phép (tối đa 5MB)."

Complexity: L  
Dependencies: US-301  

---

**US-303: Remove a student from a class**  
As a teacher, I want to remove a student from a class so that they no longer appear in attendance or billing.

Acceptance Criteria:  
- Given a student has no attendance or grade records, when I remove them, they are hard-deleted from the roster.  
- Given a student has existing attendance records, when I remove them, they are soft-deleted (marked inactive) and their historical records are preserved.  
- Given a student is soft-deleted, they do not appear in the active roster or in future attendance sheets.

Complexity: S  
Dependencies: US-301  

---

### Feature 4 — Session Attendance Tracking

**US-401: Auto-generate sessions**  
As a teacher, I want sessions to be automatically created when I set up a class schedule so that I do not have to create each session manually.

Acceptance Criteria:  
- Given a class has a schedule of "Monday and Wednesday, 18:00–19:30" with start date 2026-06-01 and end date 2026-08-31, when I create the class, then all Monday and Wednesday sessions between those dates are created automatically.  
- Given I add an extra ad-hoc session manually, then it is appended to the session list with a sequential number.

Complexity: M  
Dependencies: US-201  

---

**US-402: Record attendance for a session**  
As a teacher, I want to mark each student as present or absent for a session so that I have an accurate attendance record for billing.

Acceptance Criteria:  
- Given I open a session, I see a list of all active students in the class with attendance status defaulting to "Chưa điểm danh."  
- Given I mark 8 students as Present and 2 as Absent, when I save, then the session shows "8 có mặt / 2 vắng."  
- Given I save a partially completed attendance sheet, then it is saved without error and I can return later to complete it.  
- Given a session is marked completed, when I try to edit attendance, then fields are read-only and I see a button "Mở lại buổi học."

Complexity: M  
Dependencies: US-401, US-301  

---

**US-403: Mark a session as cancelled by teacher**  
As a teacher, I want to mark a session as cancelled (by me) so that students are not billed for it.

Acceptance Criteria:  
- Given I mark a session as "Giáo viên hủy", then that session's status changes and it is visually distinct in the session list.  
- Given a session is marked "Giáo viên hủy", then it does not appear as a billable session in any bill calculation.

Complexity: S  
Dependencies: US-401  

---

### Feature 5 — Student Comment per Session

**US-501: Add a comment for a student in a session**  
As a teacher, I want to write a comment for each student after a session so that I can track individual progress and communicate with parents.

Acceptance Criteria:  
- Given I am on the session attendance page, when I click the comment icon next to a student, then a text area opens where I can type up to 1000 characters.  
- Given I type 1001 characters and save, then a validation error "Nhận xét không được vượt quá 1000 ký tự" is shown.  
- Given a session is completed, then all comment fields are read-only.

Complexity: S  
Dependencies: US-402  

---

**US-502: View comment history for a student**  
As a teacher, I want to view all past comments for a student so that I can track their learning journey over time.

Acceptance Criteria:  
- Given I open a student's profile within a class, when I navigate to the "Nhận xét" tab, then all comments across all sessions are listed in reverse chronological order (most recent first).  
- Given a student has no comments, then I see "Chưa có nhận xét nào."

Complexity: S  
Dependencies: US-501  

---

### Feature 6 — Grade Entry

**US-601: Create an exam and enter scores**  
As a teacher, I want to create a test record and enter scores for each student so that I can track academic performance.

Acceptance Criteria:  
- Given I create an exam with name "Kiểm tra giữa kỳ", max score 100, and date 2026-07-15, when I save, then the exam appears in the grade table as a new column.  
- Given I enter a score of 110 for a student on an exam with max score 100, then I see the error "Điểm không được vượt quá điểm tối đa (100)."  
- Given I enter a negative score, then I see the error "Điểm không được âm."  
- Given a student was absent for the exam, when I leave their score blank, then it displays as "—" in the grade table.

Complexity: M  
Dependencies: US-301  

---

### Feature 7 — Monthly Billing Calculation

**US-701: Generate a monthly bill for a student**  
As a teacher, I want to generate a monthly bill for a student so that I can inform parents of the amount owed.

Acceptance Criteria:  
- Given Student A attended 8 out of 10 sessions in June, and Teacher cancelled 1 session (leaving 9 billable sessions), and the fee is 100,000 VND/session, then the bill shows: 8 billable sessions attended × 100,000 = 800,000 VND.  
- Given I generate the bill, then I see an itemized list: each session date, status (billable/không tính phí), attendance, and subtotal.  
- Given a bill has already been calculated and I mark it as paid, then the "Trạng thái" field shows "Đã thanh toán" with the payment date.

Complexity: L  
Dependencies: US-402, US-201  

---

**US-702: View billing summary for a class**  
As a teacher, I want to see a billing summary for all students in a class for a given month so that I can track who has paid and who owes.

Acceptance Criteria:  
- Given I select "Lớp A" and "Tháng 6/2026", then I see a table with one row per active student, showing sessions attended, amount due, and payment status.  
- Given 3 students have paid and 2 have not, then the paid rows are visually distinguished (e.g., green indicator) from unpaid rows.

Complexity: M  
Dependencies: US-701  

---

### Feature 8 — Bill Export

**US-801: View formatted bill on screen**  
As a teacher, I want to view a formatted bill for a student so that I can review it before sharing with parents.

Acceptance Criteria:  
- Given I navigate to a student's bill for a specific month, then I see a formatted view including: center name, teacher name, class name, student name, month, itemized session list, total, and payment status.  
- Given the bill is unpaid, then the total is highlighted and a "Đánh dấu đã thanh toán" button is visible.

Complexity: S  
Dependencies: US-701  

---

**US-802: Download bill as PDF**  
As a teacher, I want to download a bill as a PDF so that I can send it to parents via messaging apps or print it.

Acceptance Criteria:  
- Given I click "Tải xuống PDF" on a bill view, then a PDF file is downloaded with the name `HoaDon_[TenHocSinh]_[YYYY-MM].pdf`.  
- Given the PDF generation fails (timeout or server error), then I see "Không thể tạo file PDF. Vui lòng thử lại." and the download does not start.  
- Given the PDF is downloaded, its content matches the on-screen bill view exactly (same line items, totals, and student information).

Complexity: M  
Dependencies: US-801  

---

## 4. Data Model

### 4.1 Center

| Field          | Type     | Notes                                      |
|----------------|----------|--------------------------------------------|
| id             | UUID     | Primary key                                |
| name           | String   | Display name of the tutoring center        |
| address        | String   | Optional; shown on bills                   |
| phone          | String   | Optional; shown on bills                   |
| created_at     | DateTime |                                            |

---

### 4.2 Teacher

| Field          | Type     | Notes                                          |
|----------------|----------|------------------------------------------------|
| id             | UUID     | Primary key                                    |
| center_id      | UUID     | FK → Center; not nullable                      |
| full_name      | String   | Display name on bills                          |
| email          | String   | Unique; used for login                         |
| password_hash  | String   | bcrypt hash; never exposed via API             |
| is_active      | Boolean  | Deactivated teachers cannot log in             |
| created_at     | DateTime |                                                |
| updated_at     | DateTime |                                                |

Relations: belongs to one Center; has many Classes.

---

### 4.3 Class

| Field             | Type     | Notes                                                  |
|-------------------|----------|--------------------------------------------------------|
| id                | UUID     | Primary key                                            |
| teacher_id        | UUID     | FK → Teacher; not nullable                             |
| name              | String   | Unique within teacher; e.g., "Toán 8A"                 |
| subject           | String   | e.g., "Toán", "Anh Văn"                                |
| grade_level       | String   | e.g., "Lớp 8"                                         |
| schedule_days     | Array    | e.g., ["MON", "WED"]                                   |
| schedule_time     | String   | e.g., "18:00–19:30"                                    |
| start_date        | Date     |                                                        |
| end_date          | Date     | Must be after start_date                               |
| fee_per_session   | Decimal  | > 0; currency in VND                                   |
| status            | Enum     | active / archived                                      |
| created_at        | DateTime |                                                        |
| updated_at        | DateTime |                                                        |

Relations: belongs to one Teacher; has many Students, Sessions.

---

### 4.4 Student

| Field              | Type     | Notes                                             |
|--------------------|----------|---------------------------------------------------|
| id                 | UUID     | Primary key                                       |
| class_id           | UUID     | FK → Class; not nullable                          |
| full_name          | String   | Required                                          |
| date_of_birth      | Date     | Required; used for duplicate detection on import  |
| parent_name        | String   | Optional                                          |
| contact_phone      | String   | Optional; format validated if provided            |
| note               | String   | Optional free text                                |
| is_active          | Boolean  | false = soft-deleted from roster                  |
| created_at         | DateTime |                                                   |

Relations: belongs to one Class; has many Attendances, Comments, Grades.

---

### 4.5 Session

| Field          | Type     | Notes                                                        |
|----------------|----------|--------------------------------------------------------------|
| id             | UUID     | Primary key                                                  |
| class_id       | UUID     | FK → Class; not nullable                                     |
| session_number | Integer  | Sequential within a class; auto-assigned                     |
| session_date   | Date     |                                                              |
| start_time     | Time     | Optional; from class schedule                                |
| status         | Enum     | scheduled / completed / cancelled_teacher / cancelled_holiday|
| note           | String   | Optional teacher note about the session                      |
| created_at     | DateTime |                                                              |
| updated_at     | DateTime |                                                              |

Relations: belongs to one Class; has many Attendances, Comments.

---

### 4.6 Attendance

| Field        | Type     | Notes                                                  |
|--------------|----------|--------------------------------------------------------|
| id           | UUID     | Primary key                                            |
| session_id   | UUID     | FK → Session                                           |
| student_id   | UUID     | FK → Student                                           |
| status       | Enum     | present / absent_unexplained / absent_excused / unmarked|
| created_at   | DateTime |                                                        |
| updated_at   | DateTime |                                                        |

Constraint: unique on (session_id, student_id).  
Relations: belongs to one Session and one Student.

---

### 4.7 Comment

| Field        | Type     | Notes                          |
|--------------|----------|--------------------------------|
| id           | UUID     | Primary key                    |
| session_id   | UUID     | FK → Session                   |
| student_id   | UUID     | FK → Student                   |
| content      | String   | Max 1000 characters            |
| created_at   | DateTime |                                |
| updated_at   | DateTime |                                |

Constraint: unique on (session_id, student_id) — one comment per student per session.

---

### 4.8 Exam

| Field         | Type     | Notes                              |
|---------------|----------|------------------------------------|
| id            | UUID     | Primary key                        |
| class_id      | UUID     | FK → Class                         |
| name          | String   | e.g., "Kiểm tra 15 phút"           |
| exam_date     | Date     |                                    |
| max_score     | Decimal  | > 0                                |
| description   | String   | Optional                           |
| created_at    | DateTime |                                    |

---

### 4.9 Grade

| Field       | Type     | Notes                                         |
|-------------|----------|-----------------------------------------------|
| id          | UUID     | Primary key                                   |
| exam_id     | UUID     | FK → Exam                                     |
| student_id  | UUID     | FK → Student                                  |
| score       | Decimal  | Nullable (null = absent/not graded); 0 ≤ score ≤ max_score|
| created_at  | DateTime |                                               |
| updated_at  | DateTime |                                               |

Constraint: unique on (exam_id, student_id).

---

### 4.10 Bill

| Field            | Type     | Notes                                             |
|------------------|----------|---------------------------------------------------|
| id               | UUID     | Primary key                                       |
| class_id         | UUID     | FK → Class                                        |
| student_id       | UUID     | FK → Student                                      |
| billing_month    | String   | Format: YYYY-MM (e.g., "2026-06")                 |
| sessions_billed  | Integer  | Count of billable sessions attended               |
| fee_per_session  | Decimal  | Snapshot of the rate at time of calculation       |
| total_amount     | Decimal  | sessions_billed × fee_per_session                 |
| status           | Enum     | unpaid / paid                                     |
| paid_at          | DateTime | Nullable; set when status = paid                  |
| created_at       | DateTime |                                                   |
| updated_at       | DateTime |                                                   |

Constraint: unique on (class_id, student_id, billing_month).

---

### 4.11 BillLineItem

| Field         | Type     | Notes                                                          |
|---------------|----------|----------------------------------------------------------------|
| id            | UUID     | Primary key                                                    |
| bill_id       | UUID     | FK → Bill                                                      |
| session_id    | UUID     | FK → Session                                                   |
| session_date  | Date     | Snapshot                                                       |
| attendance    | Enum     | present / absent / not_applicable                              |
| is_billable   | Boolean  | true only if session not cancelled and attendance = present    |
| amount        | Decimal  | fee_per_session if is_billable, else 0                         |

---

## 5. Business Rules

**BR-001: Bill calculation formula**  
Bill Total = COUNT(BillLineItems WHERE is_billable = true) × fee_per_session (snapshot at bill creation time).  
The fee_per_session used in the bill is the value recorded on the Class at the time the bill is generated, not the current class value if it has since been edited.

**BR-002: Cancelled sessions are not billed**  
Any session with status = cancelled_teacher is excluded from billing entirely. Even if a student is recorded as "Present" on a cancelled session (e.g., due to a data entry error), that session shall not be treated as billable.

**BR-003: Maximum students per class**  
A class shall not have more than 50 active (is_active = true) students at any time. Attempts to add a student when the count is already 50 shall be rejected with an appropriate error message.

**BR-004: Bill locking after payment**  
Once a Bill record has status = paid, no fields on that Bill or its BillLineItems shall be modifiable through the standard API. This prevents retroactive tampering with paid billing records.

**BR-005: Session lock after completion**  
Once a Session has status = completed, its associated Attendance and Comment records become read-only through the standard API. Modifications require explicitly changing the session status back to "scheduled" (re-open action), which is logged.

**BR-006: Score boundary constraint**  
A Grade score must satisfy: 0 ≤ score ≤ exam.max_score. A null score is permitted and represents a student who did not sit the exam.

**BR-007: Unique bill per student-month-class**  
Only one Bill record may exist for a given combination of (class_id, student_id, billing_month). If a teacher attempts to generate a bill for a student-month that already has a bill, the system shall offer to view the existing bill rather than creating a duplicate.

**BR-008: Class date ordering**  
A class end_date must be strictly after start_date. start_date and end_date must both be valid calendar dates. Sessions cannot be manually created outside the class start_date–end_date range.

**BR-009: Teacher data ownership**  
A teacher can only perform CRUD operations on data under their own teacher_id. This is enforced at the service layer by always joining through teacher_id before returning or modifying any record.

**BR-010: Comment uniqueness per session per student**  
Only one Comment record may exist per (session_id, student_id) pair. Submitting a second comment for the same pair performs an update (upsert), not an insert.

**BR-011: Excel import partial success**  
An Excel import is non-atomic at the row level. Valid rows are committed to the database even if other rows fail validation. The teacher receives a summary of imported rows and skipped rows with reasons.

**BR-012: Inactive students excluded from billing and attendance**  
Students with is_active = false shall not appear on attendance sheets for future sessions and shall not be included in bill calculations for months after their deactivation date. Historical records remain intact.

---

## 6. Excel Import Template Spec

### 6.1 File Requirements

- Format: `.xlsx` (Excel 2007+, OOXML)
- Maximum file size: 5 MB
- Maximum rows: 50 data rows (excluding the header row)
- Sheet name: The system reads the first sheet regardless of its name

### 6.2 Column Definitions

| Column | Header Label (Vietnamese) | Header Label (English Reference) | Required | Data Type | Validation Rules |
|--------|---------------------------|-----------------------------------|----------|-----------|------------------|
| A      | Họ và tên                 | Full Name                         | Yes      | Text      | Non-empty; max 100 characters |
| B      | Ngày sinh                 | Date of Birth                     | Yes      | Date      | Format DD/MM/YYYY; must be a valid past date; student must be between 3 and 25 years old |
| C      | Tên phụ huynh             | Parent / Guardian Name            | No       | Text      | Max 100 characters |
| D      | Số điện thoại             | Contact Phone                     | No       | Text      | If provided: 10–11 digits, Vietnamese mobile format (e.g., 09xxxxxxxx, 03xxxxxxxx) |
| E      | Ghi chú                   | Note                              | No       | Text      | Max 500 characters; truncated if longer (with warning) |

### 6.3 Row 1 — Header Row

Row 1 must contain the column headers exactly as shown above (case-insensitive match is acceptable). If the header row is missing or the required columns A and B cannot be identified, the entire import is rejected with the error: "File không đúng định dạng mẫu. Vui lòng tải và sử dụng file mẫu."

### 6.4 Data Rows

Data begins on row 2. Empty rows (all cells blank) are silently skipped. The system processes rows until it encounters 3 consecutive empty rows or reaches the end of the sheet.

### 6.5 Import Result Response

The API response for a completed import shall include:

```json
{
  "imported_count": 12,
  "skipped_count": 3,
  "errors": [
    {
      "row": 4,
      "field": "Ngày sinh",
      "message": "Định dạng ngày không hợp lệ. Sử dụng DD/MM/YYYY."
    },
    {
      "row": 7,
      "field": "Họ và tên",
      "message": "Họ và tên là bắt buộc."
    }
  ],
  "warnings": [
    {
      "row": 9,
      "field": null,
      "message": "Học sinh đã tồn tại trong lớp (bỏ qua)."
    }
  ]
}
```

### 6.6 Template Download

The system shall provide a downloadable `.xlsx` template file at `GET /api/v1/templates/student-import`. The template shall include:
- Row 1: Column headers with the Vietnamese labels defined in Section 6.2
- Row 2: A sample data row with placeholder values (grayed out or italicized)
- Row 3 onward: Empty rows ready for data entry
- The template shall include a second sheet named "Hướng dẫn" (Instructions) explaining each column, required format, and examples.

---

*End of Specification v1.0*
