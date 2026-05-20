package com.classservice.classes.dto;

import com.classservice.classes.ClassStatus;
import com.classservice.classes.TutoringClass;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ClassDto(
    UUID id,
    UUID tenantId,
    String name,
    String subject,
    UUID teacherId,
    BigDecimal ratePerSession,
    ClassStatus status,
    Instant createdAt
) {
    public static ClassDto from(TutoringClass tc) {
        return new ClassDto(tc.getId(), tc.getTenantId(), tc.getName(), tc.getSubject(),
            tc.getTeacherId(), tc.getRatePerSession(), tc.getStatus(), tc.getCreatedAt());
    }
}
