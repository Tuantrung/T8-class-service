package com.classservice.attendance;

import com.classservice.attendance.dto.AttendanceDto;
import com.classservice.attendance.dto.AttendanceRecord;
import com.classservice.attendance.dto.BulkAttendanceRequest;
import com.classservice.attendance.dto.BulkAttendanceResponse;
import com.classservice.attendance.dto.UpdateAttendanceRequest;
import com.classservice.common.TenantContext;
import com.classservice.common.exception.EntityNotFoundException;
import com.classservice.sessions.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final SessionRepository sessionRepository;

    public List<AttendanceDto> getBySession(UUID sessionId) {
        UUID tenantId = TenantContext.get();
        // Verify session belongs to this tenant before returning attendance
        sessionRepository.findByIdAndTenantId(sessionId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Session", sessionId));
        return attendanceRepository.findBySessionId(sessionId)
            .stream().map(AttendanceDto::from).toList();
    }

    @Transactional
    public BulkAttendanceResponse saveBulk(BulkAttendanceRequest req) {
        UUID tenantId = TenantContext.get();
        UUID sessionId = req.sessionId();
        // Verify session belongs to this tenant
        sessionRepository.findByIdAndTenantId(sessionId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Session", sessionId));

        int count = 0;
        for (AttendanceRecord record : req.records()) {
            Attendance existing = attendanceRepository
                .findBySessionIdAndStudentId(sessionId, record.studentId())
                .orElse(null);
            if (existing != null) {
                existing.setStatus(record.status());
                attendanceRepository.save(existing);
            } else {
                Attendance attendance = Attendance.builder()
                    .tenantId(tenantId)
                    .sessionId(sessionId)
                    .studentId(record.studentId())
                    .status(record.status())
                    .createdAt(Instant.now())
                    .build();
                attendanceRepository.save(attendance);
            }
            count++;
        }

        log.info("Bulk attendance saved: {} records for session {}", count, sessionId);
        return new BulkAttendanceResponse(count, sessionId);
    }

    @Transactional
    public AttendanceDto updateAttendance(UUID attendanceId, UpdateAttendanceRequest req) {
        UUID tenantId = TenantContext.get();
        Attendance attendance = attendanceRepository.findByIdAndTenantId(attendanceId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Attendance", attendanceId));
        attendance.setStatus(req.status());
        return AttendanceDto.from(attendanceRepository.save(attendance));
    }
}
