# UX Design Specification
# Tutoring Class Management Web App — MVP

**Audience:** Vietnamese tutors and teachers managing private or small-group tutoring classes.
**Platform:** Desktop-first (minimum 1280px design width). Mobile breakpoint at 768px for essential read-only views.
**Design philosophy:** Data density first. Teachers are migrating from Excel. Every screen should feel as efficient as a spreadsheet but with guardrails that prevent data entry errors.

---

## 1. Information Architecture

### 1.1 Page Hierarchy

```
/ (root)
├── /login                        — Teacher authentication
├── /dashboard                    — Class list overview (authenticated home)
├── /classes/new                  — Create new class
├── /classes/:classId             — Class detail (tabbed)
│   ├── ?tab=students             — Student roster
│   ├── ?tab=sessions             — Session list + attendance
│   ├── ?tab=grades               — Grade entry per exam
│   └── ?tab=billing              — Monthly billing
├── /classes/:classId/sessions/new          — Record a new session
├── /classes/:classId/sessions/:sessionId   — Edit / view session detail
└── /classes/:classId/billing/:month        — Monthly bill view + PDF export
```

### 1.2 Navigation Hierarchy

```
Level 0 — Public
  /login

Level 1 — Authenticated Root
  /dashboard

Level 2 — Class Scoped
  /classes/new
  /classes/:classId  (tabs are sub-views, not separate routes)

Level 3 — Session / Billing Scoped
  /classes/:classId/sessions/new
  /classes/:classId/sessions/:sessionId
  /classes/:classId/billing/:month
```

### 1.3 Navigation Rules

- All routes except `/login` require an authenticated session. Unauthenticated requests redirect to `/login?redirect=<original-path>`.
- After login, the teacher is redirected to `/dashboard` (or the `redirect` query param destination).
- The persistent top navigation bar shows the app name on the left and the teacher's display name + logout link on the right. It is visible on all authenticated pages.
- Breadcrumbs appear on Level 2 and Level 3 pages: `Dashboard > [Class Name] > [Section]`.

---

## 2. User Flows

### 2.1 New Teacher Onboarding — First Class Created

```
Step 1: Teacher visits /login
        → Enters email + password
        → Clicks "Dang nhap" (Log In)

Step 2: First-time login state detected (no classes exist)
        → Redirected to /dashboard
        → Empty state banner shown:
          "Ban chua co lop hoc nao. Tao lop dau tien de bat dau."
          [Button: + Tao lop hoc]

Step 3: Teacher clicks "+ Tao lop hoc"
        → Navigated to /classes/new

Step 4: Teacher fills Create Class form:
        - Class name (required)
        - Subject (required, dropdown: Toan, Ly, Hoa, Van, Anh, Khac)
        - Level / grade (required, dropdown)
        - Schedule (text field, e.g. "Thu 2, 4, 6 — 18:00")
        - Rate per session (required, number, VND)
        - Start date (date picker)
        → Clicks "Luu lop hoc"

Step 5: Class created
        → Redirected to /classes/:classId?tab=students
        → Success toast: "Tao lop hoc thanh cong"
        → Empty state on Students tab prompts adding first student
```

### 2.2 Add Students — Manual Entry

```
Step 1: Teacher is on /classes/:classId?tab=students
        → Clicks "+ Them hoc sinh" button

Step 2: Inline add row appears at the top of the student table
        - Full name (text input, required)
        - Date of birth (date input, optional)
        - Parent phone (text input, optional)
        - Notes (text input, optional)

Step 3: Teacher fills fields and presses Enter or clicks checkmark icon
        → Student row saved inline
        → Row becomes read-only; edit icon appears on hover
        → Another blank row ready to receive next entry (power-entry mode)

Step 4: Teacher presses Escape or clicks "Xong" to exit add mode
```

### 2.3 Add Students — Excel Import

```
Step 1: Teacher is on /classes/:classId?tab=students
        → Clicks "Nhap tu Excel" button

Step 2: Import modal opens
        → Instructional text: "Tai xuong mau file Excel"
        → [Download template] link (downloads students_template.xlsx)
        → Drag-and-drop zone: "Keo file vao day hoac bam de chon"
        → Accepted formats: .xlsx, .xls, .csv

Step 3: Teacher uploads file
        → Parsing progress shown (spinner inside drop zone)
        → Preview table rendered: first 5 rows shown; full count stated
        → Columns mapped: Ten hoc sinh | Ngay sinh | SDT phu huynh | Ghi chu
        → Error rows highlighted in red with column-level error messages

Step 4: Teacher reviews preview
        → "Nhap [N] hoc sinh" button (disabled if any error rows exist)
        → Teacher can remove error rows using row-level delete icon
        → "Huy" cancels import and closes modal

Step 5: Teacher clicks "Nhap [N] hoc sinh"
        → Loading state on button
        → Modal closes on success
        → Toast: "Da nhap [N] hoc sinh thanh cong"
        → Student table refreshes
```

