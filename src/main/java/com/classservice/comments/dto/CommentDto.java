package com.classservice.comments.dto;

import com.classservice.comments.Comment;

import java.time.Instant;
import java.util.UUID;

public record CommentDto(
    UUID id,
    UUID sessionId,
    UUID studentId,
    UUID authorId,
    String body,
    Instant createdAt,
    Instant updatedAt
) {
    public static CommentDto from(Comment c) {
        return new CommentDto(c.getId(), c.getSessionId(), c.getStudentId(),
            c.getAuthorId(), c.getBody(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
