package com.classservice.comments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findBySessionId(UUID sessionId);

    Optional<Comment> findBySessionIdAndStudentId(UUID sessionId, UUID studentId);
}
