package com.classservice.grades;

import com.classservice.common.PageResponse;
import com.classservice.common.TenantContext;
import com.classservice.common.exception.DuplicateResourceException;
import com.classservice.common.exception.EntityNotFoundException;
import com.classservice.grades.dto.CreateGradeRequest;
import com.classservice.grades.dto.GradeDto;
import com.classservice.grades.dto.UpdateGradeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for grade management with tenant isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;

    /**
     * List grades for a class, optionally filtered by student, with pagination.
     *
     * @param classId   the class to filter by
     * @param studentId optional student filter
     * @param pageable  pagination parameters
     * @return paginated GradeDto list
     */
    public PageResponse<GradeDto> listGrades(UUID classId, UUID studentId, Pageable pageable) {
        if (studentId != null) {
            return PageResponse.from(
                gradeRepository.findByClassIdAndStudentId(classId, studentId, pageable).map(GradeDto::from));
        }
        return PageResponse.from(gradeRepository.findByClassId(classId, pageable).map(GradeDto::from));
    }

    /**
     * List all grades for a class (no pagination).
     *
     * @param classId the class UUID
     * @return list of GradeDto
     */
    public List<GradeDto> listByClass(UUID classId) {
        return gradeRepository.findByClassId(classId).stream().map(GradeDto::from).toList();
    }

    /**
     * List grades for a specific student in a class (no pagination).
     *
     * @param classId   the class UUID
     * @param studentId the student UUID
     * @return list of GradeDto
     */
    public List<GradeDto> listByClassAndStudent(UUID classId, UUID studentId) {
        return gradeRepository.findByClassIdAndStudentId(classId, studentId)
            .stream().map(GradeDto::from).toList();
    }

    /**
     * Create a grade entry. Checks for duplicate (class + student + examName).
     *
     * @param req the create request
     * @return the created GradeDto
     * @throws DuplicateResourceException if a grade already exists for this student/class/exam
     */
    @Transactional
    public GradeDto createGrade(CreateGradeRequest req) {
        UUID tenantId = TenantContext.get();

        // Check for duplicate exam entry
        if (gradeRepository.findByClassIdAndStudentIdAndExamName(
                req.classId(), req.studentId(), req.examName()).isPresent()) {
            throw new DuplicateResourceException(
                "Grade already exists for exam '" + req.examName() + "' for this student in this class");
        }

        Grade grade = Grade.builder()
            .tenantId(tenantId)
            .classId(req.classId())
            .studentId(req.studentId())
            .examName(req.examName())
            .examDate(req.examDate())
            .score(req.score())
            .maxScore(req.maxScore())
            .notes(req.notes())
            .createdAt(Instant.now())
            .build();
        return GradeDto.from(gradeRepository.save(grade));
    }

    /**
     * Update an existing grade. Only modifies non-null fields.
     *
     * @param gradeId the grade to update
     * @param req     the update request
     * @return updated GradeDto
     */
    @Transactional
    public GradeDto updateGrade(UUID gradeId, UpdateGradeRequest req) {
        Grade grade = gradeRepository.findById(gradeId)
            .orElseThrow(() -> new EntityNotFoundException("Grade", gradeId));
        if (req.examName() != null) grade.setExamName(req.examName());
        if (req.examDate() != null) grade.setExamDate(req.examDate());
        if (req.score() != null) grade.setScore(req.score());
        if (req.maxScore() != null) grade.setMaxScore(req.maxScore());
        if (req.notes() != null) grade.setNotes(req.notes());
        return GradeDto.from(gradeRepository.save(grade));
    }

    /**
     * Delete a grade entry.
     *
     * @param gradeId the grade to delete
     */
    @Transactional
    public void deleteGrade(UUID gradeId) {
        Grade grade = gradeRepository.findById(gradeId)
            .orElseThrow(() -> new EntityNotFoundException("Grade", gradeId));
        gradeRepository.delete(grade);
        log.info("Grade {} deleted", gradeId);
    }
}
