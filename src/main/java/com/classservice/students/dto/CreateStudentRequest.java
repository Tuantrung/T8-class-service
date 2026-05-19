package com.classservice.students.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateStudentRequest(
    @NotBlank String fullName,
    String phone,
    String parentPhone,
    String notes
) {}
