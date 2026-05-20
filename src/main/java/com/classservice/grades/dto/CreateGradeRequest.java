package com.classservice.grades.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateGradeRequest(
    @NotNull UUID classId,
    @NotNull UUID studentId,
    @NotBlank String examName,
    LocalDate examDate,
    BigDecimal score,
    BigDecimal maxScore,
    String notes
) {}
