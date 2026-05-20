package com.classservice.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    List<Attendance> findBySessionId(UUID sessionId);

    Optional<Attendance> findBySessionIdAndStudentId(UUID sessionId, UUID studentId);

    Optional<Attendance> findByIdAndTenantId(UUID id, UUID tenantId);

    void deleteBySessionIdAndStudentId(UUID sessionId, UUID studentId);

    List<Attendance> findByStudentIdAndSessionIdIn(UUID studentId, List<UUID> sessionIds);

    boolean existsBySessionId(UUID sessionId);
}
