package com.classservice.billing.dto;

import com.classservice.billing.BillStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detailed bill view including per-session line items.
 * Returned by GET /api/bills/{billId} and used for PDF generation.
 */
public record BillDetailDto(
    UUID id,
    String studentId,
    String studentName,
    String classId,
    String className,
    /** Billing month in "YYYY-MM" format, e.g. "2024-03" */
    String billingMonth,
    int sessionsTotal,
    int sessionsAttended,
    BigDecimal ratePerSession,
    BigDecimal totalAmount,
    BillStatus status,
    List<SessionLineItem> sessions,
    Instant createdAt
) {
    /**
     * One line item in the session breakdown table on the bill.
     */
    public record SessionLineItem(
        UUID sessionId,
        LocalDate sessionDate,
        String topic,
        boolean attended,
        boolean cancelledByTeacher
    ) {}
}
