package com.classservice.classes.dto;

import com.classservice.classes.ClassStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateClassRequest(
    String name,
    String subject,
    UUID teacherId,
    BigDecimal ratePerSession,
    ClassStatus status
) {}