### 2.4 Record a Session (Attendance + Comments)

```
Step 1: Teacher is on /classes/:classId?tab=sessions
        → Clicks "+ Ghi buoi hoc moi"

Step 2: Navigated to /classes/:classId/sessions/new

Step 3: Session header form:
        - Date (date picker, defaults to today)
        - Session number (auto-incremented, editable)
        - Topic / lesson title (text, optional)
        - Status (radio: Dien ra binh thuong | Da huy)
          → If "Da huy" selected: attendance table hidden, reason field appears

Step 4: Attendance table (one row per student):
        ┌─────────────────────────────────────────────────────────┐
        │ # │ Ten hoc sinh    │ Co mat │ Vang mat │ Nhan xet      │
        ├─────────────────────────────────────────────────────────┤
        │ 1 │ Nguyen Van A    │  (o)   │   ( )    │ [text input]  │
        │ 2 │ Tran Thi B      │  ( )   │   (o)    │ [text input]  │
        └─────────────────────────────────────────────────────────┘
        Default: all students marked "Co mat" (present)
        Teacher toggles individual rows

Step 5: Teacher clicks "Luu buoi hoc"
        → Saved, redirected to /classes/:classId?tab=sessions
        → Toast: "Da luu buoi hoc so [N]"
        → Session row appears in sessions list
```

### 2.5 Enter Grades for a Test

```
Step 1: Teacher is on /classes/:classId?tab=grades

Step 2: First time — empty state with "+ Them bai kiem tra" button
        Teacher clicks it → Grade entry modal opens:
        - Exam/test name (text, required, e.g. "Kiem tra 15 phut so 1")
        - Date (date picker, required)
        - Max score (number, required, default 10)
        - Notes (text, optional)
        → Clicks "Tao bai kiem tra"

Step 3: Grade entry table appears (inline editing):
        ┌──────────────────────────────────────────┐
        │ # │ Ten hoc sinh    │ Diem  │ Nhan xet    │
        ├──────────────────────────────────────────┤
        │ 1 │ Nguyen Van A    │ [8.5] │ [text]      │
        │ 2 │ Tran Thi B      │ [   ] │ [text]      │
        └──────────────────────────────────────────┘
        - Score cell: numeric input, validates 0 — max score
        - Tab key moves to next score cell (Excel-like navigation)
        - Empty score = not yet entered (shown as dash "—")

Step 4: Teacher fills scores using Tab to move between cells
        → Auto-save after each cell loses focus (debounced 800ms)
        → "Da luu" indicator in header updates on each save

Step 5: Teacher can add multiple exams; selector at top of Grades tab
        switches between exams
```

### 2.6 Generate and Export Monthly Bill

```
Step 1: Teacher is on /classes/:classId?tab=billing

Step 2: Month selector (month/year picker) shown at top
        Teacher selects month (defaults to current month)

Step 3: System calculates automatically:
        - Lists all sessions in the selected month
        - Flags cancelled sessions (excluded from billing)
        - Counts attended sessions per student
        - Multiplies attended sessions × rate per session

Step 4: Bill summary table:
        ┌────────────────────────────────────────────────────────────┐
        │ Ten hoc sinh │ So buoi da hoc │ Don gia    │ Thanh tien   │
        ├────────────────────────────────────────────────────────────┤
        │ Nguyen Van A │       8        │ 150,000 d  │ 1,200,000 d  │
        │ Tran Thi B   │       6        │ 150,000 d  │   900,000 d  │
        ├────────────────────────────────────────────────────────────┤
        │ Tong cong    │                │            │ 2,100,000 d  │
        └────────────────────────────────────────────────────────────┘

Step 5: Teacher reviews bill
        → "Xem chi tiet" expands a student row to show session-by-session
          breakdown (date, present/absent, amount counted)

Step 6: Export
        → "Xuat PDF" button → triggers PDF download:
          filename: "hoadon_[ClassName]_[YYYY-MM].pdf"
        → "In" button → opens browser print dialog (print-optimized CSS)
        → "Chia se" button (optional) → copies sharable link to clipboard
```

---

## 3. Wireframes (ASCII)

### 3.1 Login Page — /login

```
╔══════════════════════════════════════════════════════════════════════╗
║                                                                      ║
║                        CLASS MANAGER                                 ║
║                   Quan ly lop hoc tieu hoc                          ║
║                                                                      ║
║              ┌─────────────────────────────────┐                    ║
║              │          DANG NHAP              │                    ║
║              │                                 │                    ║
║              │  Email                          │                    ║
║              │  ┌───────────────────────────┐  │                    ║
║              │  │ giao.vien@email.com        │  │                    ║
║              │  └───────────────────────────┘  │                    ║
║              │                                 │                    ║
║              │  Mat khau                       │                    ║
║              │  ┌───────────────────────────┐  │                    ║
║              │  │ ••••••••                  │  │                    ║
║              │  └───────────────────────────┘  │                    ║
║              │                                 │                    ║
║              │  [ ] Ghi nho dang nhap          │                    ║
║              │                                 │                    ║
║              │  ┌───────────────────────────┐  │                    ║
║              │  │       DANG NHAP           │  │                    ║
║              │  └───────────────────────────┘  │                    ║
║              │                                 │                    ║
║              │  Quen mat khau?                 │                    ║
║              └─────────────────────────────────┘                    ║
║                                                                      ║
╚══════════════════════════════════════════════════════════════════════╝

States:
  Default     — form empty, button enabled
  Loading     — button shows spinner + "Dang dang nhap...", inputs disabled
  Error       — red banner above form: "Email hoac mat khau khong dung"
  Field error — red border + message below field on blur if empty
```

