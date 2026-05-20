package com.classservice.students.dto;

import java.util.List;

/**
 * Result of a student Excel import operation.
 */
public record ImportResult(
    int imported,
    int skipped,
    List<ImportError> errors
) {}
