package com.classservice.sessions;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findByClassIdOrderBySessionDateDesc(UUID classId);

    Page<Session> findByClassIdOrderBySessionDateDesc(UUID classId, Pageable pageable);

    List<Session> findByClassIdAndSessionDateBetween(UUID classId, LocalDate from, LocalDate to);

    Page<Session> findByClassIdAndSessionDateBetween(UUID classId, LocalDate from, LocalDate to, Pageable pageable);

    Optional<Session> findByIdAndTenantId(UUID id, UUID tenantId);
}