---

### 3.2 Dashboard — /dashboard

```
╔══════════════════════════════════════════════════════════════════════╗
║  CLASS MANAGER          Nguyen Thi Giao Vien  [Dang xuat]           ║
╠══════════════════════════════════════════════════════════════════════╣
║                                                                      ║
║  LOP HOC CUA TOI                      [+ Tao lop hoc moi]           ║
║                                                                      ║
║  ┌──────────┬────────────────────────────────────────────────────┐  ║
║  │ Tim kiem │ [___________________________] Loc: [Tat ca  ▼]     │  ║
║  └──────────┴────────────────────────────────────────────────────┘  ║
║                                                                      ║
║  ┌──────┬──────────────────┬────────┬────────────┬──────┬───────┐  ║
║  │  #   │ Ten lop          │ Mon    │ Hoc sinh   │ Lich │ TT    │  ║
║  ├──────┼──────────────────┼────────┼────────────┼──────┼───────┤  ║
║  │  1   │ Toan 6A          │ Toan   │ 12 em      │ T2T4 │ ACTIVE│  ║
║  │  2   │ Tieng Anh CB     │ Anh    │  8 em      │ T3T5 │ ACTIVE│  ║
║  │  3   │ Ly 8 Nang cao    │ Ly     │ 15 em      │ T6   │ ACTIVE│  ║
║  │  4   │ Van 7 (cu)       │ Van    │  6 em      │ —    │ PAUSED│  ║
║  └──────┴──────────────────┴────────┴────────────┴──────┴───────┘  ║
║                                                                      ║
║  Hien thi 4 / 4 lop hoc                                             ║
║                                                                      ║
╚══════════════════════════════════════════════════════════════════════╝

Notes:
  - Each row is clickable → navigates to /classes/:classId?tab=students
  - TT (Trang thai) column: ACTIVE = green badge, PAUSED = grey badge
  - Row hover: light blue background highlight
  - Empty state (no classes): centered illustration + text +
    large "+ Tao lop hoc dau tien" button
  - Loading state: table rows replaced by 4 skeleton rows
```

---

### 3.3 Class Detail Page — /classes/:classId

```
╔══════════════════════════════════════════════════════════════════════╗
║  CLASS MANAGER          Nguyen Thi Giao Vien  [Dang xuat]           ║
╠══════════════════════════════════════════════════════════════════════╣
║  < Dashboard > Toan 6A                                               ║
╠══════════════════════════════════════════════════════════════════════╣
║                                                                      ║
║  TOAN 6A                             [Chinh sua] [Vo hieu hoa]       ║
║  Mon: Toan  |  12 hoc sinh  |  Thu 2, 4  |  150,000d/buoi           ║
║                                                                      ║
║  ┌──────────┬────────────┬──────────┬────────────────────────────┐  ║
║  │ HOC SINH │  BUOI HOC  │  DIEM    │  HOA DON                   │  ║
║  └──────────┴────────────┴──────────┴────────────────────────────┘  ║
║                                                                      ║
║  [STUDENTS TAB ACTIVE — see 3.3a]                                    ║
║  [SESSIONS TAB — see 3.3b]                                           ║
║  [GRADES TAB — see 3.3c]                                             ║
║  [BILLING TAB — see 3.3d]                                            ║
║                                                                      ║
╚══════════════════════════════════════════════════════════════════════╝
```

#### 3.3a Students Tab

```
╔══════════════════════════════════════════════════════════════════════╗
║  HOC SINH                  [+ Them hoc sinh]  [Nhap tu Excel]        ║
║                                                                      ║
║  ┌───┬──────────────────┬────────────┬───────────────┬─────────┐   ║
║  │ # │ Ho va ten        │ Ngay sinh  │ SDT phu huynh │ Ghi chu │   ║
║  ├───┼──────────────────┼────────────┼───────────────┼─────────┤   ║
║  │ 1 │ Nguyen Van An    │ 12/03/2014 │ 0912 345 678  │         │   ║
║  │ 2 │ Tran Thi Bich    │ 05/07/2013 │ 0987 654 321  │ Hoc gioi│   ║
║  │ 3 │ Le Quang Dung    │ 20/11/2013 │ —             │         │   ║
║  │ + │ [Ho va ten    ]  │[DD/MM/YYYY]│ [0xxx xxx xxx]│ [     ] │   ║
║  └───┴──────────────────┴────────────┴───────────────┴─────────┘   ║
║                                                                      ║
║  12 hoc sinh                   [Xoa hoc sinh da chon]               ║
║                                                                      ║
╚══════════════════════════════════════════════════════════════════════╝

Notes:
  - Row 4 ("+") is the inline add row, shown when "+ Them hoc sinh" clicked
  - Pressing Enter saves the row and opens a new blank row below
  - Pressing Escape cancels and removes the blank row
  - Hovering an existing row reveals edit (pencil) and delete (trash) icons
    in a floating action column on the right side
  - Empty state: "Chua co hoc sinh nao. Them hoc sinh hoac nhap tu Excel."
```

