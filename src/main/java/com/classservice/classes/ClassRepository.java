package com.classservice.classes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassRepository extends JpaRepository<TutoringClass, UUID> {

    Page<TutoringClass> findAllByTenantId(UUID tenantId, Pageable pageable);

    Optional<TutoringClass> findByIdAndTenantId(UUID id, UUID tenantId);
}
