package com.classservice.grades.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateGradeRequest(
    String examName,
    LocalDate examDate,
    BigDecimal score,
    BigDecimal maxScore,
    String notes
) {}
