package com.classservice.classes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassStudentRepository extends JpaRepository<ClassStudent, ClassStudent.ClassStudentId> {

    List<ClassStudent> findAllByIdClassId(UUID classId);

    List<ClassStudent> findAllByIdStudentId(UUID studentId);
}
