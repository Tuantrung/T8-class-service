package com.classservice.classes.dto;

import java.util.List;
import java.util.UUID;

/**
 * Result of enrolling students in a class.
 */
public record EnrollResult(
    List<UUID> enrolled,
    List<UUID> alreadyEnrolled,
    List<UUID> notFound
) {}
