package com.classservice.billing;

import com.classservice.attendance.Attendance;
import com.classservice.attendance.AttendanceRepository;
import com.classservice.attendance.AttendanceStatus;
import com.classservice.billing.dto.BillDto;
import com.classservice.billing.dto.GenerateBillsRequest;
import com.classservice.billing.dto.GenerateBillsResult;
import com.classservice.billing.dto.UpdateBillStatusRequest;
import com.classservice.classes.ClassStudent;
import com.classservice.classes.ClassStudentRepository;
import com.classservice.classes.ClassRepository;
import com.classservice.classes.TutoringClass;
import com.classservice.common.TenantContext;
import com.classservice.common.exception.BillAlreadyIssuedException;
import com.classservice.sessions.Session;
import com.classservice.sessions.SessionRepository;
import com.classservice.students.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for billing calculation logic via BillService.generateBills().
 * No database, no Spring context — all repositories are mocked with Mockito.
 *
 * Business rules covered:
 *   BR-002 — Teacher-cancelled sessions are excluded from sessionsTotal
 *   BR-003 — Student who was absent for all sessions gets totalAmount = 0
 *   BR-004 — PRESENT and LATE statuses count; ABSENT does not
 *   ISSUED guard — Cannot regenerate a bill that is already ISSUED or PAID
 */
@ExtendWith(MockitoExtension.class)
class BillCalculationTest {

    @Mock private BillRepository billRepository;
    @Mock private ClassRepository classRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private StudentRepository studentRepository;

    @InjectMocks
    private BillService billService;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID CLASS_ID   = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final BigDecimal RATE = new BigDecimal("150000");

