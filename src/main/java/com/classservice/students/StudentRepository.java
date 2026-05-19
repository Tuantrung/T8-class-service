package com.classservice.students;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    Page<Student> findAllByTenantId(UUID tenantId, Pageable pageable);

    Optional<Student> findByIdAndTenantId(UUID id, UUID tenantId);
}
