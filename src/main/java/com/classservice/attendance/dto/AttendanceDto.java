package com.classservice.attendance.dto;

import com.classservice.attendance.Attendance;
import com.classservice.attendance.AttendanceStatus;

import java.util.UUID;

public record AttendanceDto(
    UUID id,
    UUID sessionId,
    UUID studentId,
    AttendanceStatus status
) {
    public static AttendanceDto from(Attendance a) {
        return new AttendanceDto(a.getId(), a.getSessionId(), a.getStudentId(), a.getStatus());
    }
}
