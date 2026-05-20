package com.classservice.grades;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradeRepository extends JpaRepository<Grade, UUID> {

    Optional<Grade> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Grade> findByClassIdAndTenantId(UUID classId, UUID tenantId);

    List<Grade> findByClassIdAndStudentIdAndTenantId(UUID classId, UUID studentId, UUID tenantId);

    Page<Grade> findByClassIdAndTenantId(UUID classId, UUID tenantId, Pageable pageable);

    Page<Grade> findByClassIdAndStudentIdAndTenantId(UUID classId, UUID studentId, UUID tenantId, Pageable pageable);

    Optional<Grade> findByClassIdAndStudentIdAndExamName(UUID classId, UUID studentId, String examName);
}
