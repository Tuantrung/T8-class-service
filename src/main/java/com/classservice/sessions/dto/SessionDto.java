package com.classservice.sessions.dto;

import com.classservice.sessions.Session;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SessionDto(
    UUID id,
    UUID tenantId,
    UUID classId,
    LocalDate sessionDate,
    LocalTime startTime,
    LocalTime endTime,
    String topic,
    boolean cancelledByTeacher,
    Instant createdAt
) {
    public static SessionDto from(Session s) {
        return new SessionDto(s.getId(), s.getTenantId(), s.getClassId(), s.getSessionDate(),
            s.getStartTime(), s.getEndTime(), s.getTopic(), s.isCancelledByTeacher(), s.getCreatedAt());
    }
}
