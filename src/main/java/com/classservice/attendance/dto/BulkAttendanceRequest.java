package com.classservice.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request to bulk-save attendance for an entire session.
 */
public record BulkAttendanceRequest(
    @NotNull UUID sessionId,
    @NotEmpty @Valid List<AttendanceRecord> records
) {}
