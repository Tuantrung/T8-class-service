package com.classservice.students;

import com.classservice.common.PageResponse;
import com.classservice.common.TenantContext;
import com.classservice.common.exception.EntityNotFoundException;
import com.classservice.students.dto.CreateStudentRequest;
import com.classservice.students.dto.ImportResult;
import com.classservice.students.dto.StudentDto;
import com.classservice.students.dto.UpdateStudentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

/**
 * Business logic for student management with tenant isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final ExcelImportService excelImportService;

    /**
     * List all students for the current tenant with pagination.
     *
     * @param pageable pagination parameters
     * @return paginated StudentDto list
     */
    public PageResponse<StudentDto> listStudents(Pageable pageable) {
        UUID tenantId = TenantContext.get();
        return PageResponse.from(studentRepository.findAllByTenantId(tenantId, pageable).map(StudentDto::from));
    }

    /**
     * Get a single student by ID, scoped to the current tenant.
     *
     * @param studentId the UUID of the student
     * @return StudentDto if found
     * @throws EntityNotFoundException if not found in this tenant
     */
    public StudentDto getStudent(UUID studentId) {
        UUID tenantId = TenantContext.get();
        return studentRepository.findByIdAndTenantId(studentId, tenantId)
            .map(StudentDto::from)
            .orElseThrow(() -> new EntityNotFoundException("Student", studentId));
    }

    /**
     * Create a new student for the current tenant.
     *
     * @param req creation request with required fields
     * @return the created StudentDto
     */
    @Transactional
    public StudentDto createStudent(CreateStudentRequest req) {
        UUID tenantId = TenantContext.get();
        Student student = Student.builder()
            .tenantId(tenantId)
            .fullName(req.fullName())
            .phone(req.phone())
            .parentPhone(req.parentPhone())
            .notes(req.notes())
            .schoolName(req.schoolName())
            .createdAt(Instant.now())
            .build();
        return StudentDto.from(studentRepository.save(student));
    }

    /**
     * Update an existing student. Only modifies fields that are non-null in the request.
     *
     * @param studentId the student to update
     * @param req       update request
     * @return updated StudentDto
     */
    @Transactional
    public StudentDto updateStudent(UUID studentId, UpdateStudentRequest req) {
        UUID tenantId = TenantContext.get();
        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Student", studentId));
        if (req.fullName() != null) student.setFullName(req.fullName());
        if (req.phone() != null) student.setPhone(req.phone());
        if (req.parentPhone() != null) student.setParentPhone(req.parentPhone());
        if (req.notes() != null) student.setNotes(req.notes());
        if (req.schoolName() != null) student.setSchoolName(req.schoolName());
        return StudentDto.from(studentRepository.save(student));
    }

    /**
     * Delete a student. Only allowed if the student exists in the current tenant.
     *
     * @param studentId the student to delete
     */
    @Transactional
    public void deleteStudent(UUID studentId) {
        UUID tenantId = TenantContext.get();
        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Student", studentId));
        studentRepository.delete(student);
        log.info("Student {} deleted from tenant {}", studentId, tenantId);
    }

    /**
     * Import students from an Excel file. Delegates to ExcelImportService.
     * Partial success: valid rows are committed even if some rows fail (BR-011).
     *
     * @param file the .xlsx file to parse
     * @return ImportResult with counts and error details
     */
    @Transactional
    public ImportResult importStudents(MultipartFile file) {
        return excelImportService.importStudents(file);
    }

    @Transactional
    public ImportResult importStudentsToClass(UUID classId, MultipartFile file) {
        return excelImportService.importStudents(file, classId);
    }
}
