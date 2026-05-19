package com.classservice.attendance;

import com.classservice.attendance.dto.AttendanceDto;
import com.classservice.attendance.dto.AttendanceRecord;
import com.classservice.attendance.dto.BulkAttendanceRequest;
import com.classservice.common.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public List<AttendanceDto> getBySession(UUID sessionId) {
        return attendanceRepository.findBySessionId(sessionId)
            .stream().map(AttendanceDto::from).toList();
    }

    /**
     * Upsert bulk attendance for a session. Calling twice for the same session
     * updates existing records rather than creating duplicates.
     */
    @Transactional
    public List<AttendanceDto> saveBulk(UUID sessionId, BulkAttendanceRequest req) {
        UUID tenantId = TenantContext.get();
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
                attendance.setTenantId(tenantId);
                attendanceRepository.save(attendance);
            }
        }
        return attendanceRepository.findBySessionId(sessionId)
            .stream().map(AttendanceDto::from).toList();
    }
}
