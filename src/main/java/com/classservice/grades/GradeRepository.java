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

    List<Grade> findByClassIdAndStudentId(UUID classId, UUID studentId);

    Page<Grade> findByClassIdAndStudentId(UUID classId, UUID studentId, Pageable pageable);

    List<Grade> findByClassId(UUID classId);

    Page<Grade> findByClassId(UUID classId, Pageable pageable);

    Optional<Grade> findByClassIdAndStudentIdAndExamName(UUID classId, UUID studentId, String examName);
}