#### 3.3b Sessions Tab

```
╔══════════════════════════════════════════════════════════════════════╗
║  BUOI HOC                                [+ Ghi buoi hoc moi]        ║
║                                                                      ║
║  ┌───┬────────────┬────────────────────┬──────────┬────────────┐   ║
║  │ # │ Ngay       │ Chu de / Bai hoc   │ Trang thai│ Di mat    │   ║
║  ├───┼────────────┼────────────────────┼──────────┼────────────┤   ║
║  │12 │ 17/05/2026 │ Phuong trinh bac 1 │ Binh thuong│ 11/12   │   ║
║  │11 │ 15/05/2026 │ On tap chuong 3    │ Binh thuong│ 12/12   │   ║
║  │10 │ 12/05/2026 │ —                  │ DA HUY   │ —          │   ║
║  │ 9 │ 10/05/2026 │ Phan so           │ Binh thuong│ 10/12   │   ║
║  └───┴────────────┴────────────────────┴──────────┴────────────┘   ║
║                                                                      ║
║  Tong: 12 buoi  |  Da huy: 1  |  Co tinh phi: 11                   ║
║                                                                      ║
╚══════════════════════════════════════════════════════════════════════╝

Notes:
  - Each row is clickable → navigates to /classes/:classId/sessions/:id
  - "DA HUY" rows shown with strikethrough text and grey background
  - "Di mat" column: "11/12" means 11 present out of 12 students
  - Empty state: "Chua ghi buoi hoc nao. Bam '+ Ghi buoi hoc moi' de bat dau."
```

#### 3.3c Grades Tab

```
╔══════════════════════════════════════════════════════════════════════╗
║  DIEM SO                              [+ Them bai kiem tra]          ║
║                                                                      ║
║  Bai kiem tra: [Kiem tra 15p so 1 (10/05) ▼]  [Sua] [Xoa]          ║
║                                                                      ║
║  ┌───┬──────────────────┬───────────┬───────────────────────────┐  ║
║  │ # │ Ho va ten        │ Diem (/10)│ Nhan xet                  │  ║
║  ├───┼──────────────────┼───────────┼───────────────────────────┤  ║
║  │ 1 │ Nguyen Van An    │ [8.5    ] │ [Can on tap chuong 2    ] │  ║
║  │ 2 │ Tran Thi Bich    │ [9.0    ] │ [                       ] │  ║
║  │ 3 │ Le Quang Dung    │ [  —    ] │ [                       ] │  ║
║  └───┴──────────────────┴───────────┴───────────────────────────┘  ║
║                                                                      ║
║  Trung binh lop: 8.75  |  Min: 7.0  |  Max: 10.0                   ║
║  [Luu tat ca]                                              [Xuat CSV]║
║                                                                      ║
╚══════════════════════════════════════════════════════════════════════╝

Notes:
  - Tab key moves focus from one score cell to the next (Excel behavior)
  - Score inputs: numeric, min 0, max = defined max score for the exam
  - Cells with unsaved changes show a subtle yellow background
  - "Luu tat ca" saves all pending changes; also auto-saved on blur
  - Empty state: "Chua co bai kiem tra nao. Tao bai kiem tra dau tien."
```

#### 3.3d Billing Tab

