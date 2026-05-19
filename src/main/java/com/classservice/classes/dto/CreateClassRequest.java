package com.classservice.classes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateClassRequest(
    @NotBlank String name,
    String subject,
    @NotNull UUID teacherId,
    @NotNull @DecimalMin("0") BigDecimal ratePerSession
) {}
