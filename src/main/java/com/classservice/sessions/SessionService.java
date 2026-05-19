package com.classservice.sessions;

import com.classservice.common.TenantContext;
import com.classservice.common.exception.EntityNotFoundException;
import com.classservice.sessions.dto.CreateSessionRequest;
import com.classservice.sessions.dto.SessionDto;
import com.classservice.sessions.dto.UpdateSessionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;

    public List<SessionDto> listByClass(UUID classId) {
        return sessionRepository.findByClassIdOrderBySessionDateDesc(classId)
            .stream().map(SessionDto::from).toList();
    }

    public SessionDto getSession(UUID sessionId) {
        UUID tenantId = TenantContext.get();
        return sessionRepository.findByIdAndTenantId(sessionId, tenantId)
            .map(SessionDto::from)
            .orElseThrow(() -> new EntityNotFoundException("Session", sessionId));
    }

    @Transactional
    public SessionDto createSession(CreateSessionRequest req) {
        UUID tenantId = TenantContext.get();
        Session session = Session.builder()
            .tenantId(tenantId)
            .classId(req.classId())
            .sessionDate(req.sessionDate())
            .startTime(req.startTime())
            .endTime(req.endTime())
            .topic(req.topic())
            .cancelledByTeacher(false)
            .createdAt(Instant.now())
            .build();
        session.setTenantId(tenantId);
        return SessionDto.from(sessionRepository.save(session));
    }

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

    @Transactional
    public void deleteSession(UUID sessionId) {
        UUID tenantId = TenantContext.get();
        Session session = sessionRepository.findByIdAndTenantId(sessionId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Session", sessionId));
        sessionRepository.delete(session);
    }
}