```
╔══════════════════════════════════════════════════════════════════════╗
║  HOA DON                                                             ║
║                                                                      ║
║  Thang: [05 / 2026 ▼]                           [Xuat PDF] [In]     ║
║                                                                      ║
║  Don gia: 150,000d / buoi                                           ║
║  So buoi tinh phi trong thang: 11  (1 buoi bi huy — khong tinh)     ║
║                                                                      ║
║  ┌──────────────────┬──────────────┬────────────┬────────────────┐  ║
║  │ Ho va ten        │ So buoi da hoc│ Don gia   │ Thanh tien     │  ║
║  ├──────────────────┼──────────────┼────────────┼────────────────┤  ║
║  │ Nguyen Van An    │     11       │ 150,000d   │  1,650,000d    │  ║
║  │ Tran Thi Bich    │      9       │ 150,000d   │  1,350,000d    │  ║
║  │ Le Quang Dung    │     10       │ 150,000d   │  1,500,000d    │  ║
║  │ ...              │              │            │                │  ║
║  ├──────────────────┼──────────────┼────────────┼────────────────┤  ║
║  │ TONG CONG (12)   │     —        │     —      │ 17,400,000d    │  ║
║  └──────────────────┴──────────────┴────────────┴────────────────┘  ║
║                                                                      ║
║  [v] Nguyen Van An — chi tiet                                        ║
║  ┌────────────┬────────────────────┬──────────┬──────────────────┐  ║
║  │ Ngay       │ Bai hoc            │ Trang thai│ Tinh phi        │  ║
║  ├────────────┼────────────────────┼──────────┼──────────────────┤  ║
║  │ 01/05/2026 │ Chuong 4           │ Co mat   │ 150,000d         │  ║
║  │ 06/05/2026 │ Luyen tap          │ Vang mat │ 150,000d         │  ║
║  │ 10/05/2026 │ —                  │ DA HUY   │ 0d (khong tinh) │  ║
║  └────────────┴────────────────────┴──────────┴──────────────────┘  ║
║                                                                      ║
╚══════════════════════════════════════════════════════════════════════╝

Notes:
  - "Tong buoi da hoc" counts sessions attended by that student (not total sessions)
  - Cancelled sessions shown in detail view with 0d and strikethrough
  - Absent sessions: student still charged (teacher's policy — configurable)
  - Empty state (no sessions in month): "Khong co buoi hoc nao trong thang nay."
```

---

### 3.4 Session Recording Page — /classes/:classId/sessions/new

```
╔══════════════════════════════════════════════════════════════════════╗
║  CLASS MANAGER          Nguyen Thi Giao Vien  [Dang xuat]           ║
╠══════════════════════════════════════════════════════════════════════╣
║  < Dashboard > Toan 6A > Ghi buoi hoc moi                           ║
╠══════════════════════════════════════════════════════════════════════╣
║                                                                      ║
║  GHI BUOI HOC SO 13                                                  ║
║                                                                      ║
║  ┌─────────────────────────────────────────────────────────────┐   ║
║  │ Ngay buoi hoc:  [17 / 05 / 2026    ]  (o) Binh thuong       │   ║
║  │ Chu de / bai:   [________________________]  ( ) Da huy       │   ║
║  │ Ghi chu buoi:   [________________________]                   │   ║
║  └─────────────────────────────────────────────────────────────┘   ║
║                                                                      ║
║  DIEM DANH                                       [Chon tat ca]      ║
║                                                  [Bo chon tat ca]   ║
║                                                                      ║
║  ┌───┬──────────────────┬────────────┬──────────────────────────┐  ║
║  │ # │ Ho va ten        │ Co mat     │ Nhan xet / ghi chu       │  ║
║  ├───┼──────────────────┼────────────┼──────────────────────────┤  ║
║  │ 1 │ Nguyen Van An    │  [x] Co mat│ [Tich cuc phat bieu   ]  │  ║
║  │ 2 │ Tran Thi Bich    │  [x] Co mat│ [                     ]  │  ║
║  │ 3 │ Le Quang Dung    │  [ ] Vang  │ [Nghi om              ]  │  ║
║  │ 4 │ Pham Minh Khoa   │  [x] Co mat│ [                     ]  │  ║
║  │ 5 │ Hoang Thu Ha     │  [x] Co mat│ [Can on phan so       ]  │  ║
║  │ 6 │ Nguyen Bao Long  │  [x] Co mat│ [                     ]  │  ║
║  └───┴──────────────────┴────────────┴──────────────────────────┘  ║
║                                                                      ║
║  Co mat: 11  |  Vang mat: 1  |  Tong: 12                           ║
║                                                                      ║
║            [Huy]                    [Luu buoi hoc]                  ║
║                                                                      ║
╚══════════════════════════════════════════════════════════════════════╝

States:
  - "Da huy" radio selected: attendance table fades out (opacity 40%),
    "Ly do huy" text field appears below the radio group
  - "Luu buoi hoc" button: loading state shows "Dang luu..." + spinner
  - After save: navigates to /classes/:classId?tab=sessions
    success toast: "Da luu buoi hoc so 13"
```

---

### 3.5 Monthly Bill View Page — /classes/:classId/billing/:month

