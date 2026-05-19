package com.classservice.classes;

import com.classservice.classes.dto.ClassDto;
import com.classservice.classes.dto.CreateClassRequest;
import com.classservice.classes.dto.UpdateClassRequest;
import com.classservice.common.PageResponse;
import com.classservice.common.TenantContext;
import com.classservice.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;

    public PageResponse<ClassDto> listClasses(Pageable pageable) {
        UUID tenantId = TenantContext.get();
        return PageResponse.from(classRepository.findAllByTenantId(tenantId, pageable).map(ClassDto::from));
    }

    public ClassDto getClass(UUID classId) {
        UUID tenantId = TenantContext.get();
        return classRepository.findByIdAndTenantId(classId, tenantId)
            .map(ClassDto::from)
            .orElseThrow(() -> new EntityNotFoundException("TutoringClass", classId));
    }

    @Transactional
    public ClassDto createClass(CreateClassRequest req) {
        UUID tenantId = TenantContext.get();
        TutoringClass tc = TutoringClass.builder()
            .tenantId(tenantId)
            .name(req.name())
            .subject(req.subject())
            .teacherId(req.teacherId())
            .ratePerSession(req.ratePerSession())
            .status(ClassStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();
        // Ensure tenantId is set on TenantEntity superclass
        tc.setTenantId(tenantId);
        return ClassDto.from(classRepository.save(tc));
    }

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

    @Transactional
    public void deleteClass(UUID classId) {
        UUID tenantId = TenantContext.get();
        TutoringClass tc = classRepository.findByIdAndTenantId(classId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("TutoringClass", classId));
        classRepository.delete(tc);
    }

    @Transactional
    public void enrollStudents(UUID classId, List<UUID> studentIds) {
        UUID tenantId = TenantContext.get();
        classRepository.findByIdAndTenantId(classId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("TutoringClass", classId));
        for (UUID studentId : studentIds) {
            ClassStudent cs = ClassStudent.builder()
                .id(new ClassStudent.ClassStudentId(classId, studentId))
                .enrolledAt(Instant.now())
                .build();
            classStudentRepository.save(cs);
        }
    }
}
