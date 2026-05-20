package com.classservice.sessions;

import com.classservice.common.ApiResponse;
import com.classservice.common.PageResponse;
import com.classservice.sessions.dto.CreateSessionRequest;
import com.classservice.sessions.dto.SessionDto;
import com.classservice.sessions.dto.UpdateSessionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller for session management.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SessionController {

    private final SessionService sessionService;

    /**
     * List sessions for a class with optional date range filter.
     */
    @GetMapping
    public ResponseEntity<PageResponse<SessionDto>> listByClass(
            @RequestParam UUID classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(sessionService.listByClass(
            classId, from, to,
            PageRequest.of(page, size, Sort.by("sessionDate").descending())));
    }

    /**
     * Get a single session by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getSession(id)));
    }

    /**
     * Create a session. Defaults all enrolled students to PRESENT attendance.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SessionDto>> create(@Valid @RequestBody CreateSessionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(sessionService.createSession(req), "Session created"));
    }

    /**
     * Update session metadata.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSessionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.updateSession(id, req)));
    }

    /**
     * Delete a session. ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
