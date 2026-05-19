package com.classservice.attendance;

import com.classservice.attendance.dto.AttendanceDto;
import com.classservice.attendance.dto.BulkAttendanceRequest;
import com.classservice.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> get(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getBySession(sessionId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> saveBulk(
            @PathVariable UUID sessionId,
            @Valid @RequestBody BulkAttendanceRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.saveBulk(sessionId, req)));
    }
}
