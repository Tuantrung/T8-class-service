package com.classservice.billing;

import com.classservice.attendance.AttendanceRepository;
import com.classservice.attendance.AttendanceStatus;
import com.classservice.billing.dto.BillDetailDto;
import com.classservice.billing.dto.BillDto;
import com.classservice.billing.dto.GenerateBillsRequest;
import com.classservice.billing.dto.GenerateBillsResult;
import com.classservice.billing.dto.UpdateBillStatusRequest;
import com.classservice.classes.ClassRepository;
import com.classservice.classes.ClassStudentRepository;
import com.classservice.students.StudentRepository;
import com.classservice.classes.TutoringClass;
import com.classservice.common.PageResponse;
import com.classservice.common.TenantContext;
import com.classservice.common.exception.BillAlreadyIssuedException;
import com.classservice.common.exception.EntityNotFoundException;
import com.classservice.sessions.Session;
import com.classservice.sessions.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final SessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;

    public PageResponse<BillDto> listBills(Pageable pageable) {
        UUID tenantId = TenantContext.get();
        return PageResponse.from(billRepository.findByTenantId(tenantId, pageable).map(BillDto::from));
    }

    public BillDto getBill(UUID billId) {
        UUID tenantId = TenantContext.get();
        return billRepository.findByIdAndTenantId(billId, tenantId)
            .map(BillDto::from)
            .orElseThrow(() -> new EntityNotFoundException("Bill", billId));
    }

    public BillDetailDto getBillDetail(UUID billId) {
        UUID tenantId = TenantContext.get();
        Bill bill = billRepository.findByIdAndTenantId(billId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Bill", billId));

        String studentName = studentRepository.findByIdAndTenantId(bill.getStudentId(), tenantId)
            .map(s -> s.getFullName())
            .orElse(bill.getStudentId().toString());

        String className = classRepository.findByIdAndTenantId(bill.getClassId(), tenantId)
            .map(c -> c.getName())
            .orElse(bill.getClassId().toString());

        LocalDate monthStart = bill.getBillingMonth();
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        List<Session> sessions = sessionRepository.findByClassIdAndSessionDateBetween(
            bill.getClassId(), monthStart, monthEnd);

        List<UUID> sessionIds = sessions.stream().map(Session::getId).toList();
        Map<UUID, AttendanceStatus> attendanceMap = attendanceRepository
            .findByStudentIdAndSessionIdIn(bill.getStudentId(), sessionIds)
            .stream()
            .collect(Collectors.toMap(a -> a.getSessionId(), a -> a.getStatus()));

        List<BillDetailDto.SessionLineItem> lineItems = sessions.stream()
            .map(s -> new BillDetailDto.SessionLineItem(
                s.getId(),
                s.getSessionDate(),
                s.getTopic(),
                attendanceMap.getOrDefault(s.getId(), AttendanceStatus.ABSENT) == AttendanceStatus.PRESENT
                    || attendanceMap.getOrDefault(s.getId(), AttendanceStatus.ABSENT) == AttendanceStatus.LATE,
                s.isCancelledByTeacher()
            ))
            .toList();

        String billingMonthStr = bill.getBillingMonth().getYear() + "-"
            + String.format("%02d", bill.getBillingMonth().getMonthValue());

        return new BillDetailDto(
            bill.getId(),
            bill.getStudentId().toString(),
            studentName,
            bill.getClassId().toString(),
            className,
            billingMonthStr,
            bill.getSessionsTotal(),
            bill.getSessionsAttended(),
            bill.getRatePerSession(),
            bill.getTotalAmount(),
            bill.getStatus(),
            lineItems,
            bill.getCreatedAt()
        );
    }

    public List<BillDto> listBillsByMonth(String month, UUID classId) {
        UUID tenantId = TenantContext.get();
        LocalDate billingMonth = LocalDate.parse(month + "-01");
        List<Bill> bills = classId != null
            ? billRepository.findByTenantIdAndClassIdAndBillingMonth(tenantId, classId, billingMonth)
            : billRepository.findByTenantIdAndBillingMonth(tenantId, billingMonth);
        return bills.stream().map(BillDto::from).toList();
    }

    /**
     * Core billing calculation engine.
     * For each enrolled student in the class:
     *   - sessionsTotal = non-cancelled sessions in month
     *   - sessionsAttended = PRESENT or LATE attendance in those sessions
     *   - totalAmount = sessionsAttended * class.ratePerSession
     * Only overwrites DRAFT bills — raises BillAlreadyIssuedException for ISSUED/PAID.
     */
    @Transactional
    public GenerateBillsResult generateBills(GenerateBillsRequest req) {
        UUID tenantId = TenantContext.get();
        LocalDate billingMonth = req.month().atDay(1);

        TutoringClass tc = classRepository.findByIdAndTenantId(req.classId(), tenantId)
            .orElseThrow(() -> new EntityNotFoundException("TutoringClass", req.classId()));

        LocalDate monthStart = billingMonth;
        LocalDate monthEnd = billingMonth.withDayOfMonth(billingMonth.lengthOfMonth());

        var sessions = sessionRepository.findByClassIdAndSessionDateBetween(req.classId(), monthStart, monthEnd);
        var nonCancelledSessions = sessions.stream().filter(s -> !s.isCancelledByTeacher()).toList();
        var nonCancelledSessionIds = nonCancelledSessions.stream().map(s -> s.getId()).toList();

        var enrollments = classStudentRepository.findAllByIdClassId(req.classId());

        List<BillDto> generated = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (var enrollment : enrollments) {
            UUID studentId = enrollment.getId().getStudentId();

            // Check if bill already exists and is not DRAFT
            var existing = billRepository.findByTenantIdAndStudentIdAndClassIdAndBillingMonth(
                tenantId, studentId, req.classId(), billingMonth);

            if (existing.isPresent() && existing.get().getStatus() != BillStatus.DRAFT) {
                skipped.add("Student " + studentId + ": bill already " + existing.get().getStatus());
                continue;
            }

            final long attended = nonCancelledSessionIds.isEmpty() ? 0L :
                attendanceRepository
                    .findByStudentIdAndSessionIdIn(studentId, nonCancelledSessionIds)
                    .stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                    .count();

            final BigDecimal totalAmount = tc.getRatePerSession().multiply(BigDecimal.valueOf(attended));

            Bill bill = existing.map(b -> {
                b.setSessionsTotal(nonCancelledSessions.size());
                b.setSessionsAttended((int) attended);
                b.setRatePerSession(tc.getRatePerSession());
                b.setTotalAmount(totalAmount);
                b.setUpdatedAt(Instant.now());
                return b;
            }).orElseGet(() -> {
                Bill newBill = Bill.builder()
                    .tenantId(tenantId)
                    .studentId(studentId)
                    .classId(req.classId())
                    .billingMonth(billingMonth)
                    .sessionsTotal(nonCancelledSessions.size())
                    .sessionsAttended((int) attended)
                    .ratePerSession(tc.getRatePerSession())
                    .totalAmount(totalAmount)
                    .status(BillStatus.DRAFT)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
                newBill.setTenantId(tenantId);
                return newBill;
            });

            generated.add(BillDto.from(billRepository.save(bill)));
        }

        return new GenerateBillsResult(generated.size(), skipped.size(), generated, skipped);
    }

    @Transactional
    public BillDto updateStatus(UUID billId, UpdateBillStatusRequest req) {
        UUID tenantId = TenantContext.get();
        Bill bill = billRepository.findByIdAndTenantId(billId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Bill", billId));

        if (bill.getStatus() == BillStatus.PAID) {
            throw new BillAlreadyIssuedException("Cannot change status of a PAID bill");
        }

        bill.setStatus(req.status());
        bill.setUpdatedAt(Instant.now());
        return BillDto.from(billRepository.save(bill));
    }
}