    private TutoringClass tutoringClass;
    private GenerateBillsRequest request;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID);

        tutoringClass = TutoringClass.builder()
            .id(CLASS_ID)
            .tenantId(TENANT_ID)
            .name("Math 101")
            .ratePerSession(RATE)
            .createdAt(Instant.now())
            .build();

        request = new GenerateBillsRequest(CLASS_ID, YearMonth.of(2024, 3));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // -----------------------------------------------------------------------
    // Happy path: student attends all sessions in the month
    // -----------------------------------------------------------------------

    @Test
    void happyPath_studentAttendsBothSessions_billEqualsSessionsTimesRate() {
        UUID session1Id = UUID.randomUUID();
        UUID session2Id = UUID.randomUUID();

        List<Session> sessions = List.of(
            buildSession(session1Id, LocalDate.of(2024, 3, 5), false),
            buildSession(session2Id, LocalDate.of(2024, 3, 12), false)
        );

        List<ClassStudent> enrollments = List.of(buildEnrollment(CLASS_ID, STUDENT_ID));

        List<Attendance> attendanceRecords = List.of(
            buildAttendance(session1Id, STUDENT_ID, AttendanceStatus.PRESENT),
            buildAttendance(session2Id, STUDENT_ID, AttendanceStatus.PRESENT)
        );

        when(classRepository.findByIdAndTenantId(CLASS_ID, TENANT_ID)).thenReturn(Optional.of(tutoringClass));
        when(sessionRepository.findByClassIdAndSessionDateBetween(CLASS_ID,
            LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31))).thenReturn(sessions);
        when(classStudentRepository.findAllByIdClassId(CLASS_ID)).thenReturn(enrollments);
        when(billRepository.findByTenantIdAndStudentIdAndClassIdAndBillingMonth(
            TENANT_ID, STUDENT_ID, CLASS_ID, LocalDate.of(2024, 3, 1))).thenReturn(Optional.empty());
        when(attendanceRepository.findByStudentIdAndSessionIdIn(STUDENT_ID, List.of(session1Id, session2Id)))
            .thenReturn(attendanceRecords);
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        GenerateBillsResult result = billService.generateBills(request);

        assertThat(result.generated()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        BillDto bill = result.bills().get(0);
        assertThat(bill.sessionsTotal()).isEqualTo(2);
        assertThat(bill.sessionsAttended()).isEqualTo(2);
        assertThat(bill.totalAmount()).isEqualByComparingTo(RATE.multiply(BigDecimal.valueOf(2)));
    }

    // -----------------------------------------------------------------------
    // BR-002: Teacher cancels 1 of 2 sessions — only 1 counts
    // -----------------------------------------------------------------------

    @Test
    void br002_teacherCancelsOneSession_onlyNonCancelledSessionCounts() {
        UUID activeSessionId    = UUID.randomUUID();
        UUID cancelledSessionId = UUID.randomUUID();

        List<Session> sessions = List.of(
            buildSession(activeSessionId, LocalDate.of(2024, 3, 5), false),
            buildSession(cancelledSessionId, LocalDate.of(2024, 3, 12), true) // cancelled
        );

        List<ClassStudent> enrollments = List.of(buildEnrollment(CLASS_ID, STUDENT_ID));

        // Attendance for the non-cancelled session only
        List<Attendance> attendanceRecords = List.of(
            buildAttendance(activeSessionId, STUDENT_ID, AttendanceStatus.PRESENT)
        );

        when(classRepository.findByIdAndTenantId(CLASS_ID, TENANT_ID)).thenReturn(Optional.of(tutoringClass));
        when(sessionRepository.findByClassIdAndSessionDateBetween(CLASS_ID,
            LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31))).thenReturn(sessions);
        when(classStudentRepository.findAllByIdClassId(CLASS_ID)).thenReturn(enrollments);
        when(billRepository.findByTenantIdAndStudentIdAndClassIdAndBillingMonth(
            TENANT_ID, STUDENT_ID, CLASS_ID, LocalDate.of(2024, 3, 1))).thenReturn(Optional.empty());
        when(attendanceRepository.findByStudentIdAndSessionIdIn(STUDENT_ID, List.of(activeSessionId)))
            .thenReturn(attendanceRecords);
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        GenerateBillsResult result = billService.generateBills(request);

        BillDto bill = result.bills().get(0);
        assertThat(bill.sessionsTotal()).isEqualTo(1);         // only non-cancelled sessions count
        assertThat(bill.sessionsAttended()).isEqualTo(1);
        assertThat(bill.totalAmount()).isEqualByComparingTo(RATE); // 1 × rate
    }

    // -----------------------------------------------------------------------
    // BR-003: Student absent for all sessions — bill = 0
    // -----------------------------------------------------------------------

    @Test
    void br003_studentAbsentBothSessions_billIsZero() {
        UUID session1Id = UUID.randomUUID();
        UUID session2Id = UUID.randomUUID();

        List<Session> sessions = List.of(
            buildSession(session1Id, LocalDate.of(2024, 3, 5), false),
            buildSession(session2Id, LocalDate.of(2024, 3, 12), false)
        );

        List<ClassStudent> enrollments = List.of(buildEnrollment(CLASS_ID, STUDENT_ID));

        List<Attendance> attendanceRecords = List.of(
            buildAttendance(session1Id, STUDENT_ID, AttendanceStatus.ABSENT),
            buildAttendance(session2Id, STUDENT_ID, AttendanceStatus.ABSENT)
        );

        when(classRepository.findByIdAndTenantId(CLASS_ID, TENANT_ID)).thenReturn(Optional.of(tutoringClass));
        when(sessionRepository.findByClassIdAndSessionDateBetween(CLASS_ID,
            LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31))).thenReturn(sessions);
        when(classStudentRepository.findAllByIdClassId(CLASS_ID)).thenReturn(enrollments);
        when(billRepository.findByTenantIdAndStudentIdAndClassIdAndBillingMonth(
            TENANT_ID, STUDENT_ID, CLASS_ID, LocalDate.of(2024, 3, 1))).thenReturn(Optional.empty());
        when(attendanceRepository.findByStudentIdAndSessionIdIn(STUDENT_ID, List.of(session1Id, session2Id)))
            .thenReturn(attendanceRecords);
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        GenerateBillsResult result = billService.generateBills(request);

        BillDto bill = result.bills().get(0);
        assertThat(bill.sessionsAttended()).isZero();
        assertThat(bill.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // -----------------------------------------------------------------------
    // BR-004: Mix of PRESENT, LATE, ABSENT — PRESENT+LATE count, ABSENT does not
    // -----------------------------------------------------------------------

    @Test
    void br004_mixedAttendance_presentAndLateCountAbsentDoesNot() {
        UUID session1Id = UUID.randomUUID(); // PRESENT
        UUID session2Id = UUID.randomUUID(); // LATE
        UUID session3Id = UUID.randomUUID(); // ABSENT

        List<Session> sessions = List.of(
            buildSession(session1Id, LocalDate.of(2024, 3, 5), false),
            buildSession(session2Id, LocalDate.of(2024, 3, 12), false),
            buildSession(session3Id, LocalDate.of(2024, 3, 19), false)
        );

        List<ClassStudent> enrollments = List.of(buildEnrollment(CLASS_ID, STUDENT_ID));

        List<Attendance> attendanceRecords = List.of(
            buildAttendance(session1Id, STUDENT_ID, AttendanceStatus.PRESENT),
            buildAttendance(session2Id, STUDENT_ID, AttendanceStatus.LATE),
            buildAttendance(session3Id, STUDENT_ID, AttendanceStatus.ABSENT)
        );

        when(classRepository.findByIdAndTenantId(CLASS_ID, TENANT_ID)).thenReturn(Optional.of(tutoringClass));
        when(sessionRepository.findByClassIdAndSessionDateBetween(CLASS_ID,
            LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31))).thenReturn(sessions);
        when(classStudentRepository.findAllByIdClassId(CLASS_ID)).thenReturn(enrollments);
        when(billRepository.findByTenantIdAndStudentIdAndClassIdAndBillingMonth(
            TENANT_ID, STUDENT_ID, CLASS_ID, LocalDate.of(2024, 3, 1))).thenReturn(Optional.empty());
        when(attendanceRepository.findByStudentIdAndSessionIdIn(STUDENT_ID,
            List.of(session1Id, session2Id, session3Id))).thenReturn(attendanceRecords);
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        GenerateBillsResult result = billService.generateBills(request);

        BillDto bill = result.bills().get(0);
        assertThat(bill.sessionsTotal()).isEqualTo(3);
        // Only PRESENT (1) and LATE (1) count — ABSENT (1) does not
        assertThat(bill.sessionsAttended()).isEqualTo(2);
        assertThat(bill.totalAmount()).isEqualByComparingTo(RATE.multiply(BigDecimal.valueOf(2)));
    }

    // -----------------------------------------------------------------------
    // ISSUED bill guard: re-generating an ISSUED bill must throw
    // -----------------------------------------------------------------------

    @Test
    void issuedBillGuard_attemptToRegenerateIssuedBill_skipsAndDoesNotThrow() {
        UUID session1Id = UUID.randomUUID();

        List<Session> sessions = List.of(
            buildSession(session1Id, LocalDate.of(2024, 3, 5), false)
        );

        List<ClassStudent> enrollments = List.of(buildEnrollment(CLASS_ID, STUDENT_ID));

        Bill issuedBill = Bill.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .studentId(STUDENT_ID)
            .classId(CLASS_ID)
            .billingMonth(LocalDate.of(2024, 3, 1))
            .sessionsTotal(1)
            .sessionsAttended(1)
            .ratePerSession(RATE)
            .totalAmount(RATE)
            .status(BillStatus.ISSUED)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(classRepository.findByIdAndTenantId(CLASS_ID, TENANT_ID)).thenReturn(Optional.of(tutoringClass));
        when(sessionRepository.findByClassIdAndSessionDateBetween(CLASS_ID,
            LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31))).thenReturn(sessions);
        when(classStudentRepository.findAllByIdClassId(CLASS_ID)).thenReturn(enrollments);
        when(billRepository.findByTenantIdAndStudentIdAndClassIdAndBillingMonth(
            TENANT_ID, STUDENT_ID, CLASS_ID, LocalDate.of(2024, 3, 1))).thenReturn(Optional.of(issuedBill));

        GenerateBillsResult result = billService.generateBills(request);

        // The service skips ISSUED bills rather than throwing; verify the bill was skipped
        assertThat(result.generated()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.skipReasons()).hasSize(1);
        assertThat(result.skipReasons().get(0)).contains("ISSUED");
    }

    @Test
    void issuedBillGuard_attemptToRegeneratePaidBill_isAlsoSkipped() {
        UUID session1Id = UUID.randomUUID();

        List<Session> sessions = List.of(
            buildSession(session1Id, LocalDate.of(2024, 3, 5), false)
        );

        List<ClassStudent> enrollments = List.of(buildEnrollment(CLASS_ID, STUDENT_ID));

        Bill paidBill = Bill.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .studentId(STUDENT_ID)
            .classId(CLASS_ID)
            .billingMonth(LocalDate.of(2024, 3, 1))
            .sessionsTotal(1)
            .sessionsAttended(1)
            .ratePerSession(RATE)
            .totalAmount(RATE)
            .status(BillStatus.PAID)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(classRepository.findByIdAndTenantId(CLASS_ID, TENANT_ID)).thenReturn(Optional.of(tutoringClass));
        when(sessionRepository.findByClassIdAndSessionDateBetween(CLASS_ID,
            LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31))).thenReturn(sessions);
        when(classStudentRepository.findAllByIdClassId(CLASS_ID)).thenReturn(enrollments);
        when(billRepository.findByTenantIdAndStudentIdAndClassIdAndBillingMonth(
            TENANT_ID, STUDENT_ID, CLASS_ID, LocalDate.of(2024, 3, 1))).thenReturn(Optional.of(paidBill));

        GenerateBillsResult result = billService.generateBills(request);

        assertThat(result.generated()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.skipReasons().get(0)).contains("PAID");
    }

    @Test
    void updateStatus_changingPaidBill_throwsBillAlreadyIssuedException() {
        UUID billId = UUID.randomUUID();

        Bill paidBill = Bill.builder()
            .id(billId)
            .tenantId(TENANT_ID)
            .studentId(STUDENT_ID)
            .classId(CLASS_ID)
            .billingMonth(LocalDate.of(2024, 3, 1))
            .sessionsTotal(1)
            .sessionsAttended(1)
            .ratePerSession(RATE)
            .totalAmount(RATE)
            .status(BillStatus.PAID)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(billRepository.findByIdAndTenantId(billId, TENANT_ID)).thenReturn(Optional.of(paidBill));

        var updateReq = new UpdateBillStatusRequest(BillStatus.DRAFT);

        assertThatThrownBy(() -> billService.updateStatus(billId, updateReq))
            .isInstanceOf(BillAlreadyIssuedException.class)
            .hasMessageContaining("PAID");
    }

    // -----------------------------------------------------------------------
    // Edge: no sessions in month — zero bill
    // -----------------------------------------------------------------------

    @Test
    void noSessionsInMonth_billIsZeroWithZeroSessionsTotal() {
        List<ClassStudent> enrollments = List.of(buildEnrollment(CLASS_ID, STUDENT_ID));

        when(classRepository.findByIdAndTenantId(CLASS_ID, TENANT_ID)).thenReturn(Optional.of(tutoringClass));
        when(sessionRepository.findByClassIdAndSessionDateBetween(CLASS_ID,
            LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31))).thenReturn(List.of());
        when(classStudentRepository.findAllByIdClassId(CLASS_ID)).thenReturn(enrollments);
        when(billRepository.findByTenantIdAndStudentIdAndClassIdAndBillingMonth(
            TENANT_ID, STUDENT_ID, CLASS_ID, LocalDate.of(2024, 3, 1))).thenReturn(Optional.empty());
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        GenerateBillsResult result = billService.generateBills(request);

        BillDto bill = result.bills().get(0);
        assertThat(bill.sessionsTotal()).isZero();
        assertThat(bill.sessionsAttended()).isZero();
        assertThat(bill.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Session buildSession(UUID id, LocalDate date, boolean cancelled) {
        Session s = new Session();
        s.setId(id);
        s.setTenantId(TENANT_ID);
        s.setClassId(CLASS_ID);
        s.setSessionDate(date);
        s.setCancelledByTeacher(cancelled);
        s.setCreatedAt(Instant.now());
        return s;
    }

    private Attendance buildAttendance(UUID sessionId, UUID studentId, AttendanceStatus status) {
        Attendance a = new Attendance();
        a.setId(UUID.randomUUID());
        a.setTenantId(TENANT_ID);
        a.setSessionId(sessionId);
        a.setStudentId(studentId);
        a.setStatus(status);
        a.setCreatedAt(Instant.now());
        return a;
    }

    private ClassStudent buildEnrollment(UUID classId, UUID studentId) {
        return ClassStudent.builder()
            .id(new ClassStudent.ClassStudentId(classId, studentId))
            .enrolledAt(Instant.now())
            .build();
    }
}
