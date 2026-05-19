package com.classservice.comments;

import com.classservice.auth.AppUser;
import com.classservice.comments.dto.CommentDto;
import com.classservice.comments.dto.CreateCommentRequest;
import com.classservice.comments.dto.UpdateCommentRequest;
import com.classservice.common.TenantContext;
import com.classservice.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    public List<CommentDto> getBySession(UUID sessionId) {
        return commentRepository.findBySessionId(sessionId).stream().map(CommentDto::from).toList();
    }

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
            comment.setTenantId(tenantId);
        }
        return CommentDto.from(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new EntityNotFoundException("Comment", commentId));
        commentRepository.delete(comment);
    }
}
