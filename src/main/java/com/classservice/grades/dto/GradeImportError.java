package com.classservice.grades.dto;

public record GradeImportError(
    int rowNumber,
    String message
) {}
