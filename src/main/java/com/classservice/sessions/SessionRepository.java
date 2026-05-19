package com.classservice.sessions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findByClassIdOrderBySessionDateDesc(UUID classId);

    List<Session> findByClassIdAndSessionDateBetween(UUID classId, LocalDate from, LocalDate to);

    Optional<Session> findByIdAndTenantId(UUID id, UUID tenantId);
}
