package com.classservice.billing.dto;

import com.classservice.billing.Bill;
import com.classservice.billing.BillStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BillDto(
    UUID id,
    UUID tenantId,
    UUID studentId,
    UUID classId,
    LocalDate billingMonth,
    int sessionsTotal,
    int sessionsAttended,
    BigDecimal ratePerSession,
    BigDecimal totalAmount,
    BillStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static BillDto from(Bill b) {
        return new BillDto(b.getId(), b.getTenantId(), b.getStudentId(), b.getClassId(),
            b.getBillingMonth(), b.getSessionsTotal(), b.getSessionsAttended(),
            b.getRatePerSession(), b.getTotalAmount(), b.getStatus(),
            b.getCreatedAt(), b.getUpdatedAt());
    }
}
