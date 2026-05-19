package com.classservice.billing.dto;

import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;
import java.util.UUID;

public record GenerateBillsRequest(
    @NotNull UUID classId,
    @NotNull YearMonth month
) {}
