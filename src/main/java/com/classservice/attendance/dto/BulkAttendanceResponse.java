package com.classservice.attendance.dto;

import java.util.UUID;

/**
 * Response for bulk attendance save operation.
 */
public record BulkAttendanceResponse(
    int saved,
    UUID sessionId
) {}