```
╔══════════════════════════════════════════════════════════════════════╗
║  CLASS MANAGER          Nguyen Thi Giao Vien  [Dang xuat]           ║
╠══════════════════════════════════════════════════════════════════════╣
║  < Dashboard > Toan 6A > Hoa don Thang 5/2026                       ║
╠══════════════════════════════════════════════════════════════════════╣
║                                                                      ║
║  HOA DON THANG 5/2026 — LOP TOAN 6A                                 ║
║  Giao vien: Nguyen Thi Giao Vien                                     ║
║  Ngay xuat: 19/05/2026                                               ║
║                                                                      ║
║  ┌──────────────────────────────────────────────────────────────┐   ║
║  │ Tong so buoi: 12  |  Buoi da huy: 1  |  Tinh phi: 11 buoi   │   ║
║  │ Don gia: 150,000d/buoi                                        │   ║
║  └──────────────────────────────────────────────────────────────┘   ║
║                                                                      ║
║  ┌──────────────────┬──────────────┬────────────┬────────────────┐  ║
║  │ Ho va ten        │ Buoi da hoc  │ Don gia    │ Thanh tien     │  ║
║  ├──────────────────┼──────────────┼────────────┼────────────────┤  ║
║  │ Nguyen Van An    │     11       │ 150,000d   │  1,650,000d    │  ║
║  │ Tran Thi Bich    │      9       │ 150,000d   │  1,350,000d    │  ║
║  │ Le Quang Dung    │     10       │ 150,000d   │  1,500,000d    │  ║
║  │ Pham Minh Khoa   │     11       │ 150,000d   │  1,650,000d    │  ║
║  │ Hoang Thu Ha     │      8       │ 150,000d   │  1,200,000d    │  ║
║  │ Nguyen Bao Long  │     11       │ 150,000d   │  1,650,000d    │  ║
║  │ ... (6 more)     │              │            │                │  ║
║  ├──────────────────┼──────────────┼────────────┼────────────────┤  ║
║  │ TONG CONG        │              │            │ 17,400,000d    │  ║
║  └──────────────────┴──────────────┴────────────┴────────────────┘  ║
║                                                                      ║
║  [< Thang truoc]    [Thang 5/2026]    [Thang sau >]                 ║
║                                                                      ║
║  [Xuat PDF]  [In trang nay]  [Sao chep link]                        ║
║                                                                      ║
╚══════════════════════════════════════════════════════════════════════╝

PDF Export layout (print-optimized):
  - Header: school/teacher name, class name, month, generated date
  - Table: all students, no truncation
  - Footer: teacher signature line
  - Font: 11pt, table borders visible, no browser chrome
```

---

## 4. Component List

### 4.1 Navigation Components

| Component | Description | States |
|---|---|---|
| TopNav | Fixed top bar with app logo, teacher name, logout | default, mobile-collapsed |
| Breadcrumb | Hierarchical page path with clickable ancestors | default, truncated (>3 levels) |
| TabBar | Horizontal tab switcher (Students/Sessions/Grades/Billing) | default, active, hover, disabled |

### 4.2 Form Components

| Component | Description | States |
|---|---|---|
| TextInput | Single-line text field | default, focus, filled, error, disabled |
| NumberInput | Numeric input (scores, rates) | default, focus, error (out of range), disabled |
| DatePicker | Date selection, locale vi-VN | default, open, selected, error, disabled |
| MonthPicker | Month + year selector | default, open, selected |
| SelectDropdown | Single-select dropdown | default, open, selected, disabled, error |
| Checkbox | Boolean toggle for attendance | unchecked, checked, indeterminate, disabled |
| RadioGroup | Mutually exclusive options (e.g. session status) | default, selected, disabled |
| TextArea | Multi-line text (comments, notes) | default, focus, filled, error, disabled |
| FileDropZone | Drag-and-drop file upload area | idle, hover/drag-over, uploading, error, success |

### 4.3 Data Display Components

| Component | Description | States |
|---|---|---|
| DataTable | Sortable, dense table for all list views | default, loading (skeleton), empty, error |
| InlineEditRow | Table row that converts to form fields on click | read, editing, saving, error |
| ExpandableRow | Table row with collapsible detail section | collapsed, expanded |
| StatSummaryBar | Horizontal bar showing key counts/totals | default, loading |
| Badge | Colored label for status (ACTIVE, PAUSED, DA HUY) | active (green), paused (grey), cancelled (orange) |
| ScoreCell | Number input optimized for grade entry | empty, filled, invalid, saving, saved |

### 4.4 Modal Components

| Component | Description | States |
|---|---|---|
| ConfirmDialog | "Are you sure?" for destructive actions | default, loading (on confirm), error |
| CreateClassModal | Form to create a new class | default, loading, validation-error |
| CreateExamModal | Form to define a new exam/test | default, loading, validation-error |
| ImportExcelModal | File upload + preview for student import | idle, uploading, preview, error, success |
| EditClassModal | Pre-filled form to edit class details | default, loading, validation-error |

### 4.5 Feedback Components

| Component | Description | States |
|---|---|---|
| ToastNotification | Transient success/error/info messages (top-right) | success, error, info, warning |
| InlineError | Error message directly below a form field | shown, hidden |
| EmptyState | Full-area illustration + CTA for zero-data screens | default |
| SkeletonRow | Placeholder row during table loading | animated |
| PageSpinner | Full-page loading overlay for navigations | visible, hidden |
| UnsavedChangesWarning | Banner when user tries to navigate away | shown, hidden |

### 4.6 Action Components

