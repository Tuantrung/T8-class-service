package com.classservice.grades;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GradeRepository extends JpaRepository<Grade, UUID> {

    List<Grade> findByClassIdAndStudentId(UUID classId, UUID studentId);

    List<Grade> findByClassId(UUID classId);
}
