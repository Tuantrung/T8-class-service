package com.classservice.comments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to create a comment for a student on a session.
 */
public record CreateCommentRequest(
    @NotNull UUID sessionId,
    @NotNull UUID studentId,
    @NotBlank String body
) {}
