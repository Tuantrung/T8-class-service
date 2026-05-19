package com.classservice.students.dto;

import java.util.List;

public record ImportResult(
    List<StudentDto> imported,
    List<ImportError> errors
) {}
