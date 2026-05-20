package com.classservice.attendance;

import com.classservice.attendance.dto.AttendanceDto;
import com.classservice.attendance.dto.BulkAttendanceRequest;
import com.classservice.attendance.dto.BulkAttendanceResponse;
import com.classservice.attendance.dto.UpdateAttendanceRequest;
import com.classservice.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for attendance management.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * Get all attendance records for a session.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> get(@RequestParam UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getBySession(sessionId)));
    }

    /**
     * Bulk save (upsert) attendance for an entire session.
     */
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkAttendanceResponse>> saveBulk(
            @Valid @RequestBody BulkAttendanceRequest req) {
        BulkAttendanceResponse response = attendanceService.saveBulk(req);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Update a single attendance record.
     */
    @PatchMapping("/{attendanceId}")
    public ResponseEntity<ApiResponse<AttendanceDto>> update(
            @PathVariable UUID attendanceId,
            @Valid @RequestBody UpdateAttendanceRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.updateAttendance(attendanceId, req)));
    }
}
