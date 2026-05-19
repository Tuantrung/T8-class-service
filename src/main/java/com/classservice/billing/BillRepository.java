package com.classservice.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<Bill, UUID> {

    Page<Bill> findByTenantId(UUID tenantId, Pageable pageable);

    List<Bill> findByTenantIdAndBillingMonth(UUID tenantId, LocalDate billingMonth);

    List<Bill> findByTenantIdAndClassIdAndBillingMonth(UUID tenantId, UUID classId, LocalDate billingMonth);

    Optional<Bill> findByTenantIdAndStudentIdAndClassIdAndBillingMonth(
        UUID tenantId, UUID studentId, UUID classId, LocalDate billingMonth);

    Optional<Bill> findByIdAndTenantId(UUID id, UUID tenantId);
}
