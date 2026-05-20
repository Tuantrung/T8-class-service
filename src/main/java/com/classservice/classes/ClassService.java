package com.classservice.classes;

import com.classservice.auth.UserRepository;
import com.classservice.classes.dto.ClassDto;
import com.classservice.classes.dto.CreateClassRequest;
import com.classservice.classes.dto.EnrollResult;
import com.classservice.classes.dto.UpdateClassRequest;
import com.classservice.common.PageResponse;
import com.classservice.common.TenantContext;
import com.classservice.common.exception.EntityNotFoundException;
import com.classservice.students.Student;
import com.classservice.students.StudentRepository;
import com.classservice.students.dto.StudentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for class management with tenant isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    /**
     * List all classes for the current tenant with optional pagination.
     *
     * @param pageable pagination parameters
     * @return paginated list of ClassDto
     */
    public PageResponse<ClassDto> listClasses(Pageable pageable) {
        UUID tenantId = TenantContext.get();
        return PageResponse.from(classRepository.findAllByTenantId(tenantId, pageable).map(ClassDto::from));
    }

    /**
     * Get a single class by ID, scoped to the current tenant.
     *
     * @param classId the UUID of the class
     * @return ClassDto if found
     * @throws EntityNotFoundException if not found or in a different tenant
     */
    public ClassDto getClass(UUID classId) {
        UUID tenantId = TenantContext.get();
        return classRepository.findByIdAndTenantId(classId, tenantId)
            .map(ClassDto::from)
            .orElseThrow(() -> new EntityNotFoundException("TutoringClass", classId));
    }

    /**
     * Create a new class for the current tenant.
     *
     * @param req the creation request
     * @return the created ClassDto
     */
    @Transactional
    public ClassDto createClass(CreateClassRequest req) {
        UUID tenantId = TenantContext.get();
        // Verify teacher belongs to the same tenant
        userRepository.findById(req.teacherId())
            .filter(u -> tenantId.equals(u.getTenantId()))
            .orElseThrow(() -> new EntityNotFoundException("AppUser", req.teacherId()));

        TutoringClass tc = TutoringClass.builder()
            .tenantId(tenantId)
            .name(req.name())
            .subject(req.subject())
            .teacherId(req.teacherId())
            .ratePerSession(req.ratePerSession())
            .status(ClassStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();
        return ClassDto.from(classRepository.save(tc));
    }

    /**
     * Update an existing class. Verifies tenant ownership.
     *
     * @param classId the class to update
     * @param req     the update request fields
     * @return updated ClassDto
     */
    @Transactional
    public ClassDto updateClass(UUID classId, UpdateClassRequest req) {
        UUID tenantId = TenantContext.get();
        TutoringClass tc = classRepository.findByIdAndTenantId(classId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("TutoringClass", classId));
        if (req.name() != null) tc.setName(req.name());
        if (req.subject() != null) tc.setSubject(req.subject());
        if (req.teacherId() != null) tc.setTeacherId(req.teacherId());
        if (req.ratePerSession() != null) tc.setRatePerSession(req.ratePerSession());
        if (req.status() != null) tc.setStatus(req.status());
        return ClassDto.from(classRepository.save(tc));
    }

    /**
     * Soft-delete (archive) a class by setting its status to ARCHIVED.
     *
     * @param classId the class to archive
     */
    @Transactional
    public void deactivateClass(UUID classId) {
        UUID tenantId = TenantContext.get();
        TutoringClass tc = classRepository.findByIdAndTenantId(classId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("TutoringClass", classId));
        tc.setStatus(ClassStatus.ARCHIVED);
        classRepository.save(tc);
        log.info("Class {} archived by tenant {}", classId, tenantId);
    }

    /**
     * List all students currently enrolled in a class.
     *
     * @param classId the class whose students to list
     * @return list of StudentDto for each enrolled student
     */
    public List<StudentDto> listStudentsInClass(UUID classId) {
        UUID tenantId = TenantContext.get();
        classRepository.findByIdAndTenantId(classId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("TutoringClass", classId));

        List<ClassStudent> enrollments = classStudentRepository.findAllByIdClassId(classId);
        List<StudentDto> result = new ArrayList<>();
        for (ClassStudent enrollment : enrollments) {
            Optional<Student> student = studentRepository.findByIdAndTenantId(
                enrollment.getId().getStudentId(), tenantId);
            student.map(StudentDto::from).ifPresent(result::add);
        }
        return result;
    }

    /**
     * Enroll one or more students in a class. Ignores already-enrolled students.
     * Returns counts of enrolled, already-enrolled, and not-found students.
     *
     * @param classId    the target class
     * @param studentIds list of student UUIDs to enroll
     * @return EnrollResult with enrolled/alreadyEnrolled/notFound lists
     */
    @Transactional
    public EnrollResult enrollStudents(UUID classId, List<UUID> studentIds) {
        UUID tenantId = TenantContext.get();
        classRepository.findByIdAndTenantId(classId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("TutoringClass", classId));

        List<UUID> enrolled = new ArrayList<>();
        List<UUID> alreadyEnrolled = new ArrayList<>();
        List<UUID> notFound = new ArrayList<>();

        for (UUID studentId : studentIds) {
            // Verify student belongs to this tenant
            Optional<Student> studentOpt = studentRepository.findByIdAndTenantId(studentId, tenantId);
            if (studentOpt.isEmpty()) {
                notFound.add(studentId);
                continue;
            }

            ClassStudent.ClassStudentId csId = new ClassStudent.ClassStudentId(classId, studentId);
            if (classStudentRepository.existsById(csId)) {
                alreadyEnrolled.add(studentId);
            } else {
                ClassStudent cs = ClassStudent.builder()
                    .id(csId)
                    .enrolledAt(Instant.now())
                    .build();
                classStudentRepository.save(cs);
                enrolled.add(studentId);
            }
        }

        log.info("Enrolled {} students in class {}, {} already enrolled, {} not found",
            enrolled.size(), classId, alreadyEnrolled.size(), notFound.size());
        return new EnrollResult(enrolled, alreadyEnrolled, notFound);
    }

    /**
     * Remove a student from a class (unenroll).
     *
     * @param classId   the class
     * @param studentId the student to remove
     * @throws EntityNotFoundException if class or enrollment not found
     */
    @Transactional
    public void removeStudent(UUID classId, UUID studentId) {
        UUID tenantId = TenantContext.get();
        classRepository.findByIdAndTenantId(classId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("TutoringClass", classId));

        ClassStudent.ClassStudentId csId = new ClassStudent.ClassStudentId(classId, studentId);
        if (!classStudentRepository.existsById(csId)) {
            throw new EntityNotFoundException("ClassStudent enrollment", studentId);
        }
        classStudentRepository.deleteById(csId);
        log.info("Student {} removed from class {}", studentId, classId);
    }
}
