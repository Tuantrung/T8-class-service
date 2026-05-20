package com.classservice.sessions;

import com.classservice.attendance.Attendance;
import com.classservice.attendance.AttendanceRepository;
import com.classservice.attendance.AttendanceStatus;
import com.classservice.classes.ClassRepository;
import com.classservice.classes.ClassStudent;
import com.classservice.classes.ClassStudentRepository;
import com.classservice.common.PageResponse;
import com.classservice.common.TenantContext;
import com.classservice.common.exception.EntityNotFoundException;
import com.classservice.sessions.dto.CreateSessionRequest;
import com.classservice.sessions.dto.SessionDto;
import com.classservice.sessions.dto.UpdateSessionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for session management with tenant isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final AttendanceRepository attendanceRepository;

    /**
     * List sessions for a class with optional date range filter.
     *
     * @param classId the class to filter by
     * @param from    optional start date (inclusive)
     * @param to      optional end date (inclusive)
     * @param pageable pagination parameters
     * @return paginated list of SessionDto
     */
    public PageResponse<SessionDto> listByClass(UUID classId, LocalDate from, LocalDate to, Pageable pageable) {
        if (from != null && to != null) {
            return PageResponse.from(
                sessionRepository.findByClassIdAndSessionDateBetween(classId, from, to, pageable)
                    .map(SessionDto::from));
        }
        return PageResponse.from(
            sessionRepository.findByClassIdOrderBySessionDateDesc(classId, pageable)
                .map(SessionDto::from));
    }

    /**
     * List all sessions for a class (no pagination).
     *
     * @param classId the class to filter by
     * @return list of SessionDto ordered by date descending
     */
    public List<SessionDto> listByClass(UUID classId) {
        return sessionRepository.findByClassIdOrderBySessionDateDesc(classId)
            .stream().map(SessionDto::from).toList();
    }

    /**
     * Get a single session by ID, scoped to the current tenant.
     *
     * @param sessionId the UUID of the session
     * @return SessionDto if found
     * @throws EntityNotFoundException if not found in this tenant
     */
    public SessionDto getSession(UUID sessionId) {
        UUID tenantId = TenantContext.get();
        return sessionRepository.findByIdAndTenantId(sessionId, tenantId)
            .map(SessionDto::from)
            .orElseThrow(() -> new EntityNotFoundException("Session", sessionId));
    }

    /**
     * Create a session for a class and default all enrolled students to PRESENT attendance.
     *
     * @param req the session creation request
     * @return the created SessionDto
     */
    @Transactional
    public SessionDto createSession(CreateSessionRequest req) {
        UUID tenantId = TenantContext.get();

        // Verify class belongs to tenant
        classRepository.findByIdAndTenantId(req.classId(), tenantId)
            .orElseThrow(() -> new EntityNotFoundException("TutoringClass", req.classId()));

        Session session = Session.builder()
            .tenantId(tenantId)
            .classId(req.classId())
            .sessionDate(req.sessionDate())
            .startTime(req.startTime())
            .endTime(req.endTime())
            .topic(req.topic())
            .cancelledByTeacher(req.cancelledByTeacher() != null && req.cancelledByTeacher())
            .createdAt(Instant.now())
            .build();
        Session saved = sessionRepository.save(session);

        // Default all enrolled students to PRESENT
        List<ClassStudent> enrollments = classStudentRepository.findAllByIdClassId(req.classId());
        for (ClassStudent enrollment : enrollments) {
            UUID studentId = enrollment.getId().getStudentId();
            Attendance attendance = Attendance.builder()
                .tenantId(tenantId)
                .sessionId(saved.getId())
                .studentId(studentId)
                .status(AttendanceStatus.PRESENT)
                .createdAt(Instant.now())
                .build();
            attendanceRepository.save(attendance);
        }
        log.info("Session {} created with {} default attendance records", saved.getId(), enrollments.size());

        return SessionDto.from(saved);
    }

    /**
     * Update session metadata (date, time, topic, cancellation status).
     *
     * @param sessionId the session to update
     * @param req       the update request fields
     * @return updated SessionDto
     */
    @Transactional
    public SessionDto updateSession(UUID sessionId, UpdateSessionRequest req) {
        UUID tenantId = TenantContext.get();
        Session session = sessionRepository.findByIdAndTenantId(sessionId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Session", sessionId));
        if (req.sessionDate() != null) session.setSessionDate(req.sessionDate());
        if (req.startTime() != null) session.setStartTime(req.startTime());
        if (req.endTime() != null) session.setEndTime(req.endTime());
        if (req.topic() != null) session.setTopic(req.topic());
        if (req.cancelledByTeacher() != null) session.setCancelledByTeacher(req.cancelledByTeacher());
        return SessionDto.from(sessionRepository.save(session));
    }

    /**
     * Delete a session. Should only be called if no attendance exists.
     *
     * @param sessionId the session to delete
     */
    @Transactional
    public void deleteSession(UUID sessionId) {
        UUID tenantId = TenantContext.get();
        Session session = sessionRepository.findByIdAndTenantId(sessionId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Session", sessionId));
        sessionRepository.delete(session);
        log.info("Session {} deleted from tenant {}", sessionId, tenantId);
    }
}
