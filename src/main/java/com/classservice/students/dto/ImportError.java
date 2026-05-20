package com.classservice.students.dto;

public record ImportError(
    int rowNumber,
    String message
) {}
