package com.classservice.grades.dto;

import com.classservice.grades.Grade;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GradeDto(
    UUID id,
    UUID classId,
    UUID studentId,
    String examName,
    LocalDate examDate,
    BigDecimal score,
    BigDecimal maxScore,
    String notes,
    Instant createdAt
) {
    public static GradeDto from(Grade g) {
        return new GradeDto(g.getId(), g.getClassId(), g.getStudentId(), g.getExamName(),
            g.getExamDate(), g.getScore(), g.getMaxScore(), g.getNotes(), g.getCreatedAt());
    }
}
