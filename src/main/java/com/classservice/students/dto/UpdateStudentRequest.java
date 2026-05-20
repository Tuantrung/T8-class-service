package com.classservice.students.dto;

public record UpdateStudentRequest(
    String fullName,
    String phone,
    String parentPhone,
    String notes
) {}
