package com.classservice.classes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Join entity for the class_student many-to-many enrollment table.
 */
@Entity
@Table(name = "class_student")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassStudent {

    @EmbeddedId
    private ClassStudentId id;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassStudentId implements Serializable {

        @Column(name = "class_id")
        private UUID classId;

        @Column(name = "student_id")
        private UUID studentId;
    }
}
