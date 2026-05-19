package com.classservice.comments;

import com.classservice.auth.AppUser;
import com.classservice.comments.dto.CommentDto;
import com.classservice.comments.dto.CreateCommentRequest;
import com.classservice.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentDto>>> get(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.getBySession(sessionId)));
    }

    @PutMapping("/{studentId}")
    public ResponseEntity<ApiResponse<CommentDto>> upsert(
            @PathVariable UUID sessionId,
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateCommentRequest req,
            @AuthenticationPrincipal AppUser author) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.upsertComment(sessionId, studentId, req, author)));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID sessionId, @PathVariable UUID commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
