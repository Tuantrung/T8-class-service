package com.classservice.students;

import com.classservice.common.PageResponse;
import com.classservice.common.TenantContext;
import com.classservice.common.exception.EntityNotFoundException;
import com.classservice.students.dto.CreateStudentRequest;
import com.classservice.students.dto.StudentDto;
import com.classservice.students.dto.UpdateStudentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public PageResponse<StudentDto> listStudents(Pageable pageable) {
        UUID tenantId = TenantContext.get();
        return PageResponse.from(studentRepository.findAllByTenantId(tenantId, pageable).map(StudentDto::from));
    }

    public StudentDto getStudent(UUID studentId) {
        UUID tenantId = TenantContext.get();
        return studentRepository.findByIdAndTenantId(studentId, tenantId)
            .map(StudentDto::from)
            .orElseThrow(() -> new EntityNotFoundException("Student", studentId));
    }

    @Transactional
    public StudentDto createStudent(CreateStudentRequest req) {
        UUID tenantId = TenantContext.get();
        Student student = Student.builder()
            .tenantId(tenantId)
            .fullName(req.fullName())
            .phone(req.phone())
            .parentPhone(req.parentPhone())
            .notes(req.notes())
            .createdAt(Instant.now())
            .build();
        student.setTenantId(tenantId);
        return StudentDto.from(studentRepository.save(student));
    }

    @Transactional
    public StudentDto updateStudent(UUID studentId, UpdateStudentRequest req) {
        UUID tenantId = TenantContext.get();
        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Student", studentId));
        if (req.fullName() != null) student.setFullName(req.fullName());
        if (req.phone() != null) student.setPhone(req.phone());
        if (req.parentPhone() != null) student.setParentPhone(req.parentPhone());
        if (req.notes() != null) student.setNotes(req.notes());
        return StudentDto.from(studentRepository.save(student));
    }

    @Transactional
    public void deleteStudent(UUID studentId) {
        UUID tenantId = TenantContext.get();
        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Student", studentId));
        studentRepository.delete(student);
    }
}
