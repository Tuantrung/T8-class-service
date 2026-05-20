package com.classservice.attendance.dto;

import com.classservice.attendance.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request to update a single attendance record status.
 */
public record UpdateAttendanceRequest(
    @NotNull AttendanceStatus status
) {}
