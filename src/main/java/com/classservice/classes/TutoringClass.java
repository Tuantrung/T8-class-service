package com.classservice.classes;

import com.classservice.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a tutoring class (named TutoringClass to avoid SQL reserved word 'class').
 * Maps to the 'class' table.
 */
@Entity
@Table(name = "class")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TutoringClass extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String subject;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "rate_per_session", nullable = false)
    private BigDecimal ratePerSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    
    private ClassStatus status = ClassStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
