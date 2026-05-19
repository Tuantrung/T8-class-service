package com.classservice.grades;

import com.classservice.common.TenantContext;
import com.classservice.common.exception.EntityNotFoundException;
import com.classservice.grades.dto.CreateGradeRequest;
import com.classservice.grades.dto.GradeDto;
import com.classservice.grades.dto.UpdateGradeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;

    public List<GradeDto> listByClass(UUID classId) {
        return gradeRepository.findByClassId(classId).stream().map(GradeDto::from).toList();
    }

    public List<GradeDto> listByClassAndStudent(UUID classId, UUID studentId) {
        return gradeRepository.findByClassIdAndStudentId(classId, studentId)
            .stream().map(GradeDto::from).toList();
    }

    @Transactional
    public GradeDto createGrade(CreateGradeRequest req) {
        UUID tenantId = TenantContext.get();
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
        grade.setTenantId(tenantId);
        return GradeDto.from(gradeRepository.save(grade));
    }

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

    @Transactional
    public void deleteGrade(UUID gradeId) {
        Grade grade = gradeRepository.findById(gradeId)
            .orElseThrow(() -> new EntityNotFoundException("Grade", gradeId));
        gradeRepository.delete(grade);
    }
}
