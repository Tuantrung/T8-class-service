package com.classservice.comments.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(
    @NotBlank String body
) {}
