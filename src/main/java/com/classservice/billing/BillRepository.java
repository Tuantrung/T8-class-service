package com.classservice.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Filtered list for GET /api/bills — all params except tenantId are optional (null = no filter).
     */
    @Query("""
        SELECT b FROM Bill b
        WHERE b.tenantId = :tenantId
          AND (:billingMonth IS NULL OR b.billingMonth = :billingMonth)
          AND (:classId     IS NULL OR b.classId     = :classId)
          AND (:studentId   IS NULL OR b.studentId   = :studentId)
          AND (:status      IS NULL OR b.status      = :status)
        ORDER BY b.createdAt DESC
        """)
    Page<Bill> findFiltered(
        @Param("tenantId")     UUID tenantId,
        @Param("billingMonth") LocalDate billingMonth,
        @Param("classId")      UUID classId,
        @Param("studentId")    UUID studentId,
        @Param("status")       BillStatus status,
        Pageable pageable);

    /**
     * All ISSUED or PAID bills for a month, optionally scoped to a class — used by ZIP export.
     */
    @Query("""
        SELECT b FROM Bill b
        WHERE b.tenantId    = :tenantId
          AND b.billingMonth = :billingMonth
          AND b.status      IN :statuses
          AND (:classId     IS NULL OR b.classId = :classId)
        ORDER BY b.classId, b.studentId
        """)
    List<Bill> findForExport(
        @Param("tenantId")     UUID tenantId,
        @Param("billingMonth") LocalDate billingMonth,
        @Param("statuses")     List<BillStatus> statuses,
        @Param("classId")      UUID classId);
}
