package com.classservice.comments;

import com.classservice.auth.AppUser;
import com.classservice.comments.dto.CommentDto;
import com.classservice.comments.dto.CreateCommentRequest;
import com.classservice.comments.dto.UpdateCommentRequest;
import com.classservice.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for comment management.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CommentController {

    private final CommentService commentService;

    /**
     * Get comments for a session, optionally filtered by student.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentDto>>> get(
            @RequestParam UUID sessionId,
            @RequestParam(required = false) UUID studentId) {
        List<CommentDto> comments = studentId != null
            ? commentService.getBySessionAndStudent(sessionId, studentId)
            : commentService.getBySession(sessionId);
        return ResponseEntity.ok(ApiResponse.ok(comments));
    }

    /**
     * Create a comment for a student on a session.
     * Returns 409 if comment already exists — use PUT to update.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CommentDto>> create(
            @Valid @RequestBody CreateCommentRequest req,
            @AuthenticationPrincipal AppUser author) {
        CommentDto comment = commentService.createComment(req, author);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(comment, "Comment created"));
    }

    /**
     * Update a comment body. Only the author or ADMIN can update.
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentDto>> update(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest req,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.updateComment(commentId, req, currentUser)));
    }

    /**
     * Delete a comment. Only the author or ADMIN can delete.
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal AppUser currentUser) {
        commentService.deleteComment(commentId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
