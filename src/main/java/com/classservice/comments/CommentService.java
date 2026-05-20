package com.classservice.comments;

import com.classservice.auth.AppUser;
import com.classservice.auth.UserRole;
import com.classservice.comments.dto.CommentDto;
import com.classservice.comments.dto.CreateCommentRequest;
import com.classservice.comments.dto.UpdateCommentRequest;
import com.classservice.common.TenantContext;
import com.classservice.common.exception.DuplicateResourceException;
import com.classservice.common.exception.EntityNotFoundException;
import com.classservice.common.exception.TenantMismatchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for comment management with tenant isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    /**
     * Get all comments for a session.
     *
     * @param sessionId the session UUID
     * @return list of CommentDto
     */
    public List<CommentDto> getBySession(UUID sessionId) {
        return commentRepository.findBySessionId(sessionId).stream()
            .map(CommentDto::from).toList();
    }

    /**
     * Get comments for a session filtered by student.
     *
     * @param sessionId the session UUID
     * @param studentId the student UUID to filter by
     * @return list of CommentDto
     */
    public List<CommentDto> getBySessionAndStudent(UUID sessionId, UUID studentId) {
        return commentRepository.findBySessionIdAndStudentId(sessionId, studentId)
            .map(CommentDto::from)
            .map(List::of)
            .orElse(List.of());
    }

    /**
     * Create a new comment for a student on a session.
     * Throws DuplicateResourceException if a comment already exists for this student/session.
     *
     * @param req    the create request with sessionId, studentId, and body
     * @param author the authenticated user creating the comment
     * @return the created CommentDto
     * @throws DuplicateResourceException if a comment already exists (use PUT to update)
     */
    @Transactional
    public CommentDto createComment(CreateCommentRequest req, AppUser author) {
        UUID tenantId = TenantContext.get();

        if (commentRepository.findBySessionIdAndStudentId(req.sessionId(), req.studentId()).isPresent()) {
            throw new DuplicateResourceException(
                "Comment already exists for student " + req.studentId() + " in session " + req.sessionId()
                    + ". Use PUT /api/comments/{commentId} to update.");
        }

        Comment comment = Comment.builder()
            .tenantId(tenantId)
            .sessionId(req.sessionId())
            .studentId(req.studentId())
            .authorId(author.getId())
            .body(req.body())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        return CommentDto.from(commentRepository.save(comment));
    }

    /**
     * Update a comment body. Only the author or ADMIN can update.
     *
     * @param commentId   the comment to update
     * @param req         the update request with new body
     * @param currentUser the authenticated user requesting the update
     * @return updated CommentDto
     * @throws EntityNotFoundException if comment not found
     * @throws TenantMismatchException if user is not the author and not ADMIN
     */
    @Transactional
    public CommentDto updateComment(UUID commentId, UpdateCommentRequest req, AppUser currentUser) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new EntityNotFoundException("Comment", commentId));

        if (!comment.getAuthorId().equals(currentUser.getId())
                && currentUser.getRole() != UserRole.ADMIN) {
            throw new TenantMismatchException("You are not authorized to edit this comment");
        }

        comment.setBody(req.body());
        comment.setUpdatedAt(Instant.now());
        return CommentDto.from(commentRepository.save(comment));
    }

    /**
     * Delete a comment. Only the author or ADMIN can delete.
     *
     * @param commentId   the comment to delete
     * @param currentUser the authenticated user requesting the deletion
     * @throws EntityNotFoundException if comment not found
     * @throws TenantMismatchException if user is not the author and not ADMIN
     */
    @Transactional
    public void deleteComment(UUID commentId, AppUser currentUser) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new EntityNotFoundException("Comment", commentId));

        if (!comment.getAuthorId().equals(currentUser.getId())
                && currentUser.getRole() != UserRole.ADMIN) {
            throw new TenantMismatchException("You are not authorized to delete this comment");
        }
        commentRepository.delete(comment);
        log.info("Comment {} deleted by user {}", commentId, currentUser.getId());
    }

    /**
     * Upsert a comment (legacy method kept for backward compatibility).
     *
     * @param sessionId the session UUID
     * @param studentId the student UUID
     * @param req       the comment body
     * @param author    the authenticated user
     * @return the upserted CommentDto
     */
    @Transactional
    public CommentDto upsertComment(UUID sessionId, UUID studentId, CreateCommentRequest req, AppUser author) {
        UUID tenantId = TenantContext.get();
        Comment comment = commentRepository.findBySessionIdAndStudentId(sessionId, studentId).orElse(null);
        if (comment != null) {
            comment.setBody(req.body());
            comment.setUpdatedAt(Instant.now());
        } else {
            comment = Comment.builder()
                .tenantId(tenantId)
                .sessionId(sessionId)
                .studentId(studentId)
                .authorId(author.getId())
                .body(req.body())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        }
        return CommentDto.from(commentRepository.save(comment));
    }
}
