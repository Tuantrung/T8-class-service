package com.classservice.grades.dto;

import java.util.List;

public record GradeImportResult(
    int imported,
    int updated,
    int skipped,
    List<GradeImportError> errors
) {}
