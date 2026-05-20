package com.classservice.students.dto;

import com.classservice.students.Student;

import java.time.Instant;
import java.util.UUID;

public record StudentDto(
    UUID id,
    UUID tenantId,
    String fullName,
    String phone,
    String parentPhone,
    String notes,
    String schoolName,
    Instant createdAt
) {
    public static StudentDto from(Student s) {
        return new StudentDto(s.getId(), s.getTenantId(), s.getFullName(),
            s.getPhone(), s.getParentPhone(), s.getNotes(), s.getSchoolName(), s.getCreatedAt());
    }
}
