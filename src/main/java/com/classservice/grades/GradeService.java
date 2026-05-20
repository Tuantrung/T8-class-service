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

@Slf4j
@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;

    public PageResponse<GradeDto> listGrades(UUID classId, UUID studentId, Pageable pageable) {
        UUID tenantId = TenantContext.get();
        if (studentId != null) {
            return PageResponse.from(
                gradeRepository.findByClassIdAndStudentIdAndTenantId(classId, studentId, tenantId, pageable)
                    .map(GradeDto::from));
        }
        return PageResponse.from(gradeRepository.findByClassIdAndTenantId(classId, tenantId, pageable).map(GradeDto::from));
    }

    public List<GradeDto> listByClass(UUID classId) {
        UUID tenantId = TenantContext.get();
        return gradeRepository.findByClassIdAndTenantId(classId, tenantId).stream().map(GradeDto::from).toList();
    }

    public List<GradeDto> listByClassAndStudent(UUID classId, UUID studentId) {
        UUID tenantId = TenantContext.get();
        return gradeRepository.findByClassIdAndStudentIdAndTenantId(classId, studentId, tenantId)
            .stream().map(GradeDto::from).toList();
    }

    @Transactional
    public GradeDto createGrade(CreateGradeRequest req) {
        UUID tenantId = TenantContext.get();
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

    @Transactional
    public GradeDto updateGrade(UUID gradeId, UpdateGradeRequest req) {
        UUID tenantId = TenantContext.get();
        Grade grade = gradeRepository.findByIdAndTenantId(gradeId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Grade", gradeId));
        if (req.examName() != null) grade.setExamName(req.examName());
        if (req.examDate() != null) grade.setExamDate(req.examDate());
        if (req.score() != null) grade.setScore(req.score());
        if (req.maxScore() != null) grade.setMaxScore(req.maxScore());
        if (req.notes() != null) grade.setNotes(req.notes());
        return GradeDto.from(gradeRepository.save(grade));
    }

    @Transactional
    public void deleteGrade(UUID gradeId) {
        UUID tenantId = TenantContext.get();
        Grade grade = gradeRepository.findByIdAndTenantId(gradeId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Grade", gradeId));
        gradeRepository.delete(grade);
        log.info("Grade {} deleted", gradeId);
    }
}
