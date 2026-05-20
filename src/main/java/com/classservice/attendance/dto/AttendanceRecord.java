package com.classservice.attendance.dto;

import com.classservice.attendance.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttendanceRecord(
    @NotNull UUID studentId,
    @NotNull AttendanceStatus status
) {}
