package com.classservice.attendance;

import com.classservice.attendance.dto.AttendanceDto;
import com.classservice.attendance.dto.AttendanceRecord;
import com.classservice.attendance.dto.BulkAttendanceRequest;
import com.classservice.attendance.dto.BulkAttendanceResponse;
import com.classservice.attendance.dto.UpdateAttendanceRequest;
import com.classservice.common.TenantContext;
import com.classservice.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for attendance management with tenant isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    /**
     * Get all attendance records for a session.
     *
     * @param sessionId the session UUID
     * @return list of AttendanceDto
     */
    public List<AttendanceDto> getBySession(UUID sessionId) {
        return attendanceRepository.findBySessionId(sessionId)
            .stream().map(AttendanceDto::from).toList();
    }

    /**
     * Bulk upsert attendance for an entire session.
     * Existing records are updated; missing records are created.
     * The session must not be cancelled.
     *
     * @param req the bulk attendance request with sessionId and records
     * @return BulkAttendanceResponse with count saved and sessionId
     */
    @Transactional
    public BulkAttendanceResponse saveBulk(BulkAttendanceRequest req) {
        UUID tenantId = TenantContext.get();
        UUID sessionId = req.sessionId();
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

    /**
     * Update a single attendance record by ID.
     *
     * @param attendanceId the UUID of the attendance record
     * @param req          the update request with new status
     * @return updated AttendanceDto
     * @throws EntityNotFoundException if the attendance record is not found
     */
    @Transactional
    public AttendanceDto updateAttendance(UUID attendanceId, UpdateAttendanceRequest req) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
            .orElseThrow(() -> new EntityNotFoundException("Attendance", attendanceId));
        attendance.setStatus(req.status());
        return AttendanceDto.from(attendanceRepository.save(attendance));
    }
}