| Component | Description | States |
|---|---|---|
| PrimaryButton | Main CTA (e.g. Luu, Tao, Nhap) | default, hover, active, loading, disabled |
| SecondaryButton | Secondary action (e.g. Huy, Chinh sua) | default, hover, active, disabled |
| IconButton | Icon-only action (edit, delete, expand) | default, hover, active, disabled |
| ExportPDFButton | PDF export trigger with progress feedback | default, loading, success, error |

---

## 5. Accessibility and UX Notes

### 5.1 Core UX Principles for Vietnamese Teachers

**5.1.1 Replace Excel muscle memory, do not fight it.**
Teachers will arrive with ingrained Excel habits. The design deliberately adopts table-first layouts, Tab-key navigation between data cells, and inline editing rather than page-per-record forms. This lowers the activation energy for switching tools.

**5.1.2 Data density over whitespace.**
Default table row height: 36px (compact). Information should be scannable at a glance. Avoid card-based layouts for data that belongs in a table. Padding and whitespace are tools for hierarchy, not decoration.

**5.1.3 Protect teachers from accidental data loss.**
Before any navigation away from unsaved changes, an `UnsavedChangesWarning` banner or browser `beforeunload` dialog must appear. Auto-save with explicit "Da luu" confirmation reduces anxiety.

**5.1.4 Vietnamese language throughout.**
All UI text, labels, placeholder text, error messages, and success messages must be in Vietnamese. Never use English labels even for technical concepts. Use "Dang nhap" not "Login", "Hoc sinh" not "Student", "Trang thai" not "Status".

**5.1.5 Bulk operations for efficiency.**
Every list that a teacher populates will have 5–25 items. Bulk select checkboxes, "Chon tat ca" / "Bo chon tat ca" actions, and keyboard shortcuts for common operations reduce session recording time.

**5.1.6 Forgiving inputs.**
- Accept both comma and period as decimal separators in score fields.
- Normalize phone number formatting on display (add spaces) regardless of how the user types.
- Date fields accept both DD/MM/YYYY and YYYY-MM-DD.
- Trim whitespace from all text inputs before saving.

**5.1.7 Offline tolerance.**
Teachers in Vietnam frequently experience intermittent connectivity. Show "Dang luu..." indicators prominently and display a "Mat ket noi — se thu lai" toast on network failure. Do not silently swallow errors.

---

### 5.2 WCAG 2.1 AA Compliance Requirements

