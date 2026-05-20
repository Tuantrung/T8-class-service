package com.classservice.billing.dto;

import com.classservice.billing.BillStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateBillStatusRequest(
    @NotNull BillStatus status
) {}