**Color contrast.**
- Body text on white background: minimum 4.5:1 ratio (use #1A1A1A on #FFFFFF).
- Large text (18pt+) and UI components: minimum 3:1 ratio.
- Error states: never use red alone — always pair with an icon and text.
- Status badges: text within badges must meet 4.5:1 against badge background.

**Keyboard navigation.**
- All interactive elements reachable via Tab in logical document order (left-to-right, top-to-bottom).
- Tab order within the session recording page: Date → Topic → Status radio → then down each student row (Checkbox → Comment → next row).
- Focus indicator: 2px solid outline, #0055CC (blue), never removed for keyboard users.
- Escape key closes any open modal or dropdown.
- Enter key submits any focused primary action button.

**Screen reader requirements.**
- All form inputs have associated `<label>` elements (not placeholder-as-label).
- Tables use `<th scope="col">` and `<th scope="row">` correctly.
- The attendance checkbox label includes the student's name: `aria-label="Co mat — Nguyen Van An"`.
- Toast notifications use `role="alert"` and `aria-live="polite"` (success) or `aria-live="assertive"` (error).
- Loading states: `aria-busy="true"` on the container, `aria-label="Dang tai du lieu"` on spinners.
- Modal dialogs: focus trapped inside while open; `aria-modal="true"`, `aria-labelledby` pointing to modal title.
- Icon-only buttons: `aria-label` required on every `IconButton`.

**Touch target sizes.**
- All interactive elements: minimum 44x44px touch target (even if the visual element is smaller, use padding).
- Table row checkboxes in the session recording page: 44x44px tap area.

**Reduced motion.**
- All animations wrapped in `@media (prefers-reduced-motion: reduce)` — transitions fall back to instant.

---

### 5.3 Design Tokens

#### Color Palette

| Token | Value | Usage |
|---|---|---|
| `color-primary` | #1A56DB | Primary buttons, links, active tab indicator |
| `color-primary-hover` | #1347C8 | Button hover state |
| `color-primary-light` | #EBF2FF | Selected row background, active badge background |
| `color-danger` | #C81E1E | Error states, delete button |
| `color-danger-light` | #FDE8E8 | Error input background, error row highlight |
| `color-success` | #057A55 | Success toasts, ACTIVE badge |
| `color-success-light` | #DEF7EC | Success toast background |
| `color-warning` | #B45309 | Warning toasts, unsaved changes indicator |
| `color-warning-light` | #FEF3C7 | Warning backgrounds |
| `color-neutral-900` | #111827 | Primary body text |
| `color-neutral-600` | #4B5563 | Secondary text, placeholders |
| `color-neutral-300` | #D1D5DB | Borders, dividers |
| `color-neutral-100` | #F3F4F6 | Table alternating rows, disabled inputs |
| `color-neutral-50` | #F9FAFB | Page background |
| `color-white` | #FFFFFF | Card backgrounds, modals |
| `color-cancelled-bg` | #F3F4F6 | Cancelled session row background |
| `color-cancelled-text` | #9CA3AF | Cancelled session text (strikethrough) |

#### Typography Scale

| Token | Size | Weight | Line Height | Usage |
|---|---|---|---|---|
| `text-page-title` | 20px | 700 | 28px | Page headings (e.g. "TOAN 6A") |
| `text-section-title` | 16px | 600 | 24px | Section headings (e.g. "HOC SINH") |
| `text-body` | 14px | 400 | 20px | Table cell text, form labels |
| `text-body-medium` | 14px | 500 | 20px | Column headers, emphasis |
| `text-small` | 12px | 400 | 16px | Metadata, timestamps, helper text |
| `text-caption` | 11px | 400 | 14px | Badge text, table footnotes |

Font family: `"Inter", "Be Vietnam Pro", system-ui, sans-serif`
(Be Vietnam Pro as secondary for better Vietnamese diacritic rendering)

#### Spacing Scale

| Token | Value | Usage |
|---|---|---|
| `space-1` | 4px | Icon gap, tight padding |
| `space-2` | 8px | Input internal padding, badge padding |
| `space-3` | 12px | Table cell padding (horizontal) |
| `space-4` | 16px | Form field gap, card padding |
| `space-5` | 20px | Section gap |
| `space-6` | 24px | Page section margin |
| `space-8` | 32px | Major section separation |

#### Border Radii

| Token | Value | Usage |
|---|---|---|
| `radius-sm` | 4px | Input fields, table cells |
| `radius-md` | 6px | Buttons, cards, badges |
| `radius-lg` | 8px | Modals, dropdown panels |
| `radius-full` | 9999px | Pill badges |

#### Breakpoints

| Name | Min Width | Design Width | Notes |
|---|---|---|---|
| mobile | 0px | 375px | Essential views only (read attendance, check bill) |
| tablet | 768px | 768px | Tabbed navigation visible, simplified tables |
| desktop | 1024px | 1280px | Full data density, all columns visible |

---

### 5.4 Responsive Behavior

**Desktop (>= 1024px) — primary design target.**
Full table layouts with all columns. Sidebar navigation optional in future. Inline editing active.

**Tablet (768px–1023px).**
- Top navigation collapses teacher name to avatar icon.
- Tables: secondary columns (Date of birth, Notes) hidden by default; "Xem them" link reveals them.
- Tabs on Class Detail: full labels visible.
- Modal width: 600px max.

**Mobile (< 768px).**
- Session recording and bill view are the only flows optimized for mobile (teachers may check on phone).
- Tables on mobile: cards replace rows. Each card shows Name + primary value.
- Bottom action bar replaces inline footer buttons.
- Grade entry not supported on mobile (complexity too high, direct users to desktop).
- Top navigation: hamburger menu collapses all nav items.

---

### 5.5 Loading and Error State Specifications

**Page-level loading.**
Navigating to any route shows a top progress bar (thin, 3px, `color-primary`) animating from 0% to 85% then completing when data loads.

**Table loading.**
Replace table body with 5 skeleton rows. Each cell contains an animated grey rectangle (`color-neutral-100` to `color-neutral-300`, 1.5s loop).

**Form submission loading.**
The submit button enters `loading` state: spinner replaces icon, label changes to `"Dang luu..."`, button disabled. All form inputs disabled during submission.

**Network error on data fetch.**
Replace table/content area with:
```
  [!] Khong the tai du lieu.
  Kiem tra ket noi mang va thu lai.
  [Thu lai]
```
"Thu lai" button re-triggers the fetch.

**Validation errors.**
Errors appear below the relevant input field immediately on blur. The field border turns `color-danger`. The submit button remains enabled so teachers can review all errors before attempting submission.

**Empty states (per screen).**

| Screen | Empty State Text | CTA |
|---|---|---|
| Dashboard (no classes) | "Ban chua co lop hoc nao. Hay tao lop hoc dau tien de bat dau quan ly." | + Tao lop hoc |
| Students tab | "Lop hoc nay chua co hoc sinh. Them tung hoc sinh hoac nhap danh sach tu file Excel." | + Them hoc sinh |
| Sessions tab | "Chua co buoi hoc nao duoc ghi. Bam nut ben tren de ghi lai buoi hoc dau tien." | + Ghi buoi hoc moi |
| Grades tab | "Chua co bai kiem tra nao. Tao bai kiem tra de bat dau nhap diem." | + Them bai kiem tra |
| Billing tab (no sessions in month) | "Khong co buoi hoc nao trong thang [month/year]. Chon thang khac hoac ghi them buoi hoc." | (month picker) |
